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

import android.util.Log;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.JsonReader;
import com.squareup.moshi.JsonWriter;

import java.io.IOException;

import email.schaal.ocreader.database.model.Folder;

/**
 * TypeAdapter to deserialize the JSON response for Folders.
 */
public class FolderTypeAdapter extends JsonAdapter<Folder> {
    private final static String TAG = FolderTypeAdapter.class.getName();

    @Override
    public void toJson(JsonWriter out, Folder value) throws IOException {
    }

    @Override
    public Folder fromJson(JsonReader in) throws IOException {
        if (in.peek() == JsonReader.Token.NULL) {
            in.nextNull();
            return null;
        }
        Folder folder = new Folder();
        in.beginObject();
        while (in.hasNext()) {
            String name = in.nextName();
            switch (name) {
                case "id":
                    folder.setId(in.nextLong());
                    break;
                case "name":
                    folder.setName(in.nextString());
                    break;
                default:
                    Log.w(TAG, "Unknown value in folder json: " + name);
                    in.skipValue();
                    break;
            }
        }
        in.endObject();
        return folder;
    }
}
