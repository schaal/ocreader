package email.schaal.ocreader.api.json.v12

import email.schaal.ocreader.database.model.Item
import java.util.*

/**
 * Aggregates feedIds and guidHashes, used to mark multiple items as starred
 */
class ItemMap(sourceItems: Iterable<Item>) {
    val items: Iterable<Map<String, Any?>> = sourceItems.map {
        mapOf("feedId" to it.feedId, "guidHash" to it.guidHash)
    }
}