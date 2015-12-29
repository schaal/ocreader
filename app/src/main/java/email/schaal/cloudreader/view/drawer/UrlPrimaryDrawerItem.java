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

import android.view.View;
import android.widget.ImageView;

import com.mikepenz.materialdrawer.holder.ImageHolder;
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem;

/**
 * PrimaryDrawerItem with ability to specify an image url as an Icon.
 */
class UrlPrimaryDrawerItem extends PrimaryDrawerItem {
    @Override
    protected void bindViewHelper(BaseViewHolder viewHolder) {
        super.bindViewHelper(viewHolder);

        if (icon != null && icon.getUri() != null) {
            ImageView imageView = (ImageView) viewHolder.itemView.findViewById(com.mikepenz.materialdrawer.R.id.material_drawer_icon);
            imageView.setVisibility(View.VISIBLE);
            ImageHolder.applyTo(icon, imageView);
        }
    }

    public UrlPrimaryDrawerItem withIcon(String url) {
        icon = new ImageHolder(url);
        return this;
    }
}
