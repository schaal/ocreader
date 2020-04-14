package email.schaal.ocreader.api.json.v12

import com.squareup.moshi.JsonClass
import email.schaal.ocreader.api.json.Items
import email.schaal.ocreader.database.model.Item

/**
 * Aggregates item ids, used to mark multiple items as read
 */
@JsonClass(generateAdapter = true)
class ItemIds(val items: List<Long>) {
    constructor(sourceItems: Iterable<Item>) : this(sourceItems.map { it.id })
}