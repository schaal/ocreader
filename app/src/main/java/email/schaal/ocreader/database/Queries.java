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

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import email.schaal.ocreader.model.AllUnreadFolder;
import email.schaal.ocreader.model.Feed;
import email.schaal.ocreader.model.Folder;
import email.schaal.ocreader.model.Item;
import email.schaal.ocreader.model.StarredFolder;
import email.schaal.ocreader.model.TemporaryFeed;
import email.schaal.ocreader.model.TreeItem;
import email.schaal.ocreader.util.AlarmUtils;
import io.realm.DynamicRealm;
import io.realm.DynamicRealmObject;
import io.realm.Realm;
import io.realm.RealmConfiguration;
import io.realm.RealmList;
import io.realm.RealmMigration;
import io.realm.RealmObject;
import io.realm.RealmQuery;
import io.realm.RealmResults;
import io.realm.RealmSchema;
import io.realm.Sort;
import io.realm.exceptions.RealmException;

/**
 * Utility class containing some commonly used Queries for the Realm database.
 */
public class Queries {
    private final static String TAG = Queries.class.getName();

    public final static int SCHEMA_VERSION = 4;

    private static Queries instance;

    private static final RealmMigration migration = new RealmMigration() {
        @Override
        public void migrate(DynamicRealm realm, long oldVersion, long newVersion) {
            Log.d(TAG, "Starting migration from " + oldVersion + "to " + newVersion);

            RealmSchema schema = realm.getSchema();

            /**
             * v1 -> v2
             * - Add unreadChanged and starredChanged boolean fields to Item
             * - Migrate data from changedItems to new fields
             * - Remove ChangedItems
             */
            if(oldVersion == 1) {
                DynamicRealmObject changedItems = realm.where("ChangedItems").findFirst();
                final RealmList<DynamicRealmObject> unreadChangedItems = changedItems.getList("unreadChangedItems");
                final RealmList<DynamicRealmObject> starredChangedItems = changedItems.getList("starredChangedItems");

                schema.get("Item")
                        .addField(Item.UNREAD_CHANGED, boolean.class)
                        .addField(Item.STARRED_CHANGED, boolean.class);

                for(DynamicRealmObject item: unreadChangedItems) {
                    item.set(Item.UNREAD_CHANGED, true);
                }
                for(DynamicRealmObject item: starredChangedItems) {
                    item.set(Item.STARRED_CHANGED, true);
                }

                schema.remove("ChangedItems");
                oldVersion++;
            }

            /**
             * v2 -> v3
             * - Add indexed fingerprint field to Item
             */
            if(oldVersion == 2) {
                schema.get("Item")
                        .addField(Item.FINGERPRINT, String.class)
                        .addIndex(Item.FINGERPRINT);
                oldVersion++;
            }

            /**
             * v3 -> v4
             * - Add feed field to Item
             */
            if(oldVersion == 3) {
                schema.get("Item")
                        .addRealmObjectField(Item.FEED, schema.get("Feed"));
                RealmResults<DynamicRealmObject> items = realm.where("Item").findAll();
                for(DynamicRealmObject item: items) {
                    item.setObject(Item.FEED, realm.where("Feed").equalTo(Feed.ID, item.getLong(Item.FEED_ID)).findFirst());
                }
                oldVersion++;
            }
        }
    };

    private Queries(Context context) {
        this(new RealmConfiguration.Builder(context)
                .schemaVersion(SCHEMA_VERSION)
                .migration(migration)
                .build());
    }

