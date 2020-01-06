package email.schaal.ocreader.view

import email.schaal.ocreader.database.model.Feed

/**
 * Callbacks for feed management
 */
interface FeedManageListener {
    fun addNewFeed(url: String?, folderId: Long, finishAfterAdd: Boolean)
    fun deleteFeed(feed: Feed?)
    fun changeFeed(feedId: Long, folderId: Long)
    fun showFeedDialog(feed: Feed?)
}