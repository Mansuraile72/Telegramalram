package org.telegram.alarm.utils;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.widget.Toast;

import org.telegram.alarm.receiver.AlarmReceiver;

import java.util.Calendar;

public class AlarmHelper {
    
    private static final String PREFS_NAME = "AlarmPrefs";
    private static final String KEY_ALARM_COUNT = "alarm_count";
    
    public static void setAlarm(Context context, int hour, int minute, String label, int alarmId) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        
        Intent intent = new Intent(context, AlarmReceiver.class);
        intent.putExtra("alarm_label", label);
        intent.putExtra("alarm_time", String.format("%02d:%02d", hour, minute));
        
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
            context, 
            alarmId, 
            intent, 
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);
        
        // If alarm time has passed today, set for tomorrow
        if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }
        
        // Set the alarm
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendar.getTimeInMillis(),
                pendingIntent
            );
        } else {
            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                calendar.getTimeInMillis(),
                pendingIntent
            );
        }
        
        // Save alarm to SharedPreferences
        saveAlarm(context, alarmId, hour, minute, label);
        
        Toast.makeText(context, 
            "Alarm set for " + String.format("%02d:%02d", hour, minute), 
            Toast.LENGTH_SHORT).show();
    }
    
    public static void cancelAlarm(Context context, int alarmId) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, AlarmReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
            context, 
            alarmId, 
            intent, 
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        alarmManager.cancel(pendingIntent);
        
        // Remove from SharedPreferences
        removeAlarm(context, alarmId);
    }
    
    private static void saveAlarm(Context context, int alarmId, int hour, int minute, String label) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        
        editor.putInt("alarm_" + alarmId + "_hour", hour);
        editor.putInt("alarm_" + alarmId + "_minute", minute);
        editor.putString("alarm_" + alarmId + "_label", label);
        editor.putBoolean("alarm_" + alarmId + "_enabled", true);
        
        // Update alarm count
        int count = prefs.getInt(KEY_ALARM_COUNT, 0);
        if (!prefs.contains("alarm_" + alarmId + "_hour")) {
            editor.putInt(KEY_ALARM_COUNT, count + 1);
        }
        
        editor.apply();
    }
    
    private static void removeAlarm(Context context, int alarmId) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        
        editor.remove("alarm_" + alarmId + "_hour");
        editor.remove("alarm_" + alarmId + "_minute");
        editor.remove("alarm_" + alarmId + "_label");
        editor.remove("alarm_" + alarmId + "_enabled");
        
        editor.apply();
    }
    
    public static void loadAlarms(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        int count = prefs.getInt(KEY_ALARM_COUNT, 0);
        
        for (int i = 0; i < count; i++) {
            if (prefs.contains("alarm_" + i + "_hour")) {
                int hour = prefs.getInt("alarm_" + i + "_hour", 0);
                int minute = prefs.getInt("alarm_" + i + "_minute", 0);
                String label = prefs.getString("alarm_" + i + "_label", "");
                boolean enabled = prefs.getBoolean("alarm_" + i + "_enabled", true);
                
                if (enabled) {
                    // Re-set the alarm if enabled
                    setAlarm(context, hour, minute, label, i);
                }
            }
        }
    }
}
