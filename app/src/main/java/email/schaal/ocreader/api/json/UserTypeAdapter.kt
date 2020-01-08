/*
 * Copyright (C) 2015-2016 Daniel Schaal <daniel@schaal.email>
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
package email.schaal.ocreader.api.json

import android.util.Log
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import email.schaal.ocreader.database.model.User
import java.io.IOException
import java.util.*

/**
 * TypeAdapter to deserialize the JSON response for Users.
 */
class UserTypeAdapter : JsonAdapter<User?>() {
    @Throws(IOException::class)
    override fun toJson(out: JsonWriter, value: User?) {
    }

    @Throws(IOException::class)
    override fun fromJson(`in`: JsonReader): User? {
        if (`in`.peek() == JsonReader.Token.NULL) {
            `in`.nextNull<Any>()
            return null
        }
        val user = User()
        `in`.beginObject()
        while (`in`.hasNext()) {
            when (val name = `in`.nextName()) {
                "userId" -> user.userId = `in`.nextString()
                "displayName" -> user.displayName = `in`.nextString()
                "lastLoginTimestamp" -> user.lastLogin = Date(`in`.nextLong() * 1000)
                "avatar" -> if (`in`.peek() == JsonReader.Token.NULL) `in`.skipValue() else readAvatar(`in`, user, name)
                else -> {
                    Log.w(TAG, "Unknown value in user json: $name")
                    `in`.skipValue()
                }
            }
        }
        `in`.endObject()
        return user
    }

    @Throws(IOException::class)
    private fun readAvatar(`in`: JsonReader, user: User, name: String) {
        `in`.beginObject()
        while (`in`.hasNext()) {
            when (`in`.nextName()) {
                "data" -> user.avatar = `in`.nextString()
                "mime" -> user.avatarMime = `in`.nextString()
                else -> {
                    Log.w(TAG, "Unknown value in avatar json: $name")
                    `in`.skipValue()
                }
            }
        }
        `in`.endObject()
    }

    companion object {
        private val TAG = UserTypeAdapter::class.java.name
    }
}