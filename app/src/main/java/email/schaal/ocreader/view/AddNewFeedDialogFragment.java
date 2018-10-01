package email.schaal.ocreader.view;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.os.Bundle;
import androidx.annotation.Nullable;

import email.schaal.ocreader.ManageFeedsActivity;
import email.schaal.ocreader.R;
import email.schaal.ocreader.database.model.Feed;
import email.schaal.ocreader.databinding.FragmentAddNewFeedBinding;

/**
 * Display form to add new Feed
 */
public class AddNewFeedDialogFragment extends DialogFragment {
    public static final String ARG_URL = "url";
    public static final String ARG_FOLDER_ID = "folder_id";
    public static final String ARG_FINISH_AFTER_CLOSE = "finish_after_close";
    private static final String ARG_FEED_ID = "feed_id";

    private FeedManageListener listener;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        try {
           listener = (FeedManageListener) context;
        } catch(ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement FeedManageListener");
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        ManageFeedsActivity activity = (ManageFeedsActivity) getActivity();

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);

        final FragmentAddNewFeedBinding binding = FragmentAddNewFeedBinding.inflate(activity.getLayoutInflater());

        final Bundle arguments = getArguments();
        final long feedId = arguments.getLong(ARG_FEED_ID, -1);
        final boolean newFeed = feedId < 0;

        builder.setTitle(newFeed ? R.string.add_new_feed : R.string.edit_feed);

        binding.feedUrl.setEnabled(newFeed);

        final boolean finishAfterClose;

        binding.folder.setAdapter(activity.getFolderSpinnerAdapter());

        binding.feedUrl.setText(arguments.getString(ARG_URL));
        binding.folder.setSelection(activity.getFolderSpinnerAdapter().getPosition(arguments.getLong(ARG_FOLDER_ID, 0)));
        finishAfterClose = arguments.getBoolean(ARG_FINISH_AFTER_CLOSE, false);

        builder.setPositiveButton(newFeed ? R.string.add : R.string.save, (dialogInterface, i) -> {
            if(listener != null) {
                if(newFeed)
                    listener.addNewFeed(binding.feedUrl.getText().toString(), binding.folder.getSelectedItemId(), finishAfterClose);
                else
                    listener.changeFeed(feedId, binding.folder.getSelectedItemId());
            }
        });

        builder.setView(binding.getRoot());

        return builder.create();
    }

    /**
     * Show feed edit dialog
     * @param activity Activity to get the FragmentManager
     * @param feed feed to edit or add (id < 0 means add new feed, id >=0 means edit existing feed)
     * @param finishAfterClose should the activity be finished after operation was successful
     */
    public static void show(Activity activity, @Nullable Feed feed, boolean finishAfterClose) {
        AddNewFeedDialogFragment dialogFragment = new AddNewFeedDialogFragment();

        Bundle bundle = new Bundle();

        bundle.putBoolean(ARG_FINISH_AFTER_CLOSE, finishAfterClose);

        if(feed != null) {
            bundle.putString(ARG_URL, feed.getUrl());
            if (feed.getId() >= 0) {
                bundle.putLong(ARG_FEED_ID, feed.getId());
                bundle.putLong(ARG_FOLDER_ID, feed.getFolderId());
            }
        }

        dialogFragment.setArguments(bundle);
        dialogFragment.show(activity.getFragmentManager(), "newfeed");
    }
}
