package email.schaal.ocreader.view;

import android.annotation.TargetApi;
import android.content.Context;
import android.support.v4.view.NestedScrollingChild;
import android.support.v4.view.NestedScrollingChildHelper;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.webkit.WebView;

/**
 * Workaround until WebView supports nested scrolling (according to
 * http://b.android.com/201385#c3 probably not any time soon)
 */
public class NestedScrollWebView extends WebView implements NestedScrollingChild, GestureDetector.OnGestureListener {
    private final NestedScrollingChildHelper helper = new NestedScrollingChildHelper(this);

    private final GestureDetector gestureDetector = new GestureDetector(getContext(), this);
    public NestedScrollWebView(Context context) {
        super(context);

    }

    public NestedScrollWebView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public NestedScrollWebView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @TargetApi(21)
    public NestedScrollWebView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event){
        final boolean handled = gestureDetector.onTouchEvent(event);
        if (!handled && event.getAction() == MotionEvent.ACTION_UP) {
            stopNestedScroll();
        }
        return super.onTouchEvent(event);
    }

    @Override
    public void setNestedScrollingEnabled(boolean enabled) {
        helper.setNestedScrollingEnabled(enabled);
    }

    @Override
    public boolean isNestedScrollingEnabled() {
        return helper.isNestedScrollingEnabled();
    }

    @Override
    public boolean startNestedScroll(int axes) {
        return helper.startNestedScroll(axes);
    }

    @Override
    public void stopNestedScroll() {
        helper.stopNestedScroll();
    }

    @Override
    public boolean hasNestedScrollingParent() {
        return helper.hasNestedScrollingParent();
    }

    @Override
    public boolean dispatchNestedScroll(int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed, int[] offsetInWindow) {
        return helper.dispatchNestedScroll(dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed, offsetInWindow);
    }

    @Override
    public boolean dispatchNestedPreScroll(int dx, int dy, int[] consumed, int[] offsetInWindow) {
        return helper.dispatchNestedPreScroll(dx, dy, consumed, offsetInWindow);
    }

    @Override
    public boolean dispatchNestedFling(float velocityX, float velocityY, boolean consumed) {
        return helper.dispatchNestedFling(velocityX, velocityY, consumed);
    }

    @Override
    public boolean dispatchNestedPreFling(float velocityX, float velocityY) {
        return helper.dispatchNestedPreFling(velocityX, velocityY);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        helper.onDetachedFromWindow();
    }

    @Override
    public boolean onDown(MotionEvent e) {
        startNestedScroll(ViewCompat.SCROLL_AXIS_VERTICAL);
        return true;
    }

    @Override
    public void onShowPress(MotionEvent e) {

    }

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        return false;
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        dispatchNestedPreScroll(0, (int) distanceY, null, null);
        if(Math.abs(distanceY) < 10.0f || Math.abs(distanceX) > 10.0f)
            distanceY = 0.0f;
        dispatchNestedScroll(0, (int) distanceY, 0, 0, null);
        return true;
    }

    @Override
    public void onLongPress(MotionEvent e) {

    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        return true;
    }
}
