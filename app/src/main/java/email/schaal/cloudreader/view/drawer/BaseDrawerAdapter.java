/*
 * Copyright (C) 2015 Daniel Schaal <daniel@schaal.email>
 *
 * This file is part of Cloudreader.
 *
 * Cloudreader is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Cloudreader is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Cloudreader.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package email.schaal.cloudreader.view.drawer;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.mikepenz.materialdrawer.adapter.DrawerAdapter;
import com.mikepenz.materialdrawer.holder.StringHolder;
import com.mikepenz.materialdrawer.model.interfaces.Badgeable;
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.ListIterator;

import email.schaal.cloudreader.database.Queries;
import email.schaal.cloudreader.model.TreeItem;
import io.realm.Realm;

/**
 * Base class for the DrawerAdapter used by the Drawers in {@link email.schaal.cloudreader.ListActivity}
 */
abstract class BaseDrawerAdapter extends DrawerAdapter {
    public final void reload(boolean showOnlyUnread) {
        Realm realm = null;
        try {
            realm = Realm.getDefaultInstance();
            clearDrawerItems();
            setDrawerItems(reloadDrawerItems(realm, showOnlyUnread));
        } finally {
            if (realm != null)
                realm.close();
        }
    }

    protected abstract ArrayList<IDrawerItem> reloadDrawerItems(Realm realm, boolean showOnlyUnread);

    public void updateUnreadCount(boolean showOnlyUnread) {
        Realm realm = null;
        try {
            realm = Realm.getDefaultInstance();

            ListIterator<IDrawerItem> itemIterator = getDrawerItems().listIterator();

            while (itemIterator.hasNext()) {
                int position = itemIterator.nextIndex() + getHeaderItemCount();
                IDrawerItem drawerItem = itemIterator.next();
                if (drawerItem instanceof Badgeable) {
                    Badgeable badgeable = (Badgeable) drawerItem;
                    Integer count = Queries.getInstance().getCount(realm, (TreeItem) drawerItem.getTag());

                    if (count <= 0) {
                        if (showOnlyUnread) {
                            itemIterator.remove();
                            notifyItemRemoved(position);
                        } else {
                            updateBadge(badgeable, null, position);
                        }
                    } else {
                        updateBadge(badgeable, String.valueOf(count), position);
                    }
                }
            }
        } finally {
            if (realm != null)
                realm.close();
        }
    }

    private boolean compareBadges(@Nullable StringHolder lhs, @Nullable StringHolder rhs) {
        String l = lhs != null ? lhs.getText() : null;
        String r = rhs != null ? rhs.getText() : null;

        return l == null ? r == null : l.equals(r);
    }

    private void updateBadge(@NonNull Badgeable badgeable, @Nullable String badgeString, int position) {
        StringHolder oldBadge = badgeable.getBadge();
        StringHolder newBadge = new StringHolder(badgeString);
        if (!compareBadges(oldBadge, newBadge)) {
            badgeable.withBadge(newBadge);
            notifyItemChanged(position);
        }
    }
}
