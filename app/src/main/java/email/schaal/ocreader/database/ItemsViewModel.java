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
import androidx.lifecycle.ViewModel;

import java.util.List;

import email.schaal.ocreader.Preferences;
import email.schaal.ocreader.database.model.Item;
import email.schaal.ocreader.database.model.TemporaryFeed;
import email.schaal.ocreader.view.drawer.DrawerManager;
import io.realm.Realm;
import io.realm.RealmResults;

public class ItemsViewModel extends ViewModel {
    private final Realm realm;
    private final LiveData<List<Item>> items;

    public ItemsViewModel(final DrawerManager.State state, final boolean updateTemporaryFeed, final SharedPreferences preferences) {
        realm = Realm.getDefaultInstance();
        items = updateItems(state, updateTemporaryFeed, preferences);
    }

    private boolean isOnlyUnread(SharedPreferences preferences) {
        return Preferences.SHOW_ONLY_UNREAD.getBoolean(preferences);
    }

    private LiveRealmResults<Item> updateItems(final DrawerManager.State state, boolean updateTemporaryFeed, SharedPreferences preferences) {
        if(state.getTreeItem() == null)
            return null;

        final TemporaryFeed temporaryFeed = TemporaryFeed.getListTemporaryFeed(realm);

        if (updateTemporaryFeed || temporaryFeed.getTreeItemId() != state.getTreeItem().getId()) {
            realm.executeTransaction(realm -> {
                List<Item> tempItems = state.getTreeItem().getItems(realm, isOnlyUnread(preferences));
                temporaryFeed.setTreeItemId(state.getTreeItem().getId());
                temporaryFeed.setName(state.getTreeItem().getName());
                temporaryFeed.getItems().clear();
                if (tempItems != null) {
                    temporaryFeed.getItems().addAll(tempItems);
                }
            });
        }

        return new LiveRealmResults<>(temporaryFeed.getItems().sort(Preferences.SORT_FIELD.getString(preferences), Preferences.ORDER.getOrder(preferences)));
    }

    public LiveData<List<Item>> getItems() {
        return items;
    }

    @Override
    protected void onCleared() {
        realm.close();
        super.onCleared();
    }
}
