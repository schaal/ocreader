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
import android.support.v4.util.ArrayMap;
import android.util.Log;
import android.widget.Toast;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import email.schaal.ocreader.Preferences;
import email.schaal.ocreader.api.APIService;
import email.schaal.ocreader.model.Feed;
import email.schaal.ocreader.model.Item;
import email.schaal.ocreader.model.StarredFolder;
import email.schaal.ocreader.util.AlarmUtils;
import io.realm.Realm;
import io.realm.RealmResults;

public class SyncService extends Service {
    private static final String TAG = SyncService.class.getSimpleName();

    public static final String SYNC_FINISHED = "email.schaal.ocreader.action.SYNC_FINISHED";
    public static final String SYNC_STARTED = "email.schaal.ocreader.action.SYNC_STARTED";

    public static final String ACTION_SYNC_CHANGES_ONLY = "email.schaal.ocreader.action.SYNC_CHANGES_ONLY";
    public static final String ACTION_FULL_SYNC = "email.schaal.ocreader.action.FULL_SYNC";
    public static final String ACTION_LOAD_MORE = "email.schaal.ocreader.action.LOAD_MORE";

    public static final String EXTRA_ID = "email.schaal.ocreader.action.extra.ID";
    public static final String EXTRA_IS_FEED = "email.schaal.ocreader.action.extra.IS_FEED";
    public static final String EXTRA_OFFSET = "email.schaal.ocreader.action.extra.OFFSET";
    public static final String EXTRA_TYPE = "email.schaal.ocreader.action.extra.TYPE";

    private SharedPreferences sharedPreferences;

    private enum SyncType {
        FULL_SYNC,
        SYNC_CHANGES_ONLY,
        LOAD_MORE
    }

    private static final Map<String, SyncType> syncTypeMap;
    static {
        syncTypeMap = new ArrayMap<>(SyncType.values().length);
        syncTypeMap.put(ACTION_FULL_SYNC, SyncType.FULL_SYNC);
        syncTypeMap.put(ACTION_SYNC_CHANGES_ONLY, SyncType.SYNC_CHANGES_ONLY);
        syncTypeMap.put(ACTION_LOAD_MORE, SyncType.LOAD_MORE);
    }

    public static final IntentFilter syncFilter;
    static {
        syncFilter = new IntentFilter();
        syncFilter.addAction(SYNC_STARTED);
        syncFilter.addAction(SYNC_FINISHED);
    }

    private final Executor executor = Executors.newSingleThreadExecutor();
    private Realm realm;

    private final Map<Integer, CountDownLatch> countDownLatches = new ArrayMap<>(4);
    private final List<Integer> startIds = new ArrayList<>();

    public SyncService() {
    }

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

        final SyncType syncType = syncTypeMap.get(action);

        if(syncType != null) {
            notifySyncStatus(SYNC_STARTED, action);
            startIds.add(startId);

            AlarmUtils.getInstance().cancelAlarm();
            APIService.getInstance().syncChanges(realm, new APIService.OnCompletionListener() {
                @Override
                public void onCompleted() {
                    switch (syncType) {
                        case SYNC_CHANGES_ONLY:
                            notifySyncStatus(SYNC_FINISHED, action);
                            stopSelf(startIds.remove(0));
                            break;
                        case FULL_SYNC:
                            long lastSync = getLastSyncTimestamp(realm);

                            APIService.APICallback apiCallback = getApiCallback(startId, lastSync == 0L ? 5 : 4);

                            APIService.getInstance().user(realm, apiCallback);
                            APIService.getInstance().folders(realm, apiCallback);
                            APIService.getInstance().feeds(realm, apiCallback);

                            APIService.getInstance().items(realm, lastSync, apiCallback);

                            if(lastSync == 0L)
                                APIService.getInstance().starredItems(realm, apiCallback);

                            waitForCountdownLatch(startId, action);
                            break;
                        case LOAD_MORE:
                            long id = intent.getLongExtra(EXTRA_ID, -1);
                            long offset = intent.getLongExtra(EXTRA_OFFSET, 0);
                            boolean isFeed = intent.getBooleanExtra(EXTRA_IS_FEED, false);

                            APIService.QueryType queryType;

                            if(id == StarredFolder.ID) {
                                queryType = APIService.QueryType.STARRED;
                                id = 0;
                            } else {
                                queryType = isFeed ? APIService.QueryType.FEED : APIService.QueryType.FOLDER;
                            }

                            APIService.getInstance().moreItems(realm, queryType, offset, id, getApiCallback(startId, 1));

                            waitForCountdownLatch(startId, action);
                            break;
                    }
                }
            });
        } else {
            Log.w(TAG, "unknown Intent received: " + action);
        }

        return START_NOT_STICKY;
    }

    @NonNull
    private APIService.APICallback getApiCallback(int startId, int taskCount) {
        CountDownLatch countDownLatch = new CountDownLatch(taskCount);

        countDownLatches.put(startId, countDownLatch);

        return new CountdownAPICallback(countDownLatch);
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
                if (feed.getFaviconLink() != null)
                    Picasso.with(SyncService.this).load(feed.getFaviconLink()).fetch();
            }
        }
    };

    private void waitForCountdownLatch(final int startId, final String action) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    countDownLatches.get(startId).await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } finally {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            realm.executeTransaction(postProcessFeedTransaction);
                            countDownLatches.remove(startId);
                            notifySyncStatus(SYNC_FINISHED, action);
                            stopSelf(startIds.remove(0));
                        }
                    });
                }
            }
        });
    }

    private long getLastSyncTimestamp(Realm realm) {
        long lastSync = 0;
        Date maximumDate = realm.where(Item.class).maximumDate(Item.LAST_MODIFIED);
        if (maximumDate != null)
            lastSync = maximumDate.getTime() / 1000;

        return lastSync;
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
        Intent syncIntent = new Intent(ACTION_FULL_SYNC, null, activity, SyncService.class);
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
