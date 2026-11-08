package com.example.floatingutility;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.graphics.Color;
import android.view.Gravity;

public class MainActivity extends Activity {
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(28, 36, 28, 28);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setBackgroundColor(Color.rgb(245,247,250));

        TextView title = new TextView(this);
        title.setText("Floating Utility");
        title.setTextSize(26);
        title.setTextColor(Color.rgb(25,35,45));
        root.addView(title);

        TextView info = new TextView(this);
        info.setText("A simple draggable floating panel. It does not modify games, apps, or device memory.");
        info.setTextSize(16);
        info.setPadding(0, 18, 0, 24);
        root.addView(info);

        Button start = new Button(this);
        start.setText("Enable floating icon");
        root.addView(start);
        start.setOnClickListener(v -> {
            if (!Settings.canDrawOverlays(this)) {
                Intent i = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
                startActivity(i);
            } else {
                startService(new Intent(this, FloatingService.class));
                finish();
            }
        });

        setContentView(root);
    }

    @Override protected void onResume() {
        super.onResume();
    }
}
