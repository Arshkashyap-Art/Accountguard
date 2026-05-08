package com.myapp.accountguard;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (!Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) return;

        SharedPreferences prefs =
            context.getSharedPreferences("accountguard", Context.MODE_PRIVATE);

        // Only auto-start if user had it running before reboot
        boolean wasRunning = prefs.getBoolean("service_running", false);
        if (wasRunning) {
            Intent service = new Intent(context, WatcherService.class);
            context.startForegroundService(service);
        }
    }
}
