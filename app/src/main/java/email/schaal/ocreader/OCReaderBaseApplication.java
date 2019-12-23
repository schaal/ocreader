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

package email.schaal.ocreader;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.preference.PreferenceManager;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.content.res.AppCompatResources;
import android.widget.ImageView;

import com.bumptech.glide.Glide;

import email.schaal.ocreader.database.Queries;
import email.schaal.ocreader.util.AlarmUtils;

/**
 * Application base class to setup the singletons
 */
public abstract class OCReaderBaseApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        if(shouldExit())
            return;

        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);

        AppCompatDelegate.setDefaultNightMode(Preferences.getNightMode(preferences));

        preferences.edit()
                .putBoolean(Preferences.SYS_SYNC_RUNNING.getKey(), false)
                .apply();

        Queries.init(this);

        AlarmUtils.init(this);
    }

    /**
     *
     * @return true if app specific initialization should be skipped, e.g. for ACRA process
     */
    protected boolean shouldExit() {
        return false;
    }
}
