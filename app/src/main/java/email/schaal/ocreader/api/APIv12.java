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

package email.schaal.ocreader.api;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import email.schaal.ocreader.api.json.APILevels;
import email.schaal.ocreader.api.json.Feeds;
import email.schaal.ocreader.api.json.Folders;
import email.schaal.ocreader.api.json.Items;
import email.schaal.ocreader.api.json.Status;
import email.schaal.ocreader.api.json.v12.ItemIds;
import email.schaal.ocreader.api.json.v12.ItemMap;
import email.schaal.ocreader.database.Queries;
import email.schaal.ocreader.database.model.Feed;
import email.schaal.ocreader.database.model.Folder;
import email.schaal.ocreader.database.model.Item;
import email.schaal.ocreader.database.model.StarredFolder;
import email.schaal.ocreader.database.model.User;
import email.schaal.ocreader.http.HttpManager;
import email.schaal.ocreader.service.SyncService;
import email.schaal.ocreader.util.AlarmUtils;
import io.realm.Realm;
import io.realm.RealmResults;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

import static email.schaal.ocreader.service.SyncService.EXTRA_ID;
import static email.schaal.ocreader.service.SyncService.EXTRA_IS_FEED;
import static email.schaal.ocreader.service.SyncService.EXTRA_OFFSET;

/**
 * This class encapsulates the Nextcloud News API v1-2
 */
class APIv12 extends API {
    private static final String TAG = APIv12.class.getName();

    private static final int BATCH_SIZE = 100;

    private final Executor executor = Executors.newSingleThreadExecutor();
    private final Handler handler = new Handler(Looper.getMainLooper());

    private enum MarkAction {
        MARK_READ(Item.UNREAD, Item.UNREAD_CHANGED, false),
        MARK_UNREAD(Item.UNREAD, Item.UNREAD_CHANGED, true),
        MARK_STARRED(Item.STARRED, Item.STARRED_CHANGED, true),
        MARK_UNSTARRED(Item.STARRED, Item.STARRED_CHANGED, false);

        private final String key;
        private final String changedKey;
        private final boolean value;

        public String getKey() {
            return key;
        }

        public String getChangedKey() {
            return changedKey;
        }

        public boolean getValue() {
            return value;
        }

        MarkAction(String key, String changedKey, boolean value) {
            this.key = key;
            this.changedKey = changedKey;
            this.value = value;
        }
    }

    private interface OnCompletionListener {
        void onCompleted(boolean result);
    }

    private abstract class ResultRunnable implements Runnable {
        protected final boolean result;

        private ResultRunnable(boolean result) {
            this.result = result;
        }
    }

