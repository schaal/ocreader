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

package email.schaal.cloudreader.view.drawer;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;

import com.mikepenz.materialdrawer.model.DividerDrawerItem;
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem;
import com.mikepenz.materialdrawer.model.SectionDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.Badgeable;
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem;

import java.util.ArrayList;
import java.util.List;

import email.schaal.cloudreader.R;
import email.schaal.cloudreader.database.Queries;
import email.schaal.cloudreader.model.AllUnreadFolder;
import email.schaal.cloudreader.model.Feed;
import email.schaal.cloudreader.model.Folder;
import email.schaal.cloudreader.model.StarredFolder;
import email.schaal.cloudreader.model.TreeIconable;
import email.schaal.cloudreader.model.TreeItem;
import io.realm.Realm;
import io.realm.RealmObject;
import io.realm.RealmResults;

/**
 * Manages the drawers displaying feeds and folders.
 */
public class DrawerManager {
    private static final String TAG = DrawerManager.class.getSimpleName();

    private final Context context;

    public State getState() {
        return state;
    }

    private final State state;

    private final SubscriptionDrawerAdapter startAdapter;
    private final FolderDrawerAdapter endAdapter;

    public DrawerManager(Context context) {
        this.context = context;
        state = new State(context);
        startAdapter = new SubscriptionDrawerAdapter();
        endAdapter = new FolderDrawerAdapter();
    }

    public SubscriptionDrawerAdapter getStartAdapter() {
        return startAdapter;
    }

    public FolderDrawerAdapter getEndAdapter() {
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
    public class SubscriptionDrawerAdapter extends BaseDrawerAdapter {
        private final String TAG = getClass().getCanonicalName();

        private final List<IDrawerItem> topDrawerItems = new ArrayList<>(3);

        public SubscriptionDrawerAdapter() {
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

            topDrawerItems.add(new DividerDrawerItem());
        }

        @Override
        protected ArrayList<IDrawerItem> reloadDrawerItems(Realm realm, boolean showOnlyUnread) {
            ArrayList<IDrawerItem> drawerItems = new ArrayList<>();

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
            if(folders != null)
                for (Folder folder : folders) {
                    drawerItems.add(getDrawerItem(realm, folder));
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

    public class FolderDrawerAdapter extends BaseDrawerAdapter {
        private final String TAG = getClass().getCanonicalName();

        public FolderDrawerAdapter() {
        }

        @Override
        protected ArrayList<IDrawerItem> reloadDrawerItems(Realm realm, boolean showOnlyUnread) {
            ArrayList<IDrawerItem> drawerItems = new ArrayList<>();

            if (state.isFeedSelected())
                return drawerItems;

            SectionDrawerItem sectionHeader = new SectionDrawerItem().withDivider(false);
            drawerItems.add(sectionHeader);
            Iterable<Feed> feeds = Queries.getInstance().getFeedsForTreeItem(realm, state.getStartDrawerItem());

            drawerItems.addAll(getDrawerItems(feeds));

            sectionHeader.withName(state.getStartDrawerItem().getTitle());

            return drawerItems;
        }

        private List<IDrawerItem> getDrawerItems(@Nullable Iterable<Feed> feedItemList) {
            List<IDrawerItem> children = new ArrayList<>();
            if (feedItemList != null) {
                for (Feed feed : feedItemList) {
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
                    children.add(drawerItem);
                }
            }
            return children;
        }
    }

    /**
     * Created by daniel on 20.11.15.
     */
    public class State {
        private static final String BUNDLE_DRAWER_STARTITEM_ID = "BUNDLE_DRAWER_STARTITEM_ID";
        private static final String BUNDLE_DRAWER_IS_FEED = "BUNDLE_DRAWER_IS_FEED";
        private static final String BUNDLE_DRAWER_ENDITEM_ID = "BUNDLE_DRAWER_ENDITEM_ID";

        private TreeItem startDrawerItem;
        private Feed endDrawerItem;
        private Long startDrawerItemId;
        private Long endDrawerItemId;

        public State(Context context) {
            startDrawerItem = new AllUnreadFolder(context);
            startDrawerItemId = null;
            endDrawerItemId = null;
            endDrawerItem = null;
        }

        public Bundle saveInstanceState(Bundle bundle) {
            bundle.putLong(BUNDLE_DRAWER_STARTITEM_ID, startDrawerItem.getId());
            bundle.putBoolean(BUNDLE_DRAWER_IS_FEED, isFeedSelected());
            if(endDrawerItem != null)
                bundle.putLong(BUNDLE_DRAWER_ENDITEM_ID, endDrawerItem.getId());
            return bundle;
        }

        public void restoreInstanceState(Bundle bundle) {
            if(bundle != null) {
                if(bundle.containsKey(BUNDLE_DRAWER_STARTITEM_ID)) {
                    boolean isFeed = bundle.getBoolean(BUNDLE_DRAWER_IS_FEED);
                    startDrawerItemId = bundle.getLong(BUNDLE_DRAWER_STARTITEM_ID);
                    if(startDrawerItemId == AllUnreadFolder.ID) {
                        startDrawerItem = new AllUnreadFolder(context);
                    } else if(startDrawerItemId == StarredFolder.ID) {
                        startDrawerItem = new StarredFolder(context);
                    } else {
                        Realm realm = null;
                        try {
                            realm = Realm.getDefaultInstance();
                            if (isFeed) {
                                startDrawerItem = Queries.getInstance().getFeed(realm, startDrawerItemId);
                            } else {
                                startDrawerItem = Queries.getInstance().getFolder(realm, startDrawerItemId);
                            }
                            if(bundle.containsKey(BUNDLE_DRAWER_ENDITEM_ID)) {
                                endDrawerItemId = bundle.getLong(BUNDLE_DRAWER_ENDITEM_ID);
                                endDrawerItem = Queries.getInstance().getFeed(realm, endDrawerItemId);
                            }
                        } finally {
                            if(realm != null)
                                realm.close();
                        }

                    }
                }
            }
        }

        public TreeItem getStartDrawerItem() {
            return startDrawerItem;
        }

        public void setStartDrawerItem(TreeItem startDrawerItem) {
            this.startDrawerItem = startDrawerItem;
            if (startDrawerItem instanceof RealmObject)
                startDrawerItemId = startDrawerItem.getId();
            else
                startDrawerItemId = null;
        }

        public Feed getEndDrawerItem() {
            return endDrawerItem;
        }

        public void setEndDrawerItem(Feed endDrawerItem) {
            this.endDrawerItem = endDrawerItem;
            if (endDrawerItem != null)
                endDrawerItemId = endDrawerItem.getId();
            else
                endDrawerItemId = null;
        }

        public boolean isFeedSelected() {
            return startDrawerItem instanceof Feed;
        }

        public TreeItem getTreeItem() {
            return endDrawerItem != null ? endDrawerItem : startDrawerItem;
        }
    }
}
