package email.schaal.ocreader;

import com.squareup.moshi.Moshi;

import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.*;

import email.schaal.ocreader.model.Feed;
import email.schaal.ocreader.model.FeedTypeAdapter;

public class JsonTest {
    @Test
    public void TestJsonWithUnexpectedNull() throws IOException {
        Moshi moshi = new Moshi.Builder().add(Feed.class, new FeedTypeAdapter()).build();
        String feedJson = "{\"id\":28,\"url\":\"http://rss.slashdot.org/Slashdot/slashdot\",\"title\":\"Slashdot\",\"faviconLink\":null,\"added\":1435334890,\"folderId\":0,\"unreadCount\":1093,\"ordering\":0,\"link\":\"http://slashdot.org/\",\"pinned\":false}";
        Feed feed = moshi.adapter(Feed.class).fromJson(feedJson);
        assertNull(feed.getFaviconLink());
    }
}
