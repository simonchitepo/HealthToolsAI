package com.cypher.zealth;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import java.util.Calendar;

public class WomensNotificationScheduler {

    public static final int REQ_DAILY = 9001;

    public static void scheduleDaily(Context ctx) {
        WomensStorage storage = new WomensStorage(ctx);

        int hour = storage.getNotifHour();
        int minute = storage.getNotifMinute();

        long triggerAt = nextTriggerAt(hour, minute);

        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;

        PendingIntent pi = buildPendingIntent(ctx);

        am.cancel(pi);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi);
        } else {
            am.setExact(AlarmManager.RTC_WAKEUP, triggerAt, pi);
        }
    }

    public static void cancelDaily(Context ctx) {
        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;
        PendingIntent pi = buildPendingIntent(ctx);
        am.cancel(pi);
    }

    private static PendingIntent buildPendingIntent(Context ctx) {
        Intent i = new Intent(ctx, WomensNotificationReceiver.class);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= 23) flags |= PendingIntent.FLAG_IMMUTABLE;
        return PendingIntent.getBroadcast(ctx, REQ_DAILY, i, flags);
    }

    private static long nextTriggerAt(int hour, int minute) {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        c.set(Calendar.HOUR_OF_DAY, hour);
        c.set(Calendar.MINUTE, minute);

        long now = System.currentTimeMillis();
        if (c.getTimeInMillis() <= now) {
            c.add(Calendar.DAY_OF_YEAR, 1);
        }
        return c.getTimeInMillis();
    }
}
