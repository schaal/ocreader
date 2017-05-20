/*
 * Copyright (C) 2015-2016 Daniel Schaal <daniel@schaal.email>
 *
 * This file is part of OCReader.
 *
 * OCReader is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * OCReader is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with OCReader.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package email.schaal.ocreader.view.drawer;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.mikepenz.materialdrawer.Drawer;
import com.mikepenz.materialdrawer.holder.StringHolder;
import com.mikepenz.materialdrawer.model.interfaces.Badgeable;
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem;

import java.util.List;

import email.schaal.ocreader.database.model.TreeItem;
import io.realm.Realm;

/**
 * Base class for the DrawerAdapter used by the Drawers in {@link email.schaal.ocreader.ListActivity}
 */
abstract class BaseDrawerManager {
    private final Drawer drawer;

    public BaseDrawerManager(Drawer drawer) {
        this.drawer = drawer;
    }

    public final void reload(Realm realm, boolean showOnlyUnread) {
        drawer.setItems(reloadDrawerItems(realm, showOnlyUnread));
    }

    protected abstract List<IDrawerItem> reloadDrawerItems(Realm realm, boolean showOnlyUnread);

    public void updateUnreadCount(Realm realm, boolean showOnlyUnread) {
        if(showOnlyUnread) {
            reload(realm, true);
        } else {
            for (IDrawerItem drawerItem : drawer.getDrawerItems()) {
                if (drawerItem instanceof Badgeable && drawerItem.getTag() instanceof TreeItem) {
                    Badgeable badgeable = (Badgeable) drawerItem;
                    Integer count = ((TreeItem) drawerItem.getTag()).getCount(realm);

                    if (count <= 0) {
                        updateBadge(badgeable, null);
                    } else {
                        updateBadge(badgeable, String.valueOf(count));
                    }
                }
            }
        }
    }

    private boolean compareBadges(@Nullable StringHolder lhs, @Nullable StringHolder rhs) {
        String l = lhs != null ? lhs.getText() : null;
        String r = rhs != null ? rhs.getText() : null;

        return l == null ? r == null : l.equals(r);
    }

    private void updateBadge(@NonNull Badgeable badgeable, @Nullable String badgeString) {
        StringHolder oldBadge = badgeable.getBadge();
        StringHolder newBadge = new StringHolder(badgeString);
        if (!compareBadges(oldBadge, newBadge)) {
            badgeable.withBadge(newBadge);
            drawer.updateItem((IDrawerItem)badgeable);
        }
    }
}
