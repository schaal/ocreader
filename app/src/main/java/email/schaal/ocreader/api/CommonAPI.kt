package email.schaal.ocreader.api

import email.schaal.ocreader.api.json.APILevels
import retrofit2.Response
import retrofit2.http.GET

interface CommonAPI {
    @GET(API.API_ROOT)
    suspend fun apiLevels(): Response<APILevels>
}