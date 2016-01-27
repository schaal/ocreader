package email.schaal.ocreader.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.support.design.widget.FloatingActionButton;
import android.util.AttributeSet;

import email.schaal.ocreader.R;

/**
 * FloatingActionButton with ability to show progress
 */
public class ProgressFloatingActionButton extends FloatingActionButton {
    private final Paint circlePaint = new Paint();
    private final Paint backgroundPaint = new Paint();

    private float progress;

    public ProgressFloatingActionButton(Context context, AttributeSet attrs) {
        super(context, attrs);

        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.ProgressFloatingActionButton);
        try {
            circlePaint.setColor(typedArray.getColor(R.styleable.ProgressFloatingActionButton_progressColor, 0));
        } finally {
            typedArray.recycle();
        }
    }

    public float getProgress() {
        return progress;
    }

    public void setProgress(float progress) {
        this.progress = progress;
    }

    @Override
    public void setBackgroundColor(int color) {
        backgroundPaint.setColor(color);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float diameter = canvas.getHeight();
        float radius = diameter / 2;

        // draw background circle
        canvas.drawCircle(radius, radius, radius, backgroundPaint);

        canvas.save();

        // draw progress circle fraction
        canvas.clipRect(0, diameter * (1 - progress), diameter, diameter);
        canvas.drawCircle(radius, radius, radius, circlePaint);

        canvas.restore();

        super.onDraw(canvas);
    }
}
