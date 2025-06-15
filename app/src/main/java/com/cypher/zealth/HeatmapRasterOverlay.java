package com.cypher.zealth;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.Shader;
import android.util.LruCache;

import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.Projection;
import org.osmdroid.views.overlay.Overlay;

import java.util.ArrayList;
import java.util.List;


public class HeatmapRasterOverlay extends Overlay {

    public static class HeatPoint {
        public final GeoPoint geo;
        public final float intensity01; // 0..1
        public HeatPoint(GeoPoint geo, float intensity01) {
            this.geo = geo;
            this.intensity01 = Math.max(0f, Math.min(1f, intensity01));
        }
    }

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint clearPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private volatile List<HeatPoint> points = new ArrayList<>();
    private volatile boolean enabled = true;

    private final LruCache<String, Bitmap> bitmapCache;

    private float minRadiusPx = 18f;
    private float maxRadiusPx = 90f;

    public HeatmapRasterOverlay(int cacheBytes) {
        bitmapCache = new LruCache<String, Bitmap>(cacheBytes) {
            @Override protected int sizeOf(String key, Bitmap value) {
                return value.getByteCount();
            }
        };

        clearPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setPoints(List<HeatPoint> pts) {
        this.points = (pts == null) ? new ArrayList<>() : pts;
      
        bitmapCache.evictAll();
    }

    public void setRadiusRangePx(float minPx, float maxPx) {
        this.minRadiusPx = Math.max(1f, minPx);
        this.maxRadiusPx = Math.max(this.minRadiusPx, maxPx);
        bitmapCache.evictAll();
    }

    @Override
    public void draw(Canvas canvas, MapView mapView, boolean shadow) {
        if (shadow || !enabled) return;
        final List<HeatPoint> local = points;
        if (local == null || local.isEmpty()) return;

        final Projection pj = mapView.getProjection();
        final Rect screen = pj.getScreenRect();
        final int w = screen.width();
        final int h = screen.height();
        if (w <= 0 || h <= 0) return;

        double z = mapView.getZoomLevelDouble();
        int zoomBucket = (int) Math.round(z * 2.0); 

     
        org.osmdroid.util.BoundingBox bb = mapView.getBoundingBox();
        String key = zoomBucket + "|" +
                round(bb.getLatNorth(), 2) + "," + round(bb.getLonWest(), 2) + "," +
                round(bb.getLatSouth(), 2) + "," + round(bb.getLonEast(), 2) +
                "|" + w + "x" + h;

        Bitmap bmp = bitmapCache.get(key);
        if (bmp == null || bmp.isRecycled()) {
            bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            Canvas bc = new Canvas(bmp);


            bc.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
            for (HeatPoint hp : local) {
                android.graphics.Point p = pj.toPixels(hp.geo, null);
                float ix = p.x - screen.left;
                float iy = p.y - screen.top;

                if (ix < -200 || iy < -200 || ix > w + 200 || iy > h + 200) continue;

                float r = lerp(minRadiusPx, maxRadiusPx, hp.intensity01);

                int centerColor = riskColor(hp.intensity01, 160);
                int edgeColor = riskColor(hp.intensity01, 0);

                Shader shader = new RadialGradient(
                        ix, iy, r,
                        new int[]{centerColor, edgeColor},
                        new float[]{0f, 1f},
                        Shader.TileMode.CLAMP
                );
                paint.setShader(shader);
                bc.drawCircle(ix, iy, r, paint);
            }

            bitmapCache.put(key, bmp);
        }

        canvas.drawBitmap(bmp, screen.left, screen.top, null);
    }

    private static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    private static double round(double v, int decimals) {
        double m = Math.pow(10, decimals);
        return Math.round(v * m) / m;
    }

  
    private static int riskColor(float intensity01, int alpha) {
        intensity01 = Math.max(0f, Math.min(1f, intensity01));


        int r, g, b;

        if (intensity01 < 0.25f) {
            float t = intensity01 / 0.25f;
            r = (int) lerpI(0, 0, t);
            g = (int) lerpI(90, 200, t);
            b = (int) lerpI(255, 255, t);
        } else if (intensity01 < 0.5f) {
            float t = (intensity01 - 0.25f) / 0.25f;
            r = (int) lerpI(0, 40, t);
            g = (int) lerpI(200, 210, t);
            b = (int) lerpI(255, 80, t);
        } else if (intensity01 < 0.75f) {
            float t = (intensity01 - 0.5f) / 0.25f;
            r = (int) lerpI(40, 255, t);
            g = (int) lerpI(210, 160, t);
            b = (int) lerpI(80, 0, t);
        } else {
            float t = (intensity01 - 0.75f) / 0.25f;
            r = (int) lerpI(255, 229, t);
            g = (int) lerpI(160, 57, t);
            b = (int) lerpI(0, 53, t);
        }

        return Color.argb(alpha, clamp255(r), clamp255(g), clamp255(b));
    }

    private static float lerpI(int a, int b, float t) {
        return a + (b - a) * t;
    }

    private static int clamp255(int x) {
        return Math.max(0, Math.min(255, x));
    }
}
