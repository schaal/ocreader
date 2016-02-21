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

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;

import email.schaal.ocreader.util.StringUtils;

/**
 * TypeAdapter to deserialize the JSON response for Feeds.
 */
public class FeedTypeAdapter extends NewsTypeAdapter<Feed> {
    private final static String TAG = FeedTypeAdapter.class.getName();

    @Override
    public void write(JsonWriter out, Feed value) throws IOException {
    }

    @Override
    public Feed read(JsonReader in) throws IOException {
        if (in.peek() == JsonToken.NULL) {
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
                case "title":
                    feed.setTitle(StringUtils.cleanString(in.nextString()));
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
                    feed.setOrdering(in.nextInt());
                    break;
                case "pinned":
                    feed.setPinned(in.nextBoolean());
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
}
