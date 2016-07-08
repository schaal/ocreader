package email.schaal.ocreader.view;

import email.schaal.ocreader.model.Feed;

/**
 * Created by daniel on 02.08.16.
 */
public interface FeedManageListener {
    void addNewFeed(String url, long folderId);
    void deleteFeed(Feed feed);
    void moveFeed(Feed feed, long newFolderId, FeedViewHolder feedViewHolder);
}
