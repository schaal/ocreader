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

import android.util.Log
import io.realm.Realm
import io.realm.RealmModel
import io.realm.RealmObject
import io.realm.annotations.PrimaryKey
import io.realm.annotations.RealmClass
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

@RealmClass
open class User(
        var userId: String = "",
        var displayName: String = "",
) : RealmModel, Insertable {
    @PrimaryKey var id = 0L

    override fun insert(realm: Realm) {
        realm.insertOrUpdate(this)
    }

    override fun delete(realm: Realm) {
        RealmObject.deleteFromRealm(this)
    }

    fun avatarUrl(baseUrl: String): String? {
        return baseUrl.toHttpUrlOrNull()?.resolve("avatar/${userId}/128")?.toString().also {
            if (it != null) {
                Log.d("User", it)
            }
        }
    }
}