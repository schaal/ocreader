package email.schaal.ocreader

import androidx.test.espresso.Espresso
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.rule.ActivityTestRule
import email.schaal.ocreader.LoginActivity
import email.schaal.ocreader.api.API
import email.schaal.ocreader.database.Queries
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

/**
 * Created by daniel on 13.10.16.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class LoginActivityTest {
    @Rule
    val activityTestRule = ActivityTestRule(LoginActivity::class.java)
    private val server = MockWebServer()
    private val dispatcher = APIDispatcher()
    @Before
    @Throws(Exception::class)
    fun setUp() {
        Queries.resetDatabase()
        server.start()
    }

    @After
    @Throws(Exception::class)
    fun tearDown() {
        Queries.resetDatabase()
        server.shutdown()
    }

    @Test
    @Throws(IOException::class)
    fun testInsecureLogin() {
        val baseUrl = server.url("")
        Espresso.onView(ViewMatchers.withId(R.id.url)).perform(ViewActions.clearText(), ViewActions.typeText(baseUrl.toString()))
        Espresso.onView(ViewMatchers.withId(R.id.username)).perform(ViewActions.clearText(), ViewActions.typeText("admin"))
        Espresso.onView(ViewMatchers.withId(R.id.password)).perform(ViewActions.clearText(), ViewActions.typeText("admin"))
        Espresso.onView(ViewMatchers.withId(R.id.sign_in_button)).perform(ViewActions.scrollTo(), ViewActions.click())
        Espresso.onView(ViewMatchers.withId(R.id.url)).check(ViewAssertions.matches(ViewMatchers.hasErrorText(activityTestRule.activity.getString(R.string.error_insecure_connection))))
        Espresso.onView(ViewMatchers.withId(R.id.sign_in_button)).perform(ViewActions.scrollTo(), ViewActions.click())
    }

    @Test
    @Throws(IOException::class)
    fun testUnknownHost() {
        Espresso.onView(ViewMatchers.withId(R.id.url)).perform(ViewActions.clearText(), ViewActions.typeText("https://unknown-host"))
        Espresso.onView(ViewMatchers.withId(R.id.username)).perform(ViewActions.clearText(), ViewActions.typeText("admin"))
        Espresso.onView(ViewMatchers.withId(R.id.password)).perform(ViewActions.clearText(), ViewActions.typeText("admin"))
        Espresso.onView(ViewMatchers.withId(R.id.sign_in_button)).perform(ViewActions.scrollTo(), ViewActions.click())
        Espresso.onView(ViewMatchers.withId(R.id.url)).check(ViewAssertions.matches(ViewMatchers.hasErrorText(activityTestRule.activity.getString(R.string.error_unknown_host))))
    }

    @Test
    @Throws(IOException::class)
    fun testOutdatedVersion() {
        val originalVersion = dispatcher.version
        dispatcher.version = "8.8.0"
        val baseUrl = server.url("")
        Espresso.onView(ViewMatchers.withId(R.id.url)).perform(ViewActions.clearText(), ViewActions.typeText(baseUrl.toString()))
        Espresso.onView(ViewMatchers.withId(R.id.username)).perform(ViewActions.clearText(), ViewActions.typeText("admin"))
        Espresso.onView(ViewMatchers.withId(R.id.password)).perform(ViewActions.clearText(), ViewActions.typeText("admin"))
        Espresso.onView(ViewMatchers.withId(R.id.sign_in_button)).perform(ViewActions.scrollTo(), ViewActions.click())
        Espresso.onView(ViewMatchers.withId(R.id.url)).check(ViewAssertions.matches(ViewMatchers.hasErrorText(activityTestRule.activity.getString(R.string.error_insecure_connection))))
        Espresso.onView(ViewMatchers.withId(R.id.sign_in_button)).perform(ViewActions.scrollTo(), ViewActions.click())
        Espresso.onView(ViewMatchers.withId(R.id.status)).check(ViewAssertions.matches(ViewMatchers.withText(activityTestRule.activity.getString(R.string.ncnews_too_old, API.MIN_VERSION.toString()))))
        dispatcher.version = originalVersion
    }

    init {
        server.dispatcher = dispatcher
    }
}