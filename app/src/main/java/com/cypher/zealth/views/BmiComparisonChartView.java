package com.cypher.zealth.views;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import com.cypher.zealth.R;

public class BmiComparisonChartView extends View {

    private static final double MIN_BMI = 10.0;
    private static final double MAX_BMI = 40.0;

    private static final double HEALTHY_MIN = 18.5;
    private static final double HEALTHY_MAX = 24.9;

    private double userBmi = Double.NaN;
    private double refBmi = Double.NaN;

    private final Paint pBg = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pTrack = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pBand = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pTick = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pUser = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pRef = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pText = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final RectF r = new RectF();

    public BmiComparisonChartView(Context context) {
        super(context);
        init();
    }

    public BmiComparisonChartView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public BmiComparisonChartView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        pBg.setColor(ContextCompat.getColor(getContext(), R.color.iosSecondaryGroupedBackground));
        pTrack.setColor(ContextCompat.getColor(getContext(), R.color.iosSeparator));

        pBand.setColor(ContextCompat.getColor(getContext(), R.color.iosGreenSoft));

        pTick.setColor(ContextCompat.getColor(getContext(), R.color.iosSeparator));

        pUser.setColor(ContextCompat.getColor(getContext(), R.color.iosBlue));
        pRef.setColor(ContextCompat.getColor(getContext(), R.color.iosGray));

        pText.setColor(ContextCompat.getColor(getContext(), R.color.iosSecondaryLabel));
        pText.setTextSize(sp(12));
    }

    public void setValues(double userBmi, double referenceBmi) {
        this.userBmi = userBmi;
        this.refBmi = referenceBmi;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float w = getWidth();
        float h = getHeight();

        float pad = dp(10);
        float top = dp(14);
        float left = pad;
        float right = w - pad;

        float trackH = dp(14);
        float trackTop = (h * 0.45f);
        float trackBottom = trackTop + trackH;

        float radius = dp(10);

        r.set(0, 0, w, h);
        canvas.drawRoundRect(r, dp(12), dp(12), pBg);

        r.set(left, trackTop, right, trackBottom);
        canvas.drawRoundRect(r, radius, radius, pTrack);
        float xHMin = mapToX(HEALTHY_MIN, left, right);
        float xHMax = mapToX(HEALTHY_MAX, left, right);
        r.set(xHMin, trackTop, xHMax, trackBottom);
        canvas.drawRoundRect(r, radius, radius, pBand);

        drawTick(canvas, 10, left, right, trackTop, trackBottom);
        drawTick(canvas, 20, left, right, trackTop, trackBottom);
        drawTick(canvas, 30, left, right, trackTop, trackBottom);
        drawTick(canvas, 40, left, right, trackTop, trackBottom);

        canvas.drawText("10", mapToX(10, left, right) - dp(8), top, pText);
        canvas.drawText("20", mapToX(20, left, right) - dp(8), top, pText);
        canvas.drawText("30", mapToX(30, left, right) - dp(8), top, pText);
        canvas.drawText("40", mapToX(40, left, right) - dp(8), top, pText);

        float cy = (trackTop + trackBottom) / 2f;
        float markerR = dp(7);

        if (!Double.isNaN(refBmi)) {
            float x = mapToX(refBmi, left, right);
            canvas.drawCircle(x, cy, markerR, pRef);
            canvas.drawText("Global", clampXForText(x, left, right, "Global"), trackBottom + dp(18), pText);
        }

        if (!Double.isNaN(userBmi)) {
            float x = mapToX(userBmi, left, right);
            canvas.drawCircle(x, cy, markerR, pUser);
            Paint pUserLabel = new Paint(pText);
            pUserLabel.setColor(ContextCompat.getColor(getContext(), R.color.iosLabel));
            pUserLabel.setFakeBoldText(true);
            canvas.drawText("You", clampXForText(x, left, right, "You"), trackBottom + dp(36), pUserLabel);
        }
    }

    private void drawTick(Canvas canvas, double bmi, float left, float right, float trackTop, float trackBottom) {
        float x = mapToX(bmi, left, right);
        canvas.drawLine(x, trackTop - dp(6), x, trackBottom + dp(6), pTick);
    }

    private float mapToX(double bmi, float left, float right) {
        double clamped = Math.max(MIN_BMI, Math.min(MAX_BMI, bmi));
        double t = (clamped - MIN_BMI) / (MAX_BMI - MIN_BMI);
        return (float) (left + (right - left) * t);
    }

    private float clampXForText(float x, float left, float right, String text) {
        float textW = pText.measureText(text);
        float min = left;
        float max = right - textW;
        return Math.max(min, Math.min(max, x - (textW / 2f)));
    }

    private float dp(float v) {
        return v * getResources().getDisplayMetrics().density;
    }

    private float sp(float v) {
        return v * getResources().getDisplayMetrics().scaledDensity;
    }
}
