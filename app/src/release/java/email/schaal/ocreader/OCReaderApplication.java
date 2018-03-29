/*
 * Copyright (C) 2015-2016 Daniel Schaal <daniel@schaal.email>
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

package email.schaal.ocreader;

import android.content.Context;

import org.acra.ACRA;
import org.acra.annotation.AcraCore;
import org.acra.annotation.AcraDialog;
import org.acra.annotation.AcraMailSender;

/**
 * Application class to setup the singletons
 */
@AcraCore(buildConfigClass = BuildConfig.class)
@AcraDialog(resText = R.string.app_name, reportDialogClass = email.schaal.ocreader.CustomCrashReportDialog.class)
@AcraMailSender(mailTo = "ocreader+reports@schaal.email")
public class OCReaderApplication extends OCReaderBaseApplication {
    @Override
    protected boolean shouldExit() {
        return ACRA.isACRASenderServiceProcess();
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        if(!BuildConfig.DEBUG)
            ACRA.init(this);
    }
}
