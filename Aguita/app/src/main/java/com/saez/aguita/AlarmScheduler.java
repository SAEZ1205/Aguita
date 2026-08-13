package com.saez.aguita;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.provider.Settings;

import java.util.Calendar;

public final class AlarmScheduler {
    private static final int MAIN_REQUEST = 5101;
    private static final int NAG_REQUEST = 5102;
    private AlarmScheduler() {}

    private static PendingIntent pending(Context c, int request, boolean nag) {
        Intent i = new Intent(c, AlarmReceiver.class);
        i.putExtra("nag", nag);
        return PendingIntent.getBroadcast(
                c,
                request,
                i,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
    }


    private static PendingIntent showApp(Context c) {
        Intent i = new Intent(c, MainActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        return PendingIntent.getActivity(c, 5199, i,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    public static boolean canExact(Context c) {
        AlarmManager am = (AlarmManager)c.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) return am.canScheduleExactAlarms();
        return true;
    }

    public static Intent exactAlarmSettingsIntent(Context c) {
        Intent i = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
        i.setData(android.net.Uri.parse("package:" + c.getPackageName()));
        return i;
    }

    public static long computeNext(Context c, long afterMillis) {
        int startH = Prefs.p(c).getInt("startHour", 8);
        int startM = Prefs.p(c).getInt("startMinute", 0);
        int endH = Prefs.p(c).getInt("endHour", 22);
        int endM = Prefs.p(c).getInt("endMinute", 0);
        int interval = Math.max(30, Prefs.interval(c));

        Calendar now = Calendar.getInstance();
        now.setTimeInMillis(afterMillis);

        Calendar start = (Calendar) now.clone();
        start.set(Calendar.HOUR_OF_DAY, startH);
        start.set(Calendar.MINUTE, startM);
        start.set(Calendar.SECOND, 0);
        start.set(Calendar.MILLISECOND, 0);

        Calendar end = (Calendar) now.clone();
        end.set(Calendar.HOUR_OF_DAY, endH);
        end.set(Calendar.MINUTE, endM);
        end.set(Calendar.SECOND, 0);
        end.set(Calendar.MILLISECOND, 0);

        if (afterMillis < start.getTimeInMillis()) return start.getTimeInMillis();
        if (afterMillis > end.getTimeInMillis()) {
            start.add(Calendar.DAY_OF_MONTH, 1);
            return start.getTimeInMillis();
        }

        long step = interval * 60_000L;
        long elapsed = Math.max(0, afterMillis - start.getTimeInMillis());
        long slots = (elapsed / step) + 1;
        long candidate = start.getTimeInMillis() + slots * step;
        if (candidate <= end.getTimeInMillis()) return candidate;
        start.add(Calendar.DAY_OF_MONTH, 1);
        return start.getTimeInMillis();
    }

    public static void scheduleNext(Context c) {
        if (!Prefs.enabled(c)) {
            cancelAll(c);
            return;
        }
        long trigger = computeNext(c, System.currentTimeMillis());
        scheduleAt(c, trigger, false);
        Prefs.p(c).edit().putLong("nextAlarm", trigger).apply();
    }

    public static void scheduleAt(Context c, long trigger, boolean nag) {
        AlarmManager am = (AlarmManager)c.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;
        PendingIntent pi = pending(c, nag ? NAG_REQUEST : MAIN_REQUEST, nag);
        if (canExact(c)) {
            try {
                AlarmManager.AlarmClockInfo info = new AlarmManager.AlarmClockInfo(trigger, showApp(c));
                am.setAlarmClock(info, pi);
                return;
            } catch (SecurityException ignored) { }
        }
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pi);
    }

    public static void scheduleNag(Context c, int minutes) {
        long trigger = System.currentTimeMillis() + minutes * 60_000L;
        scheduleAt(c, trigger, true);
        Prefs.p(c).edit().putLong("nagAlarm", trigger).apply();
    }

    public static void cancelNag(Context c) {
        AlarmManager am = (AlarmManager)c.getSystemService(Context.ALARM_SERVICE);
        if (am != null) am.cancel(pending(c, NAG_REQUEST, true));
        Prefs.p(c).edit().putInt("nagCount", 0).remove("nagAlarm").apply();
    }

    public static void cancelAll(Context c) {
        AlarmManager am = (AlarmManager)c.getSystemService(Context.ALARM_SERVICE);
        if (am != null) {
            am.cancel(pending(c, MAIN_REQUEST, false));
            am.cancel(pending(c, NAG_REQUEST, true));
        }
        Prefs.p(c).edit().remove("nextAlarm").remove("nagAlarm").putInt("nagCount", 0).apply();
    }
}
