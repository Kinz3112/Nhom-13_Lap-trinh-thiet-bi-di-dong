package com.example.myapplication;


import android.app.AlarmManager;
import android.app.DatePickerDialog;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Calendar;

public class AddReminderActivity extends AppCompatActivity {

    private EditText etTitle, etNote;
    private Spinner spFrequency;
    private Button btnSelectDate, btnSelectTime, btnSave;

    private Calendar selectedCalendar; //ngày giờ
    private AppDatabase db;
    private int currentUserId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_reminder);

        db = AppDatabase.getInstance(this);
        selectedCalendar = Calendar.getInstance(); // giờ hiện tại

        SharedPreferences pref = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        currentUserId = pref.getInt("userId", -1);

        etTitle = findViewById(R.id.etReminderTitle);
        etNote = findViewById(R.id.etReminderNote);
        spFrequency = findViewById(R.id.spFrequency);
        btnSelectDate = findViewById(R.id.btnSelectDate);
        btnSelectTime = findViewById(R.id.btnSelectTime);
        btnSave = findViewById(R.id.btnSaveReminder);

        setupSpinner();

        btnSelectDate.setOnClickListener(v -> showDatePicker());
        btnSelectTime.setOnClickListener(v -> showTimePicker());
        btnSave.setOnClickListener(v -> saveReminder());
    }

    private void setupSpinner() {
        String[] frequencies = {"Một lần", "Hàng ngày", "Hàng tuần", "Hàng tháng"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, frequencies);
        spFrequency.setAdapter(adapter);
    }

    private void showDatePicker() {
        new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            selectedCalendar.set(Calendar.YEAR, year);
            selectedCalendar.set(Calendar.MONTH, month);
            selectedCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            btnSelectDate.setText(dayOfMonth + "/" + (month + 1) + "/" + year);
        },
                selectedCalendar.get(Calendar.YEAR),
                selectedCalendar.get(Calendar.MONTH),
                selectedCalendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void showTimePicker() {
        new TimePickerDialog(this, (view, hourOfDay, minute) -> {
            selectedCalendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
            selectedCalendar.set(Calendar.MINUTE, minute);
            selectedCalendar.set(Calendar.SECOND, 0);
            btnSelectTime.setText(String.format("%02d:%02d", hourOfDay, minute));
        },
                selectedCalendar.get(Calendar.HOUR_OF_DAY),
                selectedCalendar.get(Calendar.MINUTE), true).show();
    }

    private void saveReminder() {
        String title = etTitle.getText().toString().trim();
        if (title.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập tên lời nhắc", Toast.LENGTH_SHORT).show();
            return;
        }

        Reminder reminder = new Reminder();
        reminder.userId = currentUserId;
        reminder.title = title;
        reminder.frequency = spFrequency.getSelectedItem().toString();
        reminder.note = etNote.getText().toString().trim();
        reminder.startDate = selectedCalendar.getTimeInMillis();
        reminder.hour = selectedCalendar.get(Calendar.HOUR_OF_DAY);
        reminder.minute = selectedCalendar.get(Calendar.MINUTE);
        reminder.isActive = true;

        new Thread(() -> {
            db.appDao().insertReminder(reminder);

            runOnUiThread(() -> {
                scheduleAlarm(reminder);
                Toast.makeText(this, "Đã lưu và bật nhắc nhở!", Toast.LENGTH_SHORT).show();
                finish();
            });
        }).start();
    }
    private void scheduleAlarm(Reminder reminder) {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

        // 1. Kiểm tra quyền báo thức chính xác (Cho Android 12+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager != null && !alarmManager.canScheduleExactAlarms()) {
                Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                startActivity(intent);
                Toast.makeText(this, "Bạn cần cấp quyền 'Báo thức & nhắc nhở' để nhận thông báo", Toast.LENGTH_LONG).show();
                return;
            }
        }

        Intent intent = new Intent(this, ReminderReceiver.class);
        intent.putExtra("title", reminder.title);
        intent.putExtra("note", reminder.note);

        // Sử dụng currentTimeMillis làm RequestCode để đảm bảo tính duy nhất
        int requestCode = (int) System.currentTimeMillis();

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        long triggerTime = selectedCalendar.getTimeInMillis();

        // 2. Đảm bảo không đặt giờ trong quá khứ
        if (triggerTime <= System.currentTimeMillis()) {
            // Nếu người dùng chọn giờ đã qua trong ngày hôm nay, tự động dời sang ngày mai
            selectedCalendar.add(Calendar.DAY_OF_MONTH, 1);
            triggerTime = selectedCalendar.getTimeInMillis();
        }

        if (alarmManager != null) {
            try {
                if (reminder.frequency.equals("Một lần")) {
                    // Dùng setExactAndAllowWhileIdle để xuyên thủng chế độ tiết kiệm pin (Doze mode)
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
                } else {
                    long interval = AlarmManager.INTERVAL_DAY;
                    if (reminder.frequency.equals("Hàng tuần")) interval = AlarmManager.INTERVAL_DAY * 7;
                    else if (reminder.frequency.equals("Hàng tháng")) interval = AlarmManager.INTERVAL_DAY * 30;

                    // Đối với báo thức lặp lại, Android hiện đại khuyên dùng setInexactRepeating để tiết kiệm pin
                    // hoặc lập lịch lại mỗi khi báo thức cũ nổ
                    alarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP, triggerTime, interval, pendingIntent);
                }
            } catch (SecurityException e) {
                e.printStackTrace();
            }
        }
    }
}