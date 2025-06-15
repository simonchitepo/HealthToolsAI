package com.cypher.zealth;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;

import java.util.Calendar;

public class MoodReminderReceiver extends BroadcastReceiver {

    private static final int REQ = 7421;

    public static PendingIntent pendingIntent(Context ctx) {
        Intent i = new Intent(ctx, MoodReminderReceiver.class);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) flags |= PendingIntent.FLAG_IMMUTABLE;
        return PendingIntent.getBroadcast(ctx, REQ, i, flags);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Intent open = new Intent(context, MentalHealthActivity.class);
        open.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        context.startActivity(open);

        SharedPreferences p = context.getSharedPreferences(MentalHealthActivity.PREF_UI, Context.MODE_PRIVATE);
        if (p.getBoolean(MentalHealthActivity.KEY_REMINDERS, false)) {
            int hour = p.getInt(MentalHealthActivity.KEY_REMINDER_HOUR, 20);
            int min = p.getInt(MentalHealthActivity.KEY_REMINDER_MIN, 0);

            AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            if (am == null) return;

            Calendar c = Calendar.getInstance();
            c.set(Calendar.HOUR_OF_DAY, hour);
            c.set(Calendar.MINUTE, min);
            c.set(Calendar.SECOND, 0);
            c.set(Calendar.MILLISECOND, 0);
            c.add(Calendar.DAY_OF_YEAR, 1);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, c.getTimeInMillis(), pendingIntent(context));
            } else {
                am.setExact(AlarmManager.RTC_WAKEUP, c.getTimeInMillis(), pendingIntent(context));
            }
        }
    }
}
