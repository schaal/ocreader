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

import android.content.Context;
import android.content.res.TypedArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import email.schaal.ocreader.R;
import email.schaal.ocreader.database.model.AllUnreadFolder;
import email.schaal.ocreader.database.model.Feed;
import email.schaal.ocreader.database.model.Folder;
import email.schaal.ocreader.database.model.FreshFolder;
import email.schaal.ocreader.database.model.Item;
import email.schaal.ocreader.database.model.StarredFolder;
import email.schaal.ocreader.database.model.TreeIconable;
import email.schaal.ocreader.database.model.TreeItem;
import email.schaal.ocreader.databinding.ListDividerBinding;
import email.schaal.ocreader.databinding.ListFolderBinding;
import io.realm.Realm;

public class FoldersAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private final List<TreeItem> topFolders;
    @Nullable private List<? extends TreeItem> folders;
    private final TreeItemClickListener clickListener;

    private long selectedTreeItemId = AllUnreadFolder.ID;

    public void setSelectedTreeItemId(long id) {
        this.selectedTreeItemId = id;
        notifyDataSetChanged();
    }

    private static class DividerTreeItem implements TreeItem {
        private final String name;

        private DividerTreeItem(final String name) {
            this.name = name;
        }

        @Override
        public long getId() {
            return 0;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public int getCount(Realm realm) {
            return 0;
        }

        @Override
        public boolean canLoadMore() {
            return false;
        }

        @Override
        public List<Feed> getFeeds(Realm realm, boolean onlyUnread) {
            return null;
        }

        @Override
        public List<Item> getItems(Realm realm, boolean onlyUnread) {
            return null;
        }
    }

    public FoldersAdapter(final Context context, @Nullable final List<? extends TreeItem> folders, final Collection<TreeItem> defaultTopFolders, TreeItemClickListener clickListener) {
        this.folders = folders;
        this.clickListener = clickListener;

        topFolders = new ArrayList<>(defaultTopFolders.size() + 1);
        topFolders.addAll(defaultTopFolders);
        topFolders.add(new DividerTreeItem(context.getString(R.string.folder)));

        setHasStableIds(true);
    }

    public void updateFolders(final List<? extends TreeItem> folders) {
        this.folders = folders;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == R.id.viewtype_item) {
            return new FolderViewHolder(ListFolderBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false), clickListener);
        }
        return new DividerViewHolder(ListDividerBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public int getItemViewType(int position) {
        final TreeItem treeItem = getTreeItem(position);
        if(treeItem instanceof DividerTreeItem)
            return R.id.viewtype_divider;
        else
            return R.id.viewtype_item;
    }

    @Nullable
    private TreeItem getTreeItem(int position) {
        if(position >= topFolders.size())
            return folders != null ? folders.get(position - topFolders.size()) : null;
        else
            return topFolders.get(position);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if(holder instanceof FolderViewHolder)
            ((FolderViewHolder)holder).bind(getTreeItem(position), selectedTreeItemId);
        else if(holder instanceof DividerViewHolder)
            ((DividerViewHolder)holder).bind(getTreeItem(position));
    }

    @Override
    public int getItemCount() {
        return topFolders.size() + (folders != null ? folders.size() : 0);
    }

    public interface TreeItemClickListener {
        void onTreeItemClick(TreeItem treeItem);
    }

    private static class DividerViewHolder extends RecyclerView.ViewHolder {
        private final ListDividerBinding binding;
        DividerViewHolder(@NonNull ListDividerBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(@NonNull TreeItem treeItem) {
            binding.textViewDivider.setText(treeItem.getName());
        }
    }
    public static class FolderViewHolder extends RecyclerView.ViewHolder {
        private final ListFolderBinding binding;
        private final TreeItemClickListener clickListener;

        FolderViewHolder(@NonNull ListFolderBinding binding, TreeItemClickListener clickListener) {
            super(binding.getRoot());
            this.binding = binding;
            this.clickListener = clickListener;
        }

        void bind(@Nullable final TreeItem folder, long selectedTreeItemId) {
            if(folder != null) {
                itemView.setSelected(folder.getId() == selectedTreeItemId);
                itemView.setOnClickListener(v -> clickListener.onTreeItemClick(folder));
                if (folder instanceof TreeIconable)
                    binding.imageviewFavicon.setImageResource(((TreeIconable) folder).getIcon());
                else
                    binding.imageviewFavicon.setImageResource(R.drawable.ic_feed_icon);
                binding.textViewTitle.setText(folder.getName());
            }
        }

        private void setSelected(boolean selected) {
            int backgroundResource = R.drawable.item_background;
            if (!selected) {
                int[] attrs = new int[]{R.attr.selectableItemBackground};
                TypedArray typedArray = itemView.getContext().obtainStyledAttributes(attrs);
                backgroundResource = typedArray.getResourceId(0, 0);
                typedArray.recycle();
            }

            itemView.setBackgroundResource(backgroundResource);
        }
    }
}
