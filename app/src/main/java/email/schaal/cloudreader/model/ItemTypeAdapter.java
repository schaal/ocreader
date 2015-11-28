/*
 * Copyright (C) 2015 Daniel Schaal <daniel@schaal.email>
 *
 * This file is part of Cloudreader.
 *
 * Cloudreader is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Cloudreader is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Cloudreader.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package email.schaal.cloudreader.model;

import android.util.Log;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.util.Date;

import email.schaal.cloudreader.util.StringUtils;

/**
 * Created by daniel on 10.11.15.
 */
public class ItemTypeAdapter extends TypeAdapter<Item> {
    private final static String TAG = ItemTypeAdapter.class.getSimpleName();

    @Override
    public void write(JsonWriter out, Item value) throws IOException {
    }

    @Override
    public Item read(JsonReader in) throws IOException {
        if (in.peek() == JsonToken.NULL) {
            in.nextNull();
            return null;
        }
        Item item = new Item();
        in.beginObject();
        while (in.hasNext()) {
            String name = in.nextName();
            switch (name) {
                case "id":
                    item.setId(in.nextLong());
                    break;
                case "guid":
                    item.setGuid(in.nextString());
                    break;
                case "guidHash":
                    item.setGuidHash(in.nextString());
                    break;
                case "url":
                    item.setUrl(in.nextString());
                    break;
                case "title":
                    item.setTitle(StringUtils.cleanString(in.nextString()));
                    break;
                case "author":
                    item.setAuthor(StringUtils.nullIfEmpty(in.nextString()));
                    break;
                case "pubDate":
                    item.setPubDate(new Date(in.nextLong() * 1000));
                    break;
                case "body":
                    item.setBody(in.nextString());
                    break;
                case "enclosureMime":
                    if(in.peek() != JsonToken.NULL)
                        item.setEnclosureMime(StringUtils.nullIfEmpty(in.nextString()));
                    else
                        in.skipValue();
                    break;
                case "enclosureLink":
                    if(in.peek() != JsonToken.NULL)
                        item.setEnclosureLink(StringUtils.nullIfEmpty(in.nextString()));
                    else
                        in.skipValue();
                    break;
                case "feedId":
                    item.setFeedId(in.nextLong());
                    break;
                case "unread":
                    item.setUnread(in.nextBoolean());
                    break;
                case "starred":
                    item.setStarred(in.nextBoolean());
                    break;
                case "lastModified":
                    item.setLastModified(new Date(in.nextLong() * 1000));
                    break;
                case "rtl":
                    in.skipValue();
                    break;
                default:
                    Log.w(TAG, "Unknown value in item json: " + name);
                    in.skipValue();
                    break;
            }
        }
        in.endObject();
        return item;
    }
}