    private void syncChanges(@Nullable final OnCompletionListener completionListener) {
        AlarmUtils.getInstance().cancelAlarm();

        executor.execute(new Runnable() {
            @Override
            public void run() {
                Realm realm = null;
                boolean result = true;
                try {
                    realm = Realm.getDefaultInstance();
                    for (final MarkAction action : MarkAction.values()) {
                        result = result && markItems(action, realm);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    Queries.closeRealm(realm);
                    handler.post(new ResultRunnable(result) {
                        @Override
                        public void run() {
                            if (completionListener != null)
                                completionListener.onCompleted(this.result);
                        }
                    });
                }
            }
        });
    }

    private boolean markItems(@NonNull final MarkAction action, final Realm realm) throws IOException {
        final RealmResults<Item> results = realm.where(Item.class)
                .equalTo(action.getChangedKey(), true)
                .equalTo(action.getKey(), action.getValue()).findAll();

        if (results.size() == 0) {
            // Nothing to do, countdown and return
            return true;
        }

        ItemIds ids = null;
        ItemMap itemMap = null;

        if (action == MarkAction.MARK_UNREAD || action == MarkAction.MARK_READ) {
            ids = new ItemIds(results);
        } else {
            itemMap = new ItemMap(results);
        }

        Response<Void> response;

        switch (action) {
            case MARK_READ:
                response = api.markItemsRead(ids).execute();
                break;
            case MARK_UNREAD:
                response = api.markItemsUnread(ids).execute();
                break;
            case MARK_STARRED:
                response = api.markItemsStarred(itemMap).execute();
                break;
            case MARK_UNSTARRED:
                response = api.markItemsUnstarred(itemMap).execute();
                break;
            default:
                throw new IllegalArgumentException("Unkown mark action");
        }

        if (response.isSuccessful()) {
            realm.executeTransaction(new Realm.Transaction() {
                @Override
                public void execute(Realm realm) {
                    if (action == MarkAction.MARK_READ || action == MarkAction.MARK_UNREAD) {
                        for (Item item : results) {
                            item.setUnreadChanged(false);
                        }
                    } else {
                        for (Item item : results) {
                            item.setStarredChanged(false);
                        }
                    }
                }
            });
        }

        return response.isSuccessful();
    }

    private enum QueryType {
        FEED(0),
        FOLDER(1),
        STARRED(2),
        ALL(3);

        private final int type;

        QueryType(int type) {
            this.type = type;
        }

        public int getType() {
            return type;
        }
    }

    private APIv12Interface api;

    APIv12(Context context) {
        super(context, APILevels.Level.V12);
    }

    @Override
    public void setupApi(HttpManager httpManager) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(httpManager.getCredentials().getRootUrl().resolve(String.format("%s%s/", API_ROOT, apiLevel.getLevel())))
                .client(httpManager.getClient())
                .addConverterFactory(converterFactory)
                .build();

        api = retrofit.create(APIv12Interface.class);
    }

    @SuppressWarnings("SameParameterValue")
    private interface APIv12Interface {
        /** SERVER **/

        /** Since 6.0.5 **/
        @GET("user")
        Call<User> user();

        @GET("status")
        Call<Status> status();

        /** FOLDERS **/
        @GET("folders")
        Call<Folders> folders();

        /** FEEDS **/
        @GET("feeds")
        Call<Feeds> feeds();

        @POST("feeds")
        Call<Feeds> createFeed(@Body Map<String, Object> feedMap);

        @PUT("feeds/{feedId}/move")
        Call<Void> moveFeed(@Path("feedId") long feedId, @Body Map<String,Long> folderIdMap);

        @DELETE("feeds/{feedId}")
        Call<Void> deleteFeed(@Path("feedId") long feedId);

        /** ITEMS **/
        @GET("items")
        Call<Items> items(
                @Query("batchSize") long batchSize,
                @Query("offset") long offset,
                @Query("type") int type,
                @Query("id") long id,
                @Query("getRead") boolean getRead,
                @Query("oldestFirst") boolean oldestFirst
        );

        @GET("items/updated")
        Call<Items> updatedItems(
                @Query("lastModified") long lastModified,
                @Query("type") int type,
                @Query("id") long id
        );

        @PUT("items/read/multiple")
        Call<Void> markItemsRead(@Body ItemIds items);

        @PUT("items/unread/multiple")
        Call<Void> markItemsUnread(@Body ItemIds items);

        @PUT("items/star/multiple")
        Call<Void> markItemsStarred(@Body ItemMap itemMap);

        @PUT("items/unstar/multiple")
        Call<Void> markItemsUnstarred(@Body ItemMap itemMap);
    }

    @Override
    protected void metaData(Callback<Status> callback) {
        api.status().enqueue(callback);
    }

    private final ExecutorService threadPool = Executors.newCachedThreadPool();

