package email.schaal.ocreader;

import android.graphics.Color;
import android.os.Build;
import android.support.v4.content.ContextCompat;
import android.util.Pair;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.List;

import email.schaal.ocreader.model.Feed;
import email.schaal.ocreader.util.FaviconLoader;
import email.schaal.ocreader.util.TextDrawable;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Tests for FaviconLoader
 */
@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, sdk = Build.VERSION_CODES.M, application = TestApplication.class)
public class FaviconLoaderTest {

    @Test
    public void testGetCssColor() throws Exception {
        List<Pair<Integer, String>> colorPairs = new ArrayList<>(3);

        colorPairs.add(new Pair<>(Color.argb(30,30,30,30), "rgba(30,30,30,0.12)"));
        colorPairs.add(new Pair<>(Color.argb(0,0,0,0), "rgba(0,0,0,0.00)"));
        colorPairs.add(new Pair<>(Color.argb(255,255,255,255), "rgba(255,255,255,1.00)"));

        for(Pair<Integer, String> colorPair: colorPairs)
            assertEquals(colorPair.second, FaviconLoader.getCssColor(colorPair.first));
    }

    @Test
    public void testGetDrawable() throws Exception {
        Feed feed = new Feed();
        feed.setName("Test");
        feed.setUrl("http://example.com");
        feed.setFaviconLink(null);

        assertTrue(FaviconLoader.getDrawable(RuntimeEnvironment.application, feed) instanceof TextDrawable);
        assertEquals(ContextCompat.getDrawable(RuntimeEnvironment.application, R.drawable.ic_feed_icon), FaviconLoader.getDrawable(RuntimeEnvironment.application, null));

        // TODO: 01.08.16 Test feed with favicon
    }

    @Test
    public void testGetFeedColor() throws Exception {

    }

    @Test
    public void testLoad() throws Exception {

    }
}