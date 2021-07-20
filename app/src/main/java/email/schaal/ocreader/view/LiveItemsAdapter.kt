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

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.NO_ID
import email.schaal.ocreader.R
import email.schaal.ocreader.database.model.Item
import email.schaal.ocreader.databinding.ListItemBinding
import java.util.*

class LiveItemsAdapter(private var items: List<Item>?, private val clickListener: ItemViewHolder.OnClickListener) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    /**
     * Selected item ids, LinkedHashSet to preserve insertion order for getFirstSelectedItem()
     */
    private val selections: MutableSet<Int> = LinkedHashSet()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            R.id.viewtype_item -> {
                ItemViewHolder(
                    ListItemBinding.inflate(
                        LayoutInflater.from(parent.context),
                        parent,
                        false
                    ), clickListener
                )
            }
            else -> throw IllegalArgumentException("Unknown view type")
        }
    }

    override fun getItemId(position: Int): Long {
        return items?.get(position)?.id ?: NO_ID
    }

    fun toggleSelection(position: Int) {
        if (selections.contains(position)) selections.remove(position) else selections.add(position)
        notifyItemChanged(position)
    }

    fun clearSelection() {
        selections.clear()
        notifyDataSetChanged()
    }

    val selectedItemsCount: Int
        get() = selections.size

    val selectedItems: Array<Item?>
        get() {
            val itemsArray = arrayOfNulls<Item>(selections.size)
            var i = 0
            for (position in selections) {
                itemsArray[i++] = items?.get(position)
            }
            return itemsArray
        }

    val firstSelectedItem: Item?
        get() = if (selections.isEmpty())
            null
        else
            items?.get(selections.iterator().next())

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is ItemViewHolder) {
            items?.get(position)?.let {
                holder.bindItem(it, position, selections.contains(position))
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return R.id.viewtype_item
    }

    override fun getItemCount(): Int {
        return items?.size ?: 0
    }

    fun updateItems(items: List<Item>?) {
        this.items = items
        notifyDataSetChanged()
    }

    fun onSaveInstanceState(bundle: Bundle) {
        bundle.putIntegerArrayList("adapter_selections", ArrayList(selections))
    }

    fun onRestoreInstanceState(bundle: Bundle) {
        bundle.getIntegerArrayList("adapter_selections")?.let {
            selections.addAll(it)
        }
    }

    init {
        setHasStableIds(true)
    }
}