package email.schaal.ocreader.util

import android.content.Context
import email.schaal.ocreader.R
import java.io.IOException
import java.net.ConnectException
import java.net.UnknownHostException
import java.security.cert.CertificateException
import javax.net.ssl.SSLHandshakeException

/**
 * Turn login errors into human-readable strings
 */
class LoginError private constructor(val section: Section, val message: String?, val throwable: Throwable?) {
    enum class Section {
        URL, USER, PASSWORD, NONE, UNKNOWN
    }

    constructor(message: String?) : this(Section.NONE, message, null)
    constructor(section: Section, message: String?) : this(section, message, null)
    private constructor(throwable: Throwable) : this(Section.UNKNOWN, throwable.message, throwable)

    companion object {
        fun getError(context: Context, code: Int, defaultMessage: String): LoginError {
            return when (code) {
                401 -> LoginError(Section.USER, context.getString(R.string.error_incorrect_username_or_password))
                403, 404 -> LoginError(Section.URL, context.getString(R.string.error_oc_not_found))
                405 -> LoginError(Section.URL, context.getString(R.string.ncnews_too_old))
                else -> LoginError(context.getString(R.string.http_error, code) + ": " + defaultMessage)
            }
        }

        fun getError(context: Context, t: Throwable): LoginError {
            if (t is UnknownHostException) {
                return LoginError(Section.URL, context.getString(R.string.error_unknown_host))
            } else if (t is SSLHandshakeException) {
                if (t.cause is CertificateException) {
                    return LoginError(Section.URL, context.getString(R.string.untrusted_certificate))
                }
            } else if (t is ConnectException) {
                return LoginError(Section.URL, context.getString(R.string.could_not_connect))
            } else if (t is IOException) {
                return LoginError(Section.NONE, context.getString(R.string.ncnews_too_old))
            }
            return LoginError(t)
        }
    }

}