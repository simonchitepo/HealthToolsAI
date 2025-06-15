package com.cypher.zealth;

public class CravingEntry {
    public long timestampMs;
    public int intensity1to5;
    public String note;

    public static CravingEntry of(long timestampMs, int intensity1to5, String note) {
        CravingEntry e = new CravingEntry();
        e.timestampMs = timestampMs;
        e.intensity1to5 = intensity1to5;
        e.note = note == null ? "" : note;
        return e;
    }
}
