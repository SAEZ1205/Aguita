package com.saez.aguita;

import android.app.Application;

public class WaterApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        Prefs.ensureDefaults(this);
        NotificationHelper.createChannel(this);
    }
}
