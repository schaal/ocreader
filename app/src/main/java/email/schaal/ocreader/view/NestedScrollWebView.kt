package email.schaal.ocreader.view

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.webkit.WebView
import androidx.core.view.NestedScrollingChild
import androidx.core.view.NestedScrollingChildHelper
import androidx.core.view.ViewCompat
import kotlin.math.abs

/**
 * Workaround until WebView supports nested scrolling (according to
 * http://b.android.com/201385#c3 probably not any time soon)
 */
open class NestedScrollWebView(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : WebView(context, attrs, defStyleAttr), NestedScrollingChild {
    private val helper = NestedScrollingChildHelper(rootView)
    private val gestureDetector = GestureDetector(context, ScrollGestureListener())

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context) : this(context, null)

    private inner class ScrollGestureListener: GestureDetector.SimpleOnGestureListener() {

        override fun onSingleTapUp(e: MotionEvent?): Boolean {
            return false
        }

        override fun onDown(e: MotionEvent?): Boolean {
            startNestedScroll(ViewCompat.SCROLL_AXIS_VERTICAL)
            return true
        }

        override fun onScroll(e1: MotionEvent, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
            dispatchNestedPreScroll(0, distanceY.toInt(), null, null)

            val newDistanceY: Float = if (abs(distanceY) < 10.0f || abs(distanceX) > 10.0f)
                0.0f
            else
                distanceY

            dispatchNestedScroll(0, newDistanceY.toInt(), 0, 0, null)

            return true
        }

        override fun onFling(e1: MotionEvent?, e2: MotionEvent?, velocityX: Float, velocityY: Float): Boolean {
            return true
        }
    }


    init {
        ViewCompat.setNestedScrollingEnabled(rootView, true)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val handled = gestureDetector.onTouchEvent(event)
        if (!handled && event.action == MotionEvent.ACTION_UP) {
            stopNestedScroll()
        }
        return super.onTouchEvent(event)
    }

    override fun setNestedScrollingEnabled(enabled: Boolean) {
        helper.isNestedScrollingEnabled = enabled
    }

    override fun isNestedScrollingEnabled(): Boolean {
        return helper.isNestedScrollingEnabled
    }

    override fun startNestedScroll(axes: Int): Boolean {
        return helper.startNestedScroll(axes)
    }

    override fun stopNestedScroll() {
        helper.stopNestedScroll()
    }

    override fun hasNestedScrollingParent(): Boolean {
        return helper.hasNestedScrollingParent()
    }

    override fun dispatchNestedScroll(dxConsumed: Int, dyConsumed: Int, dxUnconsumed: Int, dyUnconsumed: Int, offsetInWindow: IntArray?): Boolean {
        return helper.dispatchNestedScroll(dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed, offsetInWindow)
    }

    override fun dispatchNestedPreScroll(dx: Int, dy: Int, consumed: IntArray?, offsetInWindow: IntArray?): Boolean {
        return helper.dispatchNestedPreScroll(dx, dy, consumed, offsetInWindow)
    }

    override fun dispatchNestedFling(velocityX: Float, velocityY: Float, consumed: Boolean): Boolean {
        return helper.dispatchNestedFling(velocityX, velocityY, consumed)
    }

    override fun dispatchNestedPreFling(velocityX: Float, velocityY: Float): Boolean {
        return helper.dispatchNestedPreFling(velocityX, velocityY)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        helper.onDetachedFromWindow()
    }
}