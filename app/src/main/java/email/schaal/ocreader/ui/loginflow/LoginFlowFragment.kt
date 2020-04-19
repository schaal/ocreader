/*
 * Copyright © 2020. Daniel Schaal <daniel@schaal.email>
 *
 * This file is part of ocreader.
 *
 * ocreader is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ocreader is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Foobar.  If not, see <http://www.gnu.org/licenses/>.
 */

package email.schaal.ocreader.ui.loginflow

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import email.schaal.ocreader.R
import email.schaal.ocreader.databinding.LoginFlowFragmentBinding
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

class LoginFlowFragment : Fragment() {
    private lateinit var binding: LoginFlowFragmentBinding

    private var url: String? = null

    companion object {
        private const val ARG_URL = "URL"

        @JvmStatic
        fun newInstance(url: String? = null) =
                LoginFlowFragment().apply {
                    url?.let {
                        arguments = Bundle().apply {
                            putString(ARG_URL, it)
                        }
                    }
                }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            url = it.getString(ARG_URL)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        binding = LoginFlowFragmentBinding.inflate(inflater, container, false)
        binding.inputUrl.setText(url, TextView.BufferType.EDITABLE)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.buttonLogin.setOnClickListener {
            // Check if inputUrl starts with a scheme (http or https)
            val urlString = binding.inputUrl.text?.let {
                if(!it.startsWith("http"))
                   "https://${it}"
                else
                    it
            }?.toString()

            val url = urlString
                    ?.toHttpUrlOrNull()
                    ?.newBuilder()
                    ?.addPathSegments("index.php/login/flow")

            if(url != null) {
                parentFragmentManager.beginTransaction()
                        .replace(R.id.container, LoginFlowWebViewFragment.newInstance(url.toString()))
                        .commit()
            } else {
                binding.inputUrl.error = getString(R.string.error_incorrect_url)
            }
        }
    }
}
