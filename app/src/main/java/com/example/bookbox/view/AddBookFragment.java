package com.example.bookbox.view;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Toast;
import android.app.AlertDialog;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.example.bookbox.R;
import com.example.bookbox.database.LocalDatabase;
import com.example.bookbox.databinding.FragmentAddBookBinding;
import com.example.bookbox.entity.Book;
import com.example.bookbox.utils.AuthUtils;
import com.example.bookbox.utils.CameraHelper;
import com.example.bookbox.utils.NotificationHelper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class AddBookFragment extends Fragment implements CameraHelper.CameraCallback {

    private FragmentAddBookBinding binding;
    private LocalDatabase database;
    private SharedPreferences sharedPreferences;
    private String currentUserType;
    private String selectedImagePath;
    private ActivityResultLauncher<Intent> galleryPickerLauncher;
    private ActivityResultLauncher<Uri> takePictureLauncher;
    private Uri tempImageUri;
    private CameraHelper cameraHelper;

    private static final int NOTIFICATION_PERMISSION_CODE = 200;
    private Book pendingBookToSave = null;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        binding = FragmentAddBookBinding.inflate(inflater, container, false);
        database = LocalDatabase.getDatabase(getContext());
        sharedPreferences = getActivity().getSharedPreferences("BookBoxPrefs", Context.MODE_PRIVATE);
        currentUserType = sharedPreferences.getString("currentUserType", "comum");

        if (!AuthUtils.checkAdminAuthenticationInFragment(this)) {
            return binding.getRoot();
        }

        galleryPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == getActivity().RESULT_OK && result.getData() != null) {
                        Uri imageUri = result.getData().getData();
                        if (imageUri != null) {
                            handleSelectedImage(imageUri);
                        }
                    }
                }
        );

        takePictureLauncher = registerForActivityResult(
                new ActivityResultContracts.TakePicture(),
                result -> {
                    if (result && tempImageUri != null) {
                        handleSelectedImage(tempImageUri);
                    }
                }
        );

        cameraHelper = new CameraHelper(this, takePictureLauncher);
        
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setupGenreSpinner();
        setupButtons();
        setupImageSelection();
    }

    private void setupGenreSpinner() {
        String[] genres = getResources().getStringArray(R.array.genre_options);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_item, genres);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerGenre.setAdapter(adapter);
    }

    private void setupButtons() {
        binding.buttonSave.setOnClickListener(v -> saveBook());
        binding.buttonCancel.setOnClickListener(v -> 
                NavHostFragment.findNavController(AddBookFragment.this).navigateUp());
        binding.buttonSelectImage.setOnClickListener(v -> showImageSourceDialog());
        binding.buttonRemoveImage.setOnClickListener(v -> useDefaultImage());
    }

    private void setupImageSelection() {
        binding.buttonSelectImage.setOnClickListener(v -> showImageSourceDialog());
        binding.imageViewBookPreview.setOnClickListener(v -> showImageSourceDialog());
        binding.buttonRemoveImage.setOnClickListener(v -> useDefaultImage());

        showDefaultImagePreview();
    }
    
    private void useDefaultImage() {
        selectedImagePath = null;
        showDefaultImagePreview();
        Toast.makeText(getContext(), "Usando imagem padrão", Toast.LENGTH_SHORT).show();
    }
    
    private void showDefaultImagePreview() {
        try {
            String defaultImagePath = getDefaultBookImagePath();
            if (!defaultImagePath.isEmpty()) {
                File imageFile = new File(getContext().getFilesDir(), defaultImagePath);
                displayImagePreview(imageFile.getAbsolutePath());
                binding.textViewImagePlaceholder.setVisibility(View.GONE);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showImageSourceDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Selecionar imagem")
                .setItems(new String[]{"Câmera", "Galeria"}, (dialog, which) -> {
                    if (which == 0) {
                        openCamera();
                    } else {
                        openGallery();
                    }
                })
                .show();
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        
        try {
            galleryPickerLauncher.launch(Intent.createChooser(intent, "Selecionar imagem"));
        } catch (Exception e) {
            Toast.makeText(getContext(), "Erro ao abrir galeria", Toast.LENGTH_SHORT).show();
        }
    }

    private void openCamera() {
        cameraHelper.openCamera(this, CameraHelper.CameraType.BOOK);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        android.util.Log.d("AddBookFragment", "onRequestPermissionsResult - requestCode: " + requestCode);
        
        if (requestCode == NOTIFICATION_PERMISSION_CODE) {
            android.util.Log.d("AddBookFragment", "Permissão de notificação processada");
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (pendingBookToSave != null) {
                    createBookInDatabase(pendingBookToSave);
                    pendingBookToSave = null;
                }
            } else {
                if (pendingBookToSave != null) {
                    createBookInDatabase(pendingBookToSave);
                    pendingBookToSave = null;
                    Toast.makeText(getContext(), "Livro criado, mas notificações foram desabilitadas", Toast.LENGTH_LONG).show();
                }
            }
        } else {
            android.util.Log.d("AddBookFragment", "Delegando para CameraHelper - requestCode: " + requestCode);
            cameraHelper.handlePermissionResult(requestCode, grantResults);
        }
    }

    @Override
    public void onCameraReady(Uri imageUri) {
        tempImageUri = imageUri;
    }

    @Override
    public void onPermissionDenied() {
        android.util.Log.d("AddBookFragment", "onPermissionDenied chamado");
        Toast.makeText(getContext(), "Sem permissão de câmera. Use a galeria para selecionar imagens.", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onError(String message) {
        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
    }

    private void handleSelectedImage(Uri imageUri) {
        try {
            String mimeType = getActivity().getContentResolver().getType(imageUri);
            if (mimeType != null && !mimeType.startsWith("image/")) {
                Toast.makeText(getContext(), "Por favor, selecione uma imagem válida", Toast.LENGTH_SHORT).show();
                return;
            }

            File imageDir = new File(getContext().getFilesDir(), "book_images");
            if (!imageDir.exists()) {
                imageDir.mkdirs();
            }

            String fileName = "book_" + System.currentTimeMillis() + ".png";
            File destinationFile = new File(imageDir, fileName);

            try (InputStream inputStream = getActivity().getContentResolver().openInputStream(imageUri)) {
                Bitmap originalBitmap = BitmapFactory.decodeStream(inputStream);
                if (originalBitmap != null) {
                    Bitmap resizedBitmap = resizeToStandardSize(originalBitmap, 400, 600);

                    try (FileOutputStream outputStream = new FileOutputStream(destinationFile)) {
                        resizedBitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
                    }

                    if (resizedBitmap != originalBitmap) {
                        originalBitmap.recycle();
                    }
                } else {
                    throw new IOException("Não foi possível decodificar a imagem");
                }
            }

            selectedImagePath = "book_images/" + fileName;

            displayImagePreview(destinationFile.getAbsolutePath());

            Toast.makeText(getContext(), "Imagem selecionada com sucesso", Toast.LENGTH_SHORT).show();

        } catch (IOException e) {
            Toast.makeText(getContext(), "Erro ao processar imagem: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private Bitmap resizeToStandardSize(Bitmap originalBitmap, int targetWidth, int targetHeight) {
        return Bitmap.createScaledBitmap(originalBitmap, targetWidth, targetHeight, true);
    }

    private void displayImagePreview(String imagePath) {
        try {
            Bitmap bitmap = BitmapFactory.decodeFile(imagePath);
            if (bitmap != null) {
                binding.imageViewBookPreview.setImageBitmap(bitmap);
                binding.textViewImagePlaceholder.setVisibility(View.GONE);
            }
        } catch (Exception e) {
            Toast.makeText(getContext(), "Erro ao exibir preview da imagem", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveBook() {
        String title = binding.editTextTitle.getText().toString().trim();
        String author = binding.editTextAuthor.getText().toString().trim();
        String description = binding.editTextDescription.getText().toString().trim();
        String genre = binding.spinnerGenre.getSelectedItem().toString();
        String yearStr = binding.editTextYear.getText().toString().trim();

        if (title.isEmpty()) {
            binding.editTextTitle.setError("Título é obrigatório");
            return;
        }

        if (author.isEmpty()) {
            binding.editTextAuthor.setError("Autor é obrigatório");
            return;
        }

        if (description.isEmpty()) {
            binding.editTextDescription.setError("Descrição é obrigatória");
            return;
        }

        if (selectedImagePath == null || selectedImagePath.isEmpty()) {
            selectedImagePath = getDefaultBookImagePath();
        }

        final int year;
        if (!yearStr.isEmpty()) {
            try {
                int tempYear = Integer.parseInt(yearStr);
                year = tempYear;
            } catch (NumberFormatException e) {
                binding.editTextYear.setError("Ano deve ser um número");
                return;
            }
        } else {
            year = 0;
        }

        final Book newBook = new Book(genre, title, author, selectedImagePath, description, year);

        if (!NotificationHelper.hasNotificationPermission(getContext())) {
            pendingBookToSave = newBook;
            NotificationHelper.requestNotificationPermission(this, NOTIFICATION_PERMISSION_CODE);
            return;
        }

        createBookInDatabase(newBook);
    }
    
    private void createBookInDatabase(Book newBook) {
        new Thread(() -> {
            Book existingBook = database.bookModel().getBookByTitle(newBook.getTitle());
            if (existingBook != null) {
                getActivity().runOnUiThread(() -> {
                    if (binding != null && getContext() != null) {
                        binding.editTextTitle.setError("Já existe um livro com este título");
                    }
                });
                return;
            }

            database.bookModel().insert(newBook);

            getActivity().runOnUiThread(() -> {
                if (binding != null && getContext() != null) {
                    NotificationHelper notificationHelper = new NotificationHelper(getContext());
                    notificationHelper.sendNewBookNotification(newBook.getTitle(), newBook.getAuthor(), newBook.getGenre());
                    
                    Toast.makeText(getContext(), "Livro adicionado com sucesso!", Toast.LENGTH_SHORT).show();
                    NavHostFragment.findNavController(AddBookFragment.this).navigateUp();
                }
            });
        }).start();
    }

    private String getDefaultBookImagePath() {
        try {
            File imageDir = new File(getContext().getFilesDir(), "book_images");
            if (!imageDir.exists()) {
                imageDir.mkdirs();
            }

            File defaultImageFile = new File(imageDir, "default_book_cover.png");

            if (!defaultImageFile.exists()) {
                createDefaultBookImage(defaultImageFile);
            }

            return "book_images/default_book_cover.png";
            
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    private void createDefaultBookImage(File outputFile) {
        try {
            android.graphics.drawable.Drawable drawable = getResources().getDrawable(R.drawable.baseline_menu_book_24);

            int size = 400;
            android.graphics.Bitmap bitmap = android.graphics.Bitmap.createBitmap(size, size, android.graphics.Bitmap.Config.ARGB_8888);
            android.graphics.Canvas canvas = new android.graphics.Canvas(bitmap);

            android.graphics.Paint backgroundPaint = new android.graphics.Paint();
            backgroundPaint.setColor(0xFFE0E0E0);
            canvas.drawRect(0, 0, size, size, backgroundPaint);

            int iconSize = size / 3;
            int iconLeft = (size - iconSize) / 2;
            int iconTop = (size - iconSize) / 2;
            
            drawable.setBounds(iconLeft, iconTop, iconLeft + iconSize, iconTop + iconSize);
            drawable.setTint(0xFF757575);
            drawable.draw(canvas);

            android.graphics.Paint textPaint = new android.graphics.Paint();
            textPaint.setColor(0xFF757575);
            textPaint.setTextSize(32);
            textPaint.setAntiAlias(true);
            textPaint.setTextAlign(android.graphics.Paint.Align.CENTER);
            
            String text = "Sem Capa";
            canvas.drawText(text, size / 2f, size - 50, textPaint);

            try (java.io.FileOutputStream out = new java.io.FileOutputStream(outputFile)) {
                bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out);
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
} 