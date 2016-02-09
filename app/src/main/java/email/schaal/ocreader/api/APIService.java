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

package email.schaal.ocreader.api;

import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.github.zafarkhaja.semver.Version;
import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import email.schaal.ocreader.api.json.Feeds;
import email.schaal.ocreader.api.json.Folders;
import email.schaal.ocreader.api.json.ItemIds;
import email.schaal.ocreader.api.json.ItemMap;
import email.schaal.ocreader.api.json.Items;
import email.schaal.ocreader.database.Queries;
import email.schaal.ocreader.http.HttpManager;
import email.schaal.ocreader.model.ChangedItems;
import email.schaal.ocreader.model.Feed;
import email.schaal.ocreader.model.FeedTypeAdapter;
import email.schaal.ocreader.model.Folder;
import email.schaal.ocreader.model.FolderTypeAdapter;
import email.schaal.ocreader.model.Item;
import email.schaal.ocreader.model.ItemTypeAdapter;
import email.schaal.ocreader.model.User;
import email.schaal.ocreader.model.UserTypeAdapter;
import email.schaal.ocreader.model.VersionTypeAdapter;
import io.realm.Realm;
import io.realm.RealmList;
import io.realm.RealmObject;
import io.realm.RealmResults;
import okhttp3.HttpUrl;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

/**
 * This class encapsulates the ownCloud News API and communicates with the remote ownCloud instance.
 */
public class APIService {
    private static final String TAG = APIService.class.getSimpleName();

    private static final String ROOT_PATH_APIv1_2 = "./index.php/apps/news/api/v1-2/";

    private static final int BATCH_SIZE = 100;
    private static final int MAX_ITEMS = 10000;

    private final Gson gson;

    private final Executor executor = Executors.newSingleThreadExecutor();
    private final Handler handler = new Handler(Looper.getMainLooper());

    private enum MarkAction {
        MARK_READ(Item.UNREAD, false),
        MARK_UNREAD(Item.UNREAD, true),
        MARK_STARRED(Item.STARRED, true),
        MARK_UNSTARRED(Item.STARRED, false);

        private final String key;
        private final boolean value;

        public String getKey() {
            return key;
        }

        public boolean getValue() {
            return value;
        }

        MarkAction(String key, boolean value) {
            this.key = key;
            this.value = value;
        }
    }

    public interface OnCompletionListener {
        void onCompleted();
    }

    private final Set<List<Item>> itemsToClear = new HashSet<>(MarkAction.values().length);

