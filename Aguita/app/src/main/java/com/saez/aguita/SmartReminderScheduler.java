package com.saez.aguita;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

public final class SmartReminderScheduler {
    private SmartReminderScheduler() {}

    private static void one(Context c, long id, String title, long when, String phase) {
        if (when <= System.currentTimeMillis()) return;
        AlarmManager am=(AlarmManager)c.getSystemService(Context.ALARM_SERVICE);
        if(am==null)return;
        Intent i=new Intent(c,CompanionAlarmReceiver.class);
        i.putExtra("id",id); i.putExtra("title",title); i.putExtra("phase",phase);
        PendingIntent pi=PendingIntent.getBroadcast(c,(int)((id+phase.hashCode())&0x7fffffff),i,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        try {
            if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.S && am.canScheduleExactAlarms())
                am.setAlarmClock(new AlarmManager.AlarmClockInfo(when,null),pi);
            else if(Build.VERSION.SDK_INT<Build.VERSION_CODES.S)
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,when,pi);
            else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,when,pi);
        } catch(Exception e){ am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,when,pi); }
    }

    public static void schedule(Context c,long id,String title,long when,int importance){
        one(c,id,title,when,"ahora");
        if(importance>=2) one(c,id+11,title,when-3*60*60*1000L,"3 horas antes");
        if(importance>=3) one(c,id+22,title,when-24*60*60*1000L,"1 día antes");
    }
}
