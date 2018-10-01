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

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Intent;
import android.content.SharedPreferences;
import androidx.databinding.DataBindingUtil;
import android.os.Bundle;
import android.preference.PreferenceManager;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.TextView;

import email.schaal.ocreader.api.API;
import email.schaal.ocreader.api.json.Status;
import email.schaal.ocreader.databinding.ActivityLoginBinding;
import email.schaal.ocreader.util.LoginError;
import okhttp3.HttpUrl;

/**
 * A login screen that offers login via email/password.
 */
public class LoginActivity extends AppCompatActivity {
    public static final int REQUEST_CODE = 1;

    public static final String EXTRA_IMPROPERLY_CONFIGURED_CRON = "email.schaal.ocreader.extra.improperlyConfiguredCron";
    private static final int WARNING_RECEIVED = 666;
    private static final String SCHEME_ADDED = "SCHEME_ADDED";

    // UI references.
    private ActivityLoginBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_login);

        setSupportActionBar(binding.toolbarLayout.toolbar);

        binding.password.setOnEditorActionListener((textView, id, keyEvent) -> {
            if (id == R.integer.ime_login_id || id == EditorInfo.IME_NULL) {
                attemptLogin();
                return true;
            }
            return false;
        });

        final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        // Only show the home button in the action bar when already logged in
        boolean hasCredentials = Preferences.USERNAME.getString(sharedPreferences) != null;

        //noinspection ConstantConditions
        getSupportActionBar().setHomeButtonEnabled(hasCredentials);
        getSupportActionBar().setDisplayHomeAsUpEnabled(hasCredentials);

        binding.username.setText(Preferences.USERNAME.getString(sharedPreferences));
        binding.password.setText(Preferences.PASSWORD.getString(sharedPreferences));
        binding.url.setText(Preferences.URL.getString(sharedPreferences));

        binding.signInButton.setOnClickListener(view -> attemptLogin());

        binding.url.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateSecureState(true);
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
    }

    /**
     * Attempts to sign in or register the account specified by the login form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    private void attemptLogin() {

        // Reset errors.
        binding.username.setError(null);
        binding.password.setError(null);
        binding.url.setError(null);

        LoginError error = null;

        // Store values at the time of the login attempt.
        String username = binding.username.getText().toString();
        String password = binding.password.getText().toString();
        String urlString = binding.url.getText().toString();

        if(!urlString.startsWith("https://") && !urlString.startsWith("http://")) {
            urlString = "https://" + urlString;
            binding.url.setText(urlString);
            binding.url.setTag(SCHEME_ADDED);
        }

        final HttpUrl url = HttpUrl.parse(urlString);

        // Check for a valid username
        if (TextUtils.isEmpty(username)) {
            error = new LoginError(LoginError.Section.USER, getString(R.string.error_field_required));
        }

        if (TextUtils.isEmpty(password)) {
            error = new LoginError(LoginError.Section.PASSWORD, getString(R.string.error_field_required));
        }

        if (TextUtils.isEmpty(urlString)) {
            error = new LoginError(LoginError.Section.URL, getString(R.string.error_field_required));
        } else if (url == null) {
            error = new LoginError(LoginError.Section.URL, getString(R.string.error_incorrect_url));
        } else if(binding.signInButton.getTag() == null && !url.isHttps()) {
            error = new LoginError(LoginError.Section.URL, getString(R.string.error_insecure_connection));
            updateSecureState(false);
        }

        if (error != null) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            showError(error);
        } else {
            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.
            showProgress(true);

            final HttpUrl fixedUrl = url.newBuilder().addPathSegment("").build();

            API.login(this, fixedUrl, username, password, new API.APICallback<Status, LoginError>() {
                @Override
                public void onSuccess(Status status) {
                    Intent data = new Intent(Intent.ACTION_VIEW);
                    data.putExtra(EXTRA_IMPROPERLY_CONFIGURED_CRON, status.isImproperlyConfiguredCron());
                    setResult(RESULT_OK);
                    finish();
                }

                @Override
                public void onFailure(LoginError loginError) {
                    if(SCHEME_ADDED.equals(binding.url.getTag())) {
                        binding.url.setText(fixedUrl.newBuilder().scheme("http").toString());
                        binding.url.setTag(null);
                        attemptLogin();
                    } else {
                        showError(loginError);
                    }
                    showProgress(false);
                }
            });
        }
    }

    private void updateSecureState(boolean isSecure) {
        if(isSecure) {
            binding.signInButton.setTag(null);
            binding.signInButton.setText(R.string.action_sign_in);
        } else {
            binding.signInButton.setTag(WARNING_RECEIVED);
            binding.signInButton.setText(R.string.action_sign_in_insecurely);
        }
    }

    private void showError(@Nullable LoginError error) {
        if(error != null) {
            final TextView errorView;

            switch (error.getSection()) {
                case URL:
                    errorView = binding.url;
                    break;
                case USER:
                    errorView = binding.username;
                    break;
                case PASSWORD:
                    errorView = binding.password;
                    break;
                case NONE:
                default:
                    errorView = null;
                    break;
            }

            if(errorView != null) {
                errorView.setError(error.getMessage());
                errorView.requestFocus();
                binding.status.setVisibility(View.GONE);
            } else {
                binding.status.setVisibility(View.VISIBLE);
                binding.status.setText(error.getMessage());
            }
        } else {
            binding.status.setVisibility(View.GONE);
        }
    }

    /**
     * Shows the progress UI and hides the login form.
     */
    private void showProgress(final boolean show) {
        int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

        binding.loginForm.setVisibility(show ? View.GONE : View.VISIBLE);
        binding.loginForm.animate().setDuration(shortAnimTime).alpha(
                show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                binding.loginForm.setVisibility(show ? View.GONE : View.VISIBLE);
            }
        });

        binding.loginProgress.setVisibility(show ? View.VISIBLE : View.GONE);
        binding.loginProgress.animate().setDuration(shortAnimTime).alpha(
                show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                binding.loginProgress.setVisibility(show ? View.VISIBLE : View.GONE);
            }
        });
    }
}

