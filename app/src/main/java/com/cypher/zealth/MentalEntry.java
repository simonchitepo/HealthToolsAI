package com.cypher.zealth;

import java.util.ArrayList;
import java.util.List;

public class MentalEntry {
    public enum Type { MOOD, JOURNAL }
    public long id;          
    public long timestampMs;
    public Type type;
    public int mood;         
    public String note;
    public List<String> tags;
    public String journalText;

    public static MentalEntry moodEntry(long ts, int mood, String note, List<String> tags) {
        MentalEntry e = new MentalEntry();
        e.id = ts;
        e.timestampMs = ts;
        e.type = Type.MOOD;
        e.mood = mood;
        e.note = note;
        e.tags = (tags == null) ? new ArrayList<>() : new ArrayList<>(tags);
        return e;
    }

    public static MentalEntry journalEntry(long ts, String text) {
        MentalEntry e = new MentalEntry();
        e.id = ts;
        e.timestampMs = ts;
        e.type = Type.JOURNAL;
        e.journalText = text;
        return e;
    }
}
