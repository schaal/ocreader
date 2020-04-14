package email.schaal.ocreader.api.json.v12

import com.squareup.moshi.JsonClass
import email.schaal.ocreader.database.model.Item
import java.util.*

/**
 * Aggregates feedIds and guidHashes, used to mark multiple items as starred
 */
@JsonClass(generateAdapter = true)
class ItemMap(val items: List<Map<String, Any?>>) {
    constructor(sourceItems: Iterable<Item>) : this(sourceItems.map {
        mapOf("feedId" to it.feedId, "guidHash" to it.guidHash)
    })
}