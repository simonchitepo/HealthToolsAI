package com.cypher.zealth;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class WeightStorage {

    private static final String PREF = "weight_pref";

    private static final String KEY_START_WEIGHT = "start_weight";
    private static final String KEY_TARGET_WEIGHT = "target_weight";
    private static final String KEY_TARGET_DATE = "target_date_ms";
    private static final String KEY_LOGS = "logs_json";

    private final SharedPreferences sp;
    private final Gson gson = new Gson();
    private final Type listType = new TypeToken<List<WeightLogEntry>>() {}.getType();

    public WeightStorage(Context ctx) {
        sp = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE);
    }

    public boolean hasGoal() {
        return sp.contains(KEY_START_WEIGHT) && sp.contains(KEY_TARGET_WEIGHT);
    }

    public double getStartWeight() { return Double.longBitsToDouble(sp.getLong(KEY_START_WEIGHT, Double.doubleToLongBits(0))); }
    public double getTargetWeight() { return Double.longBitsToDouble(sp.getLong(KEY_TARGET_WEIGHT, Double.doubleToLongBits(0))); }
    public long getTargetDateMs() { return sp.getLong(KEY_TARGET_DATE, -1L); }

    public void saveGoal(double startWeight, double targetWeight, long targetDateMs) {
        sp.edit()
                .putLong(KEY_START_WEIGHT, Double.doubleToLongBits(startWeight))
                .putLong(KEY_TARGET_WEIGHT, Double.doubleToLongBits(targetWeight))
                .putLong(KEY_TARGET_DATE, targetDateMs)
                .apply();
    }

    public List<WeightLogEntry> loadLogs() {
        String json = sp.getString(KEY_LOGS, null);
        if (json == null || json.trim().isEmpty()) return new ArrayList<>();
        try {
            List<WeightLogEntry> items = gson.fromJson(json, listType);
            return items != null ? items : new ArrayList<>();
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    public void saveLogs(List<WeightLogEntry> logs) {
        sp.edit().putString(KEY_LOGS, gson.toJson(logs)).apply();
    }

    public void addLog(WeightLogEntry entry) {
        List<WeightLogEntry> logs = loadLogs();
        logs.add(entry);
        Collections.sort(logs, (a, b) -> Long.compare(b.dateMs, a.dateMs));
        saveLogs(logs);
    }

    public void clearAll() {
        sp.edit().clear().apply();
    }
}
