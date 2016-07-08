package email.schaal.ocreader;

import android.os.Build;

import com.squareup.moshi.Moshi;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.IOException;

import email.schaal.ocreader.model.Feed;
import email.schaal.ocreader.model.FeedTypeAdapter;

import static org.junit.Assert.assertNull;

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, sdk = Build.VERSION_CODES.M, application = TestApplication.class)
public class JsonTest {
    @Test
    public void TestJsonWithUnexpectedNull() throws IOException {
        Moshi moshi = new Moshi.Builder().add(Feed.class, new FeedTypeAdapter()).build();
        String feedJson = "{\"id\":28,\"url\":\"http://rss.slashdot.org/Slashdot/slashdot\",\"title\":\"Slashdot\",\"faviconLink\":null,\"added\":1435334890,\"folderId\":0,\"unreadCount\":1093,\"ordering\":null,\"link\":\"http://slashdot.org/\",\"pinned\":false}";
        Feed feed = moshi.adapter(Feed.class).fromJson(feedJson);
        assertNull(feed.getFaviconLink());
    }
}
