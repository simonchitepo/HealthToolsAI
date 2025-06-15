package com.cypher.zealth;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BodyMapStorage {

    private static final String PREF = "body_map_pref";
    private static final String KEY = "entries_json";

    private final SharedPreferences sp;
    private final Gson gson = new Gson();
    private final Type listType = new TypeToken<List<BodyEntry>>() {}.getType();

    public BodyMapStorage(Context ctx) {
        sp = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE);
    }

    public List<BodyEntry> load() {
        String json = sp.getString(KEY, null);
        if (json == null || json.trim().isEmpty()) return new ArrayList<>();
        try {
            List<BodyEntry> items = gson.fromJson(json, listType);
            return items != null ? items : new ArrayList<>();
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    public void save(List<BodyEntry> items) {
        sp.edit().putString(KEY, gson.toJson(items)).apply();
    }

    public void add(BodyEntry entry) {
        List<BodyEntry> items = load();
        items.add(entry);
        Collections.sort(items, (a, b) -> Long.compare(b.createdAtMs, a.createdAtMs));
        save(items);
    }

    public void deleteByCreatedAt(long createdAtMs) {
        List<BodyEntry> items = load();
        items.removeIf(e -> e != null && e.createdAtMs == createdAtMs);
        save(items);
    }

    public void clear() {
        sp.edit().remove(KEY).apply();
    }
}
