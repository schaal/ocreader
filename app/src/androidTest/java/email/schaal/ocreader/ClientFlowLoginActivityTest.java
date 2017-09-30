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

import email.schaal.ocreader.authentication.LoginActivity;
import email.schaal.ocreader.database.Queries;
import okhttp3.HttpUrl;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.clearText;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.typeText;
import static android.support.test.espresso.matcher.ViewMatchers.withId;

/**
 * Created by daniel on 10/28/17.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class ClientFlowLoginActivityTest {
    @Rule
    public final ActivityTestRule<LoginActivity> activityTestRule = new ActivityTestRule<>(LoginActivity.class);

    private final MockWebServer server = new MockWebServer();

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
    public void testLogin() throws IOException {
        final HttpUrl baseUrl = server.url("");
        server.enqueue(new MockResponse().setResponseCode(200).setBody("{\"installed\":true,\"maintenance\":false,\"needsDbUpgrade\":false,\"version\":\"12.0.3.3\",\"versionstring\":\"12.0.3\",\"edition\":\"\",\"productname\":\"Nextcloud\"}"));

        onView(withId(R.id.url)).perform(clearText(), typeText(baseUrl.toString()));
        onView(withId(R.id.sign_in_button)).perform(click());
    }

    @Test
    public void testUnknownHost() throws IOException {
        onView(withId(R.id.url)).perform(clearText(), typeText("http://unknown-host"));
        onView(withId(R.id.sign_in_button)).perform(click());
    }
}
