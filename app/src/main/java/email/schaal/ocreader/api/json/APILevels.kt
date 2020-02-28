package email.schaal.ocreader.api.json

import email.schaal.ocreader.api.Level

/**
 * API response containing supported API levels
 */
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