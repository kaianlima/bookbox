package com.example.bookbox.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.bookbox.entity.User;

import java.util.List;

@Dao
public interface UserDao {
    @Query("SELECT * FROM User WHERE id=:id LIMIT 1")
    User getUser(int id);
    @Query("SELECT * FROM User WHERE username=:username LIMIT 1")
    User getUserByName(String username);
    @Query("SELECT * FROM User")
    List<User> getAllUsers();
    @Insert
    void insert(User user);
    @Update
    void update(User user);
    @Delete
    void delete(User user);
}
