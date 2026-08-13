package com.saez.aguita;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class AlarmReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Prefs.ensureDefaults(context);
        if (!Prefs.enabled(context)) return;

        boolean nag = intent != null && intent.getBooleanExtra("nag", false);
        if (!nag) {
            Prefs.p(context).edit().putInt("nagCount", 0).apply();
            AlarmScheduler.scheduleNext(context);
        }

        if (Prefs.aggressive(context)) {
            int count = Prefs.p(context).getInt("nagCount", 0);
            if (count < 3) {
                Prefs.p(context).edit().putInt("nagCount", count + 1).apply();
                AlarmScheduler.scheduleNag(context, 5);
            }
        }

        try {
            AlarmService.start(context, nag);
        } catch (Exception ignored) {
            // Si Android impide el servicio por faltar un permiso especial,
            // conservamos al menos la alerta de pantalla/notificación.
            android.app.NotificationManager nm = context.getSystemService(android.app.NotificationManager.class);
            if (nm != null) nm.notify(NotificationHelper.NOTIFICATION_ID,
                    NotificationHelper.buildAlarmNotification(context, nag));
        }
    }
}
