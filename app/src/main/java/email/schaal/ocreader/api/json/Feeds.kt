package email.schaal.ocreader.api.json

import email.schaal.ocreader.database.model.Feed

/**
 * Class to deserialize the json response for feeds
 */
class Feeds {
    var feeds: List<Feed>? = null
    var starredCount = 0
    var newestItemId: Long? = null

}