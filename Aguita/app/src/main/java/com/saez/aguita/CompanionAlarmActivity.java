package com.saez.aguita;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Intent;
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

import java.util.Calendar;

public class CompanionAlarmActivity extends Activity {
    private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
    private GradientDrawable bg(int c,int r){GradientDrawable g=new GradientDrawable();g.setColor(c);g.setCornerRadius(dp(r));return g;}
    private TextView text(String s,int sp,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextColor(Color.WHITE);t.setTextSize(sp);t.setGravity(Gravity.CENTER);if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;}
    private Button button(String s,int c){Button b=new Button(this);b.setText(s);b.setAllCaps(false);b.setTextColor(Color.WHITE);b.setTextSize(18);b.setTypeface(Typeface.DEFAULT,Typeface.BOLD);b.setBackground(bg(c,18));b.setMinHeight(dp(62));return b;}

    @Override protected void onCreate(Bundle b){
        super.onCreate(b);
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.O_MR1){setShowWhenLocked(true);setTurnScreenOn(true);}else getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED|WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        String title=getIntent().getStringExtra("title");if(title==null||title.trim().isEmpty())title="Recordatorio";
        long id=getIntent().getLongExtra("id",System.currentTimeMillis());
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setGravity(Gravity.CENTER);root.setPadding(dp(24),dp(36),dp(24),dp(30));root.setBackgroundColor(Color.rgb(8,10,35));
        root.addView(text("💜",74,false));root.addView(text("SIEMPRE TE RECUERDA",25,true));TextView msg=text(title,30,true);msg.setPadding(dp(8),dp(22),dp(8),dp(28));root.addView(msg);
        Button done=button("✓ Listo, gracias",Color.rgb(125,78,238));done.setOnClickListener(v->{stopService(new Intent(this,CompanionAlarmService.class));finishAndRemoveTask();});root.addView(done,new LinearLayout.LayoutParams(-1,-2));
        Button snooze=button("⏰ Recuérdame en 10 min",Color.rgb(41,78,145));LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(-1,-2);sp.topMargin=dp(12);final String reminderTitle=title;snooze.setOnClickListener(v->{scheduleAgain(id,reminderTitle,10);stopService(new Intent(this,CompanionAlarmService.class));finishAndRemoveTask();});root.addView(snooze,sp);
        TextView note=text("La alarma seguirá sonando hasta que confirmes o la pospongas.",13,false);note.setPadding(0,dp(22),0,0);root.addView(note);setContentView(root);
    }

    private void scheduleAgain(long id,String title,int minutes){
        AlarmManager am=(AlarmManager)getSystemService(ALARM_SERVICE);Intent i=new Intent(this,CompanionAlarmReceiver.class);i.putExtra("id",id);i.putExtra("title",title);PendingIntent pi=PendingIntent.getBroadcast(this,(int)(id%Integer.MAX_VALUE),i,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);long when=System.currentTimeMillis()+minutes*60_000L;if(am!=null){try{if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.S&&am.canScheduleExactAlarms())am.setAlarmClock(new AlarmManager.AlarmClockInfo(when,null),pi);else if(Build.VERSION.SDK_INT<Build.VERSION_CODES.S)am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,when,pi);else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,when,pi);}catch(Exception ignored){}}
    }

    @Override public void onBackPressed(){}
}
