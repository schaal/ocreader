package email.schaal.ocreader.view;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.Spinner;
import android.widget.TextView;

import email.schaal.ocreader.ManageFeedsActivity;
import email.schaal.ocreader.R;
import email.schaal.ocreader.model.Feed;

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

        // There is no root view yet
        @SuppressLint("InflateParams")
        View view = activity.getLayoutInflater().inflate(R.layout.fragment_add_new_feed, null);

        final Spinner folderSpinner = (Spinner) view.findViewById(R.id.folder);
        final TextView urlTextView = (TextView) view.findViewById(R.id.feed_url);

        final Bundle arguments = getArguments();
        final long feedId = arguments.getLong(ARG_FEED_ID, -1);
        final boolean newFeed = feedId < 0;

        builder.setTitle(newFeed ? R.string.add_new_feed : R.string.edit_feed);

        urlTextView.setEnabled(newFeed);

        final boolean finishAfterClose;

        folderSpinner.setAdapter(activity.getFolderSpinnerAdapter());

        urlTextView.setText(arguments.getString(ARG_URL));
        folderSpinner.setSelection(activity.getFolderSpinnerAdapter().getPosition(arguments.getLong(ARG_FOLDER_ID, 0)));
        finishAfterClose = arguments.getBoolean(ARG_FINISH_AFTER_CLOSE, false);

        builder.setPositiveButton(newFeed ? R.string.add : R.string.save, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                if(listener != null) {
                    if(newFeed)
                        listener.addNewFeed(urlTextView.getText().toString(), folderSpinner.getSelectedItemId(), finishAfterClose);
                    else
                        listener.changeFeed(urlTextView.getText().toString(), feedId, folderSpinner.getSelectedItemId());
                }
            }
        });

        builder.setView(view);

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
