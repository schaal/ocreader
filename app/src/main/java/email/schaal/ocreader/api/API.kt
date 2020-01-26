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

package email.schaal.ocreader.api

import android.content.Context
import android.util.Log
import androidx.preference.PreferenceManager
import com.github.zafarkhaja.semver.Version
import com.squareup.moshi.Moshi
import email.schaal.ocreader.Preferences
import email.schaal.ocreader.R
import email.schaal.ocreader.api.json.*
import email.schaal.ocreader.api.json.v12.ItemIds
import email.schaal.ocreader.api.json.v12.ItemMap
import email.schaal.ocreader.database.model.*
import email.schaal.ocreader.http.HttpManager
import email.schaal.ocreader.service.SyncType
import io.realm.Realm
import io.realm.kotlin.where
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.*

class API {
    companion object {
        const val BATCH_SIZE = 100
        const val API_ROOT = "./index.php/apps/news/api/"

        val MIN_VERSION: Version = Version.forIntegers(8, 8, 2)

        val moshi: Moshi = Moshi.Builder()
                .add(Feed::class.java, FeedTypeAdapter())
                .add(Item::class.java, ItemTypeAdapter())
                .add(User::class.java, UserTypeAdapter())
                .add(Status::class.java, StatusTypeAdapter())
                .build()
        val converterFactory: MoshiConverterFactory = MoshiConverterFactory.create(moshi)

        var instance: API? = null

        suspend fun login(context: Context, baseUrl: HttpUrl, username: String, password: String): Status? {
            val httpManager = HttpManager(username, password, baseUrl)
            val resolvedBaseUrl = baseUrl.resolve("") ?: throw IllegalStateException("Couldn't parse URL")

            val moshi = Moshi.Builder().build()

            val retrofit = Retrofit.Builder()
                    .baseUrl(resolvedBaseUrl)
                    .client(httpManager.client)
                    .addConverterFactory(MoshiConverterFactory.create(moshi))
                    .build()

            val commonAPI = retrofit.create(CommonAPI::class.java)
            val response = commonAPI.apiLevels()
            if(response.isSuccessful) {
                val apiLevels = response.body()
                val apiLevel = apiLevels?.highestSupportedApi() ?: throw IllegalStateException(context.getString(R.string.error_not_compatible))
                val loginInstance = Level.getAPI(context, apiLevel, httpManager)
                val status = loginInstance.metaData()
                val version = status?.version
                if(version != null && MIN_VERSION.lessThanOrEqualTo(version)) {
                    PreferenceManager.getDefaultSharedPreferences(context).edit()
                            .putString(Preferences.USERNAME.key, username)
                            .putString(Preferences.PASSWORD.key, password)
                            .putString(Preferences.URL.key, resolvedBaseUrl.toString())
                            .putString(Preferences.SYS_DETECTED_API_LEVEL.key, apiLevel.level)
                            .apply()
                    instance = loginInstance
                    return status
                }
            }
            instance = null
            return null
        }
    }

    enum class MarkAction(val key: String, val changedKey: String, val value: Boolean) {
        MARK_READ(Item::unread.name, Item::unreadChanged.name, false),
        MARK_UNREAD(Item::unread.name, Item::unreadChanged.name, true),
        MARK_STARRED(Item::starred.name, Item::starredChanged.name, true),
        MARK_UNSTARRED(Item::starred.name, Item::starredChanged.name, false);
    }

    private interface APIv12Interface {
        /* SERVER **/
        /** Since 6.0.5  */
        @GET("user")
        suspend fun user(): User

        @GET("status")
        suspend fun status(): Status

        /** FOLDERS  */
        @GET("folders")
        suspend fun folders(): Folders

        /** FEEDS  */
        @GET("feeds")
        suspend fun feeds(): Feeds

        @POST("feeds")
        suspend fun createFeed(@Body feedMap: Map<String, Any>): Response<Feeds>

        @PUT("feeds/{feedId}/move")
        suspend fun moveFeed(@Path("feedId") feedId: Long, @Body folderIdMap: Map<String, Long>): Response<Void>

        @DELETE("feeds/{feedId}")
        suspend fun deleteFeed(@Path("feedId") feedId: Long): Response<Void>

        /** ITEMS  */
        @GET("items")
        suspend fun items(
                @Query("batchSize") batchSize: Long,
                @Query("offset") offset: Long,
                @Query("type") type: Int,
                @Query("id") id: Long,
                @Query("getRead") getRead: Boolean,
                @Query("oldestFirst") oldestFirst: Boolean
        ): Items

        @GET("items/updated")
        suspend fun updatedItems(
                @Query("lastModified") lastModified: Long,
                @Query("type") type: Int,
                @Query("id") id: Long
        ): Items

        @PUT("items/read/multiple")
        suspend fun markItemsRead(@Body items: ItemIds): Response<Void>

        @PUT("items/unread/multiple")
        suspend fun markItemsUnread(@Body items: ItemIds): Response<Void>

        @PUT("items/star/multiple")
        suspend fun markItemsStarred(@Body itemMap: ItemMap): Response<Void>

        @PUT("items/unstar/multiple")
        suspend fun markItemsUnstarred(@Body itemMap: ItemMap): Response<Void>
    }

    private val api: APIv12Interface?

    constructor(context: Context, httpManager: HttpManager) {
        api = setupApi(httpManager)
    }

