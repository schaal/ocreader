/*
 * Copyright © 2020. Daniel Schaal <daniel@schaal.email>
 *
 * This file is part of ocreader.
 *
 * ocreader is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ocreader is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Foobar.  If not, see <http://www.gnu.org/licenses/>.
 */

package email.schaal.ocreader

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.squareup.moshi.Moshi
import email.schaal.ocreader.api.json.ItemIds
import email.schaal.ocreader.api.json.ItemMap
import email.schaal.ocreader.database.model.Item
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class JsonTest {
    @Test
    fun testItemId() {
        val items = listOf(
                Item(1),
                Item(2),
                Item(3)
        )
        val itemIds = ItemIds(items)

        val adapter = Moshi.Builder().build().adapter(ItemIds::class.java)
        val adapterJsonString = adapter.toJson(itemIds)

        Assert.assertEquals(adapter.fromJson(adapterJsonString), itemIds)
    }

    @Test
    fun testItemMap() {
        val items = listOf(
                Item(1, feedId = 11L, guidHash = "hash1"),
                Item(2, feedId = 12L, guidHash = "hash2"),
                Item(3, feedId = 13L, guidHash = "hash3")
        )

        val itemMap = ItemMap(items)

        val adapter = Moshi.Builder().build().adapter(ItemMap::class.java)
        val adapterJsonString = adapter.toJson(itemMap)

        Assert.assertEquals(adapter.fromJson(adapterJsonString), itemMap)
    }
}