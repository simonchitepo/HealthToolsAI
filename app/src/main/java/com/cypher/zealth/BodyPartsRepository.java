package com.cypher.zealth;

import android.content.Context;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.InputStream;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;

public class BodyPartsRepository {

    private final Map<String, BodyPartInfo> db;

    public BodyPartsRepository(Context ctx) {
        db = loadDb(ctx);
    }

    public BodyPartInfo get(String key) {
        if (db == null) return null;
        return db.get(key);
    }

    private Map<String, BodyPartInfo> loadDb(Context ctx) {
        try {
            InputStream is = ctx.getAssets().open("body_parts_db.json");
            byte[] bytes = new byte[is.available()];
            //noinspection ResultOfMethodCallIgnored
            is.read(bytes);
            is.close();
            String json = new String(bytes, StandardCharsets.UTF_8);

            Type t = new TypeToken<Map<String, BodyPartInfo>>() {}.getType();
            Map<String, BodyPartInfo> map = new Gson().fromJson(json, t);
            return map != null ? map : Collections.emptyMap();
        } catch (Exception e) {
            return Collections.emptyMap();
        }
    }
}
