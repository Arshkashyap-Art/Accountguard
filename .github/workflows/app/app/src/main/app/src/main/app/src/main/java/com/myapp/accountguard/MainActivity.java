package com.myapp.accountguard;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.AlertDialog;
import android.app.AppOpsManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Process;
import android.provider.Settings;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private SharedPreferences prefs;
    private TextView tvDefaultAccount, tvServiceStatus, tvPermUsage, tvPermOverlay;
    private Button btnSetDefault, btnToggleService, btnUsagePerm, btnOverlayPerm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefs = getSharedPreferences("accountguard", MODE_PRIVATE);

        tvDefaultAccount = findViewById(R.id.tv_default_account);
        tvServiceStatus  = findViewById(R.id.tv_service_status);
        tvPermUsage      = findViewById(R.id.tv_perm_usage);
        tvPermOverlay    = findViewById(R.id.tv_perm_overlay);
        btnSetDefault    = findViewById(R.id.btn_set_default);
        btnToggleService = findViewById(R.id.btn_toggle_service);
        btnUsagePerm     = findViewById(R.id.btn_usage_permission);
        btnOverlayPerm   = findViewById(R.id.btn_overlay_permission);

        btnSetDefault.setOnClickListener(v -> showAccountChooser());

        btnToggleService.setOnClickListener(v -> {
            if (!hasUsagePermission()) {
                Toast.makeText(this,
                    "Please grant Usage Access first", Toast.LENGTH_LONG).show();
                return;
            }
            if (!Settings.canDrawOverlays(this)) {
                Toast.makeText(this,
                    "Please grant Overlay permission first", Toast.LENGTH_LONG).show();
                return;
            }
            if (prefs.getString("default_account", "").isEmpty()) {
                Toast.makeText(this,
                    "Please choose a default account first", Toast.LENGTH_LONG).show();
                return;
            }
            toggleService();
        });

        btnUsagePerm.setOnClickListener(v ->
            startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)));

        btnOverlayPerm.setOnClickListener(v -> {
            Intent i = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:" + getPackageName()));
            startActivity(i);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateUI();
    }

    private void updateUI() {
        String saved = prefs.getString("default_account", "");
        tvDefaultAccount.setText(saved.isEmpty() ? "Not set yet" : saved);

        boolean running = prefs.getBoolean("service_running", false);
        if (running) {
            tvServiceStatus.setText("● Running");
            tvServiceStatus.setTextColor(0xFF4CAF50);
            btnToggleService.setText("Stop Service");
            btnToggleService.setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(0xFFF44336));
        } else {
            tvServiceStatus.setText("● Stopped");
            tvServiceStatus.setTextColor(0xFFF44336);
            btnToggleService.setText("Start Service");
            btnToggleService.setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(0xFF4CAF50));
        }

        if (hasUsagePermission()) {
            tvPermUsage.setText("✓ Granted");
            tvPermUsage.setTextColor(0xFF4CAF50);
            btnUsagePerm.setText("Usage Access: Granted ✓");
            btnUsagePerm.setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(0xFF4CAF50));
        } else {
            tvPermUsage.setText("✗ Not granted");
            tvPermUsage.setTextColor(0xFFF44336);
            btnUsagePerm.setText("Grant Usage Access (Required)");
            btnUsagePerm.setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(0xFFFF9800));
        }

        if (Settings.canDrawOverlays(this)) {
            tvPermOverlay.setText("✓ Granted");
            tvPermOverlay.setTextColor(0xFF4CAF50);
            btnOverlayPerm.setText("Overlay Permission: Granted ✓");
            btnOverlayPerm.setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(0xFF4CAF50));
        } else {
            tvPermOverlay.setText("✗ Not granted");
            tvPermOverlay.setTextColor(0xFFF44336);
            btnOverlayPerm.setText("Grant Overlay Permission (Required)");
            btnOverlayPerm.setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(0xFFFF9800));
        }
    }

    private void showAccountChooser() {
        AccountManager am = AccountManager.get(this);
        Account[] accounts = am.getAccountsByType("com.google");

        if (accounts.length == 0) {
            new AlertDialog.Builder(this)
                .setTitle("No Google Accounts Found")
                .setMessage("Please add a Google account in your phone Settings first.")
                .setPositiveButton("OK", null)
                .show();
            return;
        }

        String[] names = new String[accounts.length];
        for (int i = 0; i < accounts.length; i++) {
            names[i] = accounts[i].name;
        }

        new AlertDialog.Builder(this)
            .setTitle("Select Your Default Account")
            .setMessage("This account will always be used when you close the picker or leave YouTube")
            .setItems(names, (dialog, which) -> {
                prefs.edit()
                    .putString("default_account", accounts[which].name)
                    .apply();
                tvDefaultAccount.setText(accounts[which].name);
                Toast.makeText(this, "Default account set!", Toast.LENGTH_SHORT).show();
            })
            .show();
    }

    private void toggleService() {
        Intent service = new Intent(this, WatcherService.class);
        boolean running = prefs.getBoolean("service_running", false);

        if (!running) {
            startForegroundService(service);
            prefs.edit().putBoolean("service_running", true).apply(
