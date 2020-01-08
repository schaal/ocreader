/*
 * Copyright (C) 2016 Daniel Schaal <daniel@schaal.email>
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
import java.io.IOException

/**
 * TypeAdapter to deserialize the JSON response for the status api call.
 */
class StatusTypeAdapter : JsonAdapter<Status?>() {
    @Throws(IOException::class)
    override fun toJson(out: JsonWriter, value: Status?) {
    }

    @Throws(IOException::class)
    override fun fromJson(`in`: JsonReader): Status? {
        if (`in`.peek() == JsonReader.Token.NULL) {
            `in`.nextNull<Any>()
            return null
        }
        val reader = NullableJsonReader(`in`)
        val status = Status()
        `in`.beginObject()
        while (`in`.hasNext()) {
            when (val name = `in`.nextName()) {
                "version" -> status.setVersion(reader.nextString()!!)
                "warnings", "issues" ->  // this is called warnings in api v1-2, issues in api v2
                    readWarnings(`in`, status)
                "user" -> status.user = UserTypeAdapter().fromJson(`in`)
                else -> {
                    Log.w(TAG, "Unknown value in status json: $name")
                    `in`.skipValue()
                }
            }
        }
        `in`.endObject()
        return status
    }

    @Throws(IOException::class)
    private fun readWarnings(`in`: JsonReader, status: Status) {
        `in`.beginObject()
        while (`in`.hasNext()) {
            val name = `in`.nextName()
            when (name) {
                "improperlyConfiguredCron" -> status.isImproperlyConfiguredCron = `in`.nextBoolean()
                "incorrectDbCharset" ->  // TODO: 11/9/17 Show warning
                    `in`.skipValue()
                else -> {
                    Log.w(TAG, "Unknown value in status warnings json: $name")
                    `in`.skipValue()
                }
            }
        }
        `in`.endObject()
    }

    companion object {
        private val TAG = StatusTypeAdapter::class.java.name
    }
}