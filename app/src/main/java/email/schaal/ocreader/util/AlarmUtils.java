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

package email.schaal.ocreader.util;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;

import email.schaal.ocreader.service.SyncService;

/**
 * Collection of methods to set or cancel the Alarm used to handle synchronizing changed items with
 * the remote server.
 */
public class AlarmUtils {
    private static final int INTERVAL_FIVE_MINUTES = 5 * 60 * 1000;

    private static AlarmUtils instance;

    private final AlarmManager alarmManager;
    private final PendingIntent pendingIntent;

    private boolean alarmRunning = false;

    private AlarmUtils(Context context) {
        this.alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        Intent syncChangesIntent = new Intent(SyncService.ACTION_SYNC_CHANGES_ONLY, null, context, SyncService.class);
        pendingIntent = PendingIntent.getService(context, 0, syncChangesIntent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    public static void init(Context context) {
        instance = new AlarmUtils(context);
    }

    public static AlarmUtils getInstance() {
        if(instance == null)
            throw new IllegalStateException("Initialize first");
        return instance;
    }

    public synchronized void cancelAlarm() {
        if(alarmRunning) {
            alarmManager.cancel(pendingIntent);
            alarmRunning = false;
        }
    }

    public synchronized void setAlarm() {
        if(!alarmRunning) {
            alarmManager.set(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime() + INTERVAL_FIVE_MINUTES, pendingIntent);
            alarmRunning = true;
        }
    }
}
