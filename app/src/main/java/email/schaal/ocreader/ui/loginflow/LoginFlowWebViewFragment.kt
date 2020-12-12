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

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import email.schaal.ocreader.R
import email.schaal.ocreader.databinding.FragmentLoginFlowWebViewBinding
import email.schaal.ocreader.view.LoginViewModel
import java.net.URLDecoder

/**
 * A simple [Fragment] subclass.
 * Use the [LoginFlowWebViewFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class LoginFlowWebViewFragment : Fragment() {
    private val loginViewModel: LoginViewModel by activityViewModels()

    private lateinit var binding: FragmentLoginFlowWebViewBinding

    private lateinit var url: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        url = arguments?.getString(ARG_URL) ?: ""
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        binding = FragmentLoginFlowWebViewBinding.inflate(inflater, container, false)
        binding.webViewLogin.settings.also {
            it.javaScriptEnabled = true
            it.userAgentString = "${getString(R.string.app_name)} on ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}"
        }
        binding.webViewLogin.webViewClient = object: WebViewClient() {
            // For API < 24 the WebResourceRequest variant is not defined
            override fun shouldOverrideUrlLoading(view: WebView, urlString: String): Boolean {
                return checkUrl(urlString.toUri())
            }

            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                return checkUrl(request.url)
            }

            private fun checkUrl(url: Uri): Boolean {
                if(url.scheme == "nc") {
                    val encodedPath = url.encodedPath ?: throw IllegalStateException("url path is null")

                    val decodedCredentials = credentialRegex.matchEntire(encodedPath)?.groupValues?.map { URLDecoder.decode(it, "UTF-8") } ?: throw IllegalStateException("couldn't match credentials")

                    loginViewModel.credentialsLiveData.value = mapOf(
                            "server" to decodedCredentials[1],
                            "user" to decodedCredentials[2],
                            "password" to (decodedCredentials[3])
                    )
                    return true
                }
                return false
            }
        }
        binding.webViewLogin.loadUrl(url, mapOf("OCS-APIREQUEST" to "true"))
        return binding.root
    }

    companion object {
        private const val ARG_URL = "ARG_URL"
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
