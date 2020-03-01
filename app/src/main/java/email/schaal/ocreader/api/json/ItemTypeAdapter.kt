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
        out.name(Item::id.name).value(item!!.id)
        out.name(Item::contentHash.name).value(item.contentHash)
        if (item.unreadChanged) out.name("isUnread").value(item.unread)
        if (item.starredChanged) out.name("isStarred").value(item.starred)
        out.endObject()
    }

    @Throws(IOException::class)
    override fun fromJson(jsonReader: JsonReader): Item? {
        if (jsonReader.peek() == JsonReader.Token.NULL) {
            jsonReader.nextNull<Any>()
            return null
        }
        val reader = NullableJsonReader(jsonReader)
        val builder = Item.Builder()
        jsonReader.beginObject()
        while (jsonReader.hasNext()) {
            when (val name = jsonReader.nextName()) {
                "id" -> builder.id = (jsonReader.nextLong())
                "guid" -> builder.guid = (jsonReader.nextString())
                "guidHash" -> builder.guidHash = (jsonReader.nextString())
                "url" -> builder.url = (reader.nextString())
                "title" -> builder.title = (cleanString(jsonReader.nextString()))
                "author" -> builder.author = (jsonReader.nextString().ifBlank { null })
                "pubDate" -> builder.pubDate = (Date(jsonReader.nextLong() * 1000))
                "body" -> builder.body = (jsonReader.nextString())
                "enclosureMime" -> if (jsonReader.peek() != JsonReader.Token.NULL) builder.enclosureMime = (jsonReader.nextString().ifBlank { null }) else jsonReader.skipValue()
                "enclosureLink" -> if (jsonReader.peek() != JsonReader.Token.NULL) builder.enclosureLink = (jsonReader.nextString().ifBlank { null }) else jsonReader.skipValue()
                "publishedAt" -> builder.pubDate = (parseDate(jsonReader.nextString()))
                "updatedAt" -> builder.updatedAt = (parseDate(jsonReader.nextString()))
                "enclosure" -> parseEnclosure(reader, builder)
                "feedId" -> builder.feedId = (jsonReader.nextLong())
                "isUnread", "unread" -> builder.unread = (jsonReader.nextBoolean())
                "starred", "isStarred" -> builder.starred = (jsonReader.nextBoolean())
                "lastModified" -> builder.lastModified = (jsonReader.nextLong())
                "rtl" -> jsonReader.skipValue()
                "fingerprint" -> builder.fingerprint = (reader.nextString())
                "contentHash" ->  // ignore for now, old items don't have this set yet.
//item.setContentHash(in.nextString());
                    jsonReader.skipValue()
                "updatedDate" -> if (jsonReader.peek() == JsonReader.Token.NUMBER) builder.updatedAt = (Date(jsonReader.nextLong() * 1000)) else jsonReader.skipValue()
                else -> {
                    Log.w(TAG, "Unknown value in item json: $name")
                    jsonReader.skipValue()
                }
            }
        }
        jsonReader.endObject()
        return builder.build()
    }

    @Throws(IOException::class)
    private fun parseEnclosure(reader: NullableJsonReader, builder: Item.Builder) {
        reader.`in`.beginObject()
        while (reader.`in`.hasNext()) {
            when (reader.`in`.nextName()) {
                "mimeType" -> builder.enclosureMime = (reader.nextString())
                "url" -> builder.enclosureLink = (reader.nextString())
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