package com.example.floatingutility;

import android.app.Service;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.os.IBinder;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class FloatingService extends Service {
    private WindowManager wm;
    private View bubble;
    private View panel;
    private WindowManager.LayoutParams bubbleParams;
    private WindowManager.LayoutParams panelParams;

    @Override public void onCreate() {
        super.onCreate();
        wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        int type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        bubbleParams = new WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            type, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT);
        bubbleParams.gravity = Gravity.TOP | Gravity.START;
        bubbleParams.x = 24; bubbleParams.y = 180;

        TextView b = new TextView(this);
        b.setText("F");
        b.setTextColor(Color.WHITE);
        b.setTextSize(22);
        b.setTypeface(null, Typeface.BOLD);
        b.setGravity(Gravity.CENTER);
        b.setBackgroundColor(Color.rgb(20,150,75));
        b.setPadding(18, 12, 18, 12);
        bubble = b;
        bubble.setOnClickListener(v -> togglePanel());
        makeDraggable(bubble, bubbleParams);
        wm.addView(bubble, bubbleParams);
    }

    private void togglePanel() {
        if (panel != null) { removePanel(); return; }
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(18, 16, 18, 16);
        box.setBackgroundColor(Color.rgb(32,36,43));

        TextView heading = label("FLOATING UTILITY", 18, true);
        box.addView(heading);
        addAction(box, "Home", "Floating utility is active.");
        addAction(box, "Timer", "Timer shortcut selected.");
        addAction(box, "Settings", "No device settings are changed.");
        addAction(box, "About", "Simple overlay utility • Version 1.0");
        addAction(box, "Close panel", "CLOSE");
        addAction(box, "Exit", "EXIT");

        panel = box;
        panelParams = new WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT);
        panelParams.gravity = Gravity.TOP | Gravity.START;
        panelParams.x = 100; panelParams.y = 180;
        makeDraggable(panel, panelParams);
        wm.addView(panel, panelParams);
    }

    private TextView label(String text, int size, boolean bold) {
        TextView t = new TextView(this);
        t.setText(text); t.setTextSize(size); t.setTextColor(Color.WHITE);
        t.setPadding(12, 10, 12, 10);
        if (bold) t.setTypeface(null, Typeface.BOLD);
        return t;
    }

    private void addAction(LinearLayout box, String text, String action) {
        TextView item = label(text, 16, false);
        box.addView(item);
        item.setOnClickListener(v -> {
            if ("CLOSE".equals(action)) removePanel();
            else if ("EXIT".equals(action)) stopSelf();
            else Toast.makeText(this, action, Toast.LENGTH_SHORT).show();
        });
    }

    private void removePanel() {
        if (panel != null) {
            try { wm.removeView(panel); } catch (Exception ignored) {}
            panel = null;
        }
    }

    private void makeDraggable(View view, WindowManager.LayoutParams params) {
        view.setOnTouchListener(new View.OnTouchListener() {
            int startX, startY;
            float touchX, touchY;
            @Override public boolean onTouch(View v, MotionEvent e) {
                if (e.getAction() == MotionEvent.ACTION_DOWN) {
                    startX = params.x; startY = params.y;
                    touchX = e.getRawX(); touchY = e.getRawY();
                    return false;
                }
                if (e.getAction() == MotionEvent.ACTION_MOVE) {
                    params.x = startX + (int)(e.getRawX() - touchX);
                    params.y = startY + (int)(e.getRawY() - touchY);
                    try { wm.updateViewLayout(view, params); } catch (Exception ignored) {}
                    return true;
                }
                return false;
            }
        });
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        return START_NOT_STICKY;
    }

    @Override public void onDestroy() {
        removePanel();
        if (bubble != null) {
            try { wm.removeView(bubble); } catch (Exception ignored) {}
            bubble = null;
        }
        super.onDestroy();
    }

    @Override public IBinder onBind(Intent intent) { return null; }
}
