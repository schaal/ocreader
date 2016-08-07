package email.schaal.ocreader;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Toast;

import email.schaal.ocreader.api.APIService;
import email.schaal.ocreader.model.Feed;
import email.schaal.ocreader.model.Folder;
import email.schaal.ocreader.view.AddNewFeedDialogFragment;
import email.schaal.ocreader.view.DividerItemDecoration;
import email.schaal.ocreader.view.FeedManageListener;
import email.schaal.ocreader.view.FeedViewHolder;
import email.schaal.ocreader.view.FeedsAdapter;
import email.schaal.ocreader.view.FolderSpinnerAdapter;

public class ManageFeedsActivity extends RealmActivity implements FeedManageListener {
    private static final String TAG = ManageFeedsActivity.class.getName();

    public static final int REQUEST_CODE = 3;

    private FolderSpinnerAdapter folderSpinnerAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_feeds);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        //noinspection ConstantConditions
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        folderSpinnerAdapter = new FolderSpinnerAdapter(this, getRealm().where(Folder.class).findAllSorted(Folder.TITLE));

        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.feeds_recyclerview);
        FeedsAdapter adapter = new FeedsAdapter(this, folderSpinnerAdapter, getRealm(), this);

        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.addItemDecoration(new DividerItemDecoration(this));

        FloatingActionButton fabAddNewFeed = (FloatingActionButton) findViewById(R.id.fab_add_feed);
        fabAddNewFeed.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showAddNewFeedDialog(null);
            }
        });

        if(Intent.ACTION_SEND.equals(getIntent().getAction())) {
            showAddNewFeedDialog(getIntent().getStringExtra(Intent.EXTRA_TEXT));
        }
    }

    private void showAddNewFeedDialog(@Nullable String url) {
        AddNewFeedDialogFragment dialogFragment = new AddNewFeedDialogFragment();

        if(url != null) {
            Bundle bundle = new Bundle();
            bundle.putString(AddNewFeedDialogFragment.ARG_URL, url);
            dialogFragment.setArguments(bundle);
        }

        dialogFragment.show(getFragmentManager(), "newfeed");
    }

    public FolderSpinnerAdapter getFolderSpinnerAdapter() {
        return folderSpinnerAdapter;
    }

    @Override
    public void addNewFeed(String url, long folderId, final boolean finishAfterAdd) {
        final ProgressDialog progressDialog = showProgress(this, getString(R.string.adding_feed));

        APIService.getInstance().createFeed(getRealm(), url, folderId, new APIService.APICallback() {
            @Override
            public void onSuccess() {
                progressDialog.dismiss();
                setResult(RESULT_OK);

                if(finishAfterAdd)
                    finish();
            }

            @Override
            public void onFailure(String errorMessage) {
                progressDialog.cancel();
                showErrorMessage(getString(R.string.feed_add_failed), errorMessage);
            }
        });
    }

    @Override
    public void deleteFeed(final Feed feed) {
        new AlertDialog.Builder(this)
                .setMessage(getString(R.string.confirm_feed_deletion, feed.getTitle()))
                .setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        final ProgressDialog progressDialog = showProgress(ManageFeedsActivity.this, getString(R.string.deleting_feed, feed.getTitle()));

                        APIService.getInstance().deleteFeed(getRealm(), feed, new APIService.APICallback() {
                            @Override
                            public void onSuccess() {
                                progressDialog.dismiss();
                                setResult(RESULT_OK);
                            }

                            @Override
                            public void onFailure(String errorMessage) {
                                progressDialog.cancel();
                                showErrorMessage(getString(R.string.delete_feed_failed), errorMessage);
                            }
                        });

                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    @Override
    public void moveFeed(final Feed feed, long newFolderId, final FeedViewHolder feedViewHolder) {
        final ProgressDialog progressDialog = showProgress(this, getString(R.string.moving_feed));

        APIService.getInstance().moveFeed(getRealm(), feed, newFolderId, new APIService.APICallback() {
            @Override
            public void onSuccess() {
                progressDialog.dismiss();
                setResult(RESULT_OK);
            }

            @Override
            public void onFailure(String errorMessage) {
                progressDialog.cancel();
                // Reset folderSpinner to old folder
                feedViewHolder.updateFolderSpinner(feed);
                showErrorMessage(getString(R.string.feed_move_failed), errorMessage);
            }
        });

    }

    private void showErrorMessage(String title, String message) {
        Toast.makeText(ManageFeedsActivity.this, title + "\n" + message, Toast.LENGTH_LONG).show();
    }

    @NonNull
    private ProgressDialog showProgress(Context context, String message) {
        final ProgressDialog progressDialog = new ProgressDialog(context);

        progressDialog.setIndeterminate(true);
        progressDialog.setMessage(message);
        progressDialog.setCancelable(false);
        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.show();

        return progressDialog;
    }

}
