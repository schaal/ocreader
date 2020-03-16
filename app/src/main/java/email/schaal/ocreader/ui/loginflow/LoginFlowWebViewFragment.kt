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
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.*
import androidx.fragment.app.activityViewModels
import email.schaal.ocreader.R

import email.schaal.ocreader.databinding.FragmentLoginFlowWebViewBinding
import email.schaal.ocreader.view.LoginViewModel
import java.util.*

// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_URL = "ARG_URL"

/**
 * A simple [Fragment] subclass.
 * Use the [LoginFlowWebViewFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class LoginFlowWebViewFragment : Fragment() {
    private val loginViewModel: LoginViewModel by activityViewModels()

    private lateinit var binding: FragmentLoginFlowWebViewBinding

    private var url: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            url = it.getString(ARG_URL)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        binding = FragmentLoginFlowWebViewBinding.inflate(inflater, container, false)
        binding.webViewLogin.settings.also {
            it.javaScriptEnabled = true
            it.userAgentString = "${getString(R.string.app_name)} on ${android.os.Build.MANUFACTURER.toUpperCase(Locale.getDefault())} ${android.os.Build.MODEL}"
        }
        binding.webViewLogin.webViewClient = object: WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val url = request?.url
                if(url?.scheme == "nc") {
                    val encodedPath = url.encodedPath ?: throw IllegalStateException("url path is null")
                    val groups = credentialRegex.matchEntire(encodedPath)?.groups ?: throw IllegalStateException("couldn't match credentials")
                    loginViewModel.credentialsLiveData.value = mapOf(
                            "server" to (groups[1]?.value ?: throw IllegalStateException("server is null")),
                            "user" to (groups[2]?.value ?: throw IllegalStateException("user is null")),
                            "password" to (groups[3]?.value ?: throw IllegalStateException("password is null"))
                    )
                    return true
                }
                return false
            }
        }
        val headers = mapOf(
                "OCS-APIREQUEST" to "true"
        )
        binding.webViewLogin.loadUrl(url, headers)
        return binding.root
    }

    companion object {
        private val credentialRegex = Regex("""/server:(?<server>[^&]+)&user:(?<user>[^&]+)&password:(?<password>[^&]+)""")

        @JvmStatic
        fun newInstance(url: String) =
                LoginFlowWebViewFragment().apply {
                    arguments = Bundle().apply {
                        putString(ARG_URL, url)
                    }
                }
    }
}
