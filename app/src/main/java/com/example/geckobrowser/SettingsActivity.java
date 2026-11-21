package com.example.geckobrowser;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.appcompat.widget.SwitchCompat;

public class SettingsActivity extends AppCompatActivity {

    private static final String KEY_SETTINGS_CHANGED = "settings_changed";

    private static final String[] LANG_CODES = new String[]{
            LocaleHelper.LANG_SYSTEM, "en", "vi", "es", "fr", "ja"
    };

    private static final String[] THEME_CODES = new String[]{
            LocaleHelper.THEME_SYSTEM, LocaleHelper.THEME_LIGHT, LocaleHelper.THEME_DARK
    };

    private static final String[] UI_MODE_CODES = new String[]{
            LocaleHelper.UI_MODE_PHONE, LocaleHelper.UI_MODE_TV
    };

    private static final String[] SEARCH_PRESET_URLS = new String[]{
            "https://duckduckgo.com/?q=",
            "https://www.google.com/search?q=",
            "https://www.bing.com/search?q=",
            "https://search.brave.com/search?q=",
            "https://www.ecosia.org/search?q=",
            ""
    };

    private SwitchCompat disableJavascriptToggle;
    private SwitchCompat disableTPToggle;
    private SwitchCompat desktopToggle;
    private SwitchCompat privateModeToggle;
    private SwitchCompat clearTabsOnExitToggle;
    private SwitchCompat autoHideBarToggle;
    private SwitchCompat hideWhiteSplashToggle;
    private SwitchCompat searchSuggestionsToggle;

    private EditText userAgentEditText;
    private EditText searchEngineEditText;
    private EditText homepageEditText;

    private TextView languageValueText;
    private TextView themeValueText;
    private TextView uiModeValueText;
    private TextView searchPresetValueText;

    private boolean hasChangedSettings = false;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.wrapContext(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        LocaleHelper.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        if (savedInstanceState != null && savedInstanceState.getBoolean(KEY_SETTINGS_CHANGED, false)) {
            hasChangedSettings = true;
            setResult(RESULT_OK);
        }

        bindViews();
        applyUiLayoutMode();
        loadPreferences();
        setupListeners();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putBoolean(KEY_SETTINGS_CHANGED, hasChangedSettings);
        super.onSaveInstanceState(outState);
    }

    private void bindViews() {
        disableJavascriptToggle = findViewById(R.id.disableJavascriptToggle);
        disableTPToggle = findViewById(R.id.disableTPToggle);
        desktopToggle = findViewById(R.id.desktopToggle);
        privateModeToggle = findViewById(R.id.privateModeToggle);
        clearTabsOnExitToggle = findViewById(R.id.clearTabsOnExitToggle);
        autoHideBarToggle = findViewById(R.id.autoHideBarToggle);
        hideWhiteSplashToggle = findViewById(R.id.hideWhiteSplashToggle);
        searchSuggestionsToggle = findViewById(R.id.searchSuggestionsToggle);

        userAgentEditText = findViewById(R.id.userAgentEditText);
        searchEngineEditText = findViewById(R.id.searchEngineEditText);
        homepageEditText = findViewById(R.id.homepageEditText);

        languageValueText = findViewById(R.id.languageValueText);
        themeValueText = findViewById(R.id.themeValueText);
        uiModeValueText = findViewById(R.id.uiModeValueText);
        searchPresetValueText = findViewById(R.id.searchPresetValueText);
    }

