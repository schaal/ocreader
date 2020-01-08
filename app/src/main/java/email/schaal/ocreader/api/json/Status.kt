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
import com.github.zafarkhaja.semver.UnexpectedCharacterException
import com.github.zafarkhaja.semver.Version
import email.schaal.ocreader.database.model.User

/**
 * Encapsulates the JSON response for the status api call
 */
class Status {
    var version: Version? = null
        private set
    var isImproperlyConfiguredCron = false
    /* Only returned by API v2 */
    var user: User? = null

    fun setVersion(version: String) {
        try {
            this.version = Version.valueOf(version)
        } catch (e: UnexpectedCharacterException) {
            this.version = null
            Log.e(TAG, "Failed to parse version: $version", e)
        }
    }

    companion object {
        private val TAG = Status::class.java.name
    }
}