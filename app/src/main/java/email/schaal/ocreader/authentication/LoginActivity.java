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

package email.schaal.ocreader.authentication;

import android.accounts.Account;
import android.accounts.AccountAuthenticatorActivity;
import android.accounts.AccountManager;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatDelegate;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;

import com.github.zafarkhaja.semver.ParseException;
import com.github.zafarkhaja.semver.Version;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Collections;

import email.schaal.ocreader.R;
import email.schaal.ocreader.databinding.ActivityLoginBinding;
import email.schaal.ocreader.util.LoginError;
import okhttp3.HttpUrl;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.moshi.MoshiConverterFactory;
import retrofit2.http.GET;

/**
 * A login screen that offers login via email/password.
 */
public class LoginActivity extends AccountAuthenticatorActivity {
    private static final String TAG = LoginActivity.class.getName();

    public static final int REQUEST_CODE = 1;

    public static final String NC_LOGIN_PREFIX = "nc://login/";

    public static final String ARG_ACCOUNT_TYPE = "ACCOUNT_TYPE";
    public static final String ARG_AUTH_TYPE = "AUTH_TYPE";
    public static final String ARG_IS_ADDING_NEW_ACCOUNT = "IS_ADDING_NEW_ACCOUNT";
    public static final String KEY_BASE_URL = "KEY_BASE_URL";
    public static final String KEY_USER = "KEY_USER";

    public static final String EXTRA_ACCOUNT = "EXTRA_ACCOUNT";

    private static final Version MIN_NC_VERSION = Version.forIntegers(12);

    public static final String EXTRA_IMPROPERLY_CONFIGURED_CRON = "email.schaal.ocreader.extra.improperlyConfiguredCron";
    private static final int WARNING_RECEIVED = 666;
    private static final String SCHEME_ADDED = "SCHEME_ADDED";

    // UI references.
    private ActivityLoginBinding binding;
    private AccountManager accountManager;
    private String urlScheme;

    private AppCompatDelegate delegate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        delegate = AppCompatDelegate.create(this, null);
        delegate.onCreate(savedInstanceState);

        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        delegate.setContentView(binding.getRoot());

        delegate.setSupportActionBar(binding.toolbarLayout.toolbar);

        binding.signInButton.setOnClickListener(view -> attemptLogin());

        final WebSettings settings = binding.loginWebView.getSettings();

        settings.setAllowFileAccess(false);
        settings.setJavaScriptEnabled(true);
        settings.setUserAgentString(getString(R.string.app_name));

        accountManager = AccountManager.get(this);

