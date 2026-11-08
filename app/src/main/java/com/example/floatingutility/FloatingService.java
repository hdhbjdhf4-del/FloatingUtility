package com.example.floatingutility;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class FloatingService extends Service {
    private static final String CHANNEL_ID = "floating_utility_channel";
    private static final int NOTIFICATION_ID = 41;

    private WindowManager windowManager;
    private View bubble;
    private View panel;
    private WindowManager.LayoutParams bubbleParams;
    private WindowManager.LayoutParams panelParams;
    private CountDownTimer timer;
    private TextView timerStatus;
    private float density;

    @Override
    public void onCreate() {
        super.onCreate();
        density = getResources().getDisplayMetrics().density;
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(NOTIFICATION_ID, buildNotification());
        if (!SettingsCanDraw()) {
            stopSelf();
            return START_NOT_STICKY;
        }
        if (bubble == null) addBubble();
        return START_STICKY;
    }

    private boolean SettingsCanDraw() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.M
                || android.provider.Settings.canDrawOverlays(this);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "Floating Utility", NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("Keeps the user-started floating shortcut available.");
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }

    private Notification buildNotification() {
        Intent open = new Intent(this, MainActivity.class);
        PendingIntent pending = PendingIntent.getActivity(
                this, 0, open,
                PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0));

        Notification.Builder builder = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? new Notification.Builder(this, CHANNEL_ID)
                : new Notification.Builder(this);
        return builder
                .setSmallIcon(android.R.drawable.ic_menu_view)
                .setContentTitle("Floating Utility is running")
                .setContentText("Tap the green F button to open the panel.")
                .setContentIntent(pending)
                .setOngoing(true)
                .build();
    }

    private void addBubble() {
        TextView icon = new TextView(this);
        icon.setText("F");
        icon.setTextColor(Color.WHITE);
        icon.setTextSize(21);
        icon.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        icon.setGravity(Gravity.CENTER);
        icon.setBackground(circle(Color.rgb(20, 166, 94)));
        int size = dp(54);
        icon.setElevation(dp(8));

        bubbleParams = new WindowManager.LayoutParams(
                size, size, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);
        bubbleParams.gravity = Gravity.TOP | Gravity.START;
        bubbleParams.x = dp(18);
        bubbleParams.y = dp(160);

        icon.setOnTouchListener(new View.OnTouchListener() {
            private int startX, startY;
            private float downX, downY;
            private boolean moved;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        startX = bubbleParams.x;
                        startY = bubbleParams.y;
                        downX = event.getRawX();
                        downY = event.getRawY();
                        moved = false;
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        int dx = (int) (event.getRawX() - downX);
                        int dy = (int) (event.getRawY() - downY);
                        if (Math.abs(dx) > dp(5) || Math.abs(dy) > dp(5)) moved = true;
                        bubbleParams.x = startX + dx;
                        bubbleParams.y = startY + dy;
                        try { windowManager.updateViewLayout(bubble, bubbleParams); }
                        catch (Exception ignored) {}
                        return true;
                    case MotionEvent.ACTION_UP:
                        if (!moved) togglePanel();
                        return true;
                }
                return true;
            }
        });

        bubble = icon;
        windowManager.addView(bubble, bubbleParams);
    }

    private void togglePanel() {
        if (panel != null) {
            removePanel();
            return;
        }

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(14), dp(12), dp(14), dp(14));
        card.setBackground(rounded(Color.rgb(27, 34, 45), dp(18)));
        card.setElevation(dp(12));

        TextView header = makeLabel("FLOATING UTILITY", 16, true);
        header.setTextColor(Color.rgb(117, 235, 174));
        card.addView(header);
        timerStatus = makeLabel("Ready · 15 second timer", 13, false);
        timerStatus.setPadding(dp(10), dp(8), dp(10), dp(12));
        card.addView(timerStatus);

        addButton(card, "Start 15-second timer", "TIMER");
        addButton(card, "Open Android Settings", "SETTINGS");
        addButton(card, "About this app", "ABOUT");
        addButton(card, "Close panel", "CLOSE");
        addButton(card, "Exit floating utility", "EXIT");

        panelParams = new WindowManager.LayoutParams(
                dp(250), WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT);
        panelParams.gravity = Gravity.TOP | Gravity.START;
        panelParams.x = Math.max(dp(8), bubbleParams.x - dp(5));
        panelParams.y = bubbleParams.y + dp(62);

        header.setOnTouchListener(createDragListener());
        panel = card;
        windowManager.addView(panel, panelParams);
    }

    private View.OnTouchListener createDragListener() {
        return new View.OnTouchListener() {
            private int startX, startY;
            private float downX, downY;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    startX = panelParams.x;
                    startY = panelParams.y;
                    downX = event.getRawX();
                    downY = event.getRawY();
                    return true;
                }
                if (event.getAction() == MotionEvent.ACTION_MOVE) {
                    panelParams.x = startX + (int) (event.getRawX() - downX);
                    panelParams.y = startY + (int) (event.getRawY() - downY);
                    try { windowManager.updateViewLayout(panel, panelParams); }
                    catch (Exception ignored) {}
                    return true;
                }
                return event.getAction() == MotionEvent.ACTION_UP;
            }
        };
    }

    private void addButton(LinearLayout parent, String title, String action) {
        TextView button = makeLabel(title, 14, true);
        button.setTextColor(Color.WHITE);
        button.setGravity(Gravity.CENTER_VERTICAL);
        button.setPadding(dp(12), dp(12), dp(12), dp(12));
        button.setBackground(rounded(Color.rgb(48, 59, 75), dp(10)));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(46));
        lp.topMargin = dp(6);
        parent.addView(button, lp);
        button.setOnClickListener(v -> {
            switch (action) {
                case "TIMER": startTimer(); break;
                case "SETTINGS":
                    Intent settings = new Intent(android.provider.Settings.ACTION_SETTINGS);
                    settings.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(settings);
                    break;
                case "ABOUT":
                    Toast.makeText(this, "Floating Utility · Version 1.1", Toast.LENGTH_SHORT).show();
                    break;
                case "CLOSE": removePanel(); break;
                case "EXIT": stopSelf(); break;
            }
        });
    }

    private void startTimer() {
        if (timer != null) timer.cancel();
        timerStatus.setText("Timer: 15s");
        timer = new CountDownTimer(15000, 250) {
            @Override
            public void onTick(long millis) {
                if (timerStatus != null) {
                    timerStatus.setText("Timer: " + ((millis + 999) / 1000) + "s");
                }
            }
            @Override
            public void onFinish() {
                if (timerStatus != null) timerStatus.setText("Timer complete");
                Toast.makeText(FloatingService.this, "15-second timer complete", Toast.LENGTH_SHORT).show();
            }
        }.start();
    }

    private TextView makeLabel(String text, int size, boolean bold) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(size);
        view.setTextColor(Color.WHITE);
        if (bold) view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return view;
    }

    private GradientDrawable rounded(int color, int radius) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(radius);
        return drawable;
    }

    private GradientDrawable circle(int color) {
        GradientDrawable drawable = rounded(color, dp(30));
        drawable.setStroke(dp(2), Color.WHITE);
        return drawable;
    }

    private int dp(float value) {
        return (int) (value * density + 0.5f);
    }

    private void removePanel() {
        if (panel != null) {
            try { windowManager.removeView(panel); } catch (Exception ignored) {}
            panel = null;
            timerStatus = null;
        }
    }

    @Override
    public void onDestroy() {
        if (timer != null) timer.cancel();
        removePanel();
        if (bubble != null) {
            try { windowManager.removeView(bubble); } catch (Exception ignored) {}
            bubble = null;
        }
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}