    @Override
    public void sync(SharedPreferences sharedPreferences, final Realm realm, final SyncService.SyncType syncType, final Intent intent, final APICallback<Void, String> callback) {
        syncChanges(new OnCompletionListener() {
            @Override
            public void onCompleted(boolean result) {
                if(result) {
                    final Set<Callable<Runnable>> callables = new HashSet<>(5);

                    switch (syncType) {
                        case SYNC_CHANGES_ONLY:
                            callback.onSuccess(null);
                            break;
                        case FULL_SYNC:
                            long lastSync = getLastSyncTimestamp(realm);

                            callables.add(new FoldersCallable(realm));
                            callables.add(new FeedsCallable(realm));

                            if (lastSync == 0L) {
                                callables.add(new StarredItemsCallable(realm));
                                callables.add(new ItemsCallable(realm));
                            } else {
                                callables.add(new UpdatedItemsCallable(realm, lastSync));
                            }
                            break;
                        case LOAD_MORE:
                            long id = intent.getLongExtra(EXTRA_ID, -1);
                            long offset = intent.getLongExtra(EXTRA_OFFSET, 0);
                            boolean isFeed = intent.getBooleanExtra(EXTRA_IS_FEED, false);

                            final QueryType queryType;

                            if (id == StarredFolder.ID) {
                                queryType = QueryType.STARRED;
                                id = 0;
                            } else {
                                queryType = isFeed ? QueryType.FEED : QueryType.FOLDER;
                            }

                            callables.add(new MoreItemsCallable(realm, queryType, offset, id));

                            break;
                    }

                    executor.execute(new Runnable() {
                        @Override
                        public void run() {
                            final ExecutorCompletionService<Runnable> completionService = new ExecutorCompletionService<>(threadPool);
                            // Start API Calls
                            for (Callable<Runnable> callable : callables) {
                                completionService.submit(callable);
                            }

                            try {
                                // Get API Call results
                                for (int i = 0, size = callables.size(); i < size; i++) {
                                    final Runnable runnable = completionService.take().get();

                                    // Run the Runnable on the main thread (e.g. insert result into db)
                                    handler.post(runnable);
                                }

                                // Run callback on main thread
                                handler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        callback.onSuccess(null);
                                    }
                                });
                            } catch (InterruptedException | ExecutionException e) {
                                e.printStackTrace();
                                handler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        callback.onFailure(e.getLocalizedMessage());
                                    }
                                });
                            }
                        }
                    });
                } else {
                    callback.onFailure("");
                }
            }
        });
    }

    private long getLastSyncTimestamp(Realm realm) {
        final Number lastSync = realm.where(Item.class).max(Item.LAST_MODIFIED);

        return lastSync != null ? lastSync.longValue() : 0;
    }

    @Override
    public void user(final Realm realm, final APICallback<Void, String> callback) {
        api.user().enqueue(new BaseRetrofitCallback<User>(callback) {
            @Override
            protected boolean onResponseReal(Response<User> response) {
                Queries.insert(realm, response.body());
                return true;
            }
        });
    }

    private abstract class RealmCallable<T> implements Callable<Runnable> {
        protected final Realm realm;

        RealmCallable(Realm realm) {
            this.realm = realm;
        }

        protected abstract Runnable getRunnable(Response<T> response);
        protected abstract Response<T> getResponse() throws IOException;

        @Override
        public Runnable call() throws Exception {
            final Response<T> response = getResponse();
            if(response.isSuccessful())
                return getRunnable(response);
            return null;
        }
    }

    private class ItemsCallable extends RealmCallable<Items> {
        ItemsCallable(Realm realm) {
            super(realm);
        }

        @Override
        protected Response<Items> getResponse() throws IOException {
            return api.items(-1, 0L, QueryType.ALL.getType(), 0L, false, false).execute();
        }

        @Override
        protected Runnable getRunnable(final Response<Items> response) {
            return new Runnable() {
                @Override
                public void run() {
                    Queries.insert(realm, response.body().getItems());
                }
            };
        }
    }

    private class UpdatedItemsCallable extends RealmCallable<Items> {
        private final long lastSync;

        UpdatedItemsCallable(Realm realm, long lastSync) {
            super(realm);
            this.lastSync = lastSync;
        }

        @Override
        protected Response<Items> getResponse() throws IOException {
            return api.updatedItems(lastSync, QueryType.ALL.getType(), 0L).execute();
        }

        @Override
        protected Runnable getRunnable(final Response<Items> response) {
            return new Runnable() {
                @Override
                public void run() {
                    Queries.insert(realm, response.body().getItems());
                }
            };
        }
    }

    private class StarredItemsCallable extends RealmCallable<Items> {
        StarredItemsCallable(Realm realm) {
            super(realm);
        }

        @Override
        protected Response<Items> getResponse() throws IOException {
            return api.items(-1, 0L, QueryType.STARRED.getType(), 0L, true, false).execute();
        }

        @Override
        protected Runnable getRunnable(final Response<Items> response) {
            return new Runnable() {
                @Override
                public void run() {
                    Queries.insert(realm, response.body().getItems());
                }
            };
        }
    }

    private class MoreItemsCallable extends RealmCallable<Items> {
        private final QueryType type;
        private final long offset;
        private final long id;

        MoreItemsCallable(Realm realm, final QueryType type, final long offset, final long id) {
            super(realm);
            this.type = type;
            this.offset = offset;
            this.id = id;
        }

        @Override
        protected Response<Items> getResponse() throws IOException {
            return api.items(BATCH_SIZE, offset, type.getType(), id, true, false).execute();
        }

        @Override
        protected Runnable getRunnable(final Response<Items> response) {
            return new Runnable() {
                @Override
                public void run() {
                    Queries.insert(realm, response.body().getItems());
                }
            };
        }
    }

    private class FoldersCallable extends RealmCallable<Folders> {
        FoldersCallable(Realm realm) {
            super(realm);
        }

        @Override
        protected Runnable getRunnable(final Response<Folders> response) {
            return new Runnable() {
                @Override
                public void run() {
                    Queries.deleteAndInsert(realm, Folder.class, response.body().getFolders());
                }
            };
        }

        @Override
        protected Response<Folders> getResponse() throws IOException {
            return api.folders().execute();
        }
    }

    private class FeedsCallable extends RealmCallable<Feeds> {
        FeedsCallable(Realm realm) {
            super(realm);
        }

        @Override
        protected Runnable getRunnable(final Response<Feeds> response) {
            return new Runnable() {
                @Override
                public void run() {
                    Queries.deleteAndInsert(realm, Feed.class, response.body().getFeeds());
                }
            };
        }

        @Override
        protected Response<Feeds> getResponse() throws IOException {
            return api.feeds().execute();
        }
    }

    @Override
    public void createFeed(final Realm realm, final String url, final long folderId, APICallback<Void, String> apiCallback) {
        final Map<String, Object> feedMap = new HashMap<>(2);

        feedMap.put("url", url);
        feedMap.put("folderId", folderId);

        api.createFeed(feedMap).enqueue(new BaseRetrofitCallback<Feeds>(apiCallback) {
            @Override
            protected boolean onResponseReal(final Response<Feeds> response) {
                // Set unreadCount to 0, items have not been fetched yet for this feed
                Feed feed = response.body().getFeeds().get(0);
                feed.setUnreadCount(0);

                Queries.insert(realm, feed);

                return true;
            }
        });
    }

    @Override
    public void moveFeed(final Realm realm, final Feed feed, final long folderId, APICallback<Void, String> apiCallback) {
        final Map<String, Long> folderIdMap = new HashMap<>(1);
        folderIdMap.put("folderId", folderId);

        api.moveFeed(feed.getId(), folderIdMap).enqueue(new BaseRetrofitCallback<Void>(apiCallback) {
            @Override
            protected boolean onResponseReal(Response<Void> response) {
                realm.executeTransaction(new Realm.Transaction() {
                    @Override
                    public void execute(Realm realm) {
                        feed.setFolderId(folderId);
                        feed.setFolder(Folder.getOrCreate(realm, folderId));
                    }
                });
                return true;
            }
        });
    }

    @Override
    public void deleteFeed(final Realm realm, final Feed feed, APICallback<Void, String> apiCallback) {
        api.deleteFeed(feed.getId()).enqueue(new BaseRetrofitCallback<Void>(apiCallback) {
            @Override
            protected boolean onResponseReal(Response<Void> response) {
                realm.executeTransaction(new Realm.Transaction() {
                    @Override
                    public void execute(Realm realm) {
                        feed.delete(realm);
                    }
                });
                return true;
            }
        });
    }
}
