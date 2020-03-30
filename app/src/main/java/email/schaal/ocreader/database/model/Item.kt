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
import android.content.Intent
import android.net.Uri
import android.os.Parcelable
import io.realm.Realm
import io.realm.RealmModel
import io.realm.RealmObject
import io.realm.Sort
import io.realm.annotations.PrimaryKey
import io.realm.annotations.RealmClass
import io.realm.annotations.RealmField
import io.realm.kotlin.isManaged
import io.realm.kotlin.where
import kotlinx.android.parcel.Parcelize
import java.util.*

@Parcelize
@RealmClass
open class Item(
        @PrimaryKey var id: Long = 0,
        var guid: String? = null,
        var guidHash: String? = null,
        var url: String? = null,
        var title: String? = null,
        var author: String? = null,
        var pubDate: Date? = null,
        var updatedAt: Date? = null,
        var body: String = "",
        var enclosureMime: String? = null,
        var enclosureLink: String? = null,
        var feed: Feed? = null,
        var feedId: Long = 0,
        @RealmField(name = "unread")
        private var actualUnread: Boolean = true,
        var unreadChanged: Boolean = false,
        @RealmField(name = "starred")
        private var actualStarred: Boolean = false,
        var starredChanged: Boolean = false,
        var lastModified: Long = 0,
        var fingerprint: String? = null,
        var contentHash: String? = null,
        var active: Boolean = true
) : RealmModel, Insertable, Parcelable {
    companion object {
        const val UNREAD = "actualUnread"
        const val STARRED = "actualStarred"

        fun removeExcessItems(realm: Realm, maxItems: Int) {
            val itemCount = realm.where<Item>().count()
            if (itemCount > maxItems) {
                realm.where<Item>()
                        .equalTo(UNREAD, false)
                        .equalTo(STARRED, false)
                        .equalTo(Item::active.name, false)
                        .sort(Item::lastModified.name, Sort.ASCENDING)
                        .limit(itemCount - maxItems)
                        .findAll()
                        .deleteAllFromRealm()
            }
        }

    }

    class Builder {
        var id: Long = 0
        var guid: String? = null
        var guidHash: String? = null
        var url: String? = null
        var title: String? = null
        var author: String? = null
        var pubDate: Date? = null
        var updatedAt: Date? = null
        var body: String = ""
        var enclosureMime: String? = null
        var enclosureLink: String? = null
        var feed: Feed? = null
        var feedId: Long = 0
        var unread: Boolean = true
        var unreadChanged: Boolean = false
        var starred: Boolean = false
        var starredChanged: Boolean = false
        var lastModified: Long = 0
        var fingerprint: String? = null
        var contentHash: String? = null
        var active: Boolean = true

        fun build() : Item {
            return Item(this)
        }
    }

    constructor(builder: Builder) :this(
            builder.id,
            builder.guid,
            builder.guidHash,
            builder.url,
            builder.title,
            builder.author,
            builder.pubDate,
            builder.updatedAt,
            builder.body,
            builder.enclosureMime,
            builder.enclosureLink,
            builder.feed,
            builder.feedId,
            builder.unread,
            builder.unreadChanged,
            builder.starred,
            builder.starredChanged,
            builder.lastModified,
            builder.fingerprint,
            builder.contentHash,
            builder.active
    )

    var unread: Boolean
        get() { return actualUnread }
        set(value) {
            if(isManaged() && actualUnread != value) {
                unreadChanged = !unreadChanged
                feed?.incrementUnreadCount(if(value) 1 else -1)
            }
            actualUnread = value
        }

    var starred: Boolean
        get() { return actualStarred }
        set(value) {
            if(isManaged() && actualStarred != value) {
                starredChanged = !starredChanged
                feed?.incrementStarredCount(if(value) 1 else -1)
            }
            actualStarred = value
        }

    override fun insert(realm: Realm) {
        if(title == null) {
            val fullItem = realm.where<Item>().equalTo(Item::contentHash.name, contentHash).findFirst()
            fullItem?.unread = unread
            fullItem?.starred = starred
        } else {
            feed = Feed.getOrCreate(realm, feedId)
        }
        realm.insertOrUpdate(this)
    }

    override fun delete(realm: Realm) {
        RealmObject.deleteFromRealm(this)
    }

    fun play(context: Context) {
        if(enclosureLink != null) {
            val playIntent = Intent(Intent.ACTION_VIEW)
            playIntent.data = Uri.parse(enclosureLink)
            context.startActivity(playIntent)
        }
    }
}