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
import io.realm.kotlin.where
import java.util.*

class FreshFolder(val context: Context) : TreeItem {
    val name = context.getString(R.string.fresh_items)

    companion object {
        private const val MAX_ARTICLE_AGE = 24 * 60 * 60 * 1000
        const val ID: Long = -12
    }

    override fun treeItemId(): Long {
        return ID
    }

    override fun treeItemName(): String {
        return name
    }

    override fun getIcon(): Int {
        return R.drawable.fresh
    }

    override fun getCount(realm: Realm): Int {
        return realm.where<Item>()
                .equalTo(Item.UNREAD, true)
                .greaterThan(Item::pubDate.name, getDate())
                .count()
                .toInt()
    }

    override fun getFeeds(realm: Realm, onlyUnread: Boolean): List<Feed> {
        val freshFeeds = mutableListOf<Feed>()
        for(item: Item in getItems(realm, false)) {
            val feed = item.feed
            if(!freshFeeds.contains(feed) && feed != null)
                freshFeeds.add(feed)
        }
        return freshFeeds
    }

    override fun getItems(realm: Realm, onlyUnread: Boolean): List<Item> {
        return realm.where<Item>()
                .equalTo(Item.UNREAD, true)
                .greaterThan(Item::pubDate.name, getDate())
                .findAll()
    }

    private fun getDate() : Date {
        return Date(System.currentTimeMillis() - MAX_ARTICLE_AGE)
    }
}