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

/**
 * TypeAdapter to deserialize the JSON response for Users.
 */
public class UserTypeAdapter extends TypeAdapter<User> {
    private final static String TAG = UserTypeAdapter.class.getSimpleName();

    @Override
    public void write(JsonWriter out, User value) throws IOException {
    }

    @Override
    public User read(JsonReader in) throws IOException {
        if (in.peek() == JsonToken.NULL) {
            in.nextNull();
            return null;
        }
        User user = new User();
        in.beginObject();
        while (in.hasNext()) {
            String name = in.nextName();
            switch (name) {
                case "userId":
                    user.setUserId(in.nextString());
                    break;
                case "displayName":
                    user.setDisplayName(in.nextString());
                    break;
                case "lastLoginTimestamp":
                    user.setLastLogin(new Date(in.nextLong() * 1000));
                    break;
                case "avatar":
                    if(in.peek() == JsonToken.NULL)
                        in.skipValue();
                    else
                        readAvatar(in, user, name);
                    break;
                default:
                    Log.w(TAG, "Unknown value in user json: " + name);
                    in.skipValue();
                    break;
            }
        }
        in.endObject();
        return user;
    }

    private void readAvatar(JsonReader in, User user, String name) throws IOException {
        in.beginObject();
        while(in.hasNext()) {
            String avatarName = in.nextName();
            switch (avatarName) {
                case "data":
                    user.setAvatar(in.nextString());
                    break;
                case "mime":
                    user.setAvatarMime(in.nextString());
                    break;
                default:
                    Log.w(TAG, "Unknown value in avatar json: " + name);
                    in.skipValue();
                    break;
            }
        }
        in.endObject();
    }
}
