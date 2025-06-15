package com.cypher.zealth;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class WomensStorage {

    private static final String PREF = "womens_health_pref";

    private static final String KEY_LAST_PERIOD_MS = "last_period_ms";
    private static final String KEY_CYCLE_LEN = "cycle_len";
    private static final String KEY_PERIOD_LEN = "period_len";
    private static final String KEY_LUTEAL_LEN = "luteal_len";
    private static final String KEY_HISTORY = "period_history";
    private static final String KEY_DAILY_LOGS = "daily_logs";

    private static final String KEY_NOTIF_CYCLE = "notif_cycle";
    private static final String KEY_NOTIF_GUIDE = "notif_guide";
    private static final String KEY_NOTIF_ENCOURAGE = "notif_encourage";
    private static final String KEY_NOTIF_HOUR = "notif_hour";
    private static final String KEY_NOTIF_MIN = "notif_min";

    private final SharedPreferences sp;
    private final Gson gson = new Gson();

    private final Type historyType = new TypeToken<List<WomensPeriodEntry>>() {}.getType();
    private final Type dailyType = new TypeToken<List<WomensDailyLogEntry>>() {}.getType();

    public WomensStorage(Context ctx) {
        sp = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE);
    }

    public long getLastPeriodMs() { return sp.getLong(KEY_LAST_PERIOD_MS, -1L); }
    public int getCycleLen() { return sp.getInt(KEY_CYCLE_LEN, 28); }
    public int getPeriodLen() { return sp.getInt(KEY_PERIOD_LEN, 5); }
    public int getLutealLen() { return sp.getInt(KEY_LUTEAL_LEN, 14); }

    public void saveSettings(long lastPeriodMs, int cycleLen, int periodLen, int lutealLen) {
        sp.edit()
                .putLong(KEY_LAST_PERIOD_MS, lastPeriodMs)
                .putInt(KEY_CYCLE_LEN, cycleLen)
                .putInt(KEY_PERIOD_LEN, periodLen)
                .putInt(KEY_LUTEAL_LEN, lutealLen)
                .apply();
    }

   
    public void saveSettings(long lastPeriodMs, int cycleLen, int periodLen) {
        saveSettings(lastPeriodMs, cycleLen, periodLen, getLutealLen());
    }

   
    public boolean getNotifCycleEnabled() { return sp.getBoolean(KEY_NOTIF_CYCLE, true); }
    public boolean getNotifGuideEnabled() { return sp.getBoolean(KEY_NOTIF_GUIDE, true); }
    public boolean getNotifEncourageEnabled() { return sp.getBoolean(KEY_NOTIF_ENCOURAGE, true); }

    public void setNotifCycleEnabled(boolean v) { sp.edit().putBoolean(KEY_NOTIF_CYCLE, v).apply(); }
    public void setNotifGuideEnabled(boolean v) { sp.edit().putBoolean(KEY_NOTIF_GUIDE, v).apply(); }
    public void setNotifEncourageEnabled(boolean v) { sp.edit().putBoolean(KEY_NOTIF_ENCOURAGE, v).apply(); }

    public int getNotifHour() { return sp.getInt(KEY_NOTIF_HOUR, 9); }      // default 09:00
    public int getNotifMinute() { return sp.getInt(KEY_NOTIF_MIN, 0); }

    public void setNotifTime(int hour, int minute) {
        sp.edit().putInt(KEY_NOTIF_HOUR, hour).putInt(KEY_NOTIF_MIN, minute).apply();
    }

    public List<WomensPeriodEntry> loadHistory() {
        String json = sp.getString(KEY_HISTORY, null);
        if (json == null || json.trim().isEmpty()) return new ArrayList<>();
        try {
            List<WomensPeriodEntry> items = gson.fromJson(json, historyType);
            return items != null ? items : new ArrayList<>();
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private void saveHistory(List<WomensPeriodEntry> items) {
        Collections.sort(items, (a, b) -> Long.compare(b.startDateMs, a.startDateMs));
        sp.edit().putString(KEY_HISTORY, gson.toJson(items)).apply();
    }

    public void upsertHistoryStart(long startMs) {
        List<WomensPeriodEntry> items = loadHistory();
        for (WomensPeriodEntry e : items) {
            if (e.startDateMs == startMs) {
                saveHistory(items);
                return;
            }
        }
        items.add(WomensPeriodEntry.of(startMs));
        saveHistory(items);
    }

    public void markPeriodEnded(long startMs, long endMs) {
        List<WomensPeriodEntry> items = loadHistory();
        for (WomensPeriodEntry e : items) {
            if (e.startDateMs == startMs) {
                e.endDateMs = endMs;
                break;
            }
        }
        saveHistory(items);
    }

    public void deleteHistory(long startMs) {
        List<WomensPeriodEntry> items = loadHistory();
        for (int i = items.size() - 1; i >= 0; i--) {
            if (items.get(i).startDateMs == startMs) items.remove(i);
        }
        saveHistory(items);
    }

    public void clearHistory() {
        sp.edit().remove(KEY_HISTORY).apply();
    }

    public List<WomensDailyLogEntry> loadDailyLogs() {
        String json = sp.getString(KEY_DAILY_LOGS, null);
        if (json == null || json.trim().isEmpty()) return new ArrayList<>();
        try {
            List<WomensDailyLogEntry> items = gson.fromJson(json, dailyType);
            return items != null ? items : new ArrayList<>();
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    public void upsertDailyLog(WomensDailyLogEntry entry) {
        List<WomensDailyLogEntry> items = loadDailyLogs();
        boolean replaced = false;
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).dayMs == entry.dayMs) {
                items.set(i, entry);
                replaced = true;
                break;
            }
        }
        if (!replaced) items.add(entry);

        Collections.sort(items, (a, b) -> Long.compare(b.dayMs, a.dayMs));
        sp.edit().putString(KEY_DAILY_LOGS, gson.toJson(items)).apply();
    }

    public WomensDailyLogEntry getDailyLogForDay(long dayStartMs) {
        List<WomensDailyLogEntry> items = loadDailyLogs();
        for (WomensDailyLogEntry e : items) {
            if (e != null && e.dayMs == dayStartMs) return e;
        }
        return null;
    }

    public List<Integer> getObservedCycleLengthsDays() {
        List<WomensPeriodEntry> items = loadHistory();
        if (items.size() < 2) return new ArrayList<>();

        Collections.sort(items, (a, b) -> Long.compare(b.startDateMs, a.startDateMs));

        List<Integer> diffs = new ArrayList<>();
        for (int i = 0; i < items.size() - 1; i++) {
            long newer = items.get(i).startDateMs;
            long older = items.get(i + 1).startDateMs;
            long deltaMs = newer - older;
            int days = (int) Math.round(deltaMs / 86400000.0);
            if (days > 0 && days < 120) diffs.add(days);
        }
        return diffs;
    }

    public double getAverageCycleDays() {
        List<Integer> diffs = getObservedCycleLengthsDays();
        if (diffs.isEmpty()) return -1;
        double sum = 0;
        for (int d : diffs) sum += d;
        return sum / diffs.size();
    }

    public double getCycleStdDevDays() {
        List<Integer> diffs = getObservedCycleLengthsDays();
        if (diffs.size() < 2) return -1;
        double mean = getAverageCycleDays();
        double sse = 0;
        for (int d : diffs) {
            double x = d - mean;
            sse += x * x;
        }
        return Math.sqrt(sse / (diffs.size() - 1));
    }

    public int getLoggedCyclesCount() {
        return loadHistory().size();
    }
}
