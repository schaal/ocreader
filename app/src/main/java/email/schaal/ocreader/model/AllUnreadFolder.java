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

package email.schaal.ocreader.model;

import android.content.Context;

import email.schaal.ocreader.R;

/**
 * TreeItem representing the folder with all unread items.
 */
public class AllUnreadFolder implements TreeItem, TreeIconable {
    public final static long ID = -10;

    private final String title;

    public AllUnreadFolder(Context context) {
        this.title = context.getString(R.string.unread_items);
    }

    @Override
    public long getId() {
        return ID;
    }

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public int getIcon() {
        return R.drawable.ic_feed_icon;
    }
}
