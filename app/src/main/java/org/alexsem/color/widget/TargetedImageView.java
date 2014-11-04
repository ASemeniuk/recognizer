package org.alexsem.color.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.widget.ImageView;

public class TargetedImageView extends ImageView {

    private PointF mTarget = null;
    private Paint mPaint;

    private int STROKE_LENGTH = 5;
    private int STROKE_PADDING = 3;
    private float STROKE_WIDTH = 2f;


    public TargetedImageView(Context context) {
        super(context);
        init(context);
    }

    public TargetedImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public TargetedImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    private void init(Context context) {
        int dpi = context.getResources().getDisplayMetrics().densityDpi;
        STROKE_LENGTH = STROKE_LENGTH * dpi / 160;
        STROKE_PADDING = STROKE_PADDING * dpi / 160;
        mPaint = new Paint();
        mPaint.setAntiAlias(false);
        mPaint.setColor(Color.RED);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeWidth(STROKE_WIDTH * dpi / 160f);
    }


    public void setTarget(float x, float y) {
        this.mTarget = new PointF(x, y);
        invalidate();
    }

    public void resetTarget() {
        this.mTarget = null;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (mTarget != null) {
            canvas.drawLine(mTarget.x + STROKE_PADDING, mTarget.y, mTarget.x + STROKE_PADDING + STROKE_LENGTH, mTarget.y, mPaint);
            canvas.drawLine(mTarget.x - STROKE_PADDING, mTarget.y, mTarget.x - STROKE_PADDING - STROKE_LENGTH, mTarget.y, mPaint);
            canvas.drawLine(mTarget.x, mTarget.y + STROKE_PADDING, mTarget.x, mTarget.y + STROKE_PADDING + STROKE_LENGTH, mPaint);
            canvas.drawLine(mTarget.x, mTarget.y - STROKE_PADDING, mTarget.x, mTarget.y - STROKE_PADDING - STROKE_LENGTH, mPaint);
        }
    }
}
