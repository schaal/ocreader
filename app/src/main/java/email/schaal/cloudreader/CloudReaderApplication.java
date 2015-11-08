/*
 * Copyright (C) 2015 Daniel Schaal <daniel@schaal.email>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package email.schaal.cloudreader;

import android.app.Application;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.support.v4.content.ContextCompat;
import android.widget.ImageView;

import com.mikepenz.materialdrawer.util.DrawerImageLoader;
import com.squareup.picasso.OkHttpDownloader;
import com.squareup.picasso.Picasso;

import email.schaal.cloudreader.api.APIService;
import email.schaal.cloudreader.database.Queries;
import email.schaal.cloudreader.http.HttpManager;
import email.schaal.cloudreader.util.AlarmUtils;

/**
 * Created by daniel on 08.11.15.
 */
public class CloudReaderApplication extends Application {
    private static final String TAG = CloudReaderApplication.class.getSimpleName();

    @Override
    public void onCreate() {
        super.onCreate();
        HttpManager.init(this);
        Queries.init(this);
        APIService.init(this);
        AlarmUtils.init(this);
        Picasso picasso = new Picasso.Builder(this)
                .downloader(new OkHttpDownloader(this))
                .defaultBitmapConfig(Bitmap.Config.ARGB_8888)
                .build();

        Picasso.setSingletonInstance(picasso);

        DrawerImageLoader.init(new DrawerImageLoader.IDrawerImageLoader() {
            @Override
            public void set(ImageView imageView, Uri uri, Drawable placeholder) {
                Picasso.with(imageView.getContext()).load(uri).placeholder(placeholder).into(imageView);
            }

            @Override
            public void cancel(ImageView imageView) {
                Picasso.with(imageView.getContext()).cancelRequest(imageView);
            }

            @Override
            public Drawable placeholder(Context ctx) {
                return ContextCompat.getDrawable(ctx, R.drawable.ic_feed_icon);
            }

            @Override
            public Drawable placeholder(Context ctx, String tag) {
                return ContextCompat.getDrawable(ctx, R.drawable.ic_feed_icon);
            }
        });
    }
}
