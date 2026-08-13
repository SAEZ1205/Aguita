package com.saez.aguita;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends Activity {
    private LinearLayout root;
    private TextView greeting, progressLabel, targetInfo, nextAlarm, motivation, schedulePreview, history, permissionStatus, profileSummary, alarmModeBadge;
    private ProgressBar progress;
    private Switch enabledSwitch, aggressiveSwitch;

    private int dp(int v) { return Math.round(v * getResources().getDisplayMetrics().density); }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Prefs.ensureDefaults(this);
        NotificationHelper.createChannel(this);
        buildUi();
        requestNotificationPermission();
        refresh();
    }

    @Override
    protected void onResume() {
        super.onResume();
        NotificationHelper.createChannel(this);
        if (Prefs.enabled(this) && AlarmScheduler.canExact(this)) AlarmScheduler.scheduleNext(this);
        if (root != null) refresh();
    }

    private GradientDrawable bg(int color, float radius) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(dp((int)radius));
        return g;
    }

    private TextView label(String s, int sp, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return t;
    }

    private LinearLayout card() {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setPadding(dp(18), dp(18), dp(18), dp(18));
        c.setBackground(bg(Color.WHITE, 22));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(0, 0, 0, dp(14));
        c.setLayoutParams(lp);
        c.setElevation(dp(2));
        return c;
    }

    private Button actionButton(String text, int color) {
        Button b = new Button(this);
        b.setText(text);
        b.setAllCaps(false);
        b.setTextSize(17);
        b.setTextColor(Color.WHITE);
        b.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        b.setBackground(bg(color, 18));
        b.setMinHeight(dp(58));
        b.setPadding(dp(12), dp(8), dp(12), dp(8));
        return b;
    }

    private void buildUi() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(Color.rgb(243, 248, 255));

        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(24), dp(18), dp(36));
        scroll.addView(root, new ScrollView.LayoutParams(-1, -2));

        TextView brand = label("💧 Agüita", 30, Color.rgb(14, 58, 109), true);
        root.addView(brand);
        TextView subtitle = label("Un recordatorio que sí se hace notar", 15, Color.rgb(73, 91, 112), false);
        subtitle.setPadding(0, dp(2), 0, dp(18));
        root.addView(subtitle);

        LinearLayout hero = card();
        greeting = label("", 22, Color.rgb(16, 32, 51), true);
        hero.addView(greeting);
        targetInfo = label("", 14, Color.rgb(73, 91, 112), false);
        targetInfo.setPadding(0, dp(4), 0, dp(16));
        hero.addView(targetInfo);

        progressLabel = label("", 32, Color.rgb(22, 119, 255), true);
        hero.addView(progressLabel);
        progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progress.setMax(1000);
        progress.setProgressTintList(android.content.res.ColorStateList.valueOf(Color.rgb(22,119,255)));
        LinearLayout.LayoutParams pp = new LinearLayout.LayoutParams(-1, dp(14));
        pp.setMargins(0, dp(10), 0, dp(10));
        hero.addView(progress, pp);
        nextAlarm = label("", 16, Color.rgb(14, 58, 109), true);
        hero.addView(nextAlarm);
        motivation = label("", 14, Color.rgb(73, 91, 112), false);
        motivation.setPadding(0, dp(8), 0, 0);
        hero.addView(motivation);
        root.addView(hero);

        LinearLayout addCard = card();
        addCard.addView(label("¿Tomaste agua?", 19, Color.rgb(16,32,51), true));
        TextView addHint = label("Regístralo con un toque. El progreso vuelve a cero cada día.", 14, Color.rgb(73,91,112), false);
        addHint.setPadding(0, dp(4), 0, dp(14));
        addCard.addView(addHint);

        Button mainAdd = actionButton("✓ TOMÉ MI VASO", Color.rgb(22,119,255));
        mainAdd.setOnClickListener(v -> addWater(Prefs.glass(this)));
        addCard.addView(mainAdd, new LinearLayout.LayoutParams(-1, -2));

        LinearLayout quick = new LinearLayout(this);
        quick.setGravity(Gravity.CENTER);
        quick.setPadding(0, dp(10), 0, 0);
        int[] amounts = {150, 250, 350, 500};
        for (int ml : amounts) {
            Button q = new Button(this);
            q.setText("+" + ml);
            q.setTextSize(13);
            q.setAllCaps(false);
            q.setOnClickListener(v -> addWater(ml));
            LinearLayout.LayoutParams qlp = new LinearLayout.LayoutParams(0, dp(48), 1f);
            qlp.setMargins(dp(2),0,dp(2),0);
            quick.addView(q, qlp);
        }
        addCard.addView(quick);
        root.addView(addCard);

        LinearLayout alarmCard = card();
        alarmCard.addView(label("Alarmas", 20, Color.rgb(16,32,51), true));
        enabledSwitch = new Switch(this);
        enabledSwitch.setText("Recordatorios activos");
        enabledSwitch.setTextSize(17);
        enabledSwitch.setPadding(0, dp(10), 0, dp(4));
        enabledSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> handleAlarmToggle(isChecked));
        alarmCard.addView(enabledSwitch);

        aggressiveSwitch = new Switch(this);
        aggressiveSwitch.setText("Modo insistente 🔊");
        aggressiveSwitch.setTextSize(17);
        aggressiveSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Prefs.p(this).edit().putBoolean("aggressive", isChecked).apply();
        });
        alarmCard.addView(aggressiveSwitch);

        TextView aggressiveHelp = label("Si está activo, la alarma vibra, repite y puede elevar temporalmente el volumen de ALARMA. Nunca modifica para siempre el volumen del teléfono.", 13, Color.rgb(73,91,112), false);
        aggressiveHelp.setPadding(0, dp(6), 0, dp(14));
        alarmCard.addView(aggressiveHelp);

        alarmModeBadge = label("", 14, Color.rgb(14,58,109), true);
        alarmModeBadge.setPadding(0, dp(4), 0, dp(12));
        alarmCard.addView(alarmModeBadge);

        Button test = actionButton("🚨 PROBAR ALARMA REAL AHORA", Color.rgb(14,58,109));
        test.setOnClickListener(v -> {
            try { AlarmService.start(this, false); } catch (Exception ignored) { }
            startActivity(new Intent(this, AlarmActivity.class));
        });
        alarmCard.addView(test);
        root.addView(alarmCard);

        LinearLayout scheduleCard = card();
        scheduleCard.addView(label("Tu día de agua", 20, Color.rgb(16,32,51), true));
        TextView scheduleHint = label("No tienes que pensar en la hora. Agüita organiza los próximos avisos por ti.", 14, Color.rgb(73,91,112), false);
        scheduleHint.setPadding(0, dp(4), 0, dp(10));
        scheduleCard.addView(scheduleHint);
        schedulePreview = label("", 16, Color.rgb(14,58,109), true);
        schedulePreview.setLineSpacing(dp(4), 1f);
        scheduleCard.addView(schedulePreview);
        root.addView(scheduleCard);

        LinearLayout profileCard = card();
        profileCard.addView(label("Perfil y objetivo", 20, Color.rgb(16,32,51), true));
        TextView profileHelp = label("La app adapta el objetivo y el horario a cada persona. Para actividad ligera contamos caminar o ejercicio suave; no hace falta entrenar fuerte.", 14, Color.rgb(73,91,112), false);
        profileHelp.setPadding(0, dp(5), 0, dp(8));
        profileCard.addView(profileHelp);
        profileSummary = label("", 15, Color.rgb(14,58,109), true);
        profileSummary.setPadding(0, 0, 0, dp(14));
        profileCard.addView(profileSummary);
        Button edit = actionButton("Editar mis datos y horario", Color.rgb(22,119,255));
        edit.setOnClickListener(v -> showProfileDialog());
        profileCard.addView(edit);
        root.addView(profileCard);

        LinearLayout permissions = card();
        permissions.addView(label("Configuración crítica del teléfono", 20, Color.rgb(16,32,51), true));
        permissionStatus = label("", 14, Color.rgb(73,91,112), false);
        permissionStatus.setPadding(0, dp(5), 0, dp(12));
        permissions.addView(permissionStatus);

        Button exact = actionButton("1. Permitir alarmas exactas", Color.rgb(14,58,109));
        exact.setOnClickListener(v -> openExactAlarmSettings());
        permissions.addView(exact);

        Button fullscreen = actionButton("2. Permitir alertas a pantalla completa", Color.rgb(14,58,109));
        LinearLayout.LayoutParams fsp = new LinearLayout.LayoutParams(-1, -2);
        fsp.topMargin = dp(8);
        fullscreen.setOnClickListener(v -> openFullScreenSettings());
        permissions.addView(fullscreen, fsp);

        Button notif = actionButton("3. Revisar notificaciones y sonido", Color.rgb(14,58,109));
        LinearLayout.LayoutParams nlp = new LinearLayout.LayoutParams(-1, -2);
        nlp.topMargin = dp(8);
        notif.setOnClickListener(v -> openNotificationSettings());
        permissions.addView(notif, nlp);

        Button dnd = actionButton("4. Permitir sonar en No molestar", Color.rgb(14,58,109));
        LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(-1, -2);
        dlp.topMargin = dp(8);
        dnd.setOnClickListener(v -> openDndSettings());
        permissions.addView(dnd, dlp);

        TextView note = label("En modo silencio normal, Agüita usa el canal de ALARMA. Para No molestar, Android exige que tú concedas acceso especial una vez. El sistema siempre conserva el control final.", 13, Color.rgb(73,91,112), false);
        note.setPadding(0, dp(14), 0, 0);
        permissions.addView(note);
        root.addView(permissions);

        LinearLayout historyCard = card();
        historyCard.addView(label("Hoy", 20, Color.rgb(16,32,51), true));
        history = label("", 15, Color.rgb(73,91,112), false);
        history.setPadding(0, dp(8), 0, 0);
        historyCard.addView(history);
        root.addView(historyCard);

        LinearLayout safety = card();
        safety.addView(label("⚕️ Sobre el objetivo de agua", 18, Color.rgb(16,32,51), true));
        TextView safetyText = label("Esta app organiza recordatorios; no diagnostica ni reemplaza indicaciones médicas. Si un profesional indicó una cantidad específica de líquido, usa “objetivo manual”. Si existe enfermedad renal, cardíaca o hepática, o una restricción de líquidos, consulta antes de aumentar la ingesta.", 14, Color.rgb(73,91,112), false);
        safetyText.setPadding(0, dp(7), 0, 0);
        safety.addView(safetyText);
        root.addView(safety);

        setContentView(scroll);
    }

    private void addWater(int ml) {
        Prefs.addWater(this, ml);
        AlarmScheduler.cancelNag(this);
        Toast.makeText(this, "+" + ml + " ml 💧", Toast.LENGTH_SHORT).show();
        refresh();
    }

    private void refresh() {
        int total = Prefs.todayMl(this);
        int target = Prefs.targetMl(this);
        int percent = target > 0 ? Math.min(1000, (int)Math.round(total * 1000.0 / target)) : 0;
        greeting.setText("Hola, " + Prefs.name(this) + " ❤️");
        progressLabel.setText(total + " / " + target + " ml");
        targetInfo.setText(String.format(Locale.getDefault(), "Objetivo orientativo: %.2f L · Vaso habitual: %d ml", target / 1000.0, Prefs.glass(this)));
        progress.setProgress(percent);
        int remaining = Math.max(0, target - total);
        int glassesLeft = Prefs.glass(this) > 0 ? (int)Math.ceil(remaining / (double)Prefs.glass(this)) : 0;
        if (remaining == 0) {
            motivation.setText("🎉 Objetivo orientativo completado por hoy. Sigue escuchando a tu sed y a las indicaciones de tu médico.");
        } else {
            motivation.setText("Te faltan aprox. " + remaining + " ml · " + glassesLeft + " vaso" + (glassesLeft == 1 ? "" : "s") + " de tu tamaño habitual.");
        }
        profileSummary.setText(String.format(Locale.getDefault(), "%d años · %.1f kg · %d cm · actividad %s", Prefs.age(this), Prefs.weight(this), Prefs.height(this), Prefs.p(this).getString("activity", "Ligera").toLowerCase(Locale.getDefault())));

        enabledSwitch.setOnCheckedChangeListener(null);
        enabledSwitch.setChecked(Prefs.enabled(this));
        enabledSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> handleAlarmToggle(isChecked));
        aggressiveSwitch.setChecked(Prefs.aggressive(this));

        if (Prefs.enabled(this)) {
            long next = Prefs.p(this).getLong("nextAlarm", 0L);
            if (next <= System.currentTimeMillis()) {
                next = AlarmScheduler.computeNext(this, System.currentTimeMillis());
                Prefs.p(this).edit().putLong("nextAlarm", next).apply();
            }
            String when = new SimpleDateFormat("EEE HH:mm", Locale.getDefault()).format(new Date(next));
            nextAlarm.setText("⏰ Próxima alarma: " + when + " · cada " + Prefs.interval(this) + " min");
        } else {
            nextAlarm.setText("⏸ Recordatorios desactivados");
        }

        schedulePreview.setText(buildSchedulePreview());
        boolean ready = AlarmScheduler.canExact(this) && canFullScreen() && canNotify();
        alarmModeBadge.setText(ready ? "✅ Modo alarma real listo" : "⚠️ Falta completar permisos para la experiencia completa");

        String raw = Prefs.history(this);
        if (raw.isEmpty()) {
            history.setText("Todavía no registraste agua hoy.");
        } else {
            StringBuilder sb = new StringBuilder();
            String[] entries = raw.split(";");
            int max = Math.min(entries.length, 8);
            for (int i = 0; i < max; i++) {
                String[] parts = entries[i].split("\\|");
                if (parts.length == 2) sb.append("💧 ").append(parts[0]).append("  ·  ").append(parts[1]).append(" ml\n");
            }
            history.setText(sb.toString().trim());
        }

        permissionStatus.setText(buildPermissionStatus());
    }

    private String buildPermissionStatus() {
        boolean notifOk = canNotify();
        boolean exactOk = AlarmScheduler.canExact(this);
        boolean fullOk = canFullScreen();
        NotificationManager nm = getSystemService(NotificationManager.class);
        boolean dndOk = nm != null && nm.isNotificationPolicyAccessGranted();
        return (notifOk ? "✅" : "⚠️") + " Notificaciones   " +
                (exactOk ? "✅" : "⚠️") + " Exacta   " +
                (fullOk ? "✅" : "⚠️") + " Pantalla completa\n" +
                (dndOk ? "✅" : "⚠️") + " No molestar";
    }

    private boolean canNotify() {
        return Build.VERSION.SDK_INT < 33 || checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
    }

    private boolean canFullScreen() {
        if (Build.VERSION.SDK_INT < 34) return true;
        NotificationManager nm = getSystemService(NotificationManager.class);
        return nm != null && nm.canUseFullScreenIntent();
    }

    private void handleAlarmToggle(boolean isChecked) {
        if (isChecked && !AlarmScheduler.canExact(this)) {
            Prefs.p(this).edit().putBoolean("enabled", false).apply();
            enabledSwitch.setOnCheckedChangeListener(null);
            enabledSwitch.setChecked(false);
            enabledSwitch.setOnCheckedChangeListener((buttonView, checked) -> handleAlarmToggle(checked));
            Toast.makeText(this, "Primero permite ‘Alarmas y recordatorios’ para que sea puntual aunque la app esté cerrada.", Toast.LENGTH_LONG).show();
            openExactAlarmSettings();
            return;
        }
        Prefs.p(this).edit().putBoolean("enabled", isChecked).apply();
        if (isChecked) AlarmScheduler.scheduleNext(this); else AlarmScheduler.cancelAll(this);
        refresh();
    }

    private String buildSchedulePreview() {
        int sh = Prefs.p(this).getInt("startHour", 8);
        int sm = Prefs.p(this).getInt("startMinute", 0);
        int eh = Prefs.p(this).getInt("endHour", 22);
        int em = Prefs.p(this).getInt("endMinute", 0);
        String range = String.format(Locale.getDefault(), "⏱ %02d:%02d → %02d:%02d · cada %d min", sh, sm, eh, em, Prefs.interval(this));
        if (!Prefs.enabled(this)) return range + "\nActiva los recordatorios para programar las alarmas.";
        long cursor = System.currentTimeMillis();
        StringBuilder sb = new StringBuilder(range).append("\nPróximas: ");
        for (int i = 0; i < 3; i++) {
            long n = AlarmScheduler.computeNext(this, cursor);
            if (i > 0) sb.append("  •  ");
            sb.append(new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date(n)));
            cursor = n + 1000L;
        }
        return sb.toString();
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 8001);
        }
    }

    private void openExactAlarmSettings() {
        if (Build.VERSION.SDK_INT >= 31) {
            try { startActivity(AlarmScheduler.exactAlarmSettingsIntent(this)); }
            catch (Exception e) { startActivity(new Intent(Settings.ACTION_SETTINGS)); }
        } else Toast.makeText(this, "Tu versión de Android no necesita este permiso especial.", Toast.LENGTH_SHORT).show();
    }

    private void openFullScreenSettings() {
        if (Build.VERSION.SDK_INT >= 34) {
            Intent i = new Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT,
                    Uri.parse("package:" + getPackageName()));
            try { startActivity(i); }
            catch (Exception e) { openNotificationSettings(); }
        } else {
            Toast.makeText(this, "En esta versión se gestiona desde las notificaciones.", Toast.LENGTH_SHORT).show();
            openNotificationSettings();
        }
    }

    private void openNotificationSettings() {
        Intent i = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
        i.putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
        startActivity(i);
    }

    private void openDndSettings() {
        try {
            startActivity(new Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS));
        } catch (Exception e) {
            openNotificationSettings();
        }
    }

    private EditText field(String hint, String value, int inputType) {
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setText(value);
        e.setInputType(inputType);
        e.setSelectAllOnFocus(true);
        e.setPadding(dp(10), dp(8), dp(10), dp(8));
        return e;
    }

    private TextView smallLabel(String s) {
        TextView t = label(s, 13, Color.rgb(73,91,112), true);
        t.setPadding(0, dp(9), 0, 0);
        return t;
    }

    private void showProfileDialog() {
        ScrollView sv = new ScrollView(this);
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(18), dp(6), dp(18), dp(16));
        sv.addView(box);

        EditText name = field("Nombre", Prefs.name(this), InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        EditText age = field("Edad", String.valueOf(Prefs.age(this)), InputType.TYPE_CLASS_NUMBER);
        EditText weight = field("Peso (kg)", String.valueOf(Prefs.weight(this)), InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        EditText height = field("Talla (cm)", String.valueOf(Prefs.height(this)), InputType.TYPE_CLASS_NUMBER);
        box.addView(smallLabel("Nombre")); box.addView(name);
        box.addView(smallLabel("Edad")); box.addView(age);
        box.addView(smallLabel("Peso en kg")); box.addView(weight);
        box.addView(smallLabel("Talla en cm")); box.addView(height);

        Spinner sex = new Spinner(this);
        String[] sexes = {"Mujer", "Hombre", "Prefiero no indicar"};
        sex.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, sexes));
        String savedSex = Prefs.p(this).getString("sex", "Mujer");
        for (int i=0;i<sexes.length;i++) if (sexes[i].equals(savedSex)) sex.setSelection(i);
        box.addView(smallLabel("Sexo (solo perfil)")); box.addView(sex);

        Spinner activity = new Spinner(this);
        String[] activities = {"Ligera", "Moderada", "Alta"};
        activity.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, activities));
        String savedActivity = Prefs.p(this).getString("activity", "Ligera");
        for (int i=0;i<activities.length;i++) if (activities[i].equals(savedActivity)) activity.setSelection(i);
        box.addView(smallLabel("Actividad habitual")); box.addView(activity);

        int sh=Prefs.p(this).getInt("startHour",8), sm=Prefs.p(this).getInt("startMinute",0);
        int eh=Prefs.p(this).getInt("endHour",22), em=Prefs.p(this).getInt("endMinute",0);
        EditText start = field("08:00", String.format(Locale.US,"%02d:%02d",sh,sm), InputType.TYPE_CLASS_DATETIME);
        EditText end = field("22:00", String.format(Locale.US,"%02d:%02d",eh,em), InputType.TYPE_CLASS_DATETIME);
        CheckBox autoInterval = new CheckBox(this);
        autoInterval.setText("Distribuir automáticamente mis alarmas durante el día");
        autoInterval.setChecked(Prefs.p(this).getBoolean("autoInterval", true));
        EditText interval = field("Intervalo manual (min)", String.valueOf(Prefs.p(this).getInt("interval", 90)), InputType.TYPE_CLASS_NUMBER);
        EditText glass = field("Vaso (ml)", String.valueOf(Prefs.glass(this)), InputType.TYPE_CLASS_NUMBER);
        box.addView(smallLabel("Inicio de recordatorios (HH:mm)")); box.addView(start);
        box.addView(smallLabel("Fin de recordatorios (HH:mm)")); box.addView(end);
        box.addView(autoInterval);
        box.addView(smallLabel("Si desactivas lo automático: intervalo manual")); box.addView(interval);
        box.addView(smallLabel("Tamaño de tu vaso")); box.addView(glass);

        CheckBox custom = new CheckBox(this);
        custom.setText("Usar objetivo manual indicado por mí/médico");
        custom.setChecked(Prefs.p(this).getBoolean("customTarget", false));
        box.addView(custom);
        EditText manual = field("Objetivo manual ml", String.valueOf(Prefs.p(this).getInt("manualTarget",2200)), InputType.TYPE_CLASS_NUMBER);
        box.addView(manual);

        TextView formula = label("Estimación automática: 30 ml/kg como punto de partida orientativo, con un ajuste pequeño por actividad. Si activas la distribución automática, Agüita reparte los avisos entre tu hora de inicio y fin según tu objetivo y tamaño de vaso. Puedes reemplazar el objetivo por uno indicado por un profesional.", 12, Color.rgb(73,91,112), false);
        formula.setPadding(0, dp(12), 0, 0);
        box.addView(formula);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Tu perfil de Agüita")
                .setView(sv)
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Guardar", null)
                .create();

        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            try {
                float w = Float.parseFloat(weight.getText().toString().trim().replace(',', '.'));
                int a = Integer.parseInt(age.getText().toString().trim());
                int h = Integer.parseInt(height.getText().toString().trim());
                int inter = Integer.parseInt(interval.getText().toString().trim());
                int gl = Integer.parseInt(glass.getText().toString().trim());
                int[] st = parseTime(start.getText().toString());
                int[] en = parseTime(end.getText().toString());
                int manualMl = Integer.parseInt(manual.getText().toString().trim());
                if (w < 30 || w > 250 || a < 18 || a > 110 || h < 120 || h > 230 || inter < 30 || inter > 360 || gl < 50 || gl > 1000) {
                    throw new IllegalArgumentException();
                }
                Prefs.p(this).edit()
                        .putString("name", name.getText().toString().trim().isEmpty() ? "Tú" : name.getText().toString().trim())
                        .putInt("age", a).putFloat("weight", w).putInt("height", h)
                        .putString("sex", sex.getSelectedItem().toString())
                        .putString("activity", activity.getSelectedItem().toString())
                        .putInt("startHour", st[0]).putInt("startMinute", st[1])
                        .putInt("endHour", en[0]).putInt("endMinute", en[1])
                        .putInt("interval", inter).putBoolean("autoInterval", autoInterval.isChecked()).putInt("glass", gl)
                        .putBoolean("customTarget", custom.isChecked())
                        .putInt("manualTarget", Math.max(500, Math.min(manualMl, 6000)))
                        .apply();
                if (Prefs.enabled(this)) AlarmScheduler.scheduleNext(this);
                dialog.dismiss();
                refresh();
            } catch (Exception ex) {
                Toast.makeText(this, "Revisa los datos. Usa horas como 08:00 y valores razonables.", Toast.LENGTH_LONG).show();
            }
        }));
        dialog.show();
    }

    private int[] parseTime(String s) {
        String[] p = s.trim().split(":");
        if (p.length != 2) throw new IllegalArgumentException();
        int h = Integer.parseInt(p[0]);
        int m = Integer.parseInt(p[1]);
        if (h < 0 || h > 23 || m < 0 || m > 59) throw new IllegalArgumentException();
        return new int[]{h,m};
    }
}
