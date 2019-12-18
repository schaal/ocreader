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
import email.schaal.ocreader.database.model.TreeItem;
import io.realm.Realm;

public class FeedViewModel extends ViewModel {
    private final Realm realm;
    private final MutableLiveData<TemporaryFeed> temporaryFeedLiveData;
    private final MutableLiveData<List<Item>> itemsLiveData;

    public FeedViewModel() {
        realm = Realm.getDefaultInstance();
        temporaryFeedLiveData = new LiveRealmObject<>(TemporaryFeed.getListTemporaryFeed(realm));
        itemsLiveData = new LiveRealmResults<>(realm.where(Item.class).alwaysFalse().findAll());
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

    public void updateTemporaryFeed(final SharedPreferences preferences, final boolean updateTemporaryFeed, final TreeItem treeItem) {
        final TemporaryFeed temporaryFeed = temporaryFeedLiveData.getValue();
        if(temporaryFeed == null)
            return;

        if (updateTemporaryFeed || temporaryFeed.getTreeItemId() != treeItem.getId()) {
            realm.executeTransaction(realm -> {
                final List<Item> tempItems = treeItem.getItems(realm, Preferences.SHOW_ONLY_UNREAD.getBoolean(preferences));
                temporaryFeed.setTreeItemId(treeItem.getId());
                temporaryFeed.setName(treeItem.getName());
                temporaryFeed.getItems().clear();
                if (tempItems != null) {
                    temporaryFeed.getItems().addAll(tempItems);
                }
            });
        }

        itemsLiveData.setValue(temporaryFeed.getItems().sort(Preferences.SORT_FIELD.getString(preferences), Preferences.ORDER.getOrder(preferences)));
    }
}
