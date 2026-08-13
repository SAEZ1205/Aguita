package com.saez.aguita;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class AlarmActionReceiver extends BroadcastReceiver {
    public static final String ACTION_DRANK = "com.saez.aguita.DRANK";
    public static final String ACTION_SNOOZE = "com.saez.aguita.SNOOZE";
    public static final String ACTION_SILENCE = "com.saez.aguita.SILENCE";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) return;
        String action = intent.getAction();

        if (ACTION_DRANK.equals(action)) {
            Prefs.addWater(context, Prefs.glass(context));
            AlarmScheduler.cancelNag(context);
        } else if (ACTION_SNOOZE.equals(action)) {
            AlarmScheduler.cancelNag(context);
            AlarmScheduler.scheduleNag(context, 10);
        } else if (ACTION_SILENCE.equals(action)) {
            AlarmScheduler.cancelNag(context);
        } else {
            return;
        }

        AlarmService.stop(context);
        NotificationManager nm = context.getSystemService(NotificationManager.class);
        if (nm != null) nm.cancel(NotificationHelper.NOTIFICATION_ID);
    }
}
