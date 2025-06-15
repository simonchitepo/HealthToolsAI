package com.cypher.zealth;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.List;

public class GroceryStorage {

    private static final String PREFS = "zealth_healthy_groceries";
    private static final String KEY = "categories_json";

    private final SharedPreferences sp;
    private final Gson gson = new Gson();

    public GroceryStorage(Context ctx) {
        sp = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public Type getListType() {
        return new TypeToken<List<GroceryCategory>>() {}.getType();
    }

    public List<GroceryCategory> loadOrDefault() {
        String json = sp.getString(KEY, null);
        if (json == null || json.trim().isEmpty()) return GroceryDefaults.build();
        try {
            List<GroceryCategory> parsed = gson.fromJson(json, getListType());
            return (parsed == null || parsed.isEmpty()) ? GroceryDefaults.build() : parsed;
        } catch (Exception e) {
            return GroceryDefaults.build();
        }
    }

    public void save(List<GroceryCategory> categories) {
        sp.edit().putString(KEY, gson.toJson(categories)).apply();
    }

    public void resetToDefault() {
        sp.edit().remove(KEY).apply();
    }
}
