package email.schaal.ocreader

import email.schaal.ocreader.database.model.Feed
import email.schaal.ocreader.database.model.Folder
import email.schaal.ocreader.database.model.Item
import java.util.*

/**
 * Created by daniel on 14.10.16.
 */
internal object TestGenerator {
    const val FOLDER_TITLE = "TestFolderTitle"
    const val FEED_TITLE = "TestFeedTitle"
    const val ITEM_TITLE = "TestItemTitle"
    const val BODY = "<p>TestBody</p>"
    const val AUTHOR = "TestAuthor"

    val testFolder: Folder
        get() {
            return Folder().apply {
                id = 1L
                name = FOLDER_TITLE
            }
        }

    val testFeed: Feed
        get() = getTestFeed(1)

    fun getTestFeed(id: Long): Feed {
        return Feed().apply {
            this.id = id
            folderId = 0L
            name = FEED_TITLE
        }
    }

    val testItem: Item
        get() = getTestItem(1)

    fun getTestItem(id: Long): Item {
        return Item().apply {
            this.id = id
            title = ITEM_TITLE
            body = BODY
            author = AUTHOR
            feedId = 1
            feed = testFeed
            lastModified = Date()
        }
    }
}