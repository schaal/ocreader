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

import android.support.annotation.NonNull;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

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

    private final OkHttpClient client;
    private HostCredentials credentials = null;

    public HttpManager(String username, String password, HttpUrl url) {
        client = new OkHttpClient.Builder()
                .connectTimeout(20, TimeUnit.SECONDS)
                .readTimeout(1, TimeUnit.HOURS)
                .addInterceptor(new AuthorizationInterceptor())
                .build();

        credentials = new HostCredentials(username, password, url);
    }

    public HostCredentials getCredentials() {
        return credentials;
    }

    public OkHttpClient getClient() {
        return client;
    }

    public class HostCredentials {
        private final String credentials;
        private final HttpUrl rootUrl;

        private HostCredentials(String username, String password, HttpUrl url) {
            this.credentials = Credentials.basic(username, password);
            this.rootUrl = url;
        }

        String getCredentials() {
            return credentials;
        }

        public HttpUrl getRootUrl() {
            return rootUrl;
        }
    }

    private class AuthorizationInterceptor implements Interceptor {
        AuthorizationInterceptor() {
        }

        @Override
        public Response intercept(@NonNull Chain chain) throws IOException {
            Request request = chain.request();

            // only add Authorization header for urls on the configured owncloud host
            if(credentials.getRootUrl().host().equals(request.url().host()))
                request = request.newBuilder()
                        .addHeader("Authorization",credentials.getCredentials())
                        .build();
            return chain.proceed(request);
        }
    }}
