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

import com.github.zafarkhaja.semver.Version
import com.squareup.moshi.FromJson
import com.squareup.moshi.ToJson
import java.util.*

class VersionTypeAdapter {
    @FromJson
    fun fromJson(versionString: String?): Version? {
        return Version.valueOf(versionString)
    }

    @ToJson
    fun toJson(version: Version): String {
        return version.toString()
    }
}

class DateTypeAdapter {
    @FromJson
    fun fromJson(timestamp: Long) : Date {
        return Date(timestamp * 1000)
    }

    @ToJson
    fun toJson(date: Date): Long {
        return date.time / 1000
    }
}