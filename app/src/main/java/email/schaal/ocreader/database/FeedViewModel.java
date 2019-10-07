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

import android.content.SharedPreferences;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.List;

import email.schaal.ocreader.Preferences;
import email.schaal.ocreader.database.model.Item;
import email.schaal.ocreader.database.model.TemporaryFeed;
import io.realm.Realm;
import io.realm.RealmResults;

public class FeedViewModel extends ViewModel {
    private final Realm realm;
    private MutableLiveData<TemporaryFeed> temporaryFeedLiveData;
    private MutableLiveData<List<Item>> itemsLiveData;

    public FeedViewModel() {
        realm = Realm.getDefaultInstance();
    }

    public LiveData<TemporaryFeed> getTemporaryFeed() {
        return temporaryFeedLiveData;
    }
    public LiveData<List<Item>> getItems() {
        return itemsLiveData;
    }

    @Override
    protected void onCleared() {
        realm.close();
        super.onCleared();
    }

    public void updateTemporaryFeed(TemporaryFeed temporaryFeed, SharedPreferences preferences) {
        if(temporaryFeedLiveData == null) {
            temporaryFeedLiveData = new LiveRealmObject<>(temporaryFeed);
            itemsLiveData = new LiveRealmResults<>(getSortedItems(temporaryFeed, preferences));
        } else {
            temporaryFeedLiveData.setValue(temporaryFeed);
            itemsLiveData.setValue(getSortedItems(temporaryFeed, preferences));
        }
    }

    private RealmResults<Item> getSortedItems(TemporaryFeed temporaryFeed, SharedPreferences preferences) {
        return temporaryFeed.getItems().sort(Preferences.SORT_FIELD.getString(preferences), Preferences.ORDER.getOrder(preferences));
    }

    public Realm getRealm() {
        return realm;
    }
}
