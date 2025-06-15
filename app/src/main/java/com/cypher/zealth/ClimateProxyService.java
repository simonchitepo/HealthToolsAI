package com.cypher.zealth;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class ClimateProxyService {

    private static final String TAG = "ClimateProxyService";

    // Configure this if you want a different provider :
    private static final String BASE_URL = "https://api.open-meteo.com/v1/forecast";

    private static final long TTL_MS = 6L * 60L * 60L * 1000L; // 6 hours

    private final OkHttpClient http;
    private final Gson gson = new Gson();
    private final SharedPreferences prefs;

  
    private final Map<String, CacheEntry> mem = new ConcurrentHashMap<>();

    public ClimateProxyService(@NonNull Context ctx, @NonNull OkHttpClient httpClient) {
        this.http = httpClient;
        this.prefs = ctx.getSharedPreferences("climate_proxy_cache", Context.MODE_PRIVATE);
    }

    public interface Callback {
        void onSuccess(@NonNull ClimateSample sample);
        void onError(@NonNull Throwable t);
    }

    public static class ClimateSample {
        public final String iso3;
        public final double lat;
        public final double lon;

       
        public final String dayIso; // yyyy-MM-dd (UTC)
        public final double tMaxC;
        public final double tMinC;
        public final double precipitationMm;
        public final double humidityMean; // 0..100 

        // Metadata
        public final long fetchedAtEpochMs;
        public final String sourceLabel;

        public ClimateSample(
                String iso3,
                double lat,
                double lon,
                String dayIso,
                double tMaxC,
                double tMinC,
                double precipitationMm,
                double humidityMean,
                long fetchedAtEpochMs,
                String sourceLabel
        ) {
            this.iso3 = iso3;
            this.lat = lat;
            this.lon = lon;
            this.dayIso = dayIso;
            this.tMaxC = tMaxC;
            this.tMinC = tMinC;
            this.precipitationMm = precipitationMm;
            this.humidityMean = humidityMean;
            this.fetchedAtEpochMs = fetchedAtEpochMs;
            this.sourceLabel = sourceLabel;
        }
    }

    private static class CacheEntry {
        final ClimateSample sample;
        final long expiresAt;
        CacheEntry(ClimateSample sample, long expiresAt) {
            this.sample = sample;
            this.expiresAt = expiresAt;
        }
    }

  
    public void fetchDailyForIso(
            @NonNull String iso3,
            double lat,
            double lon,
            @NonNull Callback cb
    ) {
        String key = buildKey(iso3, lat, lon);

        CacheEntry ce = mem.get(key);
        long now = System.currentTimeMillis();
        if (ce != null && now < ce.expiresAt) {
            cb.onSuccess(ce.sample);
            return;
        }

        String raw = prefs.getString(key, null);
        if (!TextUtils.isEmpty(raw)) {
            try {
                Persisted persisted = gson.fromJson(raw, Persisted.class);
                if (persisted != null && persisted.sample != null && now < persisted.expiresAt) {
                    CacheEntry loaded = new CacheEntry(persisted.sample, persisted.expiresAt);
                    mem.put(key, loaded);
                    cb.onSuccess(persisted.sample);
                    return;
                }
            } catch (Exception ignored) {}
        }

        try {
            ClimateSample s = fetchFromNetwork(iso3, lat, lon);
            long exp = now + TTL_MS;

            CacheEntry fresh = new CacheEntry(s, exp);
            mem.put(key, fresh);

            Persisted p = new Persisted();
            p.sample = s;
            p.expiresAt = exp;
            prefs.edit().putString(key, gson.toJson(p)).apply();

            cb.onSuccess(s);

        } catch (Throwable t) {
            cb.onError(t);
        }
    }

    private static String buildKey(String iso3, double lat, double lon) {
        double rLat = Math.round(lat * 100.0) / 100.0;
        double rLon = Math.round(lon * 100.0) / 100.0;
        return "v1|" + iso3.toUpperCase(Locale.ROOT) + "|" + rLat + "," + rLon;
    }

    private ClimateSample fetchFromNetwork(String iso3, double lat, double lon) throws Exception {
   
        HttpUrl url = HttpUrl.parse(BASE_URL).newBuilder()
                .addQueryParameter("latitude", String.valueOf(lat))
                .addQueryParameter("longitude", String.valueOf(lon))
                .addQueryParameter("timezone", "UTC")
                .addQueryParameter("forecast_days", "3")
                .addQueryParameter("daily", "temperature_2m_max,temperature_2m_min,precipitation_sum")
                .addQueryParameter("hourly", "relative_humidity_2m")
                .build();

        Request req = new Request.Builder().url(url).get().build();
        Response resp = http.newCall(req).execute();

        if (!resp.isSuccessful()) {
            throw new IllegalStateException("Climate HTTP " + resp.code());
        }

        String body = resp.body() != null ? resp.body().string() : "";
        ApiResponse ar = gson.fromJson(body, ApiResponse.class);
        if (ar == null || ar.daily == null || ar.daily.time == null || ar.daily.time.length == 0) {
            throw new IllegalStateException("Climate parse error (no daily block)");
        }

        int i = ar.daily.time.length - 1;
        String day = ar.daily.time[i];

        double tMax = safeArray(ar.daily.temperatureMax, i);
        double tMin = safeArray(ar.daily.temperatureMin, i);
        double pSum = safeArray(ar.daily.precipSum, i);
        double humMean = Double.NaN;
        if (ar.hourly != null && ar.hourly.time != null && ar.hourly.humidity != null) {
            humMean = computeDailyMeanHumidity(ar.hourly.time, ar.hourly.humidity, day);
        }

        long now = System.currentTimeMillis();
        return new ClimateSample(
                iso3.toUpperCase(Locale.ROOT),
                lat,
                lon,
                day,
                tMax,
                tMin,
                pSum,
                humMean,
                now,
                "Climate proxy"
        );
    }

    private static double safeArray(double[] arr, int idx) {
        if (arr == null || idx < 0 || idx >= arr.length) return Double.NaN;
        return arr[idx];
    }

    private static double computeDailyMeanHumidity(String[] hourlyTimes, double[] humidity, String dayIso) {
     
        if (hourlyTimes == null || humidity == null) return Double.NaN;
        if (hourlyTimes.length != humidity.length) return Double.NaN;

        double sum = 0;
        int n = 0;
        for (int i = 0; i < hourlyTimes.length; i++) {
            String t = hourlyTimes[i];
            if (t == null) continue;
            if (t.startsWith(dayIso)) {
                double h = humidity[i];
                if (!Double.isNaN(h)) {
                    sum += h;
                    n++;
                }
            }
        }
        return n == 0 ? Double.NaN : (sum / n);
    }

    private static class Persisted {
        ClimateSample sample;
        long expiresAt;
    }

    private static class ApiResponse {
        Daily daily;
        Hourly hourly;
    }

    private static class Daily {
        String[] time;

        @SerializedName("temperature_2m_max")
        double[] temperatureMax;

        @SerializedName("temperature_2m_min")
        double[] temperatureMin;

        @SerializedName("precipitation_sum")
        double[] precipSum;
    }

    private static class Hourly {
        String[] time;

        @SerializedName("relative_humidity_2m")
        double[] humidity;
    }

    public static String utcDayIsoNow() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.ROOT);
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        return sdf.format(new Date());
    }
}
