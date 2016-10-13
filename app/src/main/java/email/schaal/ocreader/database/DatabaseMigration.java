package email.schaal.ocreader.database;

import android.util.Log;

import io.realm.DynamicRealm;
import io.realm.RealmMigration;
import io.realm.exceptions.RealmMigrationNeededException;

/**
 * RealmMigration to migrate database between schema versions
 */
class DatabaseMigration implements RealmMigration {
    private static final String TAG = DatabaseMigration.class.getName();

    @Override
    public void migrate(final DynamicRealm realm, long oldVersion, long newVersion) {
        Log.d(TAG, "Starting migration from " + oldVersion + "to " + newVersion);

        // Migration from versions < 9 not supported, versions prior 9 were missing the
        // contentHash for items
        if(oldVersion < 9) {
            throw new IllegalStateException("Migration from Schema < 9 not supported");
        }
    }
}
