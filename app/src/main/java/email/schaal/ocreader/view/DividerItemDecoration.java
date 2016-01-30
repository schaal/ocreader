/*
 * Copyright (C) 2015 Daniel Schaal <daniel@schaal.email>
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

package email.schaal.ocreader.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;

public class DividerItemDecoration extends RecyclerView.ItemDecoration {

    private final Drawable mDivider;

    private int size = 0;

    public DividerItemDecoration(Context context) {
        final TypedArray a = context
                .obtainStyledAttributes(new int[]{android.R.attr.listDivider});
        mDivider = a.getDrawable(0);
        a.recycle();
        if (mDivider != null)
            this.size = mDivider.getIntrinsicHeight();
    }

    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent,
                               RecyclerView.State state) {
        super.getItemOffsets(outRect, view, parent, state);
        if (mDivider == null || parent.getChildLayoutPosition(view) < 1) {
            return;
        }

        outRect.top = size;
    }

    private final Rect dividerRect = new Rect(0,0,0,0);

    @Override
    public void onDraw(Canvas c, RecyclerView parent, RecyclerView.State state) {
        if (mDivider == null)
            return;

        if (!(parent.getLayoutManager() instanceof LinearLayoutManager) ||
                ((LinearLayoutManager) parent.getLayoutManager()).getOrientation() != LinearLayoutManager.VERTICAL) {
            throw new IllegalStateException(
                    "DividerItemDecoration can only be used with a vertical LinearLayoutManager.");
        }

        int childCount = parent.getChildCount();

        dividerRect.left = parent.getPaddingLeft();
        dividerRect.right = parent.getWidth() - parent.getPaddingRight();

        for (int i = 0; i < childCount; i++) {
            View child = parent.getChildAt(i);
            RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) child.getLayoutParams();

            dividerRect.top = child.getBottom() + params.bottomMargin;
            dividerRect.bottom = dividerRect.top + size;

            mDivider.setBounds(dividerRect);
            mDivider.draw(c);
        }
    }
}