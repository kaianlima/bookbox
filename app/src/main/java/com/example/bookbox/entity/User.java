package com.example.bookbox.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "User")
public class User {

    @PrimaryKey(autoGenerate = true)
    private int id;
    private String username;
    private String password;
    private String type;
    private String profileImage;

    public User(String username, String password, String type) {
        this.username = username;
        this.password = password;
        this.type = type;
        this.profileImage = null;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getProfileImage() {
        return profileImage;
    }

    public void setProfileImage(String profileImage) {
        this.profileImage = profileImage;
    }

    @NonNull
    @Override
    public String toString() {
        return "[" + id + "] " + username + " - Tipo = " + type;
    }
}
