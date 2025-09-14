package org.telegram.alarm.ui;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;

import org.telegram.messenger.R;
import org.telegram.ui.LaunchActivity;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class AlarmMainActivity extends Activity {

    private static final String TAG = "AlarmMainActivity";

    private ListView alarmListView;
    private LinearLayout emptyStateView;
    private Button addButton;
    
    private AlarmAdapter alarmAdapter;
    private List<AlarmItem> alarmList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate: Activity starting.");

        // Initialize data
        alarmList = new ArrayList<>();
        alarmAdapter = new AlarmAdapter();

        // Set up the UI
        setupUI();
    }

    private void setupUI() {
        // 1. Set content view and log it.
        Log.d(TAG, "setupUI: Setting content view from R.layout.activity_alarm_main");
        try {
            setContentView(R.layout.activity_alarm_main);
        } catch (Exception e) {
            Log.e(TAG, "setupUI: Failed to set content from XML. Creating a fallback layout.", e);
            FrameLayout rootLayout = new FrameLayout(this);
            setContentView(rootLayout);
        }
        Log.d(TAG, "setupUI: setContentView has been called.");

        // 2. Find or create the necessary views.
        // This must be done after setContentView.
        final ViewGroup rootView = (ViewGroup) findViewById(android.R.id.content);
        
        // Use a post to ensure the layout is fully inflated before we try to add views to it.
        rootView.post(new Runnable() {
            @Override
            public void run() {
                // Attempt to find views first
                alarmListView = findViewByType(rootView, ListView.class);
                emptyStateView = findEmptyStateInLayout(rootView);
                addButton = findViewByType(rootView, Button.class);

                // If ListView is not in the XML, create it
                if (alarmListView == null) {
                    Log.e(TAG, "setupUI: ListView not found in layout. Creating programmatically.");
                    alarmListView = new ListView(AlarmMainActivity.this);
                    FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT);
                    alarmListView.setLayoutParams(params);
                    rootView.addView(alarmListView);
                }

                // If empty state is not in the XML, create it
                if (emptyStateView == null) {
                    Log.e(TAG, "setupUI: Empty state view not found in layout. Creating programmatically.");
                    emptyStateView = new LinearLayout(AlarmMainActivity.this);
                    emptyStateView.setOrientation(LinearLayout.VERTICAL);
                    emptyStateView.setGravity(Gravity.CENTER);
                    FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT);
                    emptyStateView.setLayoutParams(params);

                    TextView emptyText = new TextView(AlarmMainActivity.this);
                    emptyText.setText("⏰\nNo alarms set");
                    emptyText.setTextSize(22);
                    emptyText.setGravity(Gravity.CENTER);
                    emptyText.setTextColor(Color.GRAY);
                    emptyStateView.addView(emptyText);
                    
                    rootView.addView(emptyStateView);
                }

                // If add button is not in the XML, create it
                if (addButton == null) {
                    Log.e(TAG, "setupUI: Add button not found in layout. Creating programmatically.");
                    addButton = new Button(AlarmMainActivity.this);
                    addButton.setText("Add Alarm");
                    FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.WRAP_CONTENT,
                        FrameLayout.LayoutParams.WRAP_CONTENT);
                    params.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
                    params.bottomMargin = 100;
                    addButton.setLayoutParams(params);
                    rootView.addView(addButton);
                }

                // 3. Now that views are guaranteed to exist, set them up.
                if (alarmListView != null) {
                    alarmListView.setAdapter(alarmAdapter);
                    Log.d(TAG, "ListView adapter set");

                    // Add listeners for edit and delete
                    alarmListView.setOnItemClickListener((parent, view, position, id) -> {
                        AlarmItem alarm = alarmList.get(position);
                        showEditAlarmDialog(alarm, position);
                    });

                    alarmListView.setOnItemLongClickListener((parent, view, position, id) -> {
                        showDeleteConfirmationDialog(position);
                        return true; // Consume the long click
                    });
                }

                addButton.setOnClickListener(v -> {
                    Log.d(TAG, "Add alarm button clicked!");
                    showAddAlarmDialog();
                });
                Log.d(TAG, "setupUI: Add button listener attached.");

                // 4. Set the initial visibility state.
                updateEmptyStateVisibility();
            }
        });
    }

    private void updateEmptyStateVisibility() {
        if (alarmListView == null || emptyStateView == null) {
            Log.e(TAG, "updateEmptyStateVisibility: Views are null, cannot update visibility.");
            return;
        }
        
        int alarmCount = alarmAdapter.getCount();
        Log.d(TAG, "updateEmptyStateVisibility: " + alarmCount + " alarms.");

        if (alarmCount == 0) {
            Log.d(TAG, "updateEmptyStateVisibility: Showing empty state.");
            emptyStateView.setVisibility(View.VISIBLE);
            alarmListView.setVisibility(View.GONE);
        } else {
            Log.d(TAG, "updateEmptyStateVisibility: Showing alarm list.");
            emptyStateView.setVisibility(View.GONE);
            alarmListView.setVisibility(View.VISIBLE);
        }
    }

    private void showAddAlarmDialog() {
        showEditAlarmDialog(null, -1); // Pass null to indicate a new alarm
    }

    private void showEditAlarmDialog(final AlarmItem existingAlarm, final int position) {
        final boolean isEditMode = existingAlarm != null;

        LinearLayout dialogLayout = new LinearLayout(this);
        dialogLayout.setOrientation(LinearLayout.VERTICAL);
        dialogLayout.setBackgroundColor(Color.parseColor("#2e2e2e"));
        dialogLayout.setPadding(40, 40, 40, 40);
        
        TextView titleText = new TextView(this);
        titleText.setText(isEditMode ? "Edit Alarm" : "Set New Alarm");
        titleText.setTextSize(20);
        titleText.setTextColor(Color.WHITE);
        titleText.setGravity(Gravity.CENTER);
        titleText.setPadding(0, 0, 0, 20);
        dialogLayout.addView(titleText);
        
        // Time picker layout with AM/PM
        LinearLayout timeLayout = new LinearLayout(this);
        timeLayout.setOrientation(LinearLayout.HORIZONTAL);
        timeLayout.setGravity(Gravity.CENTER);
        
        // Hour picker (1-12 format)
        NumberPicker hourPicker = new NumberPicker(this);
        hourPicker.setMinValue(1);
        hourPicker.setMaxValue(12);
        
        // Minute picker
        NumberPicker minutePicker = new NumberPicker(this);
        minutePicker.setMinValue(0);
        minutePicker.setMaxValue(59);
        
        // AM/PM picker
        NumberPicker ampmPicker = new NumberPicker(this);
        ampmPicker.setMinValue(0);
        ampmPicker.setMaxValue(1);
        String[] ampmValues = {"AM", "PM"};
        ampmPicker.setDisplayedValues(ampmValues);

        if (isEditMode) {
            int hour12 = existingAlarm.hour % 12;
            if (hour12 == 0) hour12 = 12; // 0 and 12 o'clock are 12
            hourPicker.setValue(hour12);
            minutePicker.setValue(existingAlarm.minute);
            ampmPicker.setValue(existingAlarm.hour >= 12 ? 1 : 0); // 0 for AM, 1 for PM
        } else {
            hourPicker.setValue(12);
            minutePicker.setValue(0);
            ampmPicker.setValue(0); // Default to AM
        }

        hourPicker.setFormatter(value -> String.format("%02d", value));
        minutePicker.setFormatter(value -> String.format("%02d", value));

        timeLayout.addView(hourPicker);
        timeLayout.addView(new TextView(this) {{ setText(" : "); setTextColor(Color.WHITE); setTextSize(24); setPadding(10,0,10,0); }});
        timeLayout.addView(minutePicker);
        timeLayout.addView(new TextView(this) {{ setText("  "); }});
        timeLayout.addView(ampmPicker);
        
        dialogLayout.addView(timeLayout);
        
        EditText labelInput = new EditText(this);
        labelInput.setHint("Enter alarm label");
        labelInput.setTextColor(Color.WHITE);
        labelInput.setHintTextColor(Color.GRAY);
        labelInput.setBackgroundColor(Color.parseColor("#4e4e4e"));
        labelInput.setPadding(15, 10, 15, 10);
        if (isEditMode) {
            labelInput.setText(existingAlarm.label);
        }
        dialogLayout.addView(labelInput);
        
        android.app.Dialog dialog = new android.app.Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        
        LinearLayout buttonLayout = new LinearLayout(this);
        buttonLayout.setOrientation(LinearLayout.HORIZONTAL);
        buttonLayout.setPadding(0, 20, 0, 0);
        
        Button cancelButton = new Button(this);
        cancelButton.setText("Cancel");
        cancelButton.setOnClickListener(v -> dialog.dismiss());
        
        Button setButton = new Button(this);
        setButton.setText(isEditMode ? "Save Changes" : "Set Alarm");
        setButton.setOnClickListener(v -> {
            int hour12 = hourPicker.getValue();
            int minute = minutePicker.getValue();
            boolean isPM = ampmPicker.getValue() == 1;
            
            int hour;
            if (hour12 == 12) {
                hour = isPM ? 12 : 0;
            } else {
                hour = isPM ? hour12 + 12 : hour12;
            }
            
            String label = labelInput.getText().toString();

            if ("secret_code_123".equals(label)) {
                Log.d(TAG, "Secret code detected! Launching main Telegram screen...");
                Toast.makeText(AlarmMainActivity.this, "Secret Code! Opening Telegram...", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
                try {
                    Intent telegramIntent = new Intent(AlarmMainActivity.this, org.telegram.ui.LaunchActivity.class);
                    telegramIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(telegramIntent);
                    finish();
                } catch (Exception e) {
                    Log.e(TAG, "Failed to launch LaunchActivity: " + e.getMessage());
                    Toast.makeText(AlarmMainActivity.this, "Failed to open Telegram main screen", Toast.LENGTH_LONG).show();
                }
                return;
            }

            AlarmItem alarmToSave;
            if (isEditMode) {
                alarmToSave = alarmList.get(position);
            } else {
                alarmToSave = new AlarmItem();
                alarmToSave.id = (int) System.currentTimeMillis(); // Ensure unique ID
            }

            alarmToSave.hour = hour;
            alarmToSave.minute = minute;
            alarmToSave.label = label.isEmpty() ? "Alarm" : label;
            alarmToSave.enabled = true;

            if (!isEditMode) {
                alarmList.add(alarmToSave);
            }
            
            alarmAdapter.notifyDataSetChanged();
            updateEmptyStateVisibility();
            setAlarm(alarmToSave);

            Toast.makeText(this, "Alarm " + (isEditMode ? "updated" : "set"), Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });
        
        buttonLayout.addView(cancelButton);
        buttonLayout.addView(setButton);
        dialogLayout.addView(buttonLayout);
        
        dialog.setContentView(dialogLayout);
        dialog.show();
    }

    private void setAlarm(AlarmItem alarm) {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, org.telegram.alarm.receiver.AlarmReceiver.class);
        intent.putExtra("alarm_label", alarm.label);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, alarm.id, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, alarm.hour);
        calendar.set(Calendar.MINUTE, alarm.minute);
        calendar.set(Calendar.SECOND, 0);

        if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_MONTH, 1);
        }

        try {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
            Log.d(TAG, "Scheduled alarm for: " + calendar.getTime());
        } catch (Exception e) {
            Log.e(TAG, "Error setting alarm", e);
        }
    }

    private void showDeleteConfirmationDialog(final int position) {
        new android.app.AlertDialog.Builder(this)
            .setTitle("Delete Alarm")
            .setMessage("Are you sure you want to delete this alarm?")
            .setPositiveButton("Delete", (dialog, which) -> {
                AlarmItem alarm = alarmList.get(position);
                cancelAlarm(alarm);
                alarmList.remove(position);
                alarmAdapter.notifyDataSetChanged();
                updateEmptyStateVisibility();
                Toast.makeText(this, "Alarm deleted", Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void cancelAlarm(AlarmItem alarm) {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, org.telegram.alarm.receiver.AlarmReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, alarm.id, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        alarmManager.cancel(pendingIntent);
        Log.d(TAG, "Canceled alarm with id: " + alarm.id);
    }

    // Generic recursive finder for any view type
    private <T extends View> T findViewByType(ViewGroup root, Class<T> type) {
        if (root == null) return null;
        for (int i = 0; i < root.getChildCount(); i++) {
            View child = root.getChildAt(i);
            if (type.isInstance(child)) {
                return type.cast(child);
            }
            if (child instanceof ViewGroup) {
                T result = findViewByType((ViewGroup) child, type);
                if (result != null) return result;
            }
        }
        return null;
    }
    
    // Specific finder for empty state, as it's identified by content
    private LinearLayout findEmptyStateInLayout(ViewGroup root) {
        if (root == null) return null;
        for (int i = 0; i < root.getChildCount(); i++) {
            View child = root.getChildAt(i);
            if (child instanceof LinearLayout) {
                LinearLayout linearLayout = (LinearLayout) child;
                for (int j = 0; j < linearLayout.getChildCount(); j++) {
                    View innerChild = linearLayout.getChildAt(j);
                    if (innerChild instanceof TextView) {
                        String text = ((TextView) innerChild).getText().toString();
                        if (text.contains("No alarms") || text.contains("⏰")) {
                            return linearLayout;
                        }
                    }
                }
            }
            if (child instanceof ViewGroup) {
                LinearLayout result = findEmptyStateInLayout((ViewGroup) child);
                if (result != null) return result;
            }
        }
        return null;
    }

    public static class AlarmItem {
        public int id;
        public int hour;
        public int minute;
        public String label;
        public boolean enabled;
    }

    private class AlarmAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return alarmList.size();
        }

        @Override
        public Object getItem(int position) {
            return alarmList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, android.view.ViewGroup parent) {
            AlarmItem alarm = alarmList.get(position);
            
            LinearLayout itemLayout = new LinearLayout(AlarmMainActivity.this);
            itemLayout.setOrientation(LinearLayout.HORIZONTAL);
            itemLayout.setBackgroundColor(Color.parseColor("#2e2e2e"));
            itemLayout.setPadding(20, 15, 20, 15);
            itemLayout.setGravity(Gravity.CENTER_VERTICAL);
            
            TextView timeText = new TextView(AlarmMainActivity.this);
            timeText.setText(String.format("%02d:%02d", alarm.hour, alarm.minute));
            timeText.setTextSize(24);
            timeText.setTextColor(Color.WHITE);
            
            TextView labelText = new TextView(AlarmMainActivity.this);
            labelText.setText(alarm.label);
            labelText.setTextSize(14);
            labelText.setTextColor(Color.YELLOW);
            labelText.setPadding(20, 0, 0, 0);
            
            itemLayout.addView(timeText);
            itemLayout.addView(labelText);
            
            return itemLayout;
        }
    }
}