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

import android.util.Log;

import com.github.zafarkhaja.semver.Version;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;

/**
 * TypeAdapter to deserialize the JSON response for Versions.
 */
public class VersionTypeAdapter extends NewsTypeAdapter<Version> {
    private final static String TAG = VersionTypeAdapter.class.getSimpleName();

    @Override
    public void write(JsonWriter out, Version value) throws IOException {
    }

    @Override
    public Version read(JsonReader in) throws IOException {
        if (in.peek() == JsonToken.NULL) {
            in.nextNull();
            return null;
        }
        Version version = null;
        in.beginObject();
        while (in.hasNext()) {
            String name = in.nextName();
            switch (name) {
                case "version":
                    version = Version.valueOf(in.nextString());
                    break;
                default:
                    Log.w(TAG, "Unknown value in version json: " + name);
                    in.skipValue();
                    break;
            }
        }
        in.endObject();
        return version;
    }
}
