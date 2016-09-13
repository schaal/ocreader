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

package email.schaal.ocreader.model;

import android.text.TextUtils;
import android.util.Log;

import com.squareup.moshi.JsonReader;
import com.squareup.moshi.JsonWriter;

import java.io.IOException;

import email.schaal.ocreader.util.StringUtils;

/**
 * TypeAdapter to deserialize the JSON response for Feeds.
 */
public class FeedTypeAdapter extends NewsTypeAdapter<Feed> {
    private final static String TAG = FeedTypeAdapter.class.getName();

    @Override
    public void toJson(JsonWriter out, Feed feed) throws IOException {
        out.beginObject();

        // Only write url for new feeds (id < 0)
        if(feed.getId() >= 0) {
            out.name("url");
            out.value(feed.getUrl());
        }

        out.name("folderId");
        out.value(feed.getFolderId());

        out.endObject();
    }

    @Override
    public Feed fromJson(JsonReader in) throws IOException {
        if (in.peek() == JsonReader.Token.NULL) {
            in.nextNull();
            return null;
        }
        Feed feed = new Feed();
        in.beginObject();
        while (in.hasNext()) {
            String name = in.nextName();
            switch (name) {
                case "id":
                    feed.setId(in.nextLong());
                    break;
                case "url":
                    feed.setUrl(nullSafeString(in));
                    break;
                case "link":
                    feed.setLink(nullSafeString(in));
                    break;
                case "faviconLink":
                    String faviconLink = nullSafeString(in);
                    if (faviconLink != null && TextUtils.getTrimmedLength(faviconLink) == 0)
                        faviconLink = null;
                    feed.setFaviconLink(faviconLink);
                    break;
                // title in API v1-2, name in API v2
                case "title":
                case "name":
                    feed.setName(StringUtils.cleanString(in.nextString()));
                    break;
                case "added":
                    in.skipValue();
                    break;
                case "folderId":
                    feed.setFolderId(in.nextLong());
                    break;
                case "unreadCount":
                    feed.setUnreadCount(in.nextInt());
                    break;
                case "ordering":
                    // createFeed returns a feed with ordering set to NULL
                    feed.setOrdering(nullSafeInt(in, 0));
                    break;
                case "pinned":
                    // createFeed returns a feed with pinned set to NULL
                    feed.setPinned(nullSafeBoolean(in, false));
                    break;
                case "updateErrorCount":
                    feed.setUpdateErrorCount(nullSafeInt(in, 0));
                    break;
                case "lastUpdateError":
                    feed.setLastUpdateError(nullSafeString(in));
                    break;
                case "error":
                    parseError(in, feed);
                    break;
                default:
                    Log.w(TAG, "Unknown value in feed json: " + name);
                    in.skipValue();
                    break;
            }
        }
        in.endObject();
        return feed;
    }

    private void parseError(JsonReader in, Feed feed) throws IOException {
        in.beginObject();
        while(in.hasNext()) {
            switch (in.nextName()) {
                case "code":
                    in.skipValue();
                    break;
                case "message":
                    feed.setLastUpdateError(nullSafeString(in));
                    break;
            }
        }
        in.endObject();
    }
}
