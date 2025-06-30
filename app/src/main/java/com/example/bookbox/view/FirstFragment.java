package com.example.bookbox.view;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.example.bookbox.adapter.Adapter;
import com.example.bookbox.R;
import com.example.bookbox.database.LocalDatabase;
import com.example.bookbox.databinding.FragmentFirstBinding;
import com.example.bookbox.entity.Book;
import com.example.bookbox.entity.ReadingHistory;
import com.example.bookbox.utils.AuthUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class FirstFragment extends Fragment {

    private FragmentFirstBinding binding;
    private LocalDatabase database;
    private SharedPreferences sharedPreferences;
    private List<Book> bookList = new ArrayList<>();
    private List<Book> filteredBookList = new ArrayList<>();
    private Adapter adapter;
    private int currentUserId;
    private String currentUserType;
    


    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        binding = FragmentFirstBinding.inflate(inflater, container, false);
        database = LocalDatabase.getDatabase(getContext());
        sharedPreferences = getActivity().getSharedPreferences("BookBoxPrefs", Context.MODE_PRIVATE);
        
        currentUserId = AuthUtils.getCurrentUserId(getContext());
        currentUserType = AuthUtils.getCurrentUserType(getContext());

        if (!AuthUtils.isUserAuthenticated(getContext())) {
            AuthUtils.forceLogoutFromFirstFragment(this);
            return binding.getRoot();
        }

        return binding.getRoot();
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initializeBooks();
        setupSearchAndFilters();
        setupGridView();
        setupButtons();

        PreferencesFragment.applyAccessibilityModeToView(this.getView());
    }

    private void initializeBooks() {
        new Thread(() -> {
            List<Book> existingBooks = database.bookModel().getAllBooks();
            if (existingBooks.isEmpty()) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (isAdded()) {
                            createDefaultBooks();
                        }
                    });
                }
            } else {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (isAdded()) {
                            loadBooksFromDatabase();
                        }
                    });
                }
            }
        }).start();
    }

    private void createDefaultBooks() {
        if (!isAdded() || getContext() == null) return;
        
        String[] titles = getResources().getStringArray(R.array.book_titles);
        String[] descriptions = getResources().getStringArray(R.array.book_descriptions);
        String[] genres = {"Ficção Científica", "Ficção Científica", "Drama", "Drama", "Aventura", 
                          "Romance", "Romance", "Ficção Científica", "Ficção Científica", "Fábula"};
        String[] authors = {"Aldous Huxley", "Ray Bradbury", "J.D. Salinger", "Harper Lee", "William Golding",
                           "Fyodor Dostoevsky", "F. Scott Fitzgerald", "George Orwell", "Margaret Atwood", "George Orwell"};
        int[] years = {1932, 1953, 1951, 1960, 1954, 1866, 1925, 1949, 1985, 1945};
        String[] drawableNames = {"fic1", "fic2", "fic3", "fic4", "fic5", "fic6", "fic7", "fic8", "fic9", "fic10"};

        copyDefaultImagesToStorage(drawableNames);

        new Thread(() -> {
            for (int i = 0; i < titles.length; i++) {
                String imagePath = getDefaultImagePath(drawableNames[i]);
                Book book = new Book(genres[i], titles[i], authors[i], imagePath, descriptions[i], years[i]);
                database.bookModel().insert(book);
            }

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (isAdded()) {
                        loadBooksFromDatabase();
                    }
                });
            }
        }).start();
    }

    private void copyDefaultImagesToStorage(String[] drawableNames) {
        if (!isAdded() || getContext() == null) return;
        
        try {
            File imageDir = new File(getContext().getFilesDir(), "book_images");
            if (!imageDir.exists()) {
                imageDir.mkdirs();
            }

            for (String drawableName : drawableNames) {
                File outputFile = new File(imageDir, drawableName + ".png");
                if (!outputFile.exists()) {
                    int resourceId = getResources().getIdentifier(drawableName, "drawable", getContext().getPackageName());
                    if (resourceId != 0) {
                        android.graphics.drawable.Drawable drawable = getResources().getDrawable(resourceId);
                        android.graphics.Bitmap bitmap = ((android.graphics.drawable.BitmapDrawable) drawable).getBitmap();
                        
                        try (java.io.FileOutputStream out = new java.io.FileOutputStream(outputFile)) {
                            bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out);
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String getDefaultImagePath(String drawableName) {
        if (!isAdded() || getContext() == null) return "";

        return "book_images/" + drawableName + ".png";
    }

    private void loadBooksFromDatabase() {
        new Thread(() -> {
            bookList = database.bookModel().getAllBooks();
            filteredBookList = new ArrayList<>(bookList);
            
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (isAdded() && getContext() != null) {
                        adapter = new Adapter(getContext(), filteredBookList, database, currentUserId);
                        binding.grid1.setAdapter(adapter);
                        setupGenreFilter();
                    }
                });
            }
        }).start();
    }

    private void setupSearchAndFilters() {
        binding.editTextSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterBooks();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        binding.spinnerReadStatus.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                filterBooks();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void setupGenreFilter() {
        new Thread(() -> {
            List<String> genres = database.bookModel().getDistinctGenres();
            genres.add(0, "Todos os gêneros");
            
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (isAdded() && getContext() != null) {
                        ArrayAdapter<String> genreAdapter = new ArrayAdapter<>(getContext(), 
                                android.R.layout.simple_spinner_item, genres);
                        genreAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                        binding.spinnerGenre.setAdapter(genreAdapter);
                        
                        binding.spinnerGenre.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                            @Override
                            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                                filterBooks();
                            }

                            @Override
                            public void onNothingSelected(AdapterView<?> parent) {}
                        });
                    }
                });
            }
        }).start();
    }

    private void filterBooks() {
        String searchQuery = binding.editTextSearch.getText().toString().trim().toLowerCase();
        String selectedGenre = binding.spinnerGenre.getSelectedItem() != null ? 
                binding.spinnerGenre.getSelectedItem().toString() : "Todos os gêneros";
        String selectedStatus = binding.spinnerReadStatus.getSelectedItem() != null ?
                binding.spinnerReadStatus.getSelectedItem().toString() : "Todos";

        new Thread(() -> {
            filteredBookList.clear();

            for (Book book : bookList) {
                boolean matchesSearch = searchQuery.isEmpty() || 
                        book.getTitle().toLowerCase().contains(searchQuery) ||
                        (book.getAuthor() != null && book.getAuthor().toLowerCase().contains(searchQuery));
                
                boolean matchesGenre = selectedGenre.equals("Todos os gêneros") || 
                        book.getGenre().equals(selectedGenre);

                boolean matchesStatus = true;
                if (!selectedStatus.equals("Todos")) {
                    ReadingHistory history = database.readingHistoryModel().getReadingHistoryByBookAndUser(book.getId(), currentUserId);
                    boolean isRead = history != null && history.isRead();
                    
                    matchesStatus = (selectedStatus.equals("Lidos") && isRead) ||
                                   (selectedStatus.equals("Não lidos") && !isRead);
                }

                if (matchesSearch && matchesGenre && matchesStatus) {
                    filteredBookList.add(book);
                }
            }

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (isAdded() && adapter != null) {
                        adapter.refreshReadStatusCache();
                    }
                });
            }
        }).start();
    }

    private void setupGridView() {
        binding.grid1.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Book selectedBook = filteredBookList.get(position);
                Bundle args = new Bundle();
                args.putInt("bookId", selectedBook.getId());
                NavHostFragment.findNavController(FirstFragment.this)
                        .navigate(R.id.action_FirstFragment_to_BookDetailsFragment, args);
            }
        });
    }



    private void setupButtons() {
        if ("admin".equals(currentUserType)) {
            binding.buttonAddBook.setVisibility(View.VISIBLE);
            binding.buttonAddBook.setText("Gerenciar Livros");
            binding.buttonAddBook.setOnClickListener(v -> {
                NavHostFragment.findNavController(FirstFragment.this)
                        .navigate(R.id.action_FirstFragment_to_ManageBooksFragment);
            });
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}