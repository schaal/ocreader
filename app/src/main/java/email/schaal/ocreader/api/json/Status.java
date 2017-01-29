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

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.github.zafarkhaja.semver.UnexpectedCharacterException;
import com.github.zafarkhaja.semver.Version;

import email.schaal.ocreader.database.model.User;

/**
 * Encapsulates the JSON response for the status api call
 */
public class Status {
    private final static String TAG = Status.class.getName();

    @Nullable
    private Version version;
    private boolean improperlyConfiguredCron;

    /* Only returned by API v2 */
    @Nullable
    private User user;

    @Nullable
    public User getUser() {
        return user;
    }

    public void setUser(@Nullable User user) {
        this.user = user;
    }

    @Nullable
    public Version getVersion() {
        return version;
    }

    public void setVersion(@NonNull String version) {
        try {
            this.version = Version.valueOf(version);
        } catch (UnexpectedCharacterException e) {
            this.version = null;
            Log.e(TAG, "stacktrace", e);
        }
    }

    public boolean isImproperlyConfiguredCron() {
        return improperlyConfiguredCron;
    }

    public void setImproperlyConfiguredCron(boolean improperlyConfiguredCron) {
        this.improperlyConfiguredCron = improperlyConfiguredCron;
    }
}
