package com.example.geckobrowser;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;

import androidx.appcompat.app.AppCompatDelegate;

import java.util.Locale;

public final class LocaleHelper {

    public static final String PREFS_SETTINGS = "settingsPrefs";
    public static final String KEY_LANGUAGE = "appLanguage";
    public static final String KEY_THEME = "appTheme";
    public static final String KEY_UI_MODE = "appUiMode";

    public static final String LANG_SYSTEM = "system";
    public static final String THEME_SYSTEM = "system";
    public static final String THEME_LIGHT = "light";
    public static final String THEME_DARK = "dark";
    public static final String UI_MODE_PHONE = "phone";
    public static final String UI_MODE_TV = "tv";

    private LocaleHelper() {
    }

    @SuppressWarnings("deprecation")
    private static Locale getSystemDefaultLocale() {
        Configuration sysConfig = Resources.getSystem().getConfiguration();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && !sysConfig.getLocales().isEmpty()) {
            return sysConfig.getLocales().get(0);
        }
        return sysConfig.locale != null ? sysConfig.locale : Locale.getDefault();
    }

    public static String getSavedLanguage(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_SETTINGS, Context.MODE_PRIVATE);
        return prefs.getString(KEY_LANGUAGE, LANG_SYSTEM);
    }

    public static void saveLanguage(Context context, String langCode) {
        context.getSharedPreferences(PREFS_SETTINGS, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_LANGUAGE, langCode)
                .apply();
    }

    public static String getSavedTheme(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_SETTINGS, Context.MODE_PRIVATE);
        return prefs.getString(KEY_THEME, THEME_SYSTEM);
    }

    public static void saveTheme(Context context, String themeMode) {
        context.getSharedPreferences(PREFS_SETTINGS, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_THEME, themeMode)
                .apply();
    }

    public static String getSavedUiMode(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_SETTINGS, Context.MODE_PRIVATE);
        String mode = prefs.getString(KEY_UI_MODE, UI_MODE_PHONE);
        return UI_MODE_TV.equals(mode) ? UI_MODE_TV : UI_MODE_PHONE;
    }

    public static boolean isTvMode(Context context) {
        return UI_MODE_TV.equals(getSavedUiMode(context));
    }

    public static void saveUiMode(Context context, String uiMode) {
        context.getSharedPreferences(PREFS_SETTINGS, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_UI_MODE, UI_MODE_TV.equals(uiMode) ? UI_MODE_TV : UI_MODE_PHONE)
                .apply();
    }

    public static void applyTheme(Context context) {
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
        String theme = getSavedTheme(context);
        if (THEME_LIGHT.equals(theme)) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        } else if (THEME_DARK.equals(theme)) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        }
    }

    @SuppressWarnings("deprecation")
    public static Context wrapContext(Context context) {
        String lang = getSavedLanguage(context);
        Locale targetLocale;
        if (LANG_SYSTEM.equals(lang) || lang == null || lang.isEmpty()) {
            targetLocale = getSystemDefaultLocale();
        } else {
            targetLocale = new Locale(lang);
        }
        Locale.setDefault(targetLocale);

        Resources res = context.getResources();
        Configuration config = new Configuration(res.getConfiguration());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.setLocale(targetLocale);
            return context.createConfigurationContext(config);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            config.setLocale(targetLocale);
            res.updateConfiguration(config, res.getDisplayMetrics());
            return context;
        } else {
            config.locale = targetLocale;
            res.updateConfiguration(config, res.getDisplayMetrics());
            return context;
        }
    }
}
