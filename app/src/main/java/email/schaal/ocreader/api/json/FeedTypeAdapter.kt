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

import android.text.TextUtils
import android.util.Log
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import email.schaal.ocreader.database.model.Feed
import email.schaal.ocreader.util.cleanString
import io.realm.RealmObject
import java.io.IOException

/**
 * TypeAdapter to deserialize the JSON response for Feeds.
 */
class FeedTypeAdapter : JsonAdapter<Feed?>() {
    @Throws(IOException::class)
    override fun toJson(out: JsonWriter, feed: Feed?) {
        if(feed == null)
            return

        out.beginObject()
        // Only write url for feeds that are not in the database
        if (!RealmObject.isManaged(feed)) {
            out.name("url")
            out.value(feed.url)
        }
        out.name("folderId")
        out.value(feed.folderId)
        out.endObject()
    }

    @Throws(IOException::class)
    override fun fromJson(`in`: JsonReader): Feed? {
        if (`in`.peek() == JsonReader.Token.NULL) {
            `in`.nextNull<Any>()
            return null
        }
        val reader = NullableJsonReader(`in`)
        val feed = Feed()
        `in`.beginObject()
        while (`in`.hasNext()) {
            when (val name = `in`.nextName()) {
                "id" -> feed.id = `in`.nextLong()
                "url" -> feed.url = reader.nextString() ?: ""
                "link" -> feed.link = reader.nextString() ?: ""
                "faviconLink" -> {
                    var faviconLink = reader.nextString()
                    if (faviconLink != null && TextUtils.getTrimmedLength(faviconLink) == 0) faviconLink = null
                    feed.faviconLink = faviconLink
                }
                "title", "name" -> feed.name = cleanString(`in`.nextString())
                "added" -> `in`.skipValue()
                "folderId" -> feed.folderId = `in`.nextLong()
                "unreadCount" ->  // skip unreadCount, calculated later in SyncService
                    `in`.skipValue()
                "ordering" ->  // createFeed returns a feed with ordering set to NULL
                    feed.ordering = reader.nextInt(0)
                "pinned" ->  // createFeed returns a feed with pinned set to NULL
                    feed.pinned = reader.nextBoolean(false)
                "updateErrorCount" -> feed.updateErrorCount = reader.nextInt(0)
                "lastUpdateError" -> feed.lastUpdateError = reader.nextString()
                "error" -> parseError(reader, feed)
                else -> {
                    Log.w(TAG, "Unknown value in feed json: $name")
                    `in`.skipValue()
                }
            }
        }
        `in`.endObject()
        return feed
    }

    @Throws(IOException::class)
    private fun parseError(reader: NullableJsonReader, feed: Feed) {
        reader.`in`.beginObject()
        while (reader.`in`.hasNext()) {
            when (reader.`in`.nextName()) {
                "code" -> reader.`in`.skipValue()
                "message" -> feed.lastUpdateError = reader.nextString()
            }
        }
        reader.`in`.endObject()
    }

    companion object {
        private val TAG = FeedTypeAdapter::class.java.name
    }
}