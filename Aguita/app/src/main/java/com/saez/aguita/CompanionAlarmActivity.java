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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class CompanionAlarmActivity extends Activity {
    private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}    
    private GradientDrawable bg(int c,int r){GradientDrawable g=new GradientDrawable();g.setColor(c);g.setCornerRadius(dp(r));return g;}
    private TextView t(String s,int sp,boolean b){TextView v=new TextView(this);v.setText(s);v.setTextColor(Color.WHITE);v.setTextSize(sp);v.setGravity(Gravity.CENTER);if(b)v.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return v;}
    @Override protected void onCreate(Bundle b){super.onCreate(b);if(Build.VERSION.SDK_INT>=27){setShowWhenLocked(true);setTurnScreenOn(true);}else getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED|WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        String title=getIntent().getStringExtra("title");String phase=getIntent().getStringExtra("phase");if(title==null)title="Tienes algo importante";if(phase==null)phase="ahora";
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setGravity(Gravity.CENTER);root.setPadding(dp(28),dp(34),dp(28),dp(34));root.setBackgroundColor(Color.rgb(55,39,94));
        ImageView img=new ImageView(this);img.setImageResource(R.drawable.susana_avatar);img.setScaleType(ImageView.ScaleType.CENTER_CROP);img.setBackground(bg(Color.WHITE,100));root.addView(img,new LinearLayout.LayoutParams(dp(150),dp(150)));
        TextView name=t("SUSANA 💜",26,true);name.setPadding(0,dp(18),0,dp(8));root.addView(name);
        TextView msg=t(phase.equals("ahora")?"Mi amor, llegó la hora de recordar esto:":"Mi amor, te aviso con tiempo para que estés tranquila:",18,false);root.addView(msg);
        TextView what=t(title,28,true);what.setPadding(dp(4),dp(20),dp(4),dp(24));root.addView(what);
        Button done=new Button(this);done.setText("Entendido 💜");done.setAllCaps(false);done.setTextSize(20);done.setTypeface(Typeface.DEFAULT,Typeface.BOLD);done.setTextColor(Color.rgb(70,44,110));done.setBackground(bg(Color.WHITE,22));done.setOnClickListener(v->{stopService(new android.content.Intent(this,CompanionAlarmService.class));NotificationManager nm=getSystemService(NotificationManager.class);if(nm!=null)nm.cancel(7701);finishAndRemoveTask();});root.addView(done,new LinearLayout.LayoutParams(-1,dp(68)));
        setContentView(root);
    }
    @Override public void onBackPressed(){}
}
