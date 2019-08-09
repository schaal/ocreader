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

package email.schaal.ocreader.view;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import email.schaal.ocreader.R;
import email.schaal.ocreader.database.model.Item;
import email.schaal.ocreader.databinding.ListItemBinding;

public class LiveItemsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private List<Item> items;
    private final ItemViewHolder.OnClickListener clickListener;

    /**
     * Selected item ids, LinkedHashSet to preserve insertion order for getFirstSelectedItem()
     */
    private final Set<Integer> selections = new LinkedHashSet<>();

    public LiveItemsAdapter(List<Item> items, ItemViewHolder.OnClickListener clickListener) {
        this.items = items;
        this.clickListener = clickListener;
        setHasStableIds(true);
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        RecyclerView.ViewHolder holder = null;
        switch (viewType) {
            case R.id.viewtype_item: {
                ListItemBinding binding = ListItemBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
                holder = new ItemViewHolder(binding, clickListener);
            }
            break;
        }
        return holder;
    }

    @Override
    public long getItemId(int position) {
        return items.get(position).getId();
    }

    public void toggleSelection(int position) {
        if(selections.contains(position))
            selections.remove(position);
        else
            selections.add(position);

        notifyItemChanged(position);
    }

    public void clearSelection() {
        selections.clear();
        notifyDataSetChanged();
    }

    public int getSelectedItemsCount() {
        return selections.size();
    }

    public Item[] getSelectedItems() {
        Item[] itemsArray = new Item[selections.size()];
        int i = 0;
        for(Integer position: selections) {
            itemsArray[i++] = items.get(position);
        }
        return itemsArray;
    }

    @Nullable
    public Item getFirstSelectedItem() {
        if(selections.isEmpty())
            return null;

        return items.get(selections.iterator().next());
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if(holder instanceof ItemViewHolder) {
            ((ItemViewHolder) holder).bindItem(items.get(position), position, selections.contains(position));
        }
    }

    @Override
    public int getItemViewType(int position) {
        return R.id.viewtype_item;
    }

    @Override
    public int getItemCount() {
        return items != null ? items.size() : 0;
    }

    public void updateItems(List<Item> items) {
        this.items = items;
        notifyDataSetChanged();
    }

    public void onSaveInstanceState(Bundle bundle) {
        bundle.putIntegerArrayList("adapter_selections", new ArrayList<>(selections));
    }

    public void onRestoreInstanceState(Bundle bundle) {
        final ArrayList<Integer> savedSelections = bundle.getIntegerArrayList("adapter_selections");
        if(savedSelections != null) {
            selections.addAll(savedSelections);
        }
    }
}
