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

package email.schaal.ocreader.database.model

import io.realm.Realm
import io.realm.RealmObject
import io.realm.annotations.PrimaryKey
import io.realm.annotations.Required
import java.util.Date

open class User(
        @PrimaryKey @Required var userId: String = "",
        var displayName: String = "",
        var lastLogin: Date = Date(),
        var avatar: String? = null,
        var avatarMime: String? = null
) : RealmObject(), Insertable {
    override fun insert(realm: Realm) {
        realm.insertOrUpdate(this)
    }

    override fun delete(realm: Realm) {
        deleteFromRealm()
    }
}