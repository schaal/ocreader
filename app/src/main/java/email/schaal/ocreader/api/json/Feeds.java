package email.schaal.ocreader.api.json;

import java.util.List;

import email.schaal.ocreader.model.Feed;

/**
 * Class to deserialize the json response for feeds
 */
public class Feeds {
    private List<Feed> feeds;
    private int starredCount;
    private Long newestItemId;

    public int getStarredCount() {
        return starredCount;
    }

    public void setStarredCount(int starredCount) {
        this.starredCount = starredCount;
    }

    public Long getNewestItemId() {
        return newestItemId;
    }

    public void setNewestItemId(Long newestItemId) {
        this.newestItemId = newestItemId;
    }

    public List<Feed> getFeeds() {
        return feeds;
    }

    public void setFeeds(List<Feed> feeds) {
        this.feeds = feeds;
    }
}
