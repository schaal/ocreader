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

package email.schaal.ocreader;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import androidx.databinding.DataBindingUtil;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.preference.PreferenceManager;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.snackbar.Snackbar;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProviders;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.util.Base64;
import android.util.Base64InputStream;
import android.view.ActionMode;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.mikepenz.aboutlibraries.Libs;
import com.mikepenz.aboutlibraries.LibsBuilder;
import com.mikepenz.materialdrawer.AccountHeader;
import com.mikepenz.materialdrawer.AccountHeaderBuilder;
import com.mikepenz.materialdrawer.Drawer;
import com.mikepenz.materialdrawer.DrawerBuilder;
import com.mikepenz.materialdrawer.model.ProfileDrawerItem;
import com.mikepenz.materialdrawer.model.ProfileSettingDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IProfile;
import com.mikepenz.materialdrawer.model.interfaces.Tagable;

import java.io.ByteArrayInputStream;
import java.util.Collections;
import java.util.List;

import email.schaal.ocreader.database.FeedViewModel;
import email.schaal.ocreader.database.Queries;
import email.schaal.ocreader.database.model.Feed;
import email.schaal.ocreader.database.model.Item;
import email.schaal.ocreader.database.model.TemporaryFeed;
import email.schaal.ocreader.database.model.TreeItem;
import email.schaal.ocreader.database.model.User;
import email.schaal.ocreader.databinding.ActivityListBinding;
import email.schaal.ocreader.service.SyncService;
import email.schaal.ocreader.service.SyncType;
import email.schaal.ocreader.view.DividerItemDecoration;
import email.schaal.ocreader.view.ItemViewHolder;
import email.schaal.ocreader.view.LiveItemsAdapter;
import email.schaal.ocreader.view.LoadMoreAdapter;
import email.schaal.ocreader.view.drawer.DrawerManager;

public class ListActivity extends RealmActivity implements ItemViewHolder.OnClickListener, SwipeRefreshLayout.OnRefreshListener, LoadMoreAdapter.OnLoadMoreListener, ActionMode.Callback {
    private static final String TAG = ListActivity.class.getName();

    public static final String LAYOUT_MANAGER_STATE = "LAYOUT_MANAGER_STATE";