    constructor(context: Context) {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        val username = Preferences.USERNAME.getString(sharedPreferences)
        val password = Preferences.PASSWORD.getString(sharedPreferences)
        val url = Preferences.URL.getString(sharedPreferences)

        if(username == null || password == null)
            throw IllegalStateException()

        val httpManager = HttpManager(username, password, url?.toHttpUrlOrNull()!!)

        api = setupApi(httpManager)
    }

    private fun setupApi(httpManager: HttpManager): APIv12Interface {
        val retrofit = Retrofit.Builder()
                .baseUrl(httpManager.credentials.rootUrl.resolve(String.format("%s%s/", API_ROOT, Level.V12.level))!!)
                .client(httpManager.client)
                .addConverterFactory(converterFactory)
                .build()

        return retrofit.create(APIv12Interface::class.java)
    }

    private suspend fun syncChanges(realm: Realm): Map<MarkAction, List<Item>?> = runBlocking {
        MarkAction.values()
                .toList().associateWith {
                    markItems(it, realm)
                }
    }

    private suspend fun markItems(action: MarkAction, realm: Realm): List<Item>? {
        val results = realm.where<Item>()
                .equalTo(action.changedKey, true)
                .equalTo(action.key, action.value)
                .findAll()

        if(results.isEmpty())
            return null

        val response: Response<Void>? = when(action) {
            MarkAction.MARK_READ -> {
                api?.markItemsRead(ItemIds(results))
            }
            MarkAction.MARK_UNREAD -> {
                api?.markItemsUnread(ItemIds(results))
            }
            MarkAction.MARK_STARRED -> {
                api?.markItemsStarred(ItemMap(results))
            }
            MarkAction.MARK_UNSTARRED -> {
                api?.markItemsUnstarred(ItemMap(results))
            }
        }

        return if (response != null && response.isSuccessful) results else throw IllegalStateException("Marking items failed")
    }

    private enum class QueryType(val type: Int) {
        FEED(0),
        FOLDER(1),
        STARRED(2),
        ALL(3)
    }

    suspend fun sync(realm: Realm, syncType: SyncType) {
        try {
            val result = syncChanges(realm)
            when(syncType) {
                SyncType.SYNC_CHANGES_ONLY -> {}
                SyncType.FULL_SYNC -> {
                    val lastSync = realm.where<Item>().max(Item::lastModified.name)?.toLong() ?: 0L

                    val toInsert: MutableSet<List<Insertable?>> = mutableSetOf()

                    toInsert.add(listOf(api?.user()))

                    val folders = api?.folders()?.folders
                    val feeds = api?.feeds()?.feeds

                    if(lastSync == 0L) {
                        api?.items(-1L, 0L, QueryType.STARRED.type, 0L, getRead = true, oldestFirst = false)?.items?.let { toInsert.add(it) }
                        api?.items(-1L, 0L, QueryType.ALL.type, 0L, getRead = false, oldestFirst = false)?.items?.let { toInsert.add(it) }
                    } else {
                        api?.updatedItems(lastSync, QueryType.ALL.type, 0L)?.items?.let { toInsert.add(it) }
                    }
                    withContext(Dispatchers.Main) {
                        realm.executeTransaction {
                            for ((action, results) in result) {
                                if(results != null) {
                                    when (action) {
                                        MarkAction.MARK_READ, MarkAction.MARK_UNREAD -> {
                                            for (item in results) {
                                                item.unreadChanged = false
                                            }
                                        }
                                        MarkAction.MARK_STARRED, MarkAction.MARK_UNSTARRED -> {
                                            for (item in results) {
                                                item.starredChanged = false
                                            }
                                        }
                                    }
                                }
                            }

                            if(folders != null) {
                                val dbFolders = realm.where<Folder>().findAll()
                                val foldersToDelete = dbFolders.minus(folders)

                                for(folder in folders)
                                    folder.insert(realm)

                                Log.d("API", foldersToDelete.toString())

                                for(folder in foldersToDelete)
                                    folder.delete(realm)
                            }

                            val dbFeeds = realm.where<Feed>().findAll()

                            if(feeds != null) {
                                val feedsToDelete = dbFeeds.minus(feeds)

                                for(feed in feeds)
                                    feed.insert(realm)

                                Log.d("API", feedsToDelete.toString())

                                for(feed in feedsToDelete)
                                    feed.delete(realm)
                            }

                            for (insertables in toInsert) {
                                for (insertable in insertables.filterNotNull())
                                    insertable.insert(realm)
                            }

                            for (feed in dbFeeds) {
                                feed.starredCount = realm.where<Item>()
                                        .equalTo(Item::feedId.name, feed.id)
                                        .equalTo(Item::starred.name, true).count().toInt()
                                feed.unreadCount = realm.where<Item>()
                                        .equalTo(Item::feedId.name, feed.id)
                                        .equalTo(Item::unread.name, true).count().toInt()
                            }
                        }
                    }
                }
                SyncType.LOAD_MORE -> {

                }
            }
        } catch(e: IllegalStateException) {
            e.printStackTrace()
        }
    }

    fun createFeed(realm: Realm, url: String, folderId: Long) {

    }

    fun deleteFeed(realm: Realm, feed: Feed) {

    }

    fun moveFeed(realm: Realm, feed: Feed, folderId: Long) {

    }

    private interface CommonAPI {
        @GET("index.php/apps/news/api")
        suspend fun apiLevels(): Response<APILevels>
    }

    suspend fun metaData(): Status? {
        return api?.status()
    }

}