package com.example.myapplication;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.io.Serializable;

@Entity(tableName = "transactions")
public class Transaction implements Serializable {
    @PrimaryKey(autoGenerate = true)
    public int id;
    public String title;
    public int userId;
    public double amount;
    public String type; // "Income" hoặc "Expense"
    public String category;
    public long date; // Lưu
    public String imagePath;

    public Transaction(int id, String title, double amount, String type, String category, long date) {
        this.id = id;
        this.title = title;
        this.amount = amount;
        this.type = type;
        this.category = category;
        this.date = date;
    }

    public Transaction() {
    }

}
