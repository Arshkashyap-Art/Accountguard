package com.myapp.accountguard;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import androidx.core.app.NotificationCompat;
import java.util.List;

public class WatcherService extends Service {

    private static final String WATCHED_PACKAGE = "com.google.android.youtube";
    private static final String CHANNEL_ID      = "accountguard";
    private static final long   POLL_MS         = 600;

    private Handler handler;
    private boolean youtubeWasOpen = false;
    private SharedPreferences prefs;

    @Override
    public void onCreate() {
        super.onCreate();
        prefs   = getSharedPreferences("accountguard", MODE_PRIVATE);
        handler = new Handler(Looper.getMainLooper());

        createNotificationChannel();
        startForeground(10, buildNotification());
        startWatching();
    }

    private void startWatching() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                String fg = getForegroundPackage();

                // YouTube just opened
                if (WATCHED_PACKAGE.equals(fg) && !youtubeWasOpen) {
                    youtubeWasOpen = true;
                    showAccountPicker();
                }

                // User left YouTube
                if (!WATCHED_PACKAGE.equals(fg) && youtubeWasOpen) {
                    youtubeWasOpen = false;
                    // Default account is already saved — nothing else to do
                    // Next time YouTube opens, picker will show again
                }

                handler.postDelayed(this, POLL_MS);
            }
        }, POLL_MS);
    }

    private String getForegroundPackage() {
        UsageStatsManager usm =
            (UsageStatsManager) getSystemService(Context.USAGE_STATS_SERVICE);

        long now  = System.currentTimeMillis();
        long then = now - 5000; // last 5 seconds

        List<UsageStats> stats =
            usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, then, now);

        if (stats == null || stats.isEmpty()) return "";

        UsageStats best = null;
        for (UsageStats s : stats) {
            if (best == null || s.getLastTimeUsed() > best.getLastTimeUsed()) {
                best = s;
            }
        }

        return best != null ? best.getPackageName() : "";
    }

    private void showAccountPicker() {
        Intent intent = new Intent(this, AccountPickerActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                      | Intent.FLAG_ACTIVITY_CLEAR_TOP
                      | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
    }

    private void createNotificationChannel() {
        NotificationChannel ch = new NotificationChannel(
            CHANNEL_ID,
            "AccountGuard",
            NotificationManager.IMPORTANCE_LOW);
        ch.setDescription("Watching for YouTube launches");
        getSystemService(NotificationManager.class).createNotificationChannel(ch);
    }

    private Notification buildNotification() {
        Intent openApp = new Intent(this, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(
            this, 0, openApp,
            PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("AccountGuard Active")
            .setContentText("Watching for YouTube • tap to open settings")
            .setSmallIcon(android.R.drawable.ic_menu_manage)
            .setContentIntent(pi)
            .setOngoing(true)
            .build();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (handler != null) handler.removeCallbacksAndMessages(null);
        getSharedPreferences("accountguard", MODE_PRIVATE)
            .edit().putBoolean("service_running", false).apply();
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }
}
