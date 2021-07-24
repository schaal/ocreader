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

package email.schaal.ocreader.database

import android.util.Log
import androidx.lifecycle.ViewModel
import email.schaal.ocreader.database.model.Item
import email.schaal.ocreader.database.model.Item.Companion.UNREAD
import io.realm.Realm
import io.realm.exceptions.RealmException
import io.realm.kotlin.where

open class RealmViewModel : ViewModel() {
    protected val realm: Realm by lazy { Realm.getDefaultInstance() }

    override fun onCleared() {
        Log.d(TAG, "onCleared called in ${this::class.simpleName}")
        realm.close()
        super.onCleared()
    }

    fun setItemUnread(unread: Boolean, vararg items: Item?) {
        realm.executeTransaction {
            try {
                for (item in items.filterNotNull()) { /* If the item has a fingerprint, mark all items with the same fingerprint
                          as read
                         */
                    if (item.fingerprint == null) {
                        item.unread = unread
                    } else {
                        val sameItems = it.where<Item>()
                                .equalTo(Item::fingerprint.name, item.fingerprint)
                                .equalTo(UNREAD, !unread)
                                .findAll()
                        for (sameItem in sameItems) {
                            sameItem.unread = unread
                        }
                    }
                }
            } catch (e: RealmException) {
                Log.e(TAG, "Failed to set item as unread", e)
            }
        }
    }

    fun setItemStarred(starred: Boolean, vararg items: Item?) {
        realm.executeTransaction {
            try {
                for (item in items.filterNotNull()) {
                    item.starred = starred
                }
            } catch (e: RealmException) {
                Log.e(TAG, "Failed to set item as starred", e)
            }
        }
    }

    companion object {
        const val TAG = "RealmViewModel"
    }
}