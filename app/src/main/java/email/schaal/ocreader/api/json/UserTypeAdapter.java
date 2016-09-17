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

import com.squareup.moshi.JsonReader;
import com.squareup.moshi.JsonWriter;

import java.io.IOException;
import java.util.Date;

import email.schaal.ocreader.database.model.User;

/**
 * TypeAdapter to deserialize the JSON response for Users.
 */
public class UserTypeAdapter extends NewsTypeAdapter<User> {
    private final static String TAG = UserTypeAdapter.class.getName();

    @Override
    public void toJson(JsonWriter out, User value) throws IOException {
    }

    @Override
    public User fromJson(JsonReader in) throws IOException {
        if (in.peek() == JsonReader.Token.NULL) {
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
                    if(in.peek() == JsonReader.Token.NULL)
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
