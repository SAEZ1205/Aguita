package com.saez.aguita;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

public class CompanionAlarmReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        String title=intent!=null?intent.getStringExtra("title"):"Recordatorio";
        String phase=intent!=null?intent.getStringExtra("phase"):"ahora";
        long id=intent!=null?intent.getLongExtra("id",System.currentTimeMillis()):System.currentTimeMillis();
        Intent s=new Intent(context,CompanionAlarmService.class);
        s.putExtra("title",title); s.putExtra("phase",phase); s.putExtra("id",id);
        try { if(Build.VERSION.SDK_INT>=26) context.startForegroundService(s); else context.startService(s); } catch(Exception ignored) {}
    }
}
