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

import com.squareup.moshi.JsonClass
import email.schaal.ocreader.database.model.Feed
import email.schaal.ocreader.database.model.Folder
import email.schaal.ocreader.database.model.Item

/**
 * Class to deserialize the json response for feeds
 */
@JsonClass(generateAdapter = true)
class Feeds (
        val feeds: List<Feed>,
        val starredCount: Int = 0,
        val newestItemId: Long? = null
)

/**
 * Class to deserialize the json response for folders
 */
@JsonClass(generateAdapter = true)
class Folders(val folders: List<Folder>)

/**
 * Class to deserialize the json response for items
 */
@JsonClass(generateAdapter = true)
class Items(val items: List<Item>)