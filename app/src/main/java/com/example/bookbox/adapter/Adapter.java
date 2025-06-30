package com.example.bookbox.adapter;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.content.Context;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.bookbox.R;
import com.example.bookbox.database.LocalDatabase;
import com.example.bookbox.entity.Book;
import com.example.bookbox.entity.ReadingHistory;
import com.example.bookbox.view.PreferencesFragment;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Adapter extends BaseAdapter {
    private LayoutInflater mInflater;
    private List<Book> books;
    private Context context;
    private LocalDatabase database;
    private int currentUserId;
    private Map<Integer, Boolean> readStatusCache = new HashMap<>();

    public Adapter(Context ctx, List<Book> books, LocalDatabase database, int currentUserId) {
        mInflater = LayoutInflater.from(ctx);
        this.books = books;
        this.context = ctx;
        this.database = database;
        this.currentUserId = currentUserId;
        loadReadStatusCache();
    }

    public Adapter(Context ctx, List<Book> books) {
        mInflater = LayoutInflater.from(ctx);
        this.books = books;
        this.context = ctx;
        this.database = null;
        this.currentUserId = -1;
    }

    private void loadReadStatusCache() {
        if (database != null && currentUserId != -1) {
            new Thread(() -> {
                readStatusCache.clear();

                List<Book> booksCopy;
                synchronized (this) {
                    booksCopy = new ArrayList<>(books);
                }
                
                for (Book book : booksCopy) {
                    ReadingHistory history = database.readingHistoryModel()
                            .getReadingHistoryByBookAndUser(book.getId(), currentUserId);
                    boolean isRead = history != null && history.isRead();
                    synchronized (readStatusCache) {
                    readStatusCache.put(book.getId(), isRead);
                    }
                }

                if (context instanceof android.app.Activity) {
                    ((android.app.Activity) context).runOnUiThread(() -> {
                        notifyDataSetChanged();
                    });
                }
            }).start();
        }
    }

    @Override
    public int getCount() {
        return books.size();
    }

    @Override
    public Object getItem(int position) {
        return books.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    private class BookSupport {
        ImageView image;
        ImageView readIndicator;
        TextView textView;
        TextView ratingView;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        BookSupport bookSupport;
        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.grid_view_books, null);
            bookSupport = new BookSupport();
            bookSupport.textView = convertView.findViewById(R.id.textViewDescription);
            bookSupport.image = convertView.findViewById(R.id.imageViewBook);
            bookSupport.readIndicator = convertView.findViewById(R.id.imageViewReadIndicator);
            bookSupport.ratingView = convertView.findViewById(R.id.textViewRating);
            convertView.setTag(bookSupport);
        } else {
            bookSupport = (BookSupport) convertView.getTag();
        }
        
        Book book = books.get(position);
        bookSupport.textView.setText(book.getTitle());

        loadBookImage(bookSupport.image, book);

        checkReadStatusAndShowIndicator(bookSupport.readIndicator, book);

        loadBookRating(bookSupport.ratingView, book);

        PreferencesFragment.applyAccessibilityModeToView(convertView);
        
        return convertView;
    }

    private void checkReadStatusAndShowIndicator(ImageView readIndicator, Book book) {
        if (database != null && currentUserId != -1) {
            Boolean cachedStatus;
            synchronized (readStatusCache) {
                cachedStatus = readStatusCache.get(book.getId());
            }
            
            if (cachedStatus != null) {
                if (cachedStatus) {
                    readIndicator.setVisibility(View.VISIBLE);
                } else {
                    readIndicator.setVisibility(View.GONE);
                }
            } else {
                readIndicator.setVisibility(View.GONE);
            }
        } else {
            readIndicator.setVisibility(View.GONE);
        }
    }

    @Override
    public void notifyDataSetChanged() {
        super.notifyDataSetChanged();
    }

    public void refreshReadStatusCache() {
        if (database != null && currentUserId != -1) {
            loadReadStatusCache();
        }
    }

    private void loadBookImage(ImageView imageView, Book book) {
        String imagePath = book.getImage();

        if (imagePath != null && !imagePath.isEmpty()) {
            File imageFile = new File(context.getFilesDir(), imagePath);
            
            if (imageFile.exists()) {
                try {
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inSampleSize = 2;
                    options.inJustDecodeBounds = false;
                    
                    Bitmap bitmap = BitmapFactory.decodeFile(imageFile.getAbsolutePath(), options);
                    if (bitmap != null) {
                        imageView.setImageBitmap(bitmap);
                        return;
                    }
                } catch (Exception e) {
                    imageView.setImageResource(R.drawable.baseline_menu_book_24);
                }
            }
        }

        imageView.setImageResource(R.drawable.baseline_menu_book_24);
    }

    private void loadBookRating(TextView ratingView, Book book) {
        if (database != null) {
            new Thread(() -> {
                try {
                    Double averageRatingDouble = database.ratingModel().getAverageRatingForBook(book.getId());
                    double averageRating = averageRatingDouble != null ? averageRatingDouble : 0.0;
                    int totalRatings = database.ratingModel().getRatingCountForBook(book.getId());

                    if (context instanceof android.app.Activity) {
                        ((android.app.Activity) context).runOnUiThread(() -> {
                            if (totalRatings > 0) {
                                ratingView.setText(String.format("⭐ %.1f (%d)", averageRating, totalRatings));
                            } else {
                                ratingView.setText("⭐ S/A");
                            }
                        });
                    }
                } catch (Exception e) {
                    if (context instanceof android.app.Activity) {
                        ((android.app.Activity) context).runOnUiThread(() -> {
                            ratingView.setText("⭐ S/A");
                        });
                    }
                }
            }).start();
        } else {
            ratingView.setText("⭐ S/A");
        }
    }
}
