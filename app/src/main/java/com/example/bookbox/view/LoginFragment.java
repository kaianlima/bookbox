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
import com.example.bookbox.entity.User;
import com.example.bookbox.databinding.FragmentLoginBinding;
import com.example.bookbox.utils.AuthUtils;
import com.example.bookbox.utils.CameraHelper;
import com.example.bookbox.utils.ProfileImageHelper;

import org.mindrot.jbcrypt.BCrypt;

public class LoginFragment extends Fragment implements CameraHelper.CameraCallback {

    private FragmentLoginBinding binding;
    private LocalDatabase database;
    private SharedPreferences sharedPreferences;
    private String selectedProfileImagePath;
    private ActivityResultLauncher<Uri> takePictureLauncher;
    private CameraHelper cameraHelper;
    private Uri tempImageUri;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        binding = FragmentLoginBinding.inflate(inflater, container, false);
        database = LocalDatabase.getDatabase(getContext());
        sharedPreferences = getActivity().getSharedPreferences("BookBoxPrefs", Context.MODE_PRIVATE);
        
        setupImageLaunchers();
        
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        int currentUserId = sharedPreferences.getInt("currentUserId", -1);
        if (currentUserId != -1) {
            navigateToMain();
            return;
        }

        createDefaultAdminUser(() -> {
            binding.buttonLogin.setOnClickListener(v -> performLogin());
            binding.buttonRegister.setOnClickListener(v -> performRegister());
            binding.linkToRegister.setOnClickListener(v -> toggleLoginRegister());
            binding.linkToLogin.setOnClickListener(v -> toggleLoginRegister());
            binding.buttonSelectProfileImage.setOnClickListener(v -> selectProfileImage());
        });
    }

    private void performLogin() {
        String username = binding.editTextUsername.getText().toString().trim();
        String password = binding.editTextPassword.getText().toString();

        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(getContext(), "Por favor, preencha todos os campos", Toast.LENGTH_SHORT).show();
            return;
        }

        new Thread(() -> {
            User user = database.userModel().getUserByName(username);
            
            if (user != null && BCrypt.checkpw(password, user.getPassword())) {
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putInt("currentUserId", user.getId());
                editor.putString("currentUsername", user.getUsername());
                editor.putString("currentUserType", user.getType());
                editor.apply();
                
                getActivity().runOnUiThread(() -> {
                    Toast.makeText(getContext(), "Login realizado com sucesso!", Toast.LENGTH_SHORT).show();
                    navigateToMain();
                });
            } else {
                getActivity().runOnUiThread(() -> {
                    Toast.makeText(getContext(), "Usuário ou senha incorretos", Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private void performRegister() {
        String username = binding.editTextUsernameRegister.getText().toString().trim();
        String password = binding.editTextPasswordRegister.getText().toString();
        String confirmPassword = binding.editTextConfirmPassword.getText().toString();

        if (username.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(getContext(), "Por favor, preencha todos os campos", Toast.LENGTH_SHORT).show();
            return;
        }

        if (username.length() < 3) {
            Toast.makeText(getContext(), "Nome de usuário deve ter pelo menos 3 caracteres", Toast.LENGTH_SHORT).show();
            return;
        }

        if (password.length() < 6) {
            Toast.makeText(getContext(), "Senha deve ter pelo menos 6 caracteres", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!password.equals(confirmPassword)) {
            Toast.makeText(getContext(), "As senhas não coincidem", Toast.LENGTH_SHORT).show();
            return;
        }

        new Thread(() -> {
            User existingUser = database.userModel().getUserByName(username);
            if (existingUser != null) {
                getActivity().runOnUiThread(() -> {
                    Toast.makeText(getContext(), "Nome de usuário já existe", Toast.LENGTH_SHORT).show();
                });
                return;
            }

            String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());

            User newUser = new User(username, hashedPassword, "comum");
            if (selectedProfileImagePath != null) {
                newUser.setProfileImage(selectedProfileImagePath);
            }

            database.userModel().insert(newUser);

            getActivity().runOnUiThread(() -> {
                Toast.makeText(getContext(), "Usuário cadastrado com sucesso! Faça login.", Toast.LENGTH_SHORT).show();
                clearRegisterForm();
                toggleLoginRegister();
            });
        }).start();
    }

    private void toggleLoginRegister() {
        if (binding.loginContainer.getVisibility() == View.VISIBLE) {
            clearLoginForm();
            binding.loginContainer.setVisibility(View.GONE);
            binding.registerContainer.setVisibility(View.VISIBLE);
        } else {
            clearRegisterForm();
            binding.registerContainer.setVisibility(View.GONE);
            binding.loginContainer.setVisibility(View.VISIBLE);
        }
    }

    private void clearLoginForm() {
        binding.editTextUsername.setText("");
        binding.editTextPassword.setText("");
    }

    private void createDefaultAdminUser(Runnable onComplete) {
        new Thread(() -> {
            try {
                User adminUser = database.userModel().getUserByName("admin");
                if (adminUser == null) {
                    String hashedPassword = BCrypt.hashpw("admin123", BCrypt.gensalt());
                    User admin = new User("admin", hashedPassword, "admin");
                    
                    database.userModel().insert(admin);
                    android.util.Log.d("Login", "Admin user created successfully");
                }

                if (getActivity() != null) {
                    getActivity().runOnUiThread(onComplete);
                }
            } catch (Exception e) {
                android.util.Log.e("Login", "Erro ao criar admin: " + e.getMessage());
                e.printStackTrace();
                if (getActivity() != null) {
                    getActivity().runOnUiThread(onComplete);
                }
            }
        }).start();
    }

    private void setupImageLaunchers() {
        takePictureLauncher = registerForActivityResult(
                new ActivityResultContracts.TakePicture(),
                result -> {
                    if (result) {
                        handleCapturedProfileImage();
                    }
                }
        );
        
        cameraHelper = new CameraHelper(this, takePictureLauncher);
    }

    private void selectProfileImage() {
        cameraHelper.openCamera(this, CameraHelper.CameraType.PROFILE, 0);
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
        android.util.Log.d("LoginFragment", "onPermissionDenied chamado");
        Toast.makeText(getContext(), "Sem permissão de câmera. Não será possível adicionar foto de perfil.", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onError(String message) {
        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
    }

    private void handleCapturedProfileImage() {
        try {
            selectedProfileImagePath = ProfileImageHelper.saveProfileImageFromUri(getContext(), tempImageUri, 0);

            ProfileImageHelper.loadProfileImage(getContext(), selectedProfileImagePath, binding.imageViewProfilePreview);
            
            Toast.makeText(getContext(), "Foto de perfil capturada", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(getContext(), "Erro ao processar foto: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void navigateToMain() {
        NavHostFragment.findNavController(LoginFragment.this)
                .navigate(R.id.action_LoginFragment_to_FirstFragment);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void clearRegisterForm() {
        binding.editTextUsernameRegister.setText("");
        binding.editTextPasswordRegister.setText("");
        binding.editTextConfirmPassword.setText("");

        binding.imageViewProfilePreview.setImageResource(R.drawable.avatar);
        selectedProfileImagePath = null;
    }
} 