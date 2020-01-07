/*
 * Copyright © 2017. Daniel Schaal <daniel@schaal.email>
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
package email.schaal.ocreader.api

import android.content.Context

/**
 * Created by daniel on 26.05.17.
 */
enum class Level(val level: String, val isSupported: Boolean) {
    V2("v2", false),
    V12("v1-2", true);

    companion object {
        fun getAPI(context: Context, level: Level): API {
            return when (level) {
                V12 -> APIv12(context)
                V2 -> APIv2(context)
            }
        }

        operator fun get(level: String): Level? {
            for (supportedLevel in values()) {
                if (supportedLevel.level == level) return supportedLevel
            }
            return null
        }
    }

}