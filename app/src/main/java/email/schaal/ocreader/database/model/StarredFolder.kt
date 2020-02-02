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

package email.schaal.ocreader.database.model

import android.content.Context
import email.schaal.ocreader.R
import io.realm.Realm
import io.realm.Sort
import io.realm.kotlin.where

class StarredFolder(val context: Context): TreeItem {
    val name = context.getString(R.string.starred_items)

    companion object {
        const val ID: Long = -11
    }

    override fun treeItemId(): Long {
        return ID
    }

    override fun treeItemName(): String {
        return name
    }

    override fun getIcon(): Int {
        return R.drawable.ic_star_outline
    }

    override fun getCount(realm: Realm): Int {
        return realm.where<Item>().equalTo(Item.STARRED, true).count().toInt()
    }

    override fun getFeeds(realm: Realm, onlyUnread: Boolean): List<Feed> {
        return realm.where<Feed>().greaterThan(Feed::starredCount.name, 0).sort(Feed::name.name, Sort.ASCENDING).findAll()
    }

    override fun getItems(realm: Realm, onlyUnread: Boolean): List<Item> {
        return realm.where<Item>().equalTo(Item.STARRED, true).findAll()
    }
}