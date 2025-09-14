package org.telegram.alarm.ui;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class AlarmRingingActivity extends Activity {
    
    private static final String TAG = "AlarmRingingActivity";
    private TextView clockText;
    private TextView alarmTimeText;
    private TextView alarmLabelText;
    private Button dismissButton;
    private Button snoozeButton;
    private LinearLayout pulseLayout;
    private Handler animationHandler;
    private boolean isAnimating = true;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Make activity full screen and show on lock screen
        getWindow().addFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN |
            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON |
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
            WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        );
        
        setupFullScreenUI();

        // Check for secret code in alarm label
        String alarmLabel = getIntent().getStringExtra("alarm_label");
        if (alarmLabel != null && alarmLabel.contains("secret_code_123")) {
            Log.d(TAG, "🔐 Secret alarm detected during ringing! Opening Telegram...");
            try {
                Intent telegramIntent = getPackageManager().getLaunchIntentForPackage("org.telegram.messenger");
                if (telegramIntent != null) {
                    startActivity(telegramIntent);
                    // Dismiss alarm
                    org.telegram.alarm.receiver.AlarmReceiver.dismissAlarm(this);
                    finish();
                    return;
                }
            } catch (Exception e) {
                Log.e(TAG, "Error launching Telegram from alarm", e);
            }
        }
        startAnimations();
        
        Log.d(TAG, "🎬 AlarmRingingActivity created with full screen");
    }
    
    private void setupFullScreenUI() {
        // Main container
        LinearLayout mainLayout = new LinearLayout(this);
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        mainLayout.setBackgroundColor(Color.parseColor("#FF1744")); // Red alarm color
        mainLayout.setGravity(Gravity.CENTER);
        mainLayout.setPadding(40, 60, 40, 60);
        
        // Current time display
        clockText = new TextView(this);
        updateCurrentTime();
        clockText.setTextSize(48);
        clockText.setTextColor(Color.WHITE);
        clockText.setGravity(Gravity.CENTER);
        clockText.setPadding(0, 20, 0, 30);
        mainLayout.addView(clockText);
        
        // Pulse animation layout
        pulseLayout = new LinearLayout(this);
        pulseLayout.setOrientation(LinearLayout.VERTICAL);
        pulseLayout.setGravity(Gravity.CENTER);
        pulseLayout.setBackgroundColor(Color.parseColor("#FF5722"));
        pulseLayout.setPadding(40, 40, 40, 40);
        
        // Alarm title
        TextView alarmTitle = new TextView(this);
        alarmTitle.setText("🚨 ALARM 🚨");
        alarmTitle.setTextSize(36);
        alarmTitle.setTextColor(Color.WHITE);
        alarmTitle.setGravity(Gravity.CENTER);
        alarmTitle.setPadding(0, 0, 0, 20);
        pulseLayout.addView(alarmTitle);
        
        // Alarm time
        String alarmTime = getIntent().getStringExtra("alarm_time");
        alarmTimeText = new TextView(this);
        alarmTimeText.setText("⏰ " + (alarmTime != null ? alarmTime : "00:00"));
        alarmTimeText.setTextSize(42);
        alarmTimeText.setTextColor(Color.YELLOW);
        alarmTimeText.setGravity(Gravity.CENTER);
        alarmTimeText.setPadding(0, 10, 0, 10);
        pulseLayout.addView(alarmTimeText);
        
        // Alarm label
        String alarmLabel = getIntent().getStringExtra("alarm_label");
        alarmLabelText = new TextView(this);
        alarmLabelText.setText(alarmLabel != null && !alarmLabel.isEmpty() ? alarmLabel : "Wake Up!");
        alarmLabelText.setTextSize(20);
        alarmLabelText.setTextColor(Color.WHITE);
        alarmLabelText.setGravity(Gravity.CENTER);
        alarmLabelText.setPadding(0, 0, 0, 30);
        pulseLayout.addView(alarmLabelText);
        
        mainLayout.addView(pulseLayout);
        
        // Button container
        LinearLayout buttonLayout = new LinearLayout(this);
        buttonLayout.setOrientation(LinearLayout.HORIZONTAL);
        buttonLayout.setGravity(Gravity.CENTER);
        buttonLayout.setPadding(0, 40, 0, 0);
        
        // Snooze button
        snoozeButton = new Button(this);
        snoozeButton.setText("😴\nSNOOZE\n5 MIN");
        snoozeButton.setTextSize(16);
        snoozeButton.setBackgroundColor(Color.parseColor("#FF9800")); // Orange
        snoozeButton.setTextColor(Color.WHITE);
        snoozeButton.setPadding(30, 20, 30, 20);
        snoozeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                snoozeAlarm();
            }
        });
        
        LinearLayout.LayoutParams snoozeParams = new LinearLayout.LayoutParams(
            0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f
        );
        snoozeParams.setMargins(10, 0, 10, 0);
        snoozeButton.setLayoutParams(snoozeParams);
        buttonLayout.addView(snoozeButton);
        
        // Dismiss button
        dismissButton = new Button(this);
        dismissButton.setText("✖️\nDISMISS\nALARM");
        dismissButton.setTextSize(16);
        dismissButton.setBackgroundColor(Color.parseColor("#4CAF50")); // Green
        dismissButton.setTextColor(Color.WHITE);
        dismissButton.setPadding(30, 20, 30, 20);
        dismissButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismissAlarm();
            }
        });
        
        LinearLayout.LayoutParams dismissParams = new LinearLayout.LayoutParams(
            0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f
        );
        dismissParams.setMargins(10, 0, 10, 0);
        dismissButton.setLayoutParams(dismissParams);
        buttonLayout.addView(dismissButton);
        
        mainLayout.addView(buttonLayout);
        
        setContentView(mainLayout);
    }
    
    private void startAnimations() {
        animationHandler = new Handler();
        
        // Pulse animation for the main alarm layout
        ScaleAnimation pulseAnimation = new ScaleAnimation(
            1.0f, 1.1f, 1.0f, 1.1f,
            Animation.RELATIVE_TO_SELF, 0.5f,
            Animation.RELATIVE_TO_SELF, 0.5f
        );
        pulseAnimation.setDuration(1000);
        pulseAnimation.setRepeatCount(Animation.INFINITE);
        pulseAnimation.setRepeatMode(Animation.REVERSE);
        pulseLayout.startAnimation(pulseAnimation);
        
        // Blinking animation for alarm time
        final Runnable blinkRunnable = new Runnable() {
            @Override
            public void run() {
                if (isAnimating && alarmTimeText != null) {
                    // Toggle text color between yellow and white
                    int currentColor = alarmTimeText.getCurrentTextColor();
                    if (currentColor == Color.YELLOW) {
                        alarmTimeText.setTextColor(Color.WHITE);
                    } else {
                        alarmTimeText.setTextColor(Color.YELLOW);
                    }
                    animationHandler.postDelayed(this, 500);
                }
            }
        };
        animationHandler.post(blinkRunnable);
        
        // Update current time every second
        final Runnable timeUpdateRunnable = new Runnable() {
            @Override
            public void run() {
                if (isAnimating && clockText != null) {
                    updateCurrentTime();
                    animationHandler.postDelayed(this, 1000);
                }
            }
        };
        animationHandler.post(timeUpdateRunnable);
        
        Log.d(TAG, "🎭 Animations started");
    }
    
    private void updateCurrentTime() {
        try {
            SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
            SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, MMM dd", Locale.getDefault());
            String currentTime = timeFormat.format(new Date());
            String currentDate = dateFormat.format(new Date());
            clockText.setText(currentTime + "\n" + currentDate);
        } catch (Exception e) {
            Log.e(TAG, "Error updating time", e);
        }
    }
    
    private void snoozeAlarm() {
        Log.d(TAG, "😴 Snooze button pressed");
        stopAnimations();
        
        // Send snooze broadcast
        Intent snoozeIntent = new Intent(this, org.telegram.alarm.receiver.AlarmReceiver.class);
        snoozeIntent.setAction("org.telegram.alarm.SNOOZE_ALARM");
        sendBroadcast(snoozeIntent);
        
        finish();
    }
    
    private void dismissAlarm() {
        Log.d(TAG, "✖️ Dismiss button pressed");
        stopAnimations();
        
        // Dismiss the alarm
        org.telegram.alarm.receiver.AlarmReceiver.dismissAlarm(this);
        
        finish();
    }
    
    private void stopAnimations() {
        isAnimating = false;
        if (animationHandler != null) {
            animationHandler.removeCallbacksAndMessages(null);
        }
        if (pulseLayout != null) {
            pulseLayout.clearAnimation();
        }
        Log.d(TAG, "🛑 Animations stopped");
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopAnimations();
        Log.d(TAG, "🏁 AlarmRingingActivity destroyed");
    }
    
    @Override
    public void onBackPressed() {
        // Disable back button during alarm
        Log.d(TAG, "⬅️ Back button disabled during alarm");
    }
}
