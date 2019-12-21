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
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.List;

import email.schaal.ocreader.R;
import email.schaal.ocreader.database.FeedViewModel;
import email.schaal.ocreader.database.model.Folder;
import email.schaal.ocreader.database.model.TreeItem;
import email.schaal.ocreader.databinding.DialogFoldersBinding;
import email.schaal.ocreader.databinding.ListFolderBinding;

public class FolderBottomSheetDialogFragment extends BottomSheetDialogFragment {
    private DialogFoldersBinding binding;
    private FeedViewModel viewModel;
    private FoldersAdapter foldersAdapter;
    private FoldersAdapter.TreeItemClickListener treeItemClickListener;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = ViewModelProviders.of(requireActivity()).get(FeedViewModel.class);
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

        foldersAdapter = new FoldersAdapter(requireContext(), viewModel.getFolders().getValue(), treeItemClickListener);
        binding.recyclerViewFolders.setAdapter(foldersAdapter);
        binding.recyclerViewFolders.setLayoutManager(new LinearLayoutManager(requireContext()));
        //binding.recyclerViewFolders.addItemDecoration(new DividerItemDecoration(requireContext(), R.dimen.divider_inset));

        viewModel.getFolders().observe(this, folders -> {
            foldersAdapter.updateFolders(folders);
        });
    }
}
