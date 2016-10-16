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

import com.squareup.moshi.JsonReader;

import java.io.IOException;

/**
 * Created by daniel on 16.10.16.
 */

class NullableJsonReader {
    final JsonReader in;

    NullableJsonReader(JsonReader in) {
        this.in = in;
    }

    int nextInt(int def) throws IOException {
        if(nextNull()) {
            return def;
        } else
            return in.nextInt();
    }

    long nextLong(long def) throws IOException {
        if(nextNull()) {
            return def;
        } else
            return in.nextLong();
    }

    String nextString() throws IOException {
        if(nextNull())
            return null;
        else
            return in.nextString();
    }

    boolean nextBoolean(boolean def) throws IOException {
        if(nextNull())
            return def;
        else
            return in.nextBoolean();
    }

    private boolean nextNull() throws IOException {
        if(in.peek() == JsonReader.Token.NULL) {
            in.nextNull();
            return true;
        } else
            return false;
    }
}
