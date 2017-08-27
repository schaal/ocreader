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
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatDelegate;

import email.schaal.ocreader.database.model.AllUnreadFolder;
import email.schaal.ocreader.database.model.Item;
import io.realm.Sort;

/**
 * Manage Preference values to store in SharedPreferences and retrieve those values.
 */
public enum Preferences {
    /** User preferences **/
    SHOW_ONLY_UNREAD("show_only_unread", false),
    USERNAME("username"),
    PASSWORD("password"),
    URL("url"),
    ORDER("order", Sort.ASCENDING.name(), ChangeAction.UPDATE),
    SORT_FIELD("sort_field", Item.ID, ChangeAction.UPDATE),
    DARK_THEME("dark_theme", false, ChangeAction.RECREATE),

    /** System preferences **/
    SYS_NEEDS_UPDATE_AFTER_SYNC("needs_update_after_sync", false),
    SYS_SYNC_RUNNING("is_sync_running", false),

    SYS_STARTDRAWERITEMID("startdrawer_itemid", AllUnreadFolder.ID),
    SYS_ENDRAWERITEM_ID("enddrawer_itemid"),
    SYS_ISFEED("isfeed", false),

    SYS_DETECTED_API_LEVEL("detected_api_level"),
    SYS_APIv2_ETAG("apiv2_etag");

    /**
     * What to do after the preference changes
     */
    public enum ChangeAction {
        NOTHING, // do nothing
        RECREATE, // recreate activity
        UPDATE // update item recyclerview
    }

    @NonNull
    private final String key;
    @Nullable
    private final Object defaultValue;
    @NonNull
    private final ChangeAction changeAction;

    Preferences(@NonNull String key) {
        this(key, null);
    }

    Preferences(@NonNull String key, @Nullable Object defaultValue) {
        this(key, defaultValue, ChangeAction.NOTHING);
    }

    Preferences(@NonNull String key, @Nullable Object defaultValue, @NonNull ChangeAction changeAction) {
        this.key = key;
        this.defaultValue = defaultValue;
        this.changeAction = changeAction;
    }

    @NonNull
    public String getKey() {
        return key;
    }

    public String getString(SharedPreferences preferences) {
        return preferences.getString(key, (String) defaultValue);
    }

    @NonNull
    public Boolean getBoolean(SharedPreferences preferences) {
        if(defaultValue == null)
            return false;
        return preferences.getBoolean(key, (Boolean) defaultValue);
    }

    @NonNull
    public Long getLong(SharedPreferences preferences) {
        if(defaultValue == null)
            return 0L;
        return preferences.getLong(key, (Long) defaultValue);
    }

    public Sort getOrder(SharedPreferences preferences) {
        try {
            return Sort.valueOf(preferences.getString(key, (String) defaultValue));
        } catch (ClassCastException e) {
            preferences.edit().remove(key).apply();
        }
        return Sort.valueOf((String) defaultValue);
    }

    @NonNull
    public ChangeAction getChangeAction() {
        return changeAction;
    }

    @AppCompatDelegate.NightMode
    public static int getNightMode(SharedPreferences preferences) {
        return preferences.getBoolean(DARK_THEME.getKey(), false) ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO;
    }

    public static boolean hasCredentials(SharedPreferences preferences) {
        return USERNAME.getString(preferences) != null && SYS_DETECTED_API_LEVEL.getString(preferences) != null;
    }

    @Nullable
    public static Preferences getPreference(String key) {
        for(Preferences preference: values()) {
            if(preference.getKey().equals(key))
                return preference;
        }
        return null;
    }
}
