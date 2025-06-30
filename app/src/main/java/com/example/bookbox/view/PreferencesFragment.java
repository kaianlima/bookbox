package com.example.bookbox.view;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.AdapterView;
import android.widget.TextView;
import android.util.TypedValue;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.navigation.fragment.NavHostFragment;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.bookbox.R;
import com.example.bookbox.databinding.FragmentPreferencesBinding;
import com.example.bookbox.utils.AuthUtils;

public class PreferencesFragment extends Fragment {

    private FragmentPreferencesBinding binding;
    private static boolean isAccessibilityModeEnabled = false;
    private static final float NORMAL_TEXT_SIZE = 14;
    private static final float NORMAL_TITLE_SIZE = 22;
    private static final float ACCESSIBLE_TEXT_SIZE = 18;
    private static final float ACCESSIBLE_TITLE_SIZE = 28;
    
    private static int currentTheme = 0;

    private MediaPlayer mediaPlayer;
    private String currentUserType;
    
    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        binding = FragmentPreferencesBinding.inflate(inflater, container, false);

        if (!AuthUtils.checkAuthenticationInFragment(this)) {
            return binding.getRoot();
        }
        
        currentUserType = AuthUtils.getCurrentUserType(getContext());
        
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setupAdminOptions();
        setupThemeOptions();
        setupAccessibilityOptions();

        PreferencesFragment.applyAccessibilityModeToView(this.getView());
    }
    
    private void setupAdminOptions() {
        if ("admin".equals(currentUserType)) {
            binding.adminOptionsCard.setVisibility(View.VISIBLE);
            
            binding.addBookOption.setOnClickListener(v -> {
                NavHostFragment.findNavController(PreferencesFragment.this)
                        .navigate(R.id.action_PreferencesFragment_to_AddBookFragment);
            });
            
            binding.manageBooksOption.setOnClickListener(v -> {
                NavHostFragment.findNavController(PreferencesFragment.this)
                        .navigate(R.id.action_PreferencesFragment_to_ManageBooksFragment);
            });
        } else {
            binding.adminOptionsCard.setVisibility(View.GONE);
        }
    }
    
    private void setupThemeOptions() {
        Spinner spinnerTheme = binding.spinnerTheme;
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                requireContext(),
                R.array.theme_options,
                android.R.layout.simple_spinner_item
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerTheme.setAdapter(adapter);
        spinnerTheme.setSelection(currentTheme);

        spinnerTheme.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (currentTheme != position) {
                    currentTheme = position;
                    applyTheme(position);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void setupAccessibilityOptions() {
        Switch switchAccessibility = binding.switchAccessibility;
        switchAccessibility.setChecked(isAccessibilityModeEnabled);

        switchAccessibility.setOnCheckedChangeListener((buttonView, isChecked) -> {
            playNavigationSound();
            
            isAccessibilityModeEnabled = isChecked;
            applyAccessibilityModeToView(this.getView());
        });
    }
    
    private void playNavigationSound() {
        try {
            if (mediaPlayer != null) {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                }
                mediaPlayer.release();
                mediaPlayer = null;
            }

            mediaPlayer = MediaPlayer.create(requireContext(), R.raw.navigation_sound);
            if (mediaPlayer != null) {
                mediaPlayer.setVolume(1, 1);
                
                mediaPlayer.setOnCompletionListener(mp -> {
                    mp.release();
                    mediaPlayer = null;
                });
                mediaPlayer.start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void applyTheme(int theme) {
        switch (theme) {
            case 0:
                requireContext().setTheme(R.style.Theme_BookBox);
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
            case 1:
                requireContext().setTheme(R.style.Theme_BookBox);
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
        }

        requireActivity().recreate();
    }

    public static boolean isAccessibilityModeEnabled() {
        return isAccessibilityModeEnabled;
    }

    public static void applyAccessibilityModeToView(View view) {
        if (view == null) return;

        if (view instanceof TextView) {
            TextView textView = (TextView) view;
            float currentSize = textView.getTextSize();
            
            boolean isTitle = currentSize >= TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP, 
                NORMAL_TITLE_SIZE, 
                textView.getResources().getDisplayMetrics()
            );
            
            if (isAccessibilityModeEnabled) {
                textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, isTitle ? ACCESSIBLE_TITLE_SIZE : ACCESSIBLE_TEXT_SIZE);
            } else {
                textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, isTitle ? NORMAL_TITLE_SIZE : NORMAL_TEXT_SIZE);
            }
            
            textView.setLineSpacing(1, 1);
        }

        if (view instanceof ViewGroup) {
            ViewGroup viewGroup = (ViewGroup) view;
            for (int i = 0; i < viewGroup.getChildCount(); i++) {
                applyAccessibilityModeToView(viewGroup.getChildAt(i));
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
            }
            mediaPlayer.release();
            mediaPlayer = null;
        }
        binding = null;
    }
} 