package lk.kcd.quickbite.fragment;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import java.util.Locale;

import lk.kcd.quickbite.R;
import lk.kcd.quickbite.databinding.FragmentSettingsBinding;

public class SettingsFragment extends Fragment {

    private static final String PREFS_NAME   = "quickbite_prefs";
    private static final String KEY_LANGUAGE = "selected_language";

    // Display names shown in the dialog  ←→  BCP-47 language tags
    private static final String[] LANGUAGE_LABELS = {
            "English", "සිංහල", "தமிழ்"
    };
    private static final String[] LANGUAGE_CODES = {
            "en", "si", "ta"
    };

    private FragmentSettingsBinding binding;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentSettingsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // ── Back navigation ───────────────────────────────────────────────
        requireActivity().getOnBackPressedDispatcher()
                .addCallback(getViewLifecycleOwner(), new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        navigateBack();
                    }
                });

        binding.settingsBtnBack.setOnClickListener(v -> navigateBack());

        // ── Dark Mode ─────────────────────────────────────────────────────
        boolean isDark = AppCompatDelegate.getDefaultNightMode()
                == AppCompatDelegate.MODE_NIGHT_YES;
        binding.settingsSwitchDarkMode.setChecked(isDark);
        binding.settingsSwitchDarkMode.setOnCheckedChangeListener((btn, isChecked) ->
                AppCompatDelegate.setDefaultNightMode(isChecked
                        ? AppCompatDelegate.MODE_NIGHT_YES
                        : AppCompatDelegate.MODE_NIGHT_NO));

        // ── Language ──────────────────────────────────────────────────────
        // Show the currently-saved language as the subtitle
        String savedCode = getSavedLanguageCode();
        binding.settingsLanguageValue.setText(labelForCode(savedCode));

        binding.settingsMenuLanguage.setOnClickListener(v -> showLanguagePicker());

        // ── Privacy / Terms ───────────────────────────────────────────────
        binding.settingsMenuPrivacyPolicy.setOnClickListener(v ->
                Toast.makeText(getContext(), "Privacy Policy", Toast.LENGTH_SHORT).show());
        binding.settingsMenuTerms.setOnClickListener(v ->
                Toast.makeText(getContext(), "Terms & Conditions", Toast.LENGTH_SHORT).show());

        // ── Delete Account ────────────────────────────────────────────────
        binding.settingsBtnDeleteAccount.setOnClickListener(v ->
                new AlertDialog.Builder(requireContext())
                        .setTitle("Delete Account")
                        .setMessage("Are you sure? This cannot be undone.")
                        .setPositiveButton("Delete", (d, w) ->
                                Toast.makeText(getContext(),
                                        "Not implemented yet", Toast.LENGTH_SHORT).show())
                        .setNegativeButton("Cancel", null)
                        .show());
    }

    // ── Language picker dialog ────────────────────────────────────────────
    private void showLanguagePicker() {
        String currentCode    = getSavedLanguageCode();
        int    currentIndex   = indexForCode(currentCode);

        new AlertDialog.Builder(requireContext())
                .setTitle("Select Language")
                .setSingleChoiceItems(LANGUAGE_LABELS, currentIndex, null)
                .setPositiveButton("Apply", (dialog, which) -> {
                    int chosen = ((AlertDialog) dialog).getListView().getCheckedItemPosition();
                    if (chosen < 0) return;

                    String chosenCode  = LANGUAGE_CODES[chosen];
                    String chosenLabel = LANGUAGE_LABELS[chosen];

                    // 1. Persist
                    saveLanguageCode(chosenCode);

                    // 2. Update subtitle immediately
                    binding.settingsLanguageValue.setText(chosenLabel);

                    // 3. Apply locale and recreate so all strings reload
                    applyLocale(chosenCode);
                    requireActivity().recreate();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // ── Locale helpers ────────────────────────────────────────────────────

    /**
     * Call this from your Application or BaseActivity's attachBaseContext()
     * to restore the saved locale on every launch:
     *
     *   @Override
     *   protected void attachBaseContext(Context base) {
     *       String code = new SettingsFragment.LocaleHelper().getSaved(base);
     *       super.attachBaseContext(SettingsFragment.LocaleHelper.wrap(base, code));
     *   }
     */
    private void applyLocale(String languageCode) {
        Locale locale = new Locale(languageCode);
        Locale.setDefault(locale);

        Configuration config = new Configuration(
                requireContext().getResources().getConfiguration());
        config.setLocale(locale);

        requireContext().getResources().updateConfiguration(
                config, requireContext().getResources().getDisplayMetrics());
    }

    private void saveLanguageCode(String code) {
        requireContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_LANGUAGE, code)
                .apply();
    }

    private String getSavedLanguageCode() {
        return requireContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_LANGUAGE, "en");
    }

    private String labelForCode(String code) {
        for (int i = 0; i < LANGUAGE_CODES.length; i++) {
            if (LANGUAGE_CODES[i].equals(code)) return LANGUAGE_LABELS[i];
        }
        return "English";
    }

    private int indexForCode(String code) {
        for (int i = 0; i < LANGUAGE_CODES.length; i++) {
            if (LANGUAGE_CODES[i].equals(code)) return i;
        }
        return 0;
    }

    // ── Navigation ────────────────────────────────────────────────────────
    private void navigateBack() {
        if (requireActivity().getSupportFragmentManager().getBackStackEntryCount() > 0) {
            requireActivity().getSupportFragmentManager().popBackStack();
        } else {
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new HomeFragment())
                    .commit();
        }
    }

    // ── Static helper so MainActivity / Application can restore locale ────
    public static class LocaleHelper {
        private static final String PREFS = "quickbite_prefs";
        private static final String KEY   = "selected_language";

        public static String getSaved(Context ctx) {
            return ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                    .getString(KEY, "en");
        }

        public static Context wrap(Context base, String languageCode) {
            Locale locale = new Locale(languageCode);
            Locale.setDefault(locale);
            Configuration config = new Configuration(base.getResources().getConfiguration());
            config.setLocale(locale);
            return base.createConfigurationContext(config);
        }
    }
}