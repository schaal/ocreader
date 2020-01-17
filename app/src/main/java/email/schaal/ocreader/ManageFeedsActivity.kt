package email.schaal.ocreader

import android.app.Activity
import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import email.schaal.ocreader.api.API
import email.schaal.ocreader.api.API.APICallback
import email.schaal.ocreader.api.API.InstanceReadyCallback
import email.schaal.ocreader.database.model.Feed
import email.schaal.ocreader.database.model.Folder
import email.schaal.ocreader.databinding.ActivityManageFeedsBinding
import email.schaal.ocreader.view.*

class ManageFeedsActivity : RealmActivity(), FeedManageListener {
    lateinit var folderSpinnerAdapter: FolderSpinnerAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = DataBindingUtil.setContentView<ActivityManageFeedsBinding>(this, R.layout.activity_manage_feeds)
        setSupportActionBar(binding.toolbarLayout.toolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        folderSpinnerAdapter = FolderSpinnerAdapter(this, realm.where(Folder::class.java).sort(Folder::name.name).findAll())
        val adapter = FeedsAdapter(realm, this)
        binding.feedsRecyclerview.adapter = adapter
        binding.feedsRecyclerview.layoutManager = LinearLayoutManager(this)
        binding.feedsRecyclerview.addItemDecoration(DividerItemDecoration(this, R.dimen.divider_inset))
        binding.fabAddFeed.setOnClickListener { view: View? -> AddNewFeedDialogFragment.show(this@ManageFeedsActivity, null, false) }
        if (Intent.ACTION_SEND == intent.action) {
            val feed = Feed(-1)
            feed.url = intent.getStringExtra(Intent.EXTRA_TEXT)
            AddNewFeedDialogFragment.show(this, feed, true)
        }
    }

    override fun addNewFeed(url: String, folderId: Long, finishAfterAdd: Boolean) {
        val progressDialog = showProgress(this, getString(R.string.adding_feed))
        API.get(this, object : InstanceReadyCallback {
            override fun onInstanceReady(api: API) {
                api.createFeed(realm, url, folderId, object : APICallback<Void?, Throwable?> {
                    override fun onSuccess(success: Void?) {
                        progressDialog.dismiss()
                        setResult(Activity.RESULT_OK)
                        if (finishAfterAdd) finish()
                    }

                    override fun onFailure(throwable: Throwable?) {
                        progressDialog.cancel()
                        showErrorMessage(getString(R.string.feed_add_failed), throwable!!.localizedMessage)
                    }
                })
            }

            override fun onLoginFailure(e: Throwable) {
                progressDialog.cancel()
            }
        })
    }

    override fun deleteFeed(feed: Feed) {
        AlertDialog.Builder(this)
                .setMessage(getString(R.string.confirm_feed_deletion, feed.name))
                .setPositiveButton(R.string.delete) { _: DialogInterface?, _: Int ->
                    val progressDialog = showProgress(this@ManageFeedsActivity, getString(R.string.deleting_feed, feed.name))
                    API.get(this@ManageFeedsActivity, object : InstanceReadyCallback {
                        override fun onInstanceReady(api: API) {
                            api.deleteFeed(realm, feed, object : APICallback<Void?, Throwable?> {
                                override fun onSuccess(n: Void?) {
                                    progressDialog.dismiss()
                                    setResult(Activity.RESULT_OK)
                                }

                                override fun onFailure(throwable: Throwable?) {
                                    progressDialog.cancel()
                                    showErrorMessage(getString(R.string.delete_feed_failed), throwable!!.localizedMessage)
                                }
                            })
                        }

                        override fun onLoginFailure(e: Throwable) {
                            progressDialog.cancel()
                        }
                    })
                }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
    }

    override fun showFeedDialog(feed: Feed) {
        AddNewFeedDialogFragment.show(this, feed, false)
    }

    override fun changeFeed(feedId: Long, folderId: Long) {
        val feed = Feed.get(realm, feedId)
        val progressDialog = showProgress(this, getString(R.string.moving_feed))
        API.get(this, object : InstanceReadyCallback {
            override fun onInstanceReady(api: API) {
                api.moveFeed(realm, feed, folderId, object : APICallback<Void?, Throwable?> {
                    override fun onSuccess(v: Void?) {
                        progressDialog.dismiss()
                        setResult(Activity.RESULT_OK)
                    }

                    override fun onFailure(throwable: Throwable?) {
                        progressDialog.cancel()
                        showErrorMessage(getString(R.string.feed_move_failed), throwable!!.localizedMessage)
                    }
                })
            }

            override fun onLoginFailure(e: Throwable) {
                progressDialog.cancel()
            }
        })
    }

    private fun showErrorMessage(title: String, message: String) {
        Toast.makeText(this@ManageFeedsActivity, String.format("%s\n%s", title, message), Toast.LENGTH_LONG).show()
    }

    private fun showProgress(context: Context, message: String): ProgressDialog {
        val progressDialog = ProgressDialog(context)
        progressDialog.isIndeterminate = true
        progressDialog.setMessage(message)
        progressDialog.setCancelable(false)
        progressDialog.setCanceledOnTouchOutside(false)
        progressDialog.show()
        return progressDialog
    }

    companion object {
        private val TAG = ManageFeedsActivity::class.java.name
        const val REQUEST_CODE = 3
    }
}