package com.example.bookbox.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "Book")
public class Book {

    @PrimaryKey(autoGenerate = true)
    private int id;
    private String genre;
    private String title;
    private String image;
    private String description;
    private String author;
    private int publicationYear;

    public Book() {}

    @Ignore
    public Book(String genre, String title, String image, String description) {
        this.genre = genre;
        this.title = title;
        this.image = image;
        this.description = description;
    }

    @Ignore
    public Book(String genre, String title, String author, String image, String description, int publicationYear) {
        this.genre = genre;
        this.title = title;
        this.author = author;
        this.image = image;
        this.description = description;
        this.publicationYear = publicationYear;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getGenre() {
        return genre;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public int getPublicationYear() {
        return publicationYear;
    }

    public void setPublicationYear(int publicationYear) {
        this.publicationYear = publicationYear;
    }

    @NonNull
    @Override
    public String toString() {
        return "[" + id + "] " + title + " - Genero = " + genre;
    }
}
