package email.schaal.ocreader.api.json

import com.squareup.moshi.JsonClass
import email.schaal.ocreader.api.Level

/**
 * API response containing supported API levels
 */
@JsonClass(generateAdapter = true)
data class APILevels(
        val apiLevels: List<String> = emptyList()
) {
    fun highestSupportedApi(): Level? {
        for (level in Level.values())
            if (level.isSupported && apiLevels.contains(level.level))
                return level
        return null
    }
}