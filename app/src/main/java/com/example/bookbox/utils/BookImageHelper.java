package com.example.bookbox.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.widget.ImageView;
import com.example.bookbox.R;

import java.io.File;

public class BookImageHelper {
    
    public static void loadBookImage(Context context, String imagePath, ImageView imageView) {
        android.util.Log.d("BookImageHelper", "Loading book image: " + imagePath);
        
        if (imagePath != null && !imagePath.isEmpty()) {
            File imageFile = new File(context.getFilesDir(), imagePath);
            android.util.Log.d("BookImageHelper", "Book image file path: " + imageFile.getAbsolutePath());
            android.util.Log.d("BookImageHelper", "Book image file exists: " + imageFile.exists());
            
            if (imageFile.exists()) {
                try {
                    Bitmap bitmap = BitmapFactory.decodeFile(imageFile.getAbsolutePath());
                    if (bitmap != null) {
                        android.util.Log.d("BookImageHelper", "Book bitmap loaded successfully, size: " + bitmap.getWidth() + "x" + bitmap.getHeight());
                        imageView.setImageBitmap(bitmap);
                        return;
                    } else {
                        android.util.Log.e("BookImageHelper", "Failed to decode book bitmap from file");
                    }
                } catch (Exception e) {
                    android.util.Log.e("BookImageHelper", "Error loading book image: " + e.getMessage());
                    e.printStackTrace();
                }
            } else {
                android.util.Log.w("BookImageHelper", "Book image file does not exist");
            }
        } else {
            android.util.Log.d("BookImageHelper", "Book image path is null or empty");
        }

        android.util.Log.d("BookImageHelper", "Using default book image");
        imageView.setImageResource(R.drawable.baseline_menu_book_24);
    }
} 