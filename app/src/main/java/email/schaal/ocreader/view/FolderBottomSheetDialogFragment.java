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
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProviders;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import email.schaal.ocreader.Preferences;
import email.schaal.ocreader.database.FeedViewModel;
import email.schaal.ocreader.databinding.DialogFoldersBinding;

public class FolderBottomSheetDialogFragment extends BottomSheetDialogFragment {
    private DialogFoldersBinding binding;
    private FeedViewModel viewModel;
    private FoldersAdapter foldersAdapter;
    private FoldersAdapter.TreeItemClickListener treeItemClickListener;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = ViewModelProviders.of(requireActivity(), new FeedViewModel.FeedViewModelFactory(requireContext())).get(FeedViewModel.class);
        // TODO: 12/24/19 Find out why RealmChangeListener for folders is not triggered when syncing
        viewModel.updateFolders(Preferences.SHOW_ONLY_UNREAD.getBoolean(PreferenceManager.getDefaultSharedPreferences(requireContext())));
        viewModel.getFolders().observe(this, folders -> {
            if(foldersAdapter != null)
                foldersAdapter.updateFolders(folders);
        });
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = DialogFoldersBinding.inflate(inflater, container, true);
        return binding.getRoot();
    }

    public void setTreeItemClickListener(FoldersAdapter.TreeItemClickListener treeItemClickListener) {
        this.treeItemClickListener = treeItem -> {
            treeItemClickListener.onTreeItemClick(treeItem);
            dismiss();
        };
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        foldersAdapter = new FoldersAdapter(requireContext(), viewModel.getFolders().getValue(), viewModel.getTopFolderList(), treeItemClickListener);
        binding.recyclerViewFolders.setAdapter(foldersAdapter);
        binding.recyclerViewFolders.setLayoutManager(new LinearLayoutManager(requireContext()));
    }
}
