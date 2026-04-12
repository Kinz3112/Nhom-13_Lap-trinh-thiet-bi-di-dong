package com.example.myapplication;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import java.io.Serializable;
@Entity(tableName = "reminders")
public class Reminder implements Serializable {
    @PrimaryKey(autoGenerate = true)
    public int id;
    public int userId;
    public String title;        // Tên lời nhắc
    public String frequency;    // Hàng ngày, Hàng tuần, Hàng tháng
    public long startDate;      // Ngày bắt đầu (timestamp)
    public int hour;            // Giờ nhắc
    public int minute;          // Phút nhắc
    public String note;         // Ghi chú
    public boolean isActive;    // Trạng thái bật/tắt nhắc nhở
}