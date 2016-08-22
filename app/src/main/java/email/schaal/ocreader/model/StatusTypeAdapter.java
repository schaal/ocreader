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

package email.schaal.ocreader.model;

import android.util.Log;

import com.squareup.moshi.JsonReader;
import com.squareup.moshi.JsonWriter;

import java.io.IOException;

import email.schaal.ocreader.api.json.Status;

/**
 * TypeAdapter to deserialize the JSON response for the status api call.
 */
public class StatusTypeAdapter extends NewsTypeAdapter<Status> {
    private final static String TAG = StatusTypeAdapter.class.getName();

    @Override
    public void toJson(JsonWriter out, Status value) throws IOException {
    }

    @Override
    public Status fromJson(JsonReader in) throws IOException {
        if (in.peek() == JsonReader.Token.NULL) {
            in.nextNull();
            return null;
        }

        Status status = new Status();

        in.beginObject();
        while (in.hasNext()) {
            String name = in.nextName();
            switch (name) {
                case "version":
                    status.setVersion(nullSafeString(in));
                    break;
                case "warnings":
                case "issues":
                    // warnings in api v1-2, issues in api v2
                    readWarnings(in, status);
                    break;
                case "user":
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

    private void readWarnings(JsonReader in, Status status) throws IOException {
        in.beginObject();
        while(in.hasNext()) {
            String name = in.nextName();
            switch (name) {
                case "improperlyConfiguredCron":
                    status.setImproperlyConfiguredCron(in.nextBoolean());
                    break;
                default:
                    Log.w(TAG, "Unknown value in status warnings json: " + name);
                    in.skipValue();
            }
        }
        in.endObject();
    }
}
