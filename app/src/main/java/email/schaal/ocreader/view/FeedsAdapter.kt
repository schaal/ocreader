package email.schaal.ocreader.view

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import email.schaal.ocreader.R
import email.schaal.ocreader.database.model.Feed
import email.schaal.ocreader.databinding.ListFeedBinding
import email.schaal.ocreader.util.FaviconLoader
import email.schaal.ocreader.util.FaviconLoader.FeedColorsListener
import email.schaal.ocreader.util.FeedColors
import io.realm.Realm
import io.realm.RealmRecyclerViewAdapter

/**
 * RecyclerView Adapter for Feeds
 */
class FeedsAdapter(realm: Realm, private val listener: FeedManageListener) : RealmRecyclerViewAdapter<Feed?, RecyclerView.ViewHolder>(realm.where(Feed::class.java).sort(Feed.NAME).findAll(), true) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val binding = ListFeedBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return FeedViewHolder(binding, listener)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is FeedViewHolder) {
            val feed = getItem(position)
            holder.bindFeed(feed)
        }
    }

    override fun getItemId(index: Int): Long {
        val feed = getItem(index)
        return feed?.id ?: RecyclerView.NO_ID
    }

    override fun getItemViewType(position: Int): Int {
        return R.id.viewtype_item
    }

    /**
     * ViewHolder displaying a Feed
     */
    private class FeedViewHolder internal constructor(private val binding: ListFeedBinding, private val listener: FeedManageListener) : RecyclerView.ViewHolder(binding.root), FeedColorsListener {
        fun bindFeed(feed: Feed?) {
            binding.textViewTitle.text = feed!!.name
            binding.textViewFolder.text = feed.getFolderTitle(itemView.context)
            binding.deleteFeed.setOnClickListener { listener.deleteFeed(feed) }
            if (feed.isConsideredFailed) {
                binding.feedFailure.visibility = View.VISIBLE
                binding.feedFailure.text = feed.lastUpdateError
            } else {
                binding.feedFailure.visibility = View.GONE
            }
            itemView.setOnClickListener { listener.showFeedDialog(feed) }
            FaviconLoader.Builder(binding.imageviewFavicon).build().load(itemView.context, feed, this)
        }

        override fun onGenerated(feedColors: FeedColors) {}
        override fun onStart() {}

    }

    init {
        setHasStableIds(true)
    }
}