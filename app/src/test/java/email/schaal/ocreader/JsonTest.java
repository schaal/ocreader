package email.schaal.ocreader;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.junit.Test;

import email.schaal.ocreader.model.Feed;
import email.schaal.ocreader.model.FeedTypeAdapter;

/**
 * Created by daniel on 15.02.16.
 */
public class JsonTest {
    @Test
    public void TestJsonWithUnexpectedNull() {
        Gson gson = new GsonBuilder().registerTypeAdapter(Feed.class, new FeedTypeAdapter()).create();
        String feedJson = "{\"id\":28,\"url\":\"http://rss.slashdot.org/Slashdot/slashdot\",\"title\":\"Slashdot\",\"faviconLink\":null,\"added\":1435334890,\"folderId\":0,\"unreadCount\":1093,\"ordering\":0,\"link\":\"http://slashdot.org/\",\"pinned\":false}";
        Feed feed = gson.fromJson(feedJson, Feed.class);
        assert feed.getFaviconLink() == null;
    }
}
