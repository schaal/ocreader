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

import email.schaal.ocreader.databinding.FragmentItemPagerBinding;
import email.schaal.ocreader.view.ArticleWebView;

/**
 * Fragment to display a single feed item using a WebView.
 */
public class ItemPageFragment extends Fragment {
    public static final String ARG_POSITION = "ARG_POSITION";

    private FragmentItemPagerBinding binding;

    public ItemPageFragment() {
    }

    public static ItemPageFragment newInstance(int position) {
        Bundle bundle = new Bundle();
        bundle.putInt(ARG_POSITION, position);

        ItemPageFragment fragment = new ItemPageFragment();
        fragment.setArguments(bundle);

        return fragment;
    }

    @Override
    public void onStart() {
        super.onStart();

        binding.webView.setItem(((ItemPagerActivity) getActivity()).getItemForPosition(getArguments().getInt(ARG_POSITION)));
    }

    @Override
    public void onPause() {
        super.onPause();

        if(binding != null && binding.webView != null)
            binding.webView.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        if(binding != null && binding.webView != null)
            binding.webView.onResume();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentItemPagerBinding.inflate(inflater, container, false);

        return binding.getRoot();
    }
}
