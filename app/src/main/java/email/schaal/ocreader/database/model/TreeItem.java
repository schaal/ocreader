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

import java.util.Comparator;
import java.util.List;

import io.realm.Realm;

/**
 * Interface representing a TreeItem (Feed, Folder or "special" Folders such as AllUnreadFolder).
 */
public interface TreeItem {
    String ID = "id";
    String NAME = "name";

    Comparator<TreeItem> COMPARATOR = new Comparator<TreeItem>() {
        @Override
        public int compare(TreeItem lhs, TreeItem rhs) {
            return lhs.getId() < rhs.getId() ? -1 : (lhs.getId() == rhs.getId() ? 0 : 1);
        }
    };

    long getId();
    String getName();
    int getCount(Realm realm);
    List<Feed> getFeeds(Realm realm);
}
