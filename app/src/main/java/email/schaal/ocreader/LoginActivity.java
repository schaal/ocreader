/*
 * Copyright (C) 2015 Daniel Schaal <daniel@schaal.email>
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
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.github.zafarkhaja.semver.Version;
import com.squareup.okhttp.HttpUrl;

import email.schaal.ocreader.api.APIService;
import email.schaal.ocreader.http.HttpManager;
import retrofit.Call;
import retrofit.Callback;
import retrofit.Response;
import retrofit.Retrofit;

/**
 * A login screen that offers login via email/password.
 */
public class LoginActivity extends AppCompatActivity {
    public static final int RESULT_OK = 1;
    public static final int REQUEST_CODE = 1;
    /**
     * Keep track of the login task to ensure we can cancel it if requested.
     */
    private Call<Version> mAuthTask = null;

    // UI references.
    private TextView mUsernameView;
    private EditText mPasswordView;
    private EditText mUrlView;
    private View mProgressView;
    private View mLoginFormView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        setupActionBar();
        // Set up the login form.
        mUsernameView = (TextView) findViewById(R.id.username);
        mPasswordView = (EditText) findViewById(R.id.password);
        mUrlView = (EditText) findViewById(R.id.url);
        mProgressView = findViewById(R.id.login_progress);
        mLoginFormView = findViewById(R.id.login_form);

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

        mUsernameView.setText(Preferences.USERNAME.getString(sharedPreferences));
        mPasswordView.setText(Preferences.PASSWORD.getString(sharedPreferences));
        mUrlView.setText(Preferences.URL.getString(sharedPreferences));

        Button mSignInButton = (Button) findViewById(R.id.sign_in_button);
        mSignInButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptLogin();
            }
        });
    }

    /**
     * Set up the {@link android.app.ActionBar}, if the API is available.
     */
    private void setupActionBar() {
    }

    /**
     * Attempts to sign in or register the account specified by the login form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    private void attemptLogin() {
        if (mAuthTask != null) {
            return;
        }

        // Reset errors.
        mUsernameView.setError(null);
        mPasswordView.setError(null);
        mUrlView.setError(null);

        // Store values at the time of the login attempt.
        String username = mUsernameView.getText().toString();
        String password = mPasswordView.getText().toString();
        String urlString = mUrlView.getText().toString();
        HttpUrl url = HttpUrl.parse(urlString);

        boolean cancel = false;
        View focusView = null;

        // Check for a valid username
        if (TextUtils.isEmpty(username)) {
            mUsernameView.setError(getString(R.string.error_field_required));
            focusView = mUsernameView;
            cancel = true;
        }

        if (TextUtils.isEmpty(urlString)) {
            mUrlView.setError(getString(R.string.error_field_required));
            focusView = mUrlView;
            cancel = true;
        } else if (url == null) {
            mUrlView.setError(getString(R.string.error_incorrect_url));
            focusView = mUrlView;
            cancel = true;
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {
            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.
            showProgress(true);
            url = url.newBuilder().addPathSegment("").build();
            HttpManager.getInstance().setCredentials(username, password, url);
            APIService.getInstance().setupApi();
            mAuthTask = APIService.getInstance().getApi().version();
            mAuthTask.enqueue(new LoginCallback());
        }
    }

    private class LoginCallback implements Callback<Version> {
        private void onCompletion() {
            mAuthTask = null;
            showProgress(false);
        }

        @Override
        public void onResponse(Response<Version> response, Retrofit retrofit) {
            onCompletion();

            // TODO: 03.01.16 Check if version is new enough
            HttpManager.getInstance().persistCredentials(LoginActivity.this);

            setResult(RESULT_OK);
            finish();
        }

        @Override
        public void onFailure(Throwable t) {
            onCompletion();

            t.printStackTrace();

            // TODO: 03.01.16 Get the actual reason for the failure
            mPasswordView.setError(getString(R.string.error_incorrect_password));
            mPasswordView.requestFocus();
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

