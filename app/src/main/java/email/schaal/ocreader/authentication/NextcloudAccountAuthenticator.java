package email.schaal.ocreader.authentication;

import android.accounts.AbstractAccountAuthenticator;
import android.accounts.Account;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.accounts.NetworkErrorException;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import hugo.weaving.DebugLog;

import static android.accounts.AccountManager.KEY_BOOLEAN_RESULT;

/**
 * Created by daniel on 9/3/17.
 */

public class NextcloudAccountAuthenticator extends AbstractAccountAuthenticator {
    private final Context context;

    public NextcloudAccountAuthenticator(Context context) {
        super(context);
        this.context = context;
    }

    @Override
    public Bundle editProperties(AccountAuthenticatorResponse response, String accountType) {
        return null;
    }

    @DebugLog
    @Override
    public Bundle addAccount(AccountAuthenticatorResponse response, String accountType, String authTokenType, String[] requiredFeatures, Bundle options) throws NetworkErrorException {
        AccountManager accountManager = AccountManager.get(context);

        final Bundle bundle = new Bundle();

        // Only one account is supported
        if(accountManager.getAccounts().length < 1) {
            final Intent intent = new Intent(context, LoginActivity.class);
            intent.putExtra(LoginActivity.ARG_ACCOUNT_TYPE, accountType);
            intent.putExtra(LoginActivity.ARG_AUTH_TYPE, authTokenType);
            intent.putExtra(LoginActivity.ARG_IS_ADDING_NEW_ACCOUNT, true);
            intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);

            bundle.putParcelable(AccountManager.KEY_INTENT, intent);
        } else {

        }
        return bundle;
    }

    @DebugLog
    @Override
    public Bundle confirmCredentials(AccountAuthenticatorResponse response, Account account, Bundle options) throws NetworkErrorException {
        return null;
    }

    @Override
    public Bundle getAuthToken(AccountAuthenticatorResponse response, Account account, String authTokenType, Bundle options) throws NetworkErrorException {
        final Bundle bundle = new Bundle();

        final AccountManager accountManager = AccountManager.get(context);
        String authToken = accountManager.peekAuthToken(account, authTokenType);

        if(authToken != null) {
            bundle.putString(AccountManager.KEY_ACCOUNT_NAME, account.name);
            bundle.putString(AccountManager.KEY_ACCOUNT_TYPE, account.type);
            bundle.putString(AccountManager.KEY_AUTHTOKEN, authToken);
        } else {
            Intent intent = new Intent();
            intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);
            intent.putExtra(LoginActivity.ARG_AUTH_TYPE, authTokenType);
            intent.putExtra(LoginActivity.EXTRA_ACCOUNT, account);
            intent.putExtra(LoginActivity.ARG_IS_ADDING_NEW_ACCOUNT, false);

            bundle.putParcelable(AccountManager.KEY_INTENT, intent);
        }

        return bundle;
    }

    @Override
    public String getAuthTokenLabel(String authTokenType) {
        return "clientflow";
    }

    @DebugLog
    @Override
    public Bundle updateCredentials(AccountAuthenticatorResponse response, Account account, String authTokenType, Bundle options) throws NetworkErrorException {
        return null;
    }

    @Override
    public Bundle hasFeatures(AccountAuthenticatorResponse response, Account account, String[] features) throws NetworkErrorException {
        final Bundle result = new Bundle();
        result.putBoolean(KEY_BOOLEAN_RESULT, false);
        return result;
    }
}
