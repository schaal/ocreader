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
package email.schaal.ocreader

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import email.schaal.ocreader.database.model.Item
import email.schaal.ocreader.databinding.FragmentItemPagerBinding
import email.schaal.ocreader.view.ArticleWebView

/**
 * Fragment to display a single feed item using a WebView.
 */
class ItemPageFragment : Fragment() {
    private lateinit var binding: FragmentItemPagerBinding
    private lateinit var webView: ArticleWebView

    override fun onStart() {
        super.onStart()
        val item: Item? = arguments?.getParcelable(ARG_ITEM)
        webView.setItem(item)
    }

    override fun onPause() {
        super.onPause()
        webView.onPause()
    }

    override fun onResume() {
        webView.onResume()
        super.onResume()
    }

    override fun onDestroy() {
        webView.destroy()
        super.onDestroy()
    }

    override fun onDestroyView() {
        webView.settings.javaScriptEnabled = false
        super.onDestroyView()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(WEB_VIEW_SCROLL_POSITION, binding.webView.scrollY)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentItemPagerBinding.inflate(inflater, container, false)
        webView = binding.webView
        if (savedInstanceState != null)
            webView.setScrollPosition(savedInstanceState.getInt(WEB_VIEW_SCROLL_POSITION, 0))
        return binding.root
    }

    companion object {
        private const val ARG_ITEM = "ARG_ITEM"
        private const val WEB_VIEW_SCROLL_POSITION = "webViewScrollPosition"
        fun newInstance(item: Item?): ItemPageFragment {
            val bundle = Bundle()
            bundle.putParcelable(ARG_ITEM, item)
            val fragment = ItemPageFragment()
            fragment.arguments = bundle
            return fragment
        }
    }
}