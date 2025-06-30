package com.example.bookbox.view;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.example.bookbox.R;
import com.example.bookbox.database.LocalDatabase;
import com.example.bookbox.databinding.FragmentProfileBinding;
import com.example.bookbox.entity.ReadingHistory;
import com.example.bookbox.entity.User;
import com.example.bookbox.utils.AuthUtils;
import com.example.bookbox.utils.CameraHelper;
import com.example.bookbox.utils.ProfileImageHelper;

import java.util.List;

public class ProfileFragment extends Fragment implements CameraHelper.CameraCallback {

    private FragmentProfileBinding binding;
    private LocalDatabase database;
    private SharedPreferences sharedPreferences;
    private int currentUserId;
    private String currentUsername;
    private String currentUserType;
    private ActivityResultLauncher<Uri> takePictureLauncher;
    private CameraHelper cameraHelper;
    private Uri tempImageUri;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        binding = FragmentProfileBinding.inflate(inflater, container, false);
        database = LocalDatabase.getDatabase(getContext());
        sharedPreferences = getActivity().getSharedPreferences("BookBoxPrefs", Context.MODE_PRIVATE);

        if (!AuthUtils.checkAuthenticationInFragment(this)) {
            return binding.getRoot();
        }
        
        currentUserId = AuthUtils.getCurrentUserId(getContext());
        currentUsername = AuthUtils.getCurrentUsername(getContext());
        currentUserType = AuthUtils.getCurrentUserType(getContext());
        
        setupImageLaunchers();
        
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setupUserInfo();
        setupButtons();
        loadReadingStats();

        PreferencesFragment.applyAccessibilityModeToView(this.getView());
    }

    private void setupUserInfo() {
        binding.textViewUsername.setText(currentUsername);

        if ("admin".equals(currentUserType)) {
            binding.textViewUserType.setText("Tipo: " + currentUserType);
            binding.textViewUserType.setVisibility(View.VISIBLE);
            binding.adminOptionsContainer.setVisibility(View.VISIBLE);
        } else {
            binding.textViewUserType.setVisibility(View.GONE);
            binding.adminOptionsContainer.setVisibility(View.GONE);
        }

        new Thread(() -> {
            User currentUser = database.userModel().getUser(currentUserId);
            if (currentUser != null && currentUser.getProfileImage() != null) {
                getActivity().runOnUiThread(() -> {
                    if (binding != null && getContext() != null) {
                        ProfileImageHelper.loadProfileImage(getContext(), 
                                currentUser.getProfileImage(), binding.imageViewUserProfile);
                    }
                });
            }
        }).start();
    }

    private void setupButtons() {
        binding.preferencesItem.setOnClickListener(v -> {
            NavHostFragment.findNavController(ProfileFragment.this)
                    .navigate(R.id.PreferencesFragment);
        });

        binding.aboutItem.setOnClickListener(v -> {
            NavHostFragment.findNavController(ProfileFragment.this)
                    .navigate(R.id.AboutFragment);
        });

        binding.logoutItem.setOnClickListener(v -> {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.clear();
            editor.apply();

            NavHostFragment.findNavController(ProfileFragment.this)
                    .navigate(R.id.action_ProfileFragment_to_LoginFragment);
        });

        binding.buttonChangeProfileImage.setOnClickListener(v -> selectProfileImage());
        binding.imageViewUserProfile.setOnClickListener(v -> selectProfileImage());

        if ("admin".equals(currentUserType)) {
            binding.manageBooksItem.setOnClickListener(v -> {
                NavHostFragment.findNavController(ProfileFragment.this)
                        .navigate(R.id.action_ProfileFragment_to_ManageBooksFragment);
            });
        }
    }

    private void loadReadingStats() {
        new Thread(() -> {
            int totalRead = database.readingHistoryModel().getReadBooksCountByUser(currentUserId, true);
            int totalNotRead = database.readingHistoryModel().getReadBooksCountByUser(currentUserId, false);
            int totalBooks = database.bookModel().getAllBooks().size();
            
            getActivity().runOnUiThread(() -> {
                if (binding != null && getContext() != null) {
                    binding.textViewBooksRead.setText("Livros lidos: " + totalRead);
                    binding.textViewBooksTotal.setText("Total de livros: " + totalBooks);
                }
            });
        }).start();
    }

    private void setupImageLaunchers() {
        takePictureLauncher = registerForActivityResult(
                new ActivityResultContracts.TakePicture(),
                result -> {
                    if (result && tempImageUri != null) {
                        handleCapturedProfileImage();
                    }
                }
        );
        
        cameraHelper = new CameraHelper(this, takePictureLauncher);
    }

    private void selectProfileImage() {
        cameraHelper.openCamera(this, CameraHelper.CameraType.PROFILE, currentUserId);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        cameraHelper.handlePermissionResult(requestCode, grantResults);
    }

    @Override
    public void onCameraReady(Uri imageUri) {
        tempImageUri = imageUri;
    }

    @Override
    public void onPermissionDenied() {
        android.util.Log.d("ProfileFragment", "onPermissionDenied chamado");
        Toast.makeText(getContext(), "Sem permissão de câmera. Não é possível alterar foto do perfil.", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onError(String message) {
        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
    }

    private void handleCapturedProfileImage() {
        try {
            android.util.Log.d("ProfileFragment", "Handling captured image, tempImageUri: " + tempImageUri);
            String newImagePath = ProfileImageHelper.saveProfileImageFromUri(getContext(), tempImageUri, currentUserId);
            android.util.Log.d("ProfileFragment", "Image saved to path: " + newImagePath);

            new Thread(() -> {
                User currentUser = database.userModel().getUser(currentUserId);
                if (currentUser != null) {
                    android.util.Log.d("ProfileFragment", "Current user found, old image: " + currentUser.getProfileImage());

                    if (currentUser.getProfileImage() != null) {
                        ProfileImageHelper.deleteProfileImage(getContext(), currentUser.getProfileImage());
                    }

                    currentUser.setProfileImage(newImagePath);
                    database.userModel().update(currentUser);
                    android.util.Log.d("ProfileFragment", "User updated with new image: " + newImagePath);
                    
                    getActivity().runOnUiThread(() -> {
                        if (binding != null && getContext() != null) {
                            android.util.Log.d("ProfileFragment", "Updating ImageView on UI thread");
                            ProfileImageHelper.loadProfileImage(getContext(), newImagePath, binding.imageViewUserProfile);
                            Toast.makeText(getContext(), "Foto de perfil atualizada", Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    android.util.Log.e("ProfileFragment", "Current user not found in database");
                }
            }).start();
            
        } catch (Exception e) {
            android.util.Log.e("ProfileFragment", "Error handling captured image", e);
            Toast.makeText(getContext(), "Erro ao processar foto: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
} 