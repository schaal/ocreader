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

import android.app.Fragment;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import email.schaal.ocreader.view.ArticleWebView;

/**
 * Fragment to display a single feed item using a WebView.
 */
public class ItemPageFragment extends Fragment {
    private static final String TAG = ItemPageFragment.class.getName();

    public static final String ARG_POSITION = "ARG_POSITION";

    private ArticleWebView webView;

    public ItemPageFragment() {
    }

    public static ItemPageFragment newInstance(int position) {
        ItemPageFragment fragment = new ItemPageFragment();
        Bundle bundle = new Bundle();
        bundle.putInt(ARG_POSITION, position);
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public void onStart() {
        super.onStart();

        webView.setItem(((ItemPagerActivity) getActivity()).getItemForPosition(getArguments().getInt(ARG_POSITION)));

    }

    @Override
    public void onPause() {
        super.onPause();
        if(webView != null)
            webView.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        if(webView != null)
            webView.onResume();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_item_pager, container, false);

        webView = (ArticleWebView) rootView.findViewById(R.id.webView);

        // Using software rendering to prevent frozen or blank webviews
        // See https://code.google.com/p/chromium/issues/detail?id=501901
        if(Build.HARDWARE.equals("qcom") && Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            Log.w(TAG, "Using software rendering");
            webView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        }

        return rootView;
    }
}
