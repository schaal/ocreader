package email.schaal.ocreader.api;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import java.util.Map;

import email.schaal.ocreader.Preferences;
import email.schaal.ocreader.api.json.Feeds;
import email.schaal.ocreader.api.json.Items;
import email.schaal.ocreader.api.json.Status;
import email.schaal.ocreader.api.json.v2.SyncResponse;
import email.schaal.ocreader.database.Queries;
import email.schaal.ocreader.database.model.Feed;
import email.schaal.ocreader.database.model.Folder;
import email.schaal.ocreader.database.model.Item;
import email.schaal.ocreader.http.HttpManager;
import email.schaal.ocreader.service.SyncService;
import email.schaal.ocreader.service.SyncType;
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
        super(context, Level.V2);
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
    public void user(final Realm realm, APICallback<Void, Throwable> apiCallback) {
        api.metaData().enqueue(new BaseRetrofitCallback<Status>(apiCallback) {
            @Override
            protected void onResponseReal(Response<Status> response) {
                final Status status = response.body();
                if(status != null)
                    realm.executeTransaction(realm1 -> Queries.insert(realm1, status.getUser()));
            }
        });
    }

    @Override
    public void sync(final SharedPreferences sharedPreferences, final Realm realm, SyncType syncType, Intent intent, APICallback<Void, Throwable> apiCallback) {
        // TODO: 02.10.16 sync user
        final BaseRetrofitCallback<SyncResponse> retrofitCallback = new BaseRetrofitCallback<SyncResponse>(apiCallback) {
            @Override
            protected void onResponseReal(Response<SyncResponse> response) {
                final SyncResponse syncResponse = response.body();

                if(syncResponse != null) {
                    sharedPreferences.edit().putString(Preferences.SYS_APIv2_ETAG.getKey(), response.headers().get("Etag")).apply();

                    realm.executeTransaction(realm1 -> {
                        Queries.deleteAndInsert(realm1, Folder.class, syncResponse.getFolders());
                        Queries.deleteAndInsert(realm1, Feed.class, syncResponse.getFeeds());
                        Queries.insert(realm1, syncResponse.getItems());
                    });
                }
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
                    items.setItems(realm.where(Item.class).equalTo("unreadChanged", true).or().equalTo("starredChanged", true).findAll());

                    api.sync(etag, items).enqueue(retrofitCallback);
                }
                break;
            case LOAD_MORE:
                // TODO: 06.09.16
                apiCallback.onFailure(new UnsupportedOperationException("not implemented in APIv2"));
                break;
        }
    }

    @Override
    public void createFeed(final Realm realm, String url, long folderId, APICallback<Void, Throwable> apiCallback) {
        final Feed feed = new Feed();

        feed.setUrl(url);
        feed.setFolderId(folderId);

        api.createFeed(feed).enqueue(new BaseRetrofitCallback<Feeds>(apiCallback) {
            @Override
            protected void onResponseReal(final Response<Feeds> response) {
                final Feeds feeds = response.body();

                if(feeds != null) {
                    // Set unreadCount to 0, items have not been fetched yet for this feed
                    Feed feed = feeds.getFeeds().get(0);
                    feed.setUnreadCount(0);

                    realm.executeTransaction(realm1 -> Queries.insert(realm1, feed));
                }
            }
        });
    }

    @Override
    public void moveFeed(final Realm realm, final Feed feed, final long folderId, APICallback<Void, Throwable> apiCallback) {
        final Feed changedFeed = new Feed();
        changedFeed.setUrl(feed.getUrl());
        changedFeed.setFolderId(feed.getFolderId());

        api.changeFeed(feed.getId(), changedFeed).enqueue(new BaseRetrofitCallback<Map<String,Feed>>(apiCallback) {
            @Override
            protected void onResponseReal(Response<Map<String,Feed>> response) {
                final Map<String, Feed> feedMap = response.body();

                if(feedMap != null)
                    realm.executeTransaction(realm1 -> Queries.insert(realm1, feedMap.get("feed")));
            }
        });
    }

    @Override
    public void deleteFeed(final Realm realm, final Feed feed, APICallback<Void, Throwable> apiCallback) {
        api.deleteFeed(feed.getId()).enqueue(new BaseRetrofitCallback<Void>(apiCallback) {
            @Override
            protected void onResponseReal(Response<Void> response) {
                realm.executeTransaction(feed::delete);
            }
        });
    }
}
