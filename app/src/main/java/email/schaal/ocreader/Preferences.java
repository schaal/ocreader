/*
 * Copyright (C) 2015 Daniel Schaal <daniel@schaal.email>
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

package email.schaal.ocreader;

import android.content.SharedPreferences;

import email.schaal.ocreader.model.AllUnreadFolder;

/**
 * Manage Preference values to store in SharedPreferences and retrieve those values.
 */
public enum Preferences {
    /** User preferences **/
    SHOW_ONLY_UNREAD("show_only_unread", false),
    USERNAME("username",null),
    PASSWORD("password", null),
    URL("url", null),

    /** System preferences **/
    SYS_NEEDS_UPDATE_AFTER_SYNC("needs_update_after_sync", false),
    SYS_SYNC_RUNNING("is_sync_running", false),

    SYS_STARTDRAWERITEMID("startdrawer_itemid", AllUnreadFolder.ID),
    SYS_ENDRAWERITEM_ID("enddrawer_itemid", null),
    SYS_ISFEED("isfeed", false);

    private final String key;
    private final Object defaultValue;

    Preferences(String key, Object defaultValue) {
        this.key = key;
        this.defaultValue = defaultValue;
    }

    public String getKey() {
        return key;
    }

    public String getString(SharedPreferences preferences) {
        return preferences.getString(key, (String) defaultValue);
    }

    public Boolean getBoolean(SharedPreferences preferences) {
        return preferences.getBoolean(key, (Boolean) defaultValue);
    }

    public Long getLong(SharedPreferences preferences) {
        if(defaultValue == null)
            return null;
        return preferences.getLong(key, (Long) defaultValue);
    }
}
