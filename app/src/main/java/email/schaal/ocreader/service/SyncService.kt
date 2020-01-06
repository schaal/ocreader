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
package email.schaal.ocreader.service

import android.app.Activity
import android.app.Service
import android.content.Intent
import android.content.IntentFilter
import android.os.IBinder
import androidx.preference.PreferenceManager
import android.util.Log
import android.widget.Toast
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import email.schaal.ocreader.Preferences
import email.schaal.ocreader.api.API
import email.schaal.ocreader.api.API.APICallback
import email.schaal.ocreader.api.API.InstanceReadyCallback
import email.schaal.ocreader.database.Queries
import email.schaal.ocreader.database.model.Feed
import email.schaal.ocreader.database.model.Item
import io.realm.Realm

class SyncService : Service() {
    companion object {
        private val TAG = SyncService::class.java.name
        const val SYNC_FINISHED = "email.schaal.ocreader.action.SYNC_FINISHED"
        const val SYNC_STARTED = "email.schaal.ocreader.action.SYNC_STARTED"
        const val ACTION_SYNC = "email.schaal.ocreader.action.SYNC"
        const val EXTRA_ID = "email.schaal.ocreader.action.extra.ID"
        const val EXTRA_IS_FEED = "email.schaal.ocreader.action.extra.IS_FEED"
        const val EXTRA_OFFSET = "email.schaal.ocreader.action.extra.OFFSET"
        const val EXTRA_TYPE = "email.schaal.ocreader.action.extra.TYPE"
        const val EXTRA_INITIAL_SYNC = "email.schaal.ocreader.action.extra.INITIAL_SYNC"
        val syncFilter: IntentFilter = IntentFilter()

        @JvmOverloads
        fun startSync(activity: Activity, initialSync: Boolean = false) {
            val syncIntent = Intent(ACTION_SYNC, null, activity, SyncService::class.java)
            syncIntent.putExtra(EXTRA_TYPE, SyncType.FULL_SYNC.action)
            syncIntent.putExtra(EXTRA_INITIAL_SYNC, initialSync)
            activity.startService(syncIntent)
        }

        fun startLoadMore(activity: Activity, id: Long, offset: Long, isFeed: Boolean) {
            val loadMoreIntent = Intent(ACTION_SYNC, null, activity, SyncService::class.java)
            loadMoreIntent.putExtra(EXTRA_TYPE, SyncType.LOAD_MORE.action)
            loadMoreIntent.putExtra(EXTRA_ID, id)
            loadMoreIntent.putExtra(EXTRA_OFFSET, offset)
            loadMoreIntent.putExtra(EXTRA_IS_FEED, isFeed)
            activity.startService(loadMoreIntent)
        }

        init {
            syncFilter.addAction(SYNC_STARTED)
            syncFilter.addAction(SYNC_FINISHED)
        }
    }

    private lateinit var realm: Realm
    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onCreate() {
        realm = Realm.getDefaultInstance()
        super.onCreate()
    }

    override fun onDestroy() {
        realm.close()
        super.onDestroy()
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        val action = intent.getStringExtra(EXTRA_TYPE)
        val syncType: SyncType? = SyncType[action ?: ""]
        if (syncType != null) {
            notifySyncStatus(SYNC_STARTED, syncType)
            API.get(this, object : InstanceReadyCallback {
                override fun onInstanceReady(api: API) {
                    api.sync(PreferenceManager.getDefaultSharedPreferences(this@SyncService), realm, syncType, intent, object : APICallback<Void?, Throwable?> {
                        override fun onSuccess(n: Void?) {
                            if (syncType != SyncType.LOAD_MORE) Queries.removeExcessItems(realm, Queries.MAX_ITEMS)
                            realm.executeTransaction(postProcessFeedTransaction)
                            onFinished()
                        }

                        override fun onFailure(throwable: Throwable?) {
                            Toast.makeText(this@SyncService, throwable?.localizedMessage, Toast.LENGTH_SHORT).show()
                            onFinished()
                        }

                        private fun onFinished() {
                            notifySyncStatus(SYNC_FINISHED, syncType)
                            stopSelf(startId)
                        }
                    })
                }

                override fun onLoginFailure(e: Throwable) {
                    stopSelf(startId)
                }
            })
        } else {
            Log.w(TAG, "unknown Intent received: $action")
        }
        return START_NOT_STICKY
    }

    private val postProcessFeedTransaction = Realm.Transaction { realm: Realm ->
        for (feed in realm.where(Feed::class.java).findAll()) {
            feed.starredCount = realm.where(Item::class.java)
                    .equalTo(Item.FEED_ID, feed.id)
                    .equalTo(Item.STARRED, true).count().toInt()
            feed.unreadCount = realm.where(Item::class.java)
                    .equalTo(Item.FEED_ID, feed.id)
                    .equalTo(Item.UNREAD, true).count().toInt()
        }
    }

    private fun notifySyncStatus(action: String, type: SyncType) {
        val syncStarted = action == SYNC_STARTED
        val editor = PreferenceManager.getDefaultSharedPreferences(this).edit()
        // no need to update after ACTION_SYNC_CHANGES_ONLY
        if (!syncStarted && SyncType.SYNC_CHANGES_ONLY != type) editor.putBoolean(Preferences.SYS_NEEDS_UPDATE_AFTER_SYNC.key, true)
        val intent = Intent(action)
        intent.putExtra(EXTRA_TYPE, type.action)
        editor.putBoolean(Preferences.SYS_SYNC_RUNNING.key, syncStarted)
        editor.apply()
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
        Log.d(TAG, String.format("%s: %s", action, type))
    }
}