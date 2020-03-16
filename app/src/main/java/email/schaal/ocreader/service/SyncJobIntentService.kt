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

package email.schaal.ocreader.service

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.ResultReceiver
import androidx.core.app.JobIntentService
import androidx.preference.PreferenceManager
import email.schaal.ocreader.Preferences
import email.schaal.ocreader.api.API
import email.schaal.ocreader.service.SyncResultReceiver.Companion.EXCEPTION_DATA_KEY
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class SyncJobIntentService: JobIntentService() {
    companion object {
        const val SYNC_TYPE_EXTRA = "email.schaal.ocreader.SYNC_TYPE"
        const val JOB_ID = 12345

        fun enqueueWork(context: Context, syncType: SyncType, resultReceiver: SyncResultReceiver?) {
            val intent = Intent()
            intent
                    .putExtra(SYNC_TYPE_EXTRA, syncType.action)
                    .putExtra(SyncResultReceiver.INTENT_KEY,resultReceiver)
            enqueueWork(context, SyncJobIntentService::class.java, JOB_ID, intent)
        }
    }

    override fun onHandleWork(intent: Intent) {
        val syncType = SyncType[intent.getStringExtra(SYNC_TYPE_EXTRA)] ?: SyncType.FULL_SYNC
        val preferences = PreferenceManager.getDefaultSharedPreferences(this)
        val syncResultReceiver = intent.getParcelableExtra<ResultReceiver>(SyncResultReceiver.INTENT_KEY)

        if(syncType != SyncType.SYNC_CHANGES_ONLY)
            preferences.edit().putBoolean(Preferences.SYS_SYNC_RUNNING.key, true).apply()

        GlobalScope.launch {
            try {
                API(this@SyncJobIntentService).sync(syncType)
            } catch (e: Exception) {
                e.printStackTrace()
                syncResultReceiver?.send(0, Bundle().apply { putSerializable(EXCEPTION_DATA_KEY, e) })
            }
        }.invokeOnCompletion {
            preferences.edit().putBoolean(Preferences.SYS_SYNC_RUNNING.key, false).apply()
        }
    }
}