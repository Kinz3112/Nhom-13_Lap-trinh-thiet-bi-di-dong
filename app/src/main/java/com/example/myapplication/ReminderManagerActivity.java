package com.example.myapplication;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.List;

public class ReminderManagerActivity extends AppCompatActivity {

    private RecyclerView rvReminders;
    private AppDatabase db;
    private ReminderAdapter adapter;
    private int currentUserId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reminder_manager);

        db = AppDatabase.getInstance(this);
        rvReminders = findViewById(R.id.rvReminders);
        rvReminders.setLayoutManager(new LinearLayoutManager(this));

        SharedPreferences pref = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        currentUserId = pref.getInt("userId", -1);

        FloatingActionButton fabAdd = findViewById(R.id.fab_add_reminder);
        fabAdd.setOnClickListener(v -> startActivity(new Intent(this, AddReminderActivity.class)));

        setupBottomNavigation();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadReminders();
    }

    private void loadReminders() {
        new Thread(() -> {
            List<Reminder> list = db.appDao().getRemindersForUser(currentUserId);
            runOnUiThread(() -> {
                adapter = new ReminderAdapter(list, new ReminderAdapter.OnReminderClickListener() {
                    @Override
                    public void onToggle(Reminder reminder, boolean isChecked) {
                        handleToggleReminder(reminder, isChecked);
                    }

                    @Override
                    public void onDelete(Reminder reminder) {
                        showDeleteDialog(reminder);
                    }
                });
                rvReminders.setAdapter(adapter);
            });
        }).start();
    }

    private void handleToggleReminder(Reminder reminder, boolean isChecked) {
        reminder.isActive = isChecked;
        new Thread(() -> {
            db.appDao().updateReminder(reminder);
            runOnUiThread(() -> {
                if (!isChecked) {
                    cancelAlarm(reminder);
                    Toast.makeText(this, "Đã tắt nhắc nhở", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Đã bật nhắc nhở", Toast.LENGTH_SHORT).show();
                }
            });
        }).start();
    }

    private void showDeleteDialog(Reminder reminder) {
        new AlertDialog.Builder(this)
                .setTitle("Xóa nhắc nhở")
                .setMessage("Bạn có chắc muốn xóa lời nhắc '" + reminder.title + "' không?")
                .setPositiveButton("Xóa", (dialog, which) -> {
                    new Thread(() -> {
                        db.appDao().deleteReminder(reminder);
                        runOnUiThread(() -> {
                            cancelAlarm(reminder);
                            loadReminders();
                        });
                    }).start();
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    // HÀM QUAN TRỌNG: Gỡ bỏ báo thức khỏi hệ thống Android
    private void cancelAlarm(Reminder reminder) {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, ReminderReceiver.class);
        int requestCode = reminder.title.hashCode();

        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        alarmManager.cancel(pendingIntent);
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setSelectedItemId(R.id.nav_reminders);

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_transactions) {
                startActivity(new Intent(this, MainActivity.class)); finish(); return true;
            } else if (id == R.id.nav_categories) {
                startActivity(new Intent(this, CategoryManagerActivity.class)); finish(); return true;
            } else if (id == R.id.nav_reminders) {
                return true; // Đang ở trang này
            } else if (id == R.id.nav_report) {
                startActivity(new Intent(this, ReportActivity.class)); finish(); return true;
            } else if (id == R.id.nav_settings) {
                showSettingsAndSupportDialog();
                return false;
            }
            return false;
        });
    }
    private void showSettingsAndSupportDialog() {
        String[] options = {"⚙ Cài đặt (Đang phát triển)", "📞 Liên hệ hỗ trợ", "🚪 Đăng xuất"};

        new android.app.AlertDialog.Builder(this)
                .setTitle("Tùy chọn hệ thống")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        Toast.makeText(this, "Tính năng Cài đặt đang được phát triển...", Toast.LENGTH_SHORT).show();
                    } else if (which == 1) {
                        try {
                            Intent intent = new Intent(Intent.ACTION_DIAL);
                            intent.setData(android.net.Uri.parse("tel:19001234"));
                            startActivity(intent);
                        } catch (Exception e) {
                            Toast.makeText(this, "Không thể mở trình gọi điện", Toast.LENGTH_SHORT).show();
                        }
                    } else if (which == 2) {
                        performLogout();
                    }
                })
                .show();
    }

    private void performLogout() {
        SharedPreferences pref = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        editor.clear();
        editor.apply();

        Toast.makeText(this, "Đã đăng xuất thành công", Toast.LENGTH_SHORT).show();

        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}