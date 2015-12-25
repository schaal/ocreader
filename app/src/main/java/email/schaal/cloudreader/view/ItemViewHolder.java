/*
 * Copyright (C) 2015 Daniel Schaal <daniel@schaal.email>
 *
 * This file is part of Cloudreader.
 *
 * Cloudreader is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Cloudreader is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Cloudreader.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package email.schaal.cloudreader.view;

import android.graphics.Bitmap;
import android.support.v4.content.ContextCompat;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import email.schaal.cloudreader.R;
import email.schaal.cloudreader.model.Feed;
import email.schaal.cloudreader.model.Item;
import email.schaal.cloudreader.util.FaviconUtils;
import email.schaal.cloudreader.util.StringUtils;

/**
 * Created by daniel on 08.11.15.
 */
public class ItemViewHolder extends RecyclerView.ViewHolder {
    private static final String TAG = ItemViewHolder.class.getSimpleName();

    private final OnClickListener clickListener;
    private final TextView textViewTitle;
    private final TextView textViewFeedTitle;
    private final TextView textViewTime;
    private final ImageView faviconImageView;
    private final ImageView starImageView;

    private final int defaultFeedTextColor;

    private final View[] alphaViews;
    private final FaviconUtils.PaletteBitmapAsyncListener paletteAsyncListener;

    public ItemViewHolder(final View itemView, final ItemViewHolder.OnClickListener clickListener) {
        super(itemView);
        this.clickListener = clickListener;

        textViewTitle = (TextView) itemView.findViewById(R.id.textViewTitle);
        textViewFeedTitle = (TextView) itemView.findViewById(R.id.textViewFeedTitle);
        textViewTime = (TextView) itemView.findViewById(R.id.textViewTime);

        faviconImageView = (ImageView) itemView.findViewById(R.id.imageview_favicon);
        starImageView = (ImageView) itemView.findViewById(R.id.imageview_star);

        defaultFeedTextColor = ContextCompat.getColor(itemView.getContext(),R.color.secondary_text);

        paletteAsyncListener = new FaviconUtils.PaletteBitmapAsyncListener() {
            @Override
            public void onGenerated(Palette palette, Bitmap bitmap) {
                if (bitmap != null) {
                    faviconImageView.setImageBitmap(bitmap);
                } else {
                    faviconImageView.setImageDrawable(ContextCompat.getDrawable(itemView.getContext(), R.drawable.ic_feed_icon));
                }

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
