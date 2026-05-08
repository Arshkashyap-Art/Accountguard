package com.myapp.accountguard;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class AccountPickerActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        showPicker();
    }

    private void showPicker() {
        AccountManager am = AccountManager.get(this);
        Account[] accounts = am.getAccountsByType("com.google");

        SharedPreferences prefs =
            getSharedPreferences("accountguard", MODE_PRIVATE);
        String defaultAccount = prefs.getString("default_account", "");

        if (accounts.length == 0) {
            openYouTube();
            finish();
            return;
        }

        String[] displayNames = new String[accounts.length];
        int defaultIndex = 0;

        for (int i = 0; i < accounts.length; i++) {
            if (accounts[i].name.equals(defaultAccount)) {
                displayNames[i] = accounts[i].name + "  ⭐";
                defaultIndex = i;
            } else {
                displayNames[i] = accounts[i].name;
            }
        }

        final int[] selected = {defaultIndex};
        final Account[] finalAccounts = accounts;

        new AlertDialog.Builder(this)
            .setTitle("Open YouTube as")
            .setSingleChoiceItems(displayNames, defaultIndex,
                (dialog, which) -> selected[0] = which)

            .setPositiveButton("Open YouTube", (d, w) -> {
                String chosenName = finalAccounts[selected[0]].name;
                prefs.edit()
                    .putString("temp_account", chosenName)
                    .apply();
                Toast.makeText(this,
                    "Opening as: " + chosenName,
                    Toast.LENGTH_SHORT).show();
                openYouTube();
                finish();
            })

            .setNegativeButton("Use Default", (d, w) -> {
                Toast.makeText(this,
                    "Using default: " + defaultAccount,
                    Toast.LENGTH_SHORT).show();
                openYouTube();
                finish();
            })

            .setOnCancelListener(d -> {
                openYouTube();
                finish();
            })

            .setCancelable(true)
            .show();
    }

    private void openYouTube() {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.setPackage("com.google.android.youtube");
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        try {
            startActivity(intent);
        } catch (Exception e) {
            Intent store = new Intent(Intent.ACTION_VIEW,
                Uri.parse("market://details?id=com.google.android.youtube"));
            store.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(store);
        }
    }

    @Override
    public void onBackPressed() {
        openYouTube();
        finish();
    }
}
