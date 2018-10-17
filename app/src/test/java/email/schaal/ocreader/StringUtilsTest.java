package email.schaal.ocreader;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.Date;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import email.schaal.ocreader.util.StringUtils;

import static org.junit.Assert.assertEquals;

/**
 * Test class for StringUtils
 */
@RunWith(AndroidJUnit4.class)
public class StringUtilsTest {
    @Test
    public void testGetByLine() throws Exception {
        assertEquals("", StringUtils.getByLine(ApplicationProvider.getApplicationContext(), "<p class=\"byline\">%s</p>", null));
        assertEquals("<p class=\"byline\">by testAuthor</p>", StringUtils.getByLine(ApplicationProvider.getApplicationContext(), "<p class=\"byline\">%s</p>", "testAuthor"));
    }

    @Test
    public void testGetTimeSpanString() throws Exception {
        Date testDateStart = new Date(1469849100000L);

        Date testDateMinute = new Date(1469849100000L + 60 * 1000);
        Date testDateHour = new Date(1469849100000L + 60 * 60 * 1000);
        Date testDateDay = new Date(1469849100000L + 24 * 60 * 60 * 1000);

        assertEquals("now", StringUtils.getTimeSpanString(ApplicationProvider.getApplicationContext(), testDateStart, testDateStart));
        assertEquals("1m", StringUtils.getTimeSpanString(ApplicationProvider.getApplicationContext(), testDateStart, testDateMinute));
        assertEquals("1h", StringUtils.getTimeSpanString(ApplicationProvider.getApplicationContext(), testDateStart, testDateHour));
        assertEquals("1d", StringUtils.getTimeSpanString(ApplicationProvider.getApplicationContext(), testDateStart, testDateDay));
    }

    @Test
    public void testCleanString() throws Exception {
        String html = "<span>Test</span>";
        String entity = "Test &gt; Test";

        assertEquals("Test", StringUtils.cleanString(html));
        assertEquals("Test > Test", StringUtils.cleanString(entity));
    }
}