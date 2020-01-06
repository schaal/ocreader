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
package email.schaal.ocreader.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import email.schaal.ocreader.service.SyncService
import email.schaal.ocreader.service.SyncType

/**
 * Collection of methods to set or cancel the Alarm used to handle synchronizing changed items with
 * the remote server.
 */
class AlarmUtils private constructor(context: Context) {
    private val alarmManager: AlarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val pendingIntent: PendingIntent
    private var alarmRunning = false

    @Synchronized
    fun cancelAlarm() {
        if (alarmRunning) {
            alarmManager.cancel(pendingIntent)
            alarmRunning = false
        }
    }

    @Synchronized
    fun setAlarm() {
        if (!alarmRunning) {
            alarmManager[AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime() + INTERVAL_FIVE_MINUTES] = pendingIntent
            alarmRunning = true
        }
    }

    companion object {
        private const val INTERVAL_FIVE_MINUTES = 5 * 60 * 1000
        private var instance: AlarmUtils? = null
        fun init(context: Context) {
            instance = AlarmUtils(context)
        }

        fun getInstance(): AlarmUtils? {
            checkNotNull(instance) { "Initialize first" }
            return instance
        }
    }

    init {
        val syncChangesIntent = Intent(SyncService.ACTION_SYNC, null, context, SyncService::class.java)
        syncChangesIntent.putExtra(SyncService.EXTRA_TYPE, SyncType.SYNC_CHANGES_ONLY.action)
        pendingIntent = PendingIntent.getService(context, 0, syncChangesIntent, PendingIntent.FLAG_UPDATE_CURRENT)
    }
}