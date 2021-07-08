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
import android.webkit.WebView
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
        binding.webView.apply {
            setItem(arguments?.getParcelable(ARG_ITEM))
            if (vm.position > 0f) {
                postVisualStateCallback(0, object : WebView.VisualStateCallback() {
                    override fun onComplete(requestId: Long) {
                        scrollY = (contentHeight * resources.displayMetrics.density * vm.position).toInt()
                    }
                })
            }
        }
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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentItemPagerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        binding.webView.apply {
            vm.position = scrollY / (contentHeight * resources.displayMetrics.density)
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