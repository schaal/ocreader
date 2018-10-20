package email.schaal.ocreader.view;

import android.content.Context;
import android.content.res.ColorStateList;

import androidx.annotation.Keep;
import androidx.annotation.Nullable;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.LinearLayout;

import email.schaal.ocreader.R;

/**
 * Created by daniel on 09.03.17.
 */

public class FABLayout extends LinearLayout {
    private Animation hideAnimation;
    private Animation showAnimation;

    public FABLayout(Context context) {
        super(context);
    }

    public FABLayout(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public FABLayout(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public FABLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    private void init() {

        hideAnimation = AnimationUtils.loadAnimation(getContext(), R.anim.fablayout_hide);
        hideAnimation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                animation.reset();
                clearAnimation();
                setVisibility(INVISIBLE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });

        showAnimation = AnimationUtils.loadAnimation(getContext(), R.anim.fablayout_show);
        showAnimation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                setVisibility(VISIBLE);
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                animation.reset();
                clearAnimation();
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
    }

    @Keep
    public void setFabBackgroundColor(int color) {
        final ColorStateList colorStateList = ColorStateList.valueOf(color);
        for (int i = 0; i < getChildCount(); i++) {
            View view = getChildAt(i);
            if (view instanceof FloatingActionButton)
                view.setBackgroundTintList(colorStateList);
        }
    }

    public void show() {
        if(getVisibility() == INVISIBLE)
            startAnimation(showAnimation);
    }

    public void hide() {
        if(getVisibility() == VISIBLE)
            startAnimation(hideAnimation);
    }
}
