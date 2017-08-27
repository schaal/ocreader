package email.schaal.ocreader.util;

import android.app.Activity;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.support.annotation.ColorInt;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.ColorUtils;
import android.support.v7.content.res.AppCompatResources;
import android.support.v7.graphics.Palette;
import android.util.Log;
import android.util.LruCache;
import android.widget.ImageView;

import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;

import java.util.Locale;

import email.schaal.ocreader.R;
import email.schaal.ocreader.database.model.Feed;

/**
 * Load favicons
 */
public class FaviconLoader {
    private final static String TAG = FaviconLoader.class.getName();

    private final static LruCache<Long, FeedColors> feedColorsCache = new LruCache<>(32);
    private final static LruCache<Long, Drawable> faviconCache = new LruCache<>(32);

    private final int placeholder;
    private final ImageView imageView;
    private final boolean generateFallbackImage;

    private FaviconLoader(Builder builder) {
        placeholder = builder.placeholder;
        imageView = builder.imageView;
        generateFallbackImage = builder.generateFallbackImage;
    }

    public static String getCssColor(int color) {
        // Use US locale so we always get a . as decimal separator for a valid css value
        return String.format(Locale.US,"rgba(%d,%d,%d,%.2f)",
                Color.red(color),
                Color.green(color),
                Color.blue(color),
                Color.alpha(color) / 255.0);
    }

    public static Drawable getDrawable(Context context, @Nullable Feed feed) {
        Drawable drawable;

        if(feed != null && feed.getFaviconLink() == null) {
            drawable = faviconCache.get(feed.getId());

            if(drawable == null) {
                drawable = new TextDrawable.Builder(feed.getName().length() > 0 ? feed.getName().substring(0, 1) : "?", getFeedColor(context, feed))
                        .textColor(ContextCompat.getColor(context, R.color.textdrawable_text))
                        .build();
                faviconCache.put(feed.getId(), drawable);
            }
        } else {
            drawable = AppCompatResources.getDrawable(context, R.drawable.ic_feed_icon);
        }
        return drawable;
    }

    private static int getFeedColor(Context context, @NonNull Feed feed) {
        return ContextCompat.getColor(context,ColorGenerator.MATERIAL.getColor(feed.getUrl()));
    }

    public static void clearCache() {
        feedColorsCache.evictAll();
        faviconCache.evictAll();
    }
    public void load(Context context, Feed feed, @NonNull FeedColorsListener listener) {
        if(context instanceof Activity && ((Activity) context).isDestroyed()) {
             return;
        }

        listener.onStart();
        if(feed == null) {
            listener.onGenerated(new FeedColors((Integer)null));
            return;
        }

        if(feed.getFaviconLink() != null) {
            TypedArray typedArray = context.obtainStyledAttributes(new int[] { android.R.attr.colorBackground });
            final int colorBackground = typedArray.getColor(0, Color.WHITE);
            typedArray.recycle();
            // load favicon
            RequestBuilder<Bitmap> requestBuilder = GlideApp.with(context)
                    .asBitmap()
                    .placeholder(placeholder)
                    .load(feed.getFaviconLink());
            MyTarget myTarget = new MyTarget(feed, listener, colorBackground);

            if(imageView != null) {
                requestBuilder.listener(myTarget).into(imageView);
            } else {
                requestBuilder.listener(myTarget).preload();
            }
        } else {
            // feed has no favicon
            if (imageView != null) {
                if (generateFallbackImage) {
                    // generate image
                    imageView.setImageDrawable(getDrawable(context, feed));
                } else {
                    // use placeholder
                    imageView.setImageResource(placeholder);
                }
            }
            listener.onGenerated(new FeedColors(getFeedColor(context, feed)));
        }
    }

    private void generatePalette(Bitmap bitmap, Palette.Filter filter, Palette.PaletteAsyncListener paletteAsyncListener) {
        new Palette.Builder(bitmap)
                .addFilter(filter)
                .generate(paletteAsyncListener);
    }

    public interface FeedColorsListener {
        void onGenerated(@NonNull FeedColors feedColors);
        void onStart();
    }

    public static class Builder {
        @DrawableRes
        private int placeholder = R.drawable.ic_feed_icon;
        private ImageView imageView;
        private boolean generateFallbackImage = true;

        public Builder() {
        }

        public Builder(@NonNull ImageView imageView) {
            this.imageView = imageView;
        }

        public FaviconLoader build() {
            return new FaviconLoader(this);
        }

        public Builder withPlaceholder(@DrawableRes int drawable) {
            this.placeholder = drawable;
            return this;
        }

        public Builder withGenerateFallbackImage(boolean withGenerateFallbackImage) {
            this.generateFallbackImage = withGenerateFallbackImage;
            return this;
        }
    }

    private class MyTarget implements RequestListener<Bitmap> {
        private final long feedId;
        private final FeedColorsListener listener;
        private final Palette.Filter contrastFilter;

        MyTarget(@NonNull Feed feed, @NonNull FeedColorsListener listener, final @ColorInt int backgroundColor) {
            this.feedId = feed.getId();
            this.listener = listener;
            contrastFilter = new Palette.Filter() {
                @Override
                public boolean isAllowed(int rgb, float[] hsl) {
                    return ColorUtils.calculateContrast(rgb, backgroundColor) >= 4;
                }
            };
        }

        @Override
        public boolean onLoadFailed(@Nullable GlideException e, Object o, Target<Bitmap> target, boolean b) {
            Log.e(TAG, "Loading favicon for feed with id " + feedId + " failed", e);
            listener.onGenerated(new FeedColors((Integer)null));
            return false;
        }

        @Override
        public boolean onResourceReady(Bitmap bitmap, Object model, Target<Bitmap> target, DataSource dataSource, boolean isFirstResource) {
            final FeedColors cachedFeedColors = feedColorsCache.get(feedId);
            if (cachedFeedColors == null) {
                generatePalette(bitmap, contrastFilter, new Palette.PaletteAsyncListener() {
                    @Override
                    public void onGenerated(Palette palette) {
                        FeedColors feedColors = new FeedColors(palette);
                        feedColorsCache.put(feedId, feedColors);
                        listener.onGenerated(feedColors);
                    }
                });
            } else {
                listener.onGenerated(cachedFeedColors);
            }
            return false;
        }
    }
}
