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

package email.schaal.ocreader.http;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.X509TrustManager;

import de.duenndns.ssl.MemorizingTrustManager;
import email.schaal.ocreader.Preferences;
import okhttp3.Credentials;
import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Utility class to setup the OkHttpClient and manage the credentials used to communicate with
 * the ownCloud instance.
 */
public class HttpManager {
    private static final String TAG = HttpManager.class.getName();

    private final OkHttpClient client;
    private HostCredentials credentials = null;
    private MemorizingTrustManager mtm;

    private static HttpManager instance;

    public static void init(Context context) {
        instance = new HttpManager(context);
    }

    public static HttpManager getInstance() {

        if(instance == null)
            throw new IllegalArgumentException("HttpManager must be initialized first");
        return instance;
    }

    private HttpManager(Context context) {
        SSLContext sc = null;
        try {
            sc = SSLContext.getInstance("TLS");
            mtm = new MemorizingTrustManager(context);
            sc.init(null, new X509TrustManager[]{mtm}, new java.security.SecureRandom());
        } catch (Exception e) {
        }

        OkHttpClient.Builder clientBuilder = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .addInterceptor(new AuthorizationInterceptor());

        if(sc != null && mtm != null) {
            clientBuilder
                    .sslSocketFactory(sc.getSocketFactory())
                    .hostnameVerifier(mtm.wrapHostnameVerifier(HttpsURLConnection.getDefaultHostnameVerifier()))
                    .build();
        }

        client = clientBuilder.build();

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        String username = Preferences.USERNAME.getString(preferences);

        if(username != null) {
            HttpUrl url = HttpUrl.parse(Preferences.URL.getString(preferences));
            String password = Preferences.PASSWORD.getString(preferences);
            credentials = new HostCredentials(username, password, url);
        }
    }

    public void bindActivity(Activity activity) {
        mtm.bindDisplayActivity(activity);
    }

    public void unbindActivity(Activity activity) {
        mtm.unbindDisplayActivity(activity);
    }

    public HttpUrl getRootUrl() {
        return credentials.getRootUrl();
    }

    public OkHttpClient getClient() {
        return client;
    }

    public void setCredentials(String username, String password, HttpUrl url) {
        this.credentials = new HostCredentials(username, password, url);
    }

    public void persistCredentials(Context context) {
        if(hasCredentials()) {
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
            preferences.edit()
                    .putString(Preferences.USERNAME.getKey(), credentials.getUsername())
                    .putString(Preferences.PASSWORD.getKey(), credentials.getPassword())
                    .putString(Preferences.URL.getKey(), credentials.getRootUrl().toString())
                    .apply();
        }
    }

    public boolean hasCredentials() {
        return credentials != null;
    }

    private class HostCredentials {
        private final String credentials;
        private final String username;
        private final String password;
        private final HttpUrl rootUrl;

        public HostCredentials(String username, String password, HttpUrl url) {
            this.username = username;
            this.password = password;
            this.credentials = Credentials.basic(username, password);
            this.rootUrl = url;
        }

        public String getUsername() {
            return username;
        }

        public String getPassword() {
            return password;
        }

        public String getCredentials() {
            return credentials;
        }

        public HttpUrl getRootUrl() {
            return rootUrl;
        }
    }

    private class AuthorizationInterceptor implements Interceptor {
        public AuthorizationInterceptor() {
        }

        @Override
        public Response intercept(Chain chain) throws IOException {
            Request request = chain.request();

            // only add Authorization header for urls on the configured owncloud host
            if(credentials.getRootUrl().host().equals(request.url().host()))
                request = request.newBuilder()
                        .addHeader("Authorization",credentials.getCredentials())
                        .build();
            return chain.proceed(request);
        }
    }}
