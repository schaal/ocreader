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
package email.schaal.ocreader.http

import okhttp3.*
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Utility class to setup the OkHttpClient and manage the credentials used to communicate with
 * the ownCloud instance.
 */
class HttpManager(username: String, password: String, url: HttpUrl) {
    val client: OkHttpClient
    val credentials: HostCredentials

    inner class HostCredentials constructor(username: String, password: String, url: HttpUrl) {
        val credentials: String = Credentials.basic(username, password)
        val rootUrl: HttpUrl = url
    }

    private inner class AuthorizationInterceptor internal constructor() : Interceptor {
        @Throws(IOException::class)
        override fun intercept(chain: Interceptor.Chain): Response {
            var request = chain.request()
            // only add Authorization header for urls on the configured owncloud host
            if (credentials.rootUrl.host == request.url.host) request = request.newBuilder()
                    .addHeader("Authorization", credentials.credentials)
                    .build()
            return chain.proceed(request)
        }
    }

    init {
        client = OkHttpClient.Builder()
                .connectTimeout(20, TimeUnit.SECONDS)
                .readTimeout(1, TimeUnit.HOURS)
                .addInterceptor(AuthorizationInterceptor())
                .build()
        credentials = HostCredentials(username, password, url)
    }
}