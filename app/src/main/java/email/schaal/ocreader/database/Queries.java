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

package email.schaal.ocreader.database;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import email.schaal.ocreader.model.AllUnreadFolder;
import email.schaal.ocreader.model.Feed;
import email.schaal.ocreader.model.Folder;
import email.schaal.ocreader.model.Insertable;
import email.schaal.ocreader.model.Item;
import email.schaal.ocreader.model.StarredFolder;
import email.schaal.ocreader.model.TemporaryFeed;
import email.schaal.ocreader.model.TreeItem;
import email.schaal.ocreader.util.AlarmUtils;
import io.realm.Realm;
import io.realm.RealmConfiguration;
import io.realm.RealmMigration;
import io.realm.RealmObject;
import io.realm.RealmQuery;
import io.realm.RealmResults;
import io.realm.Sort;
import io.realm.exceptions.RealmException;

/**
 * Utility class containing some commonly used Queries for the Realm database.
 */
public class Queries {
    private final static String TAG = Queries.class.getName();

    public final static int SCHEMA_VERSION = 9;

    private final static Realm.Transaction initialData = new Realm.Transaction() {
        @Override
        public void execute(Realm realm) {
            realm.deleteAll();
            realm.createObject(TemporaryFeed.class);
        }
    };

    private final static RealmMigration migration = new DatabaseMigration();

    public static void closeRealm(@Nullable Realm realm) {
        if(realm != null) {
            realm.close();
        }
    }

    public static void init(RealmConfiguration.Builder builder) {
        RealmConfiguration realmConfiguration = builder
                .schemaVersion(SCHEMA_VERSION)
                .deleteRealmIfMigrationNeeded()
                .initialData(initialData)
                .build();
        Realm.setDefaultConfiguration(realmConfiguration);

        Realm realm = null;
        try {
            Realm.compactRealm(realmConfiguration);
            realm = Realm.getDefaultInstance();
            if(realm.isEmpty())
                realm.executeTransaction(initialData);
        } catch (Exception ex) {
            ex.printStackTrace();
            closeRealm(realm);
            Realm.deleteRealm(realmConfiguration);
        } finally {
            closeRealm(realm);
        }
    }

    public static void resetDatabase() {
        Log.w(TAG, "Database will be reset");

        Realm realm = null;
        try {
            realm = Realm.getDefaultInstance();
            realm.executeTransaction(initialData);
        } finally {
            closeRealm(realm);
        }
    }

    @Nullable
    public static Folder getFolder(Realm realm, long id) {
        return realm.where(Folder.class).equalTo(Folder.ID, id).findFirst();
    }

    @Nullable
    public static Feed getFeed(Realm realm, long id) {
        return realm.where(Feed.class).equalTo(Feed.ID, id).findFirst();
    }

    /**
     * Get all items belonging to treeItem, sorted by sortFieldname using order
     * @param realm Realm object to query
     * @param treeItem TreeItem to query items from
     * @param onlyUnread Return only unread items?
     * @return items belonging to TreeItem, only unread if onlyUnread is true
     */
    @Nullable
    public static RealmResults<Item> getItems(Realm realm, TreeItem treeItem, boolean onlyUnread) {
        RealmQuery<Item> query = null;
        // Whether to return only items with a distinct fingerprint
        boolean distinct = false;

        if(treeItem instanceof Feed)
            query = realm.where(Item.class).equalTo(Item.FEED_ID, treeItem.getId());
        else if(treeItem instanceof Folder) {
            distinct = true;
            // Get all feeds belonging to Folder treeItem
            RealmResults<Feed> feeds = getFeedsForTreeItem(realm, treeItem);
            if(feeds != null && feeds.size() > 0) {
                // Find all items belonging to any feed from this folder
                Iterator<Feed> feedIterator = feeds.iterator();
                query = realm.where(Item.class)
                        .equalTo(Item.FEED_ID, feedIterator.next().getId());
                while (feedIterator.hasNext()) {
                    query.or().equalTo(Item.FEED_ID, feedIterator.next().getId());
                }
            }
        } else if(treeItem instanceof AllUnreadFolder) {
            distinct = true;
            query = realm.where(Item.class).equalTo(Item.UNREAD, true);
            // prevent onlyUnread from adding the same condition again
            onlyUnread = false;
        } else if(treeItem instanceof StarredFolder) {
            query = realm.where(Item.class).equalTo(Item.STARRED, true);
        }

        if (query != null) {
            if(onlyUnread)
                query.equalTo(Item.UNREAD, true);

            if(distinct) {
                return query.distinct(Item.FINGERPRINT);
            } else {
                return query.findAll();
            }
        } else
            return null;
    }

