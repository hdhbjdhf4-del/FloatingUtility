package com.floatingutility;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(28, 40, 28, 28);
        TextView title = new TextView(this);
        title.setText("Floating Utility");
        title.setTextSize(26);
        root.addView(title);
        TextView info = new TextView(this);
        info.setText("Allow display over other apps, then start the floating panel.");
        root.addView(info);
        Button start = new Button(this);
        start.setText("Enable Floating Panel");
        root.addView(start);
        start.setOnClickListener(v -> {
            if (!Settings.canDrawOverlays(this)) {
                Intent permission = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()));
                startActivity(permission);
            } else {
                startService(new Intent(this, FloatingService.class));
                info.setText("Floating panel started. You can return to your other apps.");
            }
        });
        setContentView(root);
    }
    @Override protected void onResume() {
        super.onResume();
    }
}
