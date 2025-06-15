package com.cypher.zealth;

import androidx.annotation.NonNull;

public final class ClimateRiskModel {

    private ClimateRiskModel() {}

    public enum ClimateDisease {
        MALARIA,
        CHOLERA
    }

    public static final class RiskResult {
        public final float risk01;      // 0..1
        public final String rationale;  // short explanation

        public RiskResult(float risk01, String rationale) {
            this.risk01 = clamp01(risk01);
            this.rationale = rationale == null ? "" : rationale;
        }
    }

 
    @NonNull
    public static RiskResult score(@NonNull ClimateDisease disease, @NonNull ClimateProxyService.ClimateSample s) {
        double tMax = s.tMaxC;
        double tMin = s.tMinC;
        double p = s.precipitationMm;
        double hum = s.humidityMean;

        if (Double.isNaN(tMax) && Double.isNaN(tMin) && Double.isNaN(p)) {
            return new RiskResult(0f, "Insufficient climate data for scoring.");
        }

        double tMean = meanSafe(tMax, tMin);

        switch (disease) {
            case MALARIA:
                return malaria(tMean, p, hum);
            case CHOLERA:
            default:
                return cholera(tMean, p, hum);
        }
    }

    private static RiskResult malaria(double tMeanC, double precipMm, double humidityMean) {

        float temp = (float) bell(tMeanC, 26.0, 8.0); 
        float rain = (float) saturating(precipMm, 6.0, 25.0); 
        float hum = Double.isNaN(humidityMean) ? 0.5f : (float) saturating(humidityMean, 45.0, 80.0);

        float risk = 0.55f * temp + 0.30f * rain + 0.15f * hum;

        String rationale = "Malaria suitability from climate proxies:\n"
                + "- Temp suitability: " + pct(temp) + "\n"
                + "- Rain suitability: " + pct(rain) + "\n"
                + "- Humidity factor: " + pct(hum) + "\n"
                + "This is a climate-based suitability index, not case surveillance.";

        return new RiskResult(risk, rationale);
    }

    private static RiskResult cholera(double tMeanC, double precipMm, double humidityMean) {
       
        float temp = (float) saturating(tMeanC, 20.0, 32.0);
        float rain = (float) saturating(precipMm, 10.0, 40.0); // “event rainfall” weighting
        float hum = Double.isNaN(humidityMean) ? 0.5f : (float) saturating(humidityMean, 40.0, 85.0);

        float risk = 0.35f * temp + 0.50f * rain + 0.15f * hum;

        String rationale = "Cholera climate risk proxy:\n"
                + "- Rain/flood proxy: " + pct(rain) + "\n"
                + "- Temperature factor: " + pct(temp) + "\n"
                + "- Humidity factor: " + pct(hum) + "\n"
                + "This is a climate proxy index, not laboratory-confirmed incidence.";

        return new RiskResult(risk, rationale);
    }


    private static double meanSafe(double a, double b) {
        if (!Double.isNaN(a) && !Double.isNaN(b)) return (a + b) / 2.0;
        if (!Double.isNaN(a)) return a;
        if (!Double.isNaN(b)) return b;
        return Double.NaN;
    }

    private static double saturating(double x, double lo, double hi) {
        if (Double.isNaN(x)) return 0.5; 
        if (x <= lo) return 0.0;
        if (x >= hi) return 1.0;
        return (x - lo) / (hi - lo);
    }

    private static double bell(double x, double mean, double sigma) {
        if (Double.isNaN(x)) return 0.5;
        double z = (x - mean) / sigma;
        return Math.exp(-0.5 * z * z);
    }

    private static String pct(float x) {
        int v = Math.round(clamp01(x) * 100f);
        return v + "%";
    }

    private static float clamp01(float x) {
        return Math.max(0f, Math.min(1f, x));
    }
}
