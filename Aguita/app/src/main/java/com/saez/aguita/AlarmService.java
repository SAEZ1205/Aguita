package com.saez.aguita;

import android.app.Notification;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;

public class AlarmService extends Service {
    public static final String ACTION_START = "com.saez.aguita.START_ALARM";
    public static final String ACTION_STOP = "com.saez.aguita.STOP_ALARM";
    public static final String EXTRA_NAG = "nag";

    private static final long MAX_RING_MS = 3 * 60_000L;

    private MediaPlayer player;
    private Vibrator vibrator;
    private AudioManager audioManager;
    private int previousAlarmVolume = -1;
    private PowerManager.WakeLock wakeLock;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable timeout = this::stopSelf;

    public static void start(Context context, boolean nag) {
        Intent i = new Intent(context, AlarmService.class);
        i.setAction(ACTION_START);
        i.putExtra(EXTRA_NAG, nag);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(i);
        } else {
            context.startService(i);
        }
    }

    public static void stop(Context context) {
        try { context.stopService(new Intent(context, AlarmService.class)); }
        catch (Exception ignored) { }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Prefs.ensureDefaults(this);
        NotificationHelper.createChannel(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_STOP.equals(intent.getAction())) {
            stopSelf();
            return START_NOT_STICKY;
        }

        boolean nag = intent != null && intent.getBooleanExtra(EXTRA_NAG, false);
        Notification notification = NotificationHelper.buildAlarmNotification(this, nag);
        startForeground(NotificationHelper.NOTIFICATION_ID, notification);
        startAttention();

        handler.removeCallbacks(timeout);
        handler.postDelayed(timeout, MAX_RING_MS);
        return START_NOT_STICKY;
    }

    private void startAttention() {
        if (player != null && player.isPlaying()) return;

        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        if (pm != null) {
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Aguita:AlarmWakeLock");
            try { wakeLock.acquire(MAX_RING_MS + 30_000L); } catch (Exception ignored) { }
        }

        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        if (Prefs.aggressive(this) && audioManager != null) {
            previousAlarmVolume = audioManager.getStreamVolume(AudioManager.STREAM_ALARM);
            int max = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM);
            if (previousAlarmVolume < max) {
                try { audioManager.setStreamVolume(AudioManager.STREAM_ALARM, max, 0); }
                catch (Exception ignored) { }
            }
        }

        try {
            Uri uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            if (uri == null) uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
            if (uri == null) uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            player = new MediaPlayer();
            player.setDataSource(this, uri);
            player.setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build());
            player.setLooping(true);
            player.prepare();
            player.start();
        } catch (Exception ignored) { }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            VibratorManager vm = (VibratorManager) getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
            if (vm != null) vibrator = vm.getDefaultVibrator();
        } else {
            vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        }
        if (vibrator != null && vibrator.hasVibrator()) {
            vibrator.vibrate(VibrationEffect.createWaveform(
                    new long[]{0, 900, 250, 900, 250, 1500}, 0));
        }
    }

    private void stopAttention() {
        handler.removeCallbacks(timeout);
        if (player != null) {
            try { player.stop(); } catch (Exception ignored) { }
            try { player.release(); } catch (Exception ignored) { }
            player = null;
        }
        if (vibrator != null) {
            try { vibrator.cancel(); } catch (Exception ignored) { }
            vibrator = null;
        }
        if (audioManager != null && previousAlarmVolume >= 0 && Prefs.aggressive(this)) {
            try { audioManager.setStreamVolume(AudioManager.STREAM_ALARM, previousAlarmVolume, 0); }
            catch (Exception ignored) { }
        }
        previousAlarmVolume = -1;
        if (wakeLock != null && wakeLock.isHeld()) {
            try { wakeLock.release(); } catch (Exception ignored) { }
        }
        wakeLock = null;
    }

    @Override
    public void onDestroy() {
        stopAttention();
        stopForeground(STOP_FOREGROUND_REMOVE);
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }
}
