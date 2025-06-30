package com.example.bookbox.view;

import android.content.pm.PackageManager;
import android.os.Bundle;

import com.example.bookbox.R;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.view.View;

import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.example.bookbox.databinding.ActivityMainBinding;
import com.example.bookbox.utils.AuthUtils;
import com.example.bookbox.utils.NotificationHelper;

import android.view.Menu;
import android.view.MenuItem;

import com.example.bookbox.utils.FileProviderHelper;

public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration appBarConfiguration;
    private ActivityMainBinding binding;
    private Menu currentMenu;
    private static final int NOTIFICATION_PERMISSION_CODE = 200;
    private static final int CAMERA_PERMISSION_CODE = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);

        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        appBarConfiguration = new AppBarConfiguration.Builder(navController.getGraph()).build();
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);

        binding.fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                navController.navigate(R.id.ProfileFragment);
            }
        });

        navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
            if (destination.getId() == R.id.FirstFragment) {
                binding.fab.setVisibility(View.VISIBLE);
            } else {
                binding.fab.setVisibility(View.GONE);
            }

            updateMenuVisibility(destination.getId());
        });

        FileProviderHelper.cleanupTempImages(this);

        requestNotificationPermissionIfNeeded();
    }
    
    private void requestNotificationPermissionIfNeeded() {
        if (AuthUtils.isUserAuthenticated(this) && !NotificationHelper.hasNotificationPermission(this)) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 
                                 NOTIFICATION_PERMISSION_CODE);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, 
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        android.util.Log.d("MainActivity", "onRequestPermissionsResult - requestCode: " + requestCode);
        
        if (requestCode == NOTIFICATION_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                android.util.Log.d("MainActivity", "Permissão de notificação concedida");
            } else {
                android.util.Log.d("MainActivity", "Permissão de notificação negada");
            }
        } else if (requestCode == CAMERA_PERMISSION_CODE) {
            android.util.Log.d("MainActivity", "Delegando permissão de câmera para fragment");
            
            NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
            androidx.fragment.app.Fragment currentFragment = getSupportFragmentManager()
                    .findFragmentById(R.id.nav_host_fragment_content_main);
            
            if (currentFragment != null) {
                androidx.fragment.app.Fragment actualFragment = currentFragment.getChildFragmentManager()
                        .getPrimaryNavigationFragment();
                
                if (actualFragment != null) {
                    android.util.Log.d("MainActivity", "Chamando onRequestPermissionsResult no fragment: " + 
                                       actualFragment.getClass().getSimpleName());
                    actualFragment.onRequestPermissionsResult(requestCode, permissions, grantResults);
                }
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        currentMenu = menu;

        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        if (navController.getCurrentDestination() != null) {
            updateMenuVisibility(navController.getCurrentDestination().getId());
        }
        
        return true;
    }
    
    private void updateMenuVisibility(int destinationId) {
        if (currentMenu == null) return;
        
        boolean isAuthenticated = AuthUtils.isUserAuthenticated(this);
        boolean isLoginOrSignupFragment = destinationId == R.id.LoginFragment;

        boolean shouldShowMenuOptions = isAuthenticated && !isLoginOrSignupFragment;
        
        MenuItem preferencesItem = currentMenu.findItem(R.id.action_preferences);
        MenuItem logoutItem = currentMenu.findItem(R.id.action_logout);
        
        if (preferencesItem != null) {
            preferencesItem.setVisible(shouldShowMenuOptions);
        }
        if (logoutItem != null) {
            logoutItem.setVisible(shouldShowMenuOptions);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_preferences) {
            NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
            navController.navigate(R.id.PreferencesFragment);
            return true;
        } else if (id == R.id.action_logout) {
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, appBarConfiguration)
                || super.onSupportNavigateUp();
    }
}