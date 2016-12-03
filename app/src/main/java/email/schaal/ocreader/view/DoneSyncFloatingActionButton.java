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

package email.schaal.ocreader.view;

import android.content.Context;
import android.graphics.drawable.TransitionDrawable;
import android.support.design.widget.FloatingActionButton;
import android.util.AttributeSet;

/**
 * FloatingActionButton that toggles between two states when clicked
 */
public class DoneSyncFloatingActionButton extends FloatingActionButton {
    private static final int TRANSITION_DURATION = 150;

    private boolean sync;
    private TransitionDrawable drawable;

    public DoneSyncFloatingActionButton(Context context) {
        super(context);
        init();
    }

    public DoneSyncFloatingActionButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public DoneSyncFloatingActionButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public boolean isSync() {
        return sync;
    }

    private void init() {
        drawable = (TransitionDrawable) getDrawable();
        drawable.setCrossFadeEnabled(true);
    }


    public void setSync(boolean sync) {
        if(sync != this.sync) {
            if(sync)
                drawable.startTransition(TRANSITION_DURATION);
            else
                drawable.reverseTransition(TRANSITION_DURATION);

            this.sync = sync;
        }
    }

    public void toggleSync() {
        setSync(!sync);
    }
}