        binding.loginWebView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if(url.startsWith(NC_LOGIN_PREFIX)) {
                    parseFlowResponse(url);
                    return true;
                }
                return false;
            }
        });

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

    private void parseFlowResponse(final String url) {
        final FlowCredentials flowCredentials = new FlowCredentials(url);

        if(flowCredentials.valid()) {
            Log.d(TAG, flowCredentials.toString());
            String accountName = String.format("%s@%s", flowCredentials.user, flowCredentials.server);

            final Account account = new Account(accountName, "email.schaal.ocreader");
            final Bundle userData = new Bundle(2);
            userData.putString(KEY_BASE_URL, String.format("%s://%s", urlScheme, flowCredentials.server));
            userData.putString(KEY_USER, flowCredentials.user);

            accountManager.addAccountExplicitly(account, flowCredentials.password, userData);
            accountManager.setAuthToken(account, "clientflow", flowCredentials.password);

            final Intent intent = new Intent();
            intent.putExtra(AccountManager.KEY_ACCOUNT_TYPE, "email.schaal.ocreader");
            intent.putExtra(AccountManager.KEY_ACCOUNT_NAME, accountName);

            setAccountAuthenticatorResult(intent.getExtras());
            setResult(RESULT_OK, intent);
        }
        finish();
    }

    private class FlowCredentials {
        String server;
        String user;
        String password;

        private FlowCredentials(String dataUrl) {
            dataUrl = dataUrl.substring(NC_LOGIN_PREFIX.length());
            final String[] values = dataUrl.split("&");
            for(String value: values) {
                if(value.startsWith("user:")) {
                    user = value(value, "user:");
                } else if(value.startsWith("password:")) {
                    password = value(value, "password:");
                } else if(value.startsWith("server:")) {
                    server = value(value, "server:");
                }
            }
        }

        @Nullable
        private String value(final String value, final String prefix) {
            try {
                return URLDecoder.decode(value.substring(prefix.length()), "UTF-8");
            } catch (UnsupportedEncodingException e) {
                return null;
            }
        }

        private boolean valid() {
            return server != null && user != null && password != null;
        }
    }

    private static class Status {
        private String productname;
        private String versionstring;
    }

    private interface StatusAPI {
        @GET("status.php")
        Call<Status> status();
    }

    /**
     * Attempts to sign in or register the account specified by the login form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    private void attemptLogin() {

        // Reset errors.
        binding.url.setError(null);

        LoginError error = null;

        // Store values at the time of the login attempt.
        String urlString = binding.url.getText().toString();

        if(!urlString.startsWith("https://") && !urlString.startsWith("http://")) {
            urlString = "https://" + urlString;
            binding.url.setText(urlString);
            binding.url.setTag(SCHEME_ADDED);
        }

        final HttpUrl url = HttpUrl.parse(urlString);

        if (TextUtils.isEmpty(urlString)) {
            error = new LoginError(LoginError.Section.URL, getString(R.string.error_field_required));
        } else if (url == null) {
            error = new LoginError(LoginError.Section.URL, getString(R.string.error_incorrect_url));
        } else if(binding.signInButton.getTag() == null && !url.isHttps()) {
            error = new LoginError(LoginError.Section.URL, getString(R.string.error_insecure_connection));
            updateSecureState(false);
        }

        final HttpUrl fixedUrl = url != null ? url.newBuilder().addPathSegment("").build() : null;
        final HttpUrl loginFlowUrl = fixedUrl != null ? fixedUrl.resolve("index.php/login/flow") : null;

        if(loginFlowUrl == null)
            error = new LoginError(LoginError.Section.URL, getString(R.string.error_incorrect_url));

        if (error != null) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            showError(error);
        } else {
            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.
            showProgress(true);

            urlScheme = loginFlowUrl.scheme();

            final Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(fixedUrl)
                    .addConverterFactory(MoshiConverterFactory.create())
                    .build();

            final StatusAPI statusAPI = retrofit.create(StatusAPI.class);
            statusAPI.status().enqueue(new Callback<Status>() {
                @Override
                public void onResponse(@NonNull final Call<Status> call, @NonNull final Response<Status> response) {
                    final Status status = response.body();

                    showProgress(false);

                    LoginError error = null;

                    if(response.isSuccessful() && status != null) {
                        try {
                            final Version version = Version.valueOf(status.versionstring);
                            if("Nextcloud".equals(status.productname) && MIN_NC_VERSION.lessThanOrEqualTo(version)) {
                                binding.loginWebView.loadUrl(loginFlowUrl.toString(), Collections.singletonMap("OCS-APIREQUEST", "true"));
                                binding.loginSwitcher.setDisplayedChild(1);
                            } else {
                                // TODO: 10/28/17 Version warning
                            }
                        } catch (ParseException e) {
                            e.printStackTrace();
                            error = LoginError.getError(LoginActivity.this, e);
                        }
                    } else {
                        error = LoginError.getError(LoginActivity.this, response.code(), response.message());
                    }

                    if(error != null)
                        showError(error);
                }

                @Override
                public void onFailure(@NonNull final Call<Status> call, @NonNull final Throwable t) {
                    t.printStackTrace();
                    showProgress(false);
                    showError(new LoginError(t.getLocalizedMessage()));
                }
            });

/* TODO:
            API.login(this, fixedUrl, username, password, new API.APICallback<NewsStatus, LoginError>() {
                @Override
                public void onSuccess(NewsStatus status) {
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
*/
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

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        delegate.onPostCreate(savedInstanceState);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        delegate.onConfigurationChanged(newConfig);
    }

    @Override
    protected void onStart() {
        super.onStart();
        delegate.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
        delegate.onStop();
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        delegate.onPostResume();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        delegate.onSaveInstanceState(outState);
    }

    @Override
    public void setTitle(CharSequence title) {
        super.setTitle(title);
        delegate.setTitle(title);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        delegate.onDestroy();
    }
}

