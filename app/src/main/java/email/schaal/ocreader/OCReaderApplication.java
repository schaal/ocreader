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

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatDelegate;
import android.support.v7.content.res.AppCompatResources;
import android.widget.ImageView;

import com.mikepenz.materialdrawer.util.DrawerImageLoader;
import com.squareup.picasso.OkHttp3Downloader;
import com.squareup.picasso.Picasso;

import org.acra.ACRA;
import org.acra.ReportingInteractionMode;
import org.acra.annotation.ReportsCrashes;

import email.schaal.ocreader.database.Queries;
import email.schaal.ocreader.database.model.Item;
import email.schaal.ocreader.util.AlarmUtils;
import email.schaal.ocreader.util.IcoRequestHandler;

/**
 * Application class to setup the singletons
 */
@ReportsCrashes(
        mailTo = "ocreader+reports@schaal.email",
        mode = ReportingInteractionMode.DIALOG,
        resDialogText = R.string.app_name,
        reportDialogClass = email.schaal.ocreader.CustomCrashReportDialog.class
)
public class OCReaderApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        if(ACRA.isACRASenderServiceProcess())
            return;

        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);

        AppCompatDelegate.setDefaultNightMode(Preferences.getNightMode(preferences));

        preferences.edit()
                .putBoolean(Preferences.SYS_SYNC_RUNNING.getKey(), false)
                .putString(Preferences.SORT_FIELD.getKey(), Item.ID)
                .apply();

        Queries.init(this);

        AlarmUtils.init(this);

        OkHttp3Downloader downloader = new OkHttp3Downloader(this);

        Picasso picasso = new Picasso.Builder(this)
                .downloader(downloader)
                .defaultBitmapConfig(Bitmap.Config.ARGB_8888)
                .addRequestHandler(new IcoRequestHandler(downloader))
                .build();

        Picasso.setSingletonInstance(picasso);

        DrawerImageLoader.init(new DrawerImageLoader.IDrawerImageLoader() {
            @Override
            public void set(ImageView imageView, Uri uri, Drawable placeholder) {
                set(imageView, uri, placeholder, null);
            }

            @Override
            public void set(ImageView imageView, Uri uri, Drawable placeholder, String tag) {
                Picasso.with(imageView.getContext()).load(uri).placeholder(placeholder).into(imageView);
            }

            @Override
            public void cancel(ImageView imageView) {
                Picasso.with(imageView.getContext()).cancelRequest(imageView);
            }

            @Override
            public Drawable placeholder(Context ctx) {
                return AppCompatResources.getDrawable(ctx, R.drawable.ic_feed_icon);
            }

            @Override
            public Drawable placeholder(Context ctx, String tag) {
                final int drawableRes;

                if(tag != null) {
                    switch (DrawerImageLoader.Tags.valueOf(tag)) {
                        case PROFILE:
                            drawableRes = R.mipmap.ic_launcher;
                            break;
                        default:
                            drawableRes = R.drawable.ic_feed_icon;
                            break;
                    }
                } else {
                    drawableRes = R.drawable.ic_feed_icon;
                }

                return AppCompatResources.getDrawable(ctx, drawableRes);
            }
        });
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        if(!BuildConfig.DEBUG)
            ACRA.init(this);
    }
}
