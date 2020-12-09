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

import android.os.Parcelable
import com.squareup.moshi.JsonClass
import email.schaal.ocreader.R
import io.realm.*
import io.realm.annotations.PrimaryKey
import io.realm.annotations.RealmClass
import io.realm.kotlin.createObject
import io.realm.kotlin.where
import kotlinx.parcelize.Parcelize

@Parcelize
@RealmClass
@JsonClass(generateAdapter = true)
open class Folder(
        @PrimaryKey var id: Long = 0,
        var name: String = ""
) : RealmModel, TreeItem, Insertable, Parcelable {

    companion object {
        fun get(realm: Realm, folderId: Long) : Folder? {
            return realm.where<Folder>().equalTo(Folder::id.name, folderId).findFirst()
        }

        fun getOrCreate(realm: Realm, folderId: Long): Folder? {
            if(folderId == 0L)
                return null
            return get(realm ,folderId) ?: realm.createObject(folderId)
        }

        fun getAll(realm: Realm, onlyUnread: Boolean) : RealmResults<Folder> {
            val query = realm.where<Folder>()
            if(onlyUnread) {
                val unreadFeeds = realm.where<Feed>().greaterThan(Feed::unreadCount.name, 0).findAll()
                if(unreadFeeds.isNotEmpty()) {
                    val feedIterator = unreadFeeds.iterator()
                    query.equalTo(Folder::id.name, feedIterator.next().folderId)
                    while(feedIterator.hasNext())
                        query.or().equalTo(Folder::id.name, feedIterator.next().folderId)
                } else {
                    query.alwaysFalse()
                }
            }
            return query.sort(Folder::name.name, Sort.ASCENDING).findAll()
        }
    }

    override fun treeItemId(): Long {
        return id
    }

    override fun treeItemName(): String {
        return name
    }

    override fun getCount(realm: Realm): Int {
        return realm.where<Feed>()
                .equalTo(Feed::folderId.name, id)
                .sum(Feed::unreadCount.name)
                .toInt()
    }

    override fun getIcon(): Int {
        return R.drawable.ic_folder
    }

    override fun getFeeds(realm: Realm, onlyUnread: Boolean): List<Feed> {
        val query = realm.where<Feed>().equalTo(Feed::folderId.name, id)
        if(onlyUnread)
            query.greaterThan(Feed::unreadCount.name, 0)
        return query.sort(Feed::name.name, Sort.ASCENDING).findAll()
    }

    override fun getItems(realm: Realm, onlyUnread: Boolean): List<Item> {
        val feeds = getFeeds(realm, onlyUnread)
        val query = realm.where<Item>()
        if(feeds.isNotEmpty()) {
            val iterator: Iterator<Feed> = feeds.iterator()
            query.beginGroup().equalTo(Item::feedId.name, iterator.next().id)
            while(iterator.hasNext())
                query.or().equalTo(Item::feedId.name, iterator.next().id)
            query.endGroup()
            if(onlyUnread)
                query.equalTo(Item.UNREAD, true)
        }
        return query.findAll()
    }

    override fun insert(realm: Realm) {
        if(name != null)
            realm.insertOrUpdate(this)
    }

    override fun delete(realm: Realm) {
        realm.where<Feed>().equalTo(Feed::folderId.name, id).findAll().deleteAllFromRealm()
        RealmObject.deleteFromRealm(this)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Folder) return false

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }
}