package email.schaal.ocreader.service

/**
 * Created by daniel on 02.04.17.
 */
enum class SyncType(val action: String) {
    FULL_SYNC("email.schaal.ocreader.action.FULL_SYNC"),
    SYNC_CHANGES_ONLY("email.schaal.ocreader.action.SYNC_CHANGES_ONLY"),
    LOAD_MORE("email.schaal.ocreader.action.LOAD_MORE");

    companion object {
        operator fun get(action: String?): SyncType? = values().find { it.action == action }
    }

}