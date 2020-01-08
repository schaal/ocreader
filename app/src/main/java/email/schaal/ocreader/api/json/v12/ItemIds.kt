package email.schaal.ocreader.api.json.v12

import email.schaal.ocreader.database.model.Item
import java.util.*

/**
 * Aggregates item ids, used to mark multiple items as read
 */
class ItemIds(items: Iterable<Item>) {
    private val items: MutableSet<Long> = HashSet()
    fun getItems(): Set<Long> {
        return items
    }

    init {
        for (item in items) {
            this.items.add(item.id)
        }
    }
}