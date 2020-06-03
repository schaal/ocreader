/*
 * Copyright © 2019. Daniel Schaal <daniel@schaal.email>
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
package email.schaal.ocreader.database

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.*
import androidx.preference.PreferenceManager
import email.schaal.ocreader.Preferences
import email.schaal.ocreader.database.model.*
import email.schaal.ocreader.database.model.TemporaryFeed.Companion.getListTemporaryFeed
import email.schaal.ocreader.putBreadCrumbs
import email.schaal.ocreader.service.SyncJobIntentService
import email.schaal.ocreader.service.SyncResultReceiver
import email.schaal.ocreader.service.SyncType
import io.realm.Realm
import io.realm.kotlin.where
import kotlin.IllegalArgumentException

class FeedViewModel(context: Context) : RealmViewModel() {
    private val temporaryFeedLiveData: MutableLiveData<TemporaryFeed>
    private val itemsLiveData: MutableLiveData<List<Item>>
    private val foldersLiveData: MutableLiveData<List<Folder>>
    private val selectedTreeItemLiveData: MutableLiveData<TreeItem>
    private val userLiveData: MutableLiveData<User>
    private val syncStatusLiveData: MutableLiveData<Boolean> = MutableLiveData(false)

    val topFolders: Array<TreeItem>

    val temporaryFeed: LiveData<TemporaryFeed>
        get() = temporaryFeedLiveData

    val items: LiveData<List<Item>>
        get() = itemsLiveData

    val folders: LiveData<List<Folder>>
        get() = foldersLiveData

    val syncStatus: LiveData<Boolean>
        get() = syncStatusLiveData

    val selectedTreeItem: LiveData<TreeItem>
        get() = selectedTreeItemLiveData

    val user: LiveData<User>
        get() = userLiveData

    fun sync(context: Context, syncType: SyncType, resultReceiver: SyncResultReceiver? = null) {
        SyncJobIntentService.enqueueWork(context, syncType, resultReceiver)
    }

    fun updateFolders(onlyUnread: Boolean) {
        foldersLiveData.value = Folder.getAll(realm, onlyUnread)
    }

    fun updateSelectedTreeItem(context: Context, treeItem: TreeItem?) {
        val newTreeItem = treeItem ?: topFolders[0]
        selectedTreeItemLiveData.value = newTreeItem
        PreferenceManager.getDefaultSharedPreferences(context).edit().putBreadCrumbs(listOf(newTreeItem.treeItemId() to (newTreeItem is Feed))).apply()
    }

    fun updateTemporaryFeed(preferences: SharedPreferences, updateTemporaryFeed: Boolean) {
        val temporaryFeed = temporaryFeedLiveData.value
        val selectedTreeItem = selectedTreeItemLiveData.value
        if (temporaryFeed == null || selectedTreeItem == null) return
        if (updateTemporaryFeed || temporaryFeed.treeItemId != selectedTreeItem.treeItemId()) {
            realm.executeTransaction { realm: Realm ->
                val tempItems = selectedTreeItem.getItems(realm, Preferences.SHOW_ONLY_UNREAD.getBoolean(preferences))
                temporaryFeed.treeItemId = selectedTreeItem.treeItemId()
                temporaryFeed.name = selectedTreeItem.treeItemName()
                temporaryFeed.items?.clear()
                temporaryFeed.items?.addAll(tempItems)
            }
        }
        itemsLiveData.value = temporaryFeed.items?.sort(Preferences.SORT_FIELD.getString(preferences), Preferences.ORDER.getOrder(preferences))
    }

    fun markTemporaryFeedAsRead() {
        realm.executeTransactionAsync(Realm.Transaction { realm1: Realm ->
            val unreadItems = getListTemporaryFeed(realm1)
                    ?.items
                    ?.where()
                    ?.equalTo(Item.UNREAD, true)
                    ?.findAll()
            if(unreadItems != null)
                for (item in unreadItems) {
                    item.unread = false
                }
        }, null, null)
    }

    fun markAboveAsRead(items: List<Item>?, lastItemId: Long) {
        if(items != null) {
            realm.executeTransaction {
                for (item in items) {
                    item.unread = false
                    if (item.id == lastItemId) {
                        break
                    }
                }
            }
        }
    }

    class FeedViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return FeedViewModel(context) as? T
                    ?: throw IllegalArgumentException("Unknown ViewModel class")
        }
    }

    companion object {
        fun getTreeItem(realm: Realm, breadcrumb: Pair<Long, Boolean>, staticFolders: Array<TreeItem>): TreeItem {
            val (treeItemId, isFeed) = breadcrumb

            for(treeItem in staticFolders)
                if(treeItem.treeItemId() == treeItemId)
                    return treeItem

            return if(isFeed)
                realm.where<Feed>().equalTo(Feed::id.name, treeItemId).findFirst() ?: staticFolders[0]
            else
                realm.where<Folder>().equalTo(Folder::id.name, treeItemId).findFirst() ?: staticFolders[0]
        }
    }

    init {
        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        val temporaryFeed = getListTemporaryFeed(realm)!!
        temporaryFeedLiveData = LiveRealmObject(temporaryFeed)
        itemsLiveData = LiveRealmResults<Item>(temporaryFeed.items?.sort(Preferences.SORT_FIELD.getString(preferences) ?: Item::pubDate.name, Preferences.ORDER.getOrder(preferences))!!)
        foldersLiveData = LiveRealmResults(Folder.getAll(realm, Preferences.SHOW_ONLY_UNREAD.getBoolean(preferences)))
        topFolders = arrayOf(
                AllUnreadFolder(context),
                StarredFolder(context),
                FreshFolder(context)
        )
        selectedTreeItemLiveData = MutableLiveData(getTreeItem(realm, Preferences.BREADCRUMBS.getBreadCrumbs(preferences).last(), topFolders))
        userLiveData = LiveRealmObject(realm.where<User>().findFirst())
        updateTemporaryFeed(preferences, false)
    }
}