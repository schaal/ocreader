/**
 * The MIT License (MIT)
 *
 * Copyright (c) 2014 Amulya Khare
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package email.schaal.ocreader.util;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.graphics.drawable.shapes.Shape;

import java.util.Locale;

/**
 * @author amulya
 */
public class TextDrawable extends ShapeDrawable {

    private final Paint textPaint;
    private final String text;
    private final int color;
    private final int height;
    private final int width;
    private final int fontSize;

    private TextDrawable(Builder builder) {
        super(builder.shape);

        // shape properties
        height = builder.height;
        width = builder.width;

        // text and color
        text = builder.toUpperCase ? builder.text.toUpperCase(Locale.getDefault()) : builder.text;
        color = builder.color;

        // text paint settings
        fontSize = builder.fontSize;
        textPaint = new Paint();
        textPaint.setColor(builder.textColor);
        textPaint.setAntiAlias(true);
        textPaint.setFakeBoldText(builder.isBold);
        textPaint.setStyle(Paint.Style.FILL);
        textPaint.setTypeface(Typeface.DEFAULT_BOLD);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setStrokeWidth(builder.borderThickness);

        // drawable paint color
        Paint paint = getPaint();
        paint.setColor(color);

    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);
        Rect r = getBounds();

        int count = canvas.save();
        canvas.translate(r.left, r.top);

        // draw text
        int width = this.width < 0 ? r.width() : this.width;
        int height = this.height < 0 ? r.height() : this.height;
        int fontSize = this.fontSize < 0 ? (Math.min(width, height) / 2) : this.fontSize;
        textPaint.setTextSize(fontSize);
        canvas.drawText(text, width / 2, height / 2 - ((textPaint.descent() + textPaint.ascent()) / 2), textPaint);

        canvas.restoreToCount(count);

    }

    @Override
    public void setAlpha(int alpha) {
        textPaint.setAlpha(alpha);
    }

    @Override
    public void setColorFilter(ColorFilter cf) {
        textPaint.setColorFilter(cf);
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }

    @Override
    public int getIntrinsicWidth() {
        return width;
    }

    @Override
    public int getIntrinsicHeight() {
        return height;
    }

    public static class Builder {

        private String text;

        private int color;

        private int borderThickness;

        private int width;

        private int height;

        private Shape shape;

        public int textColor;

        private int fontSize;

        private boolean isBold;

        private boolean toUpperCase;

        public Builder() {
            textColor = Color.WHITE;
            borderThickness = 0;
            width = -1;
            height = -1;
            shape = new OvalShape();
            fontSize = -1;
            isBold = false;
            toUpperCase = true;
        }

        public Builder width(int width) {
            this.width = width;
            return this;
        }

        public Builder height(int height) {
            this.height = height;
            return this;
        }

        public TextDrawable build(String text, int color) {
            this.color = color;
            this.text = text;
            return new TextDrawable(this);
        }
    }
}