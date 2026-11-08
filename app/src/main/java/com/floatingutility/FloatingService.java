package com.floatingutility;

import android.app.Service;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class FloatingService extends Service {
    private WindowManager manager;
    private View panel;
    private CountDownTimer timer;
    private TextView status;
    private WindowManager.LayoutParams params;

    @Override public void onCreate() {
        super.onCreate();
        manager = (WindowManager) getSystemService(WINDOW_SERVICE);
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(16, 12, 16, 12);
        box.setBackgroundColor(Color.rgb(35, 42, 48));
        TextView heading = new TextView(this);
        heading.setText("● Floating Utility");
        heading.setTextColor(Color.GREEN);
        heading.setTextSize(17);
        box.addView(heading);
        status = new TextView(this);
        status.setText("Ready");
        status.setTextColor(Color.WHITE);
        box.addView(status);
        addButton(box, "Home", () -> { Intent i = new Intent(Intent.ACTION_MAIN); i.addCategory(Intent.CATEGORY_HOME); i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); startActivity(i); });
        addButton(box, "Timer · 15 seconds", this::startTimer);
        addButton(box, "Settings", () -> status.setText("Panel settings: drag the title to move; tap Timer to start."));
        addButton(box, "About", () -> status.setText("Floating Utility 1.0 · simple on-screen tools."));
        addButton(box, "Exit", this::stopSelf);
        panel = box;
        params = new WindowManager.LayoutParams(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.TOP | Gravity.START;
        params.x = 30; params.y = 180;
        heading.setOnTouchListener(new View.OnTouchListener() {
            int startX, startY; float downX, downY;
            @Override public boolean onTouch(View v, MotionEvent e) {
                if (e.getAction() == MotionEvent.ACTION_DOWN) { startX=params.x; startY=params.y; downX=e.getRawX(); downY=e.getRawY(); return true; }
                if (e.getAction() == MotionEvent.ACTION_MOVE) { params.x=startX+(int)(e.getRawX()-downX); params.y=startY+(int)(e.getRawY()-downY); manager.updateViewLayout(panel, params); return true; }
                return true;
            }
        });
        manager.addView(panel, params);
    }
    private void addButton(LinearLayout parent, String label, Runnable action) {
        Button b = new Button(this); b.setText(label); parent.addView(b);
        b.setOnClickListener(v -> action.run());
    }
    private void startTimer() {
        if (timer != null) timer.cancel();
        timer = new CountDownTimer(15000, 1000) {
            public void onTick(long ms) { status.setText("Timer: " + ((ms + 999) / 1000) + "s"); }
            public void onFinish() { status.setText("Timer complete"); }
        }.start();
    }
    @Override public void onDestroy() {
        if (timer != null) timer.cancel();
        if (panel != null && manager != null) manager.removeView(panel);
        super.onDestroy();
    }
    @Override public IBinder onBind(Intent intent) { return null; }
}
