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

import android.app.ActivityManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.annotation.RequiresApi
import androidx.core.view.postDelayed
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import email.schaal.ocreader.database.model.Item
import email.schaal.ocreader.databinding.FragmentItemPagerBinding

internal class SavedStateViewModel(private val state: SavedStateHandle): ViewModel() {
    companion object {
        private const val POSITION: String = "POSITION"
    }

    var position: Float
        set(value) { state.set(POSITION, value)}
        get() { return state.get(POSITION) ?: 0f }
}

/**
 * Fragment to display a single feed item using a WebView.
 */
class ItemPageFragment : Fragment() {
    private lateinit var binding: FragmentItemPagerBinding
    private val vm: SavedStateViewModel by viewModels()

    override fun onStart() {
        super.onStart()
        binding.webView.setItem(arguments?.getParcelable(ARG_ITEM))
    }

    override fun onPause() {
        super.onPause()
        binding.webView.onPause()
    }

    override fun onResume() {
        binding.webView.onResume()
        super.onResume()
    }

    override fun onDestroy() {
        binding.webView.destroy()
        super.onDestroy()
    }

    override fun onDestroyView() {
        binding.webView.settings.javaScriptEnabled = false
        super.onDestroyView()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentItemPagerBinding.inflate(inflater, container, false)
        binding.webView.webViewClient = object: WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                if (vm.position > 0f) {
                    view?.apply {
                        postVisualStateCallback(0, object : WebView.VisualStateCallback() {
                                override fun onComplete(requestId: Long) {
                                    post {
                                        scrollY = (contentHeight * scale * vm.position).toInt()
                                    }
                                }
                            })
                    }
                }
            }

            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                    startActivity(this)
                }
                return true
            }
        }
        return binding.root
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        binding.webView.apply {
            vm.position = scrollY / (contentHeight * scale)
        }
    }

    companion object {
        private const val ARG_ITEM = "ARG_ITEM"

        fun newInstance(item: Item?): ItemPageFragment {
            return ItemPageFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_ITEM, item)
                }
            }
        }
    }
}