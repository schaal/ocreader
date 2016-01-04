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

import io.realm.RealmList;
import io.realm.RealmObject;

/**
 * RealmObject to store which items have a changed unread or starred status. This items are then
 * synchronized with the ownCloud News instance.
 */
public class ChangedItems extends RealmObject {
    private RealmList<Item> unreadChangedItems;
    public static final String UNREAD_CHANGED_ITEMS = "unreadChangedItems";

    private RealmList<Item> starredChangedItems;
    public static final String STARRED_CHANGED_ITEMS = "unreadChangedItems";

    public RealmList<Item> getUnreadChangedItems() {
        return unreadChangedItems;
    }

    public void setUnreadChangedItems(RealmList<Item> unreadChangedItems) {
        this.unreadChangedItems = unreadChangedItems;
    }

    public RealmList<Item> getStarredChangedItems() {
        return starredChangedItems;
    }

    public void setStarredChangedItems(RealmList<Item> starredChangedItems) {
        this.starredChangedItems = starredChangedItems;
    }
}
