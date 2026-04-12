package com.example.myapplication;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import java.util.List;

@Dao
public interface AppDao {
    @Insert
    void insertUser(User user);

    @Query("SELECT * FROM users WHERE username = :user AND password = :pass LIMIT 1")
    User getUser(String user, String pass);

    @Query("SELECT * FROM users WHERE username = :user LIMIT 1")
    User isUserExists(String user);

    @Insert
    void insertTransaction(Transaction transaction);

    @Update
    void updateTransaction(Transaction transaction);

    @Delete
    void deleteTransaction(Transaction transaction);
    @Query("UPDATE users SET initialBalance = :balance WHERE username = :user")
    void updateInitialBalance(String user, double balance);

    @Query("SELECT * FROM transactions ORDER BY date DESC")
    List<Transaction> getAllTransactions();

    @Query("SELECT SUM(amount) FROM transactions WHERE type = :type")
    double getTotalAmountByType(String type);

    // --- DANH MỤC (CATEGORIES) ---
    @Insert
    void insertCategory(Category category);

    @Query("SELECT * FROM categories")
    List<Category> getAllCategories();

    @Delete
    void deleteCategory(Category category);
    @Query("SELECT * FROM transactions WHERE userId = :userId ORDER BY date DESC")
    List<Transaction> getAllTransactionsByUser(int userId);

    @Query("SELECT SUM(amount) FROM transactions WHERE userId = :userId AND type = :type")
    double getTotalAmountByTypeAndUser(int userId, String type);
    @Query("UPDATE categories SET balance = balance + :amount WHERE name = :catName AND userId = :userId")
    void updateCategoryBalance(String catName, int userId, double amount);
    @Query("SELECT balance FROM categories WHERE name = :categoryName AND userId = :userId LIMIT 1") double getCategoryBalance(String categoryName, int userId);
    @Query("SELECT * FROM categories WHERE userId = :userId") List<Category> getCategoriesForUser(int userId);
    @Update
    void updateCategories(List<Category> categories);
    @Update
    void updateCategory(Category category);
    @Query("SELECT COUNT(*) FROM transactions WHERE category = :categoryName AND userId = :userId")
    int countTransactionsByCategory(String categoryName, int userId);
    @Query("SELECT * FROM transactions WHERE userId = :userId AND title LIKE '%' || :searchQuery || '%' AND date >= :startDate AND date <= :endDate ORDER BY date DESC")
    List<Transaction> searchAndFilterTransactions(int userId, String searchQuery, long startDate, long endDate);
    @Insert
    void insertReminder(Reminder reminder);
    @Query("SELECT * FROM reminders WHERE userId = :userId ORDER BY hour, minute ASC")
    List<Reminder> getRemindersForUser(int userId);

    @Update
    void updateReminder(Reminder reminder);

    @Delete
    void deleteReminder(Reminder reminder);

}