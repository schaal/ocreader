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

package email.schaal.ocreader.api.json;

import android.util.Log;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.JsonReader;
import com.squareup.moshi.JsonWriter;

import java.io.IOException;

/**
 * TypeAdapter to deserialize the JSON response for the status api call.
 */
public class NewsStatusTypeAdapter extends JsonAdapter<NewsStatus> {
    private final static String TAG = NewsStatusTypeAdapter.class.getName();

    @Override
    public void toJson(JsonWriter out, NewsStatus value) throws IOException {
    }

    @Override
    public NewsStatus fromJson(JsonReader in) throws IOException {
        if (in.peek() == JsonReader.Token.NULL) {
            in.nextNull();
            return null;
        }

        final NullableJsonReader reader = new NullableJsonReader(in);
        final NewsStatus status = new NewsStatus();

        in.beginObject();

        while (in.hasNext()) {
            String name = in.nextName();
            switch (name) {
                case "version":
                    status.setVersion(reader.nextString());
                    break;
                case "warnings":
                case "issues":
                    // this is called warnings in api v1-2, issues in api v2
                    readWarnings(in, status);
                    break;
                case "user":
                    status.setUser(new UserTypeAdapter().fromJson(in));
                    break;
                default:
                    Log.w(TAG, "Unknown value in status json: " + name);
                    in.skipValue();
                    break;
            }
        }
        in.endObject();

        return status;
    }

    private void readWarnings(JsonReader in, NewsStatus status) throws IOException {
        in.beginObject();
        while(in.hasNext()) {
            String name = in.nextName();
            switch (name) {
                case "improperlyConfiguredCron":
                    status.setImproperlyConfiguredCron(in.nextBoolean());
                    break;
                case "incorrectDbCharset":
                    // TODO: 11/9/17 Show warning 
                    in.skipValue();
                    break;
                default:
                    Log.w(TAG, "Unknown value in status warnings json: " + name);
                    in.skipValue();
            }
        }
        in.endObject();
    }
}
