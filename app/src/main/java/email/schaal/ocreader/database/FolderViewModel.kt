/*
 * Copyright © 2020. Daniel Schaal <daniel@schaal.email>
 *
 * This file is part of ocreader.
 *
 * ocreader is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ocreader is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Foobar.  If not, see <http://www.gnu.org/licenses/>.
 */

package email.schaal.ocreader.database

import email.schaal.ocreader.database.model.Feed
import email.schaal.ocreader.database.model.Folder
import io.realm.kotlin.where

class FolderViewModel: RealmViewModel() {
    val foldersLiveData = LiveRealmResults(Folder.getAll(realm, false))
    val feedsLiveData = LiveRealmResults(realm.where<Feed>().sort(Feed::name.name).findAll())
}