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
package email.schaal.ocreader

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.ActivityTestRule
import email.schaal.ocreader.database.Queries
import email.schaal.ocreader.database.model.Feed
import email.schaal.ocreader.database.model.Folder
import email.schaal.ocreader.database.model.Item
import io.realm.Realm
import io.realm.kotlin.where
import org.junit.*
import org.junit.runner.RunWith

/**
 * [Testing Fundamentals](http://d.android.com/tools/testing/testing_android.html)
 */
@RunWith(AndroidJUnit4::class)
class DatabaseTest {
    @Rule
    var activityTestRule = ActivityTestRule(ListActivity::class.java)

    @Before
    fun setUp() {
        Queries.resetDatabase()
    }

    @After
    fun tearDown() {
        Queries.resetDatabase()
    }

    @Test
    fun testDatabaseSetup() {
        val realm = Realm.getDefaultInstance()
        realm.close()
        Assert.assertTrue(realm.isClosed)
    }

    @Test
    fun testFolderInsert() {
        var realm: Realm? = null
        try {
            val folder = TestGenerator.testFolder
            realm = Realm.getDefaultInstance()
            realm.beginTransaction()
            folder.insert(realm)
            realm.commitTransaction()
            val dbFolder = Folder.get(realm, 1)
            Assert.assertNotNull(dbFolder)
            Assert.assertEquals(dbFolder?.name, TestGenerator.FOLDER_TITLE)
        } finally {
            Assert.assertNotNull(realm)
            realm?.close()
        }
    }

    @Test
    fun testFeedInsert() {
        var realm: Realm? = null
        try {
            var feed: Feed? = TestGenerator.testFeed
            realm = Realm.getDefaultInstance()
            realm.beginTransaction()
            feed?.insert(realm)
            realm.commitTransaction()
            feed = Feed.get(realm, 1)
            Assert.assertNotNull(feed)
            Assert.assertEquals(feed!!.name, TestGenerator.FEED_TITLE)
        } finally {
            Assert.assertNotNull(realm)
            realm!!.close()
        }
    }

    @Test
    fun testItemInsert() {
        var realm: Realm? = null
        try {
            val feed: Feed? = TestGenerator.testFeed
            var item: Item? = TestGenerator.testItem
            realm = Realm.getDefaultInstance()
            realm.beginTransaction()
            feed?.insert(realm)
            item?.insert(realm)
            realm.commitTransaction()
            item = realm.where<Item>().findFirst()
            Assert.assertEquals(item!!.id, 1)
            Assert.assertEquals(item.title, TestGenerator.ITEM_TITLE)
            Assert.assertEquals(item.body, TestGenerator.BODY)
            Assert.assertEquals(item.author, TestGenerator.AUTHOR)
            Assert.assertNull(item.enclosureLink)
        } finally {
            Assert.assertNotNull(realm)
            realm?.close()
        }
    }
}