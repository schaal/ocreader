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
import java.util.*
import kotlin.math.min

/**
 * @author amulya
 */
class TextDrawable private constructor(builder: Builder) : ShapeDrawable(builder.shape) {
    private val textPaint: Paint
    private val text: String
    private val height: Int
    private val width: Int
    private val fontSize: Int
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

    class Builder(text: String, @ColorInt color: Int) {
        val text: String
        @ColorInt
        var color: Int
        val borderThickness: Int
        var width: Int
        var height: Int
        val shape: Shape
        @ColorInt
        var textColor: Int
        val fontSize: Int
        val isBold: Boolean
        val toUpperCase: Boolean
        fun textColor(@ColorInt textColor: Int): Builder {
            this.textColor = textColor
            return this
        }

        fun color(@ColorInt color: Int): Builder {
            this.color = color
            return this
        }

        fun width(width: Int): Builder {
            this.width = width
            return this
        }

        fun height(height: Int): Builder {
            this.height = height
            return this
        }

        fun build(): TextDrawable {
            return TextDrawable(this)
        }

        init {
            textColor = Color.WHITE
            borderThickness = 0
            width = -1
            height = -1
            shape = OvalShape()
            fontSize = -1
            isBold = false
            toUpperCase = true
            this.text = text
            this.color = color
        }
    }

    init {
        // shape properties
        height = builder.height
        width = builder.width
        // text and color
        text = if (builder.toUpperCase) builder.text.toUpperCase(Locale.getDefault()) else builder.text
        // text paint settings
        fontSize = builder.fontSize
        textPaint = Paint()
        textPaint.color = builder.textColor
        textPaint.isAntiAlias = true
        textPaint.isFakeBoldText = builder.isBold
        textPaint.style = Paint.Style.FILL
        textPaint.typeface = Typeface.DEFAULT_BOLD
        textPaint.textAlign = Paint.Align.CENTER
        textPaint.strokeWidth = builder.borderThickness.toFloat()
        // drawable paint color
        val paint = paint
        paint.color = builder.color
    }
}