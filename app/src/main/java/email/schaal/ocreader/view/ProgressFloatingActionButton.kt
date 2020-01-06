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
    private var progress = 0f
    private val diameter: Float
    @Keep
    fun getProgress(): Float {
        return progress
    }

    // Used by animator in ItemPagerActivity
    @Keep
    fun setProgress(progress: Float) {
        this.progress = progress
        invalidate()
    }

    @Keep
    fun setFabBackgroundColor(color: Int) {
        val colorStateList = ColorStateList.valueOf(color)
        backgroundTintList = colorStateList
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
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.ProgressFloatingActionButton)
        try {
            circlePaint.color = typedArray.getColor(R.styleable.ProgressFloatingActionButton_progressColor, 0)
        } finally {
            typedArray.recycle()
        }
        diameter = resources.getDimensionPixelSize(R.dimen.fab_size_normal).toFloat()
    }
}