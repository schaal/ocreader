/*
 * Copyright (C) 2015 Daniel Schaal <daniel@schaal.email>
 *
 * This file is part of Cloudreader.
 *
 * Cloudreader is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Cloudreader is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Cloudreader.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package email.schaal.cloudreader.view;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ViewSwitcher;

import email.schaal.cloudreader.R;
import email.schaal.cloudreader.database.Queries;
import email.schaal.cloudreader.model.Feed;
import email.schaal.cloudreader.model.Folder;
import email.schaal.cloudreader.model.Item;
import email.schaal.cloudreader.model.TemporaryFeed;
import email.schaal.cloudreader.model.TreeItem;
import io.realm.Realm;
import io.realm.RealmList;
import io.realm.RealmResults;
import io.realm.Sort;

/**
 * Adapter for the RecyclerView to manage Items belonging to a certain TreeItem.
 */
public class ItemsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private RealmList<Item> items;
    @Nullable private TreeItem treeItem;
    private final ItemViewHolder.OnClickListener clickListener;
    private final OnLoadMoreListener loadMoreListener;

    private final static int VIEW_TYPE_ITEM = 0;
    private final static int VIEW_TYPE_EMPTY = 1;
    private final static int VIEW_TYPE_LOADMORE = 2;

    private TreeItem loadingMoreTreeItem;

    public ItemsAdapter(ItemViewHolder.OnClickListener clickListener, OnLoadMoreListener loadMoreListener) {
        this.clickListener = clickListener;
        this.loadMoreListener = loadMoreListener;

        init();
    }

    public ItemsAdapter(@Nullable TreeItem treeItem, ItemViewHolder.OnClickListener clickListener, OnLoadMoreListener loadMoreListener) {
        this.treeItem = treeItem;
        this.clickListener = clickListener;
        this.loadMoreListener = loadMoreListener;

        init();
    }

    private void init() {
        setHasStableIds(true);
        updateItems(false);
    }

    public void updateItems(boolean updateTemporaryFeed) {
        if(treeItem == null)
            return;

        Realm realm = null;
        try {
            realm = Realm.getDefaultInstance();

            final TemporaryFeed temporaryFeed = realm.where(TemporaryFeed.class).findFirst();

            if (updateTemporaryFeed || temporaryFeed.getId() != treeItem.getId()) {
                realm.executeTransaction(new Realm.Transaction() {
                    @Override
                    public void execute(Realm realm) {
                        RealmResults<Item> tempItems = Queries.getInstance().getItems(realm, treeItem, Item.PUB_DATE, Sort.DESCENDING);
                        temporaryFeed.setId(treeItem.getId());
                        temporaryFeed.setTitle(treeItem.getTitle());
                        temporaryFeed.getItems().clear();
                        if (tempItems != null) {
                            temporaryFeed.getItems().addAll(tempItems);
                        }
                    }
                });
            }

            if (temporaryFeed != null)
                items = temporaryFeed.getItems();

            notifyDataSetChanged();
        } finally {
            if (realm != null)
                realm.close();
        }
    }

    @Override
    public int getItemViewType(int position) {
        if(hasLoadMore() && position + 1 == getItemCount())
            return VIEW_TYPE_LOADMORE;

        if(hasItems())
            return VIEW_TYPE_ITEM;
        else
            return VIEW_TYPE_EMPTY;
    }

    private boolean hasItems() {
        return items != null && items.size() > 0;
    }

    private boolean hasLoadMore() {
        return treeItem instanceof Feed || treeItem instanceof Folder;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view;
        RecyclerView.ViewHolder holder = null;
        switch (viewType) {
            case VIEW_TYPE_ITEM:
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
        if(holder instanceof ItemViewHolder)
            ((ItemViewHolder) holder).bindItem(items.get(position), position);
        else if(holder instanceof LoadMoreViewHolder) {
            ((LoadMoreViewHolder) holder).showProgress(
                    loadingMoreTreeItem != null && loadingMoreTreeItem.equals(treeItem));
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

    public void setTreeItem(@NonNull TreeItem item) {
        treeItem = item;
        updateItems(true);
    }

    public void resetLoadMore() {
        loadingMoreTreeItem = null;
    }

    private class EmptyStateViewHolder extends RecyclerView.ViewHolder {
        public EmptyStateViewHolder(View itemView) {
            super(itemView);
        }
    }

    public class LoadMoreViewHolder extends RecyclerView.ViewHolder {
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
                    loadingMoreTreeItem = treeItem;
                    loadMoreListener.onLoadMore(treeItem);
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

    public interface OnLoadMoreListener {
        void onLoadMore(TreeItem treeItem);
    }
}
