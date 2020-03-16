/*
 * Copyright © 2020. Daniel Schaal <daniel@schaal.email>
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

package email.schaal.ocreader.service

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.ResultReceiver
import android.view.View
import com.google.android.material.snackbar.Snackbar
import email.schaal.ocreader.R
import email.schaal.ocreader.util.LoginError

class SyncResultReceiver(private val view: View): ResultReceiver(Handler(Looper.getMainLooper())) {
    companion object {
        const val INTENT_KEY = "email.schaal.ocreader.SyncResultReceiver"
        const val EXCEPTION_DATA_KEY = "email.schaal.ocreader.SyncResultReceiver.EXCEPTION_DATA"
    }

    override fun onReceiveResult(resultCode: Int, resultData: Bundle?) {
        (resultData?.getSerializable(EXCEPTION_DATA_KEY) as? Exception)?.let {
            Snackbar.make(view, LoginError.getError(view.context, it).message, Snackbar.LENGTH_LONG).show()
        }
    }
}