package com.cypher.zealth;

public class WomensCycleModel {

    public enum Phase {
        MENSTRUAL,
        FOLLICULAR,
        OVULATION,
        LUTEAL
    }

    public static class CycleInfo {
        public int cycleDay;       
        public Phase phase;
        public String phaseTitle;   
        public int ovulationDay;    
        public int daysToNextPeriod;
        public boolean isFertileWindow;
    }

    public static CycleInfo getCycleInfo(long todayStartMs, long lastPeriodStartMs,
                                         int cycleLen, int periodLen, int lutealLen) {
        CycleInfo info = new CycleInfo();

        if (lastPeriodStartMs <= 0 || cycleLen <= 0) {
            info.cycleDay = 0;
            info.phase = Phase.FOLLICULAR;
            info.phaseTitle = "Cycle phase: —";
            info.ovulationDay = 0;
            info.daysToNextPeriod = 0;
            info.isFertileWindow = false;
            return info;
        }

        int day = daysBetweenStartOfDays(lastPeriodStartMs, todayStartMs) + 1; // day 1 = first day of period
       
        if (day > cycleLen) {
            int mod = (day - 1) % cycleLen;
            day = mod + 1;
        }
        if (day < 1) day = 1;

        info.cycleDay = day;

        int ovulationDay = Math.max(1, cycleLen - lutealLen); 
        info.ovulationDay = ovulationDay;

        int fertileStart = Math.max(1, ovulationDay - 5);
        int fertileEnd = Math.min(cycleLen, ovulationDay + 1);
        info.isFertileWindow = (day >= fertileStart && day <= fertileEnd);

        info.daysToNextPeriod = Math.max(0, cycleLen - day);

        if (day <= periodLen) {
            info.phase = Phase.MENSTRUAL;
            info.phaseTitle = "Menstrual phase (period)";
        } else if (day < ovulationDay) {
            info.phase = Phase.FOLLICULAR;
            info.phaseTitle = "Follicular phase";
        } else if (day == ovulationDay || (day >= ovulationDay - 1 && day <= ovulationDay + 1)) {
            info.phase = Phase.OVULATION;
            info.phaseTitle = "Ovulation phase (estimated)";
        } else {
            info.phase = Phase.LUTEAL;
            info.phaseTitle = "Luteal phase";
        }

        return info;
    }

    private static int daysBetweenStartOfDays(long startMs, long endMs) {
   
        double delta = (endMs - startMs) / 86400000.0;
        return (int) Math.floor(delta);
    }
}
