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

package email.schaal.ocreader.database.model;

import android.content.Context;
import android.preference.PreferenceManager;

import java.util.List;

import email.schaal.ocreader.Preferences;
import email.schaal.ocreader.R;
import io.realm.Realm;
import io.realm.RealmQuery;
import io.realm.Sort;

/**
 * TreeItem representing the folder with all unread items.
 */
public class AllUnreadFolder implements TreeItem, TreeIconable {
    public final static long ID = -10;

    private String name;

    public AllUnreadFolder(Context context) {
        updateName(context, Preferences.SHOW_ONLY_UNREAD.getBoolean(PreferenceManager.getDefaultSharedPreferences(context)));
    }

    @Override
    public long getId() {
        return ID;
    }

    @Override
    public String getName() {
        return name;
    }

    public void updateName(Context context, boolean onlyUnread) {
        this.name = onlyUnread ? context.getString(R.string.unread_items) : context.getString(R.string.all_items);
    }

    @Override
    public int getCount(Realm realm) {
        return realm.where(Feed.class).sum(Feed.UNREAD_COUNT).intValue();
    }

    @Override
    public boolean canLoadMore() {
        return false;
    }

    @Override
    public List<Feed> getFeeds(Realm realm, boolean onlyUnread) {
        final RealmQuery<Feed> query = realm.where(Feed.class);
        if(onlyUnread)
            query.greaterThan(Feed.UNREAD_COUNT, 0);
        return query.findAllSorted(Feed.NAME, Sort.ASCENDING);
    }

    @Override
    public List<Item> getItems(Realm realm, boolean onlyUnread) {
        final RealmQuery<Item> query = realm.where(Item.class);
        if(onlyUnread)
            query.equalTo(Item.UNREAD, true);
        return query.distinct(Item.FINGERPRINT);
    }

    @Override
    public int getIcon() {
        return R.drawable.ic_feed_icon;
    }
}
