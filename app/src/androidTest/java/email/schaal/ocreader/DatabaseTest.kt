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

import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
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
    @get:Rule
    var activityTestRule = ActivityScenarioRule(ListActivity::class.java)

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
        Realm.getDefaultInstance().use { realm ->
            realm.beginTransaction()
            TestGenerator.testFolder.insert(realm)
            realm.commitTransaction()
            Folder.get(realm, 1).let { dbFolder ->
                Assert.assertNotNull(dbFolder)
                Assert.assertEquals(dbFolder?.name, TestGenerator.FOLDER_TITLE)
            }
        }
    }

    @Test
    fun testFeedInsert() {
        Realm.getDefaultInstance().use { realm ->
            realm.beginTransaction()
            TestGenerator.testFeed.insert(realm)
            realm.commitTransaction()
            Feed.get(realm, 1).let { feed ->
                Assert.assertNotNull(feed)
                Assert.assertEquals(feed?.name, TestGenerator.FEED_TITLE)
            }
        }
    }

    @Test
    fun testItemInsert() {
        Realm.getDefaultInstance().use { realm ->
            realm.beginTransaction()
            TestGenerator.testFeed.insert(realm)
            TestGenerator.testItem.insert(realm)
            realm.commitTransaction()
            realm.where<Item>().findFirst().let { item ->
                Assert.assertEquals(item?.id, 1)
                Assert.assertEquals(item?.title, TestGenerator.ITEM_TITLE)
                Assert.assertEquals(item?.body, TestGenerator.BODY)
                Assert.assertEquals(item?.author, TestGenerator.AUTHOR)
                Assert.assertNull(item?.enclosureLink)
            }
        }
    }
}