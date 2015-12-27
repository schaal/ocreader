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

package email.schaal.cloudreader;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.NestedScrollView;
import android.support.v7.graphics.Palette;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebViewFragment;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.safety.Cleaner;
import org.jsoup.safety.Whitelist;
import org.jsoup.select.Elements;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import email.schaal.cloudreader.model.Feed;
import email.schaal.cloudreader.model.Item;
import email.schaal.cloudreader.util.FaviconUtils;
import email.schaal.cloudreader.util.StringUtils;

/**
 * Created by daniel on 15.11.15.
 */
public class ItemPageFragment extends WebViewFragment {
    private static final String TAG = ItemPageFragment.class.getSimpleName();

    public static final String ARG_POSITION = "ARG_POSITION";

    private final static Cleaner cleaner = new Cleaner(Whitelist.relaxed());

    private static String css = null;

    private Item item;

    private final FaviconUtils.PaletteBitmapAsyncListener paletteAsyncListener = new FaviconUtils.PaletteBitmapAsyncListener() {
        @Override
        public void onGenerated(Palette palette, Bitmap bitmap) {
            loadWebViewData(getActivity(), palette);
        }
    };

    public ItemPageFragment() {
    }

    public static ItemPageFragment newInstance(int position) {
        ItemPageFragment fragment = new ItemPageFragment();
        Bundle bundle = new Bundle();
        bundle.putInt(ARG_POSITION, position);
        fragment.setArguments(bundle);
        return fragment;
    }

    public static String getCssColor(int color) {
        // using %f for the double value would result in a localized string, e.g. 0,12 which
        // would be an invalid css color string
        return String.format("rgba(%d,%d,%d,%s)",
                Color.red(color),
                Color.green(color),
                Color.blue(color),
                Double.toString(Color.alpha(color) / 255.0));
    }

    @Override
    public void onStart() {
        super.onStart();

        ItemPagerActivity activity = (ItemPagerActivity) getActivity();
        item = activity.getItemForPosition(getArguments().getInt(ARG_POSITION));
        final Feed feed = Item.feed(item);

        FaviconUtils.getInstance().loadFavicon(activity, feed, paletteAsyncListener);
    }

    private void loadWebViewData(Context context, @Nullable Palette palette) {
        getWebView().loadDataWithBaseURL(null, getHtml(context, item, palette), "text/html", "UTF-8", null);
    }

    private String getHtml(Context context, Item item, @Nullable Palette palette) {
        if (css == null)
            try {
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(context.getAssets().open("item_page.css")));
                String line;
                StringBuilder cssBuilder = new StringBuilder();
                while ((line = bufferedReader.readLine()) != null) {
                    cssBuilder.append(line);
                }
                css = cssBuilder.toString();
            } catch (IOException e) {
                e.printStackTrace();
            }

        int titleColor = ContextCompat.getColor(context, R.color.primary_text);
        if (palette != null) {
            titleColor = palette.getDarkVibrantColor(titleColor);
        }

        Document document = Jsoup.parse(item.getBody());
        prepareDocument(document);
        document = cleaner.clean(document);

        String cssColor = getCssColor(titleColor);
        String feedCss = String.format(".feedcolor { color: %s } a:link, a:active,a:hover { color: %s }", cssColor, cssColor);

        StringBuilder pageBuilder = new StringBuilder(
                String.format("<html><head><style type=\"text/css\">%s</style></head><body>", css + feedCss)
        );

        Feed feed = Item.feed(item);

        pageBuilder.append(String.format(
                        "<a href=\"%s\" class=\"title\">%s</a><p class=\"byline\">%s</p>",
                        item.getUrl(),
                        item.getTitle(),
                        StringUtils.getByLine(context, item.getAuthor(), String.format("<a href=\"%s\">%s</a>", feed.getLink(), feed.getTitle()))
                )
        );

        document.outputSettings().prettyPrint(false);
        pageBuilder.append(document.body().html());

        pageBuilder.append("</body></html>");

        return pageBuilder.toString();
    }

    private void prepareDocument(Document document) {
        Elements iframes = document.getElementsByTag("iframe");
        for(Element iframe: iframes) {
            final String src = iframe.attr("src");
            iframe.tagName("a");
            iframe.removeAttr("src");
            iframe.attr("href", src);
            iframe.html(src);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View childView = super.onCreateView(inflater, container, savedInstanceState);
        View rootView = inflater.inflate(R.layout.fragment_item_pager, container, false);

        NestedScrollView nestedScrollView = (NestedScrollView) rootView.findViewById(R.id.nestedscrollview);
        nestedScrollView.addView(childView, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);

        // Using software rendering to prevent frozen or blank webviews
        // See https://code.google.com/p/chromium/issues/detail?id=501901
        if(Build.HARDWARE.equals("qcom") && Build.VERSION.SDK_INT <= Build.VERSION_CODES.M) {
            Log.w(TAG, "Using software rendering");
            rootView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        }

        return rootView;
    }
}
