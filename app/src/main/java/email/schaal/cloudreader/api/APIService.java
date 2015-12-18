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

package email.schaal.cloudreader.api;

import android.content.Context;
import android.widget.Toast;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.squareup.okhttp.HttpUrl;
import com.squareup.picasso.Picasso;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import email.schaal.cloudreader.R;
import email.schaal.cloudreader.database.Queries;
import email.schaal.cloudreader.http.HttpManager;
import email.schaal.cloudreader.model.ChangedItems;
import email.schaal.cloudreader.model.Feed;
import email.schaal.cloudreader.model.FeedTypeAdapter;
import email.schaal.cloudreader.model.Folder;
import email.schaal.cloudreader.model.FolderTypeAdapter;
import email.schaal.cloudreader.model.Item;
import email.schaal.cloudreader.model.ItemTypeAdapter;
import email.schaal.cloudreader.model.User;
import email.schaal.cloudreader.model.UserTypeAdapter;
import email.schaal.cloudreader.model.Version;
import io.realm.Realm;
import io.realm.RealmList;
import io.realm.RealmObject;
import io.realm.RealmResults;
import retrofit.Call;
import retrofit.Callback;
import retrofit.GsonConverterFactory;
import retrofit.Response;
import retrofit.Retrofit;
import retrofit.http.Body;
import retrofit.http.GET;
import retrofit.http.PUT;
import retrofit.http.Path;
import retrofit.http.Query;

/**
 * Created by daniel on 08.11.15.
 */
public class APIService {
    private static final String TAG = APIService.class.getSimpleName();

    private static final String ROOT_PATH_APIv1_2 = "./index.php/apps/news/api/v1-2/";

    private static final int BATCH_SIZE = 100;
    private static final int MAX_ITEMS = 10000;

    private final Context context;
    private final Gson gson;

    public void syncChanges() {
        Realm realm = null;
        try {
            realm = Realm.getDefaultInstance();

            for(MarkAction action: MarkAction.values()) {
                markItems(realm, action);
            }

        } finally {
            if(realm != null)
                realm.close();
        }

    }

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

