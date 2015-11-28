/*
 * Copyright (C) 2015 Daniel Schaal <daniel@schaal.email>
 *
 * This file is part of Cloudreader.
 *
 * Cloudreader is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Cloudreader is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Cloudreader.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package email.schaal.cloudreader.util;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;

import email.schaal.cloudreader.service.SyncService;

/**
 * Created by daniel on 17.11.15.
 */
public class AlarmUtils {
    public static final int INTERVAL_FIVE_MINUTES = 5 * 60 * 1000;

    private static AlarmUtils instance;

    private final AlarmManager alarmManager;
    private final Context context;

    private PendingIntent pendingIntent = null;
    private final Intent syncChangesIntent;

    private AlarmUtils(Context context) {
        this.context = context;
        this.alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        syncChangesIntent = new Intent(SyncService.ACTION_SYNC_CHANGES_ONLY, null, context, SyncService.class);
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
        if(pendingIntent != null)
            alarmManager.cancel(pendingIntent);
        pendingIntent = null;
    }

    public synchronized void setAlarm() {
        if(pendingIntent == null) {
            pendingIntent = PendingIntent.getService(context, 0, syncChangesIntent, PendingIntent.FLAG_ONE_SHOT);

            alarmManager.set(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime() + INTERVAL_FIVE_MINUTES, pendingIntent);
        }
    }
}
