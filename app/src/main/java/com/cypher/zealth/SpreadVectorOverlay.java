package com.cypher.zealth;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;

import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.Projection;
import org.osmdroid.views.overlay.Overlay;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


public class SpreadVectorOverlay extends Overlay {

    public static class SpreadVector {
        public final GeoPoint start;
        public final GeoPoint end;
        public final float speedKmh; // display metric
        public SpreadVector(GeoPoint start, GeoPoint end, float speedKmh) {
            this.start = start;
            this.end = end;
            this.speedKmh = speedKmh;
        }
    }

    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint headPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private volatile boolean enabled = true;
    private volatile List<SpreadVector> vectors = new ArrayList<>();

    public SpreadVectorOverlay() {
        linePaint.setColor(Color.argb(190, 255, 255, 255));
        linePaint.setStrokeWidth(3.0f);
        linePaint.setStyle(Paint.Style.STROKE);

        headPaint.setColor(Color.argb(220, 255, 255, 255));
        headPaint.setStyle(Paint.Style.FILL);

        textPaint.setColor(Color.argb(220, 255, 255, 255));
        textPaint.setTextSize(28f);
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setVectors(List<SpreadVector> v) {
        this.vectors = (v == null) ? new ArrayList<>() : v;
    }

    @Override
    public void draw(Canvas canvas, MapView mapView, boolean shadow) {
        if (shadow || !enabled) return;
        final List<SpreadVector> local = vectors;
        if (local == null || local.isEmpty()) return;

        Projection pj = mapView.getProjection();

        for (SpreadVector sv : local) {
            android.graphics.Point p1 = pj.toPixels(sv.start, null);
            android.graphics.Point p2 = pj.toPixels(sv.end, null);

            canvas.drawLine(p1.x, p1.y, p2.x, p2.y, linePaint);
            drawArrowHead(canvas, p1.x, p1.y, p2.x, p2.y);

            String label = String.format(Locale.ROOT, "%.0f km/h", sv.speedKmh);
            canvas.drawText(label, (p1.x + p2.x) * 0.5f + 10, (p1.y + p2.y) * 0.5f - 10, textPaint);
        }
    }

    private void drawArrowHead(Canvas canvas, float x1, float y1, float x2, float y2) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len < 10f) return;

        float ux = dx / len;
        float uy = dy / len;

        float size = 18f;

        float bx = x2 - ux * size;
        float by = y2 - uy * size;

        float px = -uy;
        float py = ux;

        float leftX = bx + px * (size * 0.6f);
        float leftY = by + py * (size * 0.6f);

        float rightX = bx - px * (size * 0.6f);
        float rightY = by - py * (size * 0.6f);

        Path path = new Path();
        path.moveTo(x2, y2);
        path.lineTo(leftX, leftY);
        path.lineTo(rightX, rightY);
        path.close();

        canvas.drawPath(path, headPaint);
    }
}
