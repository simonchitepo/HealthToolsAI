package com.cypher.zealth;


public class WomensPeriodEntry {

    public long startDateMs;

   
    public long endDateMs = -1L;

   
    public String note = "";

    public static WomensPeriodEntry of(long startDateMs) {
        WomensPeriodEntry e = new WomensPeriodEntry();
        e.startDateMs = startDateMs;
        e.endDateMs = -1L;
        e.note = "";
        return e;
    }
}
