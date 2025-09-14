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
                alarmListView.setAdapter(alarmAdapter);
                Log.d(TAG, "setupUI: Adapter set on ListView.");

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
        // This method remains largely the same, but simplified for clarity
        LinearLayout dialogLayout = new LinearLayout(this);
        dialogLayout.setOrientation(LinearLayout.VERTICAL);
        dialogLayout.setPadding(40, 40, 40, 40);

        NumberPicker hourPicker = new NumberPicker(this);
        hourPicker.setMinValue(0);
        hourPicker.setMaxValue(23);
        
        NumberPicker minutePicker = new NumberPicker(this);
        minutePicker.setMinValue(0);
        minutePicker.setMaxValue(59);

        LinearLayout timeLayout = new LinearLayout(this);
        timeLayout.setOrientation(LinearLayout.HORIZONTAL);
        timeLayout.setGravity(Gravity.CENTER);
        timeLayout.addView(hourPicker);
        timeLayout.addView(minutePicker);
        
        dialogLayout.addView(timeLayout);

        EditText labelInput = new EditText(this);
        labelInput.setHint("Alarm Label");
        dialogLayout.addView(labelInput);

        android.app.Dialog dialog = new android.app.Dialog(this);
        dialog.setContentView(dialogLayout);

        Button setButton = new Button(this);
        setButton.setText("Set Alarm");
        setButton.setOnClickListener(v -> {
            int hour = hourPicker.getValue();
            int minute = minutePicker.getValue();
            String label = labelInput.getText().toString();

            AlarmItem alarm = new AlarmItem();
            alarm.id = (int) System.currentTimeMillis();
            alarm.hour = hour;
            alarm.minute = minute;
            alarm.label = label.isEmpty() ? "Alarm" : label;
            alarm.enabled = true;

            alarmList.add(alarm);
            Log.d(TAG, "Alarm added. List size: " + alarmList.size());
            
            alarmAdapter.notifyDataSetChanged();
            Log.d(TAG, "Adapter notified.");

            updateEmptyStateVisibility();
            setAlarm(alarm);

            Toast.makeText(this, "Alarm set for " + hour + ":" + minute, Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });
        
        dialogLayout.addView(setButton);
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

        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
        Log.d(TAG, "Scheduled alarm for: " + calendar.getTime());
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
        public View getView(int position, View convertView, ViewGroup parent) {
            LinearLayout itemLayout;
            if (convertView == null) {
                itemLayout = new LinearLayout(AlarmMainActivity.this);
                itemLayout.setOrientation(LinearLayout.VERTICAL);
                itemLayout.setPadding(20, 15, 20, 15);

                TextView timeText = new TextView(AlarmMainActivity.this);
                timeText.setId(android.R.id.text1);
                timeText.setTextSize(24);
                timeText.setTextColor(Color.WHITE);

                TextView labelText = new TextView(AlarmMainActivity.this);
                labelText.setId(android.R.id.text2);
                labelText.setTextSize(14);
                labelText.setTextColor(Color.YELLOW);

                itemLayout.addView(timeText);
                itemLayout.addView(labelText);
            } else {
                itemLayout = (LinearLayout) convertView;
            }

            TextView timeText = itemLayout.findViewById(android.R.id.text1);
            TextView labelText = itemLayout.findViewById(android.R.id.text2);

            AlarmItem alarm = alarmList.get(position);
            timeText.setText(String.format("%02d:%02d", alarm.hour, alarm.minute));
            labelText.setText(alarm.label);

            return itemLayout;
        }
    }
}