/*
 * Copyright (C) 2015-2016 Daniel Schaal <daniel@schaal.email>
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
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.graphics.Palette;
import android.util.LruCache;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import email.schaal.ocreader.R;
import email.schaal.ocreader.model.Feed;

/**
 * Utility class to manage Feed favicons.
 */
public class FaviconUtils {
    private final LruCache<Long, Palette> paletteCache = new LruCache<>(32);
    private static FaviconUtils instance;

    public static FaviconUtils getInstance() {
        if(instance == null)
            instance = new FaviconUtils();
        return instance;
    }

    private FaviconUtils() {

    }

    public void loadFavicon(final Context context, @Nullable final Feed feed, @NonNull final PaletteBitmapAsyncListener paletteAsyncListener) {
        if(feed != null && feed.getFaviconLink() != null) {
            Picasso.with(context).load(feed.getFaviconLink()).into(new Target() {
                @Override
                public void onBitmapLoaded(final Bitmap bitmap, Picasso.LoadedFrom from) {
                    Palette palette = paletteCache.get(feed.getId());
                    if(palette == null)
                        new Palette.Builder(bitmap).generate(new Palette.PaletteAsyncListener() {
                            @Override
                            public void onGenerated(Palette palette) {
                                paletteCache.put(feed.getId(), palette);
                                paletteAsyncListener.onGenerated(palette, bitmap);
                            }
                        });
                    else {
                        paletteAsyncListener.onGenerated(palette, bitmap);
                    }
                }

                @Override
                public void onBitmapFailed(Drawable errorDrawable) {
                    paletteAsyncListener.onGenerated(null, null);
                }

                @Override
                public void onPrepareLoad(Drawable placeHolderDrawable) {

                }
            });
        } else {
            paletteAsyncListener.onGenerated(null, null);
        }
    }

    public void loadBitmap(final Context context, @Nullable final Feed feed, final Target target) {
        Picasso.with(context).load(feed != null ? feed.getFaviconLink() : null).placeholder(R.drawable.ic_feed_icon).into(target);
    }

    public void loadPalette(final Bitmap bitmap, @Nullable final Long feedId, final Palette.PaletteAsyncListener paletteAsyncListener) {
        if(feedId != null) {
            Palette palette = paletteCache.get(feedId);
            if (palette == null) {
                new Palette.Builder(bitmap).generate(new Palette.PaletteAsyncListener() {
                    @Override
                    public void onGenerated(Palette palette) {
                        paletteCache.put(feedId, palette);
                        paletteAsyncListener.onGenerated(palette);
                    }
                });
            } else {
                paletteAsyncListener.onGenerated(palette);
            }
        }
    }

    public interface PaletteBitmapAsyncListener {
        void onGenerated(@Nullable Palette palette, @Nullable Bitmap bitmap);
    }
}
