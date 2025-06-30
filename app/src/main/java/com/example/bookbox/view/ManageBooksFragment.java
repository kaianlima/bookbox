package com.example.bookbox.view;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.bookbox.R;
import com.example.bookbox.adapter.BookManagementAdapter;
import com.example.bookbox.database.LocalDatabase;
import com.example.bookbox.databinding.FragmentManageBooksBinding;
import com.example.bookbox.entity.Book;
import com.example.bookbox.utils.AuthUtils;
import com.example.bookbox.utils.BookImageHelper;
import com.example.bookbox.utils.CameraHelper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class ManageBooksFragment extends Fragment implements BookManagementAdapter.OnBookActionListener, CameraHelper.CameraCallback {

    private FragmentManageBooksBinding binding;
    private LocalDatabase database;
    private BookManagementAdapter adapter;
    private ActivityResultLauncher<Intent> galleryPickerLauncher;
    private ActivityResultLauncher<Uri> takePictureLauncher;
    private String selectedImagePath;
    private Uri tempImageUri;
    private AlertDialog currentEditDialog;
    private CameraHelper cameraHelper;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentManageBooksBinding.inflate(inflater, container, false);
        database = LocalDatabase.getDatabase(getContext());

        if (!AuthUtils.checkAdminAuthenticationInFragment(this)) {
            return binding.getRoot();
        }
        
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setupRecyclerView();
        setupSearch();
        setupButtons();
        setupImagePicker();
        loadBooks();
    }

    private void setupRecyclerView() {
        adapter = new BookManagementAdapter(getContext(), this);
        binding.recyclerViewBooks.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerViewBooks.setAdapter(adapter);
    }

    private void setupSearch() {
        binding.editTextSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.filter(s.toString());
                updateEmptyView();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void setupButtons() {
        binding.buttonAddNew.setOnClickListener(v -> {
            NavHostFragment.findNavController(ManageBooksFragment.this)
                    .navigate(R.id.AddBookFragment);
        });
    }

    private void setupImagePicker() {
        galleryPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri selectedImageUri = result.getData().getData();
                        if (selectedImageUri != null) {
                            try {
                                selectedImagePath = saveImageToInternalStorage(selectedImageUri);
                                updateImagePreview();
                            } catch (Exception e) {
                                Toast.makeText(getContext(), "Erro ao processar imagem", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                }
        );

        takePictureLauncher = registerForActivityResult(
                new ActivityResultContracts.TakePicture(),
                result -> {
                    if (result && tempImageUri != null) {
                        try {
                            selectedImagePath = saveImageFromCamera();
                            updateImagePreview();
                            Toast.makeText(getContext(), "Imagem capturada com sucesso", Toast.LENGTH_SHORT).show();
                        } catch (Exception e) {
                            Toast.makeText(getContext(), "Erro ao processar imagem: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                }
        );
        
        cameraHelper = new CameraHelper(this, takePictureLauncher);
    }

    private void loadBooks() {
        new Thread(() -> {
            List<Book> books = database.bookModel().getAllBooks();
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    adapter.setBooks(books);
                    updateEmptyView();
                });
            }
        }).start();
    }

    private void updateEmptyView() {
        if (adapter.getItemCount() == 0) {
            binding.textViewEmpty.setVisibility(View.VISIBLE);
            binding.recyclerViewBooks.setVisibility(View.GONE);
        } else {
            binding.textViewEmpty.setVisibility(View.GONE);
            binding.recyclerViewBooks.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onEditBook(Book book) {
        showEditBookDialog(book);
    }

    @Override
    public void onDeleteBook(Book book) {
        showDeleteConfirmationDialog(book);
    }

    private void showEditBookDialog(Book book) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_edit_book, null);
        builder.setView(dialogView);

        EditText editTextTitle = dialogView.findViewById(R.id.editTextTitle);
        EditText editTextAuthor = dialogView.findViewById(R.id.editTextAuthor);
        Spinner spinnerGenre = dialogView.findViewById(R.id.spinnerGenre);
        EditText editTextYear = dialogView.findViewById(R.id.editTextYear);
        EditText editTextDescription = dialogView.findViewById(R.id.editTextDescription);
        ImageView imageViewPreview = dialogView.findViewById(R.id.imageViewBookPreview);
        Button buttonSelectImage = dialogView.findViewById(R.id.buttonSelectImage);
        Button buttonUseDefault = dialogView.findViewById(R.id.buttonUseDefault);
        Button buttonSave = dialogView.findViewById(R.id.buttonSave);
        Button buttonCancel = dialogView.findViewById(R.id.buttonCancel);

        String[] genres = {"Ficção", "Romance", "Fantasia", "Mistério", "Biografia", "História", "Ciência", "Terror", "Aventura", "Drama"};
        ArrayAdapter<String> genreAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, genres);
        genreAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerGenre.setAdapter(genreAdapter);

        String currentGenre = book.getGenre();
        if (currentGenre != null && !currentGenre.isEmpty()) {
            for (int i = 0; i < genres.length; i++) {
                if (genres[i].equals(currentGenre)) {
                    spinnerGenre.setSelection(i);
                    break;
                }
            }
        }

        editTextTitle.setText(book.getTitle());
        editTextAuthor.setText(book.getAuthor());
        editTextYear.setText(String.valueOf(book.getPublicationYear()));
        editTextDescription.setText(book.getDescription());
        selectedImagePath = book.getImage();

        loadImageIntoPreview(imageViewPreview, book.getImage());

        currentEditDialog = builder.create();
        currentEditDialog.show();

        buttonSelectImage.setOnClickListener(v -> showImageSourceDialog());
        
        buttonUseDefault.setOnClickListener(v -> {
            selectedImagePath = null;
            updateImagePreview();
        });

        buttonCancel.setOnClickListener(v -> currentEditDialog.dismiss());

        buttonSave.setOnClickListener(v -> {
            String title = editTextTitle.getText().toString().trim();
            String author = editTextAuthor.getText().toString().trim();
            String genre = spinnerGenre.getSelectedItem() != null ? spinnerGenre.getSelectedItem().toString() : "";
            String yearStr = editTextYear.getText().toString().trim();
            String description = editTextDescription.getText().toString().trim();

            if (title.isEmpty() || author.isEmpty() || genre.isEmpty() || yearStr.isEmpty()) {
                Toast.makeText(getContext(), "Preencha todos os campos obrigatórios", Toast.LENGTH_SHORT).show();
                return;
            }

            int year;
            try {
                year = Integer.parseInt(yearStr);
            } catch (NumberFormatException e) {
                Toast.makeText(getContext(), "Ano inválido", Toast.LENGTH_SHORT).show();
                return;
            }

            showSaveConfirmationDialog(() -> {
                book.setTitle(title);
                book.setAuthor(author);
                book.setGenre(genre);
                book.setPublicationYear(year);
                book.setDescription(description);
                book.setImage(selectedImagePath);

                new Thread(() -> {
                    database.bookModel().update(book);
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            Toast.makeText(getContext(), "Livro atualizado com sucesso!", Toast.LENGTH_SHORT).show();
                            currentEditDialog.dismiss();
                            loadBooks();
                        });
                    }
                }).start();
            });
        });
    }

    private void showDeleteConfirmationDialog(Book book) {
        new AlertDialog.Builder(getContext())
                .setTitle("Confirmar Exclusão")
                .setMessage("Tem certeza que deseja excluir o livro \"" + book.getTitle() + "\"?\n\nEsta ação não pode ser desfeita.")
                .setPositiveButton("Excluir", (dialog, which) -> {
                    new Thread(() -> {
                        database.ratingModel().deleteRatingsByBook(book.getId());
                        database.readingHistoryModel().deleteHistoryByBook(book.getId());
                        database.bookModel().delete(book);

                        if (book.getImage() != null && !book.getImage().isEmpty()) {
                            File imageFile = new File(getContext().getFilesDir(), book.getImage());
                            if (imageFile.exists()) {
                                imageFile.delete();
                            }
                        }

                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                Toast.makeText(getContext(), "Livro excluído com sucesso!", Toast.LENGTH_SHORT).show();
                                loadBooks();
                            });
                        }
                    }).start();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void showSaveConfirmationDialog(Runnable onConfirm) {
        new AlertDialog.Builder(getContext())
                .setTitle("Confirmar Alterações")
                .setMessage("Deseja salvar as alterações realizadas no livro?")
                .setPositiveButton("Salvar", (dialog, which) -> onConfirm.run())
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void showImageSourceDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Selecionar imagem")
                .setItems(new String[]{"Câmera", "Galeria"}, (dialog, which) -> {
                    if (which == 0) {
                        openTakePicture();
                    } else {
                        openGalleryPicker();
                    }
                })
                .show();
    }

    private void openGalleryPicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        galleryPickerLauncher.launch(intent);
    }

    private void openTakePicture() {
        cameraHelper.openCamera(this, CameraHelper.CameraType.BOOK);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        android.util.Log.d("ManageBooksFragment", "onRequestPermissionsResult - requestCode: " + requestCode);
        cameraHelper.handlePermissionResult(requestCode, grantResults);
    }

    @Override
    public void onCameraReady(Uri imageUri) {
        tempImageUri = imageUri;
    }

    @Override
    public void onPermissionDenied() {
        android.util.Log.d("ManageBooksFragment", "onPermissionDenied chamado");
        Toast.makeText(getContext(), "Sem permissão de câmera. Use a galeria para selecionar imagens dos livros.", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onError(String message) {
        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
    }

    private String saveImageToInternalStorage(Uri imageUri) throws IOException {
        String fileName = "book_" + System.currentTimeMillis() + ".png";
        File directory = new File(getContext().getFilesDir(), "book_images");
        if (!directory.exists()) {
            directory.mkdirs();
        }
        
        File file = new File(directory, fileName);

        try (InputStream inputStream = getContext().getContentResolver().openInputStream(imageUri)) {
            Bitmap originalBitmap = BitmapFactory.decodeStream(inputStream);
            if (originalBitmap != null) {
                Bitmap resizedBitmap = resizeToStandardSize(originalBitmap, 400, 600);

                try (FileOutputStream outputStream = new FileOutputStream(file)) {
                    resizedBitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
                }

                if (resizedBitmap != originalBitmap) {
                    originalBitmap.recycle();
                }
            } else {
                throw new IOException("Não foi possível decodificar a imagem");
            }
        }
        
        return "book_images/" + fileName;
    }

    private String saveImageFromCamera() throws IOException {
        String fileName = "book_" + System.currentTimeMillis() + ".png";
        File directory = new File(getContext().getFilesDir(), "book_images");
        if (!directory.exists()) {
            directory.mkdirs();
        }
        
        File file = new File(directory, fileName);

        try (InputStream inputStream = getContext().getContentResolver().openInputStream(tempImageUri)) {
            Bitmap originalBitmap = BitmapFactory.decodeStream(inputStream);
            if (originalBitmap != null) {
                Bitmap resizedBitmap = resizeToStandardSize(originalBitmap, 400, 600);

                try (FileOutputStream outputStream = new FileOutputStream(file)) {
                    resizedBitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
                }

                if (resizedBitmap != originalBitmap) {
                    originalBitmap.recycle();
                }
            } else {
                throw new IOException("Não foi possível decodificar a imagem");
            }
        }
        
        return "book_images/" + fileName;
    }

    private Bitmap resizeToStandardSize(Bitmap originalBitmap, int targetWidth, int targetHeight) {
        return Bitmap.createScaledBitmap(originalBitmap, targetWidth, targetHeight, true);
    }

    private void loadImageIntoPreview(ImageView imageView, String imagePath) {
        BookImageHelper.loadBookImage(getContext(), imagePath, imageView);
    }

    private void updateImagePreview() {
        if (currentEditDialog != null) {
            ImageView imageView = currentEditDialog.findViewById(R.id.imageViewBookPreview);
            if (imageView != null) {
                loadImageIntoPreview(imageView, selectedImagePath);
            }
        }
    }

    private Bitmap generateDefaultBookImage() {
        int width = 400;
        int height = 400;
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        canvas.drawColor(0xFFE0E0E0);

        Paint paint = new Paint();
        paint.setColor(0xFF757575);
        paint.setTextSize(48);
        paint.setAntiAlias(true);
        paint.setTextAlign(Paint.Align.CENTER);
        
        String text = "Sem Capa";
        Rect bounds = new Rect();
        paint.getTextBounds(text, 0, text.length(), bounds);
        
        canvas.drawText(text, width / 2f, height / 2f + bounds.height() / 2f, paint);
        
        return bitmap;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadBooks();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}