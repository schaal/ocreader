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
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import email.schaal.ocreader.Preferences;
import email.schaal.ocreader.api.APIService;
import email.schaal.ocreader.database.Queries;
import email.schaal.ocreader.model.Feed;
import email.schaal.ocreader.model.Item;
import email.schaal.ocreader.model.StarredFolder;
import io.realm.Realm;
import io.realm.RealmResults;

public class SyncService extends Service {
    private static final String TAG = SyncService.class.getName();

    public static final String SYNC_FINISHED = "email.schaal.ocreader.action.SYNC_FINISHED";
    public static final String SYNC_STARTED = "email.schaal.ocreader.action.SYNC_STARTED";

    public static final String ACTION_SYNC_CHANGES_ONLY = "email.schaal.ocreader.action.SYNC_CHANGES_ONLY";
    public static final String ACTION_FULL_SYNC = "email.schaal.ocreader.action.FULL_SYNC";
    public static final String ACTION_LOAD_MORE = "email.schaal.ocreader.action.LOAD_MORE";

    public static final String EXTRA_ID = "email.schaal.ocreader.action.extra.ID";
    public static final String EXTRA_IS_FEED = "email.schaal.ocreader.action.extra.IS_FEED";
    public static final String EXTRA_OFFSET = "email.schaal.ocreader.action.extra.OFFSET";
    public static final String EXTRA_TYPE = "email.schaal.ocreader.action.extra.TYPE";
    public static final String EXTRA_INITIAL_SYNC = "email.schaal.ocreader.action.extra.INITIAL_SYNC";

    private static final int MAX_ITEMS = 10000;

    private SharedPreferences sharedPreferences;

    private enum SyncType {
        FULL_SYNC(ACTION_FULL_SYNC),
        SYNC_CHANGES_ONLY(ACTION_SYNC_CHANGES_ONLY),
        LOAD_MORE(ACTION_LOAD_MORE);

        private final String action;

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

    private final Executor executor = Executors.newSingleThreadExecutor();
    private Realm realm;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        realm = Realm.getDefaultInstance();
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        realm.close();
        super.onDestroy();
    }

    @Override
    public int onStartCommand(final Intent intent, int flags, final int startId) {
        final String action = intent.getAction();

        final SyncType syncType = SyncType.get(action);

        if(syncType != null) {
            notifySyncStatus(SYNC_STARTED, action);

            APIService.getInstance().syncChanges(new APIService.OnCompletionListener() {
                @Override
                public void onCompleted(boolean result) {
                    if(result) {
                        CountdownAPICallback apiCallback;

                        switch (syncType) {
                            case SYNC_CHANGES_ONLY:
                                notifySyncStatus(SYNC_FINISHED, action);
                                stopSelf(startId);
                                break;
                            case FULL_SYNC:
                                long lastSync = 0L;

                                if (!intent.getBooleanExtra(EXTRA_INITIAL_SYNC, false))
                                    lastSync = getLastSyncTimestamp(realm);

                                apiCallback = new CountdownAPICallback(new CountDownLatch(lastSync == 0L ? 5 : 4));

                                APIService.getInstance().user(realm, apiCallback);
                                APIService.getInstance().folders(realm, apiCallback);
                                APIService.getInstance().feeds(realm, apiCallback);

                                if (lastSync == 0L) {
                                    APIService.getInstance().starredItems(realm, apiCallback);
                                    APIService.getInstance().items(realm, apiCallback);
                                } else {
                                    Queries.getInstance().removeExcessItems(realm, MAX_ITEMS);
                                    APIService.getInstance().updatedItems(realm, lastSync, apiCallback);
                                }

                                waitForCountdownLatch(startId, action, apiCallback.countDownLatch);
                                break;
                            case LOAD_MORE:
                                long id = intent.getLongExtra(EXTRA_ID, -1);
                                long offset = intent.getLongExtra(EXTRA_OFFSET, 0);
                                boolean isFeed = intent.getBooleanExtra(EXTRA_IS_FEED, false);

                                APIService.QueryType queryType;

                                if (id == StarredFolder.ID) {
                                    queryType = APIService.QueryType.STARRED;
                                    id = 0;
                                } else {
                                    queryType = isFeed ? APIService.QueryType.FEED : APIService.QueryType.FOLDER;
                                }

                                apiCallback = new CountdownAPICallback(new CountDownLatch(1));
                                APIService.getInstance().moreItems(realm, queryType, offset, id, apiCallback);

                                waitForCountdownLatch(startId, action, apiCallback.countDownLatch);
                                break;
                        }
                    } else {
                        notifySyncStatus(SYNC_FINISHED, action);
                        stopSelf(startId);
                    }
                }
            });
        } else {
            Log.w(TAG, "unknown Intent received: " + action);
        }

        return START_NOT_STICKY;
    }

    private final Realm.Transaction postProcessFeedTransaction = new Realm.Transaction() {
        @Override
        public void execute(Realm realm) {
            final RealmResults<Feed> feeds = realm.where(Feed.class).findAll();
            for (int i = 0, feedsSize = feeds.size(); i < feedsSize; i++) {
                Feed feed = feeds.get(i);
                feed.setStarredCount((int) realm.where(Item.class)
                                .equalTo(Item.FEED_ID, feed.getId())
                                .equalTo(Item.STARRED, true).count()
                );
            }
        }
    };

    private void waitForCountdownLatch(final int startId, final String action, final CountDownLatch countDownLatch) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    countDownLatch.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } finally {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            realm.executeTransaction(postProcessFeedTransaction);
                            notifySyncStatus(SYNC_FINISHED, action);
                            stopSelf(startId);
                        }
                    });
                }
            }
        });
    }

    private long getLastSyncTimestamp(Realm realm) {
        Number lastSync = realm.where(Item.class).max(Item.LAST_MODIFIED);

        return lastSync != null ? lastSync.longValue() : 0;
    }

    private final Handler handler = new Handler(Looper.getMainLooper());

    private void notifySyncStatus(@NonNull String action, String type) {
        final boolean syncStarted = action.equals(SYNC_STARTED);

        SharedPreferences.Editor editor = sharedPreferences.edit();

        // no need to update after ACTION_SYNC_CHANGES_ONLY
        if(!syncStarted && !ACTION_SYNC_CHANGES_ONLY.equals(type))
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
        Intent syncIntent = new Intent(ACTION_FULL_SYNC, null, activity, SyncService.class);
        syncIntent.putExtra(EXTRA_INITIAL_SYNC, initialSync);
        activity.startService(syncIntent);
    }

    public static void startLoadMore(Activity activity, long id, long offset, boolean isFeed) {
        Intent loadMoreIntent = new Intent(ACTION_LOAD_MORE, null, activity, SyncService.class);
        loadMoreIntent.putExtra(EXTRA_ID, id);
        loadMoreIntent.putExtra(EXTRA_OFFSET, offset);
        loadMoreIntent.putExtra(EXTRA_IS_FEED, isFeed);
        activity.startService(loadMoreIntent);
    }

    private class CountdownAPICallback implements APIService.APICallback {
        private final CountDownLatch countDownLatch;

        private CountdownAPICallback(CountDownLatch countDownLatch) {
            this.countDownLatch = countDownLatch;
        }

        @Override
        public void onSuccess() {
            countDownLatch.countDown();
        }

        @Override
        public void onFailure(String errorMessage) {
            countDownLatch.countDown();

            Toast.makeText(SyncService.this, errorMessage, Toast.LENGTH_LONG).show();
            Log.w(TAG, errorMessage);
        }
    }
}
