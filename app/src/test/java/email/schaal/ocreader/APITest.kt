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
import email.schaal.ocreader.api.API
import email.schaal.ocreader.api.API.Companion.API_ROOT
import email.schaal.ocreader.api.Level
import email.schaal.ocreader.api.json.ItemIds
import email.schaal.ocreader.api.json.ItemMap
import email.schaal.ocreader.database.model.Item
import email.schaal.ocreader.util.buildBaseUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class APITest {
    @Test
    fun testBuildBaseUrl() {
        for (url in mapOf(
                "https://test.example.com" to "https://test.example.com/index.php/apps/news/api/v1-2/",
                "https://test.example.com/" to "https://test.example.com/index.php/apps/news/api/v1-2/",
                "https://test.example.com/subdir" to "https://test.example.com/subdir/index.php/apps/news/api/v1-2/",
                "https://test.example.com/subdir/" to "https://test.example.com/subdir/index.php/apps/news/api/v1-2/")) {
            Assert.assertEquals(url.key.toHttpUrlOrNull()!!.buildBaseUrl("${API_ROOT}/${Level.V12.level}/").toString(), url.value)
        }
    }
}