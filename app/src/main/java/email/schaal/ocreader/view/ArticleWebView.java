package email.schaal.ocreader.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.support.annotation.ColorInt;
import android.support.annotation.Keep;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;

import com.google.common.base.Strings;
import com.vdurmont.emoji.EmojiManager;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.TextNode;
import org.jsoup.safety.Cleaner;
import org.jsoup.safety.Whitelist;
import org.jsoup.select.Elements;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import email.schaal.ocreader.R;
import email.schaal.ocreader.database.model.Item;
import email.schaal.ocreader.util.FaviconLoader;
import email.schaal.ocreader.util.FeedColors;
import email.schaal.ocreader.util.StringUtils;

/**
 * WebView to display a Item
 */
public class ArticleWebView extends NestedScrollWebView {
    private final static String TAG = ArticleWebView.class.getName();

    @ColorInt
    private int defaultLinkColor;

    private Item item;

    private final FaviconLoader.FeedColorsListener feedColorsListener = new FaviconLoader.FeedColorsListener() {
        @Override
        public void onGenerated(@NonNull FeedColors feedColors) {
            int titleColor = feedColors.getColor(FeedColors.Type.TEXT, defaultLinkColor);
            String cssColor = FaviconLoader.getCssColor(titleColor);
            String javascript = getResources().getString(R.string.style_change_js, cssColor);
            loadUrl(javascript);
        }

        @Override
        public void onStart() {

        }
    };

    // iframes are replaced in prepareDocument()
    private final static Cleaner cleaner = new Cleaner(Whitelist.relaxed().addTags("video","iframe").addAttributes("iframe", "src"));

    private final static String videoThumbLink = "<div style=\"position:relative\"><a href=\"%s\"><img src=\"%s\" class=\"videothumb\"></img><span class=\"play\">▶</span></a></div>";
    private final static String videoLink = "<a href=\"%s\">%s</a>";

    @ColorInt private int fontColor;
    @ColorInt private int backgroundColor;

    private int savedScrollPosition;

    public ArticleWebView(Context context) {
        super(context);

        init(context, null);
    }

    public ArticleWebView(Context context, AttributeSet attrs) {
        super(context, attrs);

        init(context, attrs);
    }

    public ArticleWebView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        init(context, attrs);
    }

    // All js from external sites gets stripped using jsoup
    @SuppressLint({"AddJavascriptInterface","SetJavaScriptEnabled"})
    private void init(Context context, AttributeSet attrs) {
        TypedArray typedArray = null;
        try {
            typedArray = context.obtainStyledAttributes(attrs, R.styleable.ArticleWebView);
            defaultLinkColor = typedArray.getColor(R.styleable.ArticleWebView_linkColor, 0);
            fontColor = typedArray.getColor(R.styleable.ArticleWebView_fontColor, 0);
            backgroundColor = typedArray.getColor(R.styleable.ArticleWebView_backgroundColor, 0);
            setBackgroundColor(backgroundColor);
        } finally {
            if(typedArray != null)
                typedArray.recycle();
        }

        WebSettings webSettings = getSettings();

        webSettings.setJavaScriptEnabled(true);

        webSettings.setBuiltInZoomControls(true);
        webSettings.setDisplayZoomControls(false);

        addJavascriptInterface(new JsCallback(), "JsCallback");
    }

    public void setItem(Item item) {
        this.item = item;
        loadDataWithBaseURL(null, getHtml(), "text/html", "UTF-8", null);
    }

    public void setScrollPosition(int position) {
        this.savedScrollPosition = position;
    }

    @Keep
    private class JsCallback {
        @JavascriptInterface
        public void startLoading() {
            post(() -> {
                new FaviconLoader.Builder()
                        .build()
                        .load(ArticleWebView.this.getContext(), item.getFeed(), feedColorsListener);
                setScrollY(savedScrollPosition);
            });
        }
    }

    private String getHtml() {
        Context context = getContext();

        Document document = Jsoup.parse(item.getBody());
        document = cleaner.clean(document);

        String firstImgString = extractFirstImg(document);

        prepareDocument(document);

        document.outputSettings().prettyPrint(false);

        return context.getString(R.string.article_html_template,
                FaviconLoader.getCssColor(defaultLinkColor),
                FaviconLoader.getCssColor(fontColor),
                FaviconLoader.getCssColor(backgroundColor),
                FaviconLoader.getCssColor(ContextCompat.getColor(context, R.color.selected_background)),
                Strings.nullToEmpty(item.getUrl()),
                item.getTitle(),
                StringUtils.getByLine(context, "<p class=\"byline\">%s</p>", item.getAuthor()),
                document.body().html(),
                firstImgString
        );
    }

    private String extractFirstImg(Document document) {
        String firstImgString = "";

        try {
            Element child = document.body().child(0);

            // if document starts with <br>, remove it
            if (child.tagName().equals("br")) {
                Element brChild = child;
                child = child.nextElementSibling();
                brChild.remove();
            }

            while (child != null && !child.tagName().equals("img")) {
                child = child.children().first();
            }

            if (child != null) {
                child.remove();
                child.addClass("headerimg");
                firstImgString = child.toString();
            }
        } catch (IndexOutOfBoundsException e) {
            Log.e(TAG, "Body has no children", e);
        }

        return firstImgString;
    }

    /**
     * Enum to convert some common iframe urls to simpler formats
     */
    private enum IframePattern {
        YOUTUBE(Pattern.compile("(https?://)(?:www\\.)?youtube\\.com/embed/([a-zA-Z0-9-_]+)(?:\\?.*)?"), "youtu.be/", "%simg.youtube.com/vi/%s/sddefault.jpg"),
        VIMEO(Pattern.compile("(https?://)(?:www\\.)?player\\.vimeo\\.com/video/([a-zA-Z0-9]+)"), "vimeo.com/", null);

        final Pattern pattern;
        final String baseUrl;
        final String thumbUrl;

        IframePattern(Pattern pattern, String baseUrl, String thumbUrl) {
            this.pattern = pattern;
            this.baseUrl = baseUrl;
            this.thumbUrl = thumbUrl;
        }
    }

    private void prepareDocument(Document document) {
        // Some blog engines replace emojis with an image and place the emoji in the image tag.
        // Find images with the tag being a single character and check if they are emoji. Then
        // replace the img with the actual emoji in unicode.
        Elements imgs = document.select("img[alt~=^.$]");
        for(Element img: imgs) {
            final String possibleEmoji = img.attr("alt");

            if(EmojiManager.isEmoji(possibleEmoji))
                img.replaceWith(new TextNode(possibleEmoji, ""));
        }

        Elements iframes = document.getElementsByTag("iframe");
        for(Element iframe: iframes) {
            if(iframe.hasAttr("src")) {
                String href = iframe.attr("src");
                String html = String.format(Locale.US, videoLink, href, href);

                // Check if url matches any known patterns
                for (IframePattern iframePattern : IframePattern.values()) {
                    Matcher matcher = iframePattern.pattern.matcher(href);
                    if (matcher.matches()) {
                        final String videoId = matcher.group(2);
                        String urlPrefix = matcher.group(1);
                        href = urlPrefix + iframePattern.baseUrl + videoId;
                        // use thumbnail if available
                        if (iframePattern.thumbUrl != null) {
                            String thumbUrl = String.format(iframePattern.thumbUrl, urlPrefix, videoId);
                            html = String.format(Locale.US, videoThumbLink, href, thumbUrl);
                        }
                        break;
                    }
                }

                iframe.replaceWith(Jsoup.parse(html).body().child(0));
            } else {
                iframe.remove();
            }
        }
    }
}
