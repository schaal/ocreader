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
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import email.schaal.ocreader.Preferences
import email.schaal.ocreader.database.FeedViewModel
import email.schaal.ocreader.database.FeedViewModel.FeedViewModelFactory
import email.schaal.ocreader.database.model.Folder
import email.schaal.ocreader.database.model.TreeItem
import email.schaal.ocreader.databinding.DialogFoldersBinding
import email.schaal.ocreader.view.FoldersAdapter.TreeItemClickListener

class FolderBottomSheetDialogFragment : BottomSheetDialogFragment() {
    private lateinit var binding: DialogFoldersBinding
    private val viewModel: FeedViewModel by activityViewModels { FeedViewModelFactory(requireContext()) }
    private var foldersAdapter: FoldersAdapter? = null
    private var treeItemClickListener: TreeItemClickListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // TODO: 12/24/19 Find out why RealmChangeListener for folders is not triggered when syncing
        viewModel.updateFolders(Preferences.SHOW_ONLY_UNREAD.getBoolean(PreferenceManager.getDefaultSharedPreferences(requireContext())))
        viewModel.folders.observe(this, Observer {
            folders: List<Folder>? -> foldersAdapter?.updateFolders(folders)
        })
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = DialogFoldersBinding.inflate(inflater, container, true)
        return binding.root
    }

    fun setTreeItemClickListener(treeItemClickListener: TreeItemClickListener) {
        this.treeItemClickListener = object : TreeItemClickListener {
            override fun onTreeItemClick(treeItem: TreeItem) {
                treeItemClickListener.onTreeItemClick(treeItem)
                dismiss()
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        foldersAdapter = FoldersAdapter(requireContext(), viewModel.folders.value, viewModel.topFolders, treeItemClickListener)
        binding.recyclerViewFolders.adapter = foldersAdapter
        binding.recyclerViewFolders.layoutManager = LinearLayoutManager(requireContext())
    }
}