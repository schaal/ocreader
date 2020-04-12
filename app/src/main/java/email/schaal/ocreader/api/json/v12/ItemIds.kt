package email.schaal.ocreader.api.json.v12

import com.squareup.moshi.JsonClass
import email.schaal.ocreader.database.model.Item

/**
 * Aggregates item ids, used to mark multiple items as read
 */
@JsonClass(generateAdapter = true)
class ItemIds(sourceItems: Iterable<Item> = emptySet()) {
    val items: Iterable<Long> = sourceItems.map { it.id }
}