package email.schaal.ocreader;

import android.support.test.filters.LargeTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;

import email.schaal.ocreader.database.Queries;
import okhttp3.HttpUrl;
import okhttp3.mockwebserver.MockWebServer;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.clearText;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.typeText;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.hasErrorText;
import static android.support.test.espresso.matcher.ViewMatchers.withId;

/**
 * Created by daniel on 13.10.16.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class LoginActivityTest {
    @Rule
    public ActivityTestRule<LoginActivity> activityTestRule = new ActivityTestRule<>(LoginActivity.class);

    private final MockWebServer server = new MockWebServer();

    public LoginActivityTest() {
        server.setDispatcher(new APIDispatcher());
    }

    @Before
    public void setUp() throws Exception {
        Queries.resetDatabase();
        server.start();
    }

    @After
    public void tearDown() throws Exception {
        Queries.resetDatabase();
        server.shutdown();
    }

    @Test
    public void testInsecureLogin() throws IOException {
        HttpUrl baseUrl = server.url("");

        onView(withId(R.id.url)).perform(clearText(), typeText(baseUrl.toString()));
        onView(withId(R.id.username)).perform(clearText(), typeText("admin"));
        onView(withId(R.id.password)).perform(clearText(), typeText("admin"));
        onView(withId(R.id.sign_in_button)).perform(click());
        onView(withId(R.id.url)).check(matches(hasErrorText(activityTestRule.getActivity().getString(R.string.error_insecure_connection))));
        onView(withId(R.id.sign_in_button)).perform(click());
    }

    @Test
    public void testUnknownHost() throws IOException {
        onView(withId(R.id.url)).perform(clearText(), typeText("https://unknown-host"));
        onView(withId(R.id.username)).perform(clearText(), typeText("admin"));
        onView(withId(R.id.password)).perform(clearText(), typeText("admin"));
        onView(withId(R.id.sign_in_button)).perform(click());
        onView(withId(R.id.url)).check(matches(hasErrorText(activityTestRule.getActivity().getString(R.string.error_unknown_host))));
    }
}
