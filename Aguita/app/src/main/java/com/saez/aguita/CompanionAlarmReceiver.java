package com.saez.aguita;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

public class CompanionAlarmReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        String title = intent != null ? intent.getStringExtra("title") : "Recordatorio";
        long id = intent != null ? intent.getLongExtra("id", System.currentTimeMillis()) : System.currentTimeMillis();
        Intent s = new Intent(context, CompanionAlarmService.class);
        s.putExtra("title", title); s.putExtra("id", id);
        try { if (Build.VERSION.SDK_INT >= 26) context.startForegroundService(s); else context.startService(s); } catch (Exception ignored) {}

        // Los recordatorios creados por la app son diarios por defecto: reprograma la misma alarma para mañana.
        long tomorrow = System.currentTimeMillis() + 24L * 60L * 60L * 1000L;
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent next = new Intent(context, CompanionAlarmReceiver.class); next.putExtra("title", title); next.putExtra("id", id);
        PendingIntent pi = PendingIntent.getBroadcast(context, (int)(id % Integer.MAX_VALUE), next, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        if (am != null) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && am.canScheduleExactAlarms()) am.setAlarmClock(new AlarmManager.AlarmClockInfo(tomorrow, null), pi);
                else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, tomorrow, pi);
                else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, tomorrow, pi);
            } catch (Exception ignored) {}
        }
    }
}
