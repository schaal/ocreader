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

import com.github.zafarkhaja.semver.Version
import com.squareup.moshi.JsonClass

/**
 * Encapsulates the JSON response for the status api call
 */
@JsonClass(generateAdapter = true)
class Status(val version: Version,
     val warnings: Map<String, Boolean>? = null) {

    val isImproperlyConfiguredCron: Boolean
        get() = warnings?.get("isImproperlyConfiguredCron") ?: false
}