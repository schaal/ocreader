/*
  The MIT License (MIT)

  Copyright (c) 2014 Amulya Khare

  Permission is hereby granted, free of charge, to any person obtaining a copy
  of this software and associated documentation files (the "Software"), to deal
  in the Software without restriction, including without limitation the rights
  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
  copies of the Software, and to permit persons to whom the Software is
  furnished to do so, subject to the following conditions:

  The above copyright notice and this permission notice shall be included in all
  copies or substantial portions of the Software.

  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
  SOFTWARE.
 */
package email.schaal.ocreader.util

import android.graphics.*
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.OvalShape
import android.graphics.drawable.shapes.Shape
import androidx.annotation.ColorInt
import java.util.Locale
import kotlin.math.min

/**
 * @author amulya
 */
class TextDrawable constructor(
        origText: String,
        @ColorInt private val color: Int,
        @ColorInt private val textColor: Int,
        private val borderThickness: Int = 0,
        private val width: Int = -1,
        private val height: Int = -1,
        shape: Shape = OvalShape(),
        private val fontSize: Int = -1,
        bold: Boolean = false,
        upperCase: Boolean = true

) : ShapeDrawable(shape) {
    private val text: String = origText.substring(0, 1).ifEmpty { "?" }.let {
        if(upperCase)
            it.toUpperCase(Locale.getDefault())
        else
            it
    }

    private val textPaint: Paint = Paint().apply {
        // text paint settings
        color = textColor
        isAntiAlias = true
        isFakeBoldText = bold
        style = Paint.Style.FILL
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
        strokeWidth = borderThickness.toFloat()

    }

    init {
        // drawable paint color
        paint.color = color
    }

    override fun draw(canvas: Canvas) {
        super.draw(canvas)
        val r = bounds
        val count = canvas.save()
        canvas.translate(r.left.toFloat(), r.top.toFloat())
        // draw text
        val width = if (width < 0) r.width() else width
        val height = if (height < 0) r.height() else height
        val fontSize = if (fontSize < 0) min(width, height) / 2 else fontSize
        textPaint.textSize = fontSize.toFloat()
        canvas.drawText(text, width / 2.toFloat(), height / 2 - (textPaint.descent() + textPaint.ascent()) / 2, textPaint)
        canvas.restoreToCount(count)
    }

    override fun setAlpha(alpha: Int) {
        textPaint.alpha = alpha
    }

    override fun setColorFilter(cf: ColorFilter?) {
        textPaint.colorFilter = cf
    }

    override fun getOpacity(): Int {
        return PixelFormat.TRANSLUCENT
    }

    override fun getIntrinsicWidth(): Int {
        return width
    }

    override fun getIntrinsicHeight(): Int {
        return height
    }
}