/*
 * Copyright (C) 2016 Daniel Schaal <daniel@schaal.email>
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

package email.schaal.ocreader.util;

import android.graphics.Bitmap;
import android.support.annotation.Nullable;

import com.squareup.picasso.Downloader;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Request;
import com.squareup.picasso.RequestHandler;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import divstar.ico4a.codec.ico.ICODecoder;

/**
 * Handle requests for Picasso for ico files
 *
 * Convert ico files containing multiple sizes to multiple images
 */
public class IcoRequestHandler extends RequestHandler {
    private final Downloader downloader;

    public IcoRequestHandler(Downloader downloader) {
        this.downloader = downloader;
    }

    @Override
    public boolean canHandleRequest(Request data) {
        // icons from s.ytimg.com have .ico suffix, but are actually png files
        return data.uri.getLastPathSegment().endsWith(".ico") && !data.uri.getHost().equals("s.ytimg.com");
    }

    @Nullable
    @Override
    public Result load(Request request, int networkPolicy) throws IOException {
        Downloader.Response response = downloader.load(request.uri, networkPolicy);

        if(response != null && response.getInputStream() != null) {
            final InputStream inputStream = response.getInputStream();

            try {
                List<Bitmap> bitmaps = ICODecoder.read(inputStream);

                Bitmap biggest = bitmaps.remove(0);

                for (Bitmap bitmap : bitmaps) {
                    if (bitmap.getHeight() > biggest.getHeight())
                        biggest = bitmap;
                }

                return new Result(biggest, Picasso.LoadedFrom.NETWORK);
            } finally {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return null;
    }
}
