package com.example.bookbox.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.example.bookbox.dao.BookDao;
import com.example.bookbox.dao.RatingDao;
import com.example.bookbox.dao.ReadingHistoryDao;
import com.example.bookbox.dao.UserDao;
import com.example.bookbox.entity.Book;
import com.example.bookbox.entity.Rating;
import com.example.bookbox.entity.ReadingHistory;
import com.example.bookbox.entity.User;

@Database(entities = {User.class, Book.class, Rating.class, ReadingHistory.class}, version = 9)
public abstract class LocalDatabase extends RoomDatabase{
    private static LocalDatabase INSTANCE;
    public static LocalDatabase getDatabase(Context context){
        if(INSTANCE==null){
            INSTANCE= Room.databaseBuilder(context.getApplicationContext(),
                    LocalDatabase.class, "BookBox")
                    .fallbackToDestructiveMigration()
                    .allowMainThreadQueries().build();
        }
        return INSTANCE;
    }

    public abstract UserDao userModel();
    public abstract BookDao bookModel();
    public abstract RatingDao ratingModel();
    public abstract ReadingHistoryDao readingHistoryModel();
}
