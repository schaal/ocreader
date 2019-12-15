package email.schaal.ocreader.view;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import androidx.annotation.Keep;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import android.util.AttributeSet;
import android.view.View;

import email.schaal.ocreader.R;

/**
 * FloatingActionButton with ability to show progress
 */
public class ProgressFloatingActionButton extends FloatingActionButton {
    private final Paint circlePaint = new Paint();

    private float progress;

    private final float diameter;

    public ProgressFloatingActionButton(Context context, AttributeSet attrs) {
        super(context, attrs);

        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.ProgressFloatingActionButton);
        try {
            circlePaint.setColor(typedArray.getColor(R.styleable.ProgressFloatingActionButton_progressColor, 0));
        } finally {
            typedArray.recycle();
        }

        diameter = getResources().getDimensionPixelSize(R.dimen.fab_size_normal);
    }

    @Keep
    public float getProgress() {
        return progress;
    }

    // Used by animator in ItemPagerActivity
    @Keep
    public void setProgress(float progress) {
        this.progress = progress;
        invalidate();
    }

    @Keep
    public void setFabBackgroundColor(int color) {
        final ColorStateList colorStateList = ColorStateList.valueOf(color);
        setBackgroundTintList(colorStateList);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float radius = diameter / 2 ;

        final int count = canvas.save();

        // draw progress circle fraction
        canvas.clipRect(0, diameter * (1 - progress), diameter, diameter);
        canvas.drawCircle(radius, radius, radius, circlePaint);

        canvas.restoreToCount(count);

        super.onDraw(canvas);
    }
}
