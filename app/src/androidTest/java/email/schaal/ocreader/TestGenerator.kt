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
            val folder = Folder(1)
            folder.name = FOLDER_TITLE
            return folder
        }

    val testFeed: Feed
        get() = getTestFeed(1)

    fun getTestFeed(id: Long): Feed {
        val feed = Feed(id)
        feed.folderId = 0L
        feed.name = FEED_TITLE
        return feed
    }

    val testItem: Item
        get() = getTestItem(1)

    fun getTestItem(id: Long): Item {
        return Item.Builder().also {
            it.id = id
            it.title = ITEM_TITLE
            it.body = BODY
            it.author = AUTHOR
            it.feedId = 1
            it.feed = testFeed
            it.lastModified = Date().time / 1000
        }.build()
    }
}