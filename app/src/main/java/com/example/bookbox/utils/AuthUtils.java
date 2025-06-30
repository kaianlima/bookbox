package com.example.bookbox.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.example.bookbox.R;

public class AuthUtils {
    
    private static final String PREFS_NAME = "BookBoxPrefs";
    private static final String KEY_USER_ID = "currentUserId";
    private static final String KEY_USERNAME = "currentUsername";
    private static final String KEY_USER_TYPE = "currentUserType";
    
    /**
     * Verifica se o usuário está autenticado (logado)
     */
    public static boolean isUserAuthenticated(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        int userId = prefs.getInt(KEY_USER_ID, -1);
        return userId > 0;
    }
    
    /**
     * Verifica se o usuário é administrador
     */
    public static boolean isUserAdmin(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String userType = prefs.getString(KEY_USER_TYPE, "comum");
        return "admin".equals(userType) && isUserAuthenticated(context);
    }
    
    /**
     * Obtém o ID do usuário atual
     */
    public static int getCurrentUserId(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getInt(KEY_USER_ID, -1);
    }
    
    /**
     * Obtém o tipo do usuário atual
     */
    public static String getCurrentUserType(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_USER_TYPE, "comum");
    }
    
    /**
     * Obtém o nome do usuário atual
     */
    public static String getCurrentUsername(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_USERNAME, "");
    }
    
    /**
     * Limpa a sessão do usuário (logout)
     */
    public static void clearSession(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.clear();
        editor.apply();
    }
    
    /**
     * Força logout e redireciona para login - para FirstFragment
     */
    public static void forceLogoutFromFirstFragment(Fragment fragment) {
        clearSession(fragment.getContext());
        Toast.makeText(fragment.getContext(), "Sessão expirada. Faça login novamente.", Toast.LENGTH_LONG).show();
        NavHostFragment.findNavController(fragment).navigate(R.id.action_FirstFragment_to_LoginFragment);
    }
    
    /**
     * Força logout e volta para tela anterior - para outros fragments
     */
    public static void forceLogoutFromOtherFragment(Fragment fragment, String message) {
        clearSession(fragment.getContext());
        Toast.makeText(fragment.getContext(), message, Toast.LENGTH_LONG).show();
        NavHostFragment.findNavController(fragment).navigateUp();
    }
    
    /**
     * Verifica autenticação básica no início de qualquer fragment
     * Retorna true se está autenticado, false caso contrário
     */
    public static boolean checkAuthenticationInFragment(Fragment fragment) {
        if (!isUserAuthenticated(fragment.getContext())) {
            forceLogoutFromOtherFragment(fragment, "Acesso negado. Faça login para continuar.");
            return false;
        }
        return true;
    }
    
    /**
     * Verifica autenticação de admin no início de fragments administrativos
     * Retorna true se é admin, false caso contrário
     */
    public static boolean checkAdminAuthenticationInFragment(Fragment fragment) {
        if (!isUserAdmin(fragment.getContext())) {
            forceLogoutFromOtherFragment(fragment, "Acesso negado. Faça login como administrador.");
            return false;
        }
        return true;
    }
} 