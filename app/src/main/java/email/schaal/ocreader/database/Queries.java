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

import email.schaal.ocreader.database.model.Feed;
import email.schaal.ocreader.database.model.Insertable;
import email.schaal.ocreader.database.model.Item;
import email.schaal.ocreader.database.model.TemporaryFeed;
import email.schaal.ocreader.database.model.TreeItem;
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

    public static final int MAX_ITEMS = 10000;

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

    public static <T extends RealmObject & TreeItem & Insertable> void deleteAndInsert(Realm realm, final Class<T> clazz, final List<T> elements) {
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
                        currentDatabaseItem.delete(realm);
                    }

                    element.insert(realm);
                }

                // Remove remaining items from the database
                while (databaseIterator.hasNext()) {
                    databaseIterator.next().delete(realm);
                }
            }
        });
    }

    @NonNull
    public static RealmResults<Feed> getFeedsWithoutFolder(Realm realm, boolean onlyUnread) {
        RealmQuery<Feed> query = realm.where(Feed.class).equalTo(Feed.FOLDER_ID, 0);
        if(onlyUnread) {
            query.greaterThan(Feed.UNREAD_COUNT, 0);
        }
        return query.findAllSorted(Feed.NAME, Sort.ASCENDING);
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

    public static void markAboveAsRead(Realm realm, final List<Item> items, final long lastItemId) {
        realm.executeTransaction(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                try {
                    for(Item item: items) {
                        item.setUnread(false);
                        if(item.getId() == lastItemId) {
                            break;
                        }
                    }
                } finally {
                    checkAlarm(realm);
                }
            }
        });
    }

    public static void markTemporaryFeedAsRead(Realm realm, Realm.Transaction.OnSuccess onSuccess, Realm.Transaction.OnError onError) {
        realm.executeTransactionAsync(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                try {
                    TemporaryFeed temporaryFeed = realm.where(TemporaryFeed.class).findFirst();

                    RealmResults<Item> unreadItems = temporaryFeed.getItems()
                            .where()
                            .equalTo(Item.UNREAD, true).findAll();

                    for(Item item: unreadItems) {
                        item.setUnread(false);
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
}
