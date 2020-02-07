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

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import email.schaal.ocreader.database.model.Folder
import io.realm.Realm

class FolderViewModel: ViewModel() {
    private val realm: Realm = Realm.getDefaultInstance()
    val foldersLiveData = LiveRealmResults(Folder.getAll(realm, false))

    override fun onCleared() {
        super.onCleared()
        realm.close()
    }
}