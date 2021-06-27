package email.schaal.ocreader.api

import email.schaal.ocreader.api.json.*
import retrofit2.Response
import retrofit2.http.*

interface APIv12Interface {
    /* SERVER **/
    @GET("status")
    suspend fun status(): Status

    /** FOLDERS  */
    @GET("folders")
    suspend fun folders(): Folders

    /** FEEDS  */
    @GET("feeds")
    suspend fun feeds(): Feeds

    @POST("feeds")
    suspend fun createFeed(@Body feedMap: Map<String, @JvmSuppressWildcards Any>): Feeds

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