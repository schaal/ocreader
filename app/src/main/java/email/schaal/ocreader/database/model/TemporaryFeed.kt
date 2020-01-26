/*
 * Copyright (C) 2015 Daniel Schaal <daniel@schaal.email>
 *
 * This file is part of OCReader.
 *
 * OCReader is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * OCReader is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with OCReader.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package email.schaal.ocreader.database.model

import io.realm.Realm
import io.realm.RealmList
import io.realm.RealmModel
import io.realm.RealmObject
import io.realm.annotations.PrimaryKey
import io.realm.annotations.RealmClass
import io.realm.kotlin.where

/**
 * TemporaryFeed allows to store the currently displayed Items.
 */
@RealmClass
open class TemporaryFeed() : RealmModel {
    @PrimaryKey
    var id: Long = 0
    var treeItemId: Long = 0
    var name: String = ""
    var items: RealmList<Item>? = null

    constructor(id: Long) : this() {
        this.id = id
    }

    companion object {
        const val LIST_ID = 0
        const val PAGER_ID = 1

        fun getListTemporaryFeed(realm: Realm): TemporaryFeed? {
            return realm.where<TemporaryFeed>().equalTo(TemporaryFeed::id.name, LIST_ID).findFirst()
        }

        fun getPagerTemporaryFeed(realm: Realm): TemporaryFeed? {
            return realm.where<TemporaryFeed>().equalTo(TemporaryFeed::id.name, PAGER_ID).findFirst()
        }

        fun updatePagerTemporaryFeed(realm: Realm) {
            realm.executeTransaction { realm1: Realm ->
                val listTempFeed = getListTemporaryFeed(realm1)
                val pagerTempFeed = getPagerTemporaryFeed(realm1)
                for (item in realm1.where<Item>().equalTo(Item::active.name, true).findAll()) {
                    item.active = false
                }
                val listTempFeedItems = listTempFeed?.items
                if (listTempFeedItems != null) {
                    for (item in listTempFeedItems) {
                        item.active = true
                    }
                }
                pagerTempFeed?.items = listTempFeedItems
                pagerTempFeed?.name = listTempFeed?.name ?: ""
                pagerTempFeed?.treeItemId = listTempFeed?.treeItemId ?: 0
            }
        }
    }
}