package com.example.myapplication;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.icu.util.Calendar;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.util.List;

public class MainActivity extends AppCompatActivity implements TransactionAdapter.OnTransactionLongClickListener{

    private AppDatabase db;
    private TransactionAdapter adapter;
    private RecyclerView rvTransactions;
    private int currentUserId;
    private EditText etSearch;
    private Spinner spTimeFilter;
    private String currentSearchQuery = "";
    private int currentFilterMode = 0; // 0: Tất cả, 1: Tháng này, 2: Tháng trước

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //kiểm tra đăng nhập TRƯỚC khi nạp giao diện để nếu cần chuyển trang thì chuyển luôn
        SharedPreferences pref = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        if (!pref.getBoolean("isLoggedIn", false)) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_main);

        //Ánh xạ View
        rvTransactions = findViewById(R.id.rvTransactions);
        etSearch = findViewById(R.id.etSearch);
        spTimeFilter = findViewById(R.id.spTimeFilter);

        setupSearchAndFilter(); // Gọi hàm thiết lập
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        FloatingActionButton fabAdd = findViewById(R.id.fab_add);

        //khởi tạo dữ liệu và Database
        db = AppDatabase.getInstance(this);
        currentUserId = pref.getInt("userId", -1);

        // thiết lập các chức năng
        checkInitialBalance();
        addDefaultCategories();

        rvTransactions.setLayoutManager(new LinearLayoutManager(this));

        // menu
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_transactions) {
                return true;
            } else if (id == R.id.nav_categories) {
                startActivity(new Intent(MainActivity.this, CategoryManagerActivity.class));
                return true;
            } else if (id == R.id.nav_reminders) {
                startActivity(new Intent(this, ReminderManagerActivity.class));
                return true;
            } else if (id == R.id.nav_report) {
                startActivity(new Intent(MainActivity.this, ReportActivity.class));
                return true;
            } else if (id == R.id.nav_settings) {
                showSettingsAndSupportDialog();
                return false;
            }
            return false;
        });

        if (fabAdd != null) {
            fabAdd.setOnClickListener(v -> onAddClick());
        }

        loadData();
    }

    private void addSampleData() {
        new Thread(() -> {
            Transaction trans = new Transaction();
            trans.userId = currentUserId;
            trans.title = "Mua cà phê";
            trans.amount = 30000;
            trans.category = "Ăn uống";
            trans.type = "EXPENSE";
            trans.date = System.currentTimeMillis();

            db.appDao().insertTransaction(trans);

            loadData();

            runOnUiThread(() ->
                    Toast.makeText(MainActivity.this, "Đã thêm giao dịch mẫu!", Toast.LENGTH_SHORT).show()
            );
        }).start();
    }

    private void loadData() {
        new Thread(() -> {
            List<Transaction> list = db.appDao().getAllTransactionsByUser(currentUserId);
            runOnUiThread(() -> {
                adapter = new TransactionAdapter(list, this);
                rvTransactions.setAdapter(adapter);
            });
        }).start();
    }

    private void confirmDelete(Transaction transaction) {
        new android.app.AlertDialog.Builder(this)
                .setTitle("Xác nhận xóa")
                .setMessage("Bạn có chắc chắn muốn xóa giao dịch này không?")
                .setPositiveButton("Xóa", (dialog, which) -> {
                    new Thread(() -> {
                        db.appDao().deleteTransaction(transaction);
                        loadData();
                        updateBalanceDisplay();
                    }).start();
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    public void onAddClick() {
        Intent intent = new Intent(MainActivity.this, AddTransactionActivity.class);
        startActivity(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadData();
        updateBalanceDisplay();
    }
    @Override
    public void onEdit(Transaction transaction) {
        Intent intent = new Intent(MainActivity.this, AddTransactionActivity.class);
        intent.putExtra("EDIT_TRANSACTION", transaction);
        startActivity(intent);
    }

    @Override
    public void onDelete(Transaction transaction) {
        new AlertDialog.Builder(this)
                .setTitle("Xóa giao dịch")
                .setMessage("Bạn có chắc chắn muốn xóa '" + transaction.title + "'? Số tiền sẽ được hoàn trả vào hũ tương ứng.")
                .setPositiveButton("Xóa", (dialog, which) -> {
                    deleteTransaction(transaction);
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void addDefaultCategories() {
        new Thread(() -> {

            if (db.appDao().getCategoriesForUser(currentUserId).isEmpty()) {
                String[] defaults = {"Ăn uống", "Di chuyển", "Mua sắm", "Tiết kiệm", "Giải trí", "Khác"};
                for (String name : defaults) {
                    Category cat = new Category();
                    cat.userId = currentUserId;
                    cat.name = name;
                    cat.type = "EXPENSE"; // Mặc định là chi phí
                    cat.balance = 0;      // Số dư ban đầu của hũ là 0đ
                    db.appDao().insertCategory(cat);
                }
            }
        }).start();
    }

    private void checkInitialBalance() {
        SharedPreferences pref = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        String username = pref.getString("username", "");
        if (username.isEmpty()) return;
        new Thread(() -> {
            User user = db.appDao().isUserExists(username);
            if (user != null && user.initialBalance == -1) {
                // Nếu số dư bằng 0, yêu cầu nhập
                runOnUiThread(() -> showInputBalanceDialog(username));
            } else {
                // Nếu đã có số dư, cập nhật thanh hiển thị
                updateBalanceDisplay();
            }
        }).start();
    }

    private void showInputBalanceDialog(String username) {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(MainActivity.this);
        builder.setTitle("Thiết lập số dư ban đầu");
        builder.setMessage("Chào " + username + ", hãy nhập số tiền hiện có trong ví của bạn:");

        final EditText input = new EditText(MainActivity.this);
        input.setHint("Ví dụ: 500000");
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        builder.setView(input);

        builder.setCancelable(false); // Ngăn người dùng tắt dialog mà không nhập
        builder.setPositiveButton("Xác nhận", (dialog, which) -> {
            String balanceStr = input.getText().toString().trim();
            if (!balanceStr.isEmpty()) {
                double balance = Double.parseDouble(balanceStr);
                saveInitialBalance(username, balance);
            } else {
                Toast.makeText(this, "Bạn phải nhập số tiền!", Toast.LENGTH_SHORT).show();
                showInputBalanceDialog(username); // Hiện lại nếu nhập trống
            }
        });

        builder.show();
    }

    private void saveInitialBalance(String username, double balance) {
        new Thread(() -> {
            db.appDao().updateInitialBalance(username, balance);
            User user = db.appDao().isUserExists(username); // Lấy user để truyền ID

            runOnUiThread(() -> {
                Toast.makeText(this, "Hãy chia số tiền này vào các hũ!", Toast.LENGTH_LONG).show();

                // màn hình chia hũ
                Intent intent = new Intent(MainActivity.this, CategoryManagerActivity.class);
                intent.putExtra("TOTAL_AMOUNT", balance);
                intent.putExtra("USER_ID", user.id);
                startActivity(intent);
            });
        }).start();
    }
    private void updateBalanceDisplay() {
        new Thread(() -> {
            SharedPreferences pref = getSharedPreferences("UserPrefs", MODE_PRIVATE);
            String username = pref.getString("username", "");
            User user = db.appDao().isUserExists(username);

            if (user != null) {
                double initial = user.initialBalance;
                double totalIncome = db.appDao().getTotalAmountByTypeAndUser(user.id, "INCOME");
                double totalExpense = db.appDao().getTotalAmountByTypeAndUser(user.id, "EXPENSE");

                double currentBalance = initial + totalIncome - totalExpense;
                runOnUiThread(() -> {
                    TextView tvBalance = findViewById(R.id.tvCurrentBalance);
                    tvBalance.setText(String.format("%,.0f", currentBalance) + " đ");
                });
            }
        }).start();
    }
    private void deleteTransaction(Transaction transaction) {
        new Thread(() -> {
            try {
                double amountToRevert = transaction.type.equals("EXPENSE") ? transaction.amount : -transaction.amount;

                db.appDao().updateCategoryBalance(transaction.category, transaction.userId, amountToRevert);

                db.appDao().deleteTransaction(transaction);

                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "Đã xóa giao dịch và hoàn tiền vào hũ!", Toast.LENGTH_SHORT).show();
                    loadFilteredData();
                    updateBalanceDisplay();
                });
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "Lỗi khi xóa giao dịch", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void setupSearchAndFilter() {
        String[] filterOptions = {"Tất cả thời gian", "Tháng này", "Tháng trước"};
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, filterOptions);
        spTimeFilter.setAdapter(spinnerAdapter);

        spTimeFilter.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, android.view.View view, int position, long id) {
                currentFilterMode = position;
                loadFilteredData();
            }
            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });

        etSearch.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearchQuery = s.toString().trim();
                loadFilteredData();
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });
    }
    private void loadFilteredData() {
        new Thread(() -> {
            SharedPreferences pref = getSharedPreferences("UserPrefs", MODE_PRIVATE);
            String username = pref.getString("username", "");
            User user = db.appDao().isUserExists(username);

            if (user != null) {
                long startDate = 0;
                long endDate = Long.MAX_VALUE;

                Calendar cal = Calendar.getInstance();

                if (currentFilterMode == 1) {
                    // THÁNG NÀY
                    cal.set(Calendar.DAY_OF_MONTH, 1);
                    cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0);
                    startDate = cal.getTimeInMillis();
                    endDate = System.currentTimeMillis(); // Đến hiện tại
                } else if (currentFilterMode == 2) {
                    // THÁNG TRƯỚC
                    cal.add(Calendar.MONTH, -1); // Lùi về tháng trước
                    cal.set(Calendar.DAY_OF_MONTH, 1); // Ngày đầu tháng trước
                    cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0);
                    startDate = cal.getTimeInMillis();

                    cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH)); // Ngày cuối tháng trước
                    cal.set(Calendar.HOUR_OF_DAY, 23); cal.set(Calendar.MINUTE, 59); cal.set(Calendar.SECOND, 59);
                    endDate = cal.getTimeInMillis();
                }

                List<Transaction> filteredList = db.appDao().searchAndFilterTransactions(user.id, currentSearchQuery, startDate, endDate);

                runOnUiThread(() -> {
                    adapter = new TransactionAdapter(filteredList, MainActivity.this);
                    rvTransactions.setAdapter(adapter);
                });
            }
        }).start();
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