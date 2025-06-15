package com.cypher.zealth;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MentalStorage {
    private static final String PREF = "mental_pref";
    private static final String KEY = "entries_json";

    private final SharedPreferences sp;
    private final Gson gson = new Gson();
    private final Type listType = new TypeToken<List<MentalEntry>>() {}.getType();

    public MentalStorage(Context ctx) {
        sp = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE);
    }

    public List<MentalEntry> load() {
        String json = sp.getString(KEY, "[]");
        List<MentalEntry> list;
        try {
            list = gson.fromJson(json, listType);
        } catch (Exception e) {
            list = new ArrayList<>();
        }
        if (list == null) list = new ArrayList<>();
        for (MentalEntry e : list) {
            if (e.tags == null) e.tags = new ArrayList<>();
        }
        Collections.sort(list, (a, b) -> Long.compare(b.timestampMs, a.timestampMs));
        return list;
    }

    public void saveAll(List<MentalEntry> entries) {
        if (entries == null) entries = new ArrayList<>();
        for (MentalEntry e : entries) {
            if (e.tags == null) e.tags = new ArrayList<>();
        }
        Collections.sort(entries, (a, b) -> Long.compare(b.timestampMs, a.timestampMs));
        sp.edit().putString(KEY, gson.toJson(entries, listType)).apply();
    }

    public void add(MentalEntry entry) {
        if (entry == null) return;
        List<MentalEntry> list = load();

        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).id == entry.id) {
                list.remove(i);
                break;
            }
        }

        list.add(entry);
        Collections.sort(list, (a, b) -> Long.compare(b.timestampMs, a.timestampMs));
        saveAll(list);
    }

    public void deleteById(long id) {
        List<MentalEntry> list = load();
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).id == id) {
                list.remove(i);
                break;
            }
        }
        saveAll(list);
    }

    public void clear() {
        saveAll(new ArrayList<>());
    }

    public void clearByType(MentalEntry.Type type) {
        List<MentalEntry> list = load();
        List<MentalEntry> keep = new ArrayList<>();
        for (MentalEntry e : list) if (e.type != type) keep.add(e);
        saveAll(keep);
    }

    public void clearLastDays(int days) {
        long now = System.currentTimeMillis();
        long start = now - (Math.max(1, days) * 24L * 60L * 60L * 1000L);

        List<MentalEntry> list = load();
        List<MentalEntry> keep = new ArrayList<>();
        for (MentalEntry e : list) if (e.timestampMs < start) keep.add(e);
        saveAll(keep);
    }
}
