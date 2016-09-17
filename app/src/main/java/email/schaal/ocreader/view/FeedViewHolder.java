package email.schaal.ocreader.view;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import email.schaal.ocreader.R;
import email.schaal.ocreader.database.model.Feed;
import email.schaal.ocreader.util.FaviconLoader;
import email.schaal.ocreader.util.FeedColors;

/**
 * ViewHolder displaying a Feed
 */
public class FeedViewHolder extends RecyclerView.ViewHolder implements FaviconLoader.FeedColorsListener {
    private final TextView textViewTitle;
    private final ImageView imageViewFavicon;
    private final TextView textViewFolder;
    private final ImageView deleteButton;
    private final FeedManageListener listener;

    public FeedViewHolder(View itemView, FeedManageListener listener) {
        super(itemView);

        this.listener = listener;

        textViewTitle = (TextView) itemView.findViewById(R.id.textViewTitle);
        imageViewFavicon = (ImageView) itemView.findViewById(R.id.imageview_favicon);
        deleteButton = (ImageView) itemView.findViewById(R.id.delete_feed);

        textViewFolder = (TextView) itemView.findViewById(R.id.textViewFolder);
    }

    public void bindFeed(final Feed feed) {
        textViewTitle.setText(feed.getName());

        textViewFolder.setText(feed.getFolderTitle(itemView.getContext()));

        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View view) {
                listener.deleteFeed(feed);
            }
        });

        itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.showFeedDialog(feed);
            }
        });
        new FaviconLoader.Builder(imageViewFavicon, feed).build().load(itemView.getContext(), this);
    }

    @Override
    public void onGenerated(FeedColors feedColors) {

    }

    @Override
    public void onStart() {

    }
 }