    private void applyUiLayoutMode() {
        if (!LocaleHelper.isTvMode(this)) {
            return;
        }
        float density = getResources().getDisplayMetrics().density;
        int padH = Math.round(28 * density);
        int padV = Math.round(20 * density);
        View container = findViewById(R.id.settingsContentContainer);
        if (container != null) {
            container.setPadding(padH, padV, padH, padV);
        }
        int[] tvFocusIds = new int[]{
                R.id.settingsBackButton,
                R.id.settingsSaveButton,
                R.id.languageSelectorRow,
                R.id.themeSelectorRow,
                R.id.uiModeSelectorRow,
                R.id.searchPresetRow,
                R.id.useStartPageButton,
                R.id.rowSearchSuggestions,
                R.id.rowDisableJs,
                R.id.rowDisableTp,
                R.id.rowDesktopMode,
                R.id.rowPrivateMode,
                R.id.rowClearTabsOnExit,
                R.id.rowAutoHideBar,
                R.id.rowHideWhiteSplash,
                R.id.clearHistoryRow,
                R.id.clearBookmarksRow,
                R.id.clearCacheRow
        };
        for (int id : tvFocusIds) {
            View v = findViewById(id);
            if (v != null) {
                v.setBackgroundResource(R.drawable.bg_tv_focus);
            }
        }
    }

    private void loadPreferences() {
        SharedPreferences prefs = BrowserDataStore.getSettingsPrefs(this);

        desktopToggle.setChecked(prefs.getBoolean("desktopMode", false));
        disableJavascriptToggle.setChecked(!prefs.getBoolean("javascript", true));
        disableTPToggle.setChecked(!prefs.getBoolean("trackingProtection", true));
        privateModeToggle.setChecked(prefs.getBoolean("privateMode", false));
        clearTabsOnExitToggle.setChecked(prefs.getBoolean("clearTabsOnExit", false));
        autoHideBarToggle.setChecked(prefs.getBoolean("autoHideBar", false));
        hideWhiteSplashToggle.setChecked(prefs.getBoolean("hideWhiteSplash", false));
        searchSuggestionsToggle.setChecked(prefs.getBoolean("searchSuggestions", true));

        userAgentEditText.setText(prefs.getString("userAgent", ""));
        String searchUrl = prefs.getString("searchEngine", BrowserDataStore.DEFAULT_SEARCH_URL);
        searchEngineEditText.setText(searchUrl);
        homepageEditText.setText(prefs.getString("homepage", BrowserDataStore.DEFAULT_HOMEPAGE));

        refreshLanguageLabel();
        refreshThemeLabel();
        refreshUiModeLabel();
        refreshSearchPresetLabel(searchUrl);
    }

