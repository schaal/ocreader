package email.schaal.ocreader.api

import email.schaal.ocreader.database.model.User
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Path

interface OCSAPI {
    @Headers("OCS-APIRequest: true")
    @GET("ocs/v1.php/cloud/users/{userId}?format=json")
    suspend fun user(@Path("userId") userId: String): User
}