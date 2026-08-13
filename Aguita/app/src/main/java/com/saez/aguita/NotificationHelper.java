package com.saez.aguita;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;

public final class NotificationHelper {
    public static final String CHANNEL_ID = "water_alarm_real_v2";
    public static final int NOTIFICATION_ID = 4401;
    private NotificationHelper() {}

    public static void createChannel(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
        NotificationManager nm = context.getSystemService(NotificationManager.class);
        if (nm == null) return;

        Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        AudioAttributes attrs = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();

        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Alarmas fuertes de Agüita",
                NotificationManager.IMPORTANCE_HIGH
        );
        channel.setDescription("Recordatorios de agua tipo alarma");
        channel.enableVibration(true);
        channel.setVibrationPattern(new long[]{0, 900, 250, 900, 250, 1500});
        channel.setSound(alarmSound, attrs);
        channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        if (nm.isNotificationPolicyAccessGranted()) {
            try { channel.setBypassDnd(true); } catch (Exception ignored) { }
        }
        nm.createNotificationChannel(channel);
    }

    public static PendingIntent fullScreenIntent(Context context, boolean nag) {
        Intent i = new Intent(context, AlarmActivity.class);
        i.putExtra("nag", nag);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        return PendingIntent.getActivity(
                context,
                nag ? 4302 : 4301,
                i,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
    }

    private static PendingIntent action(Context context, String action, int request) {
        Intent i = new Intent(context, AlarmActionReceiver.class);
        i.setAction(action);
        return PendingIntent.getBroadcast(context, request, i,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    public static Notification buildAlarmNotification(Context context, boolean nag) {
        PendingIntent full = fullScreenIntent(context, nag);
        String title = nag ? "🚨 Agüita insiste: toma agua" : "🚨 ¡Hora de tomar agua!";
        String text = Prefs.name(context) + ", toca “Ya tomé” cuando termines tu vaso.";

        return new Notification.Builder(context, CHANNEL_ID)
                .setSmallIcon(com.saez.aguita.R.drawable.ic_water)
                .setContentTitle(title)
                .setContentText(text)
                .setStyle(new Notification.BigTextStyle().bigText(text))
                .setCategory(Notification.CATEGORY_ALARM)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setPriority(Notification.PRIORITY_MAX)
                .setOngoing(true)
                .setAutoCancel(false)
                .setFullScreenIntent(full, true)
                .setContentIntent(full)
                .addAction(0, "✓ Ya tomé", action(context, AlarmActionReceiver.ACTION_DRANK, 4402))
                .addAction(0, "10 min", action(context, AlarmActionReceiver.ACTION_SNOOZE, 4403))
                .addAction(0, "Silenciar", action(context, AlarmActionReceiver.ACTION_SILENCE, 4404))
                .build();
    }
}
