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

package email.schaal.cloudreader.database;

import android.content.Context;
import android.support.annotation.Nullable;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import email.schaal.cloudreader.model.AllUnreadFolder;
import email.schaal.cloudreader.model.ChangedItems;
import email.schaal.cloudreader.model.Feed;
import email.schaal.cloudreader.model.Folder;
import email.schaal.cloudreader.model.Item;
import email.schaal.cloudreader.model.StarredFolder;
import email.schaal.cloudreader.model.TemporaryFeed;
import email.schaal.cloudreader.model.TreeItem;
import email.schaal.cloudreader.util.AlarmUtils;
import io.realm.Realm;
import io.realm.RealmConfiguration;
import io.realm.RealmList;
import io.realm.RealmObject;
import io.realm.RealmQuery;
import io.realm.RealmResults;
import io.realm.Sort;
import io.realm.exceptions.RealmException;

/**
 * Created by daniel on 08.11.15.
 */
public class Queries {
    private final static String TAG = Queries.class.getSimpleName();

    private static Queries instance;

    private final RealmConfiguration realmConfiguration;

    private Queries(Context context) {
        this(new RealmConfiguration.Builder(context)
                .schemaVersion(1)
                .build());
    }

    private Queries(RealmConfiguration realmConfiguration) {
        this.realmConfiguration = realmConfiguration;

        Realm.setDefaultConfiguration(realmConfiguration);

        Realm realm = null;
        try {
            Realm.compactRealm(realmConfiguration);
            realm = Realm.getDefaultInstance();
        } catch (Exception ex) {
            ex.printStackTrace();
            resetDatabase();
        } finally {
            if(realm != null)
                realm.close();
        }
    }

    public static Queries getInstance() {
        if(instance == null)
            throw new IllegalStateException("Initialize first");
        return instance;
    }

    public static void init(Context context) {
        if(instance == null)
            instance = new Queries(context);
    }

    // For instrumentation tests
    public static void init(RealmConfiguration realmConfiguration) {
        instance = new Queries(realmConfiguration);
    }

    public void resetDatabase() {
        Log.w(TAG, "Database will be reset");

        Realm.deleteRealm(realmConfiguration);
        Realm realm = null;
        try {
            realm = Realm.getDefaultInstance();
            realm.executeTransaction(new Realm.Transaction() {
                @Override
                public void execute(Realm realm) {
                    realm.createObject(TemporaryFeed.class);
                    realm.createObject(ChangedItems.class);
                }
            });
        } finally {
            if(realm != null)
                realm.close();
        }
    }

    public Folder getFolder(Realm realm, long id) {
        Folder folder = realm.where(Folder.class).equalTo(Folder.ID, id).findFirst();
        if(folder == null)
            folder = new Folder(id);
        return folder;
    }

    public Feed getFeed(Realm realm, long id) {
        Feed feed = realm.where(Feed.class).equalTo(Feed.ID, id).findFirst();
        if(feed == null)
            feed = new Feed(id);
        return feed;
    }

    /**
     * Get all items belonging to treeItem, sorted by sortFieldname using order
     * @param realm Realm object to query
     * @param treeItem TreeItem to query items from
     * @param sortFieldname Sort using this fieldname
     * @param order Sort using this order
     * @return items belonging to TreeItem
     */
    public RealmResults<Item> getItems(Realm realm, TreeItem treeItem, String sortFieldname, Sort order) {
        RealmQuery<Item> query = null;
        if(treeItem instanceof Feed)
            query = realm.where(Item.class).equalTo(Item.FEED_ID, treeItem.getId());
        else if(treeItem instanceof Folder) {
            // Get all feeds belonging to Folder treeItem
            RealmResults<Feed> feeds = getFeedsForTreeItem(realm, treeItem);
            if(feeds.size() > 0) {
                // Find all items belonging to any feed from this folder
                Iterator<Feed> feedIterator = feeds.iterator();
                query = realm.where(Item.class)
                        .equalTo(Item.FEED_ID, feedIterator.next().getId());
                while (feedIterator.hasNext()) {
                    query.or().equalTo(Item.FEED_ID, feedIterator.next().getId());
                }
            }
        } else if(treeItem instanceof AllUnreadFolder) {
            query = realm.where(Item.class).equalTo(Item.UNREAD, true);
        } else if(treeItem instanceof StarredFolder) {
            query = realm.where(Item.class).equalTo(Item.STARRED, true);
        }

        if (query != null) {
            return query.findAllSorted(sortFieldname, order);
        } else
            return null;
    }

    public RealmResults<Folder> getFolders(Realm realm, boolean onlyUnread) {
        RealmQuery<Folder> query = null;
        if(onlyUnread) {
            RealmResults<Feed> unreadFeeds = realm.where(Feed.class).greaterThan(Feed.UNREAD_COUNT, 0).notEqualTo(Feed.FOLDER_ID, 0).findAll();
            if(unreadFeeds.size() > 0) {
                Iterator<Feed> feedIterator = unreadFeeds.iterator();
                query = realm.where(Folder.class)
                        .equalTo(Folder.ID, feedIterator.next().getFolderId());
                while (feedIterator.hasNext()) {
                    query.or().equalTo(Folder.ID, feedIterator.next().getFolderId());
                }
            }
        } else {
            query = realm.where(Folder.class);
        }

        return query != null ? query.findAllSorted(Folder.TITLE, Sort.ASCENDING) : null;
    }

    public RealmResults<Feed> getFeeds(Realm realm) {
        return realm.where(Feed.class).findAllSorted(Feed.PINNED, Sort.ASCENDING, Feed.TITLE, Sort.ASCENDING);
    }

    public <T extends RealmObject> void insert(Realm realm, final Iterable<T> elements) {
        realm.executeTransaction(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                realm.copyToRealmOrUpdate(elements);
            }
        });
    }

    public <T extends RealmObject> void insert(Realm realm, final T element) {
        realm.executeTransaction(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                realm.copyToRealmOrUpdate(element);
            }
        });
    }

    public <T extends RealmObject & TreeItem> void deleteAndInsert(Realm realm, final Class<T> clazz, final List<T> elements) {
        // Sort elements for binary search
        Collections.sort(elements, TreeItem.COMPARATOR);

        realm.executeTransaction(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                realm.copyToRealmOrUpdate(elements);

                RealmResults<T> results = realm.allObjects(clazz);

                List<T> itemsToRemove = new ArrayList<>();

                // iterate through items in database and add items not in elements to itemsToRemove
                for (T result : results) {
                    final int found = Collections.binarySearch(elements, result, TreeItem.COMPARATOR);
                    if (found < 0)
                        itemsToRemove.add(result);
                }

                for(T toRemove: itemsToRemove) {
                    if(clazz == Feed.class) {
                        // Also remove items belonging to feed being removed from database
                        realm.where(Item.class).equalTo(Item.FEED_ID, toRemove.getId()).findAll().clear();
                    }
                    toRemove.removeFromRealm();
                }
            }
        });
    }

    public RealmResults<Feed> getFeedsWithoutFolder(Realm realm, boolean onlyUnread) {
        RealmQuery<Feed> query = realm.where(Feed.class).equalTo(Feed.FOLDER_ID, 0);
        if(onlyUnread) {
            query.greaterThan(Feed.UNREAD_COUNT, 0);
        }
        return query.findAllSorted(Feed.TITLE, Sort.ASCENDING);
    }

    public int getCount(Realm realm, TreeItem item) {
        int count = 0;
        if(item instanceof AllUnreadFolder) {
            count = realm.where(Feed.class).sum(Feed.UNREAD_COUNT).intValue();
        } else if (item instanceof StarredFolder) {
            count = (int) realm.where(Item.class).equalTo(Item.STARRED, true).count();
        } else if (item instanceof Folder) {
            count = realm.where(Feed.class).equalTo(Feed.FOLDER_ID, item.getId()).sum(Feed.UNREAD_COUNT).intValue();
        } else if(item instanceof Feed) {
            count = ((Feed)item).getUnreadCount();
        }
        return count;
    }

    public RealmResults<Feed> getFeedsForTreeItem(Realm realm, TreeItem item) {
        RealmResults<Feed> feeds = null;
        if(item instanceof AllUnreadFolder) {
            feeds = realm.where(Feed.class).greaterThan(Feed.UNREAD_COUNT, 0).findAllSorted(Feed.TITLE, Sort.ASCENDING);
        } else if(item instanceof StarredFolder) {
            feeds = realm.where(Feed.class).greaterThan(Feed.STARRED_COUNT, 0).findAllSorted(Feed.TITLE, Sort.ASCENDING);
        } else if(item instanceof Folder) {
            feeds = realm.where(Feed.class).equalTo(Feed.FOLDER_ID, item.getId()).findAllSorted(Feed.TITLE, Sort.ASCENDING);
        }
        return feeds;
    }

    public void removeExcessItems(Realm realm, final int max_items) {
        final RealmResults<Item> expendableItems = realm.where(Item.class)
                .equalTo(Item.UNREAD, false)
                .equalTo(Item.STARRED, false)
                .findAllSorted(Item.LAST_MODIFIED, Sort.ASCENDING);
        realm.executeTransaction(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                int itemsToDelete = expendableItems.size() - max_items;
                Log.d(TAG, "no. of excess items: " + itemsToDelete);
                for (int i = 0; i < itemsToDelete; i++) {
                    expendableItems.remove(0);
                }
            }
        });
    }

    public void markTemporaryFeedAsRead(Realm realm, Realm.Transaction.Callback callback) {
        realm.executeTransaction(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                ChangedItems changedItems = null;
                try {
                    changedItems = realm.where(ChangedItems.class).findFirst();
                    RealmList<Item> unreadChangedItems = changedItems.getUnreadChangedItems();

                    TemporaryFeed temporaryFeed = realm.where(TemporaryFeed.class).findFirst();

                    // TODO: 27.12.15 Find a way to only iterate over unread items
                    List<Item> unreadItems = temporaryFeed.getItems();

                    Set<Feed> feeds = new HashSet<>();

                    for (int i = 0, unreadItemsSize = unreadItems.size(); i < unreadItemsSize; i++) {
                        Item item = unreadItems.get(i);
                        if (item.isUnread()) {
                            item.setUnread(false);
                            addToChangedList(unreadChangedItems, item);
                            feeds.add(Item.feed(item));
                        }
                    }
                    for (Feed feed : feeds) {
                        feed.setUnreadCount((int) realm.where(Item.class)
                                        .equalTo(Item.FEED_ID, feed.getId())
                                        .equalTo(Item.UNREAD, true).count()
                        );
                    }
                } finally {
                    checkAlarm(changedItems);
                }
            }
        }, callback);
    }

    public void setItemsUnread(Realm realm, final boolean newUnread, @Nullable final Realm.Transaction.Callback transactionCallback, final Item... items) {
        realm.executeTransaction(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                ChangedItems changedItems = realm.where(ChangedItems.class).findFirst();
                RealmList<Item> unreadChangedItems = changedItems.getUnreadChangedItems();

                try {
                    for (Item item : items) {
                        if (item.isUnread() != newUnread) {
                            item.setUnread(newUnread);

                            addToChangedList(unreadChangedItems, item);

                            Feed feed = Item.feed(item);
                            feed.setUnreadCount(feed.getUnreadCount() + (newUnread ? 1 : -1));
                        }
                    }

                    if (transactionCallback != null) {
                        transactionCallback.onSuccess();
                    }
                } catch (RealmException e) {
                    e.printStackTrace();
                    if (transactionCallback != null) {
                        transactionCallback.onError(e);
                    }
                } finally {
                    checkAlarm(changedItems);
                }
            }
        });
    }

    public void setItemStarred(Realm realm, final boolean newStarred, @Nullable final Realm.Transaction.Callback transactionCallback, final Item item) {
        realm.executeTransaction(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                ChangedItems changedItems = null;
                try {
                    if(item.isStarred() != newStarred) {
                        changedItems = realm.where(ChangedItems.class).findFirst();
                        RealmList<Item> starredChangedItems = changedItems.getStarredChangedItems();

                        item.setStarred(newStarred);

                        addToChangedList(starredChangedItems, item);

                        Feed feed = Item.feed(item);
                        feed.setStarredCount(feed.getStarredCount() + (newStarred ? 1 : -1));
                    }

                    if (transactionCallback != null) {
                        transactionCallback.onSuccess();
                    }
                } catch (RealmException e) {
                    e.printStackTrace();
                    if (transactionCallback != null) {
                        transactionCallback.onError(e);
                    }
                } finally {
                    checkAlarm(changedItems);
                }
            }
        });
    }

    private void checkAlarm(@Nullable ChangedItems changedItems) {
        if(changedItems != null) {
            boolean alarmNeeded = !changedItems.getStarredChangedItems().isEmpty() || !changedItems.getUnreadChangedItems().isEmpty();
            if (alarmNeeded)
                AlarmUtils.getInstance().setAlarm();
            else
                AlarmUtils.getInstance().cancelAlarm();
        }
    }

    private void addToChangedList(RealmList<Item> changedItems, Item item) {
        if (changedItems.contains(item))
            changedItems.remove(item);
        else
            changedItems.add(item);
    }
}
