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

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.rule.ActivityTestRule;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import email.schaal.ocreader.database.Queries;
import email.schaal.ocreader.database.model.Feed;
import email.schaal.ocreader.database.model.Folder;
import email.schaal.ocreader.database.model.Item;
import io.realm.Realm;

import static email.schaal.ocreader.TestGenerator.AUTHOR;
import static email.schaal.ocreader.TestGenerator.BODY;
import static email.schaal.ocreader.TestGenerator.FEED_TITLE;
import static email.schaal.ocreader.TestGenerator.FOLDER_TITLE;
import static email.schaal.ocreader.TestGenerator.ITEM_TITLE;
import static email.schaal.ocreader.TestGenerator.getTestFeed;
import static email.schaal.ocreader.TestGenerator.getTestFolder;
import static email.schaal.ocreader.TestGenerator.getTestItem;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * <a href="http://d.android.com/tools/testing/testing_android.html">Testing Fundamentals</a>
 */
@RunWith(AndroidJUnit4.class)
public class DatabaseTest {

    @Rule
    public ActivityTestRule<ListActivity> activityTestRule = new ActivityTestRule<>(ListActivity.class);

    @Before
    public void setUp() {
        Queries.resetDatabase();
    }

    @After
    public void tearDown() {
        Queries.resetDatabase();
    }

    @Test
    public void testDatabaseSetup() {
        Realm realm = Realm.getDefaultInstance();
        realm.close();
        assertTrue(realm.isClosed());
    }

    @Test
    public void testFolderInsert() {
        Realm realm = null;
        try {
            Folder folder = getTestFolder();

            realm = Realm.getDefaultInstance();
            Queries.insert(realm, folder);

            Folder dbFolder = Folder.get(realm, 1);

            assertNotNull(dbFolder);
            assertEquals(dbFolder.getName(), FOLDER_TITLE);

        } finally {
            assertNotNull(realm);
            realm.close();
        }
    }

    @Test
    public void testFeedInsert() {
        Realm realm = null;
        try {
            Feed feed = getTestFeed();

            realm = Realm.getDefaultInstance();
            Queries.insert(realm, feed);

            feed = Feed.get(realm, 1);

            assertNotNull(feed);
            assertEquals(feed.getName(), FEED_TITLE);

        } finally {
            assertNotNull(realm);
            realm.close();
        }
    }

    @Test
    public void testItemInsert() {
        Realm realm = null;
        try {
            Feed feed = getTestFeed();
            Item item = getTestItem();

            realm = Realm.getDefaultInstance();
            Queries.insert(realm, feed);
            Queries.insert(realm, item);

            item = realm.where(Item.class).findFirst();

            assertEquals(item.getId(), 1);
            assertEquals(item.getTitle(), ITEM_TITLE);
            assertEquals(item.getBody(), BODY);
            assertEquals(item.getAuthor(), AUTHOR);
            assertNull(item.getEnclosureLink());
        } finally {
            assertNotNull(realm);
            realm.close();
        }
    }
}