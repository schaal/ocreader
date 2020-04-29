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
import android.text.format.DateUtils
import androidx.core.text.HtmlCompat
import email.schaal.ocreader.R
import email.schaal.ocreader.database.model.Feed
import java.util.*

/**
 * Utility functions to handle Strings.
 */
fun getByLine(context: Context, template: String, author: String?, feed: Feed?): String {
    return if (author != null && feed != null) {
        String.format(template, context.getString(R.string.by_author_on_feed, author, "<a href=\"${feed.link}\">${feed.name}</a>"))
    } else if(author != null) {
        String.format(template, context.getString(R.string.by_author, author))
    } else if (feed != null) {
        String.format(template, context.getString(R.string.on_feed, "<a href=\"${feed.link}\">${feed.name}</a>"))
    } else {
        ""
    }
}

fun getTimeSpanString(context: Context, startDate: Date?, endDate: Date? = Date()): String {
    if(startDate == null || endDate == null)
        return ""

    val timeSpanString: String
    val timeDiff = endDate.time - startDate.time
    timeSpanString = when {
        timeDiff <= 0 -> context.getString(R.string.now)
        timeDiff <= 59 * DateUtils.MINUTE_IN_MILLIS -> context.getString(R.string.minutes, timeDiff / DateUtils.MINUTE_IN_MILLIS)
        timeDiff <= 23 * DateUtils.HOUR_IN_MILLIS -> context.getString(R.string.hours, timeDiff / DateUtils.HOUR_IN_MILLIS)
        else -> context.getString(R.string.days, timeDiff / DateUtils.DAY_IN_MILLIS)
    }
    return timeSpanString
}

fun String.cleanString(): String {
    return HtmlCompat.fromHtml(this, HtmlCompat.FROM_HTML_MODE_COMPACT).toString()
}
