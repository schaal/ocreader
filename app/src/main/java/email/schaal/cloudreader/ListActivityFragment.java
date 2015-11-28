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

package email.schaal.cloudreader;

import android.app.Fragment;
import android.os.Bundle;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import email.schaal.cloudreader.model.AllUnreadFolder;
import email.schaal.cloudreader.model.TreeItem;
import email.schaal.cloudreader.view.DividerItemDecoration;
import email.schaal.cloudreader.view.ItemViewHolder;
import email.schaal.cloudreader.view.ItemsAdapter;

public class ListActivityFragment extends Fragment {
    private final static String TAG = ListActivityFragment.class.getSimpleName();

    private RecyclerView itemsRecyclerView;

    private ItemsAdapter adapter;

    public ListActivityFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_itemlist, container, false);

        itemsRecyclerView = (RecyclerView) rootView.findViewById(R.id.items_recyclerview);

        adapter = new ItemsAdapter(new AllUnreadFolder(getActivity()), (ItemViewHolder.OnClickListener)getActivity());

        itemsRecyclerView.setAdapter(adapter);
        itemsRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        itemsRecyclerView.addItemDecoration(new DividerItemDecoration(getActivity()));
        itemsRecyclerView.setItemAnimator(new DefaultItemAnimator());

        return rootView;
    }

    public void setItem(TreeItem item) {
        adapter.setTreeItem(item);
    }

    public int getCount() {
        return adapter.getItemCount();
    }
}
