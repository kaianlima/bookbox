package com.example.bookbox.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;

import java.util.Date;

@Entity(tableName = "ReadingHistory",
        foreignKeys = {
                @ForeignKey(entity = User.class,
                        parentColumns = "id",
                        childColumns = "userId",
                        onDelete = ForeignKey.CASCADE),
                @ForeignKey(entity = Book.class,
                        parentColumns = "id",
                        childColumns = "bookId",
                        onDelete = ForeignKey.CASCADE)
        })
public class ReadingHistory {

    @PrimaryKey(autoGenerate = true)
    private int id;
    private int userId;
    private int bookId;
    private boolean isRead;
    private long readDate;

    public ReadingHistory(int userId, int bookId, boolean isRead, long readDate) {
        this.userId = userId;
        this.bookId = bookId;
        this.isRead = isRead;
        this.readDate = readDate;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public int getBookId() {
        return bookId;
    }

    public void setBookId(int bookId) {
        this.bookId = bookId;
    }

    public boolean isRead() {
        return isRead;
    }

    public void setRead(boolean read) {
        isRead = read;
    }

    public long getReadDate() {
        return readDate;
    }

    public void setReadDate(long readDate) {
        this.readDate = readDate;
    }

    @NonNull
    @Override
    public String toString() {
        return "[" + id + "] User: " + userId + " Book: " + bookId + " Read: " + isRead;
    }
} 