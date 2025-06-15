package com.cypher.zealth;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import java.util.Calendar;
import java.util.Locale;

public class WomensNotificationReceiver extends BroadcastReceiver {

    private static final String CH_ID = "womens_health_daily";
    private static final int NOTIF_ID = 42001;

    @Override
    public void onReceive(Context context, Intent intent) {
        WomensStorage storage = new WomensStorage(context);

        boolean cycleOn = storage.getNotifCycleEnabled();
        boolean guideOn = storage.getNotifGuideEnabled();
        boolean encourageOn = storage.getNotifEncourageEnabled();

        boolean any = cycleOn || guideOn || encourageOn;
        if (!any) {
            WomensNotificationScheduler.cancelDaily(context);
            return;
        }

        long lastPeriod = storage.getLastPeriodMs();
        int cycleLen = storage.getCycleLen();
        int periodLen = storage.getPeriodLen();
        int lutealLen = storage.getLutealLen();

        long today = startOfDay(System.currentTimeMillis());
        WomensCycleModel.CycleInfo info = WomensCycleModel.getCycleInfo(today, lastPeriod, cycleLen, periodLen, lutealLen);

        String title = "Women’s Health";
        StringBuilder body = new StringBuilder();

        if (cycleOn && lastPeriod > 0) {
            body.append(info.phaseTitle)
                    .append(" • Day ")
                    .append(info.cycleDay)
                    .append(" of ~")
                    .append(cycleLen)
                    .append(". ");
        } else if (cycleOn) {
            body.append("Set your last period start date to enable phase predictions. ");
        }

        if (guideOn) {
            body.append(WomensGuidance.shortGuideForNotification(info)).append(" ");
        }

        if (encourageOn) {
            body.append(WomensGuidance.encouragementForNotification(info));
        }

        showNotification(context, title, body.toString().trim());

      
        WomensNotificationScheduler.scheduleDaily(context);
    }

    private void showNotification(Context ctx, String title, String text) {
        NotificationManager nm = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(
                    CH_ID,
                    "Women’s Health Daily",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            ch.setDescription("Cycle phase, guide, and encouragement notifications.");
            nm.createNotificationChannel(ch);
        }

        Intent open = new Intent(ctx, WomensHealthActivity.class);
        int pFlags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= 23) pFlags |= PendingIntent.FLAG_IMMUTABLE;
        PendingIntent pi = PendingIntent.getActivity(ctx, 0, open, pFlags);

        NotificationCompat.Builder b = new NotificationCompat.Builder(ctx, CH_ID)
                .setSmallIcon(R.drawable.ic_notification) 
                .setContentTitle(title)
                .setContentText(text)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(text))
                .setAutoCancel(true)
                .setContentIntent(pi)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        nm.notify(NOTIF_ID, b.build());
    }

    private long startOfDay(long ms) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(ms);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }
}
