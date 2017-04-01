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

import android.support.annotation.Nullable;

import java.util.Iterator;
import java.util.List;

import email.schaal.ocreader.R;
import io.realm.Realm;
import io.realm.RealmObject;
import io.realm.RealmQuery;
import io.realm.RealmResults;
import io.realm.Sort;
import io.realm.annotations.PrimaryKey;

/**
 * RealmObject representing a Folder.
 */
public class Folder extends RealmObject implements TreeItem, Insertable, TreeIconable {
    @PrimaryKey
    private long id;

    private String name;

    public Folder() {
    }

    public Folder(long id) {
        this.id = id;
    }

    @Override
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    @Override
    public int getCount(Realm realm) {
        return realm.where(Feed.class).equalTo(Feed.FOLDER_ID, getId()).sum(Feed.UNREAD_COUNT).intValue();
    }

    @Override
    public List<Feed> getFeeds(Realm realm, boolean onlyUnread) {
        final RealmQuery<Feed> query = realm.where(Feed.class).equalTo(Feed.FOLDER_ID, getId());
        if(onlyUnread)
            query.greaterThan(Feed.UNREAD_COUNT, 0);
        return query.findAllSorted(Feed.NAME, Sort.ASCENDING);
    }

    @Override
    public List<Item> getItems(Realm realm, boolean onlyUnread) {
        // Get all feeds belonging to Folder treeItem
        List<Feed> feeds = getFeeds(realm, onlyUnread);
        RealmQuery<Item> query = null;
        if(feeds != null && feeds.size() > 0) {
            // Find all items belonging to any feed from this folder
            Iterator<Feed> feedIterator = feeds.iterator();
            query = realm.where(Item.class)
                    .equalTo(Item.FEED_ID, feedIterator.next().getId());
            while (feedIterator.hasNext()) {
                query.or().equalTo(Item.FEED_ID, feedIterator.next().getId());
            }
            if(onlyUnread)
                query.equalTo(Item.UNREAD, true);
        }
        return query != null ? query.distinct(Item.FINGERPRINT) : null;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public boolean equals(Object obj) {
        if(obj instanceof Folder)
            return ((Folder) obj).getId() == getId();
        return super.equals(obj);
    }

    @Override
    public int hashCode() {
        return Long.valueOf(getId()).hashCode();
    }

    @Override
    public void insert(Realm realm) {
        if(getName() != null)
            realm.insertOrUpdate(this);
    }

    @Override
    public void delete(Realm realm) {
        for(Feed feed: getFeeds(realm, false)) {
            feed.delete(realm);
        }
        deleteFromRealm();
    }

    @Nullable
    public static Folder get(Realm realm, long id) {
        return realm.where(Folder.class).equalTo(Folder.ID, id).findFirst();
    }

    /**
     * Return the folder with id folderId, or insert a new (temporary) folder into the database.
     * @param realm Database to operate on
     * @param folderId id of the folder
     * @return Folder with id folderId (either from the database or a newly created one)
     */
    @Nullable
    public static Folder getOrCreate(Realm realm, long folderId) {
        // root has folderId == 0, which has no folder in db
        if(folderId == 0)
            return null;

        Folder folder = Folder.get(realm, folderId);
        if(folder == null) {
            folder = realm.createObject(Folder.class, folderId);
        }
        return folder;
    }

    @Nullable
    public static List<Folder> getAll(Realm realm, boolean onlyUnread) {
        RealmQuery<Folder> query = null;
        if(onlyUnread) {
            RealmResults<Feed> unreadFeeds = realm.where(Feed.class).greaterThan(Feed.UNREAD_COUNT, 0).notEqualTo(Feed.FOLDER_ID, 0).findAll();
            if(unreadFeeds.size() > 0) {
                Iterator<Feed> feedIterator = unreadFeeds.iterator();
                query = realm.where(Folder.class)
                        .equalTo(Folder.ID, feedIterator.next().getFolderId());
                while (feedIterator.hasNext()) {
                    query.or().equalTo(Folder.ID, feedIterator.next().getFolderId());
                }
            }
        } else {
            query = realm.where(Folder.class);
        }

        return query != null ? query.findAllSorted(Folder.NAME, Sort.ASCENDING) : null;
    }

    @Override
    public int getIcon() {
        return R.drawable.ic_folder;
    }
}
