package com.saez.aguita;

import android.app.Activity;
import android.app.NotificationManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class AlarmActivity extends Activity {
    private int dp(int v) { return Math.round(v * getResources().getDisplayMetrics().density); }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Prefs.ensureDefaults(this);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
        } else {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        }
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // Si esta pantalla se abrió manualmente como prueba, también arranca el motor real de alarma.
        try { AlarmService.start(this, getIntent().getBooleanExtra("nag", false)); } catch (Exception ignored) { }
        buildUi();
    }

    private GradientDrawable rounded(int color, int radius) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(dp(radius));
        return g;
    }

    private TextView text(String value, int sp, boolean bold) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextColor(Color.WHITE);
        t.setTextSize(sp);
        t.setGravity(Gravity.CENTER);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        t.setPadding(dp(12), dp(6), dp(12), dp(6));
        return t;
    }

    private Button button(String label, boolean primary) {
        Button b = new Button(this);
        b.setText(label);
        b.setTextSize(primary ? 20 : 16);
        b.setAllCaps(false);
        b.setTypeface(Typeface.DEFAULT, primary ? Typeface.BOLD : Typeface.NORMAL);
        b.setMinHeight(dp(primary ? 72 : 58));
        b.setTextColor(primary ? Color.rgb(14,58,109) : Color.WHITE);
        b.setBackground(rounded(primary ? Color.WHITE : Color.rgb(24,76,133), 20));
        return b;
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setPadding(dp(24), dp(34), dp(24), dp(30));
        root.setBackgroundColor(Color.rgb(10, 51, 97));

        boolean nag = getIntent().getBooleanExtra("nag", false);
        root.addView(text("💧", 78, false));
        root.addView(text(nag ? "TE LO RECUERDO OTRA VEZ" : "¡HORA DEL AGUA!", 30, true));

        TextView person = text(Prefs.name(this) + ", toca tomar un poco de agua ❤️", 21, false);
        person.setPadding(dp(8), dp(10), dp(8), 0);
        root.addView(person);

        TextView amount = text(Prefs.glass(this) + " ml", 42, true);
        amount.setPadding(dp(12), dp(18), dp(12), dp(26));
        root.addView(amount);

        Button drank = button("✓ YA TOMÉ MI VASO", true);
        drank.setOnClickListener(v -> {
            Prefs.addWater(this, Prefs.glass(this));
            AlarmScheduler.cancelNag(this);
            finishAlarm();
        });
        root.addView(drank, new LinearLayout.LayoutParams(-1, -2));

        Button snooze = button("⏰ RECUÉRDAME EN 10 MIN", false);
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(-1, -2);
        bp.topMargin = dp(12);
        snooze.setOnClickListener(v -> {
            AlarmScheduler.cancelNag(this);
            AlarmScheduler.scheduleNag(this, 10);
            finishAlarm();
        });
        root.addView(snooze, bp);

        Button stop = button("Silenciar esta vez", false);
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(-1, -2);
        sp.topMargin = dp(10);
        stop.setOnClickListener(v -> {
            AlarmScheduler.cancelNag(this);
            finishAlarm();
        });
        root.addView(stop, sp);

        TextView footer = text("Si no respondes, el modo insistente puede volver a avisarte. La siguiente alarma del día permanece programada.", 13, false);
        footer.setPadding(dp(6), dp(24), dp(6), 0);
        root.addView(footer);

        setContentView(root);
    }

    private void finishAlarm() {
        AlarmService.stop(this);
        NotificationManager nm = getSystemService(NotificationManager.class);
        if (nm != null) nm.cancel(NotificationHelper.NOTIFICATION_ID);
        finishAndRemoveTask();
    }

    @Override
    public void onBackPressed() {
        // Evita descartar la alarma accidentalmente con Atrás.
    }
}
