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

package email.schaal.ocreader.service;

import android.app.Activity;
import android.app.Service;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import email.schaal.ocreader.Preferences;
import email.schaal.ocreader.api.API;
import email.schaal.ocreader.database.Queries;
import email.schaal.ocreader.database.model.Feed;
import email.schaal.ocreader.database.model.Item;
import io.realm.Realm;

public class SyncService extends Service {
    private static final String TAG = SyncService.class.getName();

    public static final String SYNC_FINISHED = "email.schaal.ocreader.action.SYNC_FINISHED";
    public static final String SYNC_STARTED = "email.schaal.ocreader.action.SYNC_STARTED";

    public static final String ACTION_SYNC = "email.schaal.ocreader.action.SYNC";

    public static final String EXTRA_ID = "email.schaal.ocreader.action.extra.ID";
    public static final String EXTRA_IS_FEED = "email.schaal.ocreader.action.extra.IS_FEED";
    public static final String EXTRA_OFFSET = "email.schaal.ocreader.action.extra.OFFSET";
    public static final String EXTRA_TYPE = "email.schaal.ocreader.action.extra.TYPE";
    public static final String EXTRA_INITIAL_SYNC = "email.schaal.ocreader.action.extra.INITIAL_SYNC";

    public enum SyncType {
        FULL_SYNC("email.schaal.ocreader.action.FULL_SYNC"),
        SYNC_CHANGES_ONLY("email.schaal.ocreader.action.SYNC_CHANGES_ONLY"),
        LOAD_MORE("email.schaal.ocreader.action.LOAD_MORE");

        public final String action;

        SyncType(String action) {
            this.action = action;
        }

        @Nullable
        public static SyncType get(String action) {
            for(SyncType syncType: values())
                if(syncType.action.equals(action))
                    return syncType;
            return null;
        }
    }

    public static final IntentFilter syncFilter;
    static {
        syncFilter = new IntentFilter();
        syncFilter.addAction(SYNC_STARTED);
        syncFilter.addAction(SYNC_FINISHED);
    }

    private Realm realm;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        realm = Realm.getDefaultInstance();
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        realm.close();
        super.onDestroy();
    }

    @Override
    public int onStartCommand(final Intent intent, int flags, final int startId) {
        final String action = intent.getStringExtra(EXTRA_TYPE);

        final SyncType syncType = SyncType.get(action);

        if(syncType != null) {
            notifySyncStatus(SYNC_STARTED, action);

            try {
                API.getInstance(this).sync(PreferenceManager.getDefaultSharedPreferences(this), realm, syncType, intent, new API.APICallback<Void, String>() {
                    @Override
                    public void onSuccess(Void n) {
                        if(syncType != SyncType.LOAD_MORE)
                            Queries.removeExcessItems(realm, Queries.MAX_ITEMS);
                        realm.executeTransaction(postProcessFeedTransaction);
                        onFinished();
                    }

                    @Override
                    public void onFailure(String errorMessage) {
                        Toast.makeText(SyncService.this, errorMessage, Toast.LENGTH_SHORT).show();
                        onFinished();
                    }

                    private void onFinished() {
                        notifySyncStatus(SYNC_FINISHED, action);
                        stopSelf(startId);
                    }
                });
            } catch (API.NotLoggedInException e) {
                stopSelf(startId);
            }
        } else {
            Log.w(TAG, "unknown Intent received: " + action);
        }

        return START_NOT_STICKY;
    }

    private final Realm.Transaction postProcessFeedTransaction = new Realm.Transaction() {
        @Override
        public void execute(Realm realm) {
            for (Feed feed: realm.where(Feed.class).findAll()) {
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
    };

    private void notifySyncStatus(@NonNull String action, String type) {
        final boolean syncStarted = action.equals(SYNC_STARTED);

        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();

        // no need to update after ACTION_SYNC_CHANGES_ONLY
        if(!syncStarted && !SyncType.SYNC_CHANGES_ONLY.action.equals(type))
            editor.putBoolean(Preferences.SYS_NEEDS_UPDATE_AFTER_SYNC.getKey(), true);

        final Intent intent = new Intent(action);
        intent.putExtra(EXTRA_TYPE, type);

        editor.putBoolean(Preferences.SYS_SYNC_RUNNING.getKey(), syncStarted);
        editor.apply();

        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);

        Log.d(TAG, String.format("%s: %s", action, type));
    }

    public static void startSync(Activity activity) {
        startSync(activity, false);
    }
    public static void startSync(Activity activity, boolean initialSync) {
        Intent syncIntent = new Intent(ACTION_SYNC, null, activity, SyncService.class);
        syncIntent.putExtra(EXTRA_TYPE, SyncType.FULL_SYNC.action);
        syncIntent.putExtra(EXTRA_INITIAL_SYNC, initialSync);
        activity.startService(syncIntent);
    }

    public static void startLoadMore(Activity activity, long id, long offset, boolean isFeed) {
        Intent loadMoreIntent = new Intent(ACTION_SYNC, null, activity, SyncService.class);
        loadMoreIntent.putExtra(EXTRA_TYPE, SyncType.LOAD_MORE.action);
        loadMoreIntent.putExtra(EXTRA_ID, id);
        loadMoreIntent.putExtra(EXTRA_OFFSET, offset);
        loadMoreIntent.putExtra(EXTRA_IS_FEED, isFeed);
        activity.startService(loadMoreIntent);
    }

}
