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

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.preference.PreferenceManager
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import email.schaal.ocreader.api.API
import email.schaal.ocreader.databinding.ActivityLoginBinding
import email.schaal.ocreader.util.LoginError
import kotlinx.coroutines.launch
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

/**
 * A login screen that offers login via email/password.
 */
class LoginActivity : AppCompatActivity() {
    // UI references.
    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_login)
        setSupportActionBar(binding.toolbarLayout.toolbar)
        binding.password.setOnEditorActionListener { _: TextView?, id: Int, _: KeyEvent? ->
            if (id == R.integer.ime_login_id || id == EditorInfo.IME_NULL) {
                attemptLogin()
                return@setOnEditorActionListener true
            }
            false
        }
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        // Only show the home button in the action bar when already logged in
        val hasCredentials = Preferences.USERNAME.getString(sharedPreferences) != null
        supportActionBar?.setHomeButtonEnabled(hasCredentials)
        supportActionBar?.setDisplayHomeAsUpEnabled(hasCredentials)
        binding.username.setText(Preferences.USERNAME.getString(sharedPreferences))
        binding.password.setText(Preferences.PASSWORD.getString(sharedPreferences))
        binding.url.setText(Preferences.URL.getString(sharedPreferences))
        binding.signInButton.setOnClickListener { attemptLogin() }
        binding.url.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                updateSecureState(true)
            }

            override fun afterTextChanged(s: Editable) {}
        })
    }

    /**
     * Attempts to sign in or register the account specified by the login form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    private fun attemptLogin() { // Reset errors.
        binding.username.error = null
        binding.password.error = null
        binding.url.error = null
        var error: LoginError? = null
        // Store values at the time of the login attempt.
        val username = binding.username.text.toString()
        val password = binding.password.text.toString()
        var urlString = binding.url.text.toString()

        if (!urlString.startsWith("https://") && !urlString.startsWith("http://")) {
            urlString = "https://$urlString"
            binding.url.setText(urlString)
            binding.url.tag = SCHEME_ADDED
        }
        val url = urlString.toHttpUrlOrNull()
        // Check for a valid username
        if (TextUtils.isEmpty(username)) {
            error = LoginError(LoginError.Section.USER, getString(R.string.error_field_required))
        }
        if (TextUtils.isEmpty(password)) {
            error = LoginError(LoginError.Section.PASSWORD, getString(R.string.error_field_required))
        }
        if (TextUtils.isEmpty(urlString)) {
            error = LoginError(LoginError.Section.URL, getString(R.string.error_field_required))
        } else if (url == null) {
            error = LoginError(LoginError.Section.URL, getString(R.string.error_incorrect_url))
        } else if (binding.signInButton.tag == null && !url.isHttps) {
            error = LoginError(LoginError.Section.URL, getString(R.string.error_insecure_connection))
            updateSecureState(false)
        }
        if (error != null) { // There was an error; don't attempt login and focus the first
// form field with an error.
            showError(error)
        } else { // Show a progress spinner, and kick off a background task to
// perform the user login attempt.
            showProgress(true)
            val fixedUrl = url?.newBuilder()?.addPathSegment("")?.build()
            if (fixedUrl != null) {
                lifecycleScope.launch {
                    val status = API.login(this@LoginActivity, fixedUrl, username, password)
                    if(status != null) {
                        val data = Intent(Intent.ACTION_VIEW)
                        data.putExtra(EXTRA_IMPROPERLY_CONFIGURED_CRON, status.isImproperlyConfiguredCron)
                        setResult(Activity.RESULT_OK, data)
                    } else {
                        setResult(Activity.RESULT_CANCELED)
                    }
                    finish()
                }
            }
        }
    }

    private fun updateSecureState(isSecure: Boolean) {
        if (isSecure) {
            binding.signInButton.tag = null
            binding.signInButton.setText(R.string.action_sign_in)
        } else {
            binding.signInButton.tag = WARNING_RECEIVED
            binding.signInButton.setText(R.string.action_sign_in_insecurely)
        }
    }

    private fun showError(error: LoginError?) {
        if (error != null) {
            val errorView: TextView? = when (error.section) {
                LoginError.Section.URL -> binding.url
                LoginError.Section.USER -> binding.username
                LoginError.Section.PASSWORD -> binding.password
                LoginError.Section.NONE -> null
                else -> null
            }
            if (errorView != null) {
                errorView.error = error.message
                errorView.requestFocus()
                binding.status.visibility = View.GONE
            } else {
                binding.status.visibility = View.VISIBLE
                binding.status.text = error.message
            }
        } else {
            binding.status.visibility = View.GONE
        }
    }

    /**
     * Shows the progress UI and hides the login form.
     */
    private fun showProgress(show: Boolean) {
        val shortAnimTime = resources.getInteger(android.R.integer.config_shortAnimTime)
        binding.loginForm.visibility = if (show) View.GONE else View.VISIBLE
        binding.loginForm.animate().setDuration(shortAnimTime.toLong()).alpha(
                if (show) 0.toFloat() else 1.toFloat()).setListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                binding.loginForm.visibility = if (show) View.GONE else View.VISIBLE
            }
        })
        binding.loginProgress.visibility = if (show) View.VISIBLE else View.GONE
        binding.loginProgress.animate().setDuration(shortAnimTime.toLong()).alpha(
                if (show) 1.toFloat() else 0.toFloat()).setListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                binding.loginProgress.visibility = if (show) View.VISIBLE else View.GONE
            }
        })
    }

    companion object {
        const val REQUEST_CODE = 1
        const val EXTRA_IMPROPERLY_CONFIGURED_CRON = "email.schaal.ocreader.extra.improperlyConfiguredCron"
        private const val WARNING_RECEIVED = 666
        private const val SCHEME_ADDED = "SCHEME_ADDED"
    }
}