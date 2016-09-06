package email.schaal.ocreader.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.support.annotation.ColorInt;
import android.support.annotation.Keep;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.safety.Cleaner;
import org.jsoup.safety.Whitelist;
import org.jsoup.select.Elements;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import email.schaal.ocreader.R;
import email.schaal.ocreader.database.model.Feed;
import email.schaal.ocreader.database.model.Item;
import email.schaal.ocreader.util.FaviconLoader;
import email.schaal.ocreader.util.FeedColors;
import email.schaal.ocreader.util.StringUtils;

/**
 * WebView to display a Item
 */
public class ArticleWebView extends NestedScrollWebView {
    @ColorInt
    private int defaultTitleColor;

    private Item item;

    private final FaviconLoader.FeedColorsListener feedColorsListener = new FaviconLoader.FeedColorsListener() {
        @Override
        public void onGenerated(@NonNull FeedColors feedColors) {
            int titleColor = feedColors.getColor(FeedColors.Type.TEXT, defaultTitleColor);
            String cssColor = FaviconLoader.getCssColor(titleColor);
            String javascript = String.format("javascript:(function(){document.styleSheets[0].cssRules[0].style.color=\"%s\";})()", cssColor);
            loadUrl(javascript);
        }

        @Override
        public void onStart() {

        }
    };

    // iframes are replaced in prepareDocument()
    private final static Cleaner cleaner = new Cleaner(Whitelist.relaxed().addTags("video","iframe").addAttributes("iframe", "src"));

    private static String css = null;

    private final static String videoThumbLink = "<div style=\"position:relative\"><a href=\"%s\"><img src=\"%s\" class=\"videothumb\"></img><span class=\"play\">▶</span></a></div>";
    private final static String videoLink = "<a href=\"%s\">%s</a>";

    @ColorInt private int fontColor;

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
        ViewCompat.setNestedScrollingEnabled(this, true);
        final TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.ArticleWebView);
        try {
            defaultTitleColor = typedArray.getColor(R.styleable.ArticleWebView_titleColor, 0);
            fontColor = typedArray.getColor(R.styleable.ArticleWebView_fontColor, 0);
            setBackgroundColor(typedArray.getColor(R.styleable.ArticleWebView_backgroundColor, 0));
        } finally {
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

    @Keep
    private class JsCallback {
        @JavascriptInterface
        public void startLoading() {
            getHandler().post(new Runnable() {
                @Override
                public void run() {
                    new FaviconLoader.Builder(item.getFeed())
                            .build()
                            .load(ArticleWebView.this.getContext(), feedColorsListener);
                }
            });
        }
    }

    private String getHtml() {
        Context context = getContext();
        if (css == null)
            try {
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(context.getAssets().open("item_page.css")));
                String line;
                StringBuilder cssBuilder = new StringBuilder();
                while ((line = bufferedReader.readLine()) != null) {
                    cssBuilder.append(line);
                }
                css = cssBuilder.toString();
            } catch (IOException e) {
                e.printStackTrace();
            }

        Feed feed = item.getFeed();

        Document document = Jsoup.parse(item.getBody());
        document = cleaner.clean(document);
        prepareDocument(document);

        StringBuilder pageBuilder = new StringBuilder(
                "<!DOCTYPE html><html><head><meta charset=\"UTF-8\">");

        pageBuilder.append(String.format(
                "<style type=\"text/css\">a:link, a:active,a:hover { color: %s } body { color: %s } blockquote { background-color: %s } %s</style>",
                FaviconLoader.getCssColor(fontColor), FaviconLoader.getCssColor(defaultTitleColor), FaviconLoader.getCssColor(ContextCompat.getColor(context, R.color.selected_background)), css));

        pageBuilder.append("</head><body>");

        pageBuilder.append(String.format(
                "<a href=\"%s\" class=\"title\">%s</a><p class=\"byline\">%s</p>",
                item.getUrl() != null ? item.getUrl() : "",
                item.getTitle(),
                StringUtils.getByLine(context, String.format("<a href=\"%s\">%s</a>", feed.getLink(), feed.getName()), item.getAuthor())
                )
        );

        document.outputSettings().prettyPrint(false);
        pageBuilder.append(document.body().html());

        pageBuilder.append("<script>(function() { JsCallback.startLoading(); })();</script>");

        pageBuilder.append("</body></html>");

        return pageBuilder.toString();
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
