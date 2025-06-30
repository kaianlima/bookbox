package com.example.bookbox.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;

@Entity(foreignKeys={@ForeignKey(entity = Book.class,
        parentColumns = "id", childColumns = "bookId",
        onDelete = ForeignKey.CASCADE, onUpdate = ForeignKey.CASCADE),
        @ForeignKey(entity = User.class,
                parentColumns = "id", childColumns = "userId",
                onDelete = ForeignKey.CASCADE, onUpdate = ForeignKey.CASCADE)})
public class Rating {

    @PrimaryKey(autoGenerate = true)
    private int id;
    private int stars;
    private int bookId;
    private int userId;

    public Rating(int stars, int bookId, int userId) {
        this.stars = stars;
        this.bookId = bookId;
        this.userId = userId;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getStars() {
        return stars;
    }

    public void setStars(int stars) {
        this.stars = stars;
    }

    public int getBookId() {
        return bookId;
    }

    public void setBookId(int bookId) {
        this.bookId = bookId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    @NonNull
    @Override
    public String toString() {
        return Integer.toString(stars);
    }
}
