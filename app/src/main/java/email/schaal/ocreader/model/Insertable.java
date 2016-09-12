package email.schaal.ocreader.model;

import io.realm.Realm;

/**
 * Created by daniel on 12.09.16.
 */

public interface Insertable {
    void insert(Realm realm);
}
