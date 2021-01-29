package email.schaal.ocreader

import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import email.schaal.ocreader.api.API
import email.schaal.ocreader.database.FolderViewModel
import email.schaal.ocreader.database.model.Feed
import email.schaal.ocreader.databinding.ActivityManageFeedsBinding
import email.schaal.ocreader.view.*
import kotlinx.coroutines.launch

class ManageFeedsActivity : AppCompatActivity(), FeedManageListener {
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

        folderViewModel.foldersLiveData.observe(this, {
            folderSpinnerAdapter.updateFolders(it)
        })

        folderViewModel.feedsLiveData.observe(this, {
            feedsAdapter.feeds = it
        })

        binding.feedsRecyclerview.layoutManager = LinearLayoutManager(this)
        binding.feedsRecyclerview.addItemDecoration(DividerItemDecoration(this, R.dimen.divider_inset))
        binding.fabAddFeed.setOnClickListener { AddNewFeedDialogFragment.show(this@ManageFeedsActivity, null, false) }
        if (Intent.ACTION_SEND == intent.action || Intent.ACTION_VIEW == intent.action) {
            val feed = Feed(-1)
            feed.url = intent.getStringExtra(Intent.EXTRA_TEXT) ?: intent.dataString ?: ""
            AddNewFeedDialogFragment.show(this, feed, true)
        }
    }

    override fun addNewFeed(url: String, folderId: Long, finishAfterAdd: Boolean) {
        val progressDialog = showProgress(this, getString(R.string.adding_feed))
        lifecycleScope.launch {
            try {
                API(this@ManageFeedsActivity).createFeed(url, folderId)
            } catch(e: Exception) {
                showErrorMessage(getString(R.string.feed_add_failed), e)
            } finally {
                progressDialog.dismiss()
                if(finishAfterAdd)
                    finish()
            }
        }
    }

    override fun deleteFeed(feed: Feed) {
        AlertDialog.Builder(this)
                .setMessage(getString(R.string.confirm_feed_deletion, feed.name))
                .setPositiveButton(R.string.delete) { _: DialogInterface?, _: Int ->
                    val progressDialog = showProgress(this@ManageFeedsActivity, getString(R.string.deleting_feed, feed.name))
                    lifecycleScope.launch {
                        try {
                            API(this@ManageFeedsActivity).deleteFeed(feed)
                        } catch(e: Exception) {
                            showErrorMessage(getString(R.string.delete_feed_failed), e)
                        } finally {
                            progressDialog.dismiss()
                        }
                    }
                }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
    }

    override fun changeFeed(feedId: Long, folderId: Long) {
        val progressDialog = showProgress(this, getString(R.string.moving_feed))
        lifecycleScope.launch {
            try {
                API(this@ManageFeedsActivity).moveFeed(feedId, folderId)
            } catch(e: Exception) {
                showErrorMessage(getString(R.string.feed_move_failed), e)
            } finally {
                progressDialog.dismiss()
            }
        }
    }

    override fun showFeedDialog(feed: Feed) {
        AddNewFeedDialogFragment.show(this, feed, false)
    }

    private fun showErrorMessage(title: String, e: Exception) {
        e.printStackTrace()
        Toast.makeText(this@ManageFeedsActivity, "$title\n${e.localizedMessage ?: ""}", Toast.LENGTH_LONG).show()
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
}