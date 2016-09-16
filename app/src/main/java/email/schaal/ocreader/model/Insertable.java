package email.schaal.ocreader.model;

import io.realm.Realm;

/**
 * Mark an object as being able to be inserted into a realm database
 */
public interface Insertable {
    /**
     * Insert this object into the specified realm database.
     * This method should not insert this object if it is reduced.
     * @param realm Database to insert object into.
     */
    void insert(Realm realm);
    void delete(Realm realm);
}
