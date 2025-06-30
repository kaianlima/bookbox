package com.example.bookbox.view;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;

import com.example.bookbox.R;
import com.example.bookbox.database.LocalDatabase;
import com.example.bookbox.databinding.FragmentBookDetailsBinding;
import com.example.bookbox.entity.Book;
import com.example.bookbox.entity.Rating;
import com.example.bookbox.entity.ReadingHistory;
import com.example.bookbox.entity.User;
import com.example.bookbox.utils.AuthUtils;

import java.io.File;
import java.text.DecimalFormat;

public class BookDetailsFragment extends Fragment {

    private FragmentBookDetailsBinding binding;
    private LocalDatabase database;
    private SharedPreferences sharedPreferences;
    private int currentUserId;
    private Book currentBook;
    private int bookId;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentBookDetailsBinding.inflate(inflater, container, false);
        database = LocalDatabase.getDatabase(getContext());
        sharedPreferences = getActivity().getSharedPreferences("BookBoxPrefs", Context.MODE_PRIVATE);

        if (!AuthUtils.checkAuthenticationInFragment(this)) {
            return binding.getRoot();
        }

        currentUserId = AuthUtils.getCurrentUserId(getContext());

        if (getArguments() != null) {
            bookId = getArguments().getInt("bookId", -1);
        }

        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        android.util.Log.d("BookDetails", "onViewCreated - CurrentUserId: " + currentUserId);
        android.util.Log.d("BookDetails", "onViewCreated - BookId: " + bookId);

