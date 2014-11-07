package org.alexsem.color.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

/**
 * View which allows to show close colors
 */
public class CloseColorView extends View {

    private final float RANGE_HUE = 54f;
    private final float RANGE_VALUE = 0.8f;
    private final int SECTIONS_HUE = 9;
    private final int SECTIONS_VALUE = 8;

    private Integer mStartingColor = null;
    private int mCurrentHueSect;
    private int mCurrentValueSect;
    private int[][] mColors;

    private boolean isMeasurementChanged = false;
    private boolean isFull = false;

    private Paint mPaint;

    //----------------------------------------------------------------------------------------------

    public CloseColorView(Context context) {
        super(context);
        init();
    }

    public CloseColorView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CloseColorView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        mPaint = new Paint();
        mPaint.setAntiAlias(false);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        isMeasurementChanged = true;
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    //----------------------------------------------------------------------------------------------

    /**
     * Set color to process
     * @param color Color
     */
    public void setColor(int color) {
        mStartingColor = color;
        isMeasurementChanged = true;
        isFull = false;
        invalidate();
    }

    /**
     * Set full-screen display if it is not yet set or reset it if set
     */
    public void toggleFull() {
        isFull = !isFull;
        invalidate();
    }

    /**
     * Get calculated color in specified coordinates
     * @param x X-coordinate
     * @param y Y-coordinate
     * @return calculated color or null if out of bounds
     */
    public Integer getColorAt(int x, int y) {
        if (mColors == null || x < 0 || y < 0 || x > getMeasuredWidth() || y > getMeasuredHeight()) {
            return null;
        }
        int w = x / Math.round((float) getMeasuredWidth() / SECTIONS_HUE);
        int h = y / Math.round((float) getMeasuredHeight() / SECTIONS_VALUE);
        if (w >= mColors.length || h >= mColors[w].length) {
            return null;
        }
        return mColors[w][h];
    }

    //----------------------------------------------------------------------------------------------

    /**
     * Calculate data array
     */
    private void calculate() {
        if (mStartingColor == null) { //Nothing to calculate yet
            return;
        }

        int width = getMeasuredWidth();
        int height = getMeasuredHeight();

        if (width == 0 || height == 0) { //Component too narrow yet
            return;
        }

        float[] hsv = new float[3];
        Color.colorToHSV(mStartingColor, hsv);
        float hueOffset = hsv[0] - RANGE_HUE / 2;
        float hueSect = RANGE_HUE / SECTIONS_HUE;
        float valueOffset = 1f - RANGE_VALUE;
        float valueSect = RANGE_VALUE / SECTIONS_VALUE;

        mCurrentValueSect = Math.max(0, SECTIONS_VALUE - (int) ((hsv[2] - valueOffset) / valueSect) - 1);
        mCurrentHueSect = (SECTIONS_HUE + 1) / 2 - 1;
        mColors = new int[SECTIONS_HUE][SECTIONS_VALUE];

        for (int w = 0; w < SECTIONS_HUE; w++) {
            for (int h = 0; h < SECTIONS_VALUE; h++) {
                hsv[0] = (w + 0.5f) * hueSect + hueOffset;
                if (hsv[0] > 360f) {
                    hsv[0] -= 360f;
                } else if (hsv[0] < 0f) {
                    hsv[0] += 360f;
                }
                hsv[2] = (h + 0.5f) * valueSect + valueOffset;
                mColors[w][SECTIONS_VALUE - 1 - h] = Color.HSVToColor(hsv);
            }
        }

    }

    //----------------------------------------------------------------------------------------------

    @Override
    protected void onDraw(Canvas canvas) {
        if (isMeasurementChanged) {
            isMeasurementChanged = false;
            calculate();
        }

        if (mColors == null) { //Nothing to draw
            return;
        }

        //--- Full-screen display ---
        if (isFull) {
            mPaint.setStrokeWidth(0f);
            mPaint.setStyle(Paint.Style.FILL);
            mPaint.setColor(mStartingColor);
            canvas.drawRect(0, 0, canvas.getWidth(), canvas.getHeight(), mPaint);
            return;
        }

        float hueSect = canvas.getWidth() / (float) SECTIONS_HUE;
        float valueSect = canvas.getHeight() / (float) SECTIONS_VALUE;

        //--- Draw rectangles ---
        mPaint.setStrokeWidth(0f);
        mPaint.setStyle(Paint.Style.FILL);
        for (int hs = 0; hs < SECTIONS_HUE; hs++) {
            for (int vs = 0; vs < SECTIONS_VALUE; vs++) {
                mPaint.setColor(mColors[hs][vs]);
                canvas.drawRect(hs * hueSect, vs * valueSect, hs * hueSect + hueSect, vs * valueSect + valueSect, mPaint);
            }
        }

        //--- Draw black lines ---
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setColor(Color.BLACK);
        for (int hs = 1; hs < SECTIONS_HUE; hs++) {
            canvas.drawLine(hs * hueSect, 0f, hs * hueSect, canvas.getHeight(), mPaint);
        }
        for (int vs = 1; vs < SECTIONS_VALUE; vs++) {
            canvas.drawLine(0f, vs * valueSect, canvas.getWidth(), vs * valueSect, mPaint);
        }

        //--- Draw current selection ---
        mPaint.setStrokeWidth(2f);
        mPaint.setColor(Color.RED);
        canvas.drawRect(mCurrentHueSect * hueSect, mCurrentValueSect * valueSect, mCurrentHueSect * hueSect + hueSect + 1, mCurrentValueSect * valueSect + valueSect + 1, mPaint);

    }

}
