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
import email.schaal.ocreader.database.model.Folder

/**
 * TypeAdapter to deserialize the JSON response for Folders.
 */
class FolderTypeAdapter : JsonAdapter<Folder?>() {
    override fun toJson(out: JsonWriter, value: Folder?) {
    }

    override fun fromJson(inReader: JsonReader): Folder? {
        if (inReader.peek() == JsonReader.Token.NULL) {
            inReader.nextNull<Any>()
            return null
        }
        val folder = Folder()
        inReader.beginObject()
        while (inReader.hasNext()) {
            when (val name = inReader.nextName()) {
                "id" -> folder.id = inReader.nextLong()
                "name" -> folder.name = inReader.nextString()
                else -> {
                    Log.w(TAG, "Unknown value in folder json: $name")
                    inReader.skipValue()
                }
            }
        }
        inReader.endObject()
        return folder
    }

    companion object {
        private val TAG = FolderTypeAdapter::class.java.name
    }
}