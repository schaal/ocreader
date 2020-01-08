package email.schaal.ocreader.api.json.v2

import email.schaal.ocreader.database.model.Feed
import email.schaal.ocreader.database.model.Folder
import email.schaal.ocreader.database.model.Item

/**
 * API response for sync call
 */
class SyncResponse {
    var folders: List<Folder>? = null
    var feeds: List<Feed>? = null
    var items: List<Item>? = null

}