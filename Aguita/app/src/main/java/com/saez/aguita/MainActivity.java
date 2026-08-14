package com.saez.aguita;

import android.Manifest;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends Activity implements TextToSpeech.OnInitListener {
    private static final int REQ_SPEECH = 9001;
    private static final int REQ_AUDIO = 9002;
    private final int bg = Color.rgb(6, 8, 32);
    private final int card = Color.rgb(19, 23, 58);
    private final int purple = Color.rgb(130, 83, 255);
    private final int blue = Color.rgb(73, 145, 255);
    private final int pink = Color.rgb(236, 91, 145);
    private LinearLayout root;
    private TextToSpeech tts;
    private EditText chatInput;
    private TextView chatLog;
    private int speechMode = 0; // 1 chat, 2 reminder

    private int dp(int v) { return Math.round(v * getResources().getDisplayMetrics().density); }

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Prefs.ensureDefaults(this);
        tts = new TextToSpeech(this, this);
        requestNeededPermissions();
        showHome();
    }

    @Override public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) tts.setLanguage(new Locale("es", "PE"));
    }

    @Override protected void onDestroy() {
        if (tts != null) { tts.stop(); tts.shutdown(); }
        super.onDestroy();
    }

    private GradientDrawable shape(int color, int radius) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color); g.setCornerRadius(dp(radius));
        return g;
    }

    private TextView text(String s, int sp, int color, boolean bold) {
        TextView t = new TextView(this); t.setText(s); t.setTextSize(sp); t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return t;
    }

    private Button btn(String label, int color) {
        Button b = new Button(this); b.setText(label); b.setAllCaps(false); b.setTextColor(Color.WHITE);
        b.setTextSize(16); b.setTypeface(Typeface.DEFAULT, Typeface.BOLD); b.setBackground(shape(color, 18)); b.setMinHeight(dp(56));
        return b;
    }

    private LinearLayout card() {
        LinearLayout c = new LinearLayout(this); c.setOrientation(LinearLayout.VERTICAL); c.setPadding(dp(18),dp(18),dp(18),dp(18)); c.setBackground(shape(card,24));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1,-2); lp.setMargins(0,0,0,dp(14)); c.setLayoutParams(lp); return c;
    }

    private void base(String title, String subtitle) {
        ScrollView sv = new ScrollView(this); sv.setFillViewport(true); sv.setBackgroundColor(bg);
        root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(dp(18),dp(24),dp(18),dp(110));
        sv.addView(root); root.addView(text(title, 29, Color.WHITE, true));
        TextView sub = text(subtitle, 14, Color.rgb(176,183,215), false); sub.setPadding(0,dp(4),0,dp(18)); root.addView(sub);
        setContentView(sv);
        addBottomNav();
    }

    private void addBottomNav() {
        LinearLayout nav = new LinearLayout(this); nav.setGravity(Gravity.CENTER); nav.setPadding(dp(8),dp(8),dp(8),dp(8)); nav.setBackground(shape(Color.rgb(12,15,44),22));
        String[] names = {"⌂ Inicio","💬 Hablar","⏰ Recordatorios","👤 Perfil"};
        for (int i=0;i<names.length;i++) {
            Button b = btn(names[i], Color.rgb(35,39,76)); b.setTextSize(12);
            final int k=i; b.setOnClickListener(v -> { if(k==0)showHome(); else if(k==1)showChat(); else if(k==2)showReminders(); else showProfile(); });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0,dp(52),1); lp.setMargins(dp(2),0,dp(2),0); nav.addView(b,lp);
        }
        root.addView(nav);
    }

    private void showHome() {
        base("Siempre 💜", "Tu compañera para hablar, recordar y acompañarte durante el día.");
        LinearLayout hero = card();
        TextView avatar = text("👵🏻", 78, Color.WHITE, false); avatar.setGravity(Gravity.CENTER); hero.addView(avatar);
        TextView hello = text("Hola, mi amor 💜\n¿Qué quieres hacer hoy?", 22, Color.WHITE, true); hello.setGravity(Gravity.CENTER); hero.addView(hello);
        TextView hint = text("Puedes hablarme de tu día, crear un recordatorio por voz o revisar tus alarmas.",14,Color.rgb(188,195,225),false); hint.setGravity(Gravity.CENTER); hint.setPadding(0,dp(10),0,dp(18)); hero.addView(hint);
        Button talk = btn("🎙️ Hablar conmigo", purple); talk.setOnClickListener(v -> showChat()); hero.addView(talk);
        Button rem = btn("⏰ Crear recordatorio", blue); LinearLayout.LayoutParams rlp=new LinearLayout.LayoutParams(-1,-2); rlp.topMargin=dp(10); rem.setOnClickListener(v -> startVoiceReminder()); hero.addView(rem,rlp);
        root.addView(hero,0);

        LinearLayout today=card(); today.addView(text("Mi día",20,Color.WHITE,true));
        today.addView(text("💧 Agua: " + Prefs.todayMl(this) + " / " + Prefs.targetMl(this) + " ml",16,Color.rgb(165,204,255),true));
        today.addView(text("⏰ " + ReminderStore.count(this) + " recordatorio(s) guardados",15,Color.rgb(205,190,255),false));
        Button water=btn("✓ Tomé un vaso de agua", Color.rgb(35,106,186)); water.setOnClickListener(v->{Prefs.addWater(this,Prefs.glass(this));Toast.makeText(this,"+"+Prefs.glass(this)+" ml 💧",Toast.LENGTH_SHORT).show();showHome();});
        LinearLayout.LayoutParams wlp=new LinearLayout.LayoutParams(-1,-2);wlp.topMargin=dp(12);today.addView(water,wlp);root.addView(today,1);
    }

    private void showChat() {
        base("Habla conmigo", "Escribe o mantén el micrófono. Puedo responderte con voz.");
        LinearLayout box=card();
        chatLog=text("Siempre: Hola 💜. Estoy aquí contigo. Cuéntame lo que quieras.\n",16,Color.WHITE,false); chatLog.setLineSpacing(dp(5),1f); box.addView(chatLog);
        chatInput=new EditText(this); chatInput.setHint("Escribe tu mensaje…"); chatInput.setHintTextColor(Color.rgb(135,143,178)); chatInput.setTextColor(Color.WHITE); chatInput.setBackground(shape(Color.rgb(30,34,70),16)); chatInput.setPadding(dp(14),dp(12),dp(14),dp(12));
        LinearLayout.LayoutParams ilp=new LinearLayout.LayoutParams(-1,-2);ilp.topMargin=dp(12);box.addView(chatInput,ilp);
        Button send=btn("Enviar",purple); send.setOnClickListener(v->sendChat(chatInput.getText().toString())); LinearLayout.LayoutParams slp=new LinearLayout.LayoutParams(-1,-2);slp.topMargin=dp(10);box.addView(send,slp);
        Button mic=btn("🎙️ Hablar por voz",pink); mic.setOnClickListener(v->{speechMode=1;startSpeech();}); LinearLayout.LayoutParams mlp=new LinearLayout.LayoutParams(-1,-2);mlp.topMargin=dp(10);box.addView(mic,mlp);
        root.addView(box,0);
    }

    private void sendChat(String msg) {
        if(msg==null||msg.trim().isEmpty())return; msg=msg.trim();
        if(chatInput!=null)chatInput.setText("");
        appendChat("Tú: "+msg);
        if (tryReminderFromText(msg)) return;
        String key=getPreferences(MODE_PRIVATE).getString("geminiKey","");
        if(!key.isEmpty()) askGemini(msg,key); else {
            String reply=localReply(msg); appendChat("Siempre: "+reply); speak(reply);
        }
    }

    private void appendChat(String s){ if(chatLog!=null)chatLog.append("\n"+s+"\n"); }

    private String localReply(String m) {
        String x=m.toLowerCase(Locale.ROOT);
        if(x.contains("triste")||x.contains("mal")||x.contains("cansad")) return "Te escucho. Si quieres, cuéntame qué fue lo más pesado de hoy. No tienes que resumirlo bonito.";
        if(x.contains("feliz")||x.contains("bien")||x.contains("logré")) return "Me alegra escucharte así 💜. Cuéntame qué pasó; quiero celebrarlo contigo.";
        if(x.contains("hola")) return "Hola 💜. Aquí estoy. ¿Cómo estuvo tu día?";
        if(x.contains("agua")) return "Si quieres puedo recordarte tomar agua. Solo dime una hora, por ejemplo: recuérdame tomar agua a las 3 de la tarde.";
        return "Te estoy escuchando 💜. Cuéntame un poco más; ¿qué fue lo que más te quedó dando vueltas?";
    }

    private void askGemini(String msg,String key) {
        appendChat("Siempre: pensando…");
        new Thread(() -> {
            try {
                URL u=new URL("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key="+key);
                HttpURLConnection c=(HttpURLConnection)u.openConnection(); c.setRequestMethod("POST"); c.setRequestProperty("Content-Type","application/json"); c.setDoOutput(true); c.setConnectTimeout(15000); c.setReadTimeout(30000);
                JSONObject req=new JSONObject(); JSONArray contents=new JSONArray(); JSONObject content=new JSONObject(); JSONArray parts=new JSONArray();
                String system="Eres Siempre, una compañera virtual cálida. Habla en español natural de Perú, breve, cariñosa sin fingir ser humana ni una persona fallecida. Escucha, conversa y ayuda. No digas que eres la abuela del usuario. Mensaje: "+msg;
                parts.put(new JSONObject().put("text",system)); content.put("parts",parts); contents.put(content); req.put("contents",contents);
                try(OutputStream os=c.getOutputStream()){os.write(req.toString().getBytes(StandardCharsets.UTF_8));}
                BufferedReader br=new BufferedReader(new InputStreamReader(c.getInputStream(),StandardCharsets.UTF_8)); StringBuilder sb=new StringBuilder(); String line; while((line=br.readLine())!=null)sb.append(line);
                JSONObject res=new JSONObject(sb.toString()); String reply=res.getJSONArray("candidates").getJSONObject(0).getJSONObject("content").getJSONArray("parts").getJSONObject(0).getString("text").trim();
                runOnUiThread(()->{ appendChat("Siempre: "+reply); speak(reply); });
            } catch(Exception e) { runOnUiThread(()->{String r=localReply(msg);appendChat("Siempre: "+r);speak(r);}); }
        }).start();
    }

    private void startVoiceReminder(){ speechMode=2; startSpeech(); }
    private void startSpeech(){
        if(Build.VERSION.SDK_INT>=23 && checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO},REQ_AUDIO);return;}
        Intent i=new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH); i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM); i.putExtra(RecognizerIntent.EXTRA_LANGUAGE,"es-PE"); i.putExtra(RecognizerIntent.EXTRA_PROMPT, speechMode==2?"Di el recordatorio y la hora":"Te escucho…");
        try{startActivityForResult(i,REQ_SPEECH);}catch(Exception e){Toast.makeText(this,"El reconocimiento de voz no está disponible.",Toast.LENGTH_LONG).show();}
    }

    @Override protected void onActivityResult(int requestCode,int resultCode,Intent data){
        super.onActivityResult(requestCode,resultCode,data); if(requestCode==REQ_SPEECH&&resultCode==RESULT_OK&&data!=null){ArrayList<String> r=data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);if(r!=null&&!r.isEmpty()){String spoken=r.get(0);if(speechMode==2){if(!tryReminderFromText(spoken)) showManualReminder(spoken);}else{if(chatInput!=null)chatInput.setText(spoken);sendChat(spoken);}}}
    }

    private boolean tryReminderFromText(String phrase){
        String lower=phrase.toLowerCase(Locale.ROOT); if(!(lower.contains("recuérd")||lower.contains("avis")||lower.contains("alarma")))return false;
        int hour=-1,minute=0; java.util.regex.Matcher m=java.util.regex.Pattern.compile("(?:a las|a la|las)\\s+(\\d{1,2})(?::(\\d{2}))?").matcher(lower);
        if(m.find()){hour=Integer.parseInt(m.group(1));if(m.group(2)!=null)minute=Integer.parseInt(m.group(2)); if(lower.contains("tarde")||lower.contains("noche")){if(hour<12)hour+=12;} if(lower.contains("mañana")&&hour==12)hour=0;}
        if(hour<0||hour>23){return false;}
        String title=phrase.replaceAll("(?i)recu[eé]rdame|av[ií]same|pon una alarma|alarma"," ").replaceAll("(?i)a las?\\s+\\d{1,2}(?::\\d{2})?"," ").replaceAll("(?i)de la tarde|de la mañana|de la noche"," ").replaceAll("\\s+"," ").trim();
        if(title.isEmpty())title="Recordatorio"; scheduleReminder(title,hour,minute,true); String reply="Listo 💜. Te recordaré “"+title+"” a las "+String.format(Locale.getDefault(),"%02d:%02d",hour,minute)+"."; if(chatLog!=null)appendChat("Siempre: "+reply);speak(reply); return true;
    }

    private void scheduleReminder(String title,int hour,int minute,boolean daily){
        Calendar cal=Calendar.getInstance();cal.set(Calendar.HOUR_OF_DAY,hour);cal.set(Calendar.MINUTE,minute);cal.set(Calendar.SECOND,0);cal.set(Calendar.MILLISECOND,0);if(cal.getTimeInMillis()<=System.currentTimeMillis())cal.add(Calendar.DAY_OF_MONTH,1);
        long id=System.currentTimeMillis(); ReminderStore.add(this,id,title,hour,minute,daily,true); scheduleAlarm(id,title,cal.getTimeInMillis()); Toast.makeText(this,"Recordatorio creado 💜",Toast.LENGTH_SHORT).show();
    }

    private void scheduleAlarm(long id,String title,long when){
        AlarmManager am=(AlarmManager)getSystemService(ALARM_SERVICE); Intent i=new Intent(this,CompanionAlarmReceiver.class); i.putExtra("id",id);i.putExtra("title",title); PendingIntent pi=PendingIntent.getBroadcast(this,(int)(id%Integer.MAX_VALUE),i,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        if(am!=null){try{if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.S&&am.canScheduleExactAlarms())am.setAlarmClock(new AlarmManager.AlarmClockInfo(when,null),pi);else if(Build.VERSION.SDK_INT<Build.VERSION_CODES.S)am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,when,pi);else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,when,pi);}catch(Exception e){am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,when,pi);}}
    }

    private void showManualReminder(String seed){
        LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(dp(18),dp(8),dp(18),0); EditText title=new EditText(this);title.setHint("¿Qué quieres recordar?");title.setText(seed); EditText time=new EditText(this);time.setHint("Hora 24h, ej. 15:30");box.addView(title);box.addView(time);
        new AlertDialog.Builder(this).setTitle("Nuevo recordatorio").setView(box).setPositiveButton("Crear",(d,w)->{try{String[]p=time.getText().toString().split(":");scheduleReminder(title.getText().toString(),Integer.parseInt(p[0]),p.length>1?Integer.parseInt(p[1]):0,true);showReminders();}catch(Exception e){Toast.makeText(this,"Usa una hora como 15:30",Toast.LENGTH_LONG).show();}}).setNegativeButton("Cancelar",null).show();
    }

    private void showReminders(){
        base("Mis recordatorios", "Crea alarmas hablando o de forma manual.");
        LinearLayout top=card();Button voice=btn("🎙️ Crear por voz",purple);voice.setOnClickListener(v->startVoiceReminder());top.addView(voice);Button manual=btn("＋ Añadir manualmente",blue);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.topMargin=dp(10);manual.setOnClickListener(v->showManualReminder(""));top.addView(manual,lp);root.addView(top,0);
        JSONArray arr=ReminderStore.all(this); for(int i=arr.length()-1;i>=0;i--){try{JSONObject o=arr.getJSONObject(i);LinearLayout c=card();c.addView(text("⏰  "+o.getString("title"),18,Color.WHITE,true));c.addView(text(String.format(Locale.getDefault(),"Todos los días · %02d:%02d",o.getInt("hour"),o.getInt("minute")),14,Color.rgb(177,186,219),false));root.addView(c,1);}catch(Exception ignored){}}
        LinearLayout water=card();water.addView(text("💧 Alarmas de agua",19,Color.WHITE,true));water.addView(text("La función original de Agüita sigue disponible con alarma fuerte, vibración y pantalla completa.",14,Color.rgb(177,186,219),false));Button settings=btn("Configurar agua",Color.rgb(35,106,186));settings.setOnClickListener(v->showWaterDialog());LinearLayout.LayoutParams x=new LinearLayout.LayoutParams(-1,-2);x.topMargin=dp(10);water.addView(settings,x);root.addView(water,1);
    }

    private void showWaterDialog(){
        String state=Prefs.enabled(this)?"activadas":"desactivadas"; new AlertDialog.Builder(this).setTitle("Recordatorios de agua").setMessage("Actualmente están "+state+".\n\nObjetivo: "+Prefs.targetMl(this)+" ml/día\nVaso: "+Prefs.glass(this)+" ml\nIntervalo: "+Prefs.interval(this)+" min").setPositiveButton(Prefs.enabled(this)?"Desactivar":"Activar",(d,w)->{Prefs.p(this).edit().putBoolean("enabled",!Prefs.enabled(this)).apply();if(Prefs.enabled(this))AlarmScheduler.scheduleNext(this);else AlarmScheduler.cancelAll(this);showReminders();}).setNeutralButton("Probar alarma",(d,w)->{try{AlarmService.start(this,false);startActivity(new Intent(this,AlarmActivity.class));}catch(Exception ignored){}}).setNegativeButton("Cerrar",null).show();
    }

    private void showProfile(){
        base("Mi perfil", "Configura la voz y, si quieres, conecta una IA generativa."); LinearLayout c=card();c.addView(text("👵🏻  Siempre",25,Color.WHITE,true));c.addView(text("Compañera virtual · voz + recordatorios",14,Color.rgb(180,187,216),false));
        Button key=btn("✨ Configurar Gemini API",purple); key.setOnClickListener(v->editApiKey());LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.topMargin=dp(15);c.addView(key,lp);
        Button test=btn("🔊 Probar voz",pink);test.setOnClickListener(v->speak("Hola. Aquí estoy contigo. ¿Cómo estuvo tu día?"));LinearLayout.LayoutParams tlp=new LinearLayout.LayoutParams(-1,-2);tlp.topMargin=dp(10);c.addView(test,tlp); root.addView(c,0);
        LinearLayout privacy=card();privacy.addView(text("Privacidad",19,Color.WHITE,true));privacy.addView(text("Sin clave de Gemini, el chat usa respuestas locales. Si añades una clave, tus mensajes se envían a Google Gemini para generar la respuesta. La clave se guarda solo en este teléfono.",14,Color.rgb(180,187,216),false));root.addView(privacy,1);
    }

    private void editApiKey(){EditText e=new EditText(this);e.setHint("AIza…");e.setText(getPreferences(MODE_PRIVATE).getString("geminiKey",""));new AlertDialog.Builder(this).setTitle("Gemini API key").setMessage("No la subas al repositorio. Se guardará localmente en este celular.").setView(e).setPositiveButton("Guardar",(d,w)->getPreferences(MODE_PRIVATE).edit().putString("geminiKey",e.getText().toString().trim()).apply()).setNegativeButton("Cancelar",null).show();}
    private void speak(String s){if(tts!=null)tts.speak(s,TextToSpeech.QUEUE_FLUSH,null,"siempre");}
    private void requestNeededPermissions(){if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},8001);}
}
