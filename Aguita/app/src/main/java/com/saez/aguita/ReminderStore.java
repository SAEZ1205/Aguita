package com.saez.aguita;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;

public final class ReminderStore {
    private static final String KEY = "susana_reminders";
    private ReminderStore() {}

    public static JSONArray all(Context c) {
        try { return new JSONArray(Prefs.p(c).getString(KEY, "[]")); }
        catch (Exception e) { return new JSONArray(); }
    }

    public static void add(Context c, long id, String title, long when, int importance) {
        try {
            JSONArray a = all(c);
            JSONObject o = new JSONObject();
            o.put("id", id); o.put("title", title); o.put("when", when); o.put("importance", importance); o.put("active", true);
            a.put(o); Prefs.p(c).edit().putString(KEY, a.toString()).apply();
        } catch (Exception ignored) {}
    }

    public static int count(Context c) { return all(c).length(); }

    public static String summary(Context c) {
        JSONArray a = all(c); StringBuilder s = new StringBuilder();
        for (int i=0;i<a.length();i++) try {
            JSONObject o=a.getJSONObject(i);
            java.text.SimpleDateFormat f=new java.text.SimpleDateFormat("dd/MM · HH:mm", java.util.Locale.getDefault());
            s.append("• ").append(o.optString("title")).append("\n  ").append(f.format(new java.util.Date(o.optLong("when"))));
            int imp=o.optInt("importance",1); if(imp>=3)s.append(" · importante"); s.append("\n\n");
        } catch(Exception ignored){}
        return s.length()==0?"Aún no tienes recordatorios. Puedes decir: ‘mamita, recuérdame mi cita el viernes a las 9’.":s.toString();
    }
}
