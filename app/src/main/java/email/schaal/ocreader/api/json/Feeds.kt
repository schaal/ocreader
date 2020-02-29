package email.schaal.ocreader.api.json

import email.schaal.ocreader.database.model.Feed

/**
 * Class to deserialize the json response for feeds
 */
class Feeds (
    val feeds: List<Feed>,
    val starredCount: Int = 0,
    val newestItemId: Long? = null
)