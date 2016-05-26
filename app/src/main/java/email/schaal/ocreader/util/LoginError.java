package email.schaal.ocreader.util;

import android.content.Context;

import java.net.UnknownHostException;
import java.security.cert.CertificateException;

import javax.net.ssl.SSLHandshakeException;

import email.schaal.ocreader.R;

/**
 * Turn login errors into human-readable strings
 */
public class LoginError {
    public enum Section {
        URL,
        USER,
        PASSWORD,
        NONE,
    }

    private final Section section;
    private final String message;

    public LoginError(String message) {
        this(Section.NONE, message);
    }

    public LoginError(Section section, String message) {
        this.section = section;
        this.message = message;
    }

    public Section getSection() {
        return section;
    }

    public String getMessage() {
        return message;
    }

    public boolean isEmpty() {
        return message == null || message.isEmpty();
    }

    public static LoginError getError(Context context, int code, String defaultMessage) {
        switch (code) {
            case 401:
                return new LoginError(Section.USER, context.getString(R.string.error_incorrect_username_or_password));
            case 403:
            case 404:
                return new LoginError(Section.URL, context.getString(R.string.error_oc_not_found));
            case 405:
                return new LoginError(Section.URL, context.getString(R.string.ocnews_too_old));
            default:
                return new LoginError(defaultMessage);
        }
    }

    public static LoginError getError(Context context, Throwable t) {
        if (t instanceof UnknownHostException) {
            return new LoginError(Section.URL, context.getString(R.string.error_unknown_host));
        } else if (t instanceof SSLHandshakeException) {
            if (t.getCause() instanceof CertificateException) {
                return new LoginError(Section.URL, context.getString(R.string.untrusted_certificate));
            }
        }
        return new LoginError(t.getLocalizedMessage());
    }
}
