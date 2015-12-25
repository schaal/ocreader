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

package email.schaal.cloudreader.service;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.IntentService;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.util.Date;
import java.util.concurrent.CountDownLatch;

import email.schaal.cloudreader.Preferences;
import email.schaal.cloudreader.api.APIService;
import email.schaal.cloudreader.model.Feed;
import email.schaal.cloudreader.model.Item;
import email.schaal.cloudreader.util.AlarmUtils;
import hugo.weaving.DebugLog;
import io.realm.Realm;
import io.realm.RealmResults;

public class SyncService extends IntentService {
    private static final String TAG = SyncService.class.getSimpleName();

    public static final String SYNC_FINISHED = "email.schaal.cloudreader.action.SYNC_FINISHED";
    public static final String SYNC_STARTED = "email.schaal.cloudreader.action.SYNC_STARTED";

    public static final String ACTION_SYNC_CHANGES_ONLY = "email.schaal.cloudreader.action.SYNC_CHANGES_ONLY";
    public static final String ACTION_FULL_SYNC = "email.schaal.cloudreader.action.FULL_SYNC";

    public static final IntentFilter syncFilter;

    static {
        syncFilter = new IntentFilter();
        syncFilter.addAction(SYNC_STARTED);
        syncFilter.addAction(SYNC_FINISHED);
    }

    private CountDownLatch countDownLatch;

    public SyncService() {
        super("SyncService");
    }

    @DebugLog
    @Override
    protected void onHandleIntent(Intent intent) {
        AlarmUtils.getInstance().cancelAlarm();

        Realm realm = null;
        try {
            realm = Realm.getDefaultInstance();
            // do a full sync or only sync read/starred changes?
            if (ACTION_FULL_SYNC.equals(intent.getAction())) {
                fullSync(realm);
            } else if (ACTION_SYNC_CHANGES_ONLY.equals(intent.getAction())) {
                APIService.getInstance().syncChanges(realm);
            } else {
                Log.w(TAG, "unknown Intent received: " + intent.getAction());
            }
        } finally {
            if(realm != null)
                realm.close();
        }
    }

    private long getLastSyncTimestamp(Realm realm) {
        long lastSync = 0;
        Date maximumDate = realm.where(Item.class).maximumDate(Item.LAST_MODIFIED);
        if (maximumDate != null)
            lastSync = maximumDate.getTime() / 1000 + 1;

        Log.d(TAG, "Last sync timestamp: " + lastSync);
        return lastSync;
    }

    private void fullSync(Realm realm) {
        notifySyncStatus(SYNC_STARTED);

        try {
            APIService.getInstance().syncChanges(realm);

            countDownLatch = new CountDownLatch(4);

            APIService.getInstance().user(apiCallback);
            APIService.getInstance().folders(apiCallback);
            APIService.getInstance().feeds(apiCallback, true);
            APIService.getInstance().items(getLastSyncTimestamp(realm), APIService.QueryType.ALL, apiCallback);

            try {
                countDownLatch.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            // Post-process feeds: add starredCount and update unreadCount
            final RealmResults<Feed> feeds = realm.where(Feed.class).findAll();
            realm.executeTransaction(new Realm.Transaction() {
                @Override
                public void execute(Realm realm) {
                    for (int i = 0, feedsSize = feeds.size(); i < feedsSize; i++) {
                        Feed feed = feeds.get(i);
                        feed.setStarredCount((int) realm.where(Item.class)
                                        .equalTo(Item.FEED_ID, feed.getId())
                                        .equalTo(Item.STARRED, true).count()
                        );
                        feed.setUnreadCount((int) realm.where(Item.class)
                                        .equalTo(Item.FEED_ID, feed.getId())
                                        .equalTo(Item.UNREAD, true).count()
                        );
                    }
                }
            });
        } finally {
            notifySyncStatus(SYNC_FINISHED);
        }
    }

    // not on main thread, use commit() to store SharedPreferences
    @SuppressLint("CommitPrefEdits")
    private void notifySyncStatus(@NonNull String action) {
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();
        if(action.equals(SYNC_FINISHED))
            editor.putBoolean(Preferences.SYS_NEEDS_UPDATE_AFTER_SYNC.getKey(), true);
        LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(action));
        editor.putBoolean(Preferences.SYS_SYNC_RUNNING.getKey(), action.equals(SYNC_STARTED));
        editor.commit();
    }

    private final APIService.APICallback apiCallback = new APIService.APICallback() {
        @Override
        public void onSuccess() {
            countDownLatch.countDown();
        }

        @Override
        public void onFailure(String errorMessage) {
            countDownLatch.countDown();
            Log.w(TAG, errorMessage);
        }
    };

    public static void startSync(Activity activity) {
        Intent syncIntent = new Intent(SyncService.ACTION_FULL_SYNC, null, activity, SyncService.class);
        activity.startService(syncIntent);
    }
}
