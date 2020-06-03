package email.schaal.ocreader.api.json

import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonClass
import com.squareup.moshi.ToJson
import email.schaal.ocreader.database.model.Feed
import email.schaal.ocreader.util.cleanString
import java.util.*

class FeedJsonTypeAdapter {
    @FromJson
    fun fromJson(jsonFeed: JsonFeed) : Feed {
        return Feed().apply {
            id = jsonFeed.id
            folderId = jsonFeed.folderId
            url = jsonFeed.url
            name = jsonFeed.title.cleanString()
            link = jsonFeed.link?.ifBlank { null }
            faviconLink = jsonFeed.faviconLink?.ifBlank { null }
            added = Date(jsonFeed.added * 1000)
            unreadCount = jsonFeed.unreadCount
            ordering = jsonFeed.ordering
            pinned = jsonFeed.pinned
            updateErrorCount = jsonFeed.updateErrorCount ?: 0
            lastUpdateError = jsonFeed.lastUpdateError
        }
    }

    @ToJson
    fun toJson(feed: Feed) : Map<String, Any?> {
        return mapOf<String, Any?>(
                "id" to feed.id,
                "title" to feed.name
        )
    }
}

@JsonClass(generateAdapter = true)
class JsonFeed(
        val id: Long,
        val folderId: Long,
        val url: String,
        val title: String,
        val link: String?,
        val faviconLink: String?,
        val added: Long,
        val unreadCount: Int,
        val ordering: Int,
        val pinned: Boolean,
        val updateErrorCount: Int?,
        val lastUpdateError: String?
)