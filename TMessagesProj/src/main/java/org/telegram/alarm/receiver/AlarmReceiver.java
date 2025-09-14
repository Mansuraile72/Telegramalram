package org.telegram.alarm.receiver;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import android.os.Vibrator;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import org.telegram.alarm.ui.AlarmMainActivity;

public class AlarmReceiver extends BroadcastReceiver {
    
    private static final String TAG = "AlarmReceiver";
    private static final String CHANNEL_ID = "alarm_channel";
    private static final int NOTIFICATION_ID = 1001;
    private static final String ACTION_DISMISS_ALARM = "org.telegram.alarm.DISMISS_ALARM";
    private static final String ACTION_SNOOZE_ALARM = "org.telegram.alarm.SNOOZE_ALARM";
    
    private static Ringtone currentRingtone;
    private static Vibrator currentVibrator;
    
        @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        
        // Handle dismiss action
        if (ACTION_DISMISS_ALARM.equals(action)) {
            Log.d(TAG, "🛑 ALARM DISMISSED!");
            dismissAlarm(context);
            return;
        }
        
        // Handle snooze action  
        if (ACTION_SNOOZE_ALARM.equals(action)) {
            Log.d(TAG, "😴 ALARM SNOOZED!");
            dismissAlarm(context);
            return;
        }
        
        Log.d(TAG, "🚨 ALARM RECEIVED! Intent: " + intent);
        
        String label = intent.getStringExtra("alarm_label");
        String time = intent.getStringExtra("alarm_time");
        
        Log.d(TAG, "⏰ Alarm details - Label: " + label + ", Time: " + time);
        
        // Acquire wake lock to ensure device wakes up
        PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wakeLock = powerManager.newWakeLock(
            PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, 
            "AlarmApp:AlarmWakelock"
        );
        wakeLock.acquire(5 * 60 * 1000L); // 5 minutes max
        
        try {
            // FORCE LAUNCH FULL-SCREEN ALARM ACTIVITY FIRST
            launchFullScreenAlarmActivity(context, label, time);
            
            // Then start vibration and sound
            startVibration(context);
            playAlarmSound(context);
            
            // Show backup notification (but full-screen should be primary)
            showAlarmNotification(context, label, time);
            
            Log.d(TAG, "✅ All alarm actions completed successfully");
            
        } catch (Exception e) {
            Log.e(TAG, "❌ Error in alarm processing", e);
        } finally {
            if (wakeLock.isHeld()) {
                wakeLock.release();
            }
        }
    }
    
    private void startVibration(Context context) {
        try {
            currentVibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
            if (currentVibrator != null && currentVibrator.hasVibrator()) {
                long[] pattern = {0, 1000, 500, 1000, 500, 1000, 500, 1000};
                currentVibrator.vibrate(pattern, 0); // Repeat until cancelled
                Log.d(TAG, "📳 Vibration started");
            }
        } catch (Exception e) {
            Log.e(TAG, "❌ Vibration error", e);
        }
    }
    
    private void playAlarmSound(Context context) {
        try {
            Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            if (alarmSound == null) {
                alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
            }
            if (alarmSound == null) {
                alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            }
            
            currentRingtone = RingtoneManager.getRingtone(context, alarmSound);
            if (currentRingtone != null) {
                AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
                int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM);
                audioManager.setStreamVolume(AudioManager.STREAM_ALARM, maxVolume, 0);
                
                currentRingtone.play();
                Log.d(TAG, "🔊 Alarm sound started at max volume");
            }
        } catch (Exception e) {
            Log.e(TAG, "❌ Sound play error", e);
        }
    }
    
    private void showAlarmNotification(Context context, String label, String time) {
        try {
            NotificationManager notificationManager = 
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            
            // Create notification channel for Android O+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Alarm Notifications",
                    NotificationManager.IMPORTANCE_MAX
                );
                channel.setDescription("Alarm notifications");
                channel.enableVibration(true);
                channel.setVibrationPattern(new long[]{0, 1000, 500, 1000});
                channel.enableLights(true);
                channel.setLightColor(android.graphics.Color.RED);
                channel.setBypassDnd(true);
                channel.setLockscreenVisibility(NotificationCompat.VISIBILITY_PUBLIC);
                
                Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
                AudioAttributes attributes = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build();
                channel.setSound(alarmSound, attributes);
                
                notificationManager.createNotificationChannel(channel);
                Log.d(TAG, "📱 Notification channel created");
            }
            
            // Create dismiss intent
            Intent dismissIntent = new Intent(context, AlarmReceiver.class);
            dismissIntent.setAction(ACTION_DISMISS_ALARM);
            PendingIntent dismissPendingIntent = PendingIntent.getBroadcast(
                context, 
                NOTIFICATION_ID + 1, 
                dismissIntent, 
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );
            
            // Create snooze intent
            Intent snoozeIntent = new Intent(context, AlarmReceiver.class);
            snoozeIntent.setAction(ACTION_SNOOZE_ALARM);
            PendingIntent snoozePendingIntent = PendingIntent.getBroadcast(
                context, 
                NOTIFICATION_ID + 2, 
                snoozeIntent, 
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );
            
            // Create intent to open alarm activity
            Intent alarmIntent = new Intent(context, org.telegram.alarm.ui.AlarmRingingActivity.class);
            alarmIntent.putExtra("alarm_label", label);
            alarmIntent.putExtra("alarm_time", time);
            alarmIntent.putExtra("alarm_ringing", true);
            alarmIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 
                NOTIFICATION_ID, 
                alarmIntent, 
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );
            
            // Build notification with action buttons
            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle("🚨 ALARM RINGING!")
                .setContentText(label != null && !label.isEmpty() ? label : "Alarm at " + time)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setVibrate(new long[]{0, 1000, 500, 1000, 500, 1000})
                .setAutoCancel(false)
                .setOngoing(true)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setFullScreenIntent(pendingIntent, true)
                .setContentIntent(pendingIntent)
                .addAction(android.R.drawable.ic_delete, "DISMISS", dismissPendingIntent)
                .addAction(android.R.drawable.ic_media_pause, "SNOOZE", snoozePendingIntent);
                
            // Show notification
            notificationManager.notify(NOTIFICATION_ID, builder.build());
            Log.d(TAG, "🔔 Notification shown with dismiss buttons");
            
        } catch (Exception e) {
            Log.e(TAG, "❌ Notification error", e);
        }
    }
    
    private void launchFullScreenAlarmActivity(Context context, String label, String time) {
        try {
            Log.d(TAG, "🎯 Forcing full-screen alarm launch...");
            
            Intent intent = new Intent(context, org.telegram.alarm.ui.AlarmRingingActivity.class);
            intent.putExtra("alarm_label", label);
            intent.putExtra("alarm_time", time);
            intent.putExtra("alarm_ringing", true);
            
            // Add ALL possible flags to force activity to top
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
            intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
            intent.addFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            
            // Force start activity
            context.startActivity(intent);
            
            // Small delay and try again to make sure it appears
            android.os.Handler handler = new android.os.Handler();
            Runnable retryRunnable = new Runnable() {
                public void run() {
                    try {
                        context.startActivity(intent);
                        Log.d(TAG, "🎯 Second attempt to launch full-screen alarm");
                    } catch (Exception e) {
                        Log.e(TAG, "Second launch attempt failed", e);
                    }
                }
            };
            handler.postDelayed(retryRunnable, 500);
            
            Log.d(TAG, "🎯 Full-screen alarm activity launched with all flags");
            
        } catch (Exception e) {
            Log.e(TAG, "❌ Critical error launching alarm activity", e);
            
            // Emergency fallback - try to launch main activity
            try {
                Intent fallbackIntent = new Intent(context, org.telegram.alarm.ui.AlarmMainActivity.class);
                fallbackIntent.putExtra("alarm_ringing", true);
                fallbackIntent.putExtra("alarm_label", label);
                fallbackIntent.putExtra("alarm_time", time);
                fallbackIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                context.startActivity(fallbackIntent);
                Log.d(TAG, "📱 Fallback to main activity launched");
            } catch (Exception fallbackException) {
                Log.e(TAG, "❌❌ Even fallback failed!", fallbackException);
            }
        }
    }
    
    private void launchAlarmActivity(Context context, String label, String time) {
        try {
            Intent intent = new Intent(context, org.telegram.alarm.ui.AlarmRingingActivity.class);
            intent.putExtra("alarm_label", label);
            intent.putExtra("alarm_time", time);
            intent.putExtra("alarm_ringing", true);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | 
                           Intent.FLAG_ACTIVITY_CLEAR_TOP |
                           Intent.FLAG_ACTIVITY_NO_HISTORY);
                           
            context.startActivity(intent);
            Log.d(TAG, "🎯 Full-screen alarm activity launched");
        } catch (Exception e) {
            Log.e(TAG, "❌ Activity launch error", e);
        }
    }
    
    public static void dismissAlarm(Context context) {
        try {
            // Stop ringtone
            if (currentRingtone != null && currentRingtone.isPlaying()) {
                currentRingtone.stop();
                currentRingtone = null;
                Log.d(TAG, "🔇 Ringtone stopped");
            }
            
            // Stop vibration
            if (currentVibrator != null) {
                currentVibrator.cancel();
                currentVibrator = null;
                Log.d(TAG, "📵 Vibration stopped");
            }
            
            // Cancel notification
            NotificationManager notificationManager = 
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.cancel(NOTIFICATION_ID);
            Log.d(TAG, "🔕 Notification dismissed");
            
        } catch (Exception e) {
            Log.e(TAG, "❌ Error dismissing alarm", e);
        }
    }
}