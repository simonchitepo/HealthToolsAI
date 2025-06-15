package com.cypher.zealth;

public class WeightLogEntry {
    public long dateMs;     
    public double weightKg;
    public String note;

    public static WeightLogEntry of(long dateMs, double weightKg, String note) {
        WeightLogEntry e = new WeightLogEntry();
        e.dateMs = dateMs;
        e.weightKg = weightKg;
        e.note = note;
        return e;
    }
}
