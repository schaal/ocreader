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
import android.support.annotation.Nullable;

import java.util.Collections;
import java.util.Date;
import java.util.List;

import email.schaal.ocreader.R;
import email.schaal.ocreader.database.Queries;
import io.realm.Realm;
import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

/**
 * RealmObject representing a Feed
 */
public class Feed extends RealmObject implements TreeItem, Insertable {
    @PrimaryKey
    private long id;

    private String url;
    private String name;
    private String link;
    private String faviconLink;
    public static final String FAVICON_LINK = "faviconLink";

    private Date added;
    private Long folderId;
    public static final String FOLDER_ID = "folderId";

    private Folder folder;
    public static final String FOLDER = "folder";

    private int unreadCount;
    public static final String UNREAD_COUNT = "unreadCount";

    /**
     * Not part of the JSON response, calculated in-app
     */
    private int starredCount;
    public static final String STARRED_COUNT = "starredCount";

    /**
     * @since 5.1.0
     */
    private int ordering;
    /**
     * @since 6.0.3
     */
    private boolean pinned;
    public static final String PINNED = "pinned";

    /**
     * @since 8.6.0
     */
    private int updateErrorCount;
    public static final String UPDATE_ERROR_COUNT = "updateErrorCount";

    /**
     * @since 8.6.0
     */
    private String lastUpdateError;
    public static final String LAST_UPDATE_ERROR = "lastUpdateError";

    public Feed() {
    }

    public Feed(long id) {
        this.id = id;
    }

    @Override
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public String getFaviconLink() {
        return faviconLink;
    }

    public void setFaviconLink(String faviconLink) {
        this.faviconLink = faviconLink;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public int getCount(Realm realm) {
        return getUnreadCount();
    }

    @Override
    public List<Feed> getFeeds(Realm realm) {
        return Collections.singletonList(this);
    }

    @Override
    public List<Item> getItems(Realm realm, boolean onlyUnread) {
        return realm.where(Item.class).equalTo(Item.FEED_ID, id).findAll();
    }

    public void setName(String name) {
        this.name = name;
    }

    public Date getAdded() {
        return added;
    }

    public void setAdded(Date added) {
        this.added = added;
    }

    public Long getFolderId() {
        return folderId;
    }

    public void setFolderId(Long folderId) {
        this.folderId = folderId;
    }

    public int getUnreadCount() {
        return unreadCount;
    }

    public void setUnreadCount(int unreadCount) {
        this.unreadCount = unreadCount;
    }

    public void incrementUnreadCount(int increment) {
        unreadCount += increment;
    }

    public int getStarredCount() {
        return starredCount;
    }

    public void setStarredCount(int starredCount) {
        this.starredCount = starredCount;
    }

    public void incrementStarredCount(int increment) {
        starredCount += increment;
    }

    public int getOrdering() {
        return ordering;
    }

    public void setOrdering(int ordering) {
        this.ordering = ordering;
    }

    public boolean isPinned() {
        return pinned;
    }

    public void setPinned(boolean pinned) {
        this.pinned = pinned;
    }

    public int getUpdateErrorCount() {
        return updateErrorCount;
    }

    public void setUpdateErrorCount(int updateErrorCount) {
        this.updateErrorCount = updateErrorCount;
    }

    public String getLastUpdateError() {
        return lastUpdateError;
    }

    public void setLastUpdateError(String lastUpdateError) {
        this.lastUpdateError = lastUpdateError;
    }

    public void setFolder(Folder folder) {
        this.folder = folder;
    }

    public Folder getFolder() {
        return folder;
    }

    public String getFolderTitle(Context context) {
        if(folder == null) {
            if(folderId == 0)
                return context.getString(R.string.root_folder);
            else
                return null;
        } else
            return folder.getName();
    }

    public boolean isConsideredFailed() {
        return updateErrorCount >= 50;
    }

    @Override
    public void insert(Realm realm) {
        if(getName() != null) {
            setFolder(Folder.getOrCreate(realm, folderId));
            realm.insertOrUpdate(this);
        }
    }

    @Override
    public void delete(Realm realm) {
        realm.where(Item.class).equalTo(Item.FEED_ID, getId()).findAll().deleteAllFromRealm();
        deleteFromRealm();
    }

    @Nullable
    public static Feed get(Realm realm, long id) {
        return realm.where(Feed.class).equalTo(Feed.ID, id).findFirst();
    }

    /**
     * Return the feed with id feedId, or insert a new (temporary) feed into the database.
     * @param realm Database to operate on
     * @param feedId id of the feed
     * @return Feed with id feedId (either from the database or a newly created one)
     */
    public static Feed getOrCreate(Realm realm, long feedId) {
        Feed feed = Feed.get(realm, feedId);
        if(feed == null) {
            feed = realm.createObject(Feed.class);
            feed.setId(feedId);
        }
        return feed;
    }
}
