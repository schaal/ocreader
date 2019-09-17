/*
 * Copyright © 2019. Daniel Schaal <daniel@schaal.email>
 *
 * This file is part of ocreader.
 *
 * ocreader is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ocreader is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Foobar.  If not, see <http://www.gnu.org/licenses/>.
 */

package email.schaal.ocreader.database;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import java.util.List;

import email.schaal.ocreader.database.model.Item;
import io.realm.DynamicRealm;
import io.realm.Realm;

public class ItemsViewModel extends ViewModel {
    private final Realm realm;
    private LiveData<List<Item>> items;

    public ItemsViewModel() {
        realm = Realm.getDefaultInstance();
    }

    public LiveData<List<Item>> getItems() {
        return items;
    }

    @Override
    protected void onCleared() {
        realm.close();
        super.onCleared();
    }

    public void updateItems(LiveData<List<Item>> liveRealmResults) {
        items = liveRealmResults;
    }

    public Realm getRealm() {
        return realm;
    }
}
