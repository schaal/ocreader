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

import android.support.annotation.NonNull;

/**
 * Created by daniel on 13.11.15.
 */
public class Version implements Comparable<Version> {
    public Integer[] versions;

    @Override
    public int compareTo(@NonNull Version another) {
        for (int i = 0; i < versions.length; i++) {
            int compare = versions[i].compareTo(another.versions.length >= i ? another.versions[i] : -1);
            if(compare != 0)
                return compare;
        }
        return 0;
    }

    public void setVersion(String version) {
        String[] versions = version.split(".");
        this.versions = new Integer[versions.length];
        for (int i = 0; i < versions.length; i++) {
            this.versions[i] = Integer.parseInt(versions[i]);
        }
    }
}