    private Queries(RealmConfiguration realmConfiguration) {
        Realm.setDefaultConfiguration(realmConfiguration);

        Realm realm = null;
        try {
            Realm.compactRealm(realmConfiguration);
            realm = Realm.getDefaultInstance();
            if(realm.isEmpty()) {
                realm.executeTransaction(new Realm.Transaction() {
                    @Override
                    public void execute(Realm realm) {
                        realm.createObject(TemporaryFeed.class);
                    }
                });
            }
        } catch (IllegalArgumentException ex) {
            ex.printStackTrace();
            Realm.deleteRealm(realmConfiguration);
            resetDatabase();
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
        instance = new Queries(context);
    }

    // For instrumentation tests
    public static void init(RealmConfiguration realmConfiguration) {
        instance = new Queries(realmConfiguration);
    }

    public void resetDatabase() {
        Log.w(TAG, "Database will be reset");

        Realm realm = null;
        try {
            realm = Realm.getDefaultInstance();
            realm.executeTransaction(new Realm.Transaction() {
                @Override
                public void execute(Realm realm) {
                    realm.deleteAll();
                    realm.createObject(TemporaryFeed.class);
                }
            });
        } finally {
            if(realm != null)
                realm.close();
        }
    }

    @Nullable
    public Folder getFolder(Realm realm, long id) {
        return realm.where(Folder.class).equalTo(Folder.ID, id).findFirst();
    }

    @Nullable
    public Feed getFeed(Realm realm, long id) {
        return realm.where(Feed.class).equalTo(Feed.ID, id).findFirst();
    }

    /**
     * Get all items belonging to treeItem, sorted by sortFieldname using order
     * @param realm Realm object to query
     * @param treeItem TreeItem to query items from
     * @param onlyUnread Return only unread items?
     * @param sortFieldname Sort using this fieldname
     * @param order Sort using this order
     * @return items belonging to TreeItem, only unread if onlyUnread is true
     */
    @Nullable
    public RealmResults<Item> getItems(Realm realm, TreeItem treeItem, boolean onlyUnread, String sortFieldname, Sort order) {
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
                RealmResults<Item> distinctItems = query.distinct(Item.FINGERPRINT);
                return distinctItems.sort(sortFieldname, order);
            } else {
                return query.findAllSorted(sortFieldname, order);
            }
        } else
            return null;
    }

    @Nullable
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

    @NonNull
    public RealmResults<Feed> getFeeds(Realm realm) {
        return realm.where(Feed.class).findAllSorted(Feed.PINNED, Sort.ASCENDING, Feed.TITLE, Sort.ASCENDING);
    }

    public <T extends RealmObject> void insert(Realm realm, final Class<T> clazz, final Iterable<T> elements) {
        realm.executeTransaction(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                if(clazz == Item.class) {
                    for (T element : elements) {
                        Item item = (Item) element;
                        item.setFeed(getOrCreateFeed(realm, item.getFeedId()));
                    }
                }
                realm.copyToRealmOrUpdate(elements);
            }
        });

    }

    public <T extends RealmObject> void insert(Realm realm, final Class<T> clazz, final T element) {
        insert(realm, clazz, Collections.singleton(element));
    }

    public <T extends RealmObject & TreeItem> void deleteAndInsert(Realm realm, final Class<T> clazz, final List<T> elements) {
        // Sort elements for binary search
        Collections.sort(elements, TreeItem.COMPARATOR);

        realm.executeTransaction(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                realm.copyToRealmOrUpdate(elements);

                RealmResults<T> results = realm.where(clazz).findAll();

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
                        realm.where(Item.class).equalTo(Item.FEED_ID, toRemove.getId()).findAll().deleteAllFromRealm();
                    }
                    toRemove.deleteFromRealm();
                }
            }
        });
    }

    @NonNull
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

    @Nullable
    public RealmResults<Feed> getFeedsForTreeItem(Realm realm, TreeItem item) {
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

        return feedQuery != null ? feedQuery.findAllSorted(Feed.TITLE, Sort.ASCENDING) : null;
    }

    public void removeExcessItems(Realm realm, final int maxItems) {
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

    public void markTemporaryFeedAsRead(Realm realm, @Nullable final Long lastItemId, Realm.Transaction.OnSuccess onSuccess, Realm.Transaction.OnError onError) {
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

    public void setItemsUnread(Realm realm, final boolean newUnread, final Item... items) {
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

    public void setItemsStarred(Realm realm, final boolean newStarred, final Item... items) {
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

    private synchronized void checkAlarm(Realm realm) {
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
     * Return the feed with id feedId, or insert a new one into the database.
     * @param realm Database to operate on
     * @param feedId id of the feed
     * @return Feed with id feedId (either from the database or a newly created one)
     */
    public Feed getOrCreateFeed(Realm realm, long feedId) {
        Feed feed = getFeed(realm, feedId);
        if(feed == null) {
            feed = realm.createObject(Feed.class);
            feed.setId(feedId);
        }
        return feed;
    }
}
