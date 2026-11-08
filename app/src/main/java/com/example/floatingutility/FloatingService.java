package com.example.floatingutility;

import android.app.*;
import android.content.*;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.os.*;
import android.provider.Settings;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.widget.*;
import java.util.*;

public class FloatingService extends Service {
    private static final String CHANNEL = "floating_utility_channel";
    private WindowManager wm;
    private View bubble, panel;
    private WindowManager.LayoutParams bubbleLp, panelLp;
    private float density;
    private CountDownTimer timer;
    private TextView timerLabel;
    private EditText notesInput;
    private SharedPreferences prefs;

    @Override public void onCreate() {
        super.onCreate();
        density = getResources().getDisplayMetrics().density;
        wm = (WindowManager)getSystemService(WINDOW_SERVICE);
        prefs = getSharedPreferences("floating_utility", MODE_PRIVATE);
        createChannel();
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(41, notification());
        if (Build.VERSION.SDK_INT >= 23 && !Settings.canDrawOverlays(this)) {
            stopSelf();
            return START_NOT_STICKY;
        }
        if (bubble == null) addBubble();
        return START_STICKY;
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel c = new NotificationChannel(CHANNEL, "Floating Utility", NotificationManager.IMPORTANCE_LOW);
            c.setDescription("Notification for the floating shortcut and timer.");
            ((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).createNotificationChannel(c);
        }
    }

    private Notification notification() {
        Intent open = new Intent(this, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(this, 0, open,
                PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0));
        Notification.Builder b = Build.VERSION.SDK_INT >= 26 ? new Notification.Builder(this, CHANNEL) : new Notification.Builder(this);
        return b.setSmallIcon(android.R.drawable.ic_menu_view)
                .setContentTitle("Floating Utility is running")
                .setContentText("Tap the F bubble to show or hide your tools.")
                .setContentIntent(pi).setOngoing(true).build();
    }

    private void addBubble() {
        TextView f = new TextView(this);
        f.setText("F"); f.setTextColor(Color.WHITE); f.setTextSize(22);
        f.setTypeface(Typeface.DEFAULT, Typeface.BOLD); f.setGravity(Gravity.CENTER);
        f.setBackground(shape(Color.rgb(20,166,94), dp(30), Color.WHITE));
        f.setElevation(dp(8));
        bubbleLp = new WindowManager.LayoutParams(dp(56), dp(56),
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT);
        bubbleLp.gravity = Gravity.TOP | Gravity.START; bubbleLp.x = dp(16); bubbleLp.y = dp(170);
        f.setOnTouchListener(new View.OnTouchListener() {
            int sx, sy; float dx, dy; boolean moved;
            public boolean onTouch(View v, MotionEvent e) {
                switch(e.getAction()) {
                    case MotionEvent.ACTION_DOWN: sx=bubbleLp.x; sy=bubbleLp.y; dx=e.getRawX(); dy=e.getRawY(); moved=false; return true;
                    case MotionEvent.ACTION_MOVE:
                        int mx=(int)(e.getRawX()-dx), my=(int)(e.getRawY()-dy);
                        if(Math.abs(mx)>dp(5)||Math.abs(my)>dp(5)) moved=true;
                        bubbleLp.x=Math.max(0,sx+mx); bubbleLp.y=Math.max(0,sy+my);
                        try { wm.updateViewLayout(bubble,bubbleLp); } catch(Exception ignored) {}
                        return true;
                    case MotionEvent.ACTION_UP: if(!moved) togglePanel(); return true;
                } return true;
            }
        });
        bubble=f; wm.addView(bubble,bubbleLp);
    }

    private void togglePanel() { if(panel==null) showPanel(); else hidePanel(); }

