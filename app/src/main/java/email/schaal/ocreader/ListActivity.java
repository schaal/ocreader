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

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.util.Base64;
import android.util.Base64InputStream;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.TextView;

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
import java.util.ArrayList;

import email.schaal.ocreader.database.Queries;
import email.schaal.ocreader.model.AllUnreadFolder;
import email.schaal.ocreader.model.Feed;
import email.schaal.ocreader.model.Item;
import email.schaal.ocreader.model.TemporaryFeed;
import email.schaal.ocreader.model.TreeItem;
import email.schaal.ocreader.model.User;
import email.schaal.ocreader.service.SyncService;
import email.schaal.ocreader.view.DividerItemDecoration;
import email.schaal.ocreader.view.ItemViewHolder;
import email.schaal.ocreader.view.ItemsAdapter;
import email.schaal.ocreader.view.drawer.DrawerManager;
import io.realm.Realm;

public class ListActivity extends RealmActivity implements ItemViewHolder.OnClickListener, SwipeRefreshLayout.OnRefreshListener, ItemsAdapter.OnLoadMoreListener, OnCheckedChangeListener {
    private static final String TAG = ListActivity.class.getName();

    private static final int REFRESH_DRAWER_ITEM_ID = 999;
    public static final String LAYOUT_MANAGER_STATE = "LAYOUT_MANAGER_STATE";

    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getAction().equals(SyncService.SYNC_STARTED) || intent.getAction().equals(SyncService.SYNC_FINISHED)) {
                switch (intent.getStringExtra(SyncService.EXTRA_TYPE)) {
                    case SyncService.ACTION_LOAD_MORE:
                        if (intent.getAction().equals(SyncService.SYNC_FINISHED)) {
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

        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setRefreshing(syncRunning);
        }

        if(!syncRunning)
            adapter.resetLoadMore();
    }

    private Drawer startDrawer;
    private DrawerManager drawerManager;
    private ProfileDrawerItem profileDrawerItem;
    private PrimaryDrawerItem refreshDrawerItem;
    private AccountHeader accountHeader;

    private SwipeRefreshLayout swipeRefreshLayout;
    private FloatingActionButton fab_mark_all_read;

    private ItemsAdapter adapter;
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
        setContentView(R.layout.activity_list);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);

        RecyclerView itemsRecyclerView = (RecyclerView) findViewById(R.id.items_recyclerview);

        adapter = new ItemsAdapter(getRealm(), this, this);

        itemsRecyclerView.setAdapter(adapter);

        layoutManager = new LinearLayoutManager(this);

        itemsRecyclerView.setLayoutManager(layoutManager);

        if(savedInstanceState != null)
            layoutManager.onRestoreInstanceState(savedInstanceState.getParcelable(LAYOUT_MANAGER_STATE));

        itemsRecyclerView.addItemDecoration(new DividerItemDecoration(this));
        itemsRecyclerView.setItemAnimator(new DefaultItemAnimator());

        swipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipeRefreshLayout);
        swipeRefreshLayout.setColorSchemeResources(R.color.primary);
        swipeRefreshLayout.setOnRefreshListener(this);

        fab_mark_all_read = (FloatingActionButton) findViewById(R.id.fab_mark_all_read);
        fab_mark_all_read.setOnClickListener(new View.OnClickListener() {
            private void cleanup(View v) {
                adapter.updateItems(false);
                v.setEnabled(true);
            }
            @Override
            public void onClick(final View v) {
                v.setEnabled(false);

                Queries.getInstance().markTemporaryFeedAsRead(getRealm(), adapter.getItemId(layoutManager.findLastVisibleItemPosition()),
                        new Realm.Transaction.OnSuccess() {
                            @Override
                            public void onSuccess() {
                                cleanup(v);
                            }
                        }, new Realm.Transaction.OnError() {
                            @Override
                            public void onError(Throwable error) {
                                error.printStackTrace();
                                cleanup(v);
                            }
                        });
            }
        });

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
                .withHeaderBackground(new ColorDrawable(ContextCompat.getColor(this, R.color.primary)))
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

        ArrayList<IDrawerItem> stickyItems = new ArrayList<>(1);

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

        stickyItems.add(refreshDrawerItem);

        DrawerBuilder startDrawerBuilder = new DrawerBuilder()
                .withActivity(this)
                .withAccountHeader(accountHeader)
                .withStickyDrawerItems(stickyItems)
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

        startDrawerBuilder.withToolbar(toolbar);
        startDrawer = startDrawerBuilder.build();

        drawerManager = new DrawerManager(this, startDrawer, endDrawerBuilder.append(startDrawer), isShowOnlyUnread(), this);
        drawerManager.getState().restoreInstanceState(getRealm(), PreferenceManager.getDefaultSharedPreferences(this));
        adapter.setTreeItem(drawerManager.getState().getTreeItem(), drawerManager.getState().getStartDrawerItem() instanceof AllUnreadFolder, false);

        drawerManager.reloadAdapters(getRealm(), isShowOnlyUnread());

        //noinspection ConstantConditions
        getSupportActionBar().setTitle(drawerManager.getState().getTreeItem().getTitle());
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
        reloadListFragment(item);
    }

    private void onEndDrawerItemClicked(Feed feed) {
        drawerManager.setSelectedFeed(feed);
        reloadListFragment(feed);
    }

    private void reloadListFragment(TreeItem item) {
        //noinspection ConstantConditions
        getSupportActionBar().setTitle(item.getTitle());
        adapter.setTreeItem(item, drawerManager.getState().getStartDrawerItem() instanceof AllUnreadFolder);
        fab_mark_all_read.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, final Intent data) {
        if(requestCode == LoginActivity.REQUEST_CODE) {
            switch(resultCode) {
                case LoginActivity.RESULT_OK:
                    if(data.getBooleanExtra(LoginActivity.EXTRA_IMPROPERLY_CONFIGURED_CRON, false)) {
                        Snackbar snackbar = Snackbar.make(findViewById(R.id.coordinator_layout), R.string.updater_improperly_configured, Snackbar.LENGTH_INDEFINITE);
                        snackbar.setAction(R.string.more_info, new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                startActivity(data);
                            }
                        }).setActionTextColor(Color.RED);
                        TextView tv = (TextView) snackbar.getView().findViewById(android.support.design.R.id.snackbar_text);
                        tv.setTextColor(Color.WHITE);
                        snackbar.show();
                    }
                    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
                    profileDrawerItem.withName(Preferences.USERNAME.getString(preferences));
                    profileDrawerItem.withEmail(Preferences.URL.getString(preferences));

                    Queries.getInstance().resetDatabase();
                    SyncService.startSync(this, true);
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
            case R.id.menu_about:
                showAboutDialog();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showAboutDialog() {
        View aboutView = getLayoutInflater().inflate(R.layout.dialog_about, (ViewGroup) findViewById(R.id.coordinator_layout), false);

        TextView textView = (TextView) aboutView.findViewById(R.id.textViewCopyright);
        textView.setText(Html.fromHtml(getString(R.string.about_app, getString(R.string.app_year_author), getString(R.string.app_url))));
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setIcon(R.mipmap.ic_launcher);
        builder.setTitle(String.format("%s %s", getString(R.string.app_name), BuildConfig.VERSION_NAME));
        builder.setView(aboutView);
        builder.show();
    }

    @Override
    public void onItemClick(Item item, int position) {
        Intent itemActivityIntent = new Intent(this, ItemPagerActivity.class);
        itemActivityIntent.putExtra(ItemPagerActivity.POSITION, position);
        startActivity(itemActivityIntent);
    }

    @Override
    public void onRefresh() {
        SyncService.startSync(this);
    }

    private void updateUserProfile() {
        User user = getRealm().where(User.class).findFirst();
        if(user != null) {
            profileDrawerItem.withName(user.getDisplayName());
            String encodedImage = user.getAvatar();
            if(encodedImage != null) {
                Bitmap avatarBitmap = BitmapFactory.decodeStream(new Base64InputStream(new ByteArrayInputStream(encodedImage.getBytes()), Base64.DEFAULT));
                profileDrawerItem.withIcon(avatarBitmap);
            } else {
                profileDrawerItem.withIcon(R.mipmap.ic_launcher);
            }
            if(accountHeader != null)
                accountHeader.updateProfile(profileDrawerItem);
        }
    }

    @Override
    public void onLoadMore(@NonNull TreeItem treeItem) {
        TemporaryFeed temporaryFeed = getRealm().where(TemporaryFeed.class).findFirst();
        long id = treeItem.getId();

        final Number minId = temporaryFeed.getItems().where().min(Item.ID);
        long offset;

        // minId is null if there are no feed items in treeItem
        if(minId != null)
            offset = minId.longValue();
        else
            offset = 0;

        boolean isFeed = treeItem instanceof Feed;

        SyncService.startLoadMore(this, id, offset, isFeed);
    }

    @Override
    public void onCheckedChanged(IDrawerItem drawerItem, CompoundButton buttonView, boolean isChecked) {
        PreferenceManager.getDefaultSharedPreferences(this).edit().putBoolean(Preferences.SHOW_ONLY_UNREAD.getKey(), isChecked).apply();
        drawerManager.reloadAdapters(getRealm(), isChecked);
    }
}
