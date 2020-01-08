package email.schaal.ocreader.api.json

import email.schaal.ocreader.api.Level

/**
 * API response containing supported API levels
 */
class APILevels {
    private var apiLevels: List<String>? = null
    fun highestSupportedApi(): Level? {
        for (level in Level.values()) if (level.isSupported && apiLevels!!.contains(level.level)) return level
        return null
    }

    fun setApiLevels(apiLevels: List<String>?) {
        this.apiLevels = apiLevels
    }
}