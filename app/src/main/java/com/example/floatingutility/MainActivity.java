package com.example.floatingutility;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.Manifest;
import android.content.pm.PackageManager;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.graphics.Color;

public class MainActivity extends Activity {
    private boolean waitingForOverlay = false;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(24), dp(36), dp(24), dp(24));
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setBackgroundColor(Color.rgb(15,20,29));

        TextView title = new TextView(this);
        title.setText("Floating Utility 1.2");
        title.setTextColor(Color.WHITE);
        title.setTextSize(28);
        title.setGravity(Gravity.CENTER);
        root.addView(title);

        TextView help = new TextView(this);
        help.setText("Floating shortcut, timer, saved notes and app shortcuts.\n\n1. Allow display over other apps.\n2. Return here and tap Start.\n3. Tap the floating F button to open or hide the panel.");
        help.setTextColor(Color.LTGRAY);
        help.setTextSize(16);
        help.setPadding(0, dp(24), 0, dp(24));
        root.addView(help);

        Button start = new Button(this);
        start.setText("START FLOATING UTILITY");
        root.addView(start);
        start.setOnClickListener(v -> startUtility());

        Button stop = new Button(this);
        stop.setText("STOP FLOATING UTILITY");
        root.addView(stop);
        stop.setOnClickListener(v -> {
            stopService(new Intent(this, FloatingService.class));
            android.widget.Toast.makeText(this, "Floating utility stopped", android.widget.Toast.LENGTH_SHORT).show();
        });

        setContentView(root);
    }

    private void startUtility() {
        if (!Settings.canDrawOverlays(this)) {
            waitingForOverlay = true;
            Intent i = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()));
            startActivity(i);
            return;
        }
        launchService();
    }

    @Override protected void onResume() {
        super.onResume();
        if (waitingForOverlay && Settings.canDrawOverlays(this)) {
            waitingForOverlay = false;
            launchService();
        }
    }

    private void launchService() {
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
        }
        try {
            Intent i = new Intent(this, FloatingService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(i);
            else startService(i);
            android.widget.Toast.makeText(this, "Floating Utility started", android.widget.Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            android.widget.Toast.makeText(this, "Could not start: " + e.getClass().getSimpleName(), android.widget.Toast.LENGTH_LONG).show();
        }
    }

    private int dp(float v) { return (int)(v * getResources().getDisplayMetrics().density + 0.5f); }
}
