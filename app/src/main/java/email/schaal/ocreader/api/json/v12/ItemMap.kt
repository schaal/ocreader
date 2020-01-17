package email.schaal.ocreader.api.json.v12

import email.schaal.ocreader.database.model.Item
import java.util.*

/**
 * Aggregates feedIds and guidHashes, used to mark multiple items as starred
 */
class ItemMap(items: Iterable<Item>) {
    private val items: MutableSet<Map<String, Any>> = HashSet()
    fun getItems(): Set<Map<String, Any>> {
        return items
    }

    init {
        for (item in items) {
            val itemMap = HashMap<String, Any>()
            itemMap["feedId"] = item.feedId
            itemMap["guidHash"] = item.guidHash ?: ""
            this.items.add(itemMap)
        }
    }
}