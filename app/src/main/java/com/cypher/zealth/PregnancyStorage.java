package com.cypher.zealth;

import android.content.Context;
import android.content.SharedPreferences;

public class PregnancyStorage {

    private static final String PREFS = "pregnancy_storage";
    private static final String KEY_DUE_MS = "dueDateMs";
    private static final String KEY_NOTES = "notes";
    private static final String KEY_MODE = "mode";

    private final SharedPreferences sp;

    public PregnancyStorage(Context ctx) {
        sp = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public long getDueDateMs() {
        return sp.getLong(KEY_DUE_MS, -1L);
    }

    public void setDueDateMs(long ms) {
        sp.edit().putLong(KEY_DUE_MS, ms).apply();
    }

    public String getNotes() {
        return sp.getString(KEY_NOTES, "");
    }

    public void setNotes(String notes) {
        sp.edit().putString(KEY_NOTES, notes == null ? "" : notes).apply();
    }

    public int getMode(int defaultMode) {
        return sp.getInt(KEY_MODE, defaultMode);
    }

    public void setMode(int mode) {
        sp.edit().putInt(KEY_MODE, mode).apply();
    }

    public void clear() {
        sp.edit().clear().apply();
    }
}
