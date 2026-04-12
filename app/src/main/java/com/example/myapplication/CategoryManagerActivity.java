package com.example.myapplication;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.List;

public class CategoryManagerActivity extends AppCompatActivity {
    private AppDatabase db;
    private RecyclerView rvCategories;
    private EditText etCategoryName;
    private Button btnAdd;
    private CategoryAdapter adapter;

    private int currentUserId = -1;
    private String currentUsername = "";

    private double totalUserMoney = 0;
    private List<Category> currentCategoryList;
    private AlertDialog activeDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_category_manager);

        db = AppDatabase.getInstance(this);
        rvCategories = findViewById(R.id.rvCategories);
        etCategoryName = findViewById(R.id.etCategoryName);
        btnAdd = findViewById(R.id.btnAddCategory);

        rvCategories.setLayoutManager(new LinearLayoutManager(this));

        SharedPreferences pref = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        currentUserId = pref.getInt("userId", -1);
        currentUsername = pref.getString("username", "");

        fetchTotalUserMoney();
        updateBalanceDisplay();

        btnAdd.setOnClickListener(v -> addCategory());
        loadCategories();

        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);

        bottomNav.setSelectedItemId(R.id.nav_categories);

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_transactions) {
                startActivity(new Intent(CategoryManagerActivity.this, MainActivity.class));
                finish();
                return true;
            } else if (id == R.id.nav_categories) {
                return true;
            } else if (id == R.id.nav_reminders) {
                startActivity(new Intent(this, ReminderManagerActivity.class));
                finish();
                return false;
            } else if (id == R.id.nav_report) {
                startActivity(new Intent(CategoryManagerActivity.this, ReportActivity.class));
                finish();
                return true;
            } else if (id == R.id.nav_settings) {
                showSettingsAndSupportDialog();
                return false;
            }
            return false;
        });
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

    private void fetchTotalUserMoney() {
        new Thread(() -> {
            User user = db.appDao().isUserExists(currentUsername);
            if (user != null) {
                totalUserMoney = user.initialBalance;
            }
        }).start();
    }

    private void loadCategories() {
        new Thread(() -> {
            currentCategoryList = db.appDao().getCategoriesForUser(currentUserId);
            runOnUiThread(() -> {
                adapter = new CategoryAdapter(currentCategoryList, new CategoryAdapter.OnCategoryClickListener() {
                    @Override
                    public void onDeleteClick(Category category) {
                        checkAndDeleteCategory(category);
                    }

                    @Override
                    public void onJarClick(Category category) {
                        // Nhấn vào tên hũ
                        showEditAmountDialog(category);
                    }
                });
                rvCategories.setAdapter(adapter);
            });
        }).start();
    }
    private void addCategory() {
        String name = etCategoryName.getText().toString().trim();
        if (name.isEmpty() || currentUserId == -1) return;

        new Thread(() -> {
            Category cat = new Category();
            cat.userId = currentUserId;
            cat.name = name;
            cat.type = "EXPENSE";
            cat.balance = 0;

            db.appDao().insertCategory(cat);
            runOnUiThread(() -> {
                etCategoryName.setText("");
                loadCategories();
                Toast.makeText(CategoryManagerActivity.this, "Đã thêm hũ mới!", Toast.LENGTH_SHORT).show();
            });
        }).start();
    }

    private void checkAndDeleteCategory(Category category) {
        if (category.name.equalsIgnoreCase("Khác")) {
            Toast.makeText(this, "Bạn không thể xóa hũ 'Khác' vì đây là hũ mặc định của hệ thống!", Toast.LENGTH_LONG).show();
            return;
        }
        if (activeDialog != null && activeDialog.isShowing()) return;

        new Thread(() -> {
            int count = db.appDao().countTransactionsByCategory(category.name, currentUserId);

            runOnUiThread(() -> {
                if (count > 0) {
                    Toast.makeText(this, "Không thể xóa! Hũ này đang có " + count + " giao dịch.", Toast.LENGTH_LONG).show();
                } else {
                    showDeleteDialog(category);
                }
            });
        }).start();
    }

    private void showDeleteDialog(Category category) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle("Xóa hũ?")
                .setMessage("Bạn có chắc muốn xóa hũ '" + category.name + "' không?")
                .setPositiveButton("Xóa", (dialog, which) -> {
                    new Thread(() -> {
                        db.appDao().deleteCategory(category);
                        runOnUiThread(() -> loadCategories());
                    }).start();
                })
                .setNegativeButton("Hủy", null);

        activeDialog = builder.show();
    }

    private void showEditAmountDialog(Category category) {
        if (activeDialog != null && activeDialog.isShowing()) return;

        new Thread(() -> {
            User user = db.appDao().isUserExists(currentUsername);
            double initial = user.initialBalance;
            double totalIncome = db.appDao().getTotalAmountByTypeAndUser(user.id, "INCOME");
            double totalExpense = db.appDao().getTotalAmountByTypeAndUser(user.id, "EXPENSE");


            double currentRealTotalBalance = initial + totalIncome - totalExpense;


            double sumOtherJars = 0;
            for (Category c : currentCategoryList) {
                if (c.id != category.id) {
                    sumOtherJars += c.balance;
                }
            }


            double maxAllowedForThisJar = currentRealTotalBalance - sumOtherJars;


            runOnUiThread(() -> {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("Nạp tiền vào: " + category.name);
                builder.setMessage("Tối đa có thể nạp: " + String.format("%,.0f đ", maxAllowedForThisJar));

                final EditText input = new EditText(this);
                input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
                input.setPadding(50, 50, 50, 50);

                if (category.balance > 0) {
                    input.setText(String.valueOf((long) category.balance));
                }

                builder.setView(input);

                builder.setPositiveButton("Lưu", (dialog, which) -> {
                    String amountStr = input.getText().toString().trim();
                    double newAmount = amountStr.isEmpty() ? 0 : Double.parseDouble(amountStr);


                    if (newAmount > (maxAllowedForThisJar + 0.1)) {
                        Toast.makeText(this, "LỖI: Bạn không còn đủ tiền! Tối đa chỉ được nạp: " + String.format("%,.0f đ", maxAllowedForThisJar), Toast.LENGTH_LONG).show();
                        return;
                    }

                    new Thread(() -> {
                        category.balance = newAmount;
                        db.appDao().updateCategory(category);
                        runOnUiThread(() -> loadCategories());
                    }).start();
                });

                builder.setNegativeButton("Hủy", (dialog, which) -> dialog.cancel());

                activeDialog = builder.show();
            });
        }).start();
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