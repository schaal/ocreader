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
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import email.schaal.ocreader.database.model.Item;
import email.schaal.ocreader.databinding.FragmentItemPagerBinding;
import email.schaal.ocreader.view.ArticleWebView;

/**
 * Fragment to display a single feed item using a WebView.
 */
public class ItemPageFragment extends Fragment {
    private static final String ARG_ITEM = "ARG_ITEM";
    private static final String WEB_VIEW_SCROLL_POSITION = "webViewScrollPosition";

    private FragmentItemPagerBinding binding;
    private ArticleWebView webView;

    public ItemPageFragment() {
    }

    public static ItemPageFragment newInstance(Item item) {
        Bundle bundle = new Bundle();
        bundle.putParcelable(ARG_ITEM, item);

        ItemPageFragment fragment = new ItemPageFragment();
        fragment.setArguments(bundle);

        return fragment;
    }

    @Override
    public void onStart() {
        super.onStart();

        Item item = getArguments().getParcelable(ARG_ITEM);
        webView.setItem(item);
    }

    @Override
    public void onPause() {
        super.onPause();

        if(webView != null)
            webView.onPause();
    }

    @Override
    public void onResume() {
        if(webView != null)
            webView.onResume();
        super.onResume();
    }

    @Override
    public void onDestroy() {
        if(webView != null) {
            webView.destroy();
            webView = null;
        }
        super.onDestroy();
    }

    @Override
    public void onDestroyView() {
        webView.getSettings().setJavaScriptEnabled(false);
        super.onDestroyView();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(WEB_VIEW_SCROLL_POSITION, binding.webView.getScrollY());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentItemPagerBinding.inflate(inflater, container, false);
        webView = binding.webView;

        if(savedInstanceState != null)
            webView.setScrollPosition(savedInstanceState.getInt(WEB_VIEW_SCROLL_POSITION, 0));

        return binding.getRoot();
    }
}