    private void markItems(Realm realm, MarkAction action) {
        RealmResults<Item> results;
        final RealmList<Item> items;

        if(action.getKey().equals(Item.UNREAD))
            items = realm.where(ChangedItems.class).findFirst().getUnreadChangedItems();
        else
            items = realm.where(ChangedItems.class).findFirst().getStarredChangedItems();

        results = items.where().equalTo(action.getKey(), action.getValue()).findAll();

        int size = results.size();

        // Nothing to do, just return
        if(size == 0)
            return;

        ItemMap itemMap = new ItemMap();
        itemMap.put(results);

        ItemIds ids = null;
        if(action == MarkAction.MARK_UNREAD || action == MarkAction.MARK_READ) {
            ids = new ItemIds();
            ids.setItems(itemMap.getKeys());
        }

        try {
            switch (action) {
                case MARK_READ:
                    api.markItemsRead(ids).execute();
                    break;
                case MARK_UNREAD:
                    api.markItemsUnread(ids).execute();
                    break;
                case MARK_STARRED:
                    api.markItemsStarred(itemMap).execute();
                    break;
                case MARK_UNSTARRED:
                    api.markItemsUnstarred(itemMap).execute();
                    break;
            }
            realm.executeTransaction(new Realm.Transaction() {
                @Override
                public void execute(Realm realm) {
                    items.clear();
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
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
    private Version version;

    public static void init(Context context) {
        instance = new APIService(context);
    }

    public static APIService getInstance() {
        if(instance == null)
            throw new IllegalStateException("initialize first");
        return instance;
    }

    private APIService(Context context) {
        this.context = context;

        gson = new GsonBuilder()
                .registerTypeAdapter(Folder.class, new FolderTypeAdapter())
                .registerTypeAdapter(Feed.class, new FeedTypeAdapter())
                .registerTypeAdapter(Item.class, new ItemTypeAdapter())
                .registerTypeAdapter(User.class, new UserTypeAdapter())
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

    @SuppressWarnings("SameParameterValue")
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
        @GET("items?batchSize="+BATCH_SIZE)
        Call<Items> items(
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

        @PUT("items/starred/multiple")
        Call<Void> markItemsStarred(@Body ItemMap... itemIds);

        @PUT("items/{itemId}/{guidHash}/unstarred")
        Call<Void> markItemUnstarred(@Path("itemId") int itemId, @Path("guidHash") String guidHash);

        @PUT("items/unstarred/multiple")
        Call<Void> markItemsUnstarred(@Body ItemMap... itemIds);
    }

    public void user(final APICallback callback) {
        api.user().enqueue(new RealmRetrofitCallback<User>(callback) {
            @Override
            protected boolean onResponseReal(Realm realm, Response<User> response) {
                Queries.getInstance().insert(realm, response.body());
                return true;
            }
        });
    }

    public void items(long lastSync, QueryType type, final APICallback callback) {
        items(lastSync, type, 0, 0, callback);
    }

    public void items(long lastSync, final QueryType type, final long offset, final long id, final APICallback callback) {
        // get all unread items on first sync, updatedItems on further syncs
        if(lastSync == 0) {
            api.items(offset, type.getType(), id, false, false).enqueue(new RealmRetrofitCallback<Items>(callback) {
                private int totalCount = 0;

                @Override
                public boolean onResponseReal(Realm realm, Response<Items> response) {
                    final List<Item> items = response.body().getItems();
                    totalCount += items.size();

                    Queries.getInstance().insert(realm, items);

                    if (items.size() == BATCH_SIZE) {
                        Toast.makeText(context, context.getResources().getQuantityString(R.plurals.downloaded_x_items, totalCount, totalCount), Toast.LENGTH_SHORT).show();
                        long newOffset = realm.where(Item.class).min(Item.ID).longValue();
                        api.items(newOffset, type.getType(), id, false, false).enqueue(this);
                        return false;
                    } else {
                        return true;
                    }
                }
            });
        } else {
            api.updatedItems(lastSync, type.getType(), id).enqueue(new RealmRetrofitCallback<Items>(callback) {
                @Override
                protected boolean onResponseReal(Realm realm, Response<Items> response) {
                    Queries.getInstance().removeExcessItems(realm, MAX_ITEMS);
                    List<Item> items = response.body().getItems();
                    Queries.getInstance().insert(realm, items);
                    return true;
                }
            });
        }
    }

    public void folders(final APICallback callback) {
        api.folders().enqueue(new RealmRetrofitCallback<Folders>(callback) {
            @Override
            public boolean onResponseReal(Realm realm, Response<Folders> response) {
                List<Folder> folders = response.body().getFolders();

                Queries.getInstance().deleteAndInsert(realm, Folder.class, folders);
                return true;
            }
        });
    }

    public void feeds(final APICallback callback, final boolean cacheFavicons) {
        api.feeds().enqueue(new RealmRetrofitCallback<Feeds>(callback) {
            @Override
            protected boolean onResponseReal(Realm realm, Response<Feeds> response) {
                Feeds feedsBody = response.body();
                List<Feed> feeds = feedsBody.getFeeds();

                Queries.getInstance().deleteAndInsert(realm, Feed.class, feeds);

                if(cacheFavicons) {
                    for(final Feed feed: feeds) {
                        if(feed.getFaviconLink() != null)
                            Picasso.with(context).load(feed.getFaviconLink()).fetch();
                    }
                }

                return true;
            }
        });
    }

    public interface APICallback {
        void onSuccess();
        void onFailure(String errorMessage);
    }

    private abstract class RealmRetrofitCallback<T> implements Callback<T> {
        final APICallback callback;

        public RealmRetrofitCallback(APICallback callback) {
            this.callback = callback;
        }

        @Override
        public final void onResponse(Response<T> response, Retrofit retrofit) {
            Realm realm = null;
            try {
                if(response.isSuccess()) {
                    realm = Realm.getDefaultInstance();
                    if(onResponseReal(realm, response))
                        callback.onSuccess();
                } else {
                    callback.onFailure(String.format("%d: %s", response.code(), response.message()));
                }
            } catch(Exception ex) {
                callback.onFailure(ex.getLocalizedMessage());
            } finally {
                if(realm != null)
                    realm.close();
            }
        }

        /**
         * Handle the response
         *
         * @param realm Realm to insert/update items
         * @param response Retrofit response
         * @return true iff sync event is finished, false iff another run is necessary.
         */
        protected abstract boolean onResponseReal(Realm realm, Response<T> response);

        @Override
        public void onFailure(Throwable t) {
            callback.onFailure(t.getLocalizedMessage());
        }
    }

    private static class ItemMap {
        private Map<Long, String> items = new HashMap<>();

        public Map<Long, String> getItems() {
            return items;
        }

        public void setItems(Map<Long, String> items) {
            this.items = items;
        }

        public Set<Long> getKeys() {
            return items.keySet();
        }

        public void put(Iterable<Item> items) {
            for(Item item: items) {
                this.items.put(item.getId(), item.getGuidHash());
            }
        }
    }

    private static class ItemIds {
        private Set<Long> items;

        public Set<Long> getItems() {
            return items;
        }

        public void setItems(Set<Long> items) {
            this.items = items;
        }
    }

    private static class Folders {
        public List<Folder> getFolders() {
            return folders;
        }

        public void setFolders(List<Folder> folders) {
            this.folders = folders;
        }

        private List<Folder> folders;
    }

    private static class Feeds {
        private List<Feed> feeds;
        private int starredCount;
        private Long newestItemId;

        public int getStarredCount() {
            return starredCount;
        }

        public void setStarredCount(int starredCount) {
            this.starredCount = starredCount;
        }

        public Long getNewestItemId() {
            return newestItemId;
        }

        public void setNewestItemId(Long newestItemId) {
            this.newestItemId = newestItemId;
        }

        public List<Feed> getFeeds() {
            return feeds;
        }

        public void setFeeds(List<Feed> feeds) {
            this.feeds = feeds;
        }
    }

    private static class Items {
        private List<Item> items;

        public List<Item> getItems() {
            return items;
        }

        public void setItems(List<Item> items) {
            this.items = items;
        }
    }
}
