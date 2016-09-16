package email.schaal.ocreader.api;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import email.schaal.ocreader.Preferences;
import email.schaal.ocreader.api.json.APILevels;
import email.schaal.ocreader.api.json.Feeds;
import email.schaal.ocreader.api.json.Items;
import email.schaal.ocreader.api.json.Status;
import email.schaal.ocreader.api.json.v2.SyncResponse;
import email.schaal.ocreader.database.Queries;
import email.schaal.ocreader.http.HttpManager;
import email.schaal.ocreader.model.Feed;
import email.schaal.ocreader.model.Folder;
import email.schaal.ocreader.model.Item;
import email.schaal.ocreader.service.SyncService;
import io.realm.Realm;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.Path;

/**
 * Class to wrap Nextcloud news API v2
 */
class APIv2 extends API {
    APIv2(Context context) {
        super(context, APILevels.Level.V2);
    }

    private interface APIv2Interface {
        @GET("sync")
        Call<SyncResponse> sync();

        @POST("sync")
        Call<SyncResponse> sync(@Header("If-None-Match") String etag, @Body Items items);

        @POST("feeds")
        Call<Feeds> createFeed(@Body Feed feed);

        @PATCH("feeds/{feedId}")
        Call<Map<String,Feed>> changeFeed(@Path("feedId") long feedId, @Body Feed feed);

        @DELETE("feeds/{feedId}")
        Call<Void> deleteFeed(@Path("feedId") long feedId);

        @GET("./")
        Call<Status> metaData();
    }

    private APIv2Interface api;

    @Override
    protected void setupApi(HttpManager httpManager) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(httpManager.getCredentials().getRootUrl().resolve(String.format("%s%s/", API_ROOT, apiLevel.getLevel())))
                .client(httpManager.getClient())
                .addConverterFactory(converterFactory)
                .build();

        api = retrofit.create(APIv2Interface.class);
    }

    @Override
    protected void metaData(Callback<Status> callback) {
        api.metaData().enqueue(callback);
    }

    @Override
    public void user(final Realm realm, APICallback<Void, String> apiCallback) {
        api.metaData().enqueue(new BaseRetrofitCallback<Status>(apiCallback) {
            @Override
            protected boolean onResponseReal(Response<Status> response) {
                Queries.insert(realm, response.body().getUser());
                return true;
            }
        });
    }

    @Override
    public void sync(final SharedPreferences sharedPreferences, final Realm realm, SyncService.SyncType syncType, Intent intent, APICallback<Void, String> apiCallback) {
        final BaseRetrofitCallback<SyncResponse> retrofitCallback = new BaseRetrofitCallback<SyncResponse>(apiCallback) {
            @Override
            protected boolean onResponseReal(Response<SyncResponse> response) {
                sharedPreferences.edit().putString(Preferences.SYS_APIv2_ETAG.getKey(), response.headers().get("Etag")).apply();

                Queries.deleteAndInsert(realm, Folder.class, response.body().getFolders());
                Queries.deleteAndInsert(realm, Feed.class, response.body().getFeeds());
                Queries.insert(realm, response.body().getItems());
                return true;
            }
        };

        if(intent.getBooleanExtra(SyncService.EXTRA_INITIAL_SYNC, false))
            sharedPreferences.edit().remove(Preferences.SYS_APIv2_ETAG.getKey()).apply();

        switch (syncType) {
            case FULL_SYNC:
            case SYNC_CHANGES_ONLY:
                final String etag = Preferences.SYS_APIv2_ETAG.getString(sharedPreferences);

                if(etag == null) {
                    api.sync().enqueue(retrofitCallback);
                } else {
                    final Items items = new Items();
                    items.setItems(realm.where(Item.class).equalTo(Item.UNREAD_CHANGED, true).or().equalTo(Item.STARRED_CHANGED, true).findAll());

                    api.sync(etag, items).enqueue(retrofitCallback);
                }
                break;
            case LOAD_MORE:
                // TODO: 06.09.16
                apiCallback.onFailure("not implemented");
                break;
        }
    }

    @Override
    public void createFeed(final Realm realm, String url, long folderId, APICallback<Void, String> apiCallback) {
        final Feed feed = new Feed();

        feed.setUrl(url);
        feed.setFolderId(folderId);

        api.createFeed(feed).enqueue(new BaseRetrofitCallback<Feeds>(apiCallback) {
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
        final Feed changedFeed = new Feed();
        changedFeed.setUrl(feed.getUrl());
        changedFeed.setFolderId(feed.getFolderId());

        api.changeFeed(feed.getId(), changedFeed).enqueue(new BaseRetrofitCallback<Map<String,Feed>>(apiCallback) {
            @Override
            protected boolean onResponseReal(Response<Map<String,Feed>> response) {
                Queries.insert(realm, response.body().get("feed"));
                return true;
            }
        });
    }

    @Override
    public void deleteFeed(final Realm realm, final Feed feed, APICallback<Void, String> apiCallback) {
        api.deleteFeed(feed.getId()).enqueue(new BaseRetrofitCallback<Void>(apiCallback) {
            @Override
            protected boolean onResponseReal(Response<Void> response) {
                Queries.deleteFeed(realm, feed);
                return true;
            }
        });
    }
}
