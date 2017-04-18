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

import io.realm.Realm;
import io.realm.RealmList;
import io.realm.RealmModel;
import io.realm.annotations.PrimaryKey;
import io.realm.annotations.RealmClass;

/**
 * TemporaryFeed allows to store the currently displayed Items.
 */
@SuppressWarnings("unused")
@RealmClass
public class TemporaryFeed implements RealmModel {
    public static final int LIST_ID = 0;
    public static final int PAGER_ID = 1;

    @PrimaryKey
    private long id;
    public static final String ID = "id";

    private long treeItemId;
    public static final String TREE_ITEM_ID = "treeItemId";

    private String name;

    private RealmList<Item> items;

    public TemporaryFeed() {
    }

    public TemporaryFeed(long id) {
        this.id = id;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getTreeItemId() {
        return treeItemId;
    }

    public void setTreeItemId(long treeItemId) {
        this.treeItemId = treeItemId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public RealmList<Item> getItems() {
        return items;
    }

    public void setItems(RealmList<Item> items) {
        this.items = items;
    }

    public static TemporaryFeed getListTemporaryFeed(Realm realm) {
        return realm.where(TemporaryFeed.class).equalTo(ID, LIST_ID).findFirst();
    }

    public static TemporaryFeed getPagerTemporaryFeed(Realm realm) {
        return realm.where(TemporaryFeed.class).equalTo(ID, PAGER_ID).findFirst();
    }

    public static void updatePagerTemporaryFeed(Realm realm) {
        realm.executeTransaction(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                final TemporaryFeed listTempFeed = getListTemporaryFeed(realm);
                final TemporaryFeed pagerTempFeed = getPagerTemporaryFeed(realm);

                pagerTempFeed.setItems(listTempFeed.getItems());
                pagerTempFeed.setName(listTempFeed.getName());
                pagerTempFeed.setTreeItemId(listTempFeed.getTreeItemId());
            }
        });
    }
}
