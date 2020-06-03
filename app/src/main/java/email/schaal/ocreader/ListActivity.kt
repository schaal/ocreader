/*
 * Copyright (C) 2015-2016 Daniel Schaal <daniel@schaal.email>
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
package email.schaal.ocreader

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.os.Bundle
import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar.OnMenuItemClickListener
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout.OnRefreshListener
import com.google.android.material.snackbar.Snackbar
import com.mikepenz.aboutlibraries.LibsBuilder
import email.schaal.ocreader.R.string
import email.schaal.ocreader.database.FeedViewModel
import email.schaal.ocreader.database.FeedViewModel.FeedViewModelFactory
import email.schaal.ocreader.database.Queries
import email.schaal.ocreader.database.model.Item
import email.schaal.ocreader.database.model.TemporaryFeed
import email.schaal.ocreader.database.model.TreeItem
import email.schaal.ocreader.databinding.ActivityListBinding
import email.schaal.ocreader.service.SyncResultReceiver
import email.schaal.ocreader.service.SyncType
import email.schaal.ocreader.view.*
import email.schaal.ocreader.view.TreeItemsAdapter.TreeItemClickListener

class ListActivity : AppCompatActivity(), ItemViewHolder.OnClickListener, OnRefreshListener, ActionMode.Callback, TreeItemClickListener {
    private lateinit var bottomMenuClickListener: OnMenuItemClickListener
    private var actionMode: ActionMode? = null
    private lateinit var binding: ActivityListBinding
    private lateinit var adapter: LiveItemsAdapter
    private lateinit var layoutManager: LinearLayoutManager
    private lateinit var syncResultReceiver: SyncResultReceiver
    private val feedViewModel: FeedViewModel by viewModels { FeedViewModelFactory(this) }
    private lateinit var preferenceChangeListener: OnSharedPreferenceChangeListener

    private fun updateSyncStatus(syncRunning: Boolean) {
        if(!syncRunning)
            feedViewModel.updateTemporaryFeed(PreferenceManager.getDefaultSharedPreferences(this), true)

        binding.swipeRefreshLayout.isRefreshing = syncRunning
        binding.bottomAppbar.menu.findItem(R.id.menu_sync).isEnabled = !syncRunning
    }

    override fun onStart() {
        super.onStart()
        if (!Preferences.hasCredentials(PreferenceManager.getDefaultSharedPreferences(this))) {
            getLoginResult.launch()
        }
    }

    private val getSettingsResult = registerForActivityResult(object : ActivityResultContract<Unit, Boolean>() {
        override fun createIntent(context: Context, input: Unit?): Intent {
            return Intent(this@ListActivity, SettingsActivity::class.java)
        }

        override fun parseResult(resultCode: Int, intent: Intent?): Boolean {
            return if (resultCode == Activity.RESULT_OK && intent != null)
                intent.getBooleanExtra(SettingsActivity.EXTRA_RECREATE_ACTIVITY, false)
            else false
        }

    }) { if (it) recreate() }

    private val getManageFeedsResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == Activity.RESULT_OK)
            reloadListFragment()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_list)
        syncResultReceiver = SyncResultReceiver(binding.listviewSwitcher)
        setSupportActionBar(binding.toolbarLayout.toolbar)
        val preferences = PreferenceManager.getDefaultSharedPreferences(this)
        val menuItemShowOnlyUnread = binding.bottomAppbar.menu.findItem(R.id.menu_show_only_unread)
        menuItemShowOnlyUnread.isChecked = Preferences.SHOW_ONLY_UNREAD.getBoolean(preferences)

        bottomMenuClickListener = OnMenuItemClickListener { item: MenuItem ->
            when (item.itemId) {
                R.id.menu_settings -> {
                    getSettingsResult.launch()
                    true
                }
                R.id.menu_show_only_unread -> {
                    val showOnlyUnread = !item.isChecked
                    preferences.edit().putBoolean(Preferences.SHOW_ONLY_UNREAD.key, showOnlyUnread).apply()
                    item.isChecked = showOnlyUnread
                    reloadListFragment()
                    true
                }
                R.id.menu_sync -> {
                    feedViewModel.sync(this, SyncType.FULL_SYNC, syncResultReceiver)
                    true
                }
                R.id.menu_about -> {
                    showAboutDialog()
                    true
                }
                R.id.menu_mark_all_as_read -> {
                    feedViewModel.markTemporaryFeedAsRead()
                    true
                }
                R.id.menu_manage_feeds -> {
                    getManageFeedsResult.launch(Intent(this, ManageFeedsActivity::class.java))
                    true
                } else -> false
            }
        }

        binding.bottomAppbar.apply {
            setOnMenuItemClickListener(bottomMenuClickListener)
            setNavigationIcon(R.drawable.ic_folder)
            setNavigationOnClickListener {
                FolderBottomSheetDialogFragment().apply {
                    setTreeItemClickListener(this@ListActivity)
                }.show(supportFragmentManager, null)
            }
        }

        preferenceChangeListener = OnSharedPreferenceChangeListener { _: SharedPreferences?, key: String ->
            when (key) {
                Preferences.SHOW_ONLY_UNREAD.key -> {
                    feedViewModel.updateFolders(Preferences.SHOW_ONLY_UNREAD.getBoolean(preferences))
                }
                Preferences.SYS_SYNC_RUNNING.key -> {
                    updateSyncStatus(Preferences.SYS_SYNC_RUNNING.getBoolean(preferences))
                }
            }
        }

        preferences.registerOnSharedPreferenceChangeListener(preferenceChangeListener)
        binding.swipeRefreshLayout.setColorSchemeResources(R.color.primary)
        binding.swipeRefreshLayout.setOnRefreshListener(this)
        layoutManager = LinearLayoutManager(this)
        adapter = LiveItemsAdapter(emptyList(), this)
        feedViewModel.items.observe(this, Observer { items: List<Item> ->
            adapter.updateItems(items)
            if (adapter.selectedItemsCount > 0 && actionMode == null) {
                actionMode = startActionMode(this)
            }
            binding.listviewSwitcher.displayedChild = if (items.isEmpty()) 0 else 1
        })
        feedViewModel.temporaryFeed.observe(this, Observer { temporaryFeed: TemporaryFeed -> supportActionBar?.title = temporaryFeed.name })

        feedViewModel.syncStatus.observe(this, Observer { updateSyncStatus(it) })

        binding.itemsRecyclerview.adapter = adapter
        binding.itemsRecyclerview.layoutManager = layoutManager
        binding.itemsRecyclerview.addItemDecoration(DividerItemDecoration(this, R.dimen.divider_inset))
        if (savedInstanceState != null) {
            layoutManager.onRestoreInstanceState(savedInstanceState.getParcelable(LAYOUT_MANAGER_STATE))
            adapter.onRestoreInstanceState(savedInstanceState)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable(LAYOUT_MANAGER_STATE, layoutManager.onSaveInstanceState())
        adapter.onSaveInstanceState(outState)
    }

    private fun reloadListFragment() {
        feedViewModel.updateTemporaryFeed(PreferenceManager.getDefaultSharedPreferences(this), true)
        binding.itemsRecyclerview.scrollToPosition(0)
    }

    val getLoginResult = registerForActivityResult(object: ActivityResultContract<Void?, String?>() {
        override fun createIntent(context: Context, input: Void?): Intent {
            return Intent(this@ListActivity, LoginFlowActivity::class.java).apply {
                Preferences.URL.getString(PreferenceManager.getDefaultSharedPreferences(this@ListActivity))?.let {
                    putExtra(LoginFlowActivity.EXTRA_URL, it)
                }
            }
        }

        override fun parseResult(resultCode: Int, intent: Intent?): String? {
            return when(resultCode) {
                Activity.RESULT_OK -> {
                    if(intent?.getBooleanExtra(LoginFlowActivity.EXTRA_IMPROPERLY_CONFIGURED_CRON, false) == true)
                        getString(string.updater_improperly_configured)
                    else
                        null
                }
                else -> {
                    intent?.getStringExtra(LoginFlowActivity.EXTRA_MESSAGE)
                }
            }
        }
    }) { message ->
        message?.let {
            Snackbar.make(binding.coordinatorLayout, it, Snackbar.LENGTH_LONG)
                    .show()
        }
        Queries.resetDatabase()
        feedViewModel.sync(this, SyncType.FULL_SYNC, syncResultReceiver)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_item_list_top, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_account -> {
                val bottomSheetDialogFragment = UserBottomSheetDialogFragment()
                bottomSheetDialogFragment.show(supportFragmentManager, null)

                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    fun onNavigationItemClick(item: MenuItem): Boolean {
        return bottomMenuClickListener.onMenuItemClick(item)
    }

    private fun showAboutDialog() {
        LibsBuilder()
                .withAboutIconShown(true)
                .withAboutVersionShown(true)
                .withAboutDescription(getString(string.about_app, getString(string.app_year_author), getString(string.app_url)))
                .withAboutAppName(getString(string.app_name))
                .withLicenseShown(true)
                .withActivityTitle(getString(string.about))
                .withFields(string::class.java.fields)
                .start(this)
    }

    private val getItemPagerResult = registerForActivityResult(object: ActivityResultContract<Int, Int>() {
        override fun createIntent(context: Context, position: Int): Intent {
            return Intent(this@ListActivity, ItemPagerActivity::class.java).apply {
                putExtra(ItemPagerActivity.EXTRA_CURRENT_POSITION, position)
            }
        }

        override fun parseResult(resultCode: Int, intent: Intent?): Int {
            return if(resultCode == Activity.RESULT_OK && intent != null)
                intent.getIntExtra(ItemPagerActivity.EXTRA_CURRENT_POSITION, -1) else -1
        }

    }) {
        if(it >= 0) binding.itemsRecyclerview.smoothScrollToPosition(it)
    }

    override fun onItemClick(item: Item, position: Int) {
        if (actionMode == null) {
            getItemPagerResult.launch(position)
        } else {
            adapter.toggleSelection(position)
            if (adapter.selectedItemsCount == 0) actionMode?.finish() else {
                actionMode?.title = adapter.selectedItemsCount.toString()
                actionMode?.invalidate()
            }
        }
    }

    override fun onItemLongClick(item: Item, position: Int) {
        if (actionMode != null || feedViewModel.syncStatus.value == true) return
        adapter.toggleSelection(position)
        actionMode = startActionMode(this)
    }

    override fun onRefresh() {
        feedViewModel.sync(this, SyncType.FULL_SYNC, syncResultReceiver)
    }

    override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
        mode.menuInflater.inflate(R.menu.menu_item_list_action, menu)
        mode.title = adapter.selectedItemsCount.toString()
        binding.swipeRefreshLayout.isEnabled = false
        return true
    }

    override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
        val selectedItemsCount = adapter.selectedItemsCount
        // the menu only changes on the first and second selection
        if (selectedItemsCount > 2) return false
        val firstSelectedItem = adapter.firstSelectedItem
        val firstSelectedUnread = firstSelectedItem != null && firstSelectedItem.unread
        menu.findItem(R.id.action_mark_read).isVisible = firstSelectedUnread
        menu.findItem(R.id.action_mark_unread).isVisible = !firstSelectedUnread
        val firstSelectedStarred = firstSelectedItem != null && firstSelectedItem.starred
        menu.findItem(R.id.action_mark_starred).isVisible = !firstSelectedStarred
        menu.findItem(R.id.action_mark_unstarred).isVisible = firstSelectedStarred
        menu.findItem(R.id.action_mark_above_read).isVisible = selectedItemsCount == 1
        return true
    }

    override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_mark_read -> {
                feedViewModel.setItemUnread(false, *adapter.selectedItems)
                mode.finish()
                return true
            }
            R.id.action_mark_unread -> {
                feedViewModel.setItemUnread(true, *adapter.selectedItems)
                mode.finish()
                return true
            }
            R.id.action_mark_starred -> {
                feedViewModel.setItemStarred(true, *adapter.selectedItems)
                mode.finish()
                return true
            }
            R.id.action_mark_unstarred -> {
                feedViewModel.setItemStarred(false, *adapter.selectedItems)
                mode.finish()
                return true
            }
            R.id.action_mark_above_read -> {
                val lastItemId = adapter.selectedItems[0]?.id
                if(lastItemId != null)
                    feedViewModel.markAboveAsRead(feedViewModel.items.value, lastItemId)
                mode.finish()
                return true
            }
        }
        return false
    }

    override fun onDestroyActionMode(mode: ActionMode) {
        actionMode = null
        binding.swipeRefreshLayout.isEnabled = true
        adapter.clearSelection()
    }

    override fun onTreeItemClick(treeItem: TreeItem) {
        feedViewModel.updateSelectedTreeItem(this, treeItem)
        reloadListFragment()
    }

    companion object {
        private val TAG = ListActivity::class.java.name
        const val LAYOUT_MANAGER_STATE = "LAYOUT_MANAGER_STATE"
    }
}