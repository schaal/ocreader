package email.schaal.ocreader.view;

import email.schaal.ocreader.model.Feed;

/**
 * Callbacks for feed management
 */
public interface FeedManageListener {
    void addNewFeed(String url, long folderId, boolean finishAfterAdd);
    void deleteFeed(Feed feed);
    void changeFeed(String url, long feedId, long folderId);

    void showFeedDialog(Feed feed);
}
