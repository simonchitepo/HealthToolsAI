package com.cypher.zealth;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

public class SparklineView extends View {

    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

  
    private float[] values = new float[0];

    public SparklineView(Context context) {
        super(context);
        init();
    }

    public SparklineView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public SparklineView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(dp(2));
        linePaint.setStrokeCap(Paint.Cap.ROUND);

        dotPaint.setStyle(Paint.Style.FILL);

        gridPaint.setStyle(Paint.Style.STROKE);
        gridPaint.setStrokeWidth(dp(1));
        gridPaint.setAlpha(60);
    }

 
    public void setValues(float[] values1to5) {
        this.values = values1to5 == null ? new float[0] : values1to5;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int w = getWidth();
        int h = getHeight();

        if (w <= 0 || h <= 0) return;

        int primary = ContextCompat.getColor(getContext(), R.color.zealth_primary);
        int grid = ContextCompat.getColor(getContext(), R.color.zealth_info);

        linePaint.setColor(primary);
        dotPaint.setColor(primary);
        gridPaint.setColor(grid);

        float padding = dp(8);
        float left = padding;
        float top = padding;
        float right = w - padding;
        float bottom = h - padding;

       
        canvas.drawLine(left, top + (bottom - top) * 0.33f, right,
                top + (bottom - top) * 0.33f, gridPaint);
        canvas.drawLine(left, top + (bottom - top) * 0.66f, right,
                top + (bottom - top) * 0.66f, gridPaint);

        if (values.length < 2) return;

        float step = (right - left) / (values.length - 1);

        float prevX = -1;
        float prevY = -1;

   
        for (int i = 0; i < values.length; i++) {
            float v = clamp(values[i], 1f, 5f);
            float x = left + step * i;
            float y = bottom - ((v - 1f) / 4f) * (bottom - top);

            if (prevX >= 0) {
                canvas.drawLine(prevX, prevY, x, y, linePaint);
            }
            prevX = x;
            prevY = y;
        }

        for (int i = 0; i < values.length; i++) {
            float v = clamp(values[i], 1f, 5f);
            float x = left + step * i;
            float y = bottom - ((v - 1f) / 4f) * (bottom - top);
            canvas.drawCircle(x, y, dp(3), dotPaint);
        }
    }

    private float clamp(float v, float min, float max) {
        return Math.max(min, Math.min(max, v));
    }

    private float dp(float value) {
        return TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                value,
                getResources().getDisplayMetrics()
        );
    }
}
