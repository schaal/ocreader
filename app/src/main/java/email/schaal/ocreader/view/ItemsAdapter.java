/*
 * Copyright (C) 2015 Daniel Schaal <daniel@schaal.email>
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

package email.schaal.ocreader.view;

import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ViewSwitcher;

import email.schaal.ocreader.R;
import email.schaal.ocreader.database.Queries;
import email.schaal.ocreader.model.AllUnreadFolder;
import email.schaal.ocreader.model.Feed;
import email.schaal.ocreader.model.Folder;
import email.schaal.ocreader.model.Item;
import email.schaal.ocreader.model.TemporaryFeed;
import email.schaal.ocreader.model.TreeItem;
import email.schaal.ocreader.view.drawer.DrawerManager;
import io.realm.OrderedRealmCollection;
import io.realm.Realm;
import io.realm.RealmResults;
import io.realm.Sort;

/**
 * Adapter for the RecyclerView to manage Items belonging to a certain TreeItem.
 */
public class ItemsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private OrderedRealmCollection<Item> items;
    private final SparseArray<Item> selectedItems = new SparseArray<>();
    private final DrawerManager.State state;
    private final Realm realm;
    private final ItemViewHolder.OnClickListener clickListener;
    private final OnLoadMoreListener loadMoreListener;

    public final static int VIEW_TYPE_ITEM = 0;
    public final static int VIEW_TYPE_LAST_ITEM = 1;
    public final static int VIEW_TYPE_EMPTY = 2;
    public final static int VIEW_TYPE_LOADMORE = 3;

    private TreeItem loadingMoreTreeItem;

    private Sort order;

    public ItemsAdapter(Realm realm, DrawerManager.State state, ItemViewHolder.OnClickListener clickListener, OnLoadMoreListener loadMoreListener, Sort order) {
        this.realm = realm;
        this.state = state;
        this.clickListener = clickListener;
        this.loadMoreListener = loadMoreListener;

        this.order = order;

        setHasStableIds(true);
    }

    public void setOrder(Sort order) {
        this.order = order;

        updateItems(false);
    }

    public void updateItems(boolean updateTemporaryFeed) {
        if(state.getTreeItem() == null)
            return;

        final TemporaryFeed temporaryFeed = realm.where(TemporaryFeed.class).findFirst();

        if (updateTemporaryFeed || temporaryFeed.getId() != state.getTreeItem().getId()) {
            realm.executeTransaction(new Realm.Transaction() {
                @Override
                public void execute(Realm realm) {
                    RealmResults<Item> tempItems = Queries.getItems(realm, state.getTreeItem(), isOnlyUnread());
                    temporaryFeed.setId(state.getTreeItem().getId());
                    temporaryFeed.setTitle(state.getTreeItem().getTitle());
                    temporaryFeed.getItems().clear();
                    if (tempItems != null) {
                        temporaryFeed.getItems().addAll(tempItems);
                    }
                }
            });
        }

        items = temporaryFeed.getItems().sort(Item.PUB_DATE, order);

        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        if(hasLoadMore() && position + 1 == getItemCount())
            return VIEW_TYPE_LOADMORE;

        if(hasItems()) {
            if(position + 1 == getActualItemCount())
                return VIEW_TYPE_LAST_ITEM;
            else
                return VIEW_TYPE_ITEM;
        } else
            return VIEW_TYPE_EMPTY;
    }

    private boolean hasItems() {
        return items != null && items.size() > 0;
    }

    private boolean hasLoadMore() {
        return state.getTreeItem() instanceof Feed || state.getTreeItem() instanceof Folder;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view;
        RecyclerView.ViewHolder holder = null;
        switch (viewType) {
            case VIEW_TYPE_ITEM:
            case VIEW_TYPE_LAST_ITEM:
                view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item, parent, false);
                holder = new ItemViewHolder(view, clickListener);
                break;
            case VIEW_TYPE_EMPTY:
                view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_empty, parent, false);
                holder = new EmptyStateViewHolder(view);
                break;
            case VIEW_TYPE_LOADMORE:
                view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_loadmore, parent, false);
                holder = new LoadMoreViewHolder(view);
                break;
        }
        return holder;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if(holder instanceof ItemViewHolder) {
            Item item = items.get(position);
            ((ItemViewHolder) holder).bindItem(item, position, selectedItems.get(position, null) != null);
        }
        else if(holder instanceof LoadMoreViewHolder) {
            ((LoadMoreViewHolder) holder).showProgress(
                    loadingMoreTreeItem != null && loadingMoreTreeItem.equals(state.getTreeItem()));
        }
    }

    @Override
    public int getItemCount() {
        int itemCount = getActualItemCount();
        if(itemCount == 0)
            itemCount = 1;
        if(hasLoadMore())
            itemCount++;
        return itemCount;
    }

    private int getActualItemCount() {
        return items != null ? items.size() : 0;
    }

    @Override
    public long getItemId(int position) {
        if(hasItems() && position < getActualItemCount())
            return items.get(position).getId();
        else
            return -1;
    }

    private boolean isOnlyUnread() {
        return state.getStartDrawerItem() instanceof AllUnreadFolder;
    }

    public void resetLoadMore() {
        loadingMoreTreeItem = null;
    }

    public void clearSelection() {
        selectedItems.clear();
        notifyDataSetChanged();
    }

    @Nullable
    public Boolean firstSelectedUnread() {
        if(selectedItems.size() > 0) {
            return selectedItems.valueAt(0).isUnread();
        } else {
            return null;
        }
    }

    @Nullable
    public Boolean firstSelectedStarred() {
        if(selectedItems.size() > 0) {
            return selectedItems.valueAt(0).isStarred();
        } else {
            return null;
        }
    }

    public int getSelectedItemsCount() {
        return selectedItems.size();
    }

    public Item[] getSelectedItems() {
        Item[] itemsIterable = new Item[selectedItems.size()];
        for (int index = 0; index < selectedItems.size(); index++) {
            itemsIterable[index] = selectedItems.valueAt(index);
        }
        return itemsIterable;
    }

    private class EmptyStateViewHolder extends RecyclerView.ViewHolder {
        public EmptyStateViewHolder(View itemView) {
            super(itemView);
        }
    }

    private class LoadMoreViewHolder extends RecyclerView.ViewHolder {
        private final static int CHILD_BUTTON_INDEX = 0;
        private final static int CHILD_PROGRESS_INDEX = 1;

        private final Button loadMoreButton;
        private final ViewSwitcher loadMoreViewSwitcher;

        public LoadMoreViewHolder(View itemView) {
            super(itemView);
            loadMoreButton = (Button) itemView.findViewById(R.id.buttonLoadMore);
            loadMoreViewSwitcher = (ViewSwitcher) itemView.findViewById(R.id.loadMoreViewSwitcher);

            loadMoreButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showProgress(true);
                    loadingMoreTreeItem = state.getTreeItem();
                    loadMoreListener.onLoadMore(state.getTreeItem());
                }
            });
        }

        public void showProgress(boolean loading) {
            if(loading)
                loadMoreViewSwitcher.setDisplayedChild(CHILD_PROGRESS_INDEX);
            else
                loadMoreViewSwitcher.setDisplayedChild(CHILD_BUTTON_INDEX);
        }
    }

    public void toggleSelection(int position) {
        if(selectedItems.get(position,null) != null) {
            selectedItems.remove(position);
        } else {
            selectedItems.put(position, items.get(position));
        }
        notifyItemChanged(position);
    }

    public interface OnLoadMoreListener {
        void onLoadMore(TreeItem treeItem);
    }
}
