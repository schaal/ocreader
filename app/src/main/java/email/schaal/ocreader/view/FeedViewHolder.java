package email.schaal.ocreader.view;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import email.schaal.ocreader.R;
import email.schaal.ocreader.model.Feed;
import email.schaal.ocreader.util.FaviconLoader;
import email.schaal.ocreader.util.FeedColors;

/**
 * Created by daniel on 08.07.16.
 */
public class FeedViewHolder extends RecyclerView.ViewHolder implements FaviconLoader.FeedColorsListener {
    private final TextView textViewTitle;
    private final ImageView imageViewFavicon;
    private final Spinner folderSpinner;
    private final ImageView deleteButton;
    private final FolderSpinnerAdapter folderSpinnerAdapter;
    private final FeedManageListener listener;
    private ToggleOnItemSelectedListener onItemSelectedListener;

    public FeedViewHolder(FolderSpinnerAdapter folderSpinnerAdapter, View itemView, FeedManageListener listener) {
        super(itemView);

        this.listener = listener;
        this.folderSpinnerAdapter = folderSpinnerAdapter;

        textViewTitle = (TextView) itemView.findViewById(R.id.textViewTitle);
        imageViewFavicon = (ImageView) itemView.findViewById(R.id.imageview_favicon);
        deleteButton = (ImageView) itemView.findViewById(R.id.delete_feed);

        folderSpinner = (Spinner) itemView.findViewById(R.id.spinner);
        folderSpinner.setAdapter(folderSpinnerAdapter);
    }

    public void bindFeed(final Feed feed) {
        textViewTitle.setText(feed.getTitle());

        onItemSelectedListener = new ToggleOnItemSelectedListener() {
            @Override
            public void onRealItemSelected(AdapterView<?> parent, View view, int position, final long id) {
                listener.moveFeed(feed, id, FeedViewHolder.this);
            }
        };

        updateFolderSpinner(feed);

        folderSpinner.setOnItemSelectedListener(onItemSelectedListener);

        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View view) {
                listener.deleteFeed(feed);
            }
        });

        new FaviconLoader.Builder(imageViewFavicon, feed).build().load(this);
    }

    public void updateFolderSpinner(Feed feed) {
        onItemSelectedListener.enabled = false;
        folderSpinner.setSelection(folderSpinnerAdapter.getPosition(feed.getFolderId()));
    }

    @Override
    public void onGenerated(FeedColors feedColors) {

    }

    @Override
    public void onStart() {

    }

    private abstract class ToggleOnItemSelectedListener implements AdapterView.OnItemSelectedListener {
        // To prevent this listener getting called multiple times
        private boolean enabled = false;

        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            if(enabled)
                onRealItemSelected(parent, view, position, id);
            enabled = true;
        }

        protected abstract void onRealItemSelected(AdapterView<?> parent, View view, int position, long id);

        @Override
        public void onNothingSelected(AdapterView<?> adapterView) {

        }
    }
 }
