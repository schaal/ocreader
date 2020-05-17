package email.schaal.ocreader.view

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import androidx.annotation.Keep
import com.google.android.material.floatingactionbutton.FloatingActionButton
import email.schaal.ocreader.R

/**
 * FloatingActionButton with ability to show progress
 */
class ProgressFloatingActionButton(context: Context, attrs: AttributeSet?) : FloatingActionButton(context, attrs) {
    private val circlePaint = Paint()

    var progress = 0f
        @Keep
        set(value) {
            field = value
            invalidate()
        }

    private val diameter: Float = resources.getDimensionPixelSize(R.dimen.fab_size_normal).toFloat()

    @Keep
    fun setFabBackgroundColor(color: Int) {
        backgroundTintList = ColorStateList.valueOf(color)
    }

    override fun onDraw(canvas: Canvas) {
        val radius = diameter / 2
        val count = canvas.save()
        // draw progress circle fraction
        canvas.clipRect(0f, diameter * (1 - progress), diameter, diameter)
        canvas.drawCircle(radius, radius, radius, circlePaint)
        canvas.restoreToCount(count)
        super.onDraw(canvas)
    }

    init {
        context.obtainStyledAttributes(attrs, R.styleable.ProgressFloatingActionButton).also {
            circlePaint.color = it.getColor(R.styleable.ProgressFloatingActionButton_progressColor, 0)
        }.recycle()
    }
}