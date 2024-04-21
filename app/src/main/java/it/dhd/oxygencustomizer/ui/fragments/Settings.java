package it.dhd.oxygencustomizer.ui.fragments;

import static it.dhd.oxygencustomizer.utils.Constants.Packages.SYSTEM_UI;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import it.dhd.oxygencustomizer.BuildConfig;
import it.dhd.oxygencustomizer.R;
import it.dhd.oxygencustomizer.ui.activity.MainActivity;
import it.dhd.oxygencustomizer.utils.AppUtils;
import it.dhd.oxygencustomizer.utils.PrefManager;
import it.dhd.oxygencustomizer.utils.PreferenceHelper;

public class Settings extends PreferenceFragmentCompat {

    private static final int REQUEST_IMPORT = 98;
    private static final int REQUEST_EXPORT = 99;

    Preference ghPref, deleteAllPref, importPref, exportPref, creditsPref;

    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
        setPreferencesFromResource(R.xml.own_settings, rootKey);

        ghPref = findPreference("GitHubRepo");
        deleteAllPref = findPreference("deleteAllPrefs");
        exportPref = findPreference("export");
        importPref = findPreference("import");
        creditsPref = findPreference("credits");

        if (ghPref != null) {
            ghPref.setOnPreferenceClickListener(preference -> {
                // Open GitHub
                requireActivity().startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/DHD2280/Oxygen-Customizer")));
                return true;
            });
        }

        if (deleteAllPref != null) {
            deleteAllPref.setOnPreferenceClickListener(preference -> {
                // Delete all data
                PrefManager.clearPrefs(requireContext().createDeviceProtectedStorageContext().getSharedPreferences(BuildConfig.APPLICATION_ID + "_preferences", Context.MODE_PRIVATE));
                AppUtils.restartAllScope(new String[]{SYSTEM_UI});
                return true;
            });
        }

        if (exportPref != null) {
            exportPref.setOnPreferenceClickListener(preference -> {
                // Export data
                importExportSettings(true);
                return true;
            });
        }

        if (importPref != null) {
            importPref.setOnPreferenceClickListener(preference -> {
                // Export data
                importExportSettings(false);
                return true;
            });
        }

        if (creditsPref != null) {
            creditsPref.setOnPreferenceClickListener(preference -> {
                MainActivity.replaceFragment(new Credits());
                return true;
            });
        }

    }

    private void importExportSettings(boolean export) {
        Intent fileIntent = new Intent();
        fileIntent.setAction(export ? Intent.ACTION_CREATE_DOCUMENT : Intent.ACTION_GET_CONTENT);
        fileIntent.setType("*/*");
        fileIntent.putExtra(Intent.EXTRA_TITLE, "OxygenCustomizer_Config" + ".bin");
        fileIntent.putExtra("export", export);
        mImportExportLauncher.launch(fileIntent);
    }

    ActivityResultLauncher<Intent> mImportExportLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    Intent data = result.getData();
                    if (data == null) return;
                    boolean export = data.getBooleanExtra("export", true);

                    SharedPreferences prefs = requireContext().createDeviceProtectedStorageContext().getSharedPreferences(BuildConfig.APPLICATION_ID + "_preferences", Context.MODE_PRIVATE);

                    if (export) {
                        try {
                            PrefManager.exportPrefs(prefs, requireContext().getContentResolver().openOutputStream(data.getData()));
                        } catch (Exception ignored) {
                        }
                    } else {
                        try {
                            PrefManager.importPath(prefs, requireContext().getContentResolver().openInputStream(data.getData()));
                        } catch (Exception ignored) {
                        }
                    }
                }
            });

    @Override
    public void setDivider(Drawable divider) {
        super.setDivider(new ColorDrawable(Color.TRANSPARENT));
    }

}
