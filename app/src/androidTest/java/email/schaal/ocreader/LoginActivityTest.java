package email.schaal.ocreader;

import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import email.schaal.ocreader.database.Queries;

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
public class LoginActivityTest {
    @Rule
    public ActivityTestRule<LoginActivity> activityTestRule = new ActivityTestRule<>(LoginActivity.class);

    @Before
    @After
    public void setUp() throws Exception {
        Queries.resetDatabase();
    }

    @Test
    public void testLogin() {
        onView(withId(R.id.url)).perform(clearText(), typeText("http://10.0.2.2:23456"));
        onView(withId(R.id.username)).perform(clearText(), typeText("admin"));
        onView(withId(R.id.password)).perform(clearText(), typeText("admin"));
        onView(withId(R.id.sign_in_button)).perform(click());
        onView(withId(R.id.url)).check(matches(hasErrorText(activityTestRule.getActivity().getString(R.string.error_insecure_connection))));
        onView(withId(R.id.sign_in_button)).perform(click());
    }
}
