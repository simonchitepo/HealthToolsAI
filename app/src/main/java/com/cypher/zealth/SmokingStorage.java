package com.cypher.zealth;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SmokingStorage {

    private static final String PREF = "smoking_pref";

    private static final String KEY_QUIT_DATE_MS = "quit_date_ms";
    private static final String KEY_CIGS_PER_DAY = "cigs_per_day";
    private static final String KEY_COST_PER_PACK = "cost_per_pack";
    private static final String KEY_CIGS_PER_PACK = "cigs_per_pack";
    private static final String KEY_CRAVINGS = "cravings_json";

    private final SharedPreferences sp;
    private final Gson gson = new Gson();
    private final Type listType = new TypeToken<List<CravingEntry>>() {}.getType();

    public SmokingStorage(Context ctx) {
        sp = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE);
    }

    public long getQuitDateMs() { return sp.getLong(KEY_QUIT_DATE_MS, -1L); }
    public int getCigsPerDay() { return sp.getInt(KEY_CIGS_PER_DAY, 0); }
    public double getCostPerPack() {
        return Double.longBitsToDouble(sp.getLong(KEY_COST_PER_PACK, Double.doubleToLongBits(0)));
    }
    public int getCigsPerPack() { return sp.getInt(KEY_CIGS_PER_PACK, 20); }

    public void savePlan(long quitDateMs, int cigsPerDay, double costPerPack, int cigsPerPack) {
        sp.edit()
                .putLong(KEY_QUIT_DATE_MS, quitDateMs)
                .putInt(KEY_CIGS_PER_DAY, cigsPerDay)
                .putLong(KEY_COST_PER_PACK, Double.doubleToLongBits(costPerPack))
                .putInt(KEY_CIGS_PER_PACK, cigsPerPack)
                .apply();
    }

    public List<CravingEntry> loadCravings() {
        String json = sp.getString(KEY_CRAVINGS, null);
        if (json == null || json.trim().isEmpty()) return new ArrayList<>();
        try {
            List<CravingEntry> items = gson.fromJson(json, listType);
            return items != null ? items : new ArrayList<>();
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    public void addCraving(CravingEntry entry) {
        List<CravingEntry> items = loadCravings();
        items.add(entry);
        Collections.sort(items, (a, b) -> Long.compare(b.timestampMs, a.timestampMs));
        sp.edit().putString(KEY_CRAVINGS, gson.toJson(items)).apply();
    }

    public void clearAll() {
        sp.edit().clear().apply();
    }
}
