/*
 * Copyright (C) 2017 Tobias Kaminsky <tobias@kaminsky.me>
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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import email.schaal.ocreader.R;
import io.realm.Realm;

/**
 * TreeItem representing the folder with fresh items (< 24h old).
 */
public class FreshFolder implements TreeItem, TreeIconable {
    public final static long ID = -12;

    private static final int MAX_ARTICLE_AGE = 24 * 60 * 60 * 1000;

    private final String name;

    public FreshFolder(Context context) {
        this.name = context.getString(R.string.fresh_items);
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
        return (int) realm.where(Item.class).equalTo(Item.UNREAD, true).greaterThan(Item.PUB_DATE, getDate()).count();
    }

    @Override
    public boolean canLoadMore() {
        return false;
    }

    @Override
    public List<Feed> getFeeds(Realm realm, boolean onlyUnread) {
        List<Feed> freshFeeds = new ArrayList<>();

        for (Item item: getItems(realm, false)) {
            if (!freshFeeds.contains(item.getFeed())){
                freshFeeds.add(item.getFeed());
            }
        }

        return freshFeeds;
    }

    @Override
    public List<Item> getItems(Realm realm, boolean onlyUnread) {
        return realm.where(Item.class).equalTo(Item.UNREAD, true).greaterThan(Item.PUB_DATE, getDate()).findAll();
    }

    @Override
    public int getIcon() {
        return R.drawable.fresh;
    }

    private Date getDate() {
        return new Date(System.currentTimeMillis() - MAX_ARTICLE_AGE);
    }
}
