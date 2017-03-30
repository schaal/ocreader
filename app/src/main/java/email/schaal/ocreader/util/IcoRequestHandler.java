/*
 * Copyright (C) 2016 Daniel Schaal <daniel@schaal.email>
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

/*
 * Some parts:
 *
 * Copyright (C) 2013 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package email.schaal.ocreader.util;

import android.graphics.Bitmap;
import android.support.annotation.Nullable;
import android.util.Log;

import com.squareup.picasso.Downloader;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Request;
import com.squareup.picasso.RequestHandler;

import java.io.IOException;
import java.io.InputStream;

import okhttp3.CacheControl;
import okhttp3.Response;
import okhttp3.ResponseBody;

import static com.squareup.picasso.Picasso.LoadedFrom.DISK;
import static com.squareup.picasso.Picasso.LoadedFrom.NETWORK;

/**
 * Handle requests for Picasso for ico files
 *
 * Convert ico files containing multiple sizes to multiple images
 */
public class IcoRequestHandler extends RequestHandler {
    private final static String TAG = IcoRequestHandler.class.getName();

    private final Downloader downloader;

    public IcoRequestHandler(Downloader downloader) {
        this.downloader = downloader;
    }

    @Override
    public boolean canHandleRequest(Request data) {
        return data.uri.getLastPathSegment() != null && data.uri.getLastPathSegment().endsWith(".ico");
    }

    @Nullable
    @Override
    public Result load(Request request, int networkPolicy) throws IOException {
        // Set networkPolicy back to 0, for some reason picasso changes it to NetworkPolicy.OFFLINE
        networkPolicy = 0;
        Response response = downloader.load(createRequest(request, networkPolicy));

        final ResponseBody responseBody = response.body();
        final InputStream byteStream = responseBody != null ? responseBody.byteStream(): null;

        if(response.isSuccessful() && byteStream != null) {
            try {
                final Bitmap bitmap = Decoder.decode(byteStream);
                if(bitmap != null) {
                    // Cache response is only null when the response comes fully from the network. Both completely
                    // cached and conditionally cached responses will have a non-null cache response.
                    Picasso.LoadedFrom loadedFrom = response.cacheResponse() == null ? NETWORK : DISK;
                    return new Result(bitmap, loadedFrom);
                }
            } catch (IOException e) {
                Log.e(TAG, "Failed to decode favicon", e);
            } finally {
                try {
                    byteStream.close();
                } catch (IOException e) {
                    Log.e(TAG, "Failed to close favicon byteStream", e);
                }
            }
        }

        return null;
    }

    private static okhttp3.Request createRequest(Request request, int networkPolicy) {
        CacheControl cacheControl = null;
        if (networkPolicy != 0) {
            if (NetworkPolicy.isOfflineOnly(networkPolicy)) {
                cacheControl = CacheControl.FORCE_CACHE;
            } else {
                CacheControl.Builder builder = new CacheControl.Builder();
                if (!NetworkPolicy.shouldReadFromDiskCache(networkPolicy)) {
                    builder.noCache();
                }
                if (!NetworkPolicy.shouldWriteToDiskCache(networkPolicy)) {
                    builder.noStore();
                }
                cacheControl = builder.build();
            }
        }

        okhttp3.Request.Builder builder = new okhttp3.Request.Builder().url(request.uri.toString());
        if (cacheControl != null) {
            builder.cacheControl(cacheControl);
        }
        return builder.build();
    }
}
