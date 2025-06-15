package com.cypher.zealth;

public class BodyEntry {
    public String area;
    public int severity; 
    public String note;
    public long startedDateMs; 
    public long createdAtMs;

    public static BodyEntry of(String area, int severity, String note, long startedDateMs, long createdAtMs) {
        BodyEntry e = new BodyEntry();
        e.area = area;
        e.severity = severity;
        e.note = note;
        e.startedDateMs = startedDateMs;
        e.createdAtMs = createdAtMs;
        return e;
    }
}
