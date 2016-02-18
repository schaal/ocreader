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

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v4.widget.ViewDragHelper;
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

import java.io.ByteArrayInputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;

import email.schaal.ocreader.database.Queries;
import email.schaal.ocreader.model.AllUnreadFolder;
import email.schaal.ocreader.model.Feed;
import email.schaal.ocreader.model.Item;
import email.schaal.ocreader.model.TemporaryFeed;
import email.schaal.ocreader.model.TreeItem;
import email.schaal.ocreader.model.User;
import email.schaal.ocreader.service.SyncService;
import email.schaal.ocreader.view.ItemViewHolder;
import email.schaal.ocreader.view.ItemsAdapter;
import email.schaal.ocreader.view.drawer.DrawerManager;
import io.realm.Realm;

public class ListActivity extends RealmActivity implements ItemViewHolder.OnClickListener, SwipeRefreshLayout.OnRefreshListener, ItemsAdapter.OnLoadMoreListener, OnCheckedChangeListener {
    private static final String TAG = ListActivity.class.getSimpleName();

    private static final int REFRESH_DRAWER_ITEM_ID = 999;

    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getAction().equals(SyncService.SYNC_STARTED) || intent.getAction().equals(SyncService.SYNC_FINISHED)) {
                switch (intent.getStringExtra(SyncService.EXTRA_TYPE)) {
                    case SyncService.ACTION_LOAD_MORE:
                        if (intent.getAction().equals(SyncService.SYNC_FINISHED)) {
                            getAdapter().updateItems(true);
                            getAdapter().resetLoadMore();
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

            getAdapter().updateItems(true);

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
            getAdapter().resetLoadMore();
    }

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

        fab_mark_all_read = (FloatingActionButton) findViewById(R.id.fab_mark_all_read);
        fab_mark_all_read.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                v.setEnabled(false);
                Queries.getInstance().markTemporaryFeedAsRead(getRealm(), new Realm.Transaction.Callback() {
                    @Override
                    public void onError(Exception e) {
                        e.printStackTrace();
                        onSuccess();
                    }

                    @Override
                    public void onSuccess() {
                        getAdapter().updateItems(false);
                        v.setEnabled(true);
                    }
                });
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
        endDrawer = endDrawerBuilder.append(startDrawer);

        drawerManager = new DrawerManager(this, startDrawer, endDrawer, isShowOnlyUnread(), this);
        drawerManager.getState().restoreInstanceState(getRealm());
        getAdapter().setTreeItem(drawerManager.getState().getTreeItem(), drawerManager.getState().getStartDrawerItem() instanceof AllUnreadFolder, false);

        try {
            // increase the size of the drag margin for opening the drawers.
            DrawerLayout drawerLayout = startDrawer.getDrawerLayout();

            setDraggerEdgeSize(drawerLayout.getClass()
                    .getDeclaredField("mLeftDragger"), drawerLayout);
            setDraggerEdgeSize(drawerLayout.getClass()
                    .getDeclaredField("mRightDragger"), drawerLayout);
        } catch (Exception e) {
            e.printStackTrace();
        }

        drawerManager.reloadAdapters(getRealm(), isShowOnlyUnread());

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

    private boolean isShowOnlyUnread() {
        return Preferences.SHOW_ONLY_UNREAD.getBoolean(PreferenceManager.getDefaultSharedPreferences(this));
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        drawerManager.getState().saveInstanceState();
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
        getAdapter().setTreeItem(item, drawerManager.getState().getStartDrawerItem() instanceof AllUnreadFolder);
        fab_mark_all_read.show();
    }

    private ItemsAdapter getAdapter() {
        return ((ListFragment) getFragmentManager().findFragmentById(R.id.fragment_itemlist)).getAdapter();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == LoginActivity.REQUEST_CODE) {
            if(resultCode == LoginActivity.RESULT_OK) {
                SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
                profileDrawerItem.withName(Preferences.USERNAME.getString(preferences));
                profileDrawerItem.withEmail(Preferences.URL.getString(preferences));

                Queries.getInstance().resetDatabase();
                SyncService.startSync(this, true);
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
