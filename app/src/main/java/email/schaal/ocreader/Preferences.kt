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
package email.schaal.ocreader

import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.app.AppCompatDelegate.NightMode
import email.schaal.ocreader.database.model.AllUnreadFolder
import email.schaal.ocreader.database.model.Item
import io.realm.Sort

/**
 * Manage Preference values to store in SharedPreferences and retrieve those values.
 */
enum class Preferences constructor(val key: String, private val defaultValue: Any? = null, val changeAction: ChangeAction = ChangeAction.NOTHING) {
    /** User preferences  */
    SHOW_ONLY_UNREAD("show_only_unread", false),
    USERNAME("username"),
    PASSWORD("password"),
    URL("url"),
    ORDER("order", Sort.ASCENDING.name, ChangeAction.UPDATE),
    SORT_FIELD("sort_field", Item::id.name, ChangeAction.UPDATE),
    DARK_THEME("dark_theme", "system", ChangeAction.RECREATE),
    ARTICLE_FONT("article_font", "system"), /** System preferences  */
    SYS_NEEDS_UPDATE_AFTER_SYNC("needs_update_after_sync", false),
    SYS_SYNC_RUNNING("is_sync_running", false),
    SYS_STARTDRAWERITEMID("startdrawer_itemid", AllUnreadFolder.ID),
    SYS_DETECTED_API_LEVEL("detected_api_level"),
    SYS_APIv2_ETAG("apiv2_etag");

    /**
     * What to do after the preference changes
     */
    enum class ChangeAction {
        NOTHING,  // do nothing
        RECREATE,  // recreate activity
        UPDATE // update item recyclerview
    }

    fun getString(preferences: SharedPreferences): String? {
        return preferences.getString(key, defaultValue as String?)
    }

    fun getBoolean(preferences: SharedPreferences): Boolean {
        return if (defaultValue == null) false else preferences.getBoolean(key, (defaultValue as Boolean?)!!)
    }

    fun getLong(preferences: SharedPreferences): Long {
        return if (defaultValue == null) 0L else preferences.getLong(key, (defaultValue as Long?)!!)
    }

    fun getOrder(preferences: SharedPreferences): Sort {
        try {
            return Sort.valueOf(preferences.getString(key, defaultValue as String?)!!)
        } catch (e: ClassCastException) {
            preferences.edit().remove(key).apply()
        }
        return Sort.valueOf((defaultValue as String?)!!)
    }

    companion object {
        @NightMode
        fun getNightMode(preferences: SharedPreferences): Int {
            return try {
                when(preferences.getString(DARK_THEME.key, "system")) {
                    "always" -> AppCompatDelegate.MODE_NIGHT_YES
                    "never" -> AppCompatDelegate.MODE_NIGHT_NO
                    else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                }
            } catch(e: ClassCastException) {
                preferences.edit()
                        .remove(DARK_THEME.key)
                        .putString(DARK_THEME.key, "system")
                        .apply()
                AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            }
        }

        fun hasCredentials(preferences: SharedPreferences): Boolean {
            return USERNAME.getString(preferences) != null && SYS_DETECTED_API_LEVEL.getString(preferences) != null
        }

        fun getPreference(key: String): Preferences? {
            for (preference in values()) {
                if (preference.key == key) return preference
            }
            return null
        }
    }

}