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

import android.test.ApplicationTestCase;

import java.util.Date;

import email.schaal.ocreader.database.Queries;
import email.schaal.ocreader.model.Feed;
import email.schaal.ocreader.model.Folder;
import email.schaal.ocreader.model.Item;
import io.realm.Realm;
import io.realm.RealmConfiguration;

/**
 * <a href="http://d.android.com/tools/testing/testing_android.html">Testing Fundamentals</a>
 */
public class DatabaseTest extends ApplicationTestCase<OCReaderApplication> {
    private static boolean firstRun = true;

    public DatabaseTest() {
        super(OCReaderApplication.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        if(firstRun) {
            firstRun = false;
            Queries.init(new RealmConfiguration.Builder(getContext()).inMemory().build());
        }
    }

    public void testDatabaseSetup() {
        Realm realm = Realm.getDefaultInstance();
        realm.close();
        assertTrue(realm.isClosed());
    }

    public void testFolderInsert() {
        Realm realm = null;
        try {
            Folder folder = new Folder();
            folder.setId(1);
            folder.setTitle("TestFolderTitle");

            realm = Realm.getDefaultInstance();
            Queries.getInstance().insert(realm, folder);

            Folder dbFolder = Queries.getInstance().getFolder(realm, 1);

            assertNotNull(dbFolder);
            assertEquals(dbFolder.getTitle(), "TestFolderTitle");

        } finally {
            assertNotNull(realm);
            realm.close();
        }
    }

    public void testFeedInsert() {
        Realm realm = null;
        try {
            Feed feed = new Feed();
            feed.setId(1);
            feed.setTitle("TestFeedTitle");

            realm = Realm.getDefaultInstance();
            Queries.getInstance().insert(realm, feed);

            feed = Queries.getInstance().getFeed(realm, 1);

            assertNotNull(feed);
            assertEquals(feed.getTitle(), "TestFeedTitle");

        } finally {
            assertNotNull(realm);
            realm.close();
        }
    }

    public void testItemInsert() {
        Realm realm = null;
        try {
            Item item = new Item();
            item.setId(1);
            item.setTitle("TestItemTitle");
            item.setBody("TestBody");
            item.setAuthor("TestAuthor");
            item.setFeedId(1);
            item.setLastModified(new Date());

            realm = Realm.getDefaultInstance();
            Queries.getInstance().insert(realm, item);

            item = realm.where(Item.class).findFirst();

            assertEquals(item.getId(), 1);
            assertEquals(item.getTitle(), "TestItemTitle");
            assertEquals(item.getBody(), "TestBody");
            assertEquals(item.getAuthor(), "TestAuthor");
            assertNull(item.getEnclosureLink());
        } finally {
            assertNotNull(realm);
            realm.close();
        }
    }
}