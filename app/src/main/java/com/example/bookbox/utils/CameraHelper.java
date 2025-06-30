package com.example.bookbox.utils;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

public class CameraHelper {
    
    private static final String TAG = "CameraHelper";
    public static final int CAMERA_PERMISSION_CODE = 100;
    
    public interface CameraCallback {
        void onCameraReady(Uri imageUri);
        void onPermissionDenied();
        void onError(String message);
    }
    
    private Fragment fragment;
    private CameraCallback callback;
    private ActivityResultLauncher<Uri> takePictureLauncher;
    private CameraType currentType;
    private int currentUserId;
    
    public CameraHelper(Fragment fragment, ActivityResultLauncher<Uri> takePictureLauncher) {
        this.fragment = fragment;
        this.takePictureLauncher = takePictureLauncher;
    }
    
    public void openCamera(CameraCallback callback, CameraType type, int userId) {
        this.callback = callback;
        this.currentType = type;
        this.currentUserId = userId;
        
        Log.d(TAG, "openCamera chamado - tipo: " + type + ", userId: " + userId);
        
        if (hasPermission()) {
            Log.d(TAG, "Permissão já concedida, abrindo câmera");
            openCameraWithPermission(type, userId);
        } else {
            Log.d(TAG, "Permissão não concedida, solicitando permissão");
            requestPermission();
        }
    }
    
    public void openCamera(CameraCallback callback, CameraType type) {
        openCamera(callback, type, 0);
    }
    
    private boolean hasPermission() {
        boolean hasPermission = ActivityCompat.checkSelfPermission(fragment.getContext(), 
                Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
        Log.d(TAG, "hasPermission: " + hasPermission);
        return hasPermission;
    }
    
    private void requestPermission() {
        Log.d(TAG, "Solicitando permissão de câmera");
        ActivityCompat.requestPermissions(fragment.getActivity(),
                new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_CODE);
    }
    
    private void openCameraWithPermission(CameraType type, int userId) {
        try {
            Log.d(TAG, "Abrindo câmera com permissão - tipo: " + type);
            Uri imageUri;
            switch (type) {
                case PROFILE:
                    imageUri = FileProviderHelper.createProfileImageUri(fragment.getContext(), userId);
                    break;
                case BOOK:
                    imageUri = FileProviderHelper.createBookImageUri(fragment.getContext());
                    break;
                default:
                    imageUri = FileProviderHelper.createTempImageUri(fragment.getContext(), "temp");
                    break;
            }
            
            Log.d(TAG, "URI da imagem criado: " + imageUri);
            takePictureLauncher.launch(imageUri);
            if (callback != null) {
                callback.onCameraReady(imageUri);
            }
        } catch (Exception e) {
            Log.e(TAG, "Erro ao preparar câmera", e);
            if (callback != null) {
                callback.onError("Erro ao preparar câmera: " + e.getMessage());
            }
        }
    }
    
    public void handlePermissionResult(int requestCode, int[] grantResults) {
        Log.d(TAG, "handlePermissionResult - requestCode: " + requestCode + ", CAMERA_PERMISSION_CODE: " + CAMERA_PERMISSION_CODE);
        
        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Permissão de câmera concedida");
                if (fragment.getActivity() != null) {
                    fragment.getActivity().runOnUiThread(() -> {
                        Toast.makeText(fragment.getContext(), "Permissão de câmera concedida", Toast.LENGTH_SHORT).show();
                    });
                }
                openCameraWithPermission(currentType, currentUserId);
            } else {
                Log.d(TAG, "Permissão de câmera negada - executando callback");
                if (fragment.getActivity() != null && callback != null) {
                    fragment.getActivity().runOnUiThread(() -> {
                        callback.onPermissionDenied();
                    });
                }
            }
        } else {
            Log.d(TAG, "Código de request não corresponde ao CAMERA_PERMISSION_CODE");
        }
    }
    
    public enum CameraType {
        PROFILE,
        BOOK,
        TEMP
    }
} 