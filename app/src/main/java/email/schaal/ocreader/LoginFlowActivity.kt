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

package email.schaal.ocreader

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.activity.viewModels
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import email.schaal.ocreader.api.API
import email.schaal.ocreader.databinding.LoginFlowActivityBinding
import email.schaal.ocreader.ui.loginflow.LoginFlowFragment
import email.schaal.ocreader.util.LoginError
import email.schaal.ocreader.util.buildBaseUrl
import email.schaal.ocreader.view.LoginViewModel
import kotlinx.coroutines.launch
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

class LoginFlowActivity : AppCompatActivity() {
    private val loginViewModel: LoginViewModel by viewModels()

    companion object {
        const val EXTRA_URL = "email.schaal.ocreader.extra.url"
        const val EXTRA_IMPROPERLY_CONFIGURED_CRON = "email.schaal.ocreader.extra.improperlyConfiguredCron"
        const val EXTRA_MESSAGE = "email.schaal.ocreader.extra.message"
    }

    private lateinit var binding: LoginFlowActivityBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView(this, R.layout.login_flow_activity)
        setSupportActionBar(binding.toolbarLayout.toolbar)
        supportActionBar?.title = getString(R.string.title_activity_login)

        loginViewModel.credentialsLiveData.observe(this, Observer {
            val server = it?.get("server")?.let { urlString ->
                if(!urlString.endsWith("/")) "$urlString/" else urlString }?.toHttpUrlOrNull()
            val user = it?.get("user")
            val password = it?.get("password")
            if (server != null && user != null && password != null) {
                lifecycleScope.launch {
                    try {
                        API.login(this@LoginFlowActivity, server, user, password)?.let {status ->
                            setResult(Activity.RESULT_OK,
                                    Intent(Intent.ACTION_VIEW, Uri.parse(server.buildBaseUrl("index.php/apps/news").toString()))
                                            .putExtra(EXTRA_IMPROPERLY_CONFIGURED_CRON, status.isImproperlyConfiguredCron))
                        } ?: setResult(Activity.RESULT_CANCELED)
                    } catch(e: Exception) {
                        e.printStackTrace()
                        setResult(Activity.RESULT_CANCELED, Intent().apply {
                            putExtra(EXTRA_MESSAGE, LoginError.getError(this@LoginFlowActivity, e).message)
                        })
                    } finally {
                        finish()
                    }
                }
            } else {
                setResult(Activity.RESULT_CANCELED, Intent().apply {
                    putExtra(EXTRA_MESSAGE, "Couldn't parse response from server")
                })
            }
        })
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                    .replace(R.id.container, LoginFlowFragment.newInstance(intent.extras?.getString(EXTRA_URL)))
                    .commitNow()
        }
    }
}
