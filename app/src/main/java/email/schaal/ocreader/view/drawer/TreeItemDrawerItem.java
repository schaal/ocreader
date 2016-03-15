/*
 * Copyright (C) 2016 Daniel Schaal <daniel@schaal.email>
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

import android.view.View;
import android.widget.ImageView;

import com.mikepenz.materialdrawer.holder.ImageHolder;
import com.mikepenz.materialdrawer.holder.StringHolder;
import com.mikepenz.materialdrawer.model.BaseViewHolder;
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem;

import email.schaal.ocreader.R;
import email.schaal.ocreader.model.Feed;
import email.schaal.ocreader.model.Folder;
import email.schaal.ocreader.model.TreeIconable;
import email.schaal.ocreader.model.TreeItem;

/**
 * Represents a TreeItem for display in a Drawer
 */
public class TreeItemDrawerItem extends PrimaryDrawerItem {
    public TreeItemDrawerItem(TreeItem item) {
        if(item instanceof TreeIconable) {
            withIcon(((TreeIconable) item).getIcon());
        } else if(item instanceof Feed) {
            if(((Feed) item).getFaviconLink() != null)
                withIcon(((Feed) item).getFaviconLink());
            else
                withIcon(R.drawable.ic_feed_icon);
        } else if(item instanceof Folder) {
            withIcon(R.drawable.ic_folder);
        }
        withName(item.getTitle());
        withTag(item);
        withIconTintingEnabled(true);
    }

    @Override
    protected void bindViewHelper(BaseViewHolder viewHolder) {
        super.bindViewHelper(viewHolder);

        if (icon != null && icon.getUri() != null) {
            ImageView imageView = (ImageView) viewHolder.itemView.findViewById(com.mikepenz.materialdrawer.R.id.material_drawer_icon);
            imageView.setVisibility(View.VISIBLE);
            ImageHolder.applyTo(icon, imageView);
        }
    }

    public TreeItemDrawerItem withIcon(String url) {
        icon = new ImageHolder(url);
        return this;
    }

    @Override
    public TreeItemDrawerItem withBadge(int badge) {
        StringHolder newBadge;
        if(badge > 0)
            newBadge = new StringHolder(String.valueOf(badge));
        else
            newBadge = new StringHolder("");
        return (TreeItemDrawerItem) super.withBadge(newBadge);
    }
}
