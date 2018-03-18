package email.schaal.ocreader;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.widget.LinearLayoutManager;
import android.widget.Toast;

import email.schaal.ocreader.api.API;
import email.schaal.ocreader.database.model.Feed;
import email.schaal.ocreader.database.model.Folder;
import email.schaal.ocreader.databinding.ActivityManageFeedsBinding;
import email.schaal.ocreader.view.AddNewFeedDialogFragment;
import email.schaal.ocreader.view.DividerItemDecoration;
import email.schaal.ocreader.view.FeedManageListener;
import email.schaal.ocreader.view.FeedsAdapter;
import email.schaal.ocreader.view.FolderSpinnerAdapter;

public class ManageFeedsActivity extends RealmActivity implements FeedManageListener {
    private static final String TAG = ManageFeedsActivity.class.getName();

    public static final int REQUEST_CODE = 3;

    private FolderSpinnerAdapter folderSpinnerAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActivityManageFeedsBinding binding = DataBindingUtil.setContentView(this, R.layout.activity_manage_feeds);

        setSupportActionBar(binding.toolbarLayout.toolbar);
        //noinspection ConstantConditions
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        folderSpinnerAdapter = new FolderSpinnerAdapter(this, getRealm().where(Folder.class).sort(Folder.NAME).findAll());

        FeedsAdapter adapter = new FeedsAdapter(getRealm(), this);

        binding.feedsRecyclerview.setAdapter(adapter);
        binding.feedsRecyclerview.setLayoutManager(new LinearLayoutManager(this));
        binding.feedsRecyclerview.addItemDecoration(new DividerItemDecoration(this, R.dimen.divider_inset));

        binding.fabAddFeed.setOnClickListener(view -> AddNewFeedDialogFragment.show(ManageFeedsActivity.this, null, false));

        if(Intent.ACTION_SEND.equals(getIntent().getAction())) {
            Feed feed = new Feed(-1);
            feed.setUrl(getIntent().getStringExtra(Intent.EXTRA_TEXT));
            AddNewFeedDialogFragment.show(this, feed, true);
        }
    }

    public FolderSpinnerAdapter getFolderSpinnerAdapter() {
        return folderSpinnerAdapter;
    }

    @Override
    public void addNewFeed(final String url, final long folderId, final boolean finishAfterAdd) {
        final ProgressDialog progressDialog = showProgress(this, getString(R.string.adding_feed));

        API.get(this, new API.InstanceReadyCallback() {
            @Override
            public void onInstanceReady(API api) {
                    api.createFeed(getRealm(), url, folderId, new API.APICallback<Void, Throwable>() {
                        @Override
                        public void onSuccess(Void success) {
                            progressDialog.dismiss();
                            setResult(RESULT_OK);

                            if (finishAfterAdd)
                                finish();
                        }

                        @Override
                        public void onFailure(Throwable throwable) {
                            progressDialog.cancel();
                            showErrorMessage(getString(R.string.feed_add_failed), throwable.getLocalizedMessage());
                        }
                    });
            }

            @Override
            public void onLoginFailure(Throwable e) {
                progressDialog.cancel();
            }
        });
    }

    @Override
    public void deleteFeed(final Feed feed) {
        new AlertDialog.Builder(this)
                .setMessage(getString(R.string.confirm_feed_deletion, feed.getName()))
                .setPositiveButton(R.string.delete, (dialog, which) -> {
                    final ProgressDialog progressDialog = showProgress(ManageFeedsActivity.this, getString(R.string.deleting_feed, feed.getName()));

                    API.get(ManageFeedsActivity.this, new API.InstanceReadyCallback() {
                        @Override
                        public void onInstanceReady(API api) {
                            api.deleteFeed(getRealm(), feed, new API.APICallback<Void, Throwable>() {
                                @Override
                                public void onSuccess(Void n) {
                                    progressDialog.dismiss();
                                    setResult(RESULT_OK);
                                }

                                @Override
                                public void onFailure(Throwable throwable) {
                                    progressDialog.cancel();
                                    showErrorMessage(getString(R.string.delete_feed_failed), throwable.getLocalizedMessage());
                                }
                            });
                        }

                        @Override
                        public void onLoginFailure(Throwable e) {
                            progressDialog.cancel();
                        }
                    });
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    @Override
    public void showFeedDialog(Feed feed) {
        AddNewFeedDialogFragment.show(this, feed, false);
    }

    @Override
    public void changeFeed(final long feedId, final long folderId) {
        final Feed feed = Feed.get(getRealm(), feedId);
        final ProgressDialog progressDialog = showProgress(this, getString(R.string.moving_feed));

        API.get(this, new API.InstanceReadyCallback() {
            @Override
            public void onInstanceReady(API api) {
                api.moveFeed(getRealm(), feed, folderId, new API.APICallback<Void, Throwable>() {
                    @Override
                    public void onSuccess(Void v) {
                        progressDialog.dismiss();
                        setResult(RESULT_OK);
                    }

                    @Override
                    public void onFailure(Throwable throwable) {
                        progressDialog.cancel();
                        showErrorMessage(getString(R.string.feed_move_failed), throwable.getLocalizedMessage());
                    }
                });
            }

            @Override
            public void onLoginFailure(Throwable e) {
                progressDialog.cancel();
            }
        });
    }

    private void showErrorMessage(String title, String message) {
        Toast.makeText(ManageFeedsActivity.this, String.format("%s\n%s", title, message), Toast.LENGTH_LONG).show();
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
