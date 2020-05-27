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
import android.graphics.Color
import android.text.format.DateUtils
import androidx.core.text.HtmlCompat
import email.schaal.ocreader.R
import email.schaal.ocreader.database.model.Feed
import okhttp3.HttpUrl
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

fun Date.getTimeSpanString(endDate: Date = Date()): CharSequence {
    return DateUtils.getRelativeTimeSpanString(
            this.time,
            endDate.time,
            DateUtils.MINUTE_IN_MILLIS,
            DateUtils.FORMAT_ABBREV_ALL)
}

fun String.cleanString(): String {
    return HtmlCompat.fromHtml(this, HtmlCompat.FROM_HTML_MODE_COMPACT).toString()
}

fun Int.asCssString(): String { // Use US locale so we always get a . as decimal separator for a valid css value
    return String.format(Locale.US, "rgba(%d,%d,%d,%.2f)",
            Color.red(this),
            Color.green(this),
            Color.blue(this),
            Color.alpha(this) / 255.0)
}

fun HttpUrl.buildBaseUrl(apiRoot: String): HttpUrl {
    return this
            .newBuilder()
            .addPathSegments(apiRoot)
            .build()
}
