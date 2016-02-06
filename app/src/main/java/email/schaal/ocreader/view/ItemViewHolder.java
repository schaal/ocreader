/*
 * Copyright (C) 2015 Daniel Schaal <daniel@schaal.email>
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
import android.support.v4.content.ContextCompat;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import email.schaal.ocreader.R;
import email.schaal.ocreader.model.Feed;
import email.schaal.ocreader.model.Item;
import email.schaal.ocreader.util.FaviconUtils;
import email.schaal.ocreader.util.StringUtils;

/**
 * RecyclerView.ViewHolder to display a feed Item.
 */
public class ItemViewHolder extends RecyclerView.ViewHolder {
    private static final String TAG = ItemViewHolder.class.getSimpleName();

    private final OnClickListener clickListener;
    private final Drawable defaultFeedDrawable;
    private final TextView textViewTitle;
    private final TextView textViewFeedTitle;
    private final TextView textViewTime;
    private final ImageView faviconImageView;
    private final ImageView starImageView;

    private final View[] alphaViews;
    private final FaviconUtils.PaletteBitmapAsyncListener paletteAsyncListener;

    public ItemViewHolder(final View itemView, final OnClickListener clickListener, final Drawable defaultFeedDrawable) {
        super(itemView);
        this.clickListener = clickListener;
        this.defaultFeedDrawable = defaultFeedDrawable;

        textViewTitle = (TextView) itemView.findViewById(R.id.textViewTitle);
        textViewFeedTitle = (TextView) itemView.findViewById(R.id.textViewFeedTitle);
        textViewTime = (TextView) itemView.findViewById(R.id.textViewTime);

        faviconImageView = (ImageView) itemView.findViewById(R.id.imageview_favicon);
        starImageView = (ImageView) itemView.findViewById(R.id.imageview_star);

        final int defaultFeedTextColor = ContextCompat.getColor(itemView.getContext(),R.color.secondary_text);

        paletteAsyncListener = new FaviconUtils.PaletteBitmapAsyncListener() {
            @Override
            public void onGenerated(Palette palette, Bitmap bitmap) {
                if (bitmap != null)
                    faviconImageView.setImageBitmap(bitmap);

                if (palette != null)
                    textViewFeedTitle.setTextColor(palette.getDarkVibrantColor(defaultFeedTextColor));
                else
                    textViewFeedTitle.setTextColor(defaultFeedTextColor);
            }
        };

        alphaViews = new View[] {
                textViewTitle,
                textViewFeedTitle,
                textViewTime,
                faviconImageView,
                starImageView
        };
    }

    public void bindItem(final Item item, final int position) {
        textViewTitle.setText(item.getTitle());

        final Feed feed = Item.feed(item);

        if(feed != null)
            textViewFeedTitle.setText(feed.getTitle());
        else
            textViewFeedTitle.setText("");
        textViewTime.setText(StringUtils.getTimeSpanString(itemView.getContext(), item.getPubDate()));

        faviconImageView.setImageDrawable(defaultFeedDrawable);
        FaviconUtils.getInstance().loadFavicon(itemView.getContext(), feed, paletteAsyncListener);

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

    public interface OnClickListener {
        void onItemClick(Item item, int position);
    }
}
