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

package email.schaal.ocreader.view;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.support.annotation.ColorInt;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.ColorUtils;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import email.schaal.ocreader.R;
import email.schaal.ocreader.model.Feed;
import email.schaal.ocreader.model.Item;
import email.schaal.ocreader.util.FaviconUtils;
import email.schaal.ocreader.util.StringUtils;

/**
 * RecyclerView.ViewHolder to display a feed Item.
 */
public class ItemViewHolder extends RecyclerView.ViewHolder implements Target {
    private static final String TAG = ItemViewHolder.class.getName();

    private final OnClickListener clickListener;

    @ColorInt private final int defaultFeedTextColor;

    private final TextView textViewTitle;
    private final TextView textViewFeedTitle;
    private final TextView textViewTime;
    private final ImageView faviconImageView;
    private final ImageView starImageView;

    private final View[] alphaViews;

    private final Palette.PaletteAsyncListener paletteAsyncListener;

    @Nullable
    private Long feedId;

    public ItemViewHolder(final View itemView, final OnClickListener clickListener) {
        super(itemView);
        this.clickListener = clickListener;

        textViewTitle = (TextView) itemView.findViewById(R.id.textViewTitle);
        textViewFeedTitle = (TextView) itemView.findViewById(R.id.textViewFeedTitle);
        textViewTime = (TextView) itemView.findViewById(R.id.textViewTime);

        faviconImageView = (ImageView) itemView.findViewById(R.id.imageview_favicon);
        starImageView = (ImageView) itemView.findViewById(R.id.imageview_star);

        defaultFeedTextColor = ContextCompat.getColor(itemView.getContext(),R.color.secondary_text);

        alphaViews = new View[] {
                textViewTitle,
                textViewFeedTitle,
                textViewTime,
                faviconImageView,
                starImageView
        };

        paletteAsyncListener = new Palette.PaletteAsyncListener() {
            @Override
            public void onGenerated(Palette palette) {
                textViewFeedTitle.setTextColor(FaviconUtils.getTextColor(palette, defaultFeedTextColor));
            }
        };
    }

    public void bindItem(final Item item, final int position) {
        textViewTitle.setText(item.getTitle());

        final Feed feed = Item.feed(item);

        if(feed != null) {
            textViewFeedTitle.setText(feed.getTitle());
            feedId = feed.getId();
        } else {
            Log.w(TAG, "Feed == null");
            textViewFeedTitle.setText("");
            feedId = null;
        }

        textViewTime.setText(StringUtils.getTimeSpanString(itemView.getContext(), item.getPubDate()));

        FaviconUtils.getInstance().loadBitmap(itemView.getContext(), feed, this);

        itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                clickListener.onItemClick(item, position);
            }
        });

        setUnreadState(item.isUnread());
        setStarredState(item.isStarred());
    }

    private void setUnreadState(boolean unread) {
        float alpha = unread ? 1.0f : 0.5f;
        for(View view: alphaViews) {
            view.setAlpha(alpha);
        }
    }

    private void setStarredState(boolean starred) {
        starImageView.setVisibility(starred ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
        faviconImageView.setImageBitmap(bitmap);
        FaviconUtils.getInstance().loadPalette(bitmap, feedId, paletteAsyncListener);
    }

    @Override
    public void onBitmapFailed(Drawable errorDrawable) {

    }

    @Override
    public void onPrepareLoad(Drawable placeHolderDrawable) {
        faviconImageView.setImageDrawable(placeHolderDrawable);
        textViewFeedTitle.setTextColor(defaultFeedTextColor);
    }

    public interface OnClickListener {
        void onItemClick(Item item, int position);
    }
}
