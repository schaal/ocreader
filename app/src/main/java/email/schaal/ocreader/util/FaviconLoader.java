package email.schaal.ocreader.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.graphics.Palette;
import android.util.LruCache;
import android.widget.ImageView;

import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.RequestCreator;
import com.squareup.picasso.Target;

import java.util.Locale;

import email.schaal.ocreader.R;
import email.schaal.ocreader.model.Feed;

import static android.support.v7.graphics.Target.DARK_VIBRANT;
import static android.support.v7.graphics.Target.LIGHT_MUTED;
import static android.support.v7.graphics.Target.VIBRANT;

/**
 * Load favicons
 */
public class FaviconLoader {
    private final static LruCache<Long, FeedColors> feedColorsCache = new LruCache<>(32);
    private final static LruCache<Long, Drawable> faviconCache = new LruCache<>(32);

    private final int placeholder;
    private final ImageView imageView;
    private final Feed feed;
    private final boolean generateFallbackImage;
    private final Context context;

    private FaviconLoader(Builder builder) {
        placeholder = builder.placeholder;
        imageView = builder.imageView;
        context = builder.context;
        feed = builder.feed;
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
                drawable = new TextDrawable.Builder().build(feed.getTitle().substring(0, 1).toUpperCase(), ColorGenerator.MATERIAL.getColor(feed.getId()));
                faviconCache.put(feed.getId(), drawable);
            }
        } else {
            drawable = ContextCompat.getDrawable(context, R.drawable.ic_feed_icon);
        }
        return drawable;
    }

    public void load(FeedColorsListener listener) {
        listener.onStart();
        if(feed == null) {
            listener.onGenerated(null);
            return;
        }

        if(feed.getFaviconLink() != null) {
            // load favicon
            RequestCreator requestCreator = Picasso.with(context).load(feed.getFaviconLink());
            MyTarget myTarget = new MyTarget(feed, listener);

            if(imageView != null) {
                requestCreator.placeholder(placeholder).into(imageView, myTarget);
            } else {
                requestCreator.into(myTarget);
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
            listener.onGenerated(new FeedColors(ColorGenerator.MATERIAL.getColor(feed.getId())));
        }
    }

    private void generatePalette(Bitmap bitmap, Palette.PaletteAsyncListener paletteAsyncListener) {
        new Palette.Builder(bitmap)
                .clearTargets()
                .addTarget(DARK_VIBRANT)
                .addTarget(VIBRANT)
                .addTarget(LIGHT_MUTED)
                .generate(paletteAsyncListener);
    }

    public interface FeedColorsListener {
        void onGenerated(FeedColors feedColors);
        void onStart();
    }

    public static class Builder {
        @DrawableRes
        private int placeholder = R.drawable.ic_feed_icon;
        private ImageView imageView;
        private Feed feed;
        private boolean generateFallbackImage = true;
        private Context context;

        public Builder(@NonNull Context context, Feed feed) {
            this.context = context;
            this.feed = feed;
        }

        public Builder(@NonNull ImageView imageView, Feed feed) {
            this.imageView = imageView;
            this.context = imageView.getContext();
            this.feed = feed;
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

    private class MyTarget implements Target, Callback {
        private final Feed feed;
        private final FeedColorsListener listener;

        public MyTarget(Feed feed, FeedColorsListener listener) {
            this.feed = feed;
            this.listener = listener;
        }

        @Override
        public void onBitmapLoaded(final Bitmap bitmap, Picasso.LoadedFrom from) {
            final FeedColors cachedFeedColors = feedColorsCache.get(feed.getId());
            if(cachedFeedColors == null) {
                generatePalette(bitmap, new Palette.PaletteAsyncListener() {
                    @Override
                    public void onGenerated(Palette palette) {
                        FeedColors feedColors = new FeedColors(palette);
                        feedColorsCache.put(feed.getId(), feedColors);
                        listener.onGenerated(feedColors);
                    }
                });
            } else {
                listener.onGenerated(cachedFeedColors);
            }
        }

        @Override
        public void onBitmapFailed(Drawable errorDrawable) {
            listener.onGenerated(new FeedColors(ColorGenerator.MATERIAL.getColor(feed.getId())));
        }

        @Override
        public void onPrepareLoad(Drawable placeHolderDrawable) {

        }

        @Override
        public void onSuccess() {
            if(imageView != null)
                onBitmapLoaded(((BitmapDrawable)(imageView).getDrawable()).getBitmap(), null);
        }

        @Override
        public void onError() {
            onBitmapFailed(null);
        }
    }
}
