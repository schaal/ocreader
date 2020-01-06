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
@file:JvmName("StringUtils")

package email.schaal.ocreader.util

import android.content.Context
import android.text.Html
import android.text.format.DateUtils
import email.schaal.ocreader.R
import java.util.*

/**
 * Utility functions to handle Strings.
 */
fun getByLine(context: Context, template: String?, author: String?): String {
    return if (author != null) {
        String.format(template!!, context.getString(R.string.by_author, author))
    } else {
        ""
    }
}

fun getTimeSpanString(context: Context, startDate: Date): String {
    return getTimeSpanString(context, startDate, Date())
}

fun getTimeSpanString(context: Context, startDate: Date, endDate: Date): String {
    val timeSpanString: String
    val timeDiff = endDate.time - startDate.time
    timeSpanString = if (timeDiff <= 0) context.getString(R.string.now) else if (timeDiff <= 59 * DateUtils.MINUTE_IN_MILLIS) context.getString(R.string.minutes, timeDiff / DateUtils.MINUTE_IN_MILLIS) else if (timeDiff <= 23 * DateUtils.HOUR_IN_MILLIS) context.getString(R.string.hours, timeDiff / DateUtils.HOUR_IN_MILLIS) else context.getString(R.string.days, timeDiff / DateUtils.DAY_IN_MILLIS)
    return timeSpanString
}

fun cleanString(source: String): String {
    return Html.fromHtml(source).toString()
}

fun emptyToNull(source: String): String? {
    return if (source.isEmpty()) null else source
}

fun nullToEmpty(source: String?): String {
    return source ?: ""
}