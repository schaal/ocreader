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
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.databinding.DataBindingUtil;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatDelegate;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.PopupMenu;
import android.text.Html;
import android.util.Base64;
import android.util.Base64InputStream;
import android.view.ActionMode;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;

import com.mikepenz.materialdrawer.AccountHeader;
import com.mikepenz.materialdrawer.AccountHeaderBuilder;
import com.mikepenz.materialdrawer.Drawer;
import com.mikepenz.materialdrawer.DrawerBuilder;
import com.mikepenz.materialdrawer.interfaces.OnCheckedChangeListener;
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem;
import com.mikepenz.materialdrawer.model.ProfileDrawerItem;
import com.mikepenz.materialdrawer.model.ProfileSettingDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IProfile;
import com.mikepenz.materialdrawer.model.interfaces.Tagable;

import java.io.ByteArrayInputStream;

import email.schaal.ocreader.database.Queries;
import email.schaal.ocreader.database.model.Feed;
import email.schaal.ocreader.database.model.Item;
import email.schaal.ocreader.database.model.TemporaryFeed;
import email.schaal.ocreader.database.model.TreeItem;
import email.schaal.ocreader.database.model.User;
import email.schaal.ocreader.databinding.ActivityListBinding;
import email.schaal.ocreader.service.SyncService;
import email.schaal.ocreader.util.FaviconLoader;
import email.schaal.ocreader.view.DividerItemDecoration;
import email.schaal.ocreader.view.ItemViewHolder;
import email.schaal.ocreader.view.ItemsAdapter;
import email.schaal.ocreader.view.ScrollAwareFABBehavior;
import email.schaal.ocreader.view.SelectableItemsAdapter;
import email.schaal.ocreader.view.drawer.DrawerManager;
import io.realm.Realm;
import io.realm.Sort;

public class ListActivity extends RealmActivity implements ItemViewHolder.OnClickListener, SwipeRefreshLayout.OnRefreshListener, ItemsAdapter.OnLoadMoreListener, OnCheckedChangeListener, ActionMode.Callback {

    private static final int REFRESH_DRAWER_ITEM_ID = 999;
    public static final String LAYOUT_MANAGER_STATE = "LAYOUT_MANAGER_STATE";

    private ActionMode actionMode;
    private ActivityListBinding binding;

    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if(action.equals(SyncService.SYNC_STARTED) || action.equals(SyncService.SYNC_FINISHED)) {
                switch (intent.getStringExtra(SyncService.EXTRA_TYPE)) {
                    case SyncService.ACTION_LOAD_MORE:
                        if (action.equals(SyncService.SYNC_FINISHED)) {
                            adapter.updateItems(true);
                            adapter.resetLoadMore();
                        }
                        break;
                    case SyncService.ACTION_FULL_SYNC:
                        updateSyncStatus();
                        break;
                }
            }
        }
    };

    private void updateSyncStatus() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        boolean needsUpdate = Preferences.SYS_NEEDS_UPDATE_AFTER_SYNC.getBoolean(sharedPreferences);
        boolean syncRunning = Preferences.SYS_SYNC_RUNNING.getBoolean(sharedPreferences);

        if(needsUpdate) {
            drawerManager.reloadAdapters(getRealm(), isShowOnlyUnread());

            adapter.updateItems(true);

            updateUserProfile();

            sharedPreferences.edit()
                    .putBoolean(Preferences.SYS_NEEDS_UPDATE_AFTER_SYNC.getKey(), false).apply();
        }

        if (refreshDrawerItem != null) {
            refreshDrawerItem.withEnabled(!syncRunning);
            startDrawer.updateStickyFooterItem(refreshDrawerItem);
        }

        if (binding.swipeRefreshLayout != null) {
            binding.swipeRefreshLayout.setRefreshing(syncRunning);
        }

        if(!syncRunning)
            adapter.resetLoadMore();
    }

    private Drawer startDrawer;
    private DrawerManager drawerManager;
    private ProfileDrawerItem profileDrawerItem;
    private PrimaryDrawerItem refreshDrawerItem;
    private AccountHeader accountHeader;

    private SelectableItemsAdapter adapter;
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
        adapter.updateItems(false);
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

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);

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
                .withTag(new Runnable() {
                    @Override
                    public void run() {
                        Intent loginIntent = new Intent(ListActivity.this, LoginActivity.class);
                        startActivityForResult(loginIntent, LoginActivity.REQUEST_CODE);
                    }
                });

        accountHeader = new AccountHeaderBuilder()
                .withActivity(this)
                .withHeaderBackground(R.drawable.header_background)
                .addProfiles(profileDrawerItem, profileSettingsItem)
                .withCurrentProfileHiddenInList(true)
                .withProfileImagesClickable(false)
                .withSavedInstance(savedInstanceState)
                .withOnAccountHeaderListener(new AccountHeader.OnAccountHeaderListener() {
                    @Override
                    public boolean onProfileChanged(View view, IProfile profile, boolean current) {
                        if (profile instanceof Tagable) {
                            Tagable tagable = (Tagable) profile;
                            if (tagable.getTag() instanceof Runnable) {
                                ((Runnable) tagable.getTag()).run();
                                return false;
                            }
                        }
                        return true;
                    }
                })
                .build();

        refreshDrawerItem = new PrimaryDrawerItem()
                .withName(getString(R.string.action_sync))
                .withSelectable(false)
                .withIconTintingEnabled(true)
                .withIcon(R.drawable.ic_refresh)
                .withIdentifier(REFRESH_DRAWER_ITEM_ID)
                .withTag(new Runnable() {
                    @Override
                    public void run() {
                        SyncService.startSync(ListActivity.this);
                    }
                });

        DrawerBuilder startDrawerBuilder = new DrawerBuilder()
                .withActivity(this)
                .withAccountHeader(accountHeader)
                .addStickyDrawerItems(refreshDrawerItem)
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
                .withOnDrawerItemClickListener(new Drawer.OnDrawerItemClickListener() {
                    @Override
                    public boolean onItemClick(View view, int position, IDrawerItem drawerItem) {
                        if (drawerItem.getTag() instanceof TreeItem) {
                            TreeItem item = (TreeItem) drawerItem.getTag();
                            onStartDrawerItemClicked(item);
                            return false;
                        } else if (drawerItem.getTag() instanceof Runnable) {
                            ((Runnable) drawerItem.getTag()).run();
                        }
                        return true;
                    }
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
                .withOnDrawerItemClickListener(new Drawer.OnDrawerItemClickListener() {
                    @Override
                    public boolean onItemClick(View view, int position, IDrawerItem drawerItem) {
                        if (drawerItem.getTag() instanceof Feed) {
                            Feed feed = (Feed) drawerItem.getTag();
                            onEndDrawerItemClicked(feed);
                            return false;
                        }
                        return true;
                    }
                });

        startDrawerBuilder.withToolbar(binding.toolbarLayout.toolbar);
        startDrawer = startDrawerBuilder.build();

        drawerManager = new DrawerManager(this, startDrawer, endDrawerBuilder.append(startDrawer), isShowOnlyUnread(), this);

        layoutManager = new LinearLayoutManager(this);

        adapter = new SelectableItemsAdapter(getRealm(), drawerManager.getState(), this, Preferences.ORDER.getOrder(preferences), Preferences.SORT_FIELD.getString(preferences), this);

        binding.fabMarkAllAsRead.setOnClickListener(new View.OnClickListener() {
            private void onCompletion(View view) {
                adapter.updateItems(false);
                view.setEnabled(true);
            }

            @Override
            public void onClick(final View v) {
                Queries.markTemporaryFeedAsRead(getRealm(),
                        new Realm.Transaction.OnSuccess() {
                            @Override
                            public void onSuccess() {
                                onCompletion(v);
                            }
                        }, new Realm.Transaction.OnError() {
                            @Override
                            public void onError(Throwable error) {
                                error.printStackTrace();
                                onCompletion(v);
                            }
                        });
            }
        });

        binding.fabMarkAllAsRead.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                Toast.makeText(ListActivity.this, R.string.mark_all_as_read, Toast.LENGTH_SHORT).show();
                return true;
            }
        });

        binding.itemsRecyclerview.setAdapter(adapter);
        binding.itemsRecyclerview.setLayoutManager(layoutManager);

        if(savedInstanceState != null)
            layoutManager.onRestoreInstanceState(savedInstanceState.getParcelable(LAYOUT_MANAGER_STATE));

        binding.itemsRecyclerview.addItemDecoration(new DividerItemDecoration(this, 40));
        binding.itemsRecyclerview.setItemAnimator(new DefaultItemAnimator());

        drawerManager.getState().restoreInstanceState(getRealm(), PreferenceManager.getDefaultSharedPreferences(this));

        adapter.updateItems(false);

        drawerManager.reloadAdapters(getRealm(), isShowOnlyUnread());

        //noinspection ConstantConditions
        getSupportActionBar().setTitle(drawerManager.getState().getTreeItem().getName());
    }

    private boolean isShowOnlyUnread() {
        return Preferences.SHOW_ONLY_UNREAD.getBoolean(PreferenceManager.getDefaultSharedPreferences(this));
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(LAYOUT_MANAGER_STATE, layoutManager.onSaveInstanceState());
        drawerManager.getState().saveInstanceState(PreferenceManager.getDefaultSharedPreferences(this));
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
        //noinspection ConstantConditions
        getSupportActionBar().setTitle(drawerManager.getState().getTreeItem().getName());
        adapter.updateItems(true);
        binding.itemsRecyclerview.scrollToPosition(0);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable final Intent data) {
        if(resultCode == Activity.RESULT_OK) {
            switch (requestCode) {
                case LoginActivity.REQUEST_CODE:
                    if (data != null && data.getBooleanExtra(LoginActivity.EXTRA_IMPROPERLY_CONFIGURED_CRON, false)) {
                        Snackbar.make(binding.coordinatorLayout, R.string.updater_improperly_configured, Snackbar.LENGTH_INDEFINITE)
                                .setAction(R.string.more_info, new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        startActivity(data);
                                    }
                                })
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
        getMenuInflater().inflate(R.menu.menu_item_list, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.menu_sort:
                showSortPopup(findViewById(R.id.menu_sort));

                return true;
            case R.id.menu_about:
                showAboutDialog();
                return true;
            case R.id.menu_change_theme:
                final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
                final int daynightmode = Preferences.DAY_NIGHT_MODE.getInt(sharedPreferences) == AppCompatDelegate.MODE_NIGHT_NO ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO;
                sharedPreferences.edit().putInt(Preferences.DAY_NIGHT_MODE.getKey(), daynightmode).apply();
                AppCompatDelegate.setDefaultNightMode(daynightmode);
                FaviconLoader.clearCache();
                recreate();
                return true;
            case R.id.menu_manage_feeds:
                startActivityForResult(new Intent(this, ManageFeedsActivity.class), ManageFeedsActivity.REQUEST_CODE);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showSortPopup(View view) {
        PopupMenu popupMenu = new PopupMenu(this, view);
        popupMenu.inflate(R.menu.menu_sort);

        final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        Sort order = Preferences.ORDER.getOrder(sharedPreferences);
        String field = Preferences.SORT_FIELD.getString(sharedPreferences);

        final MenuItem orderMenuItem = popupMenu.getMenu().findItem(R.id.action_sort_order);
        orderMenuItem.setChecked(order == Sort.ASCENDING);

        final MenuItem selectedField;

        if(field.equals(Item.PUB_DATE)) {
            selectedField = popupMenu.getMenu().findItem(R.id.action_sort_pubdate);
        } else {
            selectedField = popupMenu.getMenu().findItem(R.id.action_sort_updatedate);
        }
        selectedField.setChecked(true);

        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                boolean updateSort = false;

                switch (item.getItemId()) {
                    case R.id.action_sort_order:
                        item.setChecked(!item.isChecked());
                        sharedPreferences.edit().putBoolean(Preferences.ORDER.getKey(), item.isChecked()).apply();
                        updateSort = true;
                        break;
                    case R.id.action_sort_pubdate:
                        sharedPreferences.edit().putString(Preferences.SORT_FIELD.getKey(), Item.PUB_DATE).apply();
                        updateSort = true;
                        break;
                    case R.id.action_sort_updatedate:
                        sharedPreferences.edit().putString(Preferences.SORT_FIELD.getKey(), Item.UPDATED_AT).apply();
                        updateSort = true;
                        break;
                }

                if(updateSort) {
                    adapter.setOrder(Preferences.ORDER.getOrder(sharedPreferences), Preferences.SORT_FIELD.getString(sharedPreferences));
                }
                return updateSort;
            }
        });

        popupMenu.show();
    }

    private void showAboutDialog() {
        View aboutView = getLayoutInflater().inflate(R.layout.dialog_about, binding.coordinatorLayout, false);

        TextView textView = (TextView) aboutView.findViewById(R.id.textViewCopyright);
        //noinspection deprecation
        textView.setText(Html.fromHtml(getString(R.string.about_app, getString(R.string.app_year_author), getString(R.string.app_url))));
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setIcon(R.drawable.ic_launcher);
        builder.setTitle(String.format("%s %s", getString(R.string.app_name), BuildConfig.VERSION_NAME));
        builder.setView(aboutView);
        builder.show();
    }

    @Override
    public void onItemClick(Item item, int position) {
        if(actionMode == null) {
            Intent itemActivityIntent = new Intent(this, ItemPagerActivity.class);
            itemActivityIntent.putExtra(ItemPagerActivity.POSITION, position);
            startActivityForResult(itemActivityIntent, ItemPagerActivity.REQUEST_CODE);
        } else {
            adapter.toggleSelection(item, position);
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

        adapter.toggleSelection(item, position);
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
                    profileDrawerItem.withIcon(R.drawable.ic_launcher);
                }
                if (accountHeader != null)
                    accountHeader.updateProfile(profileDrawerItem);
            } else {
                profileDrawerItem.withIcon(R.drawable.ic_launcher);
            }
        } else {
            profileDrawerItem.withIcon(R.drawable.ic_launcher);
        }
    }

    @Override
    public void onLoadMore(@NonNull TreeItem treeItem) {
        final Number minId = getRealm().where(TemporaryFeed.class)
                .findFirst()
                .getItems()
                .where()
                .min(Item.ID);

        // minId is null if there are no feed items in treeItem
        SyncService.startLoadMore(this, treeItem.getId(), minId != null ? minId.longValue() : 0, treeItem instanceof Feed);
    }

    @Override
    public void onCheckedChanged(IDrawerItem drawerItem, CompoundButton buttonView, boolean isChecked) {
        PreferenceManager.getDefaultSharedPreferences(this).edit().putBoolean(Preferences.SHOW_ONLY_UNREAD.getKey(), isChecked).apply();
        drawerManager.reloadAdapters(getRealm(), isChecked);
    }

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        mode.getMenuInflater().inflate(R.menu.menu_item_list_action, menu);
        mode.setTitle(String.valueOf(adapter.getSelectedItemsCount()));
        startDrawer.getDrawerLayout().setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
        binding.swipeRefreshLayout.setEnabled(false);
        binding.fabMarkAllAsRead.setVisibility(View.GONE);
        ((CoordinatorLayout.LayoutParams)binding.fabMarkAllAsRead.getLayoutParams()).setBehavior(null);
        return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        Boolean firstSelectedUnread = adapter.firstSelectedUnread();
        if(firstSelectedUnread != null) {
            menu.findItem(R.id.action_mark_read).setVisible(firstSelectedUnread);
            menu.findItem(R.id.action_mark_unread).setVisible(!firstSelectedUnread);
        }

        Boolean firstSelectedStarred = adapter.firstSelectedStarred();
        if(firstSelectedStarred != null) {
            menu.findItem(R.id.action_mark_starred).setVisible(!firstSelectedStarred);
            menu.findItem(R.id.action_mark_unstarred).setVisible(firstSelectedStarred);
        }

        int selectedItemsCount = adapter.getSelectedItemsCount();

        menu.findItem(R.id.action_mark_above_read).setVisible(selectedItemsCount == 1);

        // the menu only changes on the first and second selection
        return selectedItemsCount <= 2;
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
                Queries.markAboveAsRead(getRealm(), adapter.getItems(), adapter.getSelectedItems()[0].getId());
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
        binding.fabMarkAllAsRead.setVisibility(View.VISIBLE);
        ((CoordinatorLayout.LayoutParams)binding.fabMarkAllAsRead.getLayoutParams()).setBehavior(new ScrollAwareFABBehavior());
        adapter.clearSelection();
    }
}
