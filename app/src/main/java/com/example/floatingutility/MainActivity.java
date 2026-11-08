package com.example.floatingutility;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    private boolean waitingForOverlayPermission = false;

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        showScreen();
    }

    private void showScreen() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(24), dp(40), dp(24), dp(24));
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setBackgroundColor(Color.rgb(15, 20, 29));

        TextView title = new TextView(this);
        title.setText("Floating Utility");
        title.setTextSize(28);
        title.setTypeface(null, Typeface.BOLD);
        title.setTextColor(Color.WHITE);
        root.addView(title);

        TextView info = new TextView(this);
        info.setText("A small floating shortcut and timer that stays above other apps.\n\n1. Allow display over other apps.\n2. Return here.\n3. The green F button will appear on your screen.");
        info.setTextSize(16);
        info.setTextColor(Color.rgb(205, 214, 226));
        info.setLineSpacing(dp(5), 1f);
        info.setPadding(0, dp(22), 0, dp(26));
        root.addView(info);

        Button start = new Button(this);
        start.setText("Enable floating icon");
        root.addView(start);
        start.setOnClickListener(v -> enableFloating());

        Button stop = new Button(this);
        stop.setText("Stop floating icon");
        root.addView(stop);
        stop.setOnClickListener(v -> {
            stopService(new Intent(this, FloatingService.class));
            Toast.makeText(this, "Floating icon stopped", Toast.LENGTH_SHORT).show();
        });

        setContentView(root);
    }

    private void enableFloating() {
        if (!Settings.canDrawOverlays(this)) {
            waitingForOverlayPermission = true;
            Intent i = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivity(i);
            return;
        }
        startFloatingService();
    }

    private void startFloatingService() {
        Intent service = new Intent(this, FloatingService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(service);
        } else {
            startService(service);
        }
        Toast.makeText(this, "Floating icon enabled", Toast.LENGTH_SHORT).show();
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (waitingForOverlayPermission && Settings.canDrawOverlays(this)) {
            waitingForOverlayPermission = false;
            startFloatingService();
        }
    }

    private int dp(float value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}