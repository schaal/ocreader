package email.schaal.ocreader;

import java.util.Date;

import email.schaal.ocreader.database.model.Feed;
import email.schaal.ocreader.database.model.Folder;
import email.schaal.ocreader.database.model.Item;

/**
 * Created by daniel on 14.10.16.
 */

class TestGenerator {
    static final String FOLDER_TITLE = "TestFolderTitle";
    static final String FEED_TITLE = "TestFeedTitle";
    static final String ITEM_TITLE = "TestItemTitle";
    static final String BODY = "<p>TestBody</p>";
    static final String AUTHOR = "TestAuthor";

    static Folder getTestFolder() {
        Folder folder = new Folder(1);
        folder.setName(FOLDER_TITLE);
        return folder;
    }

    static Feed getTestFeed() {
        return getTestFeed(1);
    }

    static Feed getTestFeed(long id) {
        Feed feed = new Feed(id);
        feed.setFolderId(0L);
        feed.setName(FEED_TITLE);
        return feed;
    }

    static Item getTestItem() {
        return getTestItem(1);
    }

    static Item getTestItem(long id) {
        return new Item.Builder()
                .setId(id)
                .setTitle(ITEM_TITLE)
                .setBody(BODY)
                .setAuthor(AUTHOR)
                .setFeedId(1)
                .setFeed(getTestFeed())
                .setLastModified(new Date().getTime() / 1000)
                .build();
    }
}
