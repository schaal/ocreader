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
import email.schaal.ocreader.database.model.Item
import email.schaal.ocreader.util.cleanString
import email.schaal.ocreader.util.emptyToNull
import java.io.IOException
import java.text.DateFormat
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

/**
 * TypeAdapter to deserialize the JSON response for feed Items.
 */
class ItemTypeAdapter : JsonAdapter<Item?>() {
    @Throws(IOException::class)
    override fun toJson(out: JsonWriter, item: Item?) {
        out.beginObject()
        out.name(Item.ID).value(item!!.id)
        out.name(Item.CONTENT_HASH).value(item.contentHash)
        if (item.isUnreadChanged) out.name("isUnread").value(item.isUnread)
        if (item.isStarredChanged) out.name("isStarred").value(item.isStarred)
        out.endObject()
    }

    @Throws(IOException::class)
    override fun fromJson(`in`: JsonReader): Item? {
        if (`in`.peek() == JsonReader.Token.NULL) {
            `in`.nextNull<Any>()
            return null
        }
        val reader = NullableJsonReader(`in`)
        val builder = Item.Builder()
        `in`.beginObject()
        while (`in`.hasNext()) {
            when (val name = `in`.nextName()) {
                "id" -> builder.setId(`in`.nextLong())
                "guid" -> builder.setGuid(`in`.nextString())
                "guidHash" -> builder.setGuidHash(`in`.nextString())
                "url" -> builder.setUrl(reader.nextString())
                "title" -> builder.setTitle(cleanString(`in`.nextString()))
                "author" -> builder.setAuthor(emptyToNull(`in`.nextString()))
                "pubDate" -> builder.setPubDate(Date(`in`.nextLong() * 1000))
                "body" -> builder.setBody(`in`.nextString())
                "enclosureMime" -> if (`in`.peek() != JsonReader.Token.NULL) builder.setEnclosureMime(emptyToNull(`in`.nextString())) else `in`.skipValue()
                "enclosureLink" -> if (`in`.peek() != JsonReader.Token.NULL) builder.setEnclosureLink(emptyToNull(`in`.nextString())) else `in`.skipValue()
                "publishedAt" -> builder.setPubDate(parseDate(`in`.nextString()))
                "updatedAt" -> builder.setUpdatedAt(parseDate(`in`.nextString()))
                "enclosure" -> parseEnclosure(reader, builder)
                "feedId" -> builder.setFeedId(`in`.nextLong())
                "isUnread", "unread" -> builder.setUnread(`in`.nextBoolean())
                "starred", "isStarred" -> builder.setStarred(`in`.nextBoolean())
                "lastModified" -> builder.setLastModified(`in`.nextLong())
                "rtl" -> `in`.skipValue()
                "fingerprint" -> builder.setFingerprint(reader.nextString())
                "contentHash" ->  // ignore for now, old items don't have this set yet.
//item.setContentHash(in.nextString());
                    `in`.skipValue()
                "updatedDate" -> if (`in`.peek() == JsonReader.Token.NUMBER) builder.setUpdatedAt(Date(`in`.nextLong() * 1000)) else `in`.skipValue()
                else -> {
                    Log.w(TAG, "Unknown value in item json: $name")
                    `in`.skipValue()
                }
            }
        }
        `in`.endObject()
        return builder.build()
    }

    @Throws(IOException::class)
    private fun parseEnclosure(reader: NullableJsonReader, builder: Item.Builder) {
        reader.`in`.beginObject()
        while (reader.`in`.hasNext()) {
            when (reader.`in`.nextName()) {
                "mimeType" -> builder.setEnclosureMime(reader.nextString())
                "url" -> builder.setEnclosureLink(reader.nextString())
            }
        }
        reader.`in`.endObject()
    }

    private fun parseDate(source: String): Date? {
        return try {
            iso8601Format.parse(source)
        } catch (e: ParseException) {
            Log.e(TAG, "Failed to parse date: $source", e)
            null
        }
    }

    companion object {
        private val TAG = ItemTypeAdapter::class.java.name
        private val iso8601Format: DateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZZ", Locale.US)
    }
}