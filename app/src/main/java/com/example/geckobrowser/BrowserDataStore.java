package com.example.geckobrowser;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public final class BrowserDataStore {

    public static final String START_PAGE_URL = "about:home";
    public static final String DEFAULT_HOMEPAGE = "https://start.duckduckgo.com";
    public static final String DEFAULT_SEARCH_URL = "https://duckduckgo.com/?q=";

    private static final String PREFS_DATA = "dataPrefs";
    private static final String PREFS_SETTINGS = "settingsPrefs";

    private static final String KEY_TABS_JSON = "tabsJson";
    private static final String KEY_ACTIVE_TAB = "activeTab";
    private static final String KEY_BOOKMARKS_JSON = "bookmarksJson";
    private static final String KEY_HISTORY_JSON = "historyJson";
    private static final String KEY_DOWNLOADS_JSON = "downloadsJson";
    private static final String KEY_QUICK_SITES_JSON = "quickSitesJson";

    private static final int MAX_HISTORY = 200;

    private BrowserDataStore() {
    }

    public static class SavedPage {
        public final String title;
        public final String url;
        public final long timestamp;

        public SavedPage(String title, String url, long timestamp) {
            this.title = title;
            this.url = url;
            this.timestamp = timestamp;
        }
    }

    public static class DownloadRecord {
        public final long downloadId;
        public final String filename;
        public final String url;
        public final String mimeType;
        public final long timestamp;

        public DownloadRecord(long downloadId, String filename, String url, String mimeType, long timestamp) {
            this.downloadId = downloadId;
            this.filename = filename;
            this.url = url;
            this.mimeType = mimeType;
            this.timestamp = timestamp;
        }
    }

    public static class SavedTab {
        public final String url;
        public final String title;
        public final boolean isPrivate;

        public SavedTab(String url, String title, boolean isPrivate) {
            this.url = url;
            this.title = title;
            this.isPrivate = isPrivate;
        }
    }

    // ---- Settings Getters ----

    public static SharedPreferences getSettingsPrefs(Context context) {
        return context.getSharedPreferences(PREFS_SETTINGS, Context.MODE_PRIVATE);
    }

    public static String getHomepage(Context context) {
        String hp = getSettingsPrefs(context).getString("homepage", DEFAULT_HOMEPAGE);
        if (hp == null || hp.trim().isEmpty()) {
            return DEFAULT_HOMEPAGE;
        }
        return hp.trim();
    }

    public static String getSearchBaseUrl(Context context) {
        String engine = getSettingsPrefs(context).getString("searchEngine", DEFAULT_SEARCH_URL);
        if (engine == null || engine.trim().isEmpty()) {
            return DEFAULT_SEARCH_URL;
        }
        return engine.trim();
    }

    public static boolean isJavascriptEnabled(Context context) {
        return getSettingsPrefs(context).getBoolean("javascript", true);
    }

    public static boolean isTrackingProtectionEnabled(Context context) {
        return getSettingsPrefs(context).getBoolean("trackingProtection", true);
    }

    public static boolean isDesktopModeDefault(Context context) {
        return getSettingsPrefs(context).getBoolean("desktopMode", false);
    }

    public static boolean isPrivateModeDefault(Context context) {
        return getSettingsPrefs(context).getBoolean("privateMode", false);
    }

    public static boolean isClearTabsOnExit(Context context) {
        return getSettingsPrefs(context).getBoolean("clearTabsOnExit", false);
    }

    public static boolean isAutoHideBar(Context context) {
        return getSettingsPrefs(context).getBoolean("autoHideBar", false);
    }

    public static boolean isHideWhiteSplash(Context context) {
        return getSettingsPrefs(context).getBoolean("hideWhiteSplash", false);
    }

    public static boolean isSearchSuggestionsEnabled(Context context) {
        return getSettingsPrefs(context).getBoolean("searchSuggestions", true);
    }

    public static String getCustomUserAgent(Context context) {
        String ua = getSettingsPrefs(context).getString("userAgent", "");
        return ua != null ? ua.trim() : "";
    }

    // ---- Tabs Persistence ----

    public static void saveTabs(Context context, List<BrowserTab> tabs, int activeIndex) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_DATA, Context.MODE_PRIVATE);
        if (isClearTabsOnExit(context)) {
            prefs.edit()
                    .remove(KEY_TABS_JSON)
                    .putInt(KEY_ACTIVE_TAB, 0)
                    .apply();
            return;
        }

        JSONArray arr = new JSONArray();
        int persistedActive = 0;
        int idx = 0;
        for (int i = 0; i < tabs.size(); i++) {
            BrowserTab t = tabs.get(i);
            if (t.isPrivate) {
                continue;
            }
            JSONObject obj = new JSONObject();
            try {
                obj.put("url", t.url != null ? t.url : "");
                obj.put("title", t.title != null ? t.title : "");
                arr.put(obj);
                if (i == activeIndex) {
                    persistedActive = idx;
                }
                idx++;
            } catch (JSONException ignored) {
            }
        }
        prefs.edit()
                .putString(KEY_TABS_JSON, arr.toString())
                .putInt(KEY_ACTIVE_TAB, persistedActive)
                .apply();
    }

    public static List<SavedTab> loadTabs(Context context) {
        List<SavedTab> result = new ArrayList<>();
        if (isClearTabsOnExit(context)) {
            return result;
        }
        SharedPreferences prefs = context.getSharedPreferences(PREFS_DATA, Context.MODE_PRIVATE);
        String json = prefs.getString(KEY_TABS_JSON, null);
        if (json != null && !json.isEmpty()) {
            try {
                JSONArray arr = new JSONArray(json);
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject obj = arr.getJSONObject(i);
                    result.add(new SavedTab(
                            obj.optString("url", ""),
                            obj.optString("title", ""),
                            false
                    ));
                }
            } catch (JSONException ignored) {
            }
        } else {
            String legacy = prefs.getString("tabURLs", "");
            if (legacy != null && !legacy.trim().isEmpty()) {
                String[] parts = legacy.split(", ");
                for (String u : parts) {
                    if (u != null && !u.trim().isEmpty()) {
                        result.add(new SavedTab(u.trim(), "", false));
                    }
                }
            }
        }
        return result;
    }

    public static int loadActiveTabIndex(Context context, int tabCount) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_DATA, Context.MODE_PRIVATE);
        int idx = prefs.getInt(KEY_ACTIVE_TAB, 0);
        if (idx < 0 || idx >= tabCount) {
            return 0;
        }
        return idx;
    }

    // ---- Bookmarks ----

    public static List<SavedPage> getBookmarks(Context context) {
        return readPageList(context, KEY_BOOKMARKS_JSON);
    }

    public static boolean isBookmarked(Context context, String url) {
        if (url == null || url.trim().isEmpty()) {
            return false;
        }
        List<SavedPage> list = getBookmarks(context);
        for (SavedPage p : list) {
            if (url.equalsIgnoreCase(p.url)) {
                return true;
            }
        }
        return false;
    }

    public static void addBookmark(Context context, String title, String url) {
        if (url == null || url.trim().isEmpty()) {
            return;
        }
        List<SavedPage> list = getBookmarks(context);
        for (SavedPage p : list) {
            if (url.equalsIgnoreCase(p.url)) {
                return;
            }
        }
        String safeTitle = (title != null && !title.trim().isEmpty()) ? title.trim() : url;
        list.add(0, new SavedPage(safeTitle, url.trim(), System.currentTimeMillis()));
        writePageList(context, KEY_BOOKMARKS_JSON, list);
    }

    public static void updateBookmark(Context context, String oldUrl, String newTitle, String newUrl) {
        if (oldUrl == null || newUrl == null || newUrl.trim().isEmpty()) {
            return;
        }
        List<SavedPage> list = getBookmarks(context);
        List<SavedPage> updated = new ArrayList<>();
        String cleanNewUrl = newUrl.trim();
        String cleanTitle = (newTitle != null && !newTitle.trim().isEmpty()) ? newTitle.trim() : cleanNewUrl;
        boolean replaced = false;
        for (SavedPage p : list) {
            if (!replaced && oldUrl.equalsIgnoreCase(p.url)) {
                updated.add(new SavedPage(cleanTitle, cleanNewUrl, p.timestamp));
                replaced = true;
            } else if (!cleanNewUrl.equalsIgnoreCase(p.url)) {
                updated.add(p);
            }
        }
        writePageList(context, KEY_BOOKMARKS_JSON, updated);
    }

    public static void removeBookmark(Context context, String url) {
        if (url == null) {
            return;
        }
        List<SavedPage> list = getBookmarks(context);
        List<SavedPage> updated = new ArrayList<>();
        for (SavedPage p : list) {
            if (!url.equalsIgnoreCase(p.url)) {
                updated.add(p);
            }
        }
        writePageList(context, KEY_BOOKMARKS_JSON, updated);
    }

    public static void clearBookmarks(Context context) {
        context.getSharedPreferences(PREFS_DATA, Context.MODE_PRIVATE)
                .edit()
                .remove(KEY_BOOKMARKS_JSON)
                .apply();
    }

    // ---- History ----

    public static List<SavedPage> getHistory(Context context) {
        return readPageList(context, KEY_HISTORY_JSON);
    }

    public static void addHistory(Context context, String title, String url) {
        if (url == null || url.trim().isEmpty() || url.startsWith("about:")) {
            return;
        }
        String cleanUrl = url.trim();
        String cleanTitle = (title != null && !title.trim().isEmpty()) ? title.trim() : cleanUrl;
        List<SavedPage> list = getHistory(context);

        List<SavedPage> updated = new ArrayList<>();
        updated.add(new SavedPage(cleanTitle, cleanUrl, System.currentTimeMillis()));
        for (SavedPage p : list) {
            if (!cleanUrl.equalsIgnoreCase(p.url)) {
                updated.add(p);
                if (updated.size() >= MAX_HISTORY) {
                    break;
                }
            }
        }
        writePageList(context, KEY_HISTORY_JSON, updated);
    }

    public static void removeHistoryByUrl(Context context, String url) {
        if (url == null) {
            return;
        }
        List<SavedPage> list = getHistory(context);
        List<SavedPage> updated = new ArrayList<>();
        for (SavedPage p : list) {
            if (!url.equalsIgnoreCase(p.url)) {
                updated.add(p);
            }
        }
        writePageList(context, KEY_HISTORY_JSON, updated);
    }

    public static void clearHistory(Context context) {
        context.getSharedPreferences(PREFS_DATA, Context.MODE_PRIVATE)
                .edit()
                .remove(KEY_HISTORY_JSON)
                .apply();
    }

    // ---- Downloads ----

    public static List<DownloadRecord> getDownloads(Context context) {
        List<DownloadRecord> result = new ArrayList<>();
        String json = context.getSharedPreferences(PREFS_DATA, Context.MODE_PRIVATE).getString(KEY_DOWNLOADS_JSON, null);
        if (json == null || json.isEmpty()) {
            return result;
        }
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                result.add(new DownloadRecord(
                        obj.optLong("id", -1L),
                        obj.optString("filename", "download"),
                        obj.optString("url", ""),
                        obj.optString("mime", ""),
                        obj.optLong("ts", 0L)
                ));
            }
        } catch (JSONException ignored) {
        }
        return result;
    }

    public static void addDownload(Context context, DownloadRecord record) {
        if (record == null) {
            return;
        }
        List<DownloadRecord> list = getDownloads(context);
        list.add(0, record);
        writeDownloads(context, list);
    }

    public static void removeDownloadByRecord(Context context, DownloadRecord target) {
        if (target == null) {
            return;
        }
        List<DownloadRecord> list = getDownloads(context);
        List<DownloadRecord> updated = new ArrayList<>();
        boolean removed = false;
        for (DownloadRecord d : list) {
            if (!removed && d.downloadId == target.downloadId && d.timestamp == target.timestamp) {
                removed = true;
                continue;
            }
            updated.add(d);
        }
        writeDownloads(context, updated);
    }

    public static void clearDownloads(Context context) {
        context.getSharedPreferences(PREFS_DATA, Context.MODE_PRIVATE)
                .edit()
                .remove(KEY_DOWNLOADS_JSON)
                .apply();
    }

    private static void writeDownloads(Context context, List<DownloadRecord> list) {
        JSONArray arr = new JSONArray();
        for (DownloadRecord d : list) {
            JSONObject obj = new JSONObject();
            try {
                obj.put("id", d.downloadId);
                obj.put("filename", d.filename);
                obj.put("url", d.url);
                obj.put("mime", d.mimeType);
                obj.put("ts", d.timestamp);
                arr.put(obj);
            } catch (JSONException ignored) {
            }
        }
        context.getSharedPreferences(PREFS_DATA, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_DOWNLOADS_JSON, arr.toString())
                .apply();
    }

    // ---- Quick Sites / Speed Dial ----

    public static List<SavedPage> getQuickSites(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_DATA, Context.MODE_PRIVATE);
        if (!prefs.contains(KEY_QUICK_SITES_JSON)) {
            List<SavedPage> defaults = getDefaultQuickSites();
            writePageList(context, KEY_QUICK_SITES_JSON, defaults);
            return defaults;
        }
        return readPageList(context, KEY_QUICK_SITES_JSON);
    }

    public static void addQuickSite(Context context, String title, String url) {
        if (url == null || url.trim().isEmpty()) {
            return;
        }
        String cleanUrl = url.trim();
        if (!cleanUrl.startsWith("http://") && !cleanUrl.startsWith("https://")) {
            cleanUrl = "https://" + cleanUrl;
        }
        String cleanTitle = (title != null && !title.trim().isEmpty()) ? title.trim() : cleanUrl;
        List<SavedPage> list = getQuickSites(context);
        for (SavedPage p : list) {
            if (cleanUrl.equalsIgnoreCase(p.url)) {
                return;
            }
        }
        list.add(new SavedPage(cleanTitle, cleanUrl, System.currentTimeMillis()));
        writePageList(context, KEY_QUICK_SITES_JSON, list);
    }

    public static void removeQuickSite(Context context, String url) {
        if (url == null) {
            return;
        }
        List<SavedPage> list = getQuickSites(context);
        List<SavedPage> updated = new ArrayList<>();
        for (SavedPage p : list) {
            if (!url.equalsIgnoreCase(p.url)) {
                updated.add(p);
            }
        }
        writePageList(context, KEY_QUICK_SITES_JSON, updated);
    }

    public static List<SavedPage> getDefaultQuickSites() {
        List<SavedPage> sites = new ArrayList<>();
        sites.add(new SavedPage("DuckDuckGo", "https://start.duckduckgo.com", 0L));
        sites.add(new SavedPage("Wikipedia", "https://www.wikipedia.org", 0L));
        sites.add(new SavedPage("GitHub", "https://github.com", 0L));
        sites.add(new SavedPage("YouTube", "https://m.youtube.com", 0L));
        sites.add(new SavedPage("Google", "https://www.google.com", 0L));
        sites.add(new SavedPage("Reddit", "https://www.reddit.com", 0L));
        return sites;
    }

    private static List<SavedPage> readPageList(Context context, String key) {
        List<SavedPage> result = new ArrayList<>();
        String json = context.getSharedPreferences(PREFS_DATA, Context.MODE_PRIVATE).getString(key, null);
        if (json == null || json.isEmpty()) {
            return result;
        }
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                result.add(new SavedPage(
                        obj.optString("title", ""),
                        obj.optString("url", ""),
                        obj.optLong("ts", 0L)
                ));
            }
        } catch (JSONException ignored) {
        }
        return result;
    }

    private static void writePageList(Context context, String key, List<SavedPage> list) {
        JSONArray arr = new JSONArray();
        for (SavedPage p : list) {
            JSONObject obj = new JSONObject();
            try {
                obj.put("title", p.title);
                obj.put("url", p.url);
                obj.put("ts", p.timestamp);
                arr.put(obj);
            } catch (JSONException ignored) {
            }
        }
        context.getSharedPreferences(PREFS_DATA, Context.MODE_PRIVATE)
                .edit()
                .putString(key, arr.toString())
                .apply();
    }
}
