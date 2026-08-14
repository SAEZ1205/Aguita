package com.saez.aguita;

import android.content.Context;
import org.json.JSONArray;
import org.json.JSONObject;

public final class ReminderStore {
    private static final String KEY = "companion_reminders";
    private ReminderStore() {}

    public static JSONArray all(Context c) {
        String raw = c.getSharedPreferences("siempre", Context.MODE_PRIVATE).getString(KEY, "[]");
        try { return new JSONArray(raw); } catch (Exception e) { return new JSONArray(); }
    }

    public static int count(Context c) { return all(c).length(); }

    public static void add(Context c, long id, String title, int hour, int minute, boolean daily, boolean enabled) {
        try {
            JSONArray a = all(c);
            JSONObject o = new JSONObject();
            o.put("id", id); o.put("title", title); o.put("hour", hour); o.put("minute", minute); o.put("daily", daily); o.put("enabled", enabled);
            a.put(o);
            c.getSharedPreferences("siempre", Context.MODE_PRIVATE).edit().putString(KEY, a.toString()).apply();
        } catch (Exception ignored) {}
    }
}