    private void showPanel() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(13),dp(12),dp(13),dp(12));
        box.setBackground(shape(Color.rgb(25,32,43),dp(17),Color.rgb(75,90,110)));
        box.setElevation(dp(12));

        TextView header = label("FLOATING UTILITY  •  v1.2",16,true);
        header.setTextColor(Color.rgb(110,230,165));
        header.setPadding(dp(4),dp(4),dp(4),dp(10));
        box.addView(header);
        header.setOnTouchListener(dragPanel());

        timerLabel=label("Timer ready",13,false);
        box.addView(timerLabel);

        TextView timerTitle=label("TIMER",12,true); timerTitle.setPadding(0,dp(8),0,0); box.addView(timerTitle);
        LinearLayout timerRow=new LinearLayout(this); timerRow.setOrientation(LinearLayout.HORIZONTAL);
        addSmallButton(timerRow,"5s",5); addSmallButton(timerRow,"10s",10); addSmallButton(timerRow,"15s",15); addSmallButton(timerRow,"30s",30);
        box.addView(timerRow);

        TextView noteTitle=label("QUICK NOTES",12,true); noteTitle.setPadding(0,dp(10),0,dp(4)); box.addView(noteTitle);
        notesInput=new EditText(this);
        notesInput.setText(prefs.getString("notes",""));
        notesInput.setTextColor(Color.WHITE); notesInput.setHintTextColor(Color.LTGRAY);
        notesInput.setHint("Write a note...");
        notesInput.setTextSize(14); notesInput.setMinLines(2); notesInput.setMaxLines(4);
        notesInput.setPadding(dp(8),dp(6),dp(8),dp(6));
        notesInput.setBackground(shape(Color.rgb(40,49,63),dp(8),Color.TRANSPARENT));
        box.addView(notesInput,new LinearLayout.LayoutParams(-1,dp(86)));
        addAction(box,"Save note","SAVE");
        addAction(box,"Open an app shortcut","APPS");
        addAction(box,"Open Android Settings","SETTINGS");
        addAction(box,"Hide panel","HIDE");
        addAction(box,"Exit floating utility","EXIT");

        panelLp=new WindowManager.LayoutParams(dp(280),WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT);
        panelLp.gravity=Gravity.TOP|Gravity.START;
        panelLp.x=Math.max(dp(6),bubbleLp.x-dp(8)); panelLp.y=bubbleLp.y+dp(64);
        panel=box;
        try { wm.addView(panel,panelLp); }
        catch(Exception e) { panel=null; Toast.makeText(this,"Could not show panel: "+e.getMessage(),Toast.LENGTH_LONG).show(); }
    }

    private View.OnTouchListener dragPanel() {
        return new View.OnTouchListener() {
            int sx,sy; float dx,dy;
            public boolean onTouch(View v,MotionEvent e) {
                if(e.getAction()==MotionEvent.ACTION_DOWN){sx=panelLp.x;sy=panelLp.y;dx=e.getRawX();dy=e.getRawY();return true;}
                if(e.getAction()==MotionEvent.ACTION_MOVE){
                    panelLp.x=Math.max(0,sx+(int)(e.getRawX()-dx));
                    panelLp.y=Math.max(0,sy+(int)(e.getRawY()-dy));
                    try{wm.updateViewLayout(panel,panelLp);}catch(Exception ignored){}
                    return true;
                }
                return e.getAction()==MotionEvent.ACTION_UP;
            }
        };
    }

    private void addSmallButton(LinearLayout row,String text,int seconds) {
        TextView b=label(text,13,true); b.setGravity(Gravity.CENTER);
        b.setBackground(shape(Color.rgb(48,65,82),dp(7),Color.TRANSPARENT));
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,dp(40),1); lp.setMargins(dp(2),dp(4),dp(2),dp(4));
        row.addView(b,lp); b.setOnClickListener(v->startTimer(seconds));
    }

    private void startTimer(int seconds) {
        if(timer!=null) timer.cancel();
        timerLabel.setText("Timer: "+seconds+" seconds");
        timer=new CountDownTimer(seconds*1000L,250) {
            public void onTick(long ms){if(timerLabel!=null)timerLabel.setText("Time left: "+((ms+999)/1000)+"s");}
            public void onFinish(){if(timerLabel!=null)timerLabel.setText("Timer complete!"); Toast.makeText(FloatingService.this,"Timer complete",Toast.LENGTH_SHORT).show();}
        }.start();
    }

    private void addAction(LinearLayout box,String text,String action) {
        TextView b=label(text,14,true); b.setGravity(Gravity.CENTER_VERTICAL);
        b.setPadding(dp(10),dp(8),dp(8),dp(8));
        b.setBackground(shape(Color.rgb(46,58,74),dp(8),Color.TRANSPARENT));
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(42)); lp.topMargin=dp(5); box.addView(b,lp);
        b.setOnClickListener(v->{
            if("SAVE".equals(action)){
                prefs.edit().putString("notes",notesInput.getText().toString()).apply();
                Toast.makeText(this,"Note saved",Toast.LENGTH_SHORT).show();
                ((InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(notesInput.getWindowToken(),0);
            } else if("APPS".equals(action)) showAppPicker();
            else if("SETTINGS".equals(action)){Intent i=new Intent(Settings.ACTION_SETTINGS);i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);startActivity(i);}
            else if("HIDE".equals(action)) hidePanel();
            else if("EXIT".equals(action)) stopSelf();
        });
    }

    private void showAppPicker() {
        final ArrayList<ResolveInfoHolder> apps=new ArrayList<>();
        PackageManager pm=getPackageManager();
        Intent query=new Intent(Intent.ACTION_MAIN,null); query.addCategory(Intent.CATEGORY_LAUNCHER);
        for(android.content.pm.ResolveInfo ri:pm.queryIntentActivities(query,0)){
            if(ri.activityInfo.packageName.equals(getPackageName())) continue;
            apps.add(new ResolveInfoHolder(ri.loadLabel(pm).toString(),ri.activityInfo.packageName,ri.activityInfo.name));
        }
        Collections.sort(apps,(a,b)->a.label.compareToIgnoreCase(b.label));
        if(apps.isEmpty()){Toast.makeText(this,"No launchable apps found",Toast.LENGTH_SHORT).show();return;}
        String[] names=new String[apps.size()]; for(int i=0;i<apps.size();i++)names[i]=apps.get(i).label;
        new AlertDialog.Builder(this).setTitle("Open app").setItems(names,(d,which)->{
            ResolveInfoHolder a=apps.get(which);
            Intent launch=new Intent(Intent.ACTION_MAIN);launch.addCategory(Intent.CATEGORY_LAUNCHER);
            launch.setClassName(a.pkg,a.cls);launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            try{startActivity(launch);}catch(Exception e){Toast.makeText(this,"Could not open app",Toast.LENGTH_SHORT).show();}
        }).setNegativeButton("Cancel",null).show();
    }

    private static class ResolveInfoHolder {
        String label,pkg,cls; ResolveInfoHolder(String l,String p,String c){label=l;pkg=p;cls=c;}
    }

    private void hidePanel() {
        if(panel!=null){try{wm.removeView(panel);}catch(Exception ignored){} panel=null;timerLabel=null;notesInput=null;}
    }

    private TextView label(String text,int size,boolean bold) {
        TextView t=new TextView(this);t.setText(text);t.setTextSize(size);t.setTextColor(Color.WHITE);
        if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;
    }
    private GradientDrawable shape(int color,int radius,int stroke) {
        GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(radius);
        if(stroke!=Color.TRANSPARENT)d.setStroke(dp(1),stroke);return d;
    }
    private int dp(float v){return (int)(v*density+0.5f);}

    @Override public void onDestroy() {
        if(timer!=null)timer.cancel();hidePanel();
        if(bubble!=null){try{wm.removeView(bubble);}catch(Exception ignored){}bubble=null;}
        super.onDestroy();
    }
    @Override public IBinder onBind(Intent i){return null;}
}
