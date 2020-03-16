package email.schaal.ocreader.util

import android.content.Context
import com.squareup.moshi.Moshi
import email.schaal.ocreader.R
import retrofit2.HttpException
import java.io.IOException
import java.net.ConnectException
import java.net.UnknownHostException
import javax.net.ssl.SSLHandshakeException

/**
 * Turn login errors into human-readable strings
 */
class LoginError private constructor(val section: Section, val message: String, val throwable: Throwable? = null) {
    enum class Section {
        URL, USER, PASSWORD, NONE, UNKNOWN
    }

    private class ErrorMessage(val message: String)

    constructor(message: String) : this(Section.NONE, message)
    private constructor(throwable: Throwable) : this(Section.UNKNOWN, throwable.message ?: "", throwable)

    companion object {
        private fun getHttpError(context: Context, code: Int, e: HttpException): LoginError {
            return when (code) {
                401 -> LoginError(Section.USER, context.getString(R.string.error_access_forbidden))
                403, 404 -> LoginError(Section.URL, context.getString(R.string.error_oc_not_found))
                405 -> LoginError(Section.URL, context.getString(R.string.ncnews_too_old))
                else -> {
                    val message = e.response()?.errorBody()?.source()?.let { source ->
                        Moshi.Builder().build().adapter(ErrorMessage::class.java).fromJson(source)?.message }
                    LoginError("${context.getString(R.string.http_error, code)}${message.let { ": $it" }}")
                }
            }
        }

        fun getError(context: Context, t: Throwable): LoginError {
            return when (t) {
                is HttpException -> getHttpError(context, t.code(), t)
                is UnknownHostException -> LoginError(Section.URL, context.getString(R.string.error_unknown_host))
                is SSLHandshakeException -> LoginError(Section.URL, context.getString(R.string.untrusted_certificate))
                is ConnectException -> LoginError(Section.URL, context.getString(R.string.could_not_connect))
                is IOException -> return LoginError(Section.NONE, context.getString(R.string.ncnews_too_old))
                else -> LoginError(t)
            }
        }
    }

}