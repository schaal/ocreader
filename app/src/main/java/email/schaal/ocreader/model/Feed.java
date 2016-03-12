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

import android.support.annotation.ColorInt;

import java.util.Date;

import io.realm.Realm;
import io.realm.RealmObject;
import io.realm.annotations.Ignore;
import io.realm.annotations.PrimaryKey;

/**
 * RealmObject representing a Feed
 */
public class Feed extends RealmObject implements TreeItem {
    @PrimaryKey
    private long id;

    private String url;
    private String title;
    private String link;
    private String faviconLink;
    public static final String FAVICON_LINK = "faviconLink";

    private Date added;
    private Long folderId;
    public static final String FOLDER_ID = "folderId";
    @Ignore
    private Folder folder;

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

    @ColorInt private Integer color;
    public static final String COLOR = "color";

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
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Date getAdded() {
        return added;
    }

    public void setAdded(Date added) {
        this.added = added;
    }

    public Folder folder() {
        if(folder == null)
            folder = ((Realm)realm).where(Folder.class).equalTo(ID, getFolderId()).findFirst();
        return folder;
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

    public int getStarredCount() {
        return starredCount;
    }

    public void setStarredCount(int starredCount) {
        this.starredCount = starredCount;
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

    @ColorInt public Integer getColor() {
        return color;
    }

    public void setColor(@ColorInt Integer color) {
        this.color = color;
    }
}
