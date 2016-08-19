package email.schaal.ocreader.view;

import email.schaal.ocreader.model.Feed;

/**
 * Callbacks for feed management
 */
public interface FeedManageListener {
    void addNewFeed(String url, long folderId, boolean finishAfterAdd);
    void deleteFeed(Feed feed);
    void moveFeed(Feed feed, long newFolderId, FeedViewHolder feedViewHolder);
}
