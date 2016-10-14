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
    static final String BODY = "TestBody";
    static final String AUTHOR = "TestAuthor";

    static Folder getTestFolder() {
        Folder folder = new Folder(1);
        folder.setName(FOLDER_TITLE);
        return folder;
    }

    static Feed getTestFeed() {
        Feed feed = new Feed();
        feed.setId(1);
        feed.setFolderId(0L);
        feed.setName(FEED_TITLE);
        return feed;
    }

    static Item getTestItem() {
        Item item = new Item();
        item.setId(1);
        item.setTitle(ITEM_TITLE);
        item.setBody(BODY);
        item.setAuthor(AUTHOR);
        item.setFeedId(1);
        item.setLastModified(new Date().getTime() / 1000);
        return item;
    }
}