    private ActionMode actionMode;
    private ActivityListBinding binding;

    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if(action != null && (action.equals(SyncService.SYNC_STARTED) || action.equals(SyncService.SYNC_FINISHED))) {
                final SyncType syncType = SyncType.get(intent.getStringExtra(SyncService.EXTRA_TYPE));
                if(syncType != null) {
                    switch (syncType) {
                        case LOAD_MORE:
                            if (action.equals(SyncService.SYNC_FINISHED)) {
                                //todo: adapter.resetLoadMore();
                            }
                            break;
                        case FULL_SYNC:
                            updateSyncStatus();
                            break;
                    }
                }
            }
        }
    };

    private FeedViewModel feedViewModel;

    private void updateSyncStatus() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        boolean needsUpdate = Preferences.SYS_NEEDS_UPDATE_AFTER_SYNC.getBoolean(sharedPreferences);
        boolean syncRunning = Preferences.SYS_SYNC_RUNNING.getBoolean(sharedPreferences);

        if(needsUpdate) {
            drawerManager.reloadAdapters(getRealm(), isShowOnlyUnread());
            setupItemsViewModel(true);

            updateUserProfile();

            sharedPreferences.edit()
                    .putBoolean(Preferences.SYS_NEEDS_UPDATE_AFTER_SYNC.getKey(), false).apply();
        }

        if (binding.swipeRefreshLayout != null) {
            binding.swipeRefreshLayout.setRefreshing(syncRunning);
        }

        binding.bottomAppbar.getMenu().findItem(R.id.menu_sync).setEnabled(!syncRunning);

        //todo: if(!syncRunning)
        //    adapter.resetLoadMore();
    }

    private Drawer startDrawer;
    private DrawerManager drawerManager;
    private ProfileDrawerItem profileDrawerItem;
    private AccountHeader accountHeader;

    private LiveItemsAdapter adapter;
    private LinearLayoutManager layoutManager;

    @Override
    protected void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver);
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateSyncStatus();
        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver, SyncService.syncFilter);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if(!Preferences.hasCredentials(PreferenceManager.getDefaultSharedPreferences(this))) {
            startActivityForResult(new Intent(this, LoginActivity.class), LoginActivity.REQUEST_CODE);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_list);
        setSupportActionBar(binding.toolbarLayout.toolbar);

        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);

        final MenuItem menuItemShowOnlyUnread = binding.bottomAppbar.getMenu().findItem(R.id.menu_show_only_unread);
        menuItemShowOnlyUnread.setChecked(Preferences.SHOW_ONLY_UNREAD.getBoolean(preferences));

        binding.bottomAppbar.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case R.id.menu_settings:
                    final Intent settingsIntent = new Intent(ListActivity.this, SettingsActivity.class);
                    startActivity(settingsIntent);
                    return true;
                case R.id.menu_show_only_unread:
                    final boolean showOnlyUnread = !item.isChecked();
                    preferences.edit().putBoolean(Preferences.SHOW_ONLY_UNREAD.getKey(), showOnlyUnread).apply();
                    item.setChecked(showOnlyUnread);
                    drawerManager.reloadAdapters(getRealm(), showOnlyUnread);
                    reloadListFragment();

                    return true;
                case R.id.menu_sync:
                    SyncService.startSync(ListActivity.this);
                    return true;
                case R.id.menu_about:
                    showAboutDialog();
                    return true;
                case R.id.menu_mark_all_as_read:
                    Queries.markTemporaryFeedAsRead(getRealm(), null, null);
                    return true;
                case R.id.menu_manage_feeds:
                    startActivityForResult(new Intent(ListActivity.this, ManageFeedsActivity.class), ManageFeedsActivity.REQUEST_CODE);
                    return true;
            }
            return false;
        });

        binding.swipeRefreshLayout.setColorSchemeResources(R.color.primary);
        binding.swipeRefreshLayout.setOnRefreshListener(this);

        profileDrawerItem = new ProfileDrawerItem()
                .withName(preferences.getString(Preferences.USERNAME.getKey(), getString(R.string.app_name)))
                .withEmail(Preferences.URL.getString(preferences));

        updateUserProfile();

        IProfile profileSettingsItem = new ProfileSettingDrawerItem()
                .withName(getString(R.string.account_settings))
                .withIconTinted(true)
                .withIcon(R.drawable.ic_settings)
                .withTag((Runnable) () -> {
                    Intent loginIntent = new Intent(ListActivity.this, LoginActivity.class);
                    startActivityForResult(loginIntent, LoginActivity.REQUEST_CODE);
                });

        accountHeader = new AccountHeaderBuilder()
                .withActivity(this)
                .withHeaderBackground(R.drawable.header_background)
                .addProfiles(profileDrawerItem, profileSettingsItem)
                .withCurrentProfileHiddenInList(true)
                .withProfileImagesClickable(false)
                .withSavedInstance(savedInstanceState)
                .withOnAccountHeaderListener((view, profile, current) -> {
                    if (profile instanceof Tagable) {
                        Tagable tagable = (Tagable) profile;
                        if (tagable.getTag() instanceof Runnable) {
                            ((Runnable) tagable.getTag()).run();
                            return false;
                        }
                    }
                    return true;
                })
                .build();

        DrawerBuilder startDrawerBuilder = new DrawerBuilder()
                .withActivity(this)
                .withAccountHeader(accountHeader)
                .withOnDrawerListener(new Drawer.OnDrawerListener() {
                    @Override
                    public void onDrawerOpened(View drawerView) {
                        drawerManager.getStartAdapter().updateUnreadCount(getRealm(), isShowOnlyUnread());
                    }

                    @Override
                    public void onDrawerClosed(View drawerView) {
                    }

                    @Override
                    public void onDrawerSlide(View drawerView, float slideOffset) {

                    }
                })
                .withOnDrawerItemClickListener((view, position, drawerItem) -> {
                    if (drawerItem.getTag() instanceof TreeItem) {
                        TreeItem item = (TreeItem) drawerItem.getTag();
                        onStartDrawerItemClicked(item);
                        return false;
                    } else if (drawerItem.getTag() instanceof Runnable) {
                        ((Runnable) drawerItem.getTag()).run();
                    }
                    return true;
                })
                .withSavedInstance(savedInstanceState);

        DrawerBuilder endDrawerBuilder = new DrawerBuilder()
                .withActivity(this)
                .withDrawerGravity(Gravity.END)
                .withSavedInstance(savedInstanceState)
                .withShowDrawerOnFirstLaunch(true)
                .withOnDrawerListener(new Drawer.OnDrawerListener() {
                    @Override
                    public void onDrawerOpened(View drawerView) {
                        drawerManager.getEndAdapter().updateUnreadCount(getRealm(), isShowOnlyUnread());
                    }

                    @Override
                    public void onDrawerClosed(View drawerView) {

                    }

                    @Override
                    public void onDrawerSlide(View drawerView, float slideOffset) {

                    }
                })
                .withOnDrawerItemClickListener((view, position, drawerItem) -> {
                    if (drawerItem.getTag() instanceof Feed) {
                        Feed feed = (Feed) drawerItem.getTag();
                        onEndDrawerItemClicked(feed);
                        return false;
                    }
                    return true;
                });

        startDrawerBuilder.withToolbar(binding.bottomAppbar);
        startDrawer = startDrawerBuilder.build();

        drawerManager = new DrawerManager(this, startDrawer, endDrawerBuilder.append(startDrawer));

        layoutManager = new LinearLayoutManager(this);

        adapter = new LiveItemsAdapter(Collections.emptyList(), this);

        feedViewModel = ViewModelProviders.of(this).get(FeedViewModel.class);

        setupItemsViewModel(false);

        feedViewModel.getItems().observe(this, items -> {
            if(adapter != null) {
                adapter.updateItems(items);
                if(adapter.getSelectedItemsCount() > 0 && actionMode == null) {
                    actionMode = startActionMode(this);
                }
            }
            binding.listviewSwitcher.setDisplayedChild(items.isEmpty() ? 0 : 1);
        });

        feedViewModel.getTemporaryFeed().observe(this, temporaryFeed -> {
            //noinspection ConstantConditions
            getSupportActionBar().setTitle(temporaryFeed.getName());
        });

        binding.itemsRecyclerview.setAdapter(adapter);
        binding.itemsRecyclerview.setLayoutManager(layoutManager);

        if(savedInstanceState == null && getIntent().hasExtra(SyncService.EXTRA_ID)) {
            drawerManager.getState().restore(getRealm(), getIntent().getIntExtra(SyncService.EXTRA_ID, -10), null, false);
        } else {
            drawerManager.getState().restoreInstanceState(getRealm(), getPreferences(MODE_PRIVATE));
        }

        binding.itemsRecyclerview.addItemDecoration(new DividerItemDecoration(this, R.dimen.divider_inset));

        if(savedInstanceState != null) {
            layoutManager.onRestoreInstanceState(savedInstanceState.getParcelable(LAYOUT_MANAGER_STATE));
            adapter.onRestoreInstanceState(savedInstanceState);
        }

        drawerManager.reloadAdapters(getRealm(), isShowOnlyUnread());
    }

    private boolean isShowOnlyUnread() {
        return Preferences.SHOW_ONLY_UNREAD.getBoolean(PreferenceManager.getDefaultSharedPreferences(this));
    }

    private void setupItemsViewModel(final boolean updateTemporaryFeed) {
        final TreeItem treeItem = drawerManager.getState().getTreeItem();

        if(treeItem == null)
            return;

        final TemporaryFeed temporaryFeed = TemporaryFeed.getListTemporaryFeed(getRealm());

        if (updateTemporaryFeed || temporaryFeed.getTreeItemId() != treeItem.getId()) {
            feedViewModel.getRealm().executeTransaction(realm -> {
                List<Item> tempItems = treeItem.getItems(realm, isShowOnlyUnread());
                temporaryFeed.setTreeItemId(treeItem.getId());
                temporaryFeed.setName(treeItem.getName());
                temporaryFeed.getItems().clear();
                if (tempItems != null) {
                    temporaryFeed.getItems().addAll(tempItems);
                }
            });
        }

        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        feedViewModel.updateTemporaryFeed(temporaryFeed, preferences);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(LAYOUT_MANAGER_STATE, layoutManager.onSaveInstanceState());
        drawerManager.getState().saveInstanceState(getPreferences(MODE_PRIVATE));
        adapter.onSaveInstanceState(outState);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        drawerManager.getState().saveInstanceState(getPreferences(MODE_PRIVATE));
    }

    private void onStartDrawerItemClicked(TreeItem item) {
        drawerManager.setSelectedTreeItem(getRealm(), item, isShowOnlyUnread());
        reloadListFragment();
    }

    private void onEndDrawerItemClicked(Feed feed) {
        drawerManager.setSelectedFeed(feed);
        reloadListFragment();
    }

    private void reloadListFragment() {
        setupItemsViewModel(true);
        binding.itemsRecyclerview.scrollToPosition(0);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(resultCode == Activity.RESULT_OK) {
            switch (requestCode) {
                case LoginActivity.REQUEST_CODE:
                    if (data != null && data.getBooleanExtra(LoginActivity.EXTRA_IMPROPERLY_CONFIGURED_CRON, false)) {
                        Snackbar.make(binding.coordinatorLayout, R.string.updater_improperly_configured, Snackbar.LENGTH_INDEFINITE)
                                .setAction(R.string.more_info, v -> startActivity(data))
                                .setActionTextColor(ContextCompat.getColor(this, R.color.warning))
                                .show();
                    }
                    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
                    profileDrawerItem.withName(Preferences.USERNAME.getString(preferences));
                    profileDrawerItem.withEmail(Preferences.URL.getString(preferences));

                    drawerManager.reset();
                    reloadListFragment();
                    Queries.resetDatabase();
                    SyncService.startSync(this, true);
                    break;
                case ItemPagerActivity.REQUEST_CODE:
                    if(data != null)
                        binding.itemsRecyclerview.smoothScrollToPosition(data.getIntExtra(ItemPagerActivity.EXTRA_CURRENT_POSITION, -1));
                    break;
                case ManageFeedsActivity.REQUEST_CODE:
                    drawerManager.reset();
                    reloadListFragment();
                    drawerManager.reloadAdapters(getRealm(), isShowOnlyUnread());
                    break;
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        //getMenuInflater().inflate(R.menu.menu_item_list, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.menu_about:
                showAboutDialog();
                return true;
            case R.id.menu_manage_feeds:
                startActivityForResult(new Intent(this, ManageFeedsActivity.class), ManageFeedsActivity.REQUEST_CODE);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showAboutDialog() {
        new LibsBuilder()
                .withAboutIconShown(true)
                .withAboutVersionShown(true)
                .withAboutDescription(getString(R.string.about_app, getString(R.string.app_year_author), getString(R.string.app_url)))
                .withAboutAppName(getString(R.string.app_name))
                .withLicenseShown(true)
                .withActivityStyle(Preferences.DARK_THEME.getBoolean(PreferenceManager.getDefaultSharedPreferences(this)) ? Libs.ActivityStyle.DARK : Libs.ActivityStyle.LIGHT_DARK_TOOLBAR)
                .withActivityTitle(getString(R.string.about))
                .withFields(R.string.class.getFields())
                .start(this);
    }

    @Override
    public void onItemClick(Item item, int position) {
        if(actionMode == null) {
            Intent itemActivityIntent = new Intent(this, ItemPagerActivity.class);
            itemActivityIntent.putExtra(ItemPagerActivity.EXTRA_CURRENT_POSITION, position);
            startActivityForResult(itemActivityIntent, ItemPagerActivity.REQUEST_CODE);
        } else {
            adapter.toggleSelection(position);
            if(adapter.getSelectedItemsCount() == 0)
                actionMode.finish();
            else {
                actionMode.setTitle(String.valueOf(adapter.getSelectedItemsCount()));
                actionMode.invalidate();
            }
        }
    }

    @Override
    public void onItemLongClick(Item item, int position) {
        if(actionMode != null || Preferences.SYS_SYNC_RUNNING.getBoolean(PreferenceManager.getDefaultSharedPreferences(this)))
            return;

        adapter.toggleSelection(position);
        actionMode = startActionMode(this);
    }

    @Override
    public void onRefresh() {
        SyncService.startSync(this);
    }

    private void updateUserProfile() {
        final String username = Preferences.USERNAME.getString(PreferenceManager.getDefaultSharedPreferences(this));

        if(username != null) {
            final User user = getRealm().where(User.class).equalTo(User.USER_ID, username).findFirst();

            if (user != null) {
                profileDrawerItem.withName(user.getDisplayName());
                final String encodedImage = user.getAvatar();
                if (encodedImage != null) {
                    Bitmap avatarBitmap = BitmapFactory.decodeStream(new Base64InputStream(new ByteArrayInputStream(encodedImage.getBytes()), Base64.DEFAULT));
                    profileDrawerItem.withIcon(avatarBitmap);
                } else {
                    profileDrawerItem.withIcon(R.mipmap.ic_launcher_round);
                }
                if (accountHeader != null)
                    accountHeader.updateProfile(profileDrawerItem);
            } else {
                profileDrawerItem.withIcon(R.mipmap.ic_launcher_round);
            }
        } else {
            profileDrawerItem.withIcon(R.mipmap.ic_launcher_round);
        }
    }

    @Override
    public void onLoadMore(@NonNull TreeItem treeItem) {
        final Number minId = TemporaryFeed.getListTemporaryFeed(getRealm())
                .getItems()
                .where()
                .min(Item.ID);

        // minId is null if there are no feed items in treeItem
        SyncService.startLoadMore(this, treeItem.getId(), minId != null ? minId.longValue() : 0, treeItem instanceof Feed);
    }

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        mode.getMenuInflater().inflate(R.menu.menu_item_list_action, menu);
        mode.setTitle(String.valueOf(adapter.getSelectedItemsCount()));
        startDrawer.getDrawerLayout().setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
        binding.swipeRefreshLayout.setEnabled(false);
        return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        int selectedItemsCount = adapter.getSelectedItemsCount();

        // the menu only changes on the first and second selection
        if(selectedItemsCount > 2)
            return false;

        Item firstSelectedItem = adapter.getFirstSelectedItem();

        boolean firstSelectedUnread = firstSelectedItem != null && firstSelectedItem.isUnread();
        menu.findItem(R.id.action_mark_read).setVisible(firstSelectedUnread);
        menu.findItem(R.id.action_mark_unread).setVisible(!firstSelectedUnread);

        boolean firstSelectedStarred = firstSelectedItem != null && firstSelectedItem.isStarred();
        menu.findItem(R.id.action_mark_starred).setVisible(!firstSelectedStarred);
        menu.findItem(R.id.action_mark_unstarred).setVisible(firstSelectedStarred);

        menu.findItem(R.id.action_mark_above_read).setVisible(selectedItemsCount == 1);

        return true;
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_mark_read:
                Queries.setItemsUnread(getRealm(), false, adapter.getSelectedItems());
                mode.finish();
                return true;
            case R.id.action_mark_unread:
                Queries.setItemsUnread(getRealm(), true, adapter.getSelectedItems());
                mode.finish();
                return true;
            case R.id.action_mark_starred:
                Queries.setItemsStarred(getRealm(), true, adapter.getSelectedItems());
                mode.finish();
                return true;
            case R.id.action_mark_unstarred:
                Queries.setItemsStarred(getRealm(), false, adapter.getSelectedItems());
                mode.finish();
                return true;
            case R.id.action_mark_above_read:
                Queries.markAboveAsRead(getRealm(), feedViewModel.getItems().getValue(), adapter.getSelectedItems()[0].getId());
                mode.finish();
                return true;
        }
        return false;
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
        actionMode = null;
        startDrawer.getDrawerLayout().setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
        binding.swipeRefreshLayout.setEnabled(true);
        adapter.clearSelection();
    }
}
