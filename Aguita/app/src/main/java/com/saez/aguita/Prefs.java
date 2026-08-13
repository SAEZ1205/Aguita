package com.saez.aguita;

import android.content.Context;
import android.content.SharedPreferences;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public final class Prefs {
    private static final String FILE = "aguita_prefs";
    private Prefs() {}

    public static SharedPreferences p(Context c) {
        return c.getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    public static void ensureDefaults(Context c) {
        SharedPreferences p = p(c);
        if (p.contains("initialized")) return;
        p.edit()
            .putBoolean("initialized", true)
            .putString("name", "Mamá")
            .putInt("age", 52)
            .putFloat("weight", 84f)
            .putInt("height", 162)
            .putString("sex", "Mujer")
            .putString("activity", "Ligera")
            .putInt("startHour", 8).putInt("startMinute", 0)
            .putInt("endHour", 22).putInt("endMinute", 0)
            .putInt("interval", 90)
            .putBoolean("autoInterval", true)
            .putInt("glass", 250)
            .putBoolean("enabled", false)
            .putBoolean("aggressive", true)
            .putBoolean("customTarget", false)
            .putInt("manualTarget", 2200)
            .apply();
    }

    public static String name(Context c) { return p(c).getString("name", "Mamá"); }
    public static int age(Context c) { return p(c).getInt("age", 52); }
    public static float weight(Context c) { return p(c).getFloat("weight", 84f); }
    public static int height(Context c) { return p(c).getInt("height", 162); }
    public static int interval(Context c) {
        SharedPreferences prefs = p(c);
        if (!prefs.getBoolean("autoInterval", true)) return prefs.getInt("interval", 90);

        int start = prefs.getInt("startHour", 8) * 60 + prefs.getInt("startMinute", 0);
        int end = prefs.getInt("endHour", 22) * 60 + prefs.getInt("endMinute", 0);
        int activeMinutes = end - start;
        if (activeMinutes < 180) return prefs.getInt("interval", 90);

        int glassMl = Math.max(100, glass(c));
        int reminders = Math.max(2, Math.round(targetMl(c) / (float) glassMl));
        int raw = activeMinutes / Math.max(1, reminders - 1);
        int roundedDownTo5 = Math.max(30, (raw / 5) * 5);
        return Math.min(240, roundedDownTo5);
    }
    public static int glass(Context c) { return p(c).getInt("glass", 250); }
    public static boolean enabled(Context c) { return p(c).getBoolean("enabled", false); }
    public static boolean aggressive(Context c) { return p(c).getBoolean("aggressive", true); }

    public static int targetMl(Context c) {
        if (p(c).getBoolean("customTarget", false)) {
            return Math.max(500, p(c).getInt("manualTarget", 2200));
        }
        return HydrationCalculator.estimateMl(
                weight(c),
                p(c).getString("activity", "Ligera")
        );
    }

    public static String todayKey() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
    }

    private static void resetIfNeeded(Context c) {
        SharedPreferences p = p(c);
        String today = todayKey();
        String saved = p.getString("progressDate", "");
        if (!today.equals(saved)) {
            p.edit().putString("progressDate", today)
                    .putInt("todayMl", 0)
                    .putString("history", "")
                    .apply();
        }
    }

    public static int todayMl(Context c) {
        resetIfNeeded(c);
        return p(c).getInt("todayMl", 0);
    }

    public static void addWater(Context c, int ml) {
        resetIfNeeded(c);
        SharedPreferences p = p(c);
        int total = p.getInt("todayMl", 0) + Math.max(0, ml);
        String time = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());
        String old = p.getString("history", "");
        String entry = time + "|" + ml;
        String history = old.isEmpty() ? entry : entry + ";" + old;
        p.edit().putInt("todayMl", total).putString("history", history).apply();
    }

    public static String history(Context c) {
        resetIfNeeded(c);
        return p(c).getString("history", "");
    }
}
