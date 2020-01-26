/*
 * Copyright (C) 2015-2016 Daniel Schaal <daniel@schaal.email>
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
package email.schaal.ocreader.database

import android.content.Context
import android.util.Log
import email.schaal.ocreader.database.model.Insertable
import email.schaal.ocreader.database.model.Item
import email.schaal.ocreader.database.model.TemporaryFeed
import email.schaal.ocreader.database.model.TemporaryFeed.Companion.getListTemporaryFeed
import io.realm.Realm
import io.realm.Realm.Transaction.OnSuccess
import io.realm.RealmConfiguration
import io.realm.RealmMigration
import io.realm.kotlin.where

/**
 * Utility class containing some commonly used Queries for the Realm database.
 */
object Queries {
    private val TAG = Queries::class.java.name
    const val SCHEMA_VERSION = 12
    private val initialData = Realm.Transaction { realm: Realm ->
        realm.deleteAll()
        realm.createObject(TemporaryFeed::class.java, TemporaryFeed.LIST_ID)
        realm.createObject(TemporaryFeed::class.java, TemporaryFeed.PAGER_ID)
    }
    private val migration: RealmMigration = DatabaseMigration()
    const val MAX_ITEMS = 10000

    fun init(context: Context) {
        Realm.init(context)
        val realmConfiguration = RealmConfiguration.Builder()
                .schemaVersion(SCHEMA_VERSION.toLong())
                .migration(migration)
                .initialData(initialData)
                .compactOnLaunch()
                .build()
        Realm.setDefaultConfiguration(realmConfiguration)
        var realm: Realm? = null
        try {
            realm = Realm.getDefaultInstance()
            if (realm.isEmpty) realm.executeTransaction(initialData)
        } catch (ex: Exception) {
            Log.e(TAG, "Failed to open realm db", ex)
            realm?.close()
            Realm.deleteRealm(realmConfiguration)
        } finally {
            realm?.close()
        }
    }

    fun resetDatabase() {
        Log.w(TAG, "Database will be reset")
        var realm: Realm? = null
        try {
            realm = Realm.getDefaultInstance()
            realm.executeTransaction(initialData)
        } finally {
            realm?.close()
        }
    }

    fun insert(realm: Realm, element: Insertable?) {
        element?.insert(realm)
    }

    fun insert(realm: Realm, elements: Iterable<Insertable>) {
        for (element in elements) {
            element.insert(realm)
        }
    }

    fun markAboveAsRead(realm: Realm, items: List<Item>?, lastItemId: Long) {
        if(items != null) {
            realm.executeTransaction { realm1: Realm ->
                try {
                    for (item in items) {
                        item.unread = false
                        if (item.id == lastItemId) {
                            break
                        }
                    }
                } finally {
                    checkAlarm(realm1)
                }
            }
        }
    }

    fun markTemporaryFeedAsRead(realm: Realm, onSuccess: OnSuccess?, onError: Realm.Transaction.OnError?) {
        realm.executeTransactionAsync(Realm.Transaction { realm1: Realm ->
            try {
                val unreadItems = getListTemporaryFeed(realm1)
                        ?.items
                        ?.where()
                        ?.equalTo(Item.UNREAD, true)
                        ?.findAll()
                if(unreadItems != null)
                    for (item in unreadItems) {
                        item.unread = false
                    }
            } finally {
                checkAlarm(realm1)
            }
        }, onSuccess, onError)
    }

    @Synchronized
    private fun checkAlarm(realm: Realm) {
        val changedItemsCount = realm.where<Item>()
                .equalTo(Item.UNREAD_CHANGED, true)
                .or()
                .equalTo(Item.STARRED_CHANGED, true).count()
        if (changedItemsCount > 0) {
            TODO("Add job")
        } else {
            TODO("cancel job")
        }
    }
}