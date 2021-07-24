/*
 * Copyright © 2017. Daniel Schaal <daniel@schaal.email>
 *
 * This file is part of ocreader.
 *
 * ocreader is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ocreader is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Foobar.  If not, see <http://www.gnu.org/licenses/>.
 */
package email.schaal.ocreader

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.PreferenceManager
import email.schaal.ocreader.database.Queries
import email.schaal.ocreader.database.model.Item

/**
 * Application base class to setup the singletons
 */
class OCReaderApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        val preferences = PreferenceManager.getDefaultSharedPreferences(this)

        preferences.edit().apply {
            // Migrate to apptoken
            if (preferences.contains(Preferences.PASSWORD.key)) {
                remove(Preferences.USERNAME.key)
                remove(Preferences.PASSWORD.key)
                remove(Preferences.URL.key)
            }

            // Migrate updatedAt to lastModified
            if (Preferences.SORT_FIELD.getString(preferences) == "updatedAt")
                putString(Preferences.SORT_FIELD.key, Item::lastModified.name)

            // Directly observer WorkManager
            remove("sync_running")
        }.apply()

        AppCompatDelegate.setDefaultNightMode(Preferences.getNightMode(preferences))

        Queries.init(this)
    }
}