package email.schaal.ocreader.view;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import email.schaal.ocreader.R;
import email.schaal.ocreader.model.Feed;
import io.realm.Realm;
import io.realm.RealmRecyclerViewAdapter;

/**
 * Created by daniel on 08.07.16.
 */
public class FeedsAdapter extends RealmRecyclerViewAdapter<Feed, RecyclerView.ViewHolder> {
    private final FeedManageListener listener;
    private final FolderSpinnerAdapter folderSpinnerAdapter;

    private final static int VIEW_TYPE_FEED = 0;
    private final static int VIEW_TYPE_EMPTY = 1;

    public FeedsAdapter(Context context, FolderSpinnerAdapter folderSpinnerAdapter, Realm realm, FeedManageListener listener) {
        super(context, realm.where(Feed.class).findAllSorted(Feed.TITLE), true);
        this.folderSpinnerAdapter = folderSpinnerAdapter;
        this.listener = listener;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view;
        switch (viewType) {
            case VIEW_TYPE_FEED:
                view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_feed, parent, false);
                return new FeedViewHolder(folderSpinnerAdapter, view, listener);
        }
        return null;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if(holder instanceof FeedViewHolder) {
            Feed feed = getItem(position);
            ((FeedViewHolder)holder).bindFeed(feed);
        }

    }

    @Override
    public int getItemViewType(int position) {
        return VIEW_TYPE_FEED;
    }
}
