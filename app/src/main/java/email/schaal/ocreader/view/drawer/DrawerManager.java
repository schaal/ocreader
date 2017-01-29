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

package email.schaal.ocreader.view.drawer;

import android.content.Context;
import android.content.SharedPreferences;
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
import email.schaal.ocreader.database.model.AllUnreadFolder;
import email.schaal.ocreader.database.model.Feed;
import email.schaal.ocreader.database.model.Folder;
import email.schaal.ocreader.database.model.FreshFolder;
import email.schaal.ocreader.database.model.StarredFolder;
import email.schaal.ocreader.database.model.TreeItem;
import io.realm.Realm;

/**
 * Manages the drawers displaying feeds and folders.
 */
public class DrawerManager {

    private final State state;

    private final SubscriptionDrawerManager startAdapter;
    private final FolderDrawerManager endAdapter;

    private final AllUnreadFolder allUnreadFolder;
    private final StarredFolder starredFolder;
    private final FreshFolder freshFolder;

    public DrawerManager(Context context, Drawer startDrawer, Drawer endDrawer, boolean onlyUnread, OnCheckedChangeListener onlyUnreadChangeListener) {
        allUnreadFolder = new AllUnreadFolder(context);
        starredFolder = new StarredFolder(context);
        freshFolder = new FreshFolder(context);

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

    public State getState() {
        return state;
    }

    public void setSelectedTreeItem(Realm realm, TreeItem selectedItem, boolean showOnlyUnread) {
        state.setStartDrawerItem(selectedItem);
        state.setEndDrawerItem(null);

        endAdapter.reload(realm, showOnlyUnread);
    }

    public void reset() {
        state.setStartDrawerItem(allUnreadFolder);
        state.setEndDrawerItem(null);
    }

    public void setSelectedFeed(Feed selectedFeed) {
        state.setEndDrawerItem(selectedFeed);
    }

    public void reloadAdapters(Realm realm, boolean showOnlyUnread) {
        startAdapter.reload(realm, showOnlyUnread);
        endAdapter.reload(realm, showOnlyUnread);
    }

    /**
     * Created by daniel on 06.10.15.
     */
    public class SubscriptionDrawerManager extends BaseDrawerManager {
        private final List<IDrawerItem> topDrawerItems = new ArrayList<>(3);

        public SubscriptionDrawerManager(Drawer drawer, boolean onlyUnread, OnCheckedChangeListener onlyUnreadChangeListener) {
            super(drawer);

            topDrawerItems.add(new TreeItemDrawerItem(allUnreadFolder));
            topDrawerItems.add(new TreeItemDrawerItem(starredFolder));
            topDrawerItems.add(new TreeItemDrawerItem(freshFolder));

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
                    int count = item.getCount(realm);
                    if(count > 0 && drawerItem instanceof Badgeable)
                        ((Badgeable) drawerItem).withBadge(String.valueOf(count));
                    if (state.getStartDrawerItem().getId() == item.getId()) {
                        drawerItem.withSetSelected(true);
                        break;
                    }
                }
            }

            final List<Folder> folders = Folder.getAll(realm, showOnlyUnread);
            if(folders != null) {
                for (Folder folder : folders) {
                    drawerItems.add(getDrawerItem(realm, folder));
                }
            }

            for (Feed feed : Queries.getFeedsWithoutFolder(realm, showOnlyUnread)) {
                drawerItems.add(getDrawerItem(realm, feed));
            }

            return drawerItems;
        }

        private IDrawerItem getDrawerItem(Realm realm, TreeItem item) {
            boolean shouldSelect;

            PrimaryDrawerItem drawerItem = new TreeItemDrawerItem(item);

            if (item instanceof Feed) {
                shouldSelect = state.isFeedSelected();
            } else {
                shouldSelect = !state.isFeedSelected();
            }

            shouldSelect = shouldSelect && state.getStartDrawerItem().getId() == item.getId();

            drawerItem.withBadge(item.getCount(realm));

            return drawerItem.withSetSelected(shouldSelect);
        }
    }

    public class FolderDrawerManager extends BaseDrawerManager {
        public FolderDrawerManager(Drawer drawer) {
            super(drawer);
        }

        @Override
        protected List<IDrawerItem> reloadDrawerItems(Realm realm, boolean showOnlyUnread) {
            List<Feed> feeds = state.getStartDrawerItem().getFeeds(realm, showOnlyUnread);
            List<IDrawerItem> drawerItems = new ArrayList<>((feeds != null ? feeds.size() : 0) + 1);

            if (state.isFeedSelected())
                return drawerItems;

            drawerItems.add(new SectionDrawerItem()
                    .withDivider(false)
                    .withName(state.getStartDrawerItem().getName()));

            if (feeds != null) {
                for (Feed feed : feeds) {
                    PrimaryDrawerItem drawerItem = new TreeItemDrawerItem(feed);
                    drawerItem.withIdentifier(feed.getId());
                    drawerItem.withBadge(feed.getUnreadCount());

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
            startDrawerItem = allUnreadFolder;
            startDrawerItemId = AllUnreadFolder.ID;
            endDrawerItemId = null;
            endDrawerItem = null;
        }

        public void saveInstanceState(SharedPreferences preferences) {
            SharedPreferences.Editor editor = preferences.edit();

            editor.putLong(Preferences.SYS_STARTDRAWERITEMID.getKey(), startDrawerItemId);
            editor.putLong(Preferences.SYS_ENDRAWERITEM_ID.getKey(), endDrawerItemId != null ? endDrawerItemId : -1);
            editor.putBoolean(Preferences.SYS_ISFEED.getKey(), isFeedSelected());

            editor.apply();
        }

        public void restoreInstanceState(Realm realm, SharedPreferences preferences) {
            //noinspection ConstantConditions
            startDrawerItemId = Preferences.SYS_STARTDRAWERITEMID.getLong(preferences);
            endDrawerItemId = Preferences.SYS_ENDRAWERITEM_ID.getLong(preferences);

            if(endDrawerItemId != null && endDrawerItemId < 0)
                endDrawerItemId = null;

            boolean isFeed = Preferences.SYS_ISFEED.getBoolean(preferences);

            if(startDrawerItemId == AllUnreadFolder.ID) {
                startDrawerItem = allUnreadFolder;
            } else if(startDrawerItemId == StarredFolder.ID) {
                startDrawerItem = starredFolder;
            } else if (startDrawerItemId == FreshFolder.ID) {
                startDrawerItem = freshFolder;
            } else {
                if (isFeed) {
                    startDrawerItem = Feed.get(realm, startDrawerItemId);
                } else {
                    startDrawerItem = Folder.get(realm, startDrawerItemId);
                }
                if(endDrawerItemId != null) {
                    endDrawerItem = Feed.get(realm, endDrawerItemId);
                }
            }

            if(startDrawerItem == null) {
                startDrawerItem = allUnreadFolder;
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