    private void setupListeners() {
        AppCompatImageButton backBtn = findViewById(R.id.settingsBackButton);
        backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                saveSettingsAndClose(false);
            }
        });

        Button saveButton = findViewById(R.id.settingsSaveButton);
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                saveSettingsAndClose(true);
            }
        });

        findViewById(R.id.useStartPageButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                homepageEditText.setText(BrowserDataStore.START_PAGE_URL);
            }
        });

        findViewById(R.id.languageSelectorRow).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showLanguagePicker();
            }
        });

        findViewById(R.id.themeSelectorRow).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showThemePicker();
            }
        });

        findViewById(R.id.uiModeSelectorRow).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showUiModePicker();
            }
        });

        findViewById(R.id.searchPresetRow).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showSearchPresetPicker();
            }
        });

        findViewById(R.id.clearHistoryRow).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                BrowserDataStore.clearHistory(SettingsActivity.this);
                Toast.makeText(SettingsActivity.this, R.string.history_cleared, Toast.LENGTH_SHORT).show();
            }
        });

        findViewById(R.id.clearBookmarksRow).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                BrowserDataStore.clearBookmarks(SettingsActivity.this);
                Toast.makeText(SettingsActivity.this, R.string.bookmarks_cleared, Toast.LENGTH_SHORT).show();
            }
        });

        findViewById(R.id.clearCacheRow).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MainActivity.clearBrowsingSiteData();
                Toast.makeText(SettingsActivity.this, R.string.cache_cleared, Toast.LENGTH_SHORT).show();
            }
        });

        bindSwitchRow(R.id.rowSearchSuggestions, searchSuggestionsToggle);
        bindSwitchRow(R.id.rowDisableJs, disableJavascriptToggle);
        bindSwitchRow(R.id.rowDisableTp, disableTPToggle);
        bindSwitchRow(R.id.rowDesktopMode, desktopToggle);
        bindSwitchRow(R.id.rowPrivateMode, privateModeToggle);
        bindSwitchRow(R.id.rowClearTabsOnExit, clearTabsOnExitToggle);
        bindSwitchRow(R.id.rowAutoHideBar, autoHideBarToggle);
        bindSwitchRow(R.id.rowHideWhiteSplash, hideWhiteSplashToggle);
    }

    private void bindSwitchRow(int rowId, final SwitchCompat toggle) {
        View row = findViewById(rowId);
        if (row != null && toggle != null) {
            row.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    toggle.toggle();
                }
            });
        }
    }

    private void showLanguagePicker() {
        String[] labels = new String[]{
                getString(R.string.language_system),
                getString(R.string.language_en),
                getString(R.string.language_vi),
                getString(R.string.language_es),
                getString(R.string.language_fr),
                getString(R.string.language_ja)
        };
        String current = LocaleHelper.getSavedLanguage(this);
        int checked = 0;
        for (int i = 0; i < LANG_CODES.length; i++) {
            if (LANG_CODES[i].equals(current)) {
                checked = i;
                break;
            }
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.language_label)
                .setSingleChoiceItems(labels, checked, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        persistCurrentDraft();
                        LocaleHelper.saveLanguage(SettingsActivity.this, LANG_CODES[which]);
                        dialog.dismiss();
                        hasChangedSettings = true;
                        setResult(RESULT_OK);
                        recreate();
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void showThemePicker() {
        String[] labels = new String[]{
                getString(R.string.theme_system),
                getString(R.string.theme_light),
                getString(R.string.theme_dark)
        };
        String current = LocaleHelper.getSavedTheme(this);
        int checked = 0;
        for (int i = 0; i < THEME_CODES.length; i++) {
            if (THEME_CODES[i].equals(current)) {
                checked = i;
                break;
            }
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.theme_label)
                .setSingleChoiceItems(labels, checked, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        persistCurrentDraft();
                        LocaleHelper.saveTheme(SettingsActivity.this, THEME_CODES[which]);
                        LocaleHelper.applyTheme(SettingsActivity.this);
                        dialog.dismiss();
                        hasChangedSettings = true;
                        setResult(RESULT_OK);
                        recreate();
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void showUiModePicker() {
        String[] labels = new String[]{
                getString(R.string.ui_mode_phone),
                getString(R.string.ui_mode_tv)
        };
        String current = LocaleHelper.getSavedUiMode(this);
        int checked = 0;
        for (int i = 0; i < UI_MODE_CODES.length; i++) {
            if (UI_MODE_CODES[i].equals(current)) {
                checked = i;
                break;
            }
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.ui_mode_label)
                .setSingleChoiceItems(labels, checked, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        persistCurrentDraft();
                        LocaleHelper.saveUiMode(SettingsActivity.this, UI_MODE_CODES[which]);
                        dialog.dismiss();
                        hasChangedSettings = true;
                        setResult(RESULT_OK);
                        recreate();
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void showSearchPresetPicker() {
        final String[] labels = new String[]{
                getString(R.string.search_preset_ddg),
                getString(R.string.search_preset_google),
                getString(R.string.search_preset_bing),
                getString(R.string.search_preset_brave),
                getString(R.string.search_preset_ecosia),
                getString(R.string.search_preset_custom)
        };
        String current = searchEngineEditText.getText().toString().trim();
        int checked = SEARCH_PRESET_URLS.length - 1;
        for (int i = 0; i < SEARCH_PRESET_URLS.length - 1; i++) {
            if (SEARCH_PRESET_URLS[i].equals(current)) {
                checked = i;
                break;
            }
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.search_engine_preset)
                .setSingleChoiceItems(labels, checked, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (which < SEARCH_PRESET_URLS.length - 1) {
                            searchEngineEditText.setText(SEARCH_PRESET_URLS[which]);
                        }
                        searchPresetValueText.setText(labels[which]);
                        dialog.dismiss();
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void refreshLanguageLabel() {
        String code = LocaleHelper.getSavedLanguage(this);
        if ("en".equals(code)) {
            languageValueText.setText(R.string.language_en);
        } else if ("vi".equals(code)) {
            languageValueText.setText(R.string.language_vi);
        } else if ("es".equals(code)) {
            languageValueText.setText(R.string.language_es);
        } else if ("fr".equals(code)) {
            languageValueText.setText(R.string.language_fr);
        } else if ("ja".equals(code)) {
            languageValueText.setText(R.string.language_ja);
        } else {
            languageValueText.setText(R.string.language_system);
        }
    }

    private void refreshThemeLabel() {
        String theme = LocaleHelper.getSavedTheme(this);
        if (LocaleHelper.THEME_LIGHT.equals(theme)) {
            themeValueText.setText(R.string.theme_light);
        } else if (LocaleHelper.THEME_DARK.equals(theme)) {
            themeValueText.setText(R.string.theme_dark);
        } else {
            themeValueText.setText(R.string.theme_system);
        }
    }

    private void refreshUiModeLabel() {
        if (LocaleHelper.isTvMode(this)) {
            uiModeValueText.setText(R.string.ui_mode_tv);
        } else {
            uiModeValueText.setText(R.string.ui_mode_phone);
        }
    }

    private void refreshSearchPresetLabel(String url) {
        if (SEARCH_PRESET_URLS[0].equals(url)) {
            searchPresetValueText.setText(R.string.search_preset_ddg);
        } else if (SEARCH_PRESET_URLS[1].equals(url)) {
            searchPresetValueText.setText(R.string.search_preset_google);
        } else if (SEARCH_PRESET_URLS[2].equals(url)) {
            searchPresetValueText.setText(R.string.search_preset_bing);
        } else if (SEARCH_PRESET_URLS[3].equals(url)) {
            searchPresetValueText.setText(R.string.search_preset_brave);
        } else if (SEARCH_PRESET_URLS[4].equals(url)) {
            searchPresetValueText.setText(R.string.search_preset_ecosia);
        } else {
            searchPresetValueText.setText(R.string.search_preset_custom);
        }
    }

    private void persistCurrentDraft() {
        SharedPreferences.Editor editor = BrowserDataStore.getSettingsPrefs(this).edit();
        editor.putBoolean("desktopMode", desktopToggle.isChecked());
        editor.putBoolean("javascript", !disableJavascriptToggle.isChecked());
        editor.putBoolean("trackingProtection", !disableTPToggle.isChecked());
        editor.putBoolean("privateMode", privateModeToggle.isChecked());
        editor.putBoolean("clearTabsOnExit", clearTabsOnExitToggle.isChecked());
        editor.putBoolean("autoHideBar", autoHideBarToggle.isChecked());
        editor.putBoolean("hideWhiteSplash", hideWhiteSplashToggle.isChecked());
        editor.putBoolean("searchSuggestions", searchSuggestionsToggle.isChecked());
        editor.putString("userAgent", userAgentEditText.getText().toString().trim());

        String searchEngine = searchEngineEditText.getText().toString().trim();
        if (searchEngine.isEmpty()) {
            searchEngine = BrowserDataStore.DEFAULT_SEARCH_URL;
        }
        editor.putString("searchEngine", searchEngine);

        String homepage = homepageEditText.getText().toString().trim();
        if (homepage.isEmpty()) {
            homepage = BrowserDataStore.DEFAULT_HOMEPAGE;
        }
        editor.putString("homepage", homepage);
        editor.apply();
    }

    private void saveSettingsAndClose(boolean showToast) {
        persistCurrentDraft();
        if (showToast) {
            Toast.makeText(this, R.string.settings_saved, Toast.LENGTH_SHORT).show();
        }
        hideKeyboard();
        setResult(RESULT_OK);
        finish();
    }

    @Override
    public void onBackPressed() {
        saveSettingsAndClose(false);
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
        if (imm == null) {
            return;
        }
        View view = getCurrentFocus();
        if (view == null) {
            view = new View(this);
        }
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }
}