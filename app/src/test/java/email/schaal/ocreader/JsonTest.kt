package email.schaal.ocreader

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import email.schaal.ocreader.api.json.FeedTypeAdapter
import email.schaal.ocreader.api.json.ItemTypeAdapter
import email.schaal.ocreader.database.model.Feed
import email.schaal.ocreader.database.model.Item
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class JsonTest {
    @JsonClass(generateAdapter = true)
    private class ReducedItem {
        var id: Long = 0
        var contentHash: String? = null
        var isUnread: Boolean? = null
        var isStarred: Boolean? = null
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is ReducedItem) return false

            if (id != other.id) return false
            if (contentHash != other.contentHash) return false
            if (isUnread != other.isUnread) return false
            if (isStarred != other.isStarred) return false

            return true
        }

        override fun hashCode(): Int {
            var result = id.hashCode()
            result = 31 * result + (contentHash?.hashCode() ?: 0)
            result = 31 * result + (isUnread?.hashCode() ?: 0)
            result = 31 * result + (isStarred?.hashCode() ?: 0)
            return result
        }
    }

    @Test
    @Throws(IOException::class)
    fun TestJsonWithUnexpectedNull() {
        val moshi = Moshi.Builder().add<Feed>(Feed::class.java, FeedTypeAdapter()).build()
        val feedJson = "{\"id\":28,\"url\":\"http://rss.slashdot.org/Slashdot/slashdot\",\"title\":\"Slashdot\",\"faviconLink\":null,\"added\":1435334890,\"folderId\":0,\"unreadCount\":1093,\"ordering\":null,\"link\":\"http://slashdot.org/\",\"pinned\":false}"
        val feed = moshi.adapter(Feed::class.java).fromJson(feedJson)
        Assert.assertNull(feed!!.faviconLink)
    }

    @Test
    @Throws(IOException::class)
    fun TestItemToJson() {
        val moshi = Moshi.Builder()
                .add<Item>(Item::class.java, ItemTypeAdapter())
                .build()

        val item = Item.Builder().also {
            it.id = 1
            it.contentHash = "oijoijo"
            it.unreadChanged = true
            it.unread = false
        }.build()

        val itemJson = moshi.adapter(Item::class.java).toJson(item)
        val reducedItem = moshi.adapter(ReducedItem::class.java).fromJson(itemJson)
        val expectedReducedItem = ReducedItem()
        expectedReducedItem.id = 1
        expectedReducedItem.contentHash = "oijoijo"
        expectedReducedItem.isUnread = false
        expectedReducedItem.isStarred = null
        Assert.assertEquals(expectedReducedItem, reducedItem)
    }
}