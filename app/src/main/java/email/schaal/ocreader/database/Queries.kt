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
import io.realm.kotlin.createObject
import io.realm.kotlin.where

/**
 * Utility class containing some commonly used Queries for the Realm database.
 */
object Queries {
    private val TAG = Queries::class.java.name
    const val SCHEMA_VERSION = 12L
    private val initialData = Realm.Transaction { realm: Realm ->
        realm.deleteAll()
        realm.createObject<TemporaryFeed>(TemporaryFeed.LIST_ID)
        realm.createObject<TemporaryFeed>(TemporaryFeed.PAGER_ID)
    }

    fun init(context: Context) {
        Realm.init(context)
        val realmConfiguration = RealmConfiguration.Builder()
                .schemaVersion(SCHEMA_VERSION)
                .deleteRealmIfMigrationNeeded()
                .initialData(initialData)
                .compactOnLaunch()
                .build()
        Realm.setDefaultConfiguration(realmConfiguration)

        try {
            Realm.getDefaultInstance().use {
                if(it.isEmpty)
                    it.executeTransaction(initialData)
            }
        } catch(e: Exception) {
            Log.e(TAG, "Failed to open realm db", e)
        }
    }

    fun resetDatabase() {
        Log.w(TAG, "Database will be reset")
        Realm.getDefaultInstance().use {
            it.executeTransaction(initialData)
        }
    }

}