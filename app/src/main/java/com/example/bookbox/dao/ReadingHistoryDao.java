package com.example.bookbox.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.bookbox.entity.ReadingHistory;

import java.util.List;

@Dao
public interface ReadingHistoryDao {
    
    @Query("SELECT * FROM ReadingHistory WHERE userId=:userId")
    List<ReadingHistory> getReadingHistoryByUserId(int userId);
    
    @Query("SELECT * FROM ReadingHistory WHERE userId=:userId AND isRead=:isRead")
    List<ReadingHistory> getReadingHistoryByUserIdAndStatus(int userId, boolean isRead);
    
    @Query("SELECT * FROM ReadingHistory WHERE bookId=:bookId AND userId=:userId LIMIT 1")
    ReadingHistory getReadingHistoryByBookAndUser(int bookId, int userId);
    
    @Query("SELECT COUNT(*) FROM ReadingHistory WHERE userId=:userId AND isRead=:isRead")
    int getReadBooksCountByUser(int userId, boolean isRead);
    
    @Query("DELETE FROM ReadingHistory WHERE bookId=:bookId")
    void deleteHistoryByBook(int bookId);
    
    @Insert
    void insert(ReadingHistory readingHistory);
    
    @Update
    void update(ReadingHistory readingHistory);
    
    @Delete
    void delete(ReadingHistory readingHistory);
} 