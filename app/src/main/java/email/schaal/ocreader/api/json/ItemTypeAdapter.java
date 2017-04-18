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

import android.support.annotation.Nullable;
import android.util.Log;

import com.google.common.base.Strings;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.JsonReader;
import com.squareup.moshi.JsonWriter;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import email.schaal.ocreader.database.model.Item;
import email.schaal.ocreader.util.StringUtils;

/**
 * TypeAdapter to deserialize the JSON response for feed Items.
 */
public class ItemTypeAdapter extends JsonAdapter<Item> {
    private final static String TAG = ItemTypeAdapter.class.getName();

    @Override
    public void toJson(JsonWriter out, Item item) throws IOException {
        out.beginObject();

        out.name(Item.ID).value(item.getId());
        out.name(Item.CONTENT_HASH).value(item.getContentHash());

        if(item.isUnreadChanged())
            out.name("isUnread").value(item.isUnread());

        if(item.isStarredChanged())
            out.name("isStarred").value(item.isStarred());

        out.endObject();
    }

    @Override
    public Item fromJson(JsonReader in) throws IOException {
        if (in.peek() == JsonReader.Token.NULL) {
            in.nextNull();
            return null;
        }

        final NullableJsonReader reader = new NullableJsonReader(in);

        final Item.Builder item = new Item.Builder();

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
                    item.setUrl(reader.nextString());
                    break;
                case "title":
                    item.setTitle(StringUtils.cleanString(in.nextString()));
                    break;
                case "author":
                    item.setAuthor(Strings.emptyToNull(in.nextString()));
                    break;
                case "pubDate":
                    item.setPubDate(new Date(in.nextLong() * 1000));
                    break;
                case "body":
                    item.setBody(in.nextString());
                    break;
                case "enclosureMime":
                    if(in.peek() != JsonReader.Token.NULL)
                        item.setEnclosureMime(Strings.emptyToNull(in.nextString()));
                    else
                        in.skipValue();
                    break;
                case "enclosureLink":
                    if(in.peek() != JsonReader.Token.NULL)
                        item.setEnclosureLink(Strings.emptyToNull(in.nextString()));
                    else
                        in.skipValue();
                    break;
                case "publishedAt":
                    item.setPubDate(parseDate(in.nextString()));
                    break;
                case "updatedAt":
                    item.setUpdatedAt(parseDate(in.nextString()));
                    break;
                case "enclosure":
                    parseEnclosure(reader, item);
                    break;
                case "feedId":
                    item.setFeedId(in.nextLong());
                    break;
                case "isUnread":
                case "unread":
                    item.setUnread(in.nextBoolean());
                    break;
                case "starred":
                case "isStarred":
                    item.setStarred(in.nextBoolean());
                    break;
                case "lastModified":
                    item.setLastModified(in.nextLong());
                    break;
                case "rtl":
                    in.skipValue();
                    break;
                case "fingerprint":
                    item.setFingerprint(in.nextString());
                    break;
                case "contentHash":
                    // ignore for now, old items don't have this set yet.
                    //item.setContentHash(in.nextString());
                    in.skipValue();
                    break;
                case "updatedDate":
                    if(in.peek() == JsonReader.Token.NUMBER)
                        item.setUpdatedAt(new Date(in.nextLong() * 1000));
                    else
                        in.skipValue();
                    break;
                default:
                    Log.w(TAG, "Unknown value in item json: " + name);
                    in.skipValue();
                    break;
            }
        }
        in.endObject();

        return item.build();
    }

    private void parseEnclosure(NullableJsonReader reader, Item.Builder item) throws IOException {
        reader.in.beginObject();
        while(reader.in.hasNext()) {
            switch (reader.in.nextName()) {
                case "mimeType":
                    item.setEnclosureMime(reader.nextString());
                    break;
                case "url":
                    item.setEnclosureLink(reader.nextString());
                    break;
            }
        }
        reader.in.endObject();
    }

    private final static DateFormat iso8601Format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZZ", Locale.US);

    @Nullable
    private Date parseDate(String source) {
        try {
            return iso8601Format.parse(source);
        } catch (ParseException e) {
            Log.e(TAG, "Failed to parse date: " + source, e);
            return null;
        }
    }
}
