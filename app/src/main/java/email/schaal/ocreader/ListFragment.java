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

package email.schaal.ocreader;

import android.app.Fragment;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import email.schaal.ocreader.view.DividerItemDecoration;
import email.schaal.ocreader.view.ItemViewHolder;
import email.schaal.ocreader.view.ItemsAdapter;

public class ListFragment extends Fragment {
    private final static String TAG = ListFragment.class.getSimpleName();

    public static final String LAYOUT_MANAGER_STATE = "LAYOUT_MANAGER_STATE";

    private RecyclerView itemsRecyclerView;

    private ItemsAdapter adapter;

    public ListFragment() {
    }

    @Override
    public void onResume() {
        super.onResume();

        adapter.updateItems(false);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_itemlist, container, false);

        itemsRecyclerView = (RecyclerView) rootView.findViewById(R.id.items_recyclerview);

        Drawable defaultFeedDrawable = ContextCompat.getDrawable(rootView.getContext(), R.drawable.ic_feed_icon);
        adapter = new ItemsAdapter((ItemViewHolder.OnClickListener)getActivity(), (ItemsAdapter.OnLoadMoreListener)getActivity(), defaultFeedDrawable);

        itemsRecyclerView.setAdapter(adapter);

        itemsRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        if(savedInstanceState != null)
            itemsRecyclerView.getLayoutManager().onRestoreInstanceState(savedInstanceState.getParcelable(LAYOUT_MANAGER_STATE));

        itemsRecyclerView.addItemDecoration(new DividerItemDecoration(getActivity()));
        itemsRecyclerView.setItemAnimator(new DefaultItemAnimator());

        return rootView;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(LAYOUT_MANAGER_STATE, itemsRecyclerView.getLayoutManager().onSaveInstanceState());
    }

    public ItemsAdapter getAdapter() {
        return adapter;
    }

    public int getCount() {
        return adapter.getItemCount();
    }
}
