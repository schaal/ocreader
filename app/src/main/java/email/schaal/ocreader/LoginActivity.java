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
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import email.schaal.ocreader.api.API;
import email.schaal.ocreader.api.json.Status;
import email.schaal.ocreader.util.LoginError;
import okhttp3.HttpUrl;

/**
 * A login screen that offers login via email/password.
 */
public class LoginActivity extends AppCompatActivity {
    public static final int REQUEST_CODE = 1;

    public static final String EXTRA_IMPROPERLY_CONFIGURED_CRON = "email.schaal.ocreader.extra.improperlyConfiguredCron";
    private static final int WARNING_RECEIVED = 666;

    // UI references.
    private EditText mUsernameView;
    private EditText mPasswordView;
    private EditText mUrlView;
    private View mProgressView;
    private View mLoginFormView;

    private TextView mStatusView;
    private Button mSignInButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Set up the login form.
        mUsernameView = (EditText) findViewById(R.id.username);
        mPasswordView = (EditText) findViewById(R.id.password);
        mUrlView = (EditText) findViewById(R.id.url);
        mProgressView = findViewById(R.id.login_progress);
        mLoginFormView = findViewById(R.id.login_form);

        mStatusView = (TextView) findViewById(R.id.status);

        mPasswordView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == R.id.login || id == EditorInfo.IME_NULL) {
                    attemptLogin();
                    return true;
                }
                return false;
            }
        });

        final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        // Only show the home button in the action bar when already logged in
        boolean hasCredentials = Preferences.USERNAME.getString(sharedPreferences) != null;

        //noinspection ConstantConditions
        getSupportActionBar().setHomeButtonEnabled(hasCredentials);
        getSupportActionBar().setDisplayHomeAsUpEnabled(hasCredentials);

        mUsernameView.setText(Preferences.USERNAME.getString(sharedPreferences));
        mPasswordView.setText(Preferences.PASSWORD.getString(sharedPreferences));
        mUrlView.setText(Preferences.URL.getString(sharedPreferences));

        mSignInButton = (Button) findViewById(R.id.sign_in_button);
        mSignInButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptLogin();
            }
        });

        mUrlView.addTextChangedListener(new TextWatcher() {
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
        mUsernameView.setError(null);
        mPasswordView.setError(null);
        mUrlView.setError(null);

        LoginError error = null;

        // Store values at the time of the login attempt.
        String username = mUsernameView.getText().toString();
        String password = mPasswordView.getText().toString();
        String urlString = mUrlView.getText().toString();
        HttpUrl url = HttpUrl.parse(urlString);

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
        } else if(mSignInButton.getTag() == null && !url.isHttps()) {
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
            url = url.newBuilder().addPathSegment("").build();

            API.login(this, url, username, password, new API.APICallback<Status, LoginError>() {
                @Override
                public void onSuccess(Status status) {
                    Intent data = new Intent(Intent.ACTION_VIEW);
                    data.putExtra(EXTRA_IMPROPERLY_CONFIGURED_CRON, status.isImproperlyConfiguredCron());
                    setResult(RESULT_OK);
                    finish();
                }

                @Override
                public void onFailure(LoginError loginError) {
                    showError(loginError);
                    showProgress(false);
                }
            });
        }
    }

    private void updateSecureState(boolean isSecure) {
        if(isSecure) {
            mSignInButton.setTag(null);
            mSignInButton.setText(R.string.action_sign_in);
        } else {
            mSignInButton.setTag(WARNING_RECEIVED);
            mSignInButton.setText(R.string.action_sign_in_insecurely);
        }
    }

    private void showError(@Nullable LoginError error) {
        if(error != null) {
            TextView errorView = null;

            switch (error.getSection()) {
                case URL:
                    errorView = mUrlView;
                    break;
                case USER:
                    errorView = mUsernameView;
                    break;
                case PASSWORD:
                    errorView = mPasswordView;
                    break;
                case NONE:
                    errorView = null;
                    break;
            }

            if(errorView != null) {
                errorView.setError(error.getMessage());
                errorView.requestFocus();
                mStatusView.setVisibility(View.GONE);
            } else {
                mStatusView.setVisibility(View.VISIBLE);
                mStatusView.setText(error.getMessage());
            }
        } else {
            mStatusView.setVisibility(View.GONE);
        }
    }

    /**
     * Shows the progress UI and hides the login form.
     */
    private void showProgress(final boolean show) {
        int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

        mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
        mLoginFormView.animate().setDuration(shortAnimTime).alpha(
                show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
            }
        });

        mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
        mProgressView.animate().setDuration(shortAnimTime).alpha(
                show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            }
        });
    }
}

