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
package email.schaal.ocreader.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.view.View
import androidx.annotation.DimenRes
import androidx.core.content.res.use
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ItemDecoration
import email.schaal.ocreader.R

class DividerItemDecoration(context: Context, @DimenRes insetRes: Int) : ItemDecoration() {
    private val mDivider: Drawable?
    private val inset: Int = context.resources.getDimensionPixelSize(insetRes)
    private var size = 0

    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView,
                                state: RecyclerView.State) {
        super.getItemOffsets(outRect, view, parent, state)
        if (mDivider == null || parent.getChildLayoutPosition(view) < 1) {
            return
        }
        outRect.top = size
    }

    private val dividerRect = Rect(0, 0, 0, 0)
    override fun onDraw(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        if (mDivider == null) return
        val layoutManager = parent.layoutManager
        check(!(layoutManager !is LinearLayoutManager ||
                layoutManager.orientation != RecyclerView.VERTICAL)) { "DividerItemDecoration can only be used with a vertical LinearLayoutManager." }
        val childCount = parent.childCount
        val paddingLeft = parent.paddingLeft
        val paddingLeftInset = paddingLeft + inset
        dividerRect.right = parent.width - parent.paddingRight
        loop@ for (i in 0 until childCount) {
            val child = parent.getChildAt(i)
            when (layoutManager.getItemViewType(child)) {
                R.id.viewtype_item -> if (i == childCount - 1 || i == childCount - 2 && layoutManager.getItemViewType(parent.getChildAt(i + 1)) != R.id.viewtype_item) dividerRect.left = paddingLeft else dividerRect.left = paddingLeftInset
                R.id.viewtype_empty, R.id.viewtype_loadmore -> dividerRect.left = paddingLeft
                R.id.viewtype_error -> continue@loop
            }
            dividerRect.top = child.bottom + (child.layoutParams as RecyclerView.LayoutParams).bottomMargin
            dividerRect.bottom = dividerRect.top + size
            mDivider.bounds = dividerRect
            mDivider.draw(c)
        }
    }

    init {
        mDivider = context.obtainStyledAttributes(intArrayOf(android.R.attr.listDivider)).use {
            it.getDrawable(0)
        }
        if (mDivider != null) size = mDivider.intrinsicHeight
    }
}