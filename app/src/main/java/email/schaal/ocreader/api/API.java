package email.schaal.ocreader.api;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.github.zafarkhaja.semver.Version;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;

import java.io.IOException;
import java.util.Locale;

import email.schaal.ocreader.authentication.LoginActivity;
import email.schaal.ocreader.Preferences;
import email.schaal.ocreader.R;
import email.schaal.ocreader.api.json.APILevels;
import email.schaal.ocreader.api.json.FeedTypeAdapter;
import email.schaal.ocreader.api.json.FolderTypeAdapter;
import email.schaal.ocreader.api.json.ItemTypeAdapter;
import email.schaal.ocreader.api.json.NewsError;
import email.schaal.ocreader.api.json.NewsStatus;
import email.schaal.ocreader.api.json.NewsStatusTypeAdapter;
import email.schaal.ocreader.api.json.UserTypeAdapter;
import email.schaal.ocreader.database.model.Feed;
import email.schaal.ocreader.database.model.Folder;
import email.schaal.ocreader.database.model.Item;
import email.schaal.ocreader.database.model.User;
import email.schaal.ocreader.http.HttpManager;
import email.schaal.ocreader.service.SyncType;
import email.schaal.ocreader.util.LoginError;
import io.realm.Realm;
import okhttp3.HttpUrl;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.moshi.MoshiConverterFactory;
import retrofit2.http.GET;

/**
 * Base class to implement API interfaces.
 */
public abstract class API {
    private static final String TAG = API.class.getName();
    public static final Version MIN_VERSION = Version.forIntegers(8, 8, 2);

    private static API instance;

    final Level apiLevel;

    final static String API_ROOT = "./index.php/apps/news/api/";
    private final JsonAdapter<NewsError> errorJsonAdapter;

    @Nullable
    private static Account getAccount(final AccountManager accountManager, final String accountName) {
        final Account[] accounts = accountManager.getAccountsByType("email.schaal.ocreader");
        for (Account account : accounts) {
            if(account.name.equals(accountName)) {
                return account;
            }
        }
        return null;
    }

