package email.schaal.ocreader.api.json.v12

import com.squareup.moshi.JsonClass
import email.schaal.ocreader.database.model.Item

/**
 * Aggregates feedIds and guidHashes, used to mark multiple items as starred
 */
@JsonClass(generateAdapter = true)
data class ItemMap(val items: List<MappedItem>) {
    @JsonClass(generateAdapter = true)
    data class MappedItem(val feedId: Long, val guidHash: String?)

    constructor(sourceItems: Iterable<Item>) : this(sourceItems.map {
        MappedItem(it.feedId, it.guidHash)
    })
}