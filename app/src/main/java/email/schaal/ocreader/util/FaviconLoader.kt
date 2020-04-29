package email.schaal.ocreader.util

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.util.Log
import android.util.LruCache
import android.widget.ImageView
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.palette.graphics.Palette
import androidx.palette.graphics.Palette.PaletteAsyncListener
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import email.schaal.ocreader.R
import email.schaal.ocreader.database.model.Feed
import java.util.*

/**
 * Load favicons
 */
class FaviconLoader private constructor(builder: Builder) {
    private val placeholder: Int
    private val imageView: ImageView?
    private val generateFallbackImage: Boolean
    fun load(context: Context, feed: Feed?, listener: FeedColorsListener) {
        if (context is Activity && context.isDestroyed) {
            return
        }
        listener.onStart()
        if (feed == null) {
            listener.onGenerated(FeedColors(null as Int?))
            return
        }
        if (feed.faviconLink != null) {
            val typedArray = context.obtainStyledAttributes(intArrayOf(android.R.attr.colorBackground))
            val colorBackground = typedArray.getColor(0, Color.WHITE)
            typedArray.recycle()
            // load favicon
            val requestBuilder: RequestBuilder<Bitmap> = GlideApp.with(context)
                    .asBitmap()
                    .placeholder(placeholder)
                    .load(feed.faviconLink)
            val myTarget = MyTarget(feed, listener, colorBackground)
            if (imageView != null) {
                requestBuilder.listener(myTarget).into(imageView)
            } else {
                requestBuilder.listener(myTarget).preload()
            }
        } else { // feed has no favicon
            if (imageView != null) {
                if (generateFallbackImage) { // generate image
                    imageView.setImageDrawable(getDrawable(context, feed))
                } else { // use placeholder
                    imageView.setImageResource(placeholder)
                }
            }
            listener.onGenerated(FeedColors(getFeedColor(context, feed)))
        }
    }

    private fun generatePalette(bitmap: Bitmap?, filter: Palette.Filter, paletteAsyncListener: PaletteAsyncListener) {
        if(bitmap != null)
            Palette.Builder(bitmap)
                    .addFilter(filter)
                    .generate(paletteAsyncListener)
        else
            paletteAsyncListener.onGenerated(null)
    }

    interface FeedColorsListener {
        fun onGenerated(feedColors: FeedColors)
        fun onStart()
    }

    class Builder(var imageView: ImageView? = null) {
        @DrawableRes
        var placeholder = R.drawable.ic_feed_icon
        var generateFallbackImage = true

        fun build(): FaviconLoader {
            return FaviconLoader(this)
        }

        fun withPlaceholder(@DrawableRes drawable: Int): Builder {
            placeholder = drawable
            return this
        }

        fun withGenerateFallbackImage(withGenerateFallbackImage: Boolean): Builder {
            generateFallbackImage = withGenerateFallbackImage
            return this
        }
    }

    private inner class MyTarget internal constructor(feed: Feed, private val listener: FeedColorsListener, @ColorInt backgroundColor: Int) : RequestListener<Bitmap?> {
        private val feedId: Long = feed.id
        private val contrastFilter: Palette.Filter
        override fun onLoadFailed(e: GlideException?, o: Any, target: Target<Bitmap?>, b: Boolean): Boolean {
            Log.e(TAG, "Loading favicon for feed with id $feedId failed", e)
            listener.onGenerated(FeedColors(null as Int?))
            return false
        }

        override fun onResourceReady(bitmap: Bitmap?, model: Any, target: Target<Bitmap?>, dataSource: DataSource, isFirstResource: Boolean): Boolean {
            val cachedFeedColors = feedColorsCache[feedId]
            if (cachedFeedColors == null) {
                generatePalette(bitmap, contrastFilter, PaletteAsyncListener { palette: Palette? ->
                    val feedColors = FeedColors(palette!!)
                    feedColorsCache.put(feedId, feedColors)
                    listener.onGenerated(feedColors)
                })
            } else {
                listener.onGenerated(cachedFeedColors)
            }
            return false
        }

        init {
            contrastFilter = Palette.Filter { rgb: Int, _: FloatArray? -> ColorUtils.calculateContrast(rgb, backgroundColor) >= 4 }
        }
    }

    companion object {
        private val TAG = FaviconLoader::class.java.name
        private val feedColorsCache = LruCache<Long, FeedColors>(32)
        private val faviconCache = LruCache<Long, Drawable?>(32)
        fun getCssColor(color: Int): String { // Use US locale so we always get a . as decimal separator for a valid css value
            return String.format(Locale.US, "rgba(%d,%d,%d,%.2f)",
                    Color.red(color),
                    Color.green(color),
                    Color.blue(color),
                    Color.alpha(color) / 255.0)
        }

        fun getDrawable(context: Context, feed: Feed?): Drawable? {
            return feed?.let {
                faviconCache[feed.id]
                        ?: TextDrawable.Builder(
                                if (feed.name.isNotEmpty()) feed.name.substring(0, 1) else "?", getFeedColor(context, feed))
                                .textColor(ContextCompat.getColor(context, R.color.textdrawable_text))
                                .build().also { faviconCache.put(feed.id, it) }
            } ?: AppCompatResources.getDrawable(context, R.drawable.ic_feed_icon)
        }

        private fun getFeedColor(context: Context, feed: Feed): Int {
            return ContextCompat.getColor(context, ColorGenerator.MATERIAL.getColor(feed.url))
        }

        fun clearCache() {
            feedColorsCache.evictAll()
            faviconCache.evictAll()
        }
    }

    init {
        placeholder = builder.placeholder
        imageView = builder.imageView
        generateFallbackImage = builder.generateFallbackImage
    }
}