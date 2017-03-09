package email.schaal.ocreader.view;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
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

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
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

    public void show() {
        if(getVisibility() == INVISIBLE)
            startAnimation(showAnimation);
    }

    public void hide() {
        if(getVisibility() == VISIBLE)
            startAnimation(hideAnimation);
    }
}
