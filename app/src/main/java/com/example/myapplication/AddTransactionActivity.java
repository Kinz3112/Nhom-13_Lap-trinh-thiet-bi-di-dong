package com.example.myapplication;

import android.app.DatePickerDialog;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import androidx.appcompat.app.AlertDialog;

public class AddTransactionActivity extends AppCompatActivity {
    private Transaction existingTransaction;
    private boolean isEditMode = false;

    private EditText etTitle, etAmount;
    private Spinner spCategory;
    private RadioButton rbExpense, rbIncome;
    private Button btnSave;
    private AppDatabase db;
    private ImageView ivPreview;
    private String internalImagePath = null;
    private TextView tvSelectedDate;
    private Calendar calendar;
    private long selectedDateTimestamp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_transaction);

        // Ánh xạ View
        etTitle = findViewById(R.id.etTitle);
        etAmount = findViewById(R.id.etAmount);
        spCategory = findViewById(R.id.spCategory);
        rbExpense = findViewById(R.id.rbExpense);
        rbIncome = findViewById(R.id.rbIncome);
        btnSave = findViewById(R.id.btnSave);
        ivPreview = findViewById(R.id.ivPreview);
        Button btnPickImage = findViewById(R.id.btnPickImage);
        tvSelectedDate = findViewById(R.id.tvSelectedDate);
        calendar = Calendar.getInstance();
        spCategory = findViewById(R.id.spCategory);

        db = AppDatabase.getInstance(this);

        loadCategoriesToSpinner();


        if (getIntent().hasExtra("EDIT_TRANSACTION")) {
            isEditMode = true;
            existingTransaction = (Transaction) getIntent().getSerializableExtra("EDIT_TRANSACTION");

            etTitle.setText(existingTransaction.title);
            etAmount.setText(String.valueOf((int) existingTransaction.amount));
            btnSave.setText("CẬP NHẬT GIAO DỊCH");

            if ("INCOME".equals(existingTransaction.type)) {
                rbIncome.setChecked(true);
            } else {
                rbExpense.setChecked(true);
            }

            // Hiển thị ảnh cũ
            if (existingTransaction.imagePath != null) {
                internalImagePath = existingTransaction.imagePath;
                ivPreview.setImageURI(Uri.parse(internalImagePath));
                ivPreview.setVisibility(View.VISIBLE);
            }
        }
        if (isEditMode) {
            selectedDateTimestamp = existingTransaction.date;
        } else {
            selectedDateTimestamp = System.currentTimeMillis();
        }
        updateDateLabel();

        tvSelectedDate.setOnClickListener(v -> showDatePicker());


        ActivityResultLauncher<String> pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        ivPreview.setImageURI(uri);
                        ivPreview.setVisibility(View.VISIBLE);
                        internalImagePath = copyImageToInternalStorage(uri);
                    }
                }
        );

        btnPickImage.setOnClickListener(v -> pickImageLauncher.launch("image/*"));
        btnSave.setOnClickListener(v -> saveTransaction());
    }
    private void loadCategoriesToSpinner() {
        new Thread(() -> {
            SharedPreferences pref = getSharedPreferences("UserPrefs", MODE_PRIVATE);
            int userId = pref.getInt("userId", -1);

            List<Category> userCategories = db.appDao().getCategoriesForUser(userId);

            List<String> categoryNames = new ArrayList<>();
            for (Category cat : userCategories) {
                categoryNames.add(cat.name);
            }

            runOnUiThread(() -> {
                ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(AddTransactionActivity.this, android.R.layout.simple_spinner_item, categoryNames);
                spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spCategory.setAdapter(spinnerAdapter);

                if (isEditMode && existingTransaction != null) {
                    int spinnerPosition = spinnerAdapter.getPosition(existingTransaction.category);
                    if (spinnerPosition >= 0) {
                        spCategory.setSelection(spinnerPosition);
                    }
                }
            });
        }).start();
    }

    private void saveTransaction() {
        String title = etTitle.getText().toString().trim();
        String amountStr = etAmount.getText().toString().trim();
        String category = spCategory.getSelectedItem() != null ? spCategory.getSelectedItem().toString() : "";
        String type = rbExpense.isChecked() ? "EXPENSE" : "INCOME";

        if (title.isEmpty() || amountStr.isEmpty() || category.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập đủ thông tin", Toast.LENGTH_SHORT).show();
            return;
        }

        double amount = Double.parseDouble(amountStr);

        new Thread(() -> {
            SharedPreferences pref = getSharedPreferences("UserPrefs", MODE_PRIVATE);
            String username = pref.getString("username", "");
            User user = db.appDao().isUserExists(username);

            if (user == null) return;

            if (type.equals("EXPENSE")) {
                double currentBalance = db.appDao().getCategoryBalance(category, user.id);

                if (amount > currentBalance) {
                    runOnUiThread(() -> showBudgetWarningDialog(title, amount, type, category, user.id, currentBalance));
                } else {
                    performSaveTransaction(title, amount, type, category, user.id);
                }
            } else {
                // Nếu là thu nhập (INCOME) thì không cần kiểm tra ngân sách
                performSaveTransaction(title, amount, type, category, user.id);
            }
        }).start();
    }
    private void showBudgetWarningDialog(String title, double amount, String type, String category, int userId, double currentBalance) {
        new AlertDialog.Builder(this)
                .setTitle("Cảnh báo ngân sách!")
                .setMessage("Hũ '" + category + "' của bạn chỉ còn " + String.format("%,.0f đ", currentBalance) + ".\n\n" +
                        "Bạn đang định chi " + String.format("%,.0f đ", amount) + ".\n" +
                        "Bạn có chắc chắn muốn tiêu lố quỹ không?")
                .setCancelable(false)
                .setPositiveButton("Vẫn chi", (dialog, which) -> {
                    new Thread(() -> performSaveTransaction(title, amount, type, category, userId)).start();
                })
                .setNegativeButton("Hủy bỏ", (dialog, which) -> {
                    if (btnSave != null) btnSave.setEnabled(true);
                    dialog.dismiss();
                })
                .show();
    }

    private void performSaveTransaction(String title, double amount, String type, String category, int userId) {
        try {
            if (isEditMode && existingTransaction != null) {
                double oldAmountToRevert = existingTransaction.type.equals("EXPENSE") ? existingTransaction.amount : -existingTransaction.amount;
                db.appDao().updateCategoryBalance(existingTransaction.category, userId, oldAmountToRevert);
                existingTransaction.title = title;
                existingTransaction.amount = amount;
                existingTransaction.type = type;
                existingTransaction.category = category;
                existingTransaction.date = selectedDateTimestamp;
                if (internalImagePath != null) existingTransaction.imagePath = internalImagePath;


                db.appDao().updateTransaction(existingTransaction);


                double newAmountToUpdate = type.equals("EXPENSE") ? -amount : amount;
                db.appDao().updateCategoryBalance(category, userId, newAmountToUpdate);

            } else {

                Transaction transaction = new Transaction();
                transaction.title = title;
                transaction.amount = amount;
                transaction.type = type;
                transaction.category = category;
                transaction.userId = userId;
                transaction.date = selectedDateTimestamp != 0 ? selectedDateTimestamp : System.currentTimeMillis();
                transaction.imagePath = internalImagePath;

                db.appDao().insertTransaction(transaction);

                double amountToUpdate = type.equals("EXPENSE") ? -amount : amount;
                db.appDao().updateCategoryBalance(category, userId, amountToUpdate);
            }

            // Kết thúc: Thông báo và đóng màn hình
            runOnUiThread(() -> {
                String msg = isEditMode ? "Cập nhật thành công!" : "Thêm mới thành công!";
                Toast.makeText(AddTransactionActivity.this, msg, Toast.LENGTH_SHORT).show();
                finish();
            });

        } catch (Exception e) {
            e.printStackTrace();
            runOnUiThread(() -> {
                if (btnSave != null) btnSave.setEnabled(true);
                Toast.makeText(AddTransactionActivity.this, "LỖI: " + e.getMessage(), Toast.LENGTH_LONG).show();
            });
        }
    }


    private String copyImageToInternalStorage(Uri uri) {
        try {
            InputStream is = getContentResolver().openInputStream(uri);
            String fileName = "img_" + System.currentTimeMillis() + ".jpg";
            File file = new File(getFilesDir(), fileName);
            FileOutputStream fos = new FileOutputStream(file);

            byte[] buffer = new byte[1024];
            int read;
            while ((read = is.read(buffer)) != -1) {
                fos.write(buffer, 0, read);
            }
            is.close();
            fos.close();
            return file.getAbsolutePath();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    private void showDatePicker() {
        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                (view, year, month, dayOfMonth) -> {
                    calendar.set(Calendar.YEAR, year);
                    calendar.set(Calendar.MONTH, month);
                    calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);

                    selectedDateTimestamp = calendar.getTimeInMillis();
                    updateDateLabel();
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH));
        datePickerDialog.show();
    }

    private void updateDateLabel() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        tvSelectedDate.setText(sdf.format(new java.util.Date(selectedDateTimestamp)));
    }
}