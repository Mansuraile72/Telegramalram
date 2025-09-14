package org.telegram.alarm.models;

public class AlarmItem {
    public int id;
    public int hour;
    public int minute;
    public String label;
    public boolean enabled;
    public boolean[] repeatDays; // 0=Sunday, 1=Monday, etc.
    public long timeInMillis;
    
    public AlarmItem() {
        this.enabled = true;
        this.repeatDays = new boolean[7]; // All false by default
        this.label = "";
    }
    
    public AlarmItem(int hour, int minute, String label) {
        this();
        this.hour = hour;
        this.minute = minute;
        this.label = label;
    }
    
    public String getTimeString() {
        return String.format("%02d:%02d", hour, minute);
    }
    
    public String getDisplayLabel() {
        return (label != null && !label.isEmpty()) ? label : "Alarm";
    }
    
    @Override
    public String toString() {
        return String.format("AlarmItem{id=%d, time=%02d:%02d, label='%s', enabled=%b}", 
                           id, hour, minute, label, enabled);
    }
}
