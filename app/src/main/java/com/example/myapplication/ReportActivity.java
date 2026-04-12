package com.example.myapplication;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReportActivity extends AppCompatActivity {

    private AppDatabase db;
    private PieChart pieChart;
    private Spinner spReportFilter;

    private String currentUsername = "";
    private int currentUserId = -1;
    private int currentFilterMode = 0; // 0: Hôm nay, 1: Tuần này, 2: Tháng này, 3: Tất cả

    public static final int[] MY_COLORS = {
            Color.rgb(46, 125, 50),   // Xanh lá đậm
            Color.rgb(25, 118, 210),  // Xanh dương
            Color.rgb(255, 143, 0),   // Cam
            Color.rgb(173, 20, 87),   // Hồng đậm
            Color.rgb(106, 27, 154),  // Tím
            Color.rgb(0, 131, 143),   // Xanh lơ
            Color.rgb(216, 67, 21),   // Đỏ cam
            Color.rgb(158, 157, 36),  // Vàng chanh
            Color.rgb(78, 52, 46),    // Nâu
            Color.rgb(69, 90, 100)    // Xám xanh
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report);

        db = AppDatabase.getInstance(this);
        pieChart = findViewById(R.id.pieChart);
        spReportFilter = findViewById(R.id.spReportFilter);

        SharedPreferences pref = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        currentUserId = pref.getInt("userId", -1);
        currentUsername = pref.getString("username", "");

        setupPieChart();
        updateBalanceDisplay();
        setupFilterSpinner(); // Thiết lập Spinner
        setupBottomNavigation();
    }

    private void setupFilterSpinner() {
        String[] options = {"Hôm nay", "Tuần này", "Tháng này", "Tất cả thời gian"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, options);
        spReportFilter.setAdapter(adapter);

        spReportFilter.setSelection(1);

        spReportFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                currentFilterMode = position;
                loadChartData();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }
    private void setupPieChart() {
        pieChart.setUsePercentValues(true);
        pieChart.getDescription().setEnabled(false);

        // DONUT CHART
        pieChart.setDrawHoleEnabled(true);        // Bật lỗ hổng ở giữa
        pieChart.setHoleColor(Color.WHITE);       // Màu của lỗ (trùng màu nền card)
        pieChart.setTransparentCircleRadius(61f); // Vòng tròn mờ bên ngoài lỗ (tạo chiều sâu)
        pieChart.setHoleRadius(58f);              // Độ rộng của lỗ (càng cao vòng nhẫn càng mỏng)

        // Cấu hình chữ hiển thị
        pieChart.setEntryLabelColor(Color.BLACK);
        pieChart.setEntryLabelTextSize(11f);
        pieChart.setDrawCenterText(true);         // Cho phép vẽ chữ vào giữa lỗ

        // Tùy chỉnh chú thích ở dưới
        pieChart.getLegend().setWordWrapEnabled(true);
        pieChart.getLegend().setEnabled(true);
    }

    private void loadChartData() {
        new Thread(() -> {

            long startDate = 0;
            long endDate = Long.MAX_VALUE;
            Calendar cal = Calendar.getInstance();

            if (currentFilterMode == 0) {
                cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0);
                startDate = cal.getTimeInMillis();
                cal.set(Calendar.HOUR_OF_DAY, 23); cal.set(Calendar.MINUTE, 59); cal.set(Calendar.SECOND, 59);
                endDate = cal.getTimeInMillis();
            } else if (currentFilterMode == 1) {
                cal.setFirstDayOfWeek(Calendar.MONDAY);
                cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
                cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0);
                startDate = cal.getTimeInMillis();
                cal.add(Calendar.DAY_OF_WEEK, 6);
                cal.set(Calendar.HOUR_OF_DAY, 23); cal.set(Calendar.MINUTE, 59); cal.set(Calendar.SECOND, 59);
                endDate = cal.getTimeInMillis();
            } else if (currentFilterMode == 2) {
                cal.set(Calendar.DAY_OF_MONTH, 1);
                cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0);
                startDate = cal.getTimeInMillis();
                cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
                cal.set(Calendar.HOUR_OF_DAY, 23); cal.set(Calendar.MINUTE, 59); cal.set(Calendar.SECOND, 59);
                endDate = cal.getTimeInMillis();
            }

            List<Transaction> transactions = db.appDao().getAllTransactionsByUser(currentUserId);
            Map<String, Double> categorySums = new HashMap<>();
            double totalExpenseForPeriod = 0;

            for (Transaction t : transactions) {
                if ("EXPENSE".equals(t.type) && t.date >= startDate && t.date <= endDate) {
                    totalExpenseForPeriod += t.amount;
                    double currentSum = categorySums.containsKey(t.category) ? categorySums.get(t.category) : 0;
                    categorySums.put(t.category, currentSum + t.amount);
                }
            }

            final double finalTotal = totalExpenseForPeriod;

            // 3. CHUẨN BỊ DỮ LIỆU BIỂU ĐỒ (Phải tạo entries và data trước khi dùng)
            ArrayList<PieEntry> entries = new ArrayList<>();
            for (Map.Entry<String, Double> entry : categorySums.entrySet()) {
                entries.add(new PieEntry(entry.getValue().floatValue(), entry.getKey()));
            }

            PieDataSet dataSet = new PieDataSet(entries, "");
            dataSet.setColors(MY_COLORS); // Dùng bảng màu 10 màu của bạn
            dataSet.setValueTextSize(13f);
            dataSet.setValueTextColor(Color.WHITE); // Chữ trắng nổi bật trên nền màu
            dataSet.setSliceSpace(3f); // Tạo khoảng cách giữa các miếng nhẫn cho sang

            PieData data = new PieData(dataSet);


            runOnUiThread(() -> {
                if (categorySums.isEmpty()) {
                    pieChart.clear();
                    pieChart.setNoDataText("Chưa có chi tiêu trong thời gian này.");
                    pieChart.setCenterText("TỔNG CHI\n0 đ");
                } else {
                    String centerText = "TỔNG CHI\n" + String.format("%,.0f đ", finalTotal);
                    pieChart.setCenterText(centerText);
                    pieChart.setCenterTextSize(18f);
                    pieChart.setCenterTextColor(Color.parseColor("#E53935")); // Màu đỏ cảnh báo chi tiêu

                    pieChart.setData(data);
                    pieChart.animateY(1200); // Hiệu ứng xoay lung linh
                }
                pieChart.invalidate();
            });
        }).start();
    }

    private void updateBalanceDisplay() {
        new Thread(() -> {
            User user = db.appDao().isUserExists(currentUsername);
            if (user != null) {
                double initial = user.initialBalance;
                double totalIncome = db.appDao().getTotalAmountByTypeAndUser(user.id, "INCOME");
                double totalExpense = db.appDao().getTotalAmountByTypeAndUser(user.id, "EXPENSE");
                double currentBalance = initial + totalIncome - totalExpense;

                runOnUiThread(() -> {
                    TextView tvBalance = findViewById(R.id.tvCurrentBalance);
                    if (tvBalance != null) {
                        tvBalance.setText(String.format("%,.0f", currentBalance) + " đ");
                    }
                });
            }
        }).start();
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setSelectedItemId(R.id.nav_report);

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_transactions) {
                startActivity(new Intent(ReportActivity.this, MainActivity.class));
                finish();
                return true;
            } else if (id == R.id.nav_categories) {
                startActivity(new Intent(ReportActivity.this, CategoryManagerActivity.class));
                finish();
                return true;
            } else if (id == R.id.nav_reminders) {
                startActivity(new Intent(this, ReminderManagerActivity.class));
                finish();
                return true;
            } else if (id == R.id.nav_report) {
                return true;
            } else if (id == R.id.nav_settings) {
                showSettingsAndSupportDialog();
                return false;
            }
            return false;
        });
    }

    private void showSettingsAndSupportDialog() {
        String[] options = {"⚙ Cài đặt (Đang phát triển)", "📞 Liên hệ hỗ trợ", "🚪 Đăng xuất"};

        new AlertDialog.Builder(this)
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