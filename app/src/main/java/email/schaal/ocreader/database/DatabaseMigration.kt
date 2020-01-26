package email.schaal.ocreader.database

import android.util.Log
import email.schaal.ocreader.database.model.Item
import email.schaal.ocreader.database.model.TemporaryFeed
import io.realm.DynamicRealm
import io.realm.FieldAttribute
import io.realm.RealmMigration

/**
 * RealmMigration to migrate database between schema versions
 */
internal class DatabaseMigration : RealmMigration {
    override fun migrate(realm: DynamicRealm, oldVersion: Long, newVersion: Long) {
        var oldVersion = oldVersion
        Log.d(DatabaseMigration::class.simpleName, "Starting migration from $oldVersion to $newVersion")
        // Migration from versions < 9 not supported, versions prior 9 were missing the
// contentHash for items
        check(oldVersion >= 12) { "Migration from Schema < 12 not supported" }
    }
}