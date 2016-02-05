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

package email.schaal.ocreader;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.NestedScrollView;
import android.support.v7.graphics.Palette;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;
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
import java.util.Locale;

import email.schaal.ocreader.model.Feed;
import email.schaal.ocreader.model.Item;
import email.schaal.ocreader.util.FaviconUtils;
import email.schaal.ocreader.util.StringUtils;

/**
 * Fragment to display a single feed item using a WebView.
 */
public class ItemPageFragment extends WebViewFragment {
    private static final String TAG = ItemPageFragment.class.getSimpleName();

    public static final String ARG_POSITION = "ARG_POSITION";

    private final static Cleaner cleaner = new Cleaner(Whitelist.relaxed().addTags("video"));

    private static String css = null;

    private Item item;

    private static final String feedColorCss = "a:link, a:active,a:hover { color: %s }";

    private final FaviconUtils.PaletteBitmapAsyncListener paletteAsyncListener = new FaviconUtils.PaletteBitmapAsyncListener() {
        @Override
        public void onGenerated(Palette palette, Bitmap bitmap) {
            if (palette != null) {
                int titleColor = palette.getDarkVibrantColor(ContextCompat.getColor(getActivity(), R.color.primary_text));
                String cssColor = getCssColor(titleColor);
                String javascript = String.format("javascript:(function(){document.styleSheets[0].cssRules[0].style.color=\"%s\";})()", cssColor);
                getWebView().loadUrl(javascript);
            }
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
        // Use US locale so we always get a . as decimal separator for a valid css value
        return String.format(Locale.US,"rgba(%d,%d,%d,%.2f)",
                Color.red(color),
                Color.green(color),
                Color.blue(color),
                Color.alpha(color) / 255.0);
    }

    @Override
    public void onStart() {
        super.onStart();

        final ItemPagerActivity activity = (ItemPagerActivity) getActivity();
        item = activity.getItemForPosition(getArguments().getInt(ARG_POSITION));
        final Feed feed = Item.feed(item);

        getWebView().setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                FaviconUtils.getInstance().loadFavicon(activity, feed, paletteAsyncListener);
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW);
                browserIntent.setData(Uri.parse(url));
                getActivity().startActivity(browserIntent);
                return true;
            }
        });
        loadWebViewData(activity);
    }

    private void loadWebViewData(Context context) {
        getWebView().loadDataWithBaseURL(null, getHtml(context, item), "text/html", "UTF-8", null);
    }

    private String getHtml(Context context, Item item) {
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

        Document document = Jsoup.parse(item.getBody());
        prepareDocument(document);
        document = cleaner.clean(document);

        StringBuilder pageBuilder = new StringBuilder(
                String.format(
                        "<html><head><style type=\"text/css\">%s</style></head><body>",
                        String.format(feedColorCss, getCssColor(titleColor)) + css
                )
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

    // All js from external sites gets stripped using jsoup
    @SuppressLint("SetJavaScriptEnabled")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View childView = super.onCreateView(inflater, container, savedInstanceState);
        View rootView = inflater.inflate(R.layout.fragment_item_pager, container, false);

        NestedScrollView nestedScrollView = (NestedScrollView) rootView.findViewById(R.id.nestedscrollview);
        nestedScrollView.addView(childView, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);

        getWebView().getSettings().setJavaScriptEnabled(true);

        // Using software rendering to prevent frozen or blank webviews
        // See https://code.google.com/p/chromium/issues/detail?id=501901
        if(Build.HARDWARE.equals("qcom") && Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            Log.w(TAG, "Using software rendering");
            rootView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        }

        return rootView;
    }
}