    public void syncChanges(@NonNull final Realm realm, @Nullable final OnCompletionListener completionListener) {
        final ChangedItems changedItems = realm.where(ChangedItems.class).findFirst();
        final CountDownLatch countDownLatch = new CountDownLatch(MarkAction.values().length);

        itemsToClear.clear();
        for (MarkAction action : MarkAction.values()) {
            markItems(action, changedItems, countDownLatch, itemsToClear);
        }

        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    countDownLatch.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        realm.executeTransaction(new Realm.Transaction() {
                            @Override
                            public void execute(Realm realm) {
                                for(List<Item> itemList: itemsToClear)
                                        itemList.clear();
                            }
                        });
                        if(completionListener != null)
                            completionListener.onCompleted();
                    }
                });
            }
        });

    }

    private void markItems(final MarkAction action, ChangedItems changedItems, final CountDownLatch countDownLatch, final Set<List<Item>> itemsToClear) {
        RealmResults<Item> results;
        final RealmList<Item> items;

        if(action.getKey().equals(Item.UNREAD))
            items = changedItems.getUnreadChangedItems();
        else
            items = changedItems.getStarredChangedItems();

        results = items.where().equalTo(action.getKey(), action.getValue()).findAll();

        if(results.size() == 0) {
            // Nothing to do, countdown and return
            countDownLatch.countDown();
            return;
        }


        ItemIds ids = null;
        ItemMap itemMap = null;

        if(action == MarkAction.MARK_UNREAD || action == MarkAction.MARK_READ) {
            ids = new ItemIds(results);
        } else {
            itemMap = new ItemMap(results);
        }

        final Callback<Void> markCallback = new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                countDownLatch.countDown();
                if(response.isSuccess())
                    itemsToClear.add(items);
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                t.printStackTrace();
                countDownLatch.countDown();
            }
        };

        switch (action) {
            case MARK_READ:
                api.markItemsRead(ids).enqueue(markCallback);
                break;
            case MARK_UNREAD:
                api.markItemsUnread(ids).enqueue(markCallback);
                break;
            case MARK_STARRED:
                api.markItemsStarred(itemMap).enqueue(markCallback);
                break;
            case MARK_UNSTARRED:
                api.markItemsUnstarred(itemMap).enqueue(markCallback);
                break;
        }
    }

    public enum QueryType {
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

    private static APIService instance;

    private API api;

    public static void init() {
        instance = new APIService();
    }

    public static APIService getInstance() {
        if(instance == null)
            throw new IllegalStateException("initialize first");
        return instance;
    }

    private APIService() {
        gson = new GsonBuilder()
                .registerTypeAdapter(Folder.class, new FolderTypeAdapter())
                .registerTypeAdapter(Feed.class, new FeedTypeAdapter())
                .registerTypeAdapter(Item.class, new ItemTypeAdapter())
                .registerTypeAdapter(User.class, new UserTypeAdapter())
                .registerTypeAdapter(Version.class, new VersionTypeAdapter())
                .setExclusionStrategies(new ExclusionStrategy() {
                    @Override
                    public boolean shouldSkipField(FieldAttributes f) {
                        return f.getDeclaringClass().equals(RealmObject.class);
                    }

                    @Override
                    public boolean shouldSkipClass(Class<?> clazz) {
                        return false;
                    }
                })
                .create();

        if(HttpManager.getInstance().hasCredentials())
            setupApi();
    }

    public void setupApi() {
        HttpUrl baseUrl = HttpManager.getInstance().getRootUrl().resolve(ROOT_PATH_APIv1_2);

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(HttpManager.getInstance().getClient())
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();

        api = retrofit.create(API.class);
    }

    public API getApi() {
        return api;
    }

    @SuppressWarnings("SameParameterValue,unused")
    public interface API {
        /** SERVER **/
        @GET("version")
        Call<Version> version();

        /** Since 6.0.5 **/
        @GET("user")
        Call<User> user();

        /** FOLDERS **/
        @GET("folders")
        Call<Folders> folders();

        @PUT("folders/{folderId}/read")
        Call<Void> markItemsOfFolderRead(@Path("folderId") int folderId, @Body int newestItemId);

        /** FEEDS **/
        @GET("feeds")
        Call<Feeds> feeds();

        @PUT("feeds/{feedId}/read")
        Call<Void> markItemsOfFeedRead(@Path("feedId") int feedId, @Body int newestItemId);

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

        @PUT("items/{itemId}/read")
        Call<Void> markItemRead(@Path("itemId") int itemId);

        @PUT("items/read/multiple")
        Call<Void> markItemsRead(@Body ItemIds items);

        @PUT("items/{itemId}/unread")
        Call<Void> markItemUnread(@Path("itemId") int itemId);

        @PUT("items/unread/multiple")
        Call<Void> markItemsUnread(@Body ItemIds items);

        @PUT("items/{itemId}/{guidHash}/read")
        Call<Void> markItemStarred(@Path("itemId") int itemId, @Path("guidHash") String guidHash);

        @PUT("items/star/multiple")
        Call<Void> markItemsStarred(@Body ItemMap itemMap);

        @PUT("items/{itemId}/{guidHash}/unstarred")
        Call<Void> markItemUnstarred(@Path("itemId") int itemId, @Path("guidHash") String guidHash);

        @PUT("items/unstar/multiple")
        Call<Void> markItemsUnstarred(@Body ItemMap itemMap);
    }

    public void user(final Realm realm, final APICallback callback) {
        api.user().enqueue(new BaseRetrofitCallback<User>(callback) {
            @Override
            protected boolean onResponseReal(Response<User> response) {
                Queries.getInstance().insert(realm, response.body());
                return true;
            }
        });
    }

    public void items(final Realm realm, long lastSync, final APICallback callback) {
        // get all unread items on first sync, updatedItems on further syncs
        if(lastSync == 0L) {
            api.items(-1, 0L, QueryType.ALL.getType(), 0L, false, false).enqueue(new BaseRetrofitCallback<Items>(callback) {
                @Override
                public boolean onResponseReal(Response<Items> response) {
                    final List<Item> items = response.body().getItems();

                    Queries.getInstance().insert(realm, items);

                    return true;
                }
            });
        } else {
            api.updatedItems(lastSync, QueryType.ALL.getType(), 0L).enqueue(new BaseRetrofitCallback<Items>(callback) {
                @Override
                protected boolean onResponseReal(Response<Items> response) {
                    Queries.getInstance().removeExcessItems(realm, MAX_ITEMS);
                    List<Item> items = response.body().getItems();
                    Queries.getInstance().insert(realm, items);
                    return true;
                }
            });
        }
    }

    public void starredItems(final Realm realm, final APICallback callback) {
        api.items(-1, 0L, QueryType.STARRED.getType(), 0L, true, false).enqueue(new BaseRetrofitCallback<Items>(callback) {
            @Override
            protected boolean onResponseReal(Response<Items> response) {
                final List<Item> items = response.body().getItems();

                Queries.getInstance().insert(realm, items);
                return true;
            }
        });
    }

    public void moreItems(final Realm realm, final QueryType type, final long offset, final long id, final APICallback callback) {
        api.items(BATCH_SIZE, offset, type.getType(), id, true, false).enqueue(new BaseRetrofitCallback<Items>(callback) {
            @Override
            public boolean onResponseReal(Response<Items> response) {
                final List<Item> items = response.body().getItems();
                Queries.getInstance().insert(realm, items);
                return true;
            }
        });
    }

    public void folders(final Realm realm, final APICallback callback) {
        api.folders().enqueue(new BaseRetrofitCallback<Folders>(callback) {
            @Override
            public boolean onResponseReal(Response<Folders> response) {
                List<Folder> folders = response.body().getFolders();

                Queries.getInstance().deleteAndInsert(realm, Folder.class, folders);
                return true;
            }
        });
    }

    public void feeds(final Realm realm, final APICallback callback) {
        api.feeds().enqueue(new BaseRetrofitCallback<Feeds>(callback) {
            @Override
            protected boolean onResponseReal(Response<Feeds> response) {
                Feeds feedsBody = response.body();
                List<Feed> feeds = feedsBody.getFeeds();

                Queries.getInstance().deleteAndInsert(realm, Feed.class, feeds);

                return true;
            }
        });
    }

    public interface APICallback {
        void onSuccess();
        void onFailure(String errorMessage);
    }

    private abstract class BaseRetrofitCallback<T> implements Callback<T> {
        final APICallback callback;

        public BaseRetrofitCallback(APICallback callback) {
            this.callback = callback;
        }

        @Override
        public final void onResponse(Call<T> call, Response<T> response) {
            if (response.isSuccess()) {
                if (onResponseReal(response))
                    callback.onSuccess();
            } else {
                callback.onFailure(String.format(Locale.US, "%d: %s", response.code(), response.message()));
            }
        }

        /**
         * Handle the response
         *
         * @param response Retrofit response
         * @return true iff sync event is finished, false iff another run is necessary.
         */
        protected abstract boolean onResponseReal(Response<T> response);

        @Override
        public void onFailure(Call<T> call, Throwable t) {
            callback.onFailure(t.getLocalizedMessage());
        }
    }

}