    public static void get(Context context, final InstanceReadyCallback callback) {
        if(instance == null) {
            final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
            final String accountName = Preferences.USERNAME.getString(preferences);
            final AccountManager accountManager = AccountManager.get(context);
            final Account account = getAccount(accountManager, accountName);

            if(account != null) {
                accountManager.getAuthToken(account, "clientflow", null, true, future -> {
                    try {
                        final Bundle result = future.getResult();
                        final String userName = accountManager.getUserData(account, LoginActivity.KEY_USER);
                        final String baseUrl = accountManager.getUserData(account, LoginActivity.KEY_BASE_URL);
                        final String authToken = result.getString(AccountManager.KEY_AUTHTOKEN);

                        API.login(context, HttpUrl.parse(baseUrl), userName, authToken, new APICallback<NewsStatus, LoginError>() {
                            @Override
                            public void onSuccess(NewsStatus success) {
                                callback.onInstanceReady(instance);
                            }

                            @Override
                            public void onFailure(LoginError failure) {
                                if(failure.getSection() == LoginError.Section.USER) {
                                    Log.w(TAG, "Invalidating auth token");
                                    accountManager.invalidateAuthToken("email.schaal.ocreader", authToken);
                                }
                                callback.onLoginFailure(failure.getThrowable());
                            }
                        });
                    } catch (OperationCanceledException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (AuthenticatorException e) {
                        e.printStackTrace();
                    }
                }, null);
            } else {
                // TODO: 10/18/17 Show login notification
            }
        } else {
            callback.onInstanceReady(instance);
        }
    }

    public interface InstanceReadyCallback {
        void onInstanceReady(API api);
        void onLoginFailure(Throwable e);
    }

    final MoshiConverterFactory converterFactory;

    API(Context context, Level apiLevel) {
        this.apiLevel = apiLevel;
        final Moshi moshi = new Moshi.Builder()
                .add(Folder.class, new FolderTypeAdapter())
                .add(Feed.class, new FeedTypeAdapter())
                .add(Item.class, new ItemTypeAdapter())
                .add(User.class, new UserTypeAdapter())
                .add(NewsStatus.class, new NewsStatusTypeAdapter())
                .build();

        converterFactory = MoshiConverterFactory.create(moshi);

        errorJsonAdapter = moshi.adapter(NewsError.class);
    }

    private interface CommonAPI {
        @GET("index.php/apps/news/api")
        Call<APILevels> apiLevels();
    }

    protected abstract void setupApi(HttpManager httpManager);

    protected abstract void metaData(Callback<NewsStatus> callback);

    public abstract void user(final Realm realm, final APICallback<Void, Throwable> apiCallback);

    public abstract void sync(SharedPreferences sharedPreferences, final Realm realm, SyncType syncType, Intent intent, final APICallback<Void, Throwable> apiCallback);

    public abstract void createFeed(final Realm realm, final String url, final long folderId, final APICallback<Void, Throwable> apiCallback);

    public abstract void moveFeed(final Realm realm, final Feed feed, final long folderId, APICallback<Void, Throwable> apiCallback);

    public abstract void deleteFeed(final Realm realm, final Feed feed, APICallback<Void, Throwable> apiCallback);

    // Temporary API instance used to get the metaData when logging in
    private static API loginInstance = null;

    public static void login(final Context context, final HttpUrl baseUrl, final String username, final String password, final APICallback<NewsStatus, LoginError> loginCallback) {
        final HttpManager httpManager = new HttpManager(username, password, baseUrl);

        final HttpUrl resolvedBaseUrl = baseUrl.resolve("");

        if(resolvedBaseUrl == null) {
            loginCallback.onFailure(new LoginError("Couldn't parse URL"));
            return;
        }

        final Moshi moshi = new Moshi.Builder().build();
        final JsonAdapter<NewsError> errorJsonAdapter = moshi.adapter(NewsError.class);

        final Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(resolvedBaseUrl)
                .client(httpManager.getClient())
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build();

        final CommonAPI commonAPI = retrofit.create(CommonAPI.class);

        commonAPI.apiLevels().enqueue(new Callback<APILevels>() {
            @Override
            public void onResponse(@NonNull Call<APILevels> call, @NonNull Response<APILevels> response) {
                if(response.isSuccessful()) {
                    loginInstance = null;

                    final APILevels apiLevels = response.body();
                    final Level apiLevel = apiLevels != null ? apiLevels.highestSupportedApi() : null;

                    loginInstance = Level.getAPI(context, apiLevel);

                    if(apiLevel == null) {
                        loginCallback.onFailure(new LoginError(context.getString(R.string.error_not_compatible)));
                    } else {
                        loginInstance.setupApi(httpManager);

                        loginInstance.metaData(new Callback<NewsStatus>() {
                            @Override
                            public void onResponse(@NonNull Call<NewsStatus> call, @NonNull Response<NewsStatus> response) {
                                if(response.isSuccessful()) {
                                    final NewsStatus status = response.body();
                                    final Version version = status != null ? status.getVersion() : null;
                                    if(version != null && MIN_VERSION.lessThanOrEqualTo(version)) {
                                        instance = loginInstance;
                                        loginInstance = null;

                                        loginCallback.onSuccess(status);
                                    } else {
                                        if(version != null) {
                                            Log.d(TAG, String.format("Nextcloud News version is less than minimally supported version: %s < %s", version.toString(), MIN_VERSION.toString()));
                                            loginCallback.onFailure(new LoginError(context.getString(R.string.ncnews_too_old, MIN_VERSION.toString())));
                                        } else {
                                            Log.d(TAG, "Couldn't parse Nextcloud News version");
                                            loginCallback.onFailure(new LoginError(context.getString(R.string.failed_detect_nc_version)));
                                        }
                                    }
                                } else {
                                    String message = getErrorMessage(errorJsonAdapter, response);
                                    Log.d(TAG, "Metadata call failed with error: " + message);
                                    loginCallback.onFailure(LoginError.getError(context, response.code(), message));
                                }
                            }

                            @Override
                            public void onFailure(@NonNull Call<NewsStatus> call, @NonNull Throwable t) {
                                Log.e(TAG, "Failed to log in", t);
                                loginCallback.onFailure(LoginError.getError(context, t));
                            }
                        });
                    }
                } else {
                    Log.d(TAG, "API level call failed with error: " + response.code() + " " + getErrorMessage(errorJsonAdapter, response));
                    // Either nextcloud news is not installed or version prior 8.8.0
                    loginCallback.onFailure(LoginError.getError(context, response.code(), context.getString(R.string.ncnews_too_old, MIN_VERSION.toString())));
                }
            }

            @Override
            public void onFailure(@NonNull Call<APILevels> call, @NonNull Throwable t) {
                Log.e(TAG, "Failed to log in", t);
                loginCallback.onFailure(LoginError.getError(context, t));
            }
        });
    }

    private static String getErrorMessage(JsonAdapter<NewsError> adapter, Response<?> response) {
        String message = response.message();

        try {
            final ResponseBody errorBody = response.errorBody();
            if(errorBody != null) {
                NewsError error = adapter.fromJson(errorBody.source());
                message = error.message;
            }
        } catch (IOException e) {
            Log.e(TAG, "Failed to get error message", e);
        }

        return message;
    }

    public interface APICallback<S,F> {
        void onSuccess(S success);
        void onFailure(F failure);
    }

    abstract class BaseRetrofitCallback<T> implements Callback<T> {
        @Nullable
        final APICallback<Void, Throwable> callback;

        BaseRetrofitCallback(@Nullable APICallback<Void, Throwable> callback) {
            this.callback = callback;
        }

        @Override
        public final void onResponse(@NonNull Call<T> call, @NonNull Response<T> response) {
            if (response.isSuccessful()) {
                onResponseReal(response);

                if (callback != null) {
                    callback.onSuccess(null);
                }
            } else {
                String message = getErrorMessage(errorJsonAdapter, response);
                if (callback != null) {
                    //TODO: better exception type
                    callback.onFailure(new Exception(String.format(Locale.US, "%d: %s", response.code(), message)));
                }
            }
        }

        /**
         * Handle the response
         *
         * @param response Retrofit response
         */
        protected abstract void onResponseReal(Response<T> response);

        @Override
        public void onFailure(@NonNull Call<T> call, @NonNull Throwable t) {
            Log.e(TAG, "Retrofit call failed", t);
            if(callback != null)
                callback.onFailure(t);
        }
    }
}