        setupButtons();
        setupAdminButtons();
        loadBookDetails();
    }

    private void setupButtons() {
        binding.buttonMarkAsRead.setOnClickListener(v -> toggleReadStatus());
        binding.buttonSaveRating.setOnClickListener(v -> saveUserRating());
    }

    private void setupAdminButtons() {
        if (AuthUtils.isUserAdmin(getContext())) {
            binding.adminOptionsCard.setVisibility(View.VISIBLE);
            
            binding.buttonEditBook.setOnClickListener(v -> editBook());
            binding.buttonDeleteBook.setOnClickListener(v -> confirmDeleteBook());
        }
    }

    private void loadBookDetails() {
        currentUserId = AuthUtils.getCurrentUserId(getContext());

        if (bookId <= 0) {
            Toast.makeText(getContext(), "Erro: livro não identificado.", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (currentUserId <= 0) {
            Toast.makeText(getContext(), "Erro: usuário não identificado. Faça login novamente.", Toast.LENGTH_SHORT).show();
            return;
        }
        
        new Thread(() -> {
            currentBook = database.bookModel().getBook(bookId);
            if (currentBook != null && getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (isAdded()) {
                        displayBookInfo();
                        loadBookImage();
                        loadReadingStatus();
                        loadUserRating();
                        loadAverageRating();
                    }
                });
            } else {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (isAdded()) {
                            Toast.makeText(getContext(), "Erro: livro não encontrado.", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        }).start();
    }

    private void displayBookInfo() {
        binding.textViewTitle.setText(currentBook.getTitle());
        binding.textViewAuthor.setText(currentBook.getAuthor() != null ? currentBook.getAuthor() : "Autor desconhecido");
        binding.textViewGenre.setText(currentBook.getGenre());
        binding.textViewYear.setText(currentBook.getPublicationYear() > 0 ? String.valueOf(currentBook.getPublicationYear()) : "Ano não informado");
        binding.textViewDescription.setText(currentBook.getDescription() != null ? currentBook.getDescription() : "Descrição não disponível");
    }

    private void loadBookImage() {
        String imagePath = currentBook.getImage();
        if (imagePath != null && !imagePath.isEmpty()) {
            File imageFile = new File(getContext().getFilesDir(), imagePath);
            if (imageFile.exists()) {
                try {
                    Bitmap bitmap = BitmapFactory.decodeFile(imageFile.getAbsolutePath());
                    if (bitmap != null) {
                        binding.imageViewBook.setImageBitmap(bitmap);
                        return;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        binding.imageViewBook.setImageResource(R.drawable.baseline_menu_book_24);
    }

    private void loadReadingStatus() {
        new Thread(() -> {
            ReadingHistory history = database.readingHistoryModel().getReadingHistoryByBookAndUser(bookId, currentUserId);
            boolean isRead = history != null && history.isRead();
            
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (isAdded()) {
                        binding.buttonMarkAsRead.setText(isRead ? "Marcar como não lido" : "Marcar como lido");
                    }
                });
            }
        }).start();
    }

    private void loadUserRating() {
        new Thread(() -> {
            Rating userRating = database.ratingModel().getRatingByBookIdAndUserId(bookId, currentUserId);
            
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (isAdded()) {
                        if (userRating != null) {
                            binding.ratingBarUser.setRating(userRating.getStars());
                        } else {
                            binding.ratingBarUser.setRating(0);
                        }
                    }
                });
            }
        }).start();
    }

    private void loadAverageRating() {
        new Thread(() -> {
            Double averageRating = database.ratingModel().getAverageRatingForBook(bookId);
            int ratingCount = database.ratingModel().getRatingCountForBook(bookId);
            
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (isAdded()) {
                        binding.ratingOverlay.setVisibility(View.VISIBLE);
                        
                        if (averageRating != null && averageRating > 0) {
                            DecimalFormat df = new DecimalFormat("#.#");
                            binding.ratingBarAverage.setRating(averageRating.floatValue());
                            binding.textViewAverageRating.setText(df.format(averageRating));
                            binding.textViewRatingCount.setText("(" + ratingCount + " avaliações)");
                        } else {
                            binding.ratingBarAverage.setRating(0);
                            binding.textViewAverageRating.setText("0.0");
                            binding.textViewRatingCount.setText("(0 avaliações)");
                        }
                    }
                });
            }
        }).start();
    }

    private void toggleReadStatus() {
        currentUserId = AuthUtils.getCurrentUserId(getContext());

        if (currentUserId <= 0) {
            Toast.makeText(getContext(), "Erro: usuário não identificado. Faça login novamente.", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (bookId <= 0) {
            Toast.makeText(getContext(), "Erro: livro não identificado.", Toast.LENGTH_SHORT).show();
            return;
        }
        
        new Thread(() -> {
            try {
                User user = database.userModel().getUser(currentUserId);
                Book book = database.bookModel().getBook(bookId);
                
                if (user == null) {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            if (isAdded()) {
                                Toast.makeText(getContext(), "Erro: usuário não encontrado. Faça login novamente.", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                    return;
                }
                
                if (book == null) {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            if (isAdded()) {
                                Toast.makeText(getContext(), "Erro: livro não encontrado.", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                    return;
                }
                
                ReadingHistory history = database.readingHistoryModel().getReadingHistoryByBookAndUser(bookId, currentUserId);
                boolean newStatus;
                
                if (history == null) {
                    history = new ReadingHistory(currentUserId, bookId, true, System.currentTimeMillis());
                    database.readingHistoryModel().insert(history);
                    newStatus = true;
                } else {
                    newStatus = !history.isRead();
                    history.setRead(newStatus);
                    history.setReadDate(System.currentTimeMillis());
                    database.readingHistoryModel().update(history);
                }
                
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (isAdded()) {
                            binding.buttonMarkAsRead.setText(newStatus ? "Marcar como não lido" : "Marcar como lido");
                            String message = newStatus ? "Livro marcado como lido!" : "Livro marcado como não lido!";
                            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
                        }
                    });
                }
                
            } catch (Exception e) {
                e.printStackTrace();
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (isAdded()) {
                            Toast.makeText(getContext(), "Erro ao alterar status de leitura: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        }).start();
    }

    private void saveUserRating() {
        float userRating = binding.ratingBarUser.getRating();
        
        if (userRating == 0) {
            Toast.makeText(getContext(), "Selecione uma avaliação antes de salvar", Toast.LENGTH_SHORT).show();
            return;
        }

        currentUserId = AuthUtils.getCurrentUserId(getContext());
        
        android.util.Log.d("BookDetails", "saveUserRating - CurrentUserId obtido: " + currentUserId);

        if (currentUserId <= 0) {
            Toast.makeText(getContext(), "Erro: usuário não identificado (ID: " + currentUserId + "). Faça login novamente.", Toast.LENGTH_LONG).show();
            return;
        }
        
        if (bookId <= 0) {
            Toast.makeText(getContext(), "Erro: livro não identificado.", Toast.LENGTH_SHORT).show();
            return;
        }
        
        new Thread(() -> {
            try {
                android.util.Log.d("BookDetails", "Tentando salvar avaliação. CurrentUserId: " + currentUserId + ", BookId: " + bookId);

                User user = database.userModel().getUser(currentUserId);
                Book book = database.bookModel().getBook(bookId);

                android.util.Log.d("BookDetails", "Usuário encontrado no banco: " + (user != null ? user.getUsername() + " (ID: " + user.getId() + ")" : "NULL"));
                
                if (user == null) {
                    java.util.List<User> allUsers = database.userModel().getAllUsers();
                    android.util.Log.d("BookDetails", "Total de usuários no banco: " + allUsers.size());
                    for (User u : allUsers) {
                        android.util.Log.d("BookDetails", "Usuário no banco: ID=" + u.getId() + ", Username=" + u.getUsername());
                    }
                    
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            if (isAdded()) {
                                Toast.makeText(getContext(), "Erro: usuário não encontrado no banco (ID: " + currentUserId + "). Faça login novamente.", Toast.LENGTH_LONG).show();
                            }
                        });
                    }
                    return;
                }
                
                if (book == null) {
                    android.util.Log.d("BookDetails", "Livro não encontrado no banco: ID=" + bookId);
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            if (isAdded()) {
                                Toast.makeText(getContext(), "Erro: livro não encontrado.", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                    return;
                }
                
                android.util.Log.d("BookDetails", "Verificando avaliação existente...");
                Rating existingRating = database.ratingModel().getRatingByBookIdAndUserId(bookId, currentUserId);
                
                if (existingRating == null) {
                    Rating newRating = new Rating((int) userRating, bookId, currentUserId);
                    database.ratingModel().insert(newRating);
                } else {
                    existingRating.setStars((int) userRating);
                    database.ratingModel().update(existingRating);
                }
                
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (isAdded()) {
                            Toast.makeText(getContext(), "Avaliação salva com sucesso!", Toast.LENGTH_SHORT).show();
                            loadAverageRating();
                        }
                    });
                }
                
            } catch (Exception e) {
                e.printStackTrace();
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (isAdded()) {
                            Toast.makeText(getContext(), "Erro ao salvar avaliação: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        }).start();
    }

    private void editBook() {
        if (currentBook == null) {
            Toast.makeText(getContext(), "Erro: livro não carregado", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(getContext(), "Para editar este livro, acesse o menu Gerenciar Livros no perfil", Toast.LENGTH_LONG).show();
    }

    private void confirmDeleteBook() {
        if (currentBook == null) {
            Toast.makeText(getContext(), "Erro: livro não carregado", Toast.LENGTH_SHORT).show();
            return;
        }
        
        new AlertDialog.Builder(getContext())
                .setTitle("Confirmar Exclusão")
                .setMessage("Tem certeza que deseja excluir o livro \"" + currentBook.getTitle() + "\"?\n\nEsta ação irá:\n• Remover o livro permanentemente\n• Excluir todas as avaliações\n• Remover do histórico de leitura")
                .setPositiveButton("Excluir", (dialog, which) -> deleteBook())
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void deleteBook() {
        if (currentBook == null) return;
        
        new Thread(() -> {
            try {
                database.ratingModel().deleteRatingsByBook(bookId);
                database.readingHistoryModel().deleteHistoryByBook(bookId);
                database.bookModel().delete(currentBook);
                
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (isAdded()) {
                            Toast.makeText(getContext(), "Livro excluído com sucesso", Toast.LENGTH_SHORT).show();
                            Navigation.findNavController(requireView()).popBackStack();
                        }
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (isAdded()) {
                            Toast.makeText(getContext(), "Erro ao excluir livro: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        }).start();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
} 