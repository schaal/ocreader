/*
 * Copyright © 2019. Daniel Schaal <daniel@schaal.email>
 *
 * This file is part of ocreader.
 *
 * ocreader is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ocreader is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Foobar.  If not, see <http://www.gnu.org/licenses/>.
 */
package email.schaal.ocreader.view

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import email.schaal.ocreader.R
import email.schaal.ocreader.database.model.*
import email.schaal.ocreader.databinding.ListDividerBinding
import email.schaal.ocreader.databinding.ListFolderBinding
import io.realm.Realm

class TreeItemsAdapter(context: Context, private var treeItems: List<TreeItem>?, private val clickListener: TreeItemClickListener?, defaultTopFolders: Array<TreeItem>? = null) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private val topFolders: List<TreeItem>
    private var selectedTreeItemId = AllUnreadFolder.ID

    fun setSelectedTreeItemId(id: Long) {
        selectedTreeItemId = id
        notifyDataSetChanged()
    }

    private class DividerTreeItem(private val name: String) : TreeItem {
        override fun treeItemId(): Long {
            return 0
        }

        override fun treeItemName(): String {
            return name
        }

        override fun getIcon(): Int {
            return 0
        }

        override fun getCount(realm: Realm): Int {
            return 0
        }

        override fun getFeeds(realm: Realm, onlyUnread: Boolean): List<Feed> {
            return emptyList()
        }

        override fun getItems(realm: Realm, onlyUnread: Boolean): List<Item> {
            return emptyList()
        }

    }

    fun updateTreeItems(treeItems: List<TreeItem>?) {
        this.treeItems = treeItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == R.id.viewtype_item) {
            TreeItemViewHolder(ListFolderBinding.inflate(LayoutInflater.from(parent.context), parent, false), clickListener)
        } else DividerViewHolder(ListDividerBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun getItemViewType(position: Int): Int {
        val treeItem = getTreeItem(position)
        return if (treeItem is DividerTreeItem) R.id.viewtype_divider else R.id.viewtype_item
    }

    private fun getTreeItem(position: Int): TreeItem? {
        return if(position >= topFolders.size) {
            treeItems?.get(position - topFolders.size)
        } else {
            topFolders[position]
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val treeItem = getTreeItem(position) ?: return

        when (holder) {
            is TreeItemViewHolder -> holder.bind(treeItem, selectedTreeItemId)
            is DividerViewHolder -> holder.bind(treeItem)
        }
    }

    override fun getItemCount(): Int {
        return topFolders.size + (treeItems?.size ?: 0)
    }

    interface TreeItemClickListener {
        fun onTreeItemClick(treeItem: TreeItem)
    }

    private class DividerViewHolder internal constructor(private val binding: ListDividerBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(treeItem: TreeItem) {
            binding.textViewDivider.text = treeItem.treeItemName()
        }
    }

    class TreeItemViewHolder internal constructor(private val binding: ListFolderBinding, private val clickListener: TreeItemClickListener?) : RecyclerView.ViewHolder(binding.root) {
        fun bind(treeItem: TreeItem?, selectedTreeItemId: Long) {
            if (treeItem != null) {
                itemView.isSelected = treeItem.treeItemId() == selectedTreeItemId
                itemView.setOnClickListener { clickListener?.onTreeItemClick(treeItem) }
                binding.imageviewFavicon.setImageResource(treeItem.getIcon())
                binding.textViewTitle.text = treeItem.treeItemName()
            }
        }
    }

    init {
        topFolders = if(defaultTopFolders != null) listOf(*defaultTopFolders, DividerTreeItem(context.getString(R.string.folder))) else emptyList()
        setHasStableIds(true)
    }
}