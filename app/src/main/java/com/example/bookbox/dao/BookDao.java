package com.example.bookbox.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.bookbox.entity.Book;

import java.util.List;

@Dao
public interface BookDao {
    @Query("SELECT * FROM Book WHERE id=:id LIMIT 1")
    Book getBook(int id);
    
    @Query("SELECT * FROM Book WHERE title=:title LIMIT 1")
    Book getBookByTitle(String title);
    
    @Query("SELECT * FROM Book WHERE genre=:genre")
    List<Book> getBooksByGenre(String genre);
    
    @Query("SELECT * FROM Book")
    List<Book> getAllBooks();
    
    @Query("SELECT * FROM Book WHERE title LIKE '%' || :title || '%'")
    List<Book> searchBooksByTitle(String title);
    
    @Query("SELECT DISTINCT genre FROM Book WHERE genre IS NOT NULL AND genre != ''")
    List<String> getDistinctGenres();
    
    @Insert
    void insert(Book book);
    
    @Update
    void update(Book book);
    
    @Delete
    void delete(Book book);
}
