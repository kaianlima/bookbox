package com.example.bookbox.adapter;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.bookbox.R;
import com.example.bookbox.entity.Book;
import com.example.bookbox.utils.BookImageHelper;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class BookManagementAdapter extends RecyclerView.Adapter<BookManagementAdapter.BookViewHolder> {

    private List<Book> books;
    private List<Book> filteredBooks;
    private Context context;
    private OnBookActionListener listener;

    public interface OnBookActionListener {
        void onEditBook(Book book);
        void onDeleteBook(Book book);
    }

    public BookManagementAdapter(Context context, OnBookActionListener listener) {
        this.context = context;
        this.listener = listener;
        this.books = new ArrayList<>();
        this.filteredBooks = new ArrayList<>();
    }

    @NonNull
    @Override
    public BookViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_manage_book, parent, false);
        return new BookViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BookViewHolder holder, int position) {
        Book book = filteredBooks.get(position);
        
        holder.textViewTitle.setText(book.getTitle());
        holder.textViewAuthor.setText(book.getAuthor());
        holder.textViewGenre.setText(book.getGenre());
        holder.textViewYear.setText(String.valueOf(book.getPublicationYear()));

        BookImageHelper.loadBookImage(context, book.getImage(), holder.imageViewBook);

        holder.buttonEdit.setOnClickListener(v -> {
            if (listener != null) {
                listener.onEditBook(book);
            }
        });

        holder.buttonDelete.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDeleteBook(book);
            }
        });
    }

    @Override
    public int getItemCount() {
        return filteredBooks.size();
    }

    public void setBooks(List<Book> books) {
        this.books = new ArrayList<>(books);
        this.filteredBooks = new ArrayList<>(books);
        notifyDataSetChanged();
    }

    public void filter(String query) {
        filteredBooks.clear();
        if (query.isEmpty()) {
            filteredBooks.addAll(books);
        } else {
            String lowerCaseQuery = query.toLowerCase();
            for (Book book : books) {
                if (book.getTitle().toLowerCase().contains(lowerCaseQuery) ||
                    book.getAuthor().toLowerCase().contains(lowerCaseQuery) ||
                    book.getGenre().toLowerCase().contains(lowerCaseQuery)) {
                    filteredBooks.add(book);
                }
            }
        }
        notifyDataSetChanged();
    }

    public static class BookViewHolder extends RecyclerView.ViewHolder {
        TextView textViewTitle, textViewAuthor, textViewGenre, textViewYear;
        ImageView imageViewBook;
        Button buttonEdit, buttonDelete;

        public BookViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewTitle = itemView.findViewById(R.id.textViewTitle);
            textViewAuthor = itemView.findViewById(R.id.textViewAuthor);
            textViewGenre = itemView.findViewById(R.id.textViewGenre);
            textViewYear = itemView.findViewById(R.id.textViewYear);
            imageViewBook = itemView.findViewById(R.id.imageViewBook);
            buttonEdit = itemView.findViewById(R.id.buttonEdit);
            buttonDelete = itemView.findViewById(R.id.buttonDelete);
        }
    }
} 