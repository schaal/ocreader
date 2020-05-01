package email.schaal.ocreader.api.json

import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonClass
import com.squareup.moshi.ToJson
import email.schaal.ocreader.database.model.Item
import email.schaal.ocreader.util.cleanString
import java.util.*

class ItemJsonTypeAdapter {
    @FromJson
    fun fromJson(jsonItem: JsonItem) : Item {
        return Item().apply {
            id = jsonItem.id
            guid = jsonItem.guid
            guidHash = jsonItem.guidHash
            url = jsonItem.url
            title = jsonItem.title?.cleanString()
            author = jsonItem.author?.cleanString()
            pubDate = Date(jsonItem.pubDate * 1000)
            body = jsonItem.body
            enclosureLink = jsonItem.enclosureLink
            feedId = jsonItem.feedId
            unread = jsonItem.unread
            starred = jsonItem.starred
            lastModified = Date(jsonItem.lastModified * 1000)
            fingerprint = jsonItem.fingerprint
            contentHash = jsonItem.contentHash
        }
    }

    @ToJson
    fun toJson(item: Item) : Map<String, Any?> {
        return mutableMapOf<String, Any?>(
                "id" to item.id,
                "contentHash" to item.contentHash
        ). also {
            if(item.unreadChanged) it["isUnread"] = item.unread
            if(item.starredChanged) it["isStarred"] = item.starred
        }
    }
}

@JsonClass(generateAdapter = true)
class JsonItem(
        val id: Long,
        val guid: String?,
        val guidHash: String?,
        val url: String?,
        val title: String?,
        val author: String?,
        val pubDate: Long,
        val body: String,
        val enclosureMime: String?,
        val enclosureLink: String?,
        val feedId: Long,
        val unread: Boolean,
        val starred: Boolean,
        val lastModified: Long,
        val fingerprint: String?,
        val contentHash: String?
)