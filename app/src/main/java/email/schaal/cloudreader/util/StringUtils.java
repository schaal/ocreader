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

package email.schaal.cloudreader.util;

import android.content.Context;
import android.text.format.DateUtils;

import org.jsoup.Jsoup;
import org.jsoup.safety.Whitelist;

import java.util.Date;

import email.schaal.cloudreader.R;

/**
 * Utility class to handle Strings.
 */
public class StringUtils {
    public static String nullIfEmpty(String source) {
        if(source != null) {
            source = source.trim();
            if (source.length() == 0)
                source = null;
        }
        return source;
    }

    public static String getByLine(Context context, String author, String feedTitle) {
        if(author == null) {
            return context.getString(R.string.article_from, feedTitle);
        } else {
            return context.getString(R.string.article_by_from, author, feedTitle);
        }
    }

    public static String getTimeSpanString(Context context, Date date) {
        String timeSpanString;

        long timeDiff = (new Date().getTime() - date.getTime());

        if(timeDiff <= 0)
            timeSpanString = context.getString(R.string.now);
        else if(timeDiff <= 60 * DateUtils.MINUTE_IN_MILLIS)
            timeSpanString = context.getString(R.string.minutes, timeDiff/DateUtils.MINUTE_IN_MILLIS);
        else if(timeDiff <= 24 * DateUtils.HOUR_IN_MILLIS)
            timeSpanString = context.getString(R.string.hours, timeDiff/DateUtils.HOUR_IN_MILLIS);
        else
            timeSpanString = context.getString(R.string.days, timeDiff / DateUtils.DAY_IN_MILLIS);
        return timeSpanString;
    }

    public static String cleanString(String source) {
        return Jsoup.clean(source, Whitelist.none());
    }
}
