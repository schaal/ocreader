package email.schaal.ocreader.api.json.v12

import email.schaal.ocreader.database.model.Item
import java.util.*

/**
 * Aggregates item ids, used to mark multiple items as read
 */
class ItemIds(sourceItems: Iterable<Item>) {
    val items: Iterable<Long> = sourceItems.map { it.id }
}