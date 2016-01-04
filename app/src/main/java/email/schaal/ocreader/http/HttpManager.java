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

package email.schaal.ocreader.http;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.squareup.okhttp.Credentials;
import com.squareup.okhttp.HttpUrl;
import com.squareup.okhttp.Interceptor;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import email.schaal.ocreader.Preferences;

/**
 * Utility class to setup the OkHttpClient and manage the credentials used to communicate with
 * the ownCloud instance.
 */
public class HttpManager {
    private static final String TAG = HttpManager.class.getSimpleName();

    private final OkHttpClient client;
    private HostCredentials credentials = null;

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

        client = new OkHttpClient();

        client.setConnectTimeout(10, TimeUnit.SECONDS);
        client.setReadTimeout(60, TimeUnit.SECONDS);

        client.interceptors().add(new AuthorizationInterceptor());

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        String username = Preferences.USERNAME.getString(preferences);

        if(username != null) {
            HttpUrl url = HttpUrl.parse(Preferences.URL.getString(preferences));
            String password = Preferences.PASSWORD.getString(preferences);
            credentials = new HostCredentials(username, password, url);
        }
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
            if(credentials.getRootUrl().host().equals(request.httpUrl().host()))
                request = request.newBuilder()
                        .addHeader("Authorization",credentials.getCredentials())
                        .build();
            return chain.proceed(request);
        }
    }}
