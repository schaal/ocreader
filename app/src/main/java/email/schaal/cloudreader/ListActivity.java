/*
 * Copyright (C) 2015 Daniel Schaal <daniel@schaal.email>
 *
 * This file is part of Cloudreader.
 *
 * Cloudreader is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Cloudreader is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Cloudreader.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package email.schaal.cloudreader;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v4.widget.ViewDragHelper;
import android.support.v7.widget.Toolbar;
import android.util.Base64;
import android.util.Base64InputStream;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.mikepenz.materialdrawer.AccountHeader;
import com.mikepenz.materialdrawer.AccountHeaderBuilder;
import com.mikepenz.materialdrawer.Drawer;
import com.mikepenz.materialdrawer.DrawerBuilder;
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem;
import com.mikepenz.materialdrawer.model.ProfileDrawerItem;
import com.mikepenz.materialdrawer.model.ProfileSettingDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IProfile;

import java.io.ByteArrayInputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;

import email.schaal.cloudreader.database.Queries;
import email.schaal.cloudreader.model.Feed;
import email.schaal.cloudreader.model.Item;
import email.schaal.cloudreader.model.TemporaryFeed;
import email.schaal.cloudreader.model.TreeItem;
import email.schaal.cloudreader.model.User;
import email.schaal.cloudreader.service.SyncService;
import email.schaal.cloudreader.view.ItemViewHolder;
import email.schaal.cloudreader.view.drawer.DrawerManager;
import io.realm.RealmResults;

public class ListActivity extends RealmActivity implements ItemViewHolder.OnClickListener, SwipeRefreshLayout.OnRefreshListener {
    private static final String TAG = ListActivity.class.getSimpleName();

    private static final int REFRESH_DRAWER_ITEM_ID = 999;

    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getAction().equals(SyncService.SYNC_STARTED) || intent.getAction().equals(SyncService.SYNC_FINISHED))
                updateSyncStatus();
        }
    };

    private void updateSyncStatus() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(ListActivity.this);

        boolean needsUpdate = Preferences.SYS_NEEDS_UPDATE_AFTER_SYNC.getBoolean(sharedPreferences);
        boolean syncRunning = Preferences.SYS_SYNC_RUNNING.getBoolean(sharedPreferences);

        Log.d(TAG, "needs update/sync running: " + needsUpdate + "/" + syncRunning);

        if(needsUpdate) {
            drawerManager.reloadStartAdapter();

            getListFragment().update(true);

            updateUserProfile();

            sharedPreferences.edit()
                    .putBoolean(Preferences.SYS_NEEDS_UPDATE_AFTER_SYNC.getKey(), false).apply();
        }

        if (syncMenuItem != null) {
            syncMenuItem.setEnabled(!syncRunning);
        }

        if (refreshDrawerItem != null) {
            refreshDrawerItem.withEnabled(!syncRunning);
            startDrawer.updateStickyFooterItem(refreshDrawerItem);
        }

        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setRefreshing(syncRunning);
        }
    }

    private MenuItem syncMenuItem;
    private Drawer startDrawer;
    private Drawer endDrawer;
    private DrawerManager drawerManager;
    private ProfileDrawerItem profileDrawerItem;
    private PrimaryDrawerItem refreshDrawerItem;
    private AccountHeader accountHeader;

    private SwipeRefreshLayout swipeRefreshLayout;
    private FloatingActionButton fab_mark_all_read;

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
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);

        swipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipeRefreshLayout);
        swipeRefreshLayout.setColorSchemeResources(R.color.primary);
        swipeRefreshLayout.setOnRefreshListener(this);

        drawerManager = new DrawerManager(this);
        drawerManager.getState().restoreInstanceState(savedInstanceState);

        fab_mark_all_read = (FloatingActionButton) findViewById(R.id.fab_mark_all_read);
        fab_mark_all_read.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TemporaryFeed temporaryFeed = getRealm().where(TemporaryFeed.class).findFirst();
                final RealmResults<Item> items = temporaryFeed.getItems().where().equalTo(Item.UNREAD, true).findAll();
                Queries.getInstance().setItemsUnreadState(getRealm(), false, null, items.toArray(new Item[items.size()]));
                getListFragment().update(false);
            }
        });

        profileDrawerItem = new ProfileDrawerItem()
                .withName(preferences.getString(Preferences.USERNAME.getKey(), getString(R.string.app_name)))
                .withEmail(Preferences.URL.getString(preferences));

        updateUserProfile();

        IProfile profileSettingsItem = new ProfileSettingDrawerItem()
                .withName(getString(R.string.action_sign_in))
                .withIconTinted(true)
                .withIcon(R.drawable.ic_settings);

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
                        if (profile instanceof ProfileSettingDrawerItem) {
                            Intent loginIntent = new Intent(ListActivity.this, LoginActivity.class);
                            startActivityForResult(loginIntent, LoginActivity.REQUEST_CODE);
                        }
                        return false;
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
                        drawerManager.getStartAdapter().updateUnreadCount();
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
                .withSavedInstance(savedInstanceState)
                .withAdapter(drawerManager.getStartAdapter());

        DrawerBuilder endDrawerBuilder = new DrawerBuilder()
                .withActivity(this)
                .withDrawerGravity(Gravity.END)
                .withSavedInstance(savedInstanceState)
                .withShowDrawerOnFirstLaunch(true)
                .withAdapter(drawerManager.getEndAdapter())
                .withOnDrawerListener(new Drawer.OnDrawerListener() {
                    @Override
                    public void onDrawerOpened(View drawerView) {
                        drawerManager.getEndAdapter().updateUnreadCount();
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
        endDrawer = endDrawerBuilder.append(startDrawer);

        try {
            // increase the size of the drag margin for opening the drawers.
            DrawerLayout drawerLayout = startDrawer.getDrawerLayout();

            Field mLeftDragger = drawerLayout.getClass()
                    .getDeclaredField("mLeftDragger");
            Field mRightDragger = drawerLayout.getClass()
                    .getDeclaredField("mRightDragger");

            setDraggerEdgeSize(mLeftDragger, drawerLayout);
            setDraggerEdgeSize(mRightDragger, drawerLayout);
        } catch (Exception e) {
            e.printStackTrace();
        }

        drawerManager.reloadStartAdapter();
        drawerManager.reloadEndAdapter();

        //noinspection ConstantConditions
        getSupportActionBar().setTitle(drawerManager.getState().getTreeItem().getTitle());
    }

    private static void setDraggerEdgeSize(Field mDragger, DrawerLayout drawerLayout) throws IllegalAccessException, NoSuchFieldException {
        mDragger.setAccessible(true);
        ViewDragHelper draggerObj = (ViewDragHelper) mDragger.get(drawerLayout);

        Field mEdgeSize = draggerObj.getClass().getDeclaredField("mEdgeSize");
        mEdgeSize.setAccessible(true);
        int edge = mEdgeSize.getInt(draggerObj);

        mEdgeSize.setInt(draggerObj, edge * 2);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        drawerManager.getState().saveInstanceState(outState);
    }

    private void onStartDrawerItemClicked(TreeItem item) {
        drawerManager.setSelectedTreeItem(item);
        reloadListFragment(item);
    }

    private void onEndDrawerItemClicked(Feed feed) {
        drawerManager.setSelectedFeed(feed);
        reloadListFragment(feed);
    }

    private void reloadListFragment(TreeItem item) {
        //noinspection ConstantConditions
        getSupportActionBar().setTitle(item.getTitle());
        getListFragment().setItem(item);
    }

    private ListFragment getListFragment() {
        return (ListFragment) getFragmentManager().findFragmentById(R.id.fragment_itemlist);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == LoginActivity.REQUEST_CODE) {
            if(resultCode == LoginActivity.RESULT_OK) {
                SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
                profileDrawerItem.withName(Preferences.USERNAME.getString(preferences));
                profileDrawerItem.withEmail(Preferences.URL.getString(preferences));

                SyncService.startSync(this);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_list, menu);
        syncMenuItem = menu.findItem(R.id.action_sync);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        switch (id) {
            case R.id.action_settings:
                return true;
            case R.id.action_sync:
                SyncService.startSync(this);
                break;
        }

        return super.onOptionsItemSelected(item);
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
            }
            if(accountHeader != null)
                accountHeader.updateProfile(profileDrawerItem);
        }
    }
}
