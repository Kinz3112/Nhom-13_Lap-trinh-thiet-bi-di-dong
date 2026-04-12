package com.example.myapplication;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class RegisterActivity extends AppCompatActivity {

    private AppDatabase db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        db = AppDatabase.getInstance(this);

        EditText etUser = findViewById(R.id.etRegUser);
        EditText etPass = findViewById(R.id.etRegPass);
        Button btnReg = findViewById(R.id.btnRegister);

        btnReg.setOnClickListener(v -> {
            String userStr = etUser.getText().toString().trim();
            String passStr = etPass.getText().toString().trim();

            if (userStr.isEmpty() || passStr.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show();
                return;
            }

            new Thread(() -> {
                if (db.appDao().isUserExists(userStr) == null) {
                    User newUser = new User();
                    newUser.username = userStr;
                    newUser.password = passStr;
                    newUser.initialBalance = -1;
                    db.appDao().insertUser(newUser);

                    runOnUiThread(() -> {
                        Toast.makeText(this, "Đăng ký thành công!", Toast.LENGTH_SHORT).show();
                        finish();
                    });
                } else {
                    runOnUiThread(() -> {
                        Toast.makeText(this, "Tên đăng nhập đã bị trùng!", Toast.LENGTH_SHORT).show();
                    });
                }
            }).start();
        });
    }
}