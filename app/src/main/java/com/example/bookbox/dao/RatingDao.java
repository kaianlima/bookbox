package com.example.bookbox.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.bookbox.entity.Rating;

import java.util.List;

@Dao
public interface RatingDao {
    @Query("SELECT * FROM Rating WHERE id=:id LIMIT 1")
    Rating getRating(int id);
    
    @Query("SELECT * FROM Rating WHERE bookId=:bookId")
    List<Rating> getRatingsByBookId(int bookId);
    
    @Query("SELECT * FROM Rating WHERE bookId=:bookId AND userId=:userId LIMIT 1")
    Rating getRatingByBookIdAndUserId(int bookId, int userId);
    
    @Query("SELECT * FROM Rating WHERE userId=:userId")
    List<Rating> getRatingsByUserId(int userId);
    
    @Query("SELECT AVG(stars) FROM Rating WHERE bookId=:bookId")
    Double getAverageRatingForBook(int bookId);
    
    @Query("SELECT COUNT(*) FROM Rating WHERE bookId=:bookId")
    int getRatingCountForBook(int bookId);
    
    @Query("DELETE FROM Rating WHERE bookId=:bookId")
    void deleteRatingsByBook(int bookId);
    
    @Insert
    void insert(Rating rating);
    
    @Update
    void update(Rating rating);
    
    @Delete
    void delete(Rating rating);
}
