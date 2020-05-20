package email.schaal.ocreader.view

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import email.schaal.ocreader.ManageFeedsActivity
import email.schaal.ocreader.R
import email.schaal.ocreader.database.model.Feed
import email.schaal.ocreader.databinding.FragmentAddNewFeedBinding

/**
 * Display form to add new Feed
 */
class AddNewFeedDialogFragment : BottomSheetDialogFragment() {
    private var listener: FeedManageListener? = null
    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = try {
            context as FeedManageListener
        } catch (e: ClassCastException) {
            throw ClassCastException("$context must implement FeedManageListener")
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val activity = requireActivity() as ManageFeedsActivity

        val arguments = requireArguments()
        val feedId = arguments.getLong(ARG_FEED_ID, -1)
        val newFeed = feedId < 0

        val binding = FragmentAddNewFeedBinding.inflate(activity.layoutInflater).apply {
            feedUrl.isEnabled = newFeed
            folder.adapter = activity.folderSpinnerAdapter
            feedUrl.setText(arguments.getString(ARG_URL))
            folder.setSelection(activity.folderSpinnerAdapter.getPosition(arguments.getLong(ARG_FOLDER_ID, 0)))
        }

        return AlertDialog.Builder(activity).setTitle(if (newFeed) R.string.add_new_feed else R.string.edit_feed)
                .setPositiveButton(if (newFeed) R.string.add else R.string.save) { _, _ ->
                    if (newFeed)
                        listener?.addNewFeed(binding.feedUrl.text.toString(), binding.folder.selectedItemId, arguments.getBoolean(ARG_FINISH_AFTER_CLOSE, false))
                    else
                        listener?.changeFeed(feedId, binding.folder.selectedItemId)
                }
                .setView(binding.root)
                .create()
    }

    companion object {
        const val ARG_URL = "url"
        const val ARG_FOLDER_ID = "folder_id"
        const val ARG_FINISH_AFTER_CLOSE = "finish_after_close"
        private const val ARG_FEED_ID = "feed_id"
        /**
         * Show feed edit dialog
         * @param activity Activity to get the FragmentManager
         * @param feed feed to edit or add (id < 0 means add new feed, id >=0 means edit existing feed)
         * @param finishAfterClose should the activity be finished after operation was successful
         */
        fun show(activity: FragmentActivity, feed: Feed?, finishAfterClose: Boolean) {
            val dialogFragment = AddNewFeedDialogFragment()

            dialogFragment.arguments = Bundle().apply {
                putBoolean(ARG_FINISH_AFTER_CLOSE, finishAfterClose)
                feed?.let { feed ->
                    putString(ARG_URL, feed.url)
                    if (feed.id >= 0) {
                        putLong(ARG_FEED_ID, feed.id)
                        putLong(ARG_FOLDER_ID, feed.folderId)
                    }
                }
            }
            dialogFragment.show(activity.supportFragmentManager, "newfeed")
        }
    }
}