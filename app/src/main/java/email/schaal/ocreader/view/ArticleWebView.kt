package email.schaal.ocreader.view

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.webkit.JavascriptInterface
import androidx.annotation.ColorInt
import androidx.annotation.Keep
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import email.schaal.ocreader.Preferences
import email.schaal.ocreader.R
import email.schaal.ocreader.database.model.Item
import email.schaal.ocreader.util.FaviconLoader
import email.schaal.ocreader.util.FaviconLoader.FeedColorsListener
import email.schaal.ocreader.util.FeedColors
import email.schaal.ocreader.util.asCssString
import email.schaal.ocreader.util.getByLine
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.safety.Cleaner
import org.jsoup.safety.Whitelist
import java.util.*
import java.util.regex.Pattern

/**
 * WebView to display a Item
 */
@SuppressLint("SetJavaScriptEnabled")
class ArticleWebView(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0): NestedScrollWebView(context, attrs, defStyleAttr) {
    @ColorInt private var defaultLinkColor = 0
    @ColorInt private var fontColor = 0
    @ColorInt private var backColor = 0
    private var item: Item? = null

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context) : this(context, null)

    private val feedColorsListener: FeedColorsListener = object : FeedColorsListener {
        override fun onGenerated(feedColors: FeedColors) {
            val titleColor = feedColors.getColor(FeedColors.Type.TEXT, defaultLinkColor)
            val javascript = resources.getString(R.string.style_change_js, titleColor.asCssString())
            loadUrl(javascript)
        }

        override fun onStart() {}
    }

    init {
        context.obtainStyledAttributes(attrs, R.styleable.ArticleWebView).also { typedArray ->
            defaultLinkColor = typedArray.getColor(R.styleable.ArticleWebView_linkColor, 0)
            fontColor = typedArray.getColor(R.styleable.ArticleWebView_fontColor, 0)
            backColor = typedArray.getColor(R.styleable.ArticleWebView_backgroundColor, 0)
            setBackgroundColor(backColor)
        }.recycle()

        settings.apply {
            javaScriptEnabled = true
            builtInZoomControls = true
            displayZoomControls = false
            offscreenPreRaster = true
        }
        addJavascriptInterface(JsCallback(), "JsCallback")
    }

    fun setItem(item: Item?) {
        this.item = item
        loadDataWithBaseURL("file:///android_asset/", html, "text/html", "UTF-8", null)
    }

    @Keep
    private inner class JsCallback {
        @JavascriptInterface
        fun startLoading() {
            post {
                FaviconLoader.Builder()
                        .build()
                        .load(this@ArticleWebView.context, item?.feed, feedColorsListener)
            }
        }
    }

    private val html: String
        get() {
            val context = context
            val font = Preferences.ARTICLE_FONT.getString(PreferenceManager.getDefaultSharedPreferences(context))
            var document = Jsoup.parse(item?.body)
            document = cleaner.clean(document)
            val firstImgString = extractFirstImg(document)
            prepareDocument(document)
            document.outputSettings().prettyPrint(false)
            return context.getString(R.string.article_html_template,
                    defaultLinkColor.asCssString(),
                    fontColor.asCssString(),
                    backColor.asCssString(),
                    ContextCompat.getColor(context, R.color.selected_background).asCssString(),
                    item?.url ?: "",
                    item?.title,
                    getByLine(context, "<p class=\"byline\">%s</p>", item?.author, item?.feed),
                    document.body().html(),
                    firstImgString,
                    if ("system" != font) context.getString(R.string.crimson_font_css) else ""
            )
        }

    private fun extractFirstImg(document: Document): String {
        var firstImgString = ""
        try {
            var child = document.body().child(0)
            // if document starts with <br>, remove it
            if (child?.tagName() == "br") {
                val brChild = child
                child = child.nextElementSibling()
                brChild.remove()
            }
            while (child != null && child.tagName() != "img") {
                child = child.children().first()
            }
            if (child != null) {
                child.remove()
                child.addClass("headerimg")
                firstImgString = child.toString()
            }
        } catch (e: IndexOutOfBoundsException) {
            Log.e(TAG, "Body has no children", e)
        }
        return firstImgString
    }

    /**
     * Enum to convert some common iframe urls to simpler formats
     */
    private enum class IframePattern(val pattern: Pattern, val baseUrl: String, val thumbUrl: String?) {
        YOUTUBE(Pattern.compile("(https?://)(?:www\\.)?youtube\\.com/embed/([a-zA-Z0-9-_]+)(?:\\?.*)?"), "youtu.be/", "%simg.youtube.com/vi/%s/sddefault.jpg"),
        VIMEO(Pattern.compile("(https?://)(?:www\\.)?player\\.vimeo\\.com/video/([a-zA-Z0-9]+)"), "vimeo.com/", null);

    }

    private fun prepareDocument(document: Document) {
        val iframes = document.getElementsByTag("iframe")
        for (iframe in iframes) {
            if (iframe.hasAttr("src")) {
                var href = iframe.attr("src")
                var html = String.format(Locale.US, videoLink, href, href)
                // Check if url matches any known patterns
                for (iframePattern in IframePattern.values()) {
                    val matcher = iframePattern.pattern.matcher(href)
                    if (matcher.matches()) {
                        val videoId = matcher.group(2)
                        val urlPrefix = matcher.group(1) ?: "https://"
                        href = urlPrefix + iframePattern.baseUrl + videoId
                        // use thumbnail if available
                        if (iframePattern.thumbUrl != null) {
                            val thumbUrl = String.format(iframePattern.thumbUrl, urlPrefix, videoId)
                            html = String.format(Locale.US, videoThumbLink, href, thumbUrl)
                        }
                        break
                    }
                }
                iframe.replaceWith(Jsoup.parse(html).body().child(0))
            } else {
                iframe.remove()
            }
        }
    }

    companion object {
        private val TAG = ArticleWebView::class.java.name
        // iframes are replaced in prepareDocument()
        private val cleaner = Cleaner(Whitelist.relaxed().addTags("video", "iframe").addAttributes("iframe", "src"))
        private const val videoThumbLink = """<div style="position:relative"><a href="%s"><img src="%s" class="videothumb"></img><span class="play">▶</span></a></div>"""
        private const val videoLink = """<a href="%s">%s</a>"""
    }
}