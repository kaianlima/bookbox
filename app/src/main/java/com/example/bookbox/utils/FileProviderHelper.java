package com.example.bookbox.utils;

import android.content.Context;
import android.net.Uri;
import androidx.core.content.FileProvider;
import java.io.File;
import java.io.IOException;

public class FileProviderHelper {
    
    private static final String AUTHORITY = "com.example.bookbox.fileprovider";
    private static final String TEMP_IMAGES_DIR = "temp_images";
    
    public static Uri createTempImageUri(Context context, String prefix) throws IOException {
        File tempDir = new File(context.getFilesDir(), TEMP_IMAGES_DIR);
        if (!tempDir.exists()) {
            tempDir.mkdirs();
        }

        String fileName = prefix + "_" + System.currentTimeMillis() + ".jpg";
        File tempFile = new File(tempDir, fileName);

        return FileProvider.getUriForFile(context, AUTHORITY, tempFile);
    }
    
    public static Uri createProfileImageUri(Context context, int userId) throws IOException {
        return createTempImageUri(context, "profile_" + userId);
    }
    
    public static Uri createBookImageUri(Context context) throws IOException {
        return createTempImageUri(context, "book");
    }
    
    public static File getFileFromUri(Context context, Uri uri) {
        if (uri != null && "content".equals(uri.getScheme()) && AUTHORITY.equals(uri.getAuthority())) {
            String path = uri.getPath();
            if (path != null) {
                if (path.startsWith("/temp_images/")) {
                    String fileName = path.substring("/temp_images/".length());
                    return new File(context.getFilesDir(), TEMP_IMAGES_DIR + "/" + fileName);
                }
            }
        }
        return null;
    }
    
    public static void cleanupTempImages(Context context) {
        File tempDir = new File(context.getFilesDir(), TEMP_IMAGES_DIR);
        if (tempDir.exists() && tempDir.isDirectory()) {
            File[] files = tempDir.listFiles();
            if (files != null) {
                long currentTime = System.currentTimeMillis();
                for (File file : files) {
                    if (currentTime - file.lastModified() > 3600000) {
                        file.delete();
                    }
                }
            }
        }
    }
} 