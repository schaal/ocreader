package email.schaal.ocreader

import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import email.schaal.ocreader.api.API
import email.schaal.ocreader.database.FolderViewModel
import email.schaal.ocreader.database.model.Feed
import email.schaal.ocreader.databinding.ActivityManageFeedsBinding
import email.schaal.ocreader.view.*
import kotlinx.coroutines.launch

class ManageFeedsActivity : RealmActivity(), FeedManageListener {
    lateinit var folderSpinnerAdapter: FolderSpinnerAdapter
    private lateinit var feedsAdapter: FeedsAdapter
    private val folderViewModel by viewModels<FolderViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = DataBindingUtil.setContentView<ActivityManageFeedsBinding>(this, R.layout.activity_manage_feeds)
        setSupportActionBar(binding.toolbarLayout.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        folderSpinnerAdapter = FolderSpinnerAdapter(this)
        feedsAdapter = FeedsAdapter(this)
        binding.feedsRecyclerview.adapter = feedsAdapter

        folderViewModel.foldersLiveData.observe(this, Observer {
            folderSpinnerAdapter.updateFolders(it)
        })

        folderViewModel.feedsLiveData.observe(this, Observer {
            feedsAdapter.feeds = it
        })

        binding.feedsRecyclerview.layoutManager = LinearLayoutManager(this)
        binding.feedsRecyclerview.addItemDecoration(DividerItemDecoration(this, R.dimen.divider_inset))
        binding.fabAddFeed.setOnClickListener { AddNewFeedDialogFragment.show(this@ManageFeedsActivity, null, false) }
        if (Intent.ACTION_SEND == intent.action) {
            val feed = Feed(-1)
            feed.url = intent.getStringExtra(Intent.EXTRA_TEXT)
            AddNewFeedDialogFragment.show(this, feed, true)
        }
    }

    override fun addNewFeed(url: String, folderId: Long, finishAfterAdd: Boolean) {
        val progressDialog = showProgress(this, getString(R.string.adding_feed))
        lifecycleScope.launch {
            API(this@ManageFeedsActivity).createFeed(realm, url, folderId)
        }.invokeOnCompletion {
            if(it != null) {
                it.printStackTrace()
                showErrorMessage(getString(R.string.feed_add_failed), it.localizedMessage ?: "")
            }
            progressDialog.dismiss()
        }
    }

    override fun deleteFeed(feed: Feed) {
        AlertDialog.Builder(this)
                .setMessage(getString(R.string.confirm_feed_deletion, feed.name))
                .setPositiveButton(R.string.delete) { _: DialogInterface?, _: Int ->
                    val progressDialog = showProgress(this@ManageFeedsActivity, getString(R.string.deleting_feed, feed.name))
                    lifecycleScope.launch {
                        API(this@ManageFeedsActivity).deleteFeed(realm, feed)
                    }.invokeOnCompletion {
                        if(it != null) {
                            it.printStackTrace()
                            showErrorMessage(getString(R.string.delete_feed_failed), it.localizedMessage ?: "")
                        }
                        progressDialog.dismiss()
                    }
                }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
    }

    override fun showFeedDialog(feed: Feed) {
        AddNewFeedDialogFragment.show(this, feed, false)
    }

    override fun changeFeed(feedId: Long, folderId: Long) {
        val feed = Feed.get(realm, feedId) ?: return
        val progressDialog = showProgress(this, getString(R.string.moving_feed))
        lifecycleScope.launch {
            API(this@ManageFeedsActivity).moveFeed(realm, feed, folderId)
        }.invokeOnCompletion {
            if(it != null) {
                it.printStackTrace()
                showErrorMessage(getString(R.string.feed_move_failed), it.localizedMessage ?: "")
            }
            progressDialog.dismiss()
        }
    }

    private fun showErrorMessage(title: String, message: String) {
        Toast.makeText(this@ManageFeedsActivity, "$title\n$message", Toast.LENGTH_LONG).show()
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