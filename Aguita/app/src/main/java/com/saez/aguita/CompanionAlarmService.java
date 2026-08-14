package com.saez.aguita;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;

public class CompanionAlarmService extends Service {
    public static final String CHANNEL = "siempre_reminders_v1";
    private MediaPlayer player;
    private Vibrator vibrator;

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        String title = intent != null ? intent.getStringExtra("title") : "Recordatorio";
        long id = intent != null ? intent.getLongExtra("id", System.currentTimeMillis()) : System.currentTimeMillis();
        createChannel();
        Intent open = new Intent(this, CompanionAlarmActivity.class);
        open.putExtra("title", title); open.putExtra("id", id);
        PendingIntent pi = PendingIntent.getActivity(this, (int)(id % Integer.MAX_VALUE), open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification n = new Notification.Builder(this, CHANNEL)
                .setSmallIcon(R.drawable.ic_water)
                .setContentTitle("💜 Siempre te recuerda")
                .setContentText(title)
                .setCategory(Notification.CATEGORY_ALARM)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setPriority(Notification.PRIORITY_MAX)
                .setOngoing(true)
                .setFullScreenIntent(pi, true)
                .setContentIntent(pi)
                .build();
        startForeground(7701, n);
        ring();
        return START_NOT_STICKY;
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
        NotificationManager nm = getSystemService(NotificationManager.class);
        if (nm == null) return;
        Uri sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        AudioAttributes attrs = new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).build();
        NotificationChannel ch = new NotificationChannel(CHANNEL, "Recordatorios de Siempre", NotificationManager.IMPORTANCE_HIGH);
        ch.setDescription("Alarmas personales creadas por voz o texto"); ch.enableVibration(true); ch.setSound(sound, attrs);
        nm.createNotificationChannel(ch);
    }

    private void ring() {
        try {
            Uri uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            player = new MediaPlayer(); player.setDataSource(this, uri);
            player.setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).build());
            player.setLooping(true); player.prepare(); player.start();
        } catch (Exception ignored) {}
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                VibratorManager vm = getSystemService(VibratorManager.class); if (vm != null) vibrator = vm.getDefaultVibrator();
            } else vibrator = (Vibrator)getSystemService(VIBRATOR_SERVICE);
            if (vibrator != null) vibrator.vibrate(VibrationEffect.createWaveform(new long[]{0,800,250,800,250,1200},0));
        } catch (Exception ignored) {}
    }

    @Override public void onDestroy() {
        if (player != null) { try { player.stop(); } catch (Exception ignored) {} try { player.release(); } catch (Exception ignored) {} }
        if (vibrator != null) try { vibrator.cancel(); } catch (Exception ignored) {}
        stopForeground(STOP_FOREGROUND_REMOVE); super.onDestroy();
    }
    @Override public IBinder onBind(Intent intent) { return null; }
}
