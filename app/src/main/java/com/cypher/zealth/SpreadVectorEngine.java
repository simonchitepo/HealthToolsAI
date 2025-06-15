package com.cypher.zealth;

import android.graphics.Color;

import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.overlay.Polyline;
import org.osmdroid.views.overlay.Overlay;

import java.util.ArrayList;
import java.util.List;


public final class SpreadVectorEngine {

    private SpreadVectorEngine() {}

 
    public static List<Overlay> generate(
            DiseaseCatalog.DiseaseOption disease
    ) {
        List<Overlay> overlays = new ArrayList<>();

        if (!disease.caps.supportsSpreadIndex) {
            return overlays;
        }

        
        overlays.add(makeArrow(
                new GeoPoint(10, -20),
                new GeoPoint(20, -10),
                Color.argb(180, 255, 80, 80)
        ));

        overlays.add(makeArrow(
                new GeoPoint(0, 30),
                new GeoPoint(10, 40),
                Color.argb(180, 255, 120, 80)
        ));

        overlays.add(makeArrow(
                new GeoPoint(-15, 100),
                new GeoPoint(-5, 110),
                Color.argb(180, 255, 60, 60)
        ));

        return overlays;
    }

    private static Polyline makeArrow(
            GeoPoint from,
            GeoPoint to,
            int color
    ) {
        Polyline line = new Polyline();
        line.setPoints(List.of(from, to));
        line.setColor(color);
        line.setWidth(6f);
        line.setGeodesic(true);
        return line;
    }
}
