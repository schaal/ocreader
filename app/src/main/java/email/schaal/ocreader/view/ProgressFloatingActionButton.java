package email.schaal.ocreader.view;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Build;
import android.support.annotation.Keep;
import android.support.design.widget.FloatingActionButton;
import android.util.AttributeSet;

import email.schaal.ocreader.R;

/**
 * FloatingActionButton with ability to show progress
 */
public class ProgressFloatingActionButton extends FloatingActionButton {
    private final Paint circlePaint = new Paint();

    private float progress;
    private float diameter;


    public ProgressFloatingActionButton(Context context, AttributeSet attrs) {
        super(context, attrs);

        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.ProgressFloatingActionButton);
        try {
            circlePaint.setColor(typedArray.getColor(R.styleable.ProgressFloatingActionButton_progressColor, 0));
        } finally {
            typedArray.recycle();
        }
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

    @Override
    public void setBackgroundColor(int color) {
        if(Build.VERSION.SDK_INT >= 21) {
            setBackgroundTintList(ColorStateList.valueOf(color));
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        diameter = getHeight();
        super.onSizeChanged(w, h, oldw, oldh);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float radius = diameter / 2 ;

        canvas.save();

        // draw progress circle fraction
        canvas.clipRect(0, diameter * (1 - progress), diameter, diameter);
        canvas.drawCircle(radius, radius, radius, circlePaint);

        canvas.restore();

        super.onDraw(canvas);
    }
}
