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
import android.os.Parcelable
import email.schaal.ocreader.R
import io.realm.Realm
import io.realm.RealmModel
import io.realm.RealmObject
import io.realm.annotations.PrimaryKey
import io.realm.annotations.RealmClass
import io.realm.kotlin.createObject
import io.realm.kotlin.where
import kotlinx.android.parcel.Parcelize
import java.util.*

@Parcelize
@RealmClass
open class Feed(
        @PrimaryKey var id: Long = 0,
        var folderId: Long = 0,
        var folder: Folder? = null,
        var url: String = "",
        var name: String = "",
        var link: String = "",
        var faviconLink: String? = null,
        var added: Date = Date(),
        var unreadCount: Int = 0,
        var starredCount: Int = 0,
        var ordering: Int = 0,
        var pinned: Boolean = false,
        var updateErrorCount: Int = 0,
        var lastUpdateError: String? = null
) : RealmModel, TreeItem, Insertable, Parcelable {

    fun isConsideredFailed(): Boolean {
        return updateErrorCount >= 50
    }

    companion object {
        fun get(realm: Realm, feedId: Long) : Feed? {
            return realm.where<Feed>().equalTo(Feed::id.name, feedId).findFirst()
        }

        fun getOrCreate(realm: Realm, feedId: Long) : Feed? {
            return get(realm, feedId) ?: realm.createObject(feedId)
        }
    }

    override fun treeItemId(): Long {
        return id
    }

    override fun treeItemName(): String {
        return name
    }

    override fun getIcon(): Int {
        return R.drawable.ic_feed_icon
    }

    override fun getCount(realm: Realm): Int {
        return unreadCount
    }

    override fun getFeeds(realm: Realm, onlyUnread: Boolean): List<Feed> {
        return listOf(this)
    }

    override fun getItems(realm: Realm, onlyUnread: Boolean): List<Item> {
        val query = realm.where<Item>().equalTo(Item::feedId.name, id)
        if(onlyUnread)
            query.equalTo(Item.UNREAD, true)
        return query.findAll()
    }

    fun incrementUnreadCount(value: Int) {
        unreadCount += value
    }

    fun incrementStarredCount(value: Int) {
        starredCount += value
    }

    override fun insert(realm: Realm) {
        folder = Folder.getOrCreate(realm, folderId)
        realm.insertOrUpdate(this)
    }

    override fun delete(realm: Realm) {
        realm.where<Item>().equalTo(Item::feedId.name, id).findAll().deleteAllFromRealm()
        RealmObject.deleteFromRealm(this)
    }

    fun getFolderTitle(context: Context?): CharSequence? {
        return folder?.name ?: if(folderId == 0.toLong()) context?.getString(R.string.root_folder) else null
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Feed) return false

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }
}