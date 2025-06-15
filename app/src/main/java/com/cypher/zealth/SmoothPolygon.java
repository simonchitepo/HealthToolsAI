package com.cypher.zealth;

import android.graphics.Paint;
import android.os.Build;

import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Polygon;


public class SmoothPolygon extends Polygon {

    public SmoothPolygon(MapView mapView) {
        super(mapView);
        tunePaints();
    }

    private void tunePaints() {
      
        try {
            Paint fill = getFillPaint();
            Paint outline = getOutlinePaint();

            if (fill != null) {
                fill.setAntiAlias(true);
                fill.setDither(true);
                fill.setFilterBitmap(true);
            }

            if (outline != null) {
                outline.setAntiAlias(true);
                outline.setDither(true);
                outline.setStrokeJoin(Paint.Join.ROUND);
                outline.setStrokeCap(Paint.Cap.ROUND);

            
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                    outline.setHinting(Paint.HINTING_ON);
                }
            }
        } catch (Throwable ignored) {
         
        }
    }
}