    @Nullable
    public static RealmResults<Folder> getFolders(Realm realm, boolean onlyUnread) {
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

        return query != null ? query.findAllSorted(Folder.NAME, Sort.ASCENDING) : null;
    }

    @NonNull
    public static RealmResults<Feed> getFeeds(Realm realm) {
        return realm.where(Feed.class).findAllSorted(Feed.PINNED, Sort.ASCENDING, Feed.NAME, Sort.ASCENDING);
    }

    public static void insert(Realm realm, @Nullable final Insertable element) {
        realm.executeTransaction(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                if(element != null)
                    element.insert(realm);
            }
        });
    }

    public static void insert(Realm realm, final Iterable<? extends Insertable> elements) {
        realm.executeTransaction(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                for(final Insertable element: elements) {
                    element.insert(realm);
                }
            }
        });
    }

    public static <T extends RealmObject & TreeItem> void deleteAndInsert(Realm realm, final Class<T> clazz, final List<T> elements) {
        Collections.sort(elements, TreeItem.COMPARATOR);

        realm.executeTransaction(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                final RealmResults<T> databaseItems = realm.where(clazz).findAllSorted(TreeItem.ID, Sort.ASCENDING);

                final Iterator<T> databaseIterator = databaseItems.iterator();

                for (T element : elements) {
                    T currentDatabaseItem;

                    // The lists are sorted by id, so if currentDatabaseItem.getId() < element.getId() we can remove it from the database
                    while (databaseIterator.hasNext() && (currentDatabaseItem = databaseIterator.next()).getId() < element.getId()) {
                        deleteTreeItem(realm, currentDatabaseItem);
                    }

                    realm.insertOrUpdate(element);
                }

                // Remove remaining items from the database
                while (databaseIterator.hasNext()) {
                    deleteTreeItem(realm, databaseIterator.next());
                }
            }
        });
    }

    private static <T extends RealmObject & TreeItem> void deleteTreeItem(Realm realm, T item) {
        if (item instanceof Feed) {
            // Also remove items belonging to feed being removed from database
            realm.where(Item.class).equalTo(Item.FEED_ID, item.getId()).findAll().deleteAllFromRealm();
        }

        item.deleteFromRealm();
    }

    @NonNull
    public static RealmResults<Feed> getFeedsWithoutFolder(Realm realm, boolean onlyUnread) {
        RealmQuery<Feed> query = realm.where(Feed.class).equalTo(Feed.FOLDER_ID, 0);
        if(onlyUnread) {
            query.greaterThan(Feed.UNREAD_COUNT, 0);
        }
        return query.findAllSorted(Feed.NAME, Sort.ASCENDING);
    }

    @Nullable
    public static RealmResults<Feed> getFeedsForTreeItem(Realm realm, TreeItem item) {
        RealmQuery<Feed> feedQuery = realm.where(Feed.class);

        if(item instanceof AllUnreadFolder) {
            feedQuery.greaterThan(Feed.UNREAD_COUNT, 0);
        } else if(item instanceof StarredFolder) {
            feedQuery.greaterThan(Feed.STARRED_COUNT, 0);
        } else if(item instanceof Folder) {
            feedQuery.equalTo(Feed.FOLDER_ID, item.getId());
        } else {
            feedQuery = null;
        }

        return feedQuery != null ? feedQuery.findAllSorted(Feed.NAME, Sort.ASCENDING) : null;
    }

    public static void removeExcessItems(Realm realm, final int maxItems) {
        final RealmResults<Item> expendableItems = realm.where(Item.class)
                .equalTo(Item.UNREAD, false)
                .equalTo(Item.STARRED, false)
                .findAllSorted(Item.LAST_MODIFIED, Sort.ASCENDING);
        realm.executeTransaction(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                int itemsToDelete = expendableItems.size() - maxItems;
                for (int i = 0; i < itemsToDelete; i++) {
                    expendableItems.deleteFirstFromRealm();
                }
            }
        });
    }

    public static void markTemporaryFeedAsRead(Realm realm, @Nullable final Long lastItemId, Realm.Transaction.OnSuccess onSuccess, Realm.Transaction.OnError onError) {
        realm.executeTransactionAsync(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                try {
                    TemporaryFeed temporaryFeed = realm.where(TemporaryFeed.class).findFirst();

                    RealmQuery<Item> itemRealmQuery = temporaryFeed.getItems()
                            .where()
                            .equalTo(Item.UNREAD, true);

                    // Make sure lastItem is in the query results
                    if(lastItemId != null && lastItemId >= 0) {
                        itemRealmQuery.or().equalTo(Item.ID, lastItemId);
                    }

                    RealmResults<Item> unreadItems = itemRealmQuery.findAll();

                    for(Item item: unreadItems) {
                        item.setUnread(false);
                        if(lastItemId != null && item.getId() == lastItemId) {
                            break;
                        }
                    }
                } finally {
                    checkAlarm(realm);
                }
            }
        }, onSuccess, onError);
    }

    public static void setItemsUnread(Realm realm, final boolean newUnread, final Item... items) {
        realm.executeTransaction(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                try {
                    for (Item item : items) {
                        /** If the item has a fingerprint, mark all items with the same fingerprint
                         * as read
                         */
                        if(item.getFingerprint() == null) {
                            item.setUnread(newUnread);
                        } else {
                            RealmResults<Item> sameItems = realm.where(Item.class)
                                    .equalTo(Item.FINGERPRINT, item.getFingerprint())
                                    .equalTo(Item.UNREAD, !newUnread)
                                    .findAll();
                            for(Item sameItem: sameItems) {
                                sameItem.setUnread(newUnread);
                            }
                        }
                    }
                } catch (RealmException e) {
                    e.printStackTrace();
                } finally {
                    checkAlarm(realm);
                }
            }
        });
    }

    public static void setItemsStarred(Realm realm, final boolean newStarred, final Item... items) {
        realm.executeTransaction(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                try {
                    for (Item item : items) {
                        item.setStarred(newStarred);
                    }
                } catch (RealmException e) {
                    e.printStackTrace();
                } finally {
                    checkAlarm(realm);
                }
            }
        });
    }

    private static synchronized void checkAlarm(Realm realm) {
        long changedItemsCount = realm.where(Item.class)
                .equalTo(Item.UNREAD_CHANGED, true)
                .or()
                .equalTo(Item.STARRED_CHANGED, true).count();
        if (changedItemsCount > 0)
            AlarmUtils.getInstance().setAlarm();
        else
            AlarmUtils.getInstance().cancelAlarm();
    }

    /**
     * Return the feed with id feedId, or insert a new (temporary) feed into the database.
     * @param realm Database to operate on
     * @param feedId id of the feed
     * @return Feed with id feedId (either from the database or a newly created one)
     */
    public static Feed getOrCreateFeed(Realm realm, long feedId) {
        Feed feed = getFeed(realm, feedId);
        if(feed == null) {
            feed = realm.createObject(Feed.class);
            feed.setId(feedId);
        }
        return feed;
    }

    /**
     * Return the folder with id folderId, or insert a new (temporary) folder into the database.
     * @param realm Database to operate on
     * @param folderId id of the folder
     * @return Folder with id folderId (either from the database or a newly created one)
     */
    @Nullable
    public static Folder getOrCreateFolder(Realm realm, long folderId) {
        // root has folderId == 0, which has no folder in db
        if(folderId == 0)
            return null;

        Folder folder = getFolder(realm, folderId);
        if(folder == null) {
            folder = realm.createObject(Folder.class);
            folder.setId(folderId);
        }
        return folder;
    }

    public static void deleteFeed(final Realm realm, final Feed feed) {
        realm.executeTransaction(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                realm.where(Item.class).equalTo(Item.FEED_ID, feed.getId()).findAll().deleteAllFromRealm();
                feed.deleteFromRealm();
            }
        });
    }
}
