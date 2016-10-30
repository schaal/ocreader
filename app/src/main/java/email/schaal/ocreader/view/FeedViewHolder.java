package email.schaal.ocreader.view;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import email.schaal.ocreader.R;
import email.schaal.ocreader.database.model.Feed;
import email.schaal.ocreader.databinding.ListFeedBinding;
import email.schaal.ocreader.util.FaviconLoader;
import email.schaal.ocreader.util.FeedColors;

/**
 * ViewHolder displaying a Feed
 */
public class FeedViewHolder extends RecyclerView.ViewHolder implements FaviconLoader.FeedColorsListener {
    private final ListFeedBinding binding;
    private final FeedManageListener listener;

    public FeedViewHolder(ListFeedBinding binding, FeedManageListener listener) {
        super(binding.getRoot());

        this.binding = binding;
        this.listener = listener;
    }

    public void bindFeed(final Feed feed) {
        binding.textViewTitle.setText(feed.getName());

        binding.textViewFolder.setText(feed.getFolderTitle(itemView.getContext()));

        binding.deleteFeed.setOnClickListener(new View.OnClickListener() {
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
        new FaviconLoader.Builder(binding.imageviewFavicon, feed).build().load(itemView.getContext(), this);
    }

    @Override
    public void onGenerated(@NonNull FeedColors feedColors) {

    }

    @Override
    public void onStart() {

    }
 }
