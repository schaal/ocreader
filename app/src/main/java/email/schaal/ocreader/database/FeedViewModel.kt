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
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.preference.PreferenceManager
import email.schaal.ocreader.Preferences
import email.schaal.ocreader.database.model.*
import io.realm.Realm

class FeedViewModel(context: Context?) : ViewModel() {
    private val realm: Realm = Realm.getDefaultInstance()
    private val temporaryFeedLiveData: MutableLiveData<TemporaryFeed>
    private val itemsLiveData: MutableLiveData<List<Item>>
    private val foldersLiveData: MutableLiveData<List<Folder>>
    private val selectedTreeItemLiveData: MutableLiveData<TreeItem?>
    private val topFolderMap: MutableMap<Long, TreeItem>
    val temporaryFeed: LiveData<TemporaryFeed>
        get() = temporaryFeedLiveData

    val items: LiveData<List<Item>>
        get() = itemsLiveData

    val folders: LiveData<List<Folder>>
        get() = foldersLiveData

    val selectedTreeItem: LiveData<TreeItem?>
        get() = selectedTreeItemLiveData

    fun getTopTreeItem(id: Long): TreeItem? {
        return topFolderMap[id]
    }

    val topFolderList: Collection<TreeItem>
        get() = topFolderMap.values

    override fun onCleared() {
        realm.close()
        super.onCleared()
    }

    fun updateFolders(onlyUnread: Boolean) {
        foldersLiveData.value = Folder.getAll(realm, onlyUnread)
    }

    fun updateSelectedTreeItem(treeItem: TreeItem?) {
        selectedTreeItemLiveData.value = treeItem
    }

    fun updateTemporaryFeed(preferences: SharedPreferences, updateTemporaryFeed: Boolean) {
        val temporaryFeed = temporaryFeedLiveData.value
        val selectedTreeItem = selectedTreeItemLiveData.value
        if (temporaryFeed == null || selectedTreeItem == null) return
        if (updateTemporaryFeed || temporaryFeed.treeItemId != selectedTreeItem.id) {
            realm.executeTransaction { realm: Realm? ->
                val tempItems = selectedTreeItem.getItems(realm, Preferences.SHOW_ONLY_UNREAD.getBoolean(preferences))
                temporaryFeed.treeItemId = selectedTreeItem.id
                temporaryFeed.name = selectedTreeItem.name
                temporaryFeed.items.clear()
                if (tempItems != null) {
                    temporaryFeed.items.addAll(tempItems)
                }
            }
        }
        itemsLiveData.value = temporaryFeed.items.sort(Preferences.SORT_FIELD.getString(preferences), Preferences.ORDER.getOrder(preferences))
    }

    class FeedViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return FeedViewModel(context) as T
        }
    }

    init {
        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        val temporaryFeed = TemporaryFeed.getListTemporaryFeed(realm)
        temporaryFeedLiveData = LiveRealmObject(temporaryFeed)
        itemsLiveData = LiveRealmResults(temporaryFeed.items.sort(Preferences.SORT_FIELD.getString(preferences), Preferences.ORDER.getOrder(preferences)))
        foldersLiveData = LiveRealmResults(Folder.getAll(realm, Preferences.SHOW_ONLY_UNREAD.getBoolean(preferences)))
        topFolderMap = HashMap(3)
        topFolderMap[AllUnreadFolder.ID] = AllUnreadFolder(context)
        topFolderMap[StarredFolder.ID] = StarredFolder(context)
        topFolderMap[FreshFolder.ID] = FreshFolder(context)
        selectedTreeItemLiveData = MutableLiveData(topFolderMap[AllUnreadFolder.ID])
    }
}