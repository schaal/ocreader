/*
 * Copyright (C) 2015-2016 Daniel Schaal <daniel@schaal.email>
 *
 * This file is part of OCReader.
 *
 * OCReader is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * OCReader is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with OCReader.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package email.schaal.ocreader.view

import androidx.preference.PreferenceManager
import android.util.Log
import android.view.View
import androidx.annotation.ColorInt
import androidx.recyclerview.widget.RecyclerView
import email.schaal.ocreader.Preferences
import email.schaal.ocreader.R
import email.schaal.ocreader.database.model.Item
import email.schaal.ocreader.databinding.ListItemBinding
import email.schaal.ocreader.util.FaviconLoader
import email.schaal.ocreader.util.FaviconLoader.FeedColorsListener
import email.schaal.ocreader.util.FeedColors
import email.schaal.ocreader.util.getTimeSpanString
import java.util.*

/**
 * RecyclerView.ViewHolder to display a feed Item.
 */
class ItemViewHolder(private val binding: ListItemBinding, private val clickListener: OnClickListener) : RecyclerView.ViewHolder(binding.root), FeedColorsListener {
    @ColorInt
    private var defaultFeedTextColor = 0
    private val alphaViews: Array<View>
    fun bindItem(item: Item, position: Int, selected: Boolean) {
        binding.textViewTitle.text = item.title
        val feed = item.feed
        if (feed != null) {
            binding.textViewFeedTitle.text = feed.name
        } else {
            Log.w(TAG, "Feed == null")
            binding.textViewFeedTitle.text = ""
        }
        val preferences = PreferenceManager.getDefaultSharedPreferences(binding.root.context)
        val date: Date
        date = if (Preferences.SORT_FIELD.getString(preferences) == Item::updatedAt.name) item.updatedAt!! else item.pubDate!!
        binding.textViewTime.text = getTimeSpanString(itemView.context, date)
        binding.textViewFeedTitle.setTextColor(defaultFeedTextColor)
        FaviconLoader.Builder(binding.imageviewFavicon).build().load(binding.imageviewFavicon.context, feed, this)
        itemView.setOnClickListener { clickListener.onItemClick(item, position) }
        itemView.setOnLongClickListener {
            clickListener.onItemLongClick(item, position)
            true
        }
        if (item.enclosureLink != null) {
            binding.play.visibility = View.VISIBLE
            binding.play.setOnClickListener { item.play(itemView.context) }
        } else {
            binding.play.visibility = View.GONE
            binding.play.setOnClickListener(null)
        }
        setUnreadState(item.unread)
        setStarredState(item.starred)
        itemView.isSelected = selected
    }

    private fun setUnreadState(unread: Boolean) {
        val alpha = if (unread) 1.0f else 0.5f
        for (view in alphaViews) {
            view.alpha = alpha
        }
    }

    private fun setStarredState(starred: Boolean) {
        binding.imageviewStar.visibility = if (starred) View.VISIBLE else View.GONE
    }

    override fun onGenerated(feedColors: FeedColors) {
        binding.textViewFeedTitle.setTextColor(feedColors.getColor(FeedColors.Type.TEXT, defaultFeedTextColor))
    }

    override fun onStart() {}

    interface OnClickListener {
        fun onItemClick(item: Item, position: Int)
        fun onItemLongClick(item: Item, position: Int)
    }

    companion object {
        private val TAG = ItemViewHolder::class.java.name
    }

    init {
        val typedArray = itemView.context.obtainStyledAttributes(intArrayOf(android.R.attr.textColorSecondary))
        defaultFeedTextColor = try {
            typedArray.getColor(0, 0)
        } finally {
            typedArray.recycle()
        }
        alphaViews = arrayOf(
                binding.textViewTitle,
                binding.textViewFeedTitle,
                binding.textViewTime,
                binding.imageviewFavicon,
                binding.imageviewStar,
                binding.play
        )
    }
}