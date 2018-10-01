package email.schaal.ocreader.service;

import androidx.annotation.Nullable;

/**
 * Created by daniel on 02.04.17.
 */
public enum SyncType {
    FULL_SYNC("email.schaal.ocreader.action.FULL_SYNC"),
    SYNC_CHANGES_ONLY("email.schaal.ocreader.action.SYNC_CHANGES_ONLY"),
    LOAD_MORE("email.schaal.ocreader.action.LOAD_MORE");

    public final String action;

    SyncType(String action) {
        this.action = action;
    }

    @Nullable
    public static SyncType get(String action) {
        for (SyncType syncType : values())
            if (syncType.action.equals(action))
                return syncType;
        return null;
    }
}
