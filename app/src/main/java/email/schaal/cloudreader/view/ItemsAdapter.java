/*
 * Copyright (C) 2015 Daniel Schaal <daniel@schaal.email>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package email.schaal.cloudreader.view;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import email.schaal.cloudreader.R;
import email.schaal.cloudreader.database.Queries;
import email.schaal.cloudreader.model.Item;
import email.schaal.cloudreader.model.TemporaryFeed;
import email.schaal.cloudreader.model.TreeItem;
import io.realm.Realm;
import io.realm.RealmList;
import io.realm.RealmResults;

/**
 * Created by daniel on 08.11.15.
 */
public class ItemsAdapter extends RecyclerView.Adapter<ItemViewHolder> {
    private RealmList<Item> items;
    private TreeItem treeItem;
    private final ItemViewHolder.OnClickListener clickListener;

    public ItemsAdapter(TreeItem treeItem, ItemViewHolder.OnClickListener clickListener) {
        this.treeItem = treeItem;
        this.clickListener = clickListener;
        setHasStableIds(true);
        updateItems(false);
    }

    public void updateItems(boolean updateTemporaryFeed) {
        Realm realm = null;
        try {
            realm = Realm.getDefaultInstance();

            final TemporaryFeed temporaryFeed = realm.where(TemporaryFeed.class).findFirst();

            if (updateTemporaryFeed) {
                realm.executeTransaction(new Realm.Transaction() {
                    @Override
                    public void execute(Realm realm) {
                        RealmResults<Item> tempItems = Queries.getInstance().getItems(realm, treeItem, Item.PUB_DATE, false);
                        temporaryFeed.setTitle(treeItem.getTitle());
                        temporaryFeed.getItems().clear();
                        temporaryFeed.getItems().addAll(tempItems);
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
    public ItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item, parent, false);
        return new ItemViewHolder(view, clickListener);
    }

    @Override
    public void onBindViewHolder(ItemViewHolder holder, int position) {
        holder.bindItem(items.get(position), position);
    }

    @Override
    public int getItemCount() {
        return items != null ? items.size() : 0;
    }

    @Override
    public long getItemId(int position) {
        return items.get(position).getId();
    }

    public void setTreeItem(TreeItem item) {
        treeItem = item;
        updateItems(true);
    }
}
