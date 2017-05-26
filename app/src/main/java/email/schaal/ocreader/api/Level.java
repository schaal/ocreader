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

package email.schaal.ocreader.api;

import android.content.Context;
import android.support.annotation.Nullable;

/**
 * Created by daniel on 26.05.17.
 */
public enum Level {
    V2("v2", false),
    V12("v1-2", true);

    private final String level;
    private final boolean supported;

    public String getLevel() {
        return level;
    }

    Level(String level, boolean supported) {
        this.level = level;
        this.supported = supported;
    }

    public boolean isSupported() {
        return supported;
    }

    @Nullable
    public static API getAPI(Context context, final Level level) {
        switch (level) {
            case V12:
                return new APIv12(context);
            case V2:
                return new APIv2(context);
            default:
                return null;
        }
    }

    @Nullable
    public static Level get(String level) {
        for (Level supportedLevel : values()) {
            if (supportedLevel.level.equals(level))
                return supportedLevel;
        }
        return null;
    }
}
