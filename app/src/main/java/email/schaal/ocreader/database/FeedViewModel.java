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

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import email.schaal.ocreader.Preferences;
import email.schaal.ocreader.database.model.AllUnreadFolder;
import email.schaal.ocreader.database.model.Folder;
import email.schaal.ocreader.database.model.FreshFolder;
import email.schaal.ocreader.database.model.Item;
import email.schaal.ocreader.database.model.StarredFolder;
import email.schaal.ocreader.database.model.TemporaryFeed;
import email.schaal.ocreader.database.model.TreeItem;
import io.realm.Realm;

public class FeedViewModel extends ViewModel {
    private final Realm realm;
    private final MutableLiveData<TemporaryFeed> temporaryFeedLiveData;
    private final MutableLiveData<List<Item>> itemsLiveData;
    private final MutableLiveData<List<Folder>> foldersLiveData;
    private final MutableLiveData<TreeItem> selectedTreeItemLiveData;

    private Map<Long, TreeItem> topFolderMap;

    public FeedViewModel(final Context context) {
        realm = Realm.getDefaultInstance();

        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        final TemporaryFeed temporaryFeed = TemporaryFeed.getListTemporaryFeed(realm);

        temporaryFeedLiveData = new LiveRealmObject<>(temporaryFeed);
        itemsLiveData = new LiveRealmResults<>(temporaryFeed.getItems().sort(Preferences.SORT_FIELD.getString(preferences), Preferences.ORDER.getOrder(preferences)));
        foldersLiveData = new LiveRealmResults<>(Folder.getAll(realm, Preferences.SHOW_ONLY_UNREAD.getBoolean(preferences)));

        topFolderMap = new HashMap<>(3);
        topFolderMap.put(AllUnreadFolder.ID, new AllUnreadFolder(context));
        topFolderMap.put(StarredFolder.ID, new StarredFolder(context));
        topFolderMap.put(FreshFolder.ID, new FreshFolder(context));
        selectedTreeItemLiveData = new MutableLiveData<>(topFolderMap.get(AllUnreadFolder.ID));
    }

    public LiveData<TemporaryFeed> getTemporaryFeed() {
        return temporaryFeedLiveData;
    }
    public LiveData<List<Item>> getItems() {
        return itemsLiveData;
    }
    public LiveData<List<Folder>> getFolders() {
        return foldersLiveData;
    }
    public LiveData<TreeItem> getSelectedTreeItem() {
        return selectedTreeItemLiveData;
    }

    public TreeItem getTopTreeItem(long id) {
        return topFolderMap.get(id);
    }

    public Collection<TreeItem> getTopFolderList() {
        return topFolderMap.values();
    }

    @Override
    protected void onCleared() {
        realm.close();
        super.onCleared();
    }

    public void updateFolders(final boolean onlyUnread) {
        foldersLiveData.setValue(Folder.getAll(realm, onlyUnread));
    }

    public void updateSelectedTreeItem(TreeItem treeItem) {
        selectedTreeItemLiveData.setValue(treeItem);
    }

    public void updateTemporaryFeed(final SharedPreferences preferences, final boolean updateTemporaryFeed) {
        final TemporaryFeed temporaryFeed = temporaryFeedLiveData.getValue();
        final TreeItem selectedTreeItem = selectedTreeItemLiveData.getValue();
        if(temporaryFeed == null || selectedTreeItem == null)
            return;

        if (updateTemporaryFeed || temporaryFeed.getTreeItemId() != selectedTreeItem.getId()) {
            realm.executeTransaction(realm -> {
                final List<Item> tempItems = selectedTreeItem.getItems(realm, Preferences.SHOW_ONLY_UNREAD.getBoolean(preferences));
                temporaryFeed.setTreeItemId(selectedTreeItem.getId());
                temporaryFeed.setName(selectedTreeItem.getName());
                temporaryFeed.getItems().clear();
                if (tempItems != null) {
                    temporaryFeed.getItems().addAll(tempItems);
                }
            });
        }

        itemsLiveData.setValue(temporaryFeed.getItems().sort(Preferences.SORT_FIELD.getString(preferences), Preferences.ORDER.getOrder(preferences)));
    }

    public static class FeedViewModelFactory implements ViewModelProvider.Factory {
        private final Context context;

        public FeedViewModelFactory(final Context context) {
            this.context = context;
        }

        @NonNull
        @Override
        public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
            if(modelClass == FeedViewModel.class) {
                final FeedViewModel feedViewModel = new FeedViewModel(context);
                //noinspection unchecked
                return (T) feedViewModel;
            }
            //noinspection ConstantConditions
            return null;
        }
    }
}
