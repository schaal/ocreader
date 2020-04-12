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

package email.schaal.ocreader.api.json

import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonClass
import com.squareup.moshi.ToJson
import email.schaal.ocreader.database.model.User
import java.util.*

class UserJsonTypeAdapter {
    @FromJson
    fun fromJson(jsonUser: JsonUser): User {
        return User(jsonUser.userId, jsonUser.displayName, jsonUser.lastLoginTimestamp, jsonUser.avatar?.get("data"), jsonUser.avatar?.get("mime"))
    }

    @ToJson
    fun toJson(user: User): JsonUser {
        return JsonUser(user.userId, user.displayName, user.lastLogin, user.avatar, user.avatarMime)
    }
}

@JsonClass(generateAdapter = true)
class JsonUser (
        val userId: String,
        val displayName: String,
        val lastLoginTimestamp: Date,
        val avatar: Map<String, String>? = null
) {
    constructor(userId: String, displayName: String, lastLoginTimeStamp: Date, avatar: String?, avatarMime: String?) : this(
            userId,
            displayName,
            lastLoginTimeStamp,
            if(avatar != null && avatarMime != null ) mapOf("data" to avatar, "mime" to avatarMime) else null
    )
}
