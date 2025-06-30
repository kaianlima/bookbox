package com.example.bookbox.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.net.Uri;
import android.widget.ImageView;
import com.example.bookbox.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class ProfileImageHelper {
    
    private static final String PROFILE_IMAGES_DIR = "profile_images";
    private static final int AVATAR_SIZE = 300;
    
    public static String saveProfileImage(Context context, Uri imageUri, int userId) throws IOException {
        File imageDir = new File(context.getFilesDir(), PROFILE_IMAGES_DIR);
        if (!imageDir.exists()) {
            imageDir.mkdirs();
        }

        String fileName = "profile_" + userId + "_" + System.currentTimeMillis() + ".png";
        File destinationFile = new File(imageDir, fileName);

        try (InputStream inputStream = context.getContentResolver().openInputStream(imageUri)) {
            Bitmap originalBitmap = BitmapFactory.decodeStream(inputStream);
            if (originalBitmap != null) {
                Bitmap circularBitmap = createCircularBitmap(originalBitmap, AVATAR_SIZE);

                try (FileOutputStream outputStream = new FileOutputStream(destinationFile)) {
                    circularBitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
                }

                if (circularBitmap != originalBitmap) {
                    originalBitmap.recycle();
                }
                circularBitmap.recycle();
            } else {
                throw new IOException("Não foi possível decodificar a imagem");
            }
        }
        
        return PROFILE_IMAGES_DIR + "/" + fileName;
    }
    
    public static String saveProfileImageFromBitmap(Context context, Bitmap bitmap, int userId) throws IOException {
        File imageDir = new File(context.getFilesDir(), PROFILE_IMAGES_DIR);
        if (!imageDir.exists()) {
            imageDir.mkdirs();
        }

        String fileName = "profile_" + userId + "_" + System.currentTimeMillis() + ".png";
        File destinationFile = new File(imageDir, fileName);

        Bitmap circularBitmap = createCircularBitmap(bitmap, AVATAR_SIZE);

        try (FileOutputStream outputStream = new FileOutputStream(destinationFile)) {
            circularBitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
        }

        circularBitmap.recycle();
        
        return PROFILE_IMAGES_DIR + "/" + fileName;
    }
    
    public static String saveProfileImageFromUri(Context context, Uri imageUri, int userId) throws IOException {
        File imageDir = new File(context.getFilesDir(), PROFILE_IMAGES_DIR);
        if (!imageDir.exists()) {
            imageDir.mkdirs();
        }

        String fileName = "profile_" + userId + "_" + System.currentTimeMillis() + ".png";
        File destinationFile = new File(imageDir, fileName);

        try (InputStream inputStream = context.getContentResolver().openInputStream(imageUri)) {
            Bitmap originalBitmap = BitmapFactory.decodeStream(inputStream);
            if (originalBitmap != null) {
                Bitmap circularBitmap = createCircularBitmap(originalBitmap, AVATAR_SIZE);

                try (FileOutputStream outputStream = new FileOutputStream(destinationFile)) {
                    circularBitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
                }

                if (circularBitmap != originalBitmap) {
                    originalBitmap.recycle();
                }
                circularBitmap.recycle();
            } else {
                throw new IOException("Não foi possível decodificar a imagem do URI");
            }
        }
        
        return PROFILE_IMAGES_DIR + "/" + fileName;
    }
    
    public static void loadProfileImage(Context context, String imagePath, ImageView imageView) {
        android.util.Log.d("ProfileImageHelper", "Loading image: " + imagePath);
        
        if (imagePath != null && !imagePath.isEmpty()) {
            File imageFile = new File(context.getFilesDir(), imagePath);
            android.util.Log.d("ProfileImageHelper", "Image file path: " + imageFile.getAbsolutePath());
            android.util.Log.d("ProfileImageHelper", "Image file exists: " + imageFile.exists());
            
            if (imageFile.exists()) {
                try {
                    Bitmap bitmap = BitmapFactory.decodeFile(imageFile.getAbsolutePath());
                    if (bitmap != null) {
                        android.util.Log.d("ProfileImageHelper", "Bitmap loaded successfully, size: " + bitmap.getWidth() + "x" + bitmap.getHeight());
                        imageView.setImageBitmap(bitmap);
                        return;
                    } else {
                        android.util.Log.e("ProfileImageHelper", "Failed to decode bitmap from file");
                    }
                } catch (Exception e) {
                    android.util.Log.e("ProfileImageHelper", "Error loading image: " + e.getMessage());
                    e.printStackTrace();
                }
            } else {
                android.util.Log.w("ProfileImageHelper", "Image file does not exist");
            }
        } else {
            android.util.Log.d("ProfileImageHelper", "Image path is null or empty");
        }

        android.util.Log.d("ProfileImageHelper", "Using default avatar");
        imageView.setImageResource(R.drawable.avatar);
    }
    
    public static String getDefaultAvatarPath() {
        return null;
    }
    
    private static Bitmap createCircularBitmap(Bitmap source, int size) {
        Bitmap scaledBitmap = Bitmap.createScaledBitmap(source, size, size, true);

        Bitmap output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);
        
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        
        Rect rect = new Rect(0, 0, size, size);

        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(0xFF000000);
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint);

        paint.setXfermode(new android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(scaledBitmap, rect, rect, paint);

        if (scaledBitmap != source) {
            scaledBitmap.recycle();
        }
        
        return output;
    }
    
    public static void deleteProfileImage(Context context, String imagePath) {
        if (imagePath != null && !imagePath.isEmpty()) {
            File imageFile = new File(context.getFilesDir(), imagePath);
            if (imageFile.exists()) {
                imageFile.delete();
            }
        }
    }
} 