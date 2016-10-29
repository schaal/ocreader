package email.schaal.ocreader;

import android.os.Build;

import com.squareup.moshi.Moshi;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.IOException;

import email.schaal.ocreader.database.model.Feed;
import email.schaal.ocreader.api.json.FeedTypeAdapter;
import email.schaal.ocreader.database.model.Item;
import email.schaal.ocreader.api.json.ItemTypeAdapter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

@RunWith(RobolectricTestRunner.class)
public class JsonTest {
    private static class ReducedItem {
        long id;
        String contentHash;
        Boolean isUnread;
        Boolean isStarred;

        @Override
        public boolean equals(Object obj) {
            if(obj instanceof ReducedItem) {
                ReducedItem other = (ReducedItem) obj;
                return id == other.id && contentHash.equals(other.contentHash) && isUnread == other.isUnread && isStarred == other.isStarred;
            }
            return super.equals(obj);
        }
    }

    @Test
    public void TestJsonWithUnexpectedNull() throws IOException {
        Moshi moshi = new Moshi.Builder().add(Feed.class, new FeedTypeAdapter()).build();
        String feedJson = "{\"id\":28,\"url\":\"http://rss.slashdot.org/Slashdot/slashdot\",\"title\":\"Slashdot\",\"faviconLink\":null,\"added\":1435334890,\"folderId\":0,\"unreadCount\":1093,\"ordering\":null,\"link\":\"http://slashdot.org/\",\"pinned\":false}";
        Feed feed = moshi.adapter(Feed.class).fromJson(feedJson);
        assertNull(feed.getFaviconLink());
    }

    @Test
    public void TestItemToJson() throws IOException {
        Moshi moshi = new Moshi.Builder().add(Item.class, new ItemTypeAdapter()).build();
        Item item = new Item(1);
        item.setContentHash("oijoijo");
        item.setUnreadChanged(true);
        item.setUnread(false);

        String itemJson = moshi.adapter(Item.class).toJson(item);
        ReducedItem reducedItem = moshi.adapter(ReducedItem.class).fromJson(itemJson);
        ReducedItem expectedReducedItem = new ReducedItem();

        expectedReducedItem.id = 1;
        expectedReducedItem.contentHash = "oijoijo";
        expectedReducedItem.isUnread = false;
        expectedReducedItem.isStarred = null;

        assertEquals(expectedReducedItem, reducedItem);
    }
}
