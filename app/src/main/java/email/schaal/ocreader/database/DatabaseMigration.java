package email.schaal.ocreader.database;

import android.util.Log;

import email.schaal.ocreader.database.model.Item;
import email.schaal.ocreader.database.model.TemporaryFeed;
import io.realm.DynamicRealm;
import io.realm.DynamicRealmObject;
import io.realm.FieldAttribute;
import io.realm.RealmMigration;
import io.realm.RealmSchema;

/**
 * RealmMigration to migrate database between schema versions
 */
class DatabaseMigration implements RealmMigration {
    private static final String TAG = DatabaseMigration.class.getName();

    @Override
    public void migrate(final DynamicRealm realm, long oldVersion, long newVersion) {
        Log.d(TAG, "Starting migration from " + oldVersion + " to " + newVersion);

        // Migration from versions < 9 not supported, versions prior 9 were missing the
        // contentHash for items
        if(oldVersion < 9) {
            throw new IllegalStateException("Migration from Schema < 9 not supported");
        }

        RealmSchema schema = realm.getSchema();

        /**
         * 9 -> 10
         *
         * - Add primary key ID to TemporaryFeed
         * - Rename TemporaryFeed id to treeItemId
         * - add TemporaryFeed object for list and pager activities
         */
        if(oldVersion == 9) {
            realm.delete("TemporaryFeed");

            schema.get("TemporaryFeed")
                    .renameField(TemporaryFeed.ID, TemporaryFeed.TREE_ITEM_ID)
                    .addField(TemporaryFeed.ID, long.class, FieldAttribute.PRIMARY_KEY);

            realm.createObject("TemporaryFeed", TemporaryFeed.LIST_ID);
            realm.createObject("TemporaryFeed", TemporaryFeed.PAGER_ID);

            oldVersion++;
        }

        /**
         * 10 -> 11
         *
         *  - Make sure every item has updatedAt != null, set updatedAt = pubDate if not
         */
        if(oldVersion == 10) {
            for(DynamicRealmObject object: realm.where("Item").isNull(Item.UPDATED_AT).findAll()) {
                object.setDate(Item.UPDATED_AT, object.getDate(Item.PUB_DATE));
            }

            //noinspection UnusedAssignment
            oldVersion++;
        }
    }
}
