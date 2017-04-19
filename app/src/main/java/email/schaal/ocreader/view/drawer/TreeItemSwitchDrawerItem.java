/*
 * Copyright © 2017. Daniel Schaal <daniel@schaal.email>
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

package email.schaal.ocreader.view.drawer;

import com.mikepenz.materialdrawer.model.SwitchDrawerItem;

import email.schaal.ocreader.database.model.TreeIconable;
import email.schaal.ocreader.database.model.TreeItem;

/**
 * Created by daniel on 19.04.17.
 */

class TreeItemSwitchDrawerItem extends SwitchDrawerItem {
    TreeItemSwitchDrawerItem(TreeItem item) {
        if(item instanceof TreeIconable) {
            withIcon(((TreeIconable) item).getIcon());
        }

        withName(item.getName());
        withTag(item);
        withIconTintingEnabled(true);
    }
}
