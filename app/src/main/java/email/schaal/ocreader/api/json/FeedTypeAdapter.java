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

package email.schaal.ocreader.api.json;

import androidx.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.JsonReader;
import com.squareup.moshi.JsonWriter;

import java.io.IOException;

import email.schaal.ocreader.database.model.Feed;
import email.schaal.ocreader.util.StringUtils;
import io.realm.RealmObject;

/**
 * TypeAdapter to deserialize the JSON response for Feeds.
 */
public class FeedTypeAdapter extends JsonAdapter<Feed> {
    private final static String TAG = FeedTypeAdapter.class.getName();

    @Override
    public void toJson(@NonNull JsonWriter out, Feed feed) throws IOException {
        out.beginObject();

        // Only write url for feeds that are not in the database
        if(!RealmObject.isManaged(feed)) {
            out.name("url");
            out.value(feed.getUrl());
        }

        out.name("folderId");
        out.value(feed.getFolderId());

        out.endObject();
    }

    @Override
    public Feed fromJson(@NonNull JsonReader in) throws IOException {
        if (in.peek() == JsonReader.Token.NULL) {
            in.nextNull();
            return null;
        }

        final NullableJsonReader reader = new NullableJsonReader(in);

        final Feed feed = new Feed();

        in.beginObject();

        while (in.hasNext()) {
            String name = in.nextName();
            switch (name) {
                case "id":
                    feed.setId(in.nextLong());
                    break;
                case "url":
                    feed.setUrl(reader.nextString());
                    break;
                case "link":
                    feed.setLink(reader.nextString());
                    break;
                case "faviconLink":
                    String faviconLink = reader.nextString();
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
                    // skip unreadCount, calculated later in SyncService
                    in.skipValue();
                    break;
                case "ordering":
                    // createFeed returns a feed with ordering set to NULL
                    feed.setOrdering(reader.nextInt(0));
                    break;
                case "pinned":
                    // createFeed returns a feed with pinned set to NULL
                    feed.setPinned(reader.nextBoolean(false));
                    break;
                case "updateErrorCount":
                    feed.setUpdateErrorCount(reader.nextInt(0));
                    break;
                case "lastUpdateError":
                    feed.setLastUpdateError(reader.nextString());
                    break;
                case "error":
                    parseError(reader, feed);
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

    private void parseError(NullableJsonReader reader, Feed feed) throws IOException {
        reader.in.beginObject();
        while(reader.in.hasNext()) {
            switch (reader.in.nextName()) {
                case "code":
                    reader.in.skipValue();
                    break;
                case "message":
                    feed.setLastUpdateError(reader.nextString());
                    break;
            }
        }
        reader.in.endObject();
    }
}
