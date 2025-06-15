package com.cypher.zealth;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;


public final class DiseaseRiskEngine {

    private DiseaseRiskEngine() {}

 
    public static Map<String, Float> computeRisk(
            DiseaseCatalog.DiseaseOption disease
    ) {
        Map<String, Float> risk = new HashMap<>();

        
        for (String iso : CountryBoundaryStore.getAllIsoCodes()) {

            float base;

            switch (disease.sourceType) {
                case DISEASE_SH_COVID:
                    base = covidHeuristic(iso);
                    break;

                case CLIMATE_PROXY:
                    base = climateHeuristic(iso);
                    break;

                case WHO_GHO:
                default:
                    base = whoHeuristic(iso);
                    break;
            }

            risk.put(iso, clamp01(base));
        }

        return risk;
    }

  

    private static float whoHeuristic(String iso) {
        return hashNoise(iso, 0.3f, 0.8f);
    }

    private static float covidHeuristic(String iso) {
        return hashNoise(iso, 0.4f, 1.0f);
    }

    private static float climateHeuristic(String iso) {
        return hashNoise(iso, 0.2f, 0.9f);
    }


    private static float hashNoise(String key, float min, float max) {
        int h = Math.abs(key.hashCode());
        Random r = new Random(h);
        return min + r.nextFloat() * (max - min);
    }

    private static float clamp01(float v) {
        return Math.max(0f, Math.min(1f, v));
    }
}
