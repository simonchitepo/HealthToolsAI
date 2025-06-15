package com.cypher.zealth;

import org.osmdroid.util.GeoPoint;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class CountryBoundaryStore {

    private static final Map<String, List<GeoPoint>> boundaries = new HashMap<>();

    private CountryBoundaryStore() {}

  
    public static void init(Map<String, List<GeoPoint>> geoJsonData) {
        boundaries.clear();
        boundaries.putAll(geoJsonData);
    }

    public static Set<String> getAllIsoCodes() {
        return boundaries.keySet();
    }

    public static List<GeoPoint> getBoundary(String isoCode) {
        return boundaries.get(isoCode);
    }
}
