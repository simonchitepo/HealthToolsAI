package com.cypher.zealth;

import java.util.ArrayList;
import java.util.List;

public class WomensDailyLogEntry {
    public long dayMs;                 
    public String flow;                
    public List<String> symptoms;      
    public int mood;                   
    public int energy;                 
    public String note;                

    public static WomensDailyLogEntry of(long dayMs) {
        WomensDailyLogEntry e = new WomensDailyLogEntry();
        e.dayMs = dayMs;
        e.flow = null;
        e.symptoms = new ArrayList<>();
        e.mood = 3;
        e.energy = 3;
        e.note = "";
        return e;
    }
}
