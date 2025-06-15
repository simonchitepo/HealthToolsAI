package com.cypher.zealth;

public class PregnancyWeekItem {
    public final int week;
    public final int trimester;
    public final String tip;
    public final boolean isCurrent;

    public PregnancyWeekItem(int week, int trimester, String tip, boolean isCurrent) {
        this.week = week;
        this.trimester = trimester;
        this.tip = tip;
        this.isCurrent = isCurrent;
    }
}
