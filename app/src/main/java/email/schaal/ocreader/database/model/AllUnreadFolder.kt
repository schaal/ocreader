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

class AllUnreadFolder(val context: Context) : TreeItem {
    val name: String = context.getString(R.string.unread_items)

    companion object {
        const val ID: Long = -10
    }

    override fun treeItemId(): Long {
        return ID
    }

    override fun treeItemName(): String {
        return name
    }

    override fun getIcon(): Int {
        return R.drawable.ic_feed_icon
    }

    override fun getCount(realm: Realm): Int {
        return realm.where<Feed>().sum(Feed::unreadCount.name).toInt()
    }

    override fun getFeeds(realm: Realm, onlyUnread: Boolean): List<Feed> {
        val query = realm.where<Feed>()
        if(onlyUnread)
            query.greaterThan(Feed::unreadCount.name, 0)
        return query.sort(Feed::name.name, Sort.ASCENDING).findAll()
    }

    override fun getItems(realm: Realm, onlyUnread: Boolean): List<Item> {
        val query = realm.where<Item>()
        if(onlyUnread)
            query.equalTo(Item::unread.name, true)
        return query.distinct(Item::fingerprint.name).findAll()
    }
}