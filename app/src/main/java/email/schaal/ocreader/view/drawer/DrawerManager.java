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

package email.schaal.ocreader.view.drawer;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;

import com.mikepenz.materialdrawer.Drawer;
import com.mikepenz.materialdrawer.interfaces.OnCheckedChangeListener;
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem;
import com.mikepenz.materialdrawer.model.SecondarySwitchDrawerItem;
import com.mikepenz.materialdrawer.model.SectionDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.Badgeable;
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem;

import java.util.ArrayList;
import java.util.List;

import email.schaal.ocreader.Preferences;
import email.schaal.ocreader.R;
import email.schaal.ocreader.database.Queries;
import email.schaal.ocreader.model.AllUnreadFolder;
import email.schaal.ocreader.model.Feed;
import email.schaal.ocreader.model.Folder;
import email.schaal.ocreader.model.StarredFolder;
import email.schaal.ocreader.model.TreeIconable;
import email.schaal.ocreader.model.TreeItem;
import io.realm.Realm;
import io.realm.RealmResults;

/**
 * Manages the drawers displaying feeds and folders.
 */
public class DrawerManager {
    private static final String TAG = DrawerManager.class.getName();

    private final Context context;

    public State getState() {
        return state;
    }

    private final State state;

    private final SubscriptionDrawerManager startAdapter;
    private final FolderDrawerManager endAdapter;

    public DrawerManager(Context context, Drawer startDrawer, Drawer endDrawer, boolean onlyUnread, OnCheckedChangeListener onlyUnreadChangeListener) {
        this.context = context;
        state = new State();
        startAdapter = new SubscriptionDrawerManager(startDrawer, onlyUnread, onlyUnreadChangeListener);
        endAdapter = new FolderDrawerManager(endDrawer);
    }

    public SubscriptionDrawerManager getStartAdapter() {
        return startAdapter;
    }

    public FolderDrawerManager getEndAdapter() {
        return endAdapter;
    }

    public void reloadStartAdapter(Realm realm, boolean showOnlyUnread) {
        startAdapter.reload(realm, showOnlyUnread);
    }

    public void reloadEndAdapter(Realm realm, boolean showOnlyUnread) {
        endAdapter.reload(realm, showOnlyUnread);
    }

    public void setSelectedTreeItem(Realm realm, TreeItem selectedItem, boolean showOnlyUnread) {
        state.setStartDrawerItem(selectedItem);
        state.setEndDrawerItem(null);

        endAdapter.reload(realm, showOnlyUnread);
    }

    public void setSelectedFeed(Feed selectedFeed) {
        state.setEndDrawerItem(selectedFeed);
    }

    public void reloadAdapters(Realm realm, boolean showOnlyUnread) {
        reloadStartAdapter(realm, showOnlyUnread);
        reloadEndAdapter(realm, showOnlyUnread);
    }

    /**
     * Created by daniel on 06.10.15.
     */
    public class SubscriptionDrawerManager extends BaseDrawerManager {
        private final List<IDrawerItem> topDrawerItems = new ArrayList<>(3);

        public SubscriptionDrawerManager(Drawer drawer, boolean onlyUnread, OnCheckedChangeListener onlyUnreadChangeListener) {
            super(drawer);
            AllUnreadFolder unreadFolder = new AllUnreadFolder(context);
            StarredFolder starredFolder = new StarredFolder(context);

            topDrawerItems.add(new PrimaryDrawerItem()
                            .withName(unreadFolder.getTitle())
                            .withIcon(unreadFolder.getIcon())
                            .withIconTintingEnabled(true)
                            .withTag(unreadFolder)
            );

            topDrawerItems.add(new PrimaryDrawerItem()
                    .withName(starredFolder.getTitle())
                    .withIcon(starredFolder.getIcon())
                    .withIconTintingEnabled(true)
                    .withTag(starredFolder)
            );

            topDrawerItems.add(new SecondarySwitchDrawerItem()
                    .withName(R.string.only_unread)
                    .withSelectable(false)
                    .withOnCheckedChangeListener(onlyUnreadChangeListener)
                    .withChecked(onlyUnread)
            );
        }

        @Override
        protected List<IDrawerItem> reloadDrawerItems(Realm realm, boolean showOnlyUnread) {
            List<IDrawerItem> drawerItems = new ArrayList<>();

            drawerItems.addAll(topDrawerItems);

            for(IDrawerItem drawerItem: topDrawerItems) {
                if(drawerItem.getTag() instanceof TreeItem) {
                    TreeItem item = (TreeItem) drawerItem.getTag();
                    int count = Queries.getInstance().getCount(realm, item);
                    if(count > 0 && drawerItem instanceof Badgeable)
                        ((Badgeable) drawerItem).withBadge(String.valueOf(count));
                    if (state.getStartDrawerItem().getId() == item.getId()) {
                        drawerItem.withSetSelected(true);
                        break;
                    }
                }
            }

            final RealmResults<Folder> folders = Queries.getInstance().getFolders(realm, showOnlyUnread);
            if(folders != null) {
                for (Folder folder : folders) {
                    drawerItems.add(getDrawerItem(realm, folder));
                }
            }

            for (Feed feed : Queries.getInstance().getFeedsWithoutFolder(realm, showOnlyUnread)) {
                drawerItems.add(getDrawerItem(realm, feed));
            }

            return drawerItems;
        }

        private IDrawerItem getDrawerItem(Realm realm, TreeItem item) {
            boolean shouldSelect;

            UrlPrimaryDrawerItem drawerItem = (UrlPrimaryDrawerItem) new UrlPrimaryDrawerItem()
                    .withName(item.getTitle())
                    .withIconTintingEnabled(true)
                    .withTag(item);

            int count = Queries.getInstance().getCount(realm, item);

            if (item instanceof Feed) {
                shouldSelect = state.isFeedSelected();
                String favIcon = ((Feed) item).getFaviconLink();

                if (favIcon != null)
                    drawerItem.withIcon(favIcon);
                else
                    drawerItem.withIcon(R.drawable.ic_feed_icon);
            } else {
                shouldSelect = !state.isFeedSelected();

                if (item instanceof TreeIconable)
                    drawerItem.withIcon(((TreeIconable) item).getIcon());
                else
                    drawerItem.withIcon(R.drawable.ic_folder);
            }

            shouldSelect = shouldSelect && state.getStartDrawerItem().getId() == item.getId();

            if (count > 0) {
                drawerItem.withBadge(String.valueOf(count));
            }

            return drawerItem.withSetSelected(shouldSelect);
        }
    }

    public class FolderDrawerManager extends BaseDrawerManager {
        public FolderDrawerManager(Drawer drawer) {
            super(drawer);
        }

        @Override
        protected List<IDrawerItem> reloadDrawerItems(Realm realm, boolean showOnlyUnread) {
            List<Feed> feeds = Queries.getInstance().getFeedsForTreeItem(realm, state.getStartDrawerItem());
            List<IDrawerItem> drawerItems = new ArrayList<>((feeds != null ? feeds.size() : 0) + 1);

            if (state.isFeedSelected())
                return drawerItems;

            drawerItems.add(new SectionDrawerItem()
                    .withDivider(false)
                    .withName(state.getStartDrawerItem().getTitle()));

            if (feeds != null) {
                for (Feed feed : feeds) {
                    UrlPrimaryDrawerItem drawerItem = new UrlPrimaryDrawerItem();
                    drawerItem.withTag(feed)
                            .withName(feed.getTitle())
                            .withIconTintingEnabled(true)
                            .withIdentifier((int) feed.getId());
                    if(feed.getUnreadCount() > 0)
                        drawerItem.withBadge(String.valueOf(feed.getUnreadCount()));
                    if (feed.getFaviconLink() != null)
                        drawerItem.withIcon(feed.getFaviconLink());
                    else
                        drawerItem.withIcon(R.drawable.ic_feed_icon);
                    drawerItem.withSetSelected(state.getEndDrawerItem() != null && state.getEndDrawerItem().getId() == feed.getId());
                    drawerItems.add(drawerItem);
                }
            }

            return drawerItems;
        }

    }

    public class State {
        private TreeItem startDrawerItem;
        private long startDrawerItemId;

        @Nullable private Feed endDrawerItem;
        @Nullable private Long endDrawerItemId;

        public State() {
            startDrawerItem = new AllUnreadFolder(context);
            startDrawerItemId = AllUnreadFolder.ID;
            endDrawerItemId = null;
            endDrawerItem = null;
        }

        public void saveInstanceState() {
            SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();

            editor.putLong(Preferences.SYS_STARTDRAWERITEMID.getKey(), startDrawerItemId);
            editor.putLong(Preferences.SYS_ENDRAWERITEM_ID.getKey(), endDrawerItemId != null ? endDrawerItemId : -1);
            editor.putBoolean(Preferences.SYS_ISFEED.getKey(), isFeedSelected());

            editor.apply();
        }

        public void restoreInstanceState(Realm realm) {
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);

            //noinspection ConstantConditions
            startDrawerItemId = Preferences.SYS_STARTDRAWERITEMID.getLong(preferences);
            endDrawerItemId = Preferences.SYS_ENDRAWERITEM_ID.getLong(preferences);

            if(endDrawerItemId != null && endDrawerItemId < 0)
                endDrawerItemId = null;

            boolean isFeed = Preferences.SYS_ISFEED.getBoolean(preferences);

            if(startDrawerItemId == AllUnreadFolder.ID) {
                startDrawerItem = new AllUnreadFolder(context);
            } else if(startDrawerItemId == StarredFolder.ID) {
                startDrawerItem = new StarredFolder(context);
            } else {
                if (isFeed) {
                    startDrawerItem = Queries.getInstance().getFeed(realm, startDrawerItemId);
                } else {
                    startDrawerItem = Queries.getInstance().getFolder(realm, startDrawerItemId);
                }
                if(endDrawerItemId != null) {
                    endDrawerItem = Queries.getInstance().getFeed(realm, endDrawerItemId);
                }
            }

            if(startDrawerItem == null) {
                startDrawerItem = new AllUnreadFolder(context);
                startDrawerItemId = AllUnreadFolder.ID;
            }
        }

        public TreeItem getStartDrawerItem() {
            return startDrawerItem;
        }

        public void setStartDrawerItem(TreeItem startDrawerItem) {
            this.startDrawerItem = startDrawerItem;
            startDrawerItemId = startDrawerItem.getId();
        }

        @Nullable
        public Feed getEndDrawerItem() {
            return endDrawerItem;
        }

        public void setEndDrawerItem(@Nullable Feed endDrawerItem) {
            this.endDrawerItem = endDrawerItem;
            endDrawerItemId = endDrawerItem != null ? endDrawerItem.getId() : null;
        }

        public boolean isFeedSelected() {
            return startDrawerItem instanceof Feed;
        }

        public TreeItem getTreeItem() {
            return endDrawerItem != null ? endDrawerItem : startDrawerItem;
        }
    }
}
