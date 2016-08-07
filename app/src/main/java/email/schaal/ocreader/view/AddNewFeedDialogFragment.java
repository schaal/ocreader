package email.schaal.ocreader.view;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.Spinner;
import android.widget.TextView;

import email.schaal.ocreader.ManageFeedsActivity;
import email.schaal.ocreader.R;

/**
 * Created by daniel on 02.08.16.
 */
public class AddNewFeedDialogFragment extends DialogFragment {
    public static final String ARG_URL = "url";

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

        builder.setTitle(R.string.add_new_feed);

        View view = activity.getLayoutInflater().inflate(R.layout.fragment_add_new_feed, null);

        final Spinner folderSpinner = (Spinner) view.findViewById(R.id.folder);
        final TextView urlTextView = (TextView) view.findViewById(R.id.feed_url);

        final Bundle arguments = getArguments();

        final boolean startedFromIntent = arguments != null && arguments.containsKey(ARG_URL);

        if(startedFromIntent) {
            urlTextView.setText(arguments.getString(ARG_URL));
        }

        folderSpinner.setAdapter(activity.getFolderSpinnerAdapter());

        builder.setPositiveButton(R.string.add, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                if(listener != null)
                    listener.addNewFeed(urlTextView.getText().toString(), folderSpinner.getSelectedItemId(), startedFromIntent);
            }
        });

        builder.setView(view);

        return builder.create();
    }

}
