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

import java.util.List;

import email.schaal.ocreader.R;
import io.realm.Realm;
import io.realm.Sort;

/**
 * TreeItem representing the folder with starred items.
 */
public class StarredFolder implements TreeItem, TreeIconable {
    public final static long ID = -11;

    private final String name;

    public StarredFolder(Context context) {
        this.name = context.getString(R.string.starred_items);
    }

    @Override
    public long getId() {
        return ID;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public int getCount(Realm realm) {
        return (int) realm.where(Item.class).equalTo(Item.STARRED, true).count();
    }

    @Override
    public List<Feed> getFeeds(Realm realm) {
        return realm.where(Feed.class).greaterThan(Feed.STARRED_COUNT, 0).findAllSorted(Feed.NAME, Sort.ASCENDING);
    }

    @Override
    public int getIcon() {
        return R.drawable.ic_star_outline;
    }
}
