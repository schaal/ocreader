/*
 * Copyright (C) 2015-2016 Daniel Schaal <daniel@schaal.email>
 * Parts Copyright 2015 The Android Open Source Project
 *
 * This file is part of OCReader.
 *
 * OCReader is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * OCReader is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with OCReader.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package email.schaal.ocreader.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.graphics.Palette;
import android.util.LruCache;
import android.view.View;
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
 * Utility class to manage Feed favicons.
 */
public class FaviconUtils {
    private final LruCache<Long, FeedColors> feedColorsCache = new LruCache<>(32);
    private final LruCache<Long, Drawable> faviconCache = new LruCache<>(32);
    private static FaviconUtils instance;

    public static FaviconUtils getInstance() {
        if(instance == null)
            instance = new FaviconUtils();
        return instance;
    }

    private FaviconUtils() {

    }

    public static String getCssColor(int color) {
        // Use US locale so we always get a . as decimal separator for a valid css value
        return String.format(Locale.US,"rgba(%d,%d,%d,%.2f)",
                Color.red(color),
                Color.green(color),
                Color.blue(color),
                Color.alpha(color) / 255.0);
    }

    public void loadFavicon(final View view, @Nullable final Feed feed, @NonNull final PaletteBitmapAsyncListener paletteAsyncListener) {
        if(feed != null && feed.getFaviconLink() != null) {
            RequestCreator requestCreator = Picasso.with(view.getContext()).load(feed.getFaviconLink());
            MyTarget myTarget = new MyTarget(feed, view, paletteAsyncListener);
            if(view instanceof ImageView) {
                requestCreator.placeholder(R.drawable.ic_feed_icon).into((ImageView) view, myTarget);
            } else {
                requestCreator.into(myTarget);
            }
        } else {
            if(feed != null)
                paletteAsyncListener.onGenerated(new FeedColors(ColorGenerator.MATERIAL.getColor(feed.getId())), getDrawable(view.getContext(), feed));
            else
                paletteAsyncListener.onGenerated(null, null);
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

    public Drawable getDrawable(Context context, @Nullable Feed feed) {
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

    public interface PaletteBitmapAsyncListener {
        void onGenerated(@Nullable FeedColors palette, @Nullable Drawable favicon);
    }

    private class MyTarget implements Target, Callback {
        private final Feed feed;
        private final View view;
        private final PaletteBitmapAsyncListener paletteAsyncListener;

        public MyTarget(Feed feed, View view, PaletteBitmapAsyncListener paletteAsyncListener) {
            this.feed = feed;
            this.view = view;
            view.setTag(this);
            this.paletteAsyncListener = paletteAsyncListener;
        }

        @Override
        public void onBitmapLoaded(final Bitmap bitmap, Picasso.LoadedFrom from) {
            final FeedColors cachedFeedColors = feedColorsCache.get(feed.getId());
            final BitmapDrawable bitmapDrawable = new BitmapDrawable(view.getResources(), bitmap);
            if(cachedFeedColors == null) {
                generatePalette(bitmap, new Palette.PaletteAsyncListener() {
                    @Override
                    public void onGenerated(Palette palette) {
                        FeedColors feedColors = new FeedColors(palette);
                        feedColorsCache.put(feed.getId(), feedColors);
                        paletteAsyncListener.onGenerated(feedColors, bitmapDrawable);
                    }
                });
            } else {
                paletteAsyncListener.onGenerated(cachedFeedColors, bitmapDrawable);
            }
        }

        @Override
        public void onBitmapFailed(Drawable errorDrawable) {
            paletteAsyncListener.onGenerated(new FeedColors(ColorGenerator.MATERIAL.getColor(feed.getId())), getDrawable(view.getContext(), feed));
        }

        @Override
        public void onPrepareLoad(Drawable placeHolderDrawable) {

        }

        @Override
        public void onSuccess() {
            onBitmapLoaded(((BitmapDrawable)((ImageView)view).getDrawable()).getBitmap(), null);
        }

        @Override
        public void onError() {
            onBitmapFailed(null);
        }
    }
}
