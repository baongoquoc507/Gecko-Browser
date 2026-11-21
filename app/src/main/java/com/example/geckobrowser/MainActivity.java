package com.example.geckobrowser;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.RecognizerIntent;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.core.content.ContextCompat;
import androidx.core.widget.ImageViewCompat;

import org.json.JSONArray;
import org.json.JSONObject;
import org.mozilla.geckoview.AllowOrDeny;
import org.mozilla.geckoview.ContentBlocking;
import org.mozilla.geckoview.GeckoResult;
import org.mozilla.geckoview.GeckoRuntime;
import org.mozilla.geckoview.GeckoRuntimeSettings;
import org.mozilla.geckoview.GeckoSession;
import org.mozilla.geckoview.GeckoSessionSettings;
import org.mozilla.geckoview.GeckoView;
import org.mozilla.geckoview.StorageController;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.cert.X509Certificate;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.net.ssl.HttpsURLConnection;

public class MainActivity extends AppCompatActivity implements
        BrowserDialogs.BrowserActions,
        BrowserPromptDelegate.FilePickerHost {

    private static final String TAG = "GeckoBrowser";
    private static final int REQUEST_SETTINGS = 1001;
    private static final int REQUEST_FILE_PICKER = 1002;
    private static final int REQUEST_VOICE_SEARCH = 1003;
    private static final int MAX_CLOSED_TABS = 10;

    private static GeckoRuntime sRuntime;

    private String searchUriBase = BrowserDataStore.DEFAULT_SEARCH_URL;
    private String homepageUrl = BrowserDataStore.DEFAULT_HOMEPAGE;
    private boolean autoHideBar;
    private boolean hideWhiteSplash;
    private boolean searchSuggestionsEnabled;
    private boolean voiceSearchSupported;

    private String appliedLanguage;
    private String appliedTheme;
    private String appliedUiMode;
    private boolean isTvMode;

    private GeckoView mGeckoView;
    private ViewGroup tvTopHost;
    private ViewGroup phoneBottomDock;
    private View toolbarView;
    private View bottomNavContainer;
    private View bottomNavPill;
    private View tvNavCluster;
    private View tvNavTabsButton;
    private View privateBanner;
    private View omniboxContainer;
    private View findBar;
    private View startPageContainer;
    private View suggestionsContainer;
    private ListView suggestionsListView;
    private GridView startPageGridView;
    private FrameLayout fullscreenContainer;

    private View startPageIncognitoPill;
    private AppCompatImageView startPageIncognitoPillIcon;
    private TextView startPageIncognitoPillText;
    private AppCompatImageButton startPageSettingsBtn;
    private AppCompatImageView startPageHeroLogo;
    private TextView startPageHeroTitle;
    private TextView startPageHeroSubtitle;
    private View startPageSearchCard;
    private AppCompatImageView startPageSearchIcon;
    private TextView startPageSearchHint;
    private View startPageBookmarksBg;
    private View startPageHistoryBg;
    private View startPageDownloadsBg;
    private View startPageIncognitoBg;
    private TextView startPageBookmarksLabel;
    private TextView startPageHistoryLabel;
    private TextView startPageDownloadsLabel;
    private TextView startPageIncognitoLabel;
    private TextView startPageQuickAccessTitle;

    private EditText urlEditText;
    private EditText findEditText;
    private TextView findMatchCountText;
    private ProgressBar mProgressView;
    private AppCompatImageView securityIconView;
    private TextView trackersCountView;
    private TextView tabsCountView;
    private TextView tvTabsCountView;
    private View navPlusPillBg;
    private AppCompatImageView navHomePlusIcon;
    private AppCompatImageButton clearUrlButton;
    private AppCompatImageButton voiceSearchButton;
    private AppCompatImageButton startPageVoiceBtn;
    private AppCompatImageButton bookmarkButton;
    private AppCompatImageButton shareUrlButton;
    private AppCompatImageButton reloadStopButton;
    private AppCompatImageButton navBackButton;
    private AppCompatImageButton navForwardButton;
    private AppCompatImageButton tvNavBackButton;
    private AppCompatImageButton tvNavForwardButton;
    private AppCompatImageButton tvNavHomeButton;
    private AppCompatImageButton tvNavMoreButton;

    private final ArrayList<BrowserTab> tabs = new ArrayList<>();
    private final ArrayList<BrowserDataStore.SavedTab> closedTabs = new ArrayList<>();
    private final ArrayList<SuggestionItem> suggestionItems = new ArrayList<>();
    private final ArrayList<BrowserDataStore.SavedPage> quickSites = new ArrayList<>();
    private SuggestionsAdapter suggestionsAdapter;
    private SpeedDialAdapter speedDialAdapter;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private Runnable pendingSuggestionRunnable;
    private int suggestionRequestToken = 0;

    private GeckoSession.PromptDelegate.FilePrompt pendingFilePrompt;
    private GeckoResult<GeckoSession.PromptDelegate.PromptResponse> pendingFileResult;

    private int activeTabIndex = 0;
    private long nextTabId = 1L;
    private int scrollAnchorY = 0;
    private boolean isFullscreen = false;
    private boolean currentTabPrivate = false;

    public static void clearBrowsingSiteData() {
        if (sRuntime != null) {
            sRuntime.getStorageController().clearData(StorageController.ClearFlags.ALL);
        }
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.wrapContext(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        LocaleHelper.applyTheme(this);
        appliedLanguage = LocaleHelper.getSavedLanguage(this);
        appliedTheme = LocaleHelper.getSavedTheme(this);
        appliedUiMode = LocaleHelper.getSavedUiMode(this);
        isTvMode = LocaleHelper.UI_MODE_TV.equals(appliedUiMode);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        voiceSearchSupported = checkVoiceSearchAvailable();
        bindViews();
        applyUiLayoutMode();
        loadSettings();
        ensureRuntime();
        setupListeners();
        refreshSpeedDial();
        restoreOrCreateInitialTabs(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        if (intent != null && Intent.ACTION_VIEW.equals(intent.getAction()) && intent.getData() != null) {
            openNewTab(intent.getData().toString(), BrowserDataStore.isPrivateModeDefault(this), true);
        }
    }

    private boolean checkVoiceSearchAvailable() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        return intent.resolveActivity(getPackageManager()) != null;
    }

    private void bindViews() {
        mGeckoView = findViewById(R.id.geckoView);
        mGeckoView.setSaveEnabled(false);
        tvTopHost = findViewById(R.id.tvTopHost);
        phoneBottomDock = findViewById(R.id.phoneBottomDock);
        toolbarView = findViewById(R.id.toolbarView);
        bottomNavContainer = findViewById(R.id.bottomNavContainer);
        bottomNavPill = findViewById(R.id.bottomNavPill);
        tvNavCluster = findViewById(R.id.tvNavCluster);
        tvNavTabsButton = findViewById(R.id.tvNavTabsButton);
        privateBanner = findViewById(R.id.privateBanner);
        omniboxContainer = findViewById(R.id.omniboxContainer);
        findBar = findViewById(R.id.findBar);
        startPageContainer = findViewById(R.id.startPageContainer);
        suggestionsContainer = findViewById(R.id.suggestionsContainer);
        suggestionsListView = findViewById(R.id.suggestionsListView);
        startPageGridView = findViewById(R.id.startPageGridView);
        fullscreenContainer = findViewById(R.id.fullscreenContainer);

        startPageIncognitoPill = findViewById(R.id.startPageIncognitoPill);
        startPageIncognitoPillIcon = findViewById(R.id.startPageIncognitoPillIcon);
        startPageIncognitoPillText = findViewById(R.id.startPageIncognitoPillText);
        startPageSettingsBtn = findViewById(R.id.startPageSettingsBtn);
        startPageHeroLogo = findViewById(R.id.startPageHeroLogo);
        startPageHeroTitle = findViewById(R.id.startPageHeroTitle);
        startPageHeroSubtitle = findViewById(R.id.startPageHeroSubtitle);
        startPageSearchCard = findViewById(R.id.startPageSearchCard);
        startPageSearchIcon = findViewById(R.id.startPageSearchIcon);
        startPageSearchHint = findViewById(R.id.startPageSearchHint);
        startPageBookmarksBg = findViewById(R.id.startPageBookmarksBg);
        startPageHistoryBg = findViewById(R.id.startPageHistoryBg);
        startPageDownloadsBg = findViewById(R.id.startPageDownloadsBg);
        startPageIncognitoBg = findViewById(R.id.startPageIncognitoBg);
        startPageBookmarksLabel = findViewById(R.id.startPageBookmarksLabel);
        startPageHistoryLabel = findViewById(R.id.startPageHistoryLabel);
        startPageDownloadsLabel = findViewById(R.id.startPageDownloadsLabel);
        startPageIncognitoLabel = findViewById(R.id.startPageIncognitoLabel);
        startPageQuickAccessTitle = findViewById(R.id.startPageQuickAccessTitle);

        urlEditText = findViewById(R.id.urlEditText);
        findEditText = findViewById(R.id.findEditText);
        findMatchCountText = findViewById(R.id.findMatchCountText);
        mProgressView = findViewById(R.id.pageProgressView);
        securityIconView = findViewById(R.id.trackerCountViewImage);
        trackersCountView = findViewById(R.id.trackerCountView);
        tabsCountView = findViewById(R.id.tabsCountView);
        tvTabsCountView = findViewById(R.id.tvTabsCountView);
        navPlusPillBg = findViewById(R.id.navPlusPillBg);
        navHomePlusIcon = findViewById(R.id.navHomePlusIcon);
        clearUrlButton = findViewById(R.id.clearUrlButton);
        voiceSearchButton = findViewById(R.id.voiceSearchButton);
        startPageVoiceBtn = findViewById(R.id.startPageVoiceBtn);
        bookmarkButton = findViewById(R.id.bookmarkButton);
        shareUrlButton = findViewById(R.id.shareUrlButton);
        reloadStopButton = findViewById(R.id.reloadStopButton);
        navBackButton = findViewById(R.id.navBackButton);
        navForwardButton = findViewById(R.id.navForwardButton);
        tvNavBackButton = findViewById(R.id.tvNavBackButton);
        tvNavForwardButton = findViewById(R.id.tvNavForwardButton);
        tvNavHomeButton = findViewById(R.id.tvNavHomeButton);
        tvNavMoreButton = findViewById(R.id.tvNavMoreButton);

        suggestionsAdapter = new SuggestionsAdapter();
        suggestionsListView.setAdapter(suggestionsAdapter);

        speedDialAdapter = new SpeedDialAdapter();
        startPageGridView.setAdapter(speedDialAdapter);

        if (startPageVoiceBtn != null) {
            startPageVoiceBtn.setVisibility(voiceSearchSupported ? View.VISIBLE : View.GONE);
        }
        updateOmniboxActionButtons(false);
    }

    private void reparentToolbar(ViewGroup targetHost, int index) {
        if (toolbarView == null || targetHost == null) {
            return;
        }
        if (toolbarView.getParent() == targetHost) {
            return;
        }
        if (toolbarView.getParent() instanceof ViewGroup) {
            ((ViewGroup) toolbarView.getParent()).removeView(toolbarView);
        }
        if (index >= 0 && index <= targetHost.getChildCount()) {
            targetHost.addView(toolbarView, index);
        } else {
            targetHost.addView(toolbarView);
        }
    }

    private void applyUiLayoutMode() {
        float density = getResources().getDisplayMetrics().density;
        if (isTvMode) {
            reparentToolbar(tvTopHost, 0);
            if (tvTopHost != null) {
                tvTopHost.setVisibility(View.VISIBLE);
            }
            if (phoneBottomDock != null) {
                phoneBottomDock.setVisibility(View.GONE);
            }
            if (tvNavCluster != null) {
                tvNavCluster.setVisibility(View.VISIBLE);
            }
            if (tvNavTabsButton != null) {
                tvNavTabsButton.setVisibility(View.VISIBLE);
            }
            if (tvNavMoreButton != null) {
                tvNavMoreButton.setVisibility(View.VISIBLE);
            }
            if (bottomNavContainer != null) {
                bottomNavContainer.setVisibility(View.GONE);
            }
            if (toolbarView != null) {
                toolbarView.setVisibility(View.VISIBLE);
                toolbarView.setPadding(
                        Math.round(24 * density),
                        Math.round(10 * density),
                        Math.round(24 * density),
                        Math.round(10 * density)
                );
            }
            if (startPageContainer != null) {
                int pad = Math.round(28 * density);
                startPageContainer.setPadding(pad, pad, pad, pad);
            }
            if (startPageGridView != null) {
                startPageGridView.setColumnWidth(Math.round(148 * density));
            }
            int[] tvFocusIds = new int[]{
                    R.id.reloadStopButton,
                    R.id.bookmarkButton,
                    R.id.shareUrlButton,
                    R.id.clearUrlButton,
                    R.id.voiceSearchButton,
                    R.id.startPageVoiceBtn,
                    R.id.startPageIncognitoPill,
                    R.id.startPageSettingsBtn,
                    R.id.startPageBookmarksBtn,
                    R.id.startPageHistoryBtn,
                    R.id.startPageDownloadsBtn,
                    R.id.startPageIncognitoBtn,
                    R.id.securityShieldPill,
                    R.id.startPageAddShortcutBtn,
                    R.id.findPrevButton,
                    R.id.findNextButton,
                    R.id.findCloseButton
            };
            for (int id : tvFocusIds) {
                View v = findViewById(id);
                if (v != null) {
                    v.setBackgroundResource(R.drawable.bg_tv_focus);
                }
            }
        } else {
            reparentToolbar(phoneBottomDock, 0);
            if (tvTopHost != null) {
                tvTopHost.setVisibility(View.GONE);
            }
            if (phoneBottomDock != null && !isFullscreen) {
                phoneBottomDock.setVisibility(View.VISIBLE);
            }
            if (tvNavCluster != null) {
                tvNavCluster.setVisibility(View.GONE);
            }
            if (tvNavTabsButton != null) {
                tvNavTabsButton.setVisibility(View.GONE);
            }
            if (tvNavMoreButton != null) {
                tvNavMoreButton.setVisibility(View.GONE);
            }
            if (bottomNavContainer != null) {
                bottomNavContainer.setVisibility(View.VISIBLE);
            }
            if (toolbarView != null) {
                toolbarView.setPadding(
                        Math.round(14 * density),
                        Math.round(4 * density),
                        Math.round(14 * density),
                        Math.round(4 * density)
                );
            }
            if (startPageContainer != null) {
                int hPad = Math.round(20 * density);
                int vPad = Math.round(12 * density);
                startPageContainer.setPadding(hPad, vPad, hPad, vPad);
            }
            if (startPageGridView != null) {
                startPageGridView.setColumnWidth(Math.round(76 * density));
            }
        }
    }

    private void loadSettings() {
        searchUriBase = BrowserDataStore.getSearchBaseUrl(this);
        homepageUrl = BrowserDataStore.getHomepage(this);
        autoHideBar = BrowserDataStore.isAutoHideBar(this);
        hideWhiteSplash = BrowserDataStore.isHideWhiteSplash(this);
        searchSuggestionsEnabled = BrowserDataStore.isSearchSuggestionsEnabled(this);
        if (phoneBottomDock != null) {
            if (isTvMode || isFullscreen) {
                phoneBottomDock.setVisibility(View.GONE);
            } else if (!autoHideBar) {
                phoneBottomDock.setVisibility(View.VISIBLE);
            }
        }
    }

    private void ensureRuntime() {
        if (sRuntime == null) {
            ContentBlocking.Settings contentBlocking = new ContentBlocking.Settings.Builder()
                    .antiTracking(ContentBlocking.AntiTracking.DEFAULT)
                    .safeBrowsing(ContentBlocking.SafeBrowsing.DEFAULT)
                    .build();

            GeckoRuntimeSettings.Builder runtimeBuilder = new GeckoRuntimeSettings.Builder()
                    .consoleOutput(false)
                    .contentBlocking(contentBlocking);
            String configPath = writeGeckoConfigFile();
            if (configPath != null) {
                runtimeBuilder.configFilePath(configPath);
            }
            sRuntime = GeckoRuntime.create(getApplicationContext(), runtimeBuilder.build());
        }
    }

    private String writeGeckoConfigFile() {
        File configFile = new File(getApplicationContext().getFilesDir(), "geckoview-config.yaml");
        String yaml = "prefs:\n"
                + "  services.settings.poll_interval: 0\n"
                + "  services.settings.server: \"\"\n"
                + "  extensions.blocklist.enabled: false\n"
                + "  security.remote_settings.intermediates.enabled: false\n"
                + "  security.remote_settings.crlite_filters.enabled: false\n"
                + "  browser.chrome.errorReporter.enabled: false\n"
                + "  security.enterprise_roots.enabled: true\n"
                + "  security.tls.version.min: 3\n"
                + "  security.tls.version.max: 4\n"
                + "  security.OCSP.enabled: 1\n"
                + "  security.ssl.enable_ocsp_stapling: true\n";
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(configFile);
            fos.write(yaml.getBytes("UTF-8"));
            return configFile.getAbsolutePath();
        } catch (IOException e) {
            Log.w(TAG, "Failed to write GeckoView config file", e);
            return null;
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    private void setupListeners() {
        urlEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_GO
                        || actionId == EditorInfo.IME_ACTION_SEARCH
                        || actionId == EditorInfo.IME_ACTION_DONE
                        || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                    commitURL(urlEditText.getText().toString());
                    return true;
                }
                return false;
            }
        });

        urlEditText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                BrowserTab tab = getCurrentTab();
                boolean isStartPage = tab != null && BrowserDataStore.START_PAGE_URL.equals(tab.url);
                if (hasFocus) {
                    if (!isTvMode && toolbarView != null) {
                        toolbarView.setVisibility(View.VISIBLE);
                    }
                    urlEditText.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
                    if (tab != null && tab.url != null) {
                        urlEditText.setText(isStartPage ? "" : tab.url);
                        urlEditText.selectAll();
                    }
                    updateOmniboxActionButtons(true);
                    refreshOmniboxSuggestions(urlEditText.getText() != null ? urlEditText.getText().toString() : "");
                    suggestionsContainer.setVisibility(View.VISIBLE);
                } else {
                    hideOmniboxSuggestions();
                    urlEditText.setGravity(Gravity.CENTER);
                    if (tab != null && tab.url != null) {
                        urlEditText.setText(isStartPage ? "" : formatDisplayUrl(tab.url));
                    }
                    updateOmniboxActionButtons(false);
                    if (!isTvMode && toolbarView != null && isStartPage) {
                        toolbarView.setVisibility(View.GONE);
                    }
                }
            }
        });

        urlEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateOmniboxActionButtons(urlEditText.hasFocus());
                if (urlEditText.hasFocus()) {
                    refreshOmniboxSuggestions(s != null ? s.toString() : "");
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        suggestionsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (position >= 0 && position < suggestionItems.size()) {
                    SuggestionItem item = suggestionItems.get(position);
                    commitURL(item.actionValue);
                }
            }
        });

        clearUrlButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                urlEditText.setText("");
                urlEditText.requestFocus();
            }
        });

        voiceSearchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startVoiceSearch();
            }
        });

        bookmarkButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleBookmarkForCurrentTab();
            }
        });

        if (shareUrlButton != null) {
            shareUrlButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    shareCurrentPage();
                }
            });
        }

        if (shareUrlButton != null) {
            shareUrlButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    shareCurrentPage();
                }
            });
        }

        reloadStopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                BrowserTab tab = getCurrentTab();
                if (tab == null) {
                    return;
                }
                if (BrowserDataStore.START_PAGE_URL.equals(tab.url)) {
                    refreshSpeedDial();
                    return;
                }
                if (tab.session == null) {
                    return;
                }
                if (tab.isLoading) {
                    tab.session.stop();
                } else {
                    tab.session.reload();
                }
            }
        });

        findViewById(R.id.securityShieldPill).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                BrowserDialogs.showSecurityDialog(MainActivity.this, getCurrentTab(), MainActivity.this);
            }
        });

        View.OnClickListener backClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                BrowserTab tab = getCurrentTab();
                if (tab != null && tab.session != null && tab.canGoBack) {
                    tab.session.goBack();
                }
            }
        };
        View.OnLongClickListener historyLongClickListener = new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                BrowserDialogs.showTabSessionHistoryDialog(MainActivity.this, getCurrentTab());
                return true;
            }
        };
        navBackButton.setOnClickListener(backClickListener);
        navBackButton.setOnLongClickListener(historyLongClickListener);
        tvNavBackButton.setOnClickListener(backClickListener);
        tvNavBackButton.setOnLongClickListener(historyLongClickListener);

        View.OnClickListener forwardClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                BrowserTab tab = getCurrentTab();
                if (tab != null && tab.session != null && tab.canGoForward) {
                    tab.session.goForward();
                }
            }
        };
        navForwardButton.setOnClickListener(forwardClickListener);
        navForwardButton.setOnLongClickListener(historyLongClickListener);
        tvNavForwardButton.setOnClickListener(forwardClickListener);
        tvNavForwardButton.setOnLongClickListener(historyLongClickListener);

        View navHomeBtn = findViewById(R.id.navHomeButton);
        navHomeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                BrowserTab cur = getCurrentTab();
                openNewTab(homepageUrl, cur != null && cur.isPrivate, true);
            }
        });
        navHomeBtn.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                commitURL(homepageUrl);
                return true;
            }
        });
        tvNavHomeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                commitURL(homepageUrl);
            }
        });

        View.OnClickListener tabsClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clearOmniboxFocus();
                BrowserDialogs.showTabsDialog(MainActivity.this, tabs, activeTabIndex, homepageUrl, MainActivity.this);
            }
        };
        findViewById(R.id.navTabsButton).setOnClickListener(tabsClickListener);
        tvNavTabsButton.setOnClickListener(tabsClickListener);

        View.OnClickListener moreClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clearOmniboxFocus();
                showMoreMenuDialog();
            }
        };
        findViewById(R.id.navMoreButton).setOnClickListener(moreClickListener);
        tvNavMoreButton.setOnClickListener(moreClickListener);

        if (startPageIncognitoPill != null) {
            startPageIncognitoPill.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    BrowserTab cur = getCurrentTab();
                    boolean targetPrivate = cur == null || !cur.isPrivate;
                    openNewTab(homepageUrl, targetPrivate, true);
                }
            });
        }

        if (startPageSettingsBtn != null) {
            startPageSettingsBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startActivityForResult(new Intent(MainActivity.this, SettingsActivity.class), REQUEST_SETTINGS);
                }
            });
        }

        findViewById(R.id.startPageSearchCard).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isTvMode && toolbarView != null) {
                    toolbarView.setVisibility(View.VISIBLE);
                }
                urlEditText.requestFocus();
                InputMethodManager imm = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.showSoftInput(urlEditText, InputMethodManager.SHOW_IMPLICIT);
                }
            }
        });

        if (startPageVoiceBtn != null) {
            startPageVoiceBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startVoiceSearch();
                }
            });
        }

        View startPageBookmarksBtn = findViewById(R.id.startPageBookmarksBtn);
        if (startPageBookmarksBtn != null) {
            startPageBookmarksBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    BrowserDialogs.showSavedPagesDialog(MainActivity.this, true, MainActivity.this);
                }
            });
        }

        View startPageHistoryBtn = findViewById(R.id.startPageHistoryBtn);
        if (startPageHistoryBtn != null) {
            startPageHistoryBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    BrowserDialogs.showSavedPagesDialog(MainActivity.this, false, MainActivity.this);
                }
            });
        }

        View startPageDownloadsBtn = findViewById(R.id.startPageDownloadsBtn);
        if (startPageDownloadsBtn != null) {
            startPageDownloadsBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    BrowserHelper.showDownloadsDialog(MainActivity.this);
                }
            });
        }

        View startPageIncognitoBtn = findViewById(R.id.startPageIncognitoBtn);
        if (startPageIncognitoBtn != null) {
            startPageIncognitoBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    openNewTab(homepageUrl, true, true);
                }
            });
        }

        findViewById(R.id.startPageAddShortcutBtn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                BrowserDialogs.showAddQuickSiteDialog(MainActivity.this, MainActivity.this);
            }
        });

        startPageGridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (position >= 0 && position < quickSites.size()) {
                    commitURL(quickSites.get(position).url);
                }
            }
        });

        startPageGridView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                if (position < 0 || position >= quickSites.size()) {
                    return false;
                }
                final BrowserDataStore.SavedPage site = quickSites.get(position);
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle(site.title)
                        .setMessage(getString(R.string.remove_quick_site_confirm, site.title))
                        .setPositiveButton(R.string.delete_item, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                BrowserDataStore.removeQuickSite(MainActivity.this, site.url);
                                refreshSpeedDial();
                                Toast.makeText(MainActivity.this, R.string.quick_site_removed, Toast.LENGTH_SHORT).show();
                            }
                        })
                        .setNegativeButton(android.R.string.cancel, null)
                        .show();
                return true;
            }
        });

        findEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                runFindInPage(false);
                return true;
            }
        });

        findEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                runFindInPage(false);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        findViewById(R.id.findPrevButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                runFindInPage(true);
            }
        });

        findViewById(R.id.findNextButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                runFindInPage(false);
            }
        });

        findViewById(R.id.findCloseButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                closeFindBar();
            }
        });
    }

    private void refreshSpeedDial() {
        quickSites.clear();
        quickSites.addAll(BrowserDataStore.getQuickSites(this));
        if (speedDialAdapter != null) {
            speedDialAdapter.notifyDataSetChanged();
        }
    }

    private void clearOmniboxFocus() {
        if (urlEditText != null && urlEditText.hasFocus()) {
            urlEditText.clearFocus();
        }
        hideOmniboxSuggestions();
        hideKeyboard(this);
    }

    private void updateOmniboxActionButtons(boolean hasFocus) {
        BrowserTab tab = getCurrentTab();
        boolean isStartPage = tab == null || BrowserDataStore.START_PAGE_URL.equals(tab.url);
        boolean hasText = urlEditText.getText() != null && urlEditText.getText().length() > 0;
        boolean showClear = hasFocus && hasText;
        clearUrlButton.setVisibility(showClear ? View.VISIBLE : View.GONE);

        boolean showVoice = voiceSearchSupported && !showClear && (hasFocus || isStartPage || isTvMode);
        voiceSearchButton.setVisibility(showVoice ? View.VISIBLE : View.GONE);

        boolean showPageActions = !hasFocus && !isStartPage;
        bookmarkButton.setVisibility(showPageActions ? View.VISIBLE : View.GONE);
        if (shareUrlButton != null) {
            shareUrlButton.setVisibility(showPageActions ? View.VISIBLE : View.GONE);
        }
        reloadStopButton.setVisibility((!hasFocus && !isStartPage) || isTvMode ? View.VISIBLE : View.GONE);
    }

    private void startVoiceSearch() {
        if (!voiceSearchSupported) {
            Toast.makeText(this, R.string.voice_search_unavailable, Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_WEB_SEARCH);
            startActivityForResult(intent, REQUEST_VOICE_SEARCH);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, R.string.voice_search_unavailable, Toast.LENGTH_SHORT).show();
        }
    }

    private void hideOmniboxSuggestions() {
        if (pendingSuggestionRunnable != null) {
            mainHandler.removeCallbacks(pendingSuggestionRunnable);
        }
        suggestionsContainer.setVisibility(View.GONE);
    }

    private void refreshOmniboxSuggestions(final String rawQuery) {
        final String query = rawQuery != null ? rawQuery.trim() : "";
        final String lower = query.toLowerCase(Locale.ROOT);
        suggestionItems.clear();

        if (query.isEmpty() || BrowserDataStore.START_PAGE_URL.equalsIgnoreCase(query)) {
            List<BrowserDataStore.SavedPage> bookmarks = BrowserDataStore.getBookmarks(this);
            int added = 0;
            for (BrowserDataStore.SavedPage b : bookmarks) {
                suggestionItems.add(new SuggestionItem(SuggestionItem.TYPE_BOOKMARK, b.title, b.url, b.url));
                added++;
                if (added >= 4) {
                    break;
                }
            }
            for (BrowserDataStore.SavedPage quick : BrowserDataStore.getQuickSites(this)) {
                suggestionItems.add(new SuggestionItem(SuggestionItem.TYPE_QUICK, quick.title, quick.url, quick.url));
            }
            suggestionsAdapter.notifyDataSetChanged();
            return;
        }

        int count = 0;
        for (BrowserDataStore.SavedPage b : BrowserDataStore.getBookmarks(this)) {
            if ((b.title != null && b.title.toLowerCase(Locale.ROOT).contains(lower))
                    || (b.url != null && b.url.toLowerCase(Locale.ROOT).contains(lower))) {
                suggestionItems.add(new SuggestionItem(SuggestionItem.TYPE_BOOKMARK, b.title, b.url, b.url));
                count++;
                if (count >= 3) {
                    break;
                }
            }
        }

        count = 0;
        for (BrowserDataStore.SavedPage h : BrowserDataStore.getHistory(this)) {
            if ((h.title != null && h.title.toLowerCase(Locale.ROOT).contains(lower))
                    || (h.url != null && h.url.toLowerCase(Locale.ROOT).contains(lower))) {
                suggestionItems.add(new SuggestionItem(SuggestionItem.TYPE_HISTORY, h.title, h.url, h.url));
                count++;
                if (count >= 4) {
                    break;
                }
            }
        }

        suggestionsAdapter.notifyDataSetChanged();

        BrowserTab current = getCurrentTab();
        boolean isPrivate = current != null && current.isPrivate;
        if (searchSuggestionsEnabled && !isPrivate && !query.contains("://") && query.length() >= 2) {
            if (pendingSuggestionRunnable != null) {
                mainHandler.removeCallbacks(pendingSuggestionRunnable);
            }
            final int token = ++suggestionRequestToken;
            pendingSuggestionRunnable = new Runnable() {
                @Override
                public void run() {
                    fetchRemoteSearchSuggestions(query, token);
                }
            };
            mainHandler.postDelayed(pendingSuggestionRunnable, 220);
        }
    }

    private void fetchRemoteSearchSuggestions(final String query, final int token) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                HttpURLConnection conn = null;
                BufferedReader reader = null;
                final List<String> fetched = new ArrayList<>();
                try {
                    URL url = new URL("https://duckduckgo.com/ac/?q=" + Uri.encode(query) + "&type=list");
                    conn = (HttpURLConnection) url.openConnection();
                    if (conn instanceof HttpsURLConnection) {
                        Tls12SocketFactory.enableTls12OnPreLollipop((HttpsURLConnection) conn);
                    }
                    conn.setConnectTimeout(3000);
                    conn.setReadTimeout(3000);
                    conn.setRequestMethod("GET");
                    if (conn.getResponseCode() == 200) {
                        reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
                        StringBuilder sb = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) {
                            sb.append(line);
                        }
                        String body = sb.toString().trim();
                        if (body.startsWith("[")) {
                            JSONArray root = new JSONArray(body);
                            if (root.length() >= 2 && root.optJSONArray(1) != null) {
                                JSONArray arr = root.getJSONArray(1);
                                for (int i = 0; i < arr.length() && fetched.size() < 5; i++) {
                                    String s = arr.optString(i, "");
                                    if (!s.isEmpty()) {
                                        fetched.add(s);
                                    }
                                }
                            } else {
                                for (int i = 0; i < root.length() && fetched.size() < 5; i++) {
                                    JSONObject obj = root.optJSONObject(i);
                                    if (obj != null) {
                                        String phrase = obj.optString("phrase", "");
                                        if (!phrase.isEmpty()) {
                                            fetched.add(phrase);
                                        }
                                    }
                                }
                            }
                        }
                    }
                } catch (Exception ignored) {
                } finally {
                    if (reader != null) {
                        try {
                            reader.close();
                        } catch (IOException ignored) {
                        }
                    }
                    if (conn != null) {
                        conn.disconnect();
                    }
                }

                if (!fetched.isEmpty()) {
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (isActivityUnavailable() || token != suggestionRequestToken || !urlEditText.hasFocus()) {
                                return;
                            }
                            for (String s : fetched) {
                                suggestionItems.add(new SuggestionItem(
                                        SuggestionItem.TYPE_SEARCH,
                                        s,
                                        getString(R.string.search_suggestions),
                                        s
                                ));
                            }
                            suggestionsAdapter.notifyDataSetChanged();
                        }
                    });
                }
            }
        }).start();
    }

    private boolean isActivityUnavailable() {
        if (isFinishing()) {
            return true;
        }
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 && isDestroyed();
    }

    private void restoreOrCreateInitialTabs(Intent launchIntent) {
        boolean hasViewUri = launchIntent != null
                && Intent.ACTION_VIEW.equals(launchIntent.getAction())
                && launchIntent.getData() != null;

        List<BrowserDataStore.SavedTab> savedTabs = BrowserDataStore.loadTabs(this);
        boolean defaultDesktop = BrowserDataStore.isDesktopModeDefault(this);
        boolean defaultPrivate = BrowserDataStore.isPrivateModeDefault(this);

        if (savedTabs.isEmpty()) {
            String startUrl = hasViewUri ? launchIntent.getData().toString() : homepageUrl;
            openNewTab(startUrl, defaultPrivate, true);
        } else {
            for (BrowserDataStore.SavedTab st : savedTabs) {
                String url = (st.url == null || st.url.isEmpty()) ? homepageUrl : st.url;
                BrowserTab tab = new BrowserTab(nextTabId++, url, st.title, st.isPrivate, defaultDesktop);
                tabs.add(tab);
            }
            activeTabIndex = BrowserDataStore.loadActiveTabIndex(this, tabs.size());
            if (hasViewUri) {
                openNewTab(launchIntent.getData().toString(), defaultPrivate, true);
            } else {
                switchToTab(activeTabIndex, true);
            }
        }
    }

    private GeckoSessionSettings buildSessionSettings(boolean isPrivate, boolean desktopMode) {
        GeckoSessionSettings.Builder builder = new GeckoSessionSettings.Builder();
        builder.allowJavascript(BrowserDataStore.isJavascriptEnabled(this));
        builder.useTrackingProtection(BrowserDataStore.isTrackingProtectionEnabled(this));
        builder.usePrivateMode(isPrivate);

        String customUa = BrowserDataStore.getCustomUserAgent(this);
        if (!customUa.isEmpty()) {
            builder.userAgentOverride(customUa);
        } else if (desktopMode) {
            builder.userAgentMode(GeckoSessionSettings.USER_AGENT_MODE_DESKTOP);
            builder.displayMode(GeckoSessionSettings.DISPLAY_MODE_BROWSER);
            builder.viewportMode(GeckoSessionSettings.VIEWPORT_MODE_DESKTOP);
        } else {
            builder.userAgentMode(GeckoSessionSettings.USER_AGENT_MODE_MOBILE);
            builder.viewportMode(GeckoSessionSettings.VIEWPORT_MODE_MOBILE);
        }
        return builder.build();
    }

    private GeckoSession createSessionForTab(final BrowserTab tab) {
        GeckoSession session = new GeckoSession(buildSessionSettings(tab.isPrivate, tab.desktopMode));
        session.setProgressDelegate(new TabProgressDelegate(tab));
        session.setNavigationDelegate(new TabNavigationDelegate(tab));
        session.setContentDelegate(new TabContentDelegate(tab));
        session.setContentBlockingDelegate(new TabBlockingDelegate(tab));
        session.setPromptDelegate(new BrowserPromptDelegate(this, this));
        session.setHistoryDelegate(new GeckoSession.HistoryDelegate() {
            @Override
            public void onHistoryStateChange(@NonNull GeckoSession s, @NonNull HistoryList historyList) {
                tab.historyList = historyList;
            }
        });
        session.setScrollDelegate(new GeckoSession.ScrollDelegate() {
            @Override
            public void onScrollChanged(@NonNull GeckoSession s, int scrollX, int scrollY) {
                if (!autoHideBar || isTvMode || tab != getCurrentTab() || isFullscreen || phoneBottomDock == null) {
                    return;
                }
                int delta = scrollY - scrollAnchorY;
                boolean visible = phoneBottomDock.getVisibility() == View.VISIBLE;
                if (delta > 36 && visible) {
                    phoneBottomDock.setVisibility(View.GONE);
                    scrollAnchorY = scrollY;
                } else if (delta < -36 && !visible) {
                    phoneBottomDock.setVisibility(View.VISIBLE);
                    scrollAnchorY = scrollY;
                } else if ((visible && scrollY < scrollAnchorY) || (!visible && scrollY > scrollAnchorY)) {
                    scrollAnchorY = scrollY;
                }
            }
        });
        session.setPermissionDelegate(new GeckoSession.PermissionDelegate() {
            @Override
            public void onContentPermissionRequest(@NonNull GeckoSession s, @Nullable String uri, int type, @NonNull Callback callback) {
                callback.reject();
            }

            @Override
            public void onMediaPermissionRequest(@NonNull GeckoSession s, @NonNull String uri, @Nullable MediaSource[] video, @Nullable MediaSource[] audio, @NonNull MediaCallback callback) {
                callback.reject();
            }

            @Override
            public void onAndroidPermissionsRequest(@NonNull GeckoSession s, @Nullable String[] permissions, @NonNull Callback callback) {
                callback.reject();
            }
        });
        session.open(sRuntime);
        return session;
    }

    private BrowserTab openNewTab(String initialUrl, boolean isPrivate, boolean switchToIt) {
        return openNewTabInternal(initialUrl, isPrivate, switchToIt, true);
    }

    private BrowserTab openNewTabInternal(String initialUrl, boolean isPrivate, boolean switchToIt, boolean loadInitial) {
        String trimmed = initialUrl != null ? initialUrl.trim() : "";
        String targetUrl = loadInitial
                ? (trimmed.isEmpty() ? homepageUrl : trimmed)
                : trimmed;
        BrowserTab tab = new BrowserTab(
                nextTabId++,
                targetUrl,
                "",
                isPrivate,
                BrowserDataStore.isDesktopModeDefault(this)
        );
        tabs.add(tab);
        if (switchToIt) {
            switchToTab(tabs.size() - 1, loadInitial);
        } else {
            refreshTabCount();
        }
        return tab;
    }

    private void switchToTab(int index, boolean loadIfNeeded) {
        clearOmniboxFocus();
        if (tabs.isEmpty()) {
            openNewTab(homepageUrl, BrowserDataStore.isPrivateModeDefault(this), true);
            return;
        }
        if (index < 0 || index >= tabs.size()) {
            index = 0;
        }
        BrowserTab previousTab = getCurrentTab();
        BrowserTab tab = tabs.get(index);
        if (previousTab != null && previousTab != tab) {
            resetFullscreenIfNeeded();
            if (previousTab.session != null) {
                previousTab.session.setActive(false);
            }
        }
        activeTabIndex = index;

        boolean createdNow = false;
        if (tab.session == null) {
            tab.session = createSessionForTab(tab);
            createdNow = true;
        }
        tab.session.setActive(true);

        if (mGeckoView.getSession() != tab.session) {
            if (mGeckoView.getSession() != null) {
                mGeckoView.releaseSession();
            }
            mGeckoView.setSession(tab.session);
        }

        if (createdNow && loadIfNeeded) {
            String toLoad = (tab.url == null || tab.url.trim().isEmpty()) ? homepageUrl : tab.url;
            loadNormalizedInput(tab, toLoad);
        }

        scrollAnchorY = 0;
        if (phoneBottomDock != null && !isFullscreen && !isTvMode) {
            phoneBottomDock.setVisibility(View.VISIBLE);
        }
        updateChromeForTab(tab);
    }

    private void closeTab(int index) {
        if (index < 0 || index >= tabs.size()) {
            return;
        }
        if (index == activeTabIndex) {
            resetFullscreenIfNeeded();
        }
        BrowserTab removed = tabs.remove(index);
        if (!removed.isPrivate && removed.url != null && !removed.url.isEmpty()) {
            closedTabs.add(0, new BrowserDataStore.SavedTab(removed.url, removed.title, false));
            if (closedTabs.size() > MAX_CLOSED_TABS) {
                closedTabs.remove(closedTabs.size() - 1);
            }
        }
        if (removed.session != null) {
            if (mGeckoView.getSession() == removed.session) {
                mGeckoView.releaseSession();
            }
            removed.session.close();
            removed.session = null;
        }

        if (tabs.isEmpty()) {
            openNewTab(homepageUrl, BrowserDataStore.isPrivateModeDefault(this), true);
            return;
        }

        if (activeTabIndex >= tabs.size()) {
            activeTabIndex = tabs.size() - 1;
        } else if (index < activeTabIndex) {
            activeTabIndex--;
        }
        switchToTab(activeTabIndex, true);
    }

    private void reopenLastClosedTab() {
        if (closedTabs.isEmpty()) {
            Toast.makeText(this, R.string.no_closed_tabs, Toast.LENGTH_SHORT).show();
            return;
        }
        BrowserDataStore.SavedTab last = closedTabs.remove(0);
        openNewTab(last.url, false, true);
    }

    @Nullable
    private BrowserTab getCurrentTab() {
        if (activeTabIndex >= 0 && activeTabIndex < tabs.size()) {
            return tabs.get(activeTabIndex);
        }
        return null;
    }

    private void commitURL(String rawInput) {
        hideOmniboxSuggestions();
        BrowserTab tab = getCurrentTab();
        if (tab == null) {
            return;
        }
        if (tab.session == null) {
            tab.session = createSessionForTab(tab);
        }
        if (mGeckoView.getSession() != tab.session) {
            if (mGeckoView.getSession() != null) {
                mGeckoView.releaseSession();
            }
            mGeckoView.setSession(tab.session);
        }
        loadNormalizedInput(tab, rawInput);
        mGeckoView.requestFocus();
        hideKeyboard(this);
    }

    private void loadNormalizedInput(BrowserTab tab, String rawInput) {
        String input = rawInput != null ? rawInput.trim() : "";
        String target;
        if (input.isEmpty()) {
            target = homepageUrl;
        } else if (BrowserDataStore.START_PAGE_URL.equalsIgnoreCase(input)) {
            target = BrowserDataStore.START_PAGE_URL;
        } else if (input.startsWith("http://") || input.startsWith("https://")
                || input.startsWith("about:") || input.startsWith("file://")) {
            target = input;
        } else if (!input.contains(" ") && (input.contains(".") || input.startsWith("localhost"))) {
            target = "https://" + input;
        } else {
            target = searchUriBase + Uri.encode(input);
        }
        tab.url = target;
        if (BrowserDataStore.START_PAGE_URL.equals(target)) {
            tab.isResettingToHome = true;
            tab.title = getString(R.string.app_name);
            tab.isSecure = true;
            tab.certIssuer = null;
            tab.certValidUntil = null;
            tab.isLoading = false;
            tab.progress = 100;
            if (tab.session != null) {
                tab.session.stop();
                tab.session.loadUri("about:blank");
            }
            updateChromeForTab(tab);
        } else {
            tab.isResettingToHome = false;
            startPageContainer.setVisibility(View.GONE);
            mGeckoView.setVisibility(View.VISIBLE);
            if (tab.session != null) {
                tab.session.loadUri(target);
            }
        }
    }

    private String formatDisplayUrl(String rawUrl) {
        if (rawUrl == null || rawUrl.isEmpty() || BrowserDataStore.START_PAGE_URL.equals(rawUrl)) {
            return "";
        }
        try {
            Uri uri = Uri.parse(rawUrl);
            String host = uri.getHost();
            if (host != null && !host.isEmpty()) {
                return host.startsWith("www.") ? host.substring(4) : host;
            }
        } catch (Exception ignored) {
        }
        return rawUrl;
    }

    private void updateChromeForTab(BrowserTab tab) {
        if (tab == null) {
            return;
        }
        boolean isStartPage = BrowserDataStore.START_PAGE_URL.equals(tab.url);
        startPageContainer.setVisibility(isStartPage ? View.VISIBLE : View.GONE);

        if (!isTvMode && toolbarView != null) {
            boolean showOmniboxBar = !isStartPage || urlEditText.hasFocus();
            toolbarView.setVisibility(showOmniboxBar ? View.VISIBLE : View.GONE);
        }

        if (!urlEditText.hasFocus()) {
            urlEditText.setGravity(Gravity.CENTER);
            urlEditText.setText(isStartPage ? "" : formatDisplayUrl(tab.url));
        }
        if (!isFullscreen) {
            privateBanner.setVisibility(tab.isPrivate ? View.VISIBLE : View.GONE);
        }
        omniboxContainer.setBackgroundResource(tab.isPrivate ? R.drawable.bg_omnibox_private : R.drawable.bg_omnibox);
        updateOmniboxActionButtons(urlEditText.hasFocus());
        applyThemeForPrivateState(tab.isPrivate);

        updateSecurityIndicator(tab);
        updateBookmarkStar(tab.url);
        updateNavButtons(tab);
        updateProgressUI(tab);
        refreshTabCount();
    }

    private void applyThemeForPrivateState(boolean isPrivate) {
        if (currentTabPrivate != isPrivate) {
            currentTabPrivate = isPrivate;
            if (speedDialAdapter != null) {
                speedDialAdapter.notifyDataSetChanged();
            }
        }

        if (startPageContainer != null) {
            if (isPrivate) {
                startPageContainer.setBackgroundColor(Color.parseColor("#202124"));
            } else {
                startPageContainer.setBackgroundColor(ContextCompat.getColor(this, R.color.bg_app));
            }
        }

        if (startPageHeroLogo != null) {
            if (isPrivate) {
                startPageHeroLogo.setImageResource(R.drawable.ic_incognito_glasses);
                ImageViewCompat.setImageTintList(startPageHeroLogo, ColorStateList.valueOf(Color.parseColor("#E8EAED")));
            } else {
                startPageHeroLogo.setImageResource(R.drawable.ic_chrome_ring);
                ImageViewCompat.setImageTintList(startPageHeroLogo, null);
            }
        }

        int primaryText = isPrivate ? Color.parseColor("#F1F5F9") : ContextCompat.getColor(this, R.color.text_primary);
        int secondaryText = isPrivate ? Color.parseColor("#9AA0A6") : ContextCompat.getColor(this, R.color.text_secondary);

        if (startPageHeroTitle != null) {
            startPageHeroTitle.setText(isPrivate ? R.string.incognito_title : R.string.app_name);
            startPageHeroTitle.setTextColor(primaryText);
        }
        if (startPageHeroSubtitle != null) {
            startPageHeroSubtitle.setText(isPrivate ? R.string.private_browsing_banner : R.string.start_page_subtitle);
            startPageHeroSubtitle.setTextColor(secondaryText);
        }
        if (startPageSearchCard != null) {
            startPageSearchCard.setBackgroundResource(isPrivate ? R.drawable.bg_ios_omnibox_ntp_private : R.drawable.bg_ios_omnibox_ntp);
        }
        if (startPageSearchIcon != null) {
            ImageViewCompat.setImageTintList(startPageSearchIcon, ColorStateList.valueOf(secondaryText));
        }
        if (startPageSearchHint != null) {
            startPageSearchHint.setTextColor(secondaryText);
        }

        int squircleBg = isPrivate ? R.drawable.bg_ios_squircle_private : R.drawable.bg_ios_squircle;
        if (startPageIncognitoPill != null) {
            startPageIncognitoPill.setBackgroundResource(squircleBg);
        }
        if (startPageIncognitoPillText != null) {
            startPageIncognitoPillText.setText(isPrivate ? R.string.done_action : R.string.private_badge);
            startPageIncognitoPillText.setTextColor(primaryText);
        }
        if (startPageIncognitoPillIcon != null) {
            int pillIconTint = isPrivate ? Color.parseColor("#A8C7FA") : ContextCompat.getColor(this, R.color.accent_private);
            ImageViewCompat.setImageTintList(startPageIncognitoPillIcon, ColorStateList.valueOf(pillIconTint));
        }
        if (startPageSettingsBtn != null) {
            startPageSettingsBtn.setBackgroundResource(squircleBg);
            int settingsTint = isPrivate ? Color.parseColor("#E8EAED") : ContextCompat.getColor(this, R.color.nav_icon_tint);
            ImageViewCompat.setImageTintList(startPageSettingsBtn, ColorStateList.valueOf(settingsTint));
        }

        if (startPageBookmarksBg != null) {
            startPageBookmarksBg.setBackgroundResource(squircleBg);
        }
        if (startPageHistoryBg != null) {
            startPageHistoryBg.setBackgroundResource(squircleBg);
        }
        if (startPageDownloadsBg != null) {
            startPageDownloadsBg.setBackgroundResource(squircleBg);
        }
        if (startPageIncognitoBg != null) {
            startPageIncognitoBg.setBackgroundResource(squircleBg);
        }
        if (startPageBookmarksLabel != null) {
            startPageBookmarksLabel.setTextColor(secondaryText);
        }
        if (startPageHistoryLabel != null) {
            startPageHistoryLabel.setTextColor(secondaryText);
        }
        if (startPageDownloadsLabel != null) {
            startPageDownloadsLabel.setTextColor(secondaryText);
        }
        if (startPageIncognitoLabel != null) {
            startPageIncognitoLabel.setTextColor(secondaryText);
        }
        if (startPageQuickAccessTitle != null) {
            startPageQuickAccessTitle.setTextColor(primaryText);
        }

        if (bottomNavPill != null) {
            bottomNavPill.setBackgroundResource(isPrivate ? R.drawable.bg_ios_glass_bar_private : R.drawable.bg_ios_glass_bar);
        }
        if (navPlusPillBg != null) {
            navPlusPillBg.setBackgroundResource(isPrivate ? R.drawable.bg_ios_squircle_private : R.drawable.bg_shield_pill);
        }
        int bottomIconColor = isPrivate ? Color.parseColor("#E8EAED") : ContextCompat.getColor(this, R.color.nav_icon_tint);
        if (navHomePlusIcon != null) {
            ImageViewCompat.setImageTintList(navHomePlusIcon, ColorStateList.valueOf(bottomIconColor));
        }
        if (tabsCountView != null) {
            tabsCountView.setTextColor(bottomIconColor);
            tabsCountView.setBackgroundResource(isPrivate ? R.drawable.bg_tab_badge_private : R.drawable.bg_tab_badge);
        }
        View moreBtn = findViewById(R.id.navMoreButton);
        if (moreBtn instanceof AppCompatImageButton) {
            ImageViewCompat.setImageTintList((AppCompatImageButton) moreBtn, ColorStateList.valueOf(bottomIconColor));
        }
    }

    private void updateSecurityIndicator(BrowserTab tab) {
        trackersCountView.setText(String.valueOf(tab.blockedTrackers.size()));
        int tintColor;
        boolean isStartPage = BrowserDataStore.START_PAGE_URL.equals(tab.url);
        if (tab.isPrivate) {
            securityIconView.setImageResource(R.drawable.ic_shield_private);
            tintColor = ContextCompat.getColor(this, R.color.accent_private);
        } else if (tab.isSecure || isStartPage || (tab.url != null && tab.url.startsWith("https://"))) {
            securityIconView.setImageResource(R.drawable.ic_lock_secure);
            tintColor = ContextCompat.getColor(this, R.color.accent_secure);
        } else {
            securityIconView.setImageResource(R.drawable.ic_lock_insecure);
            tintColor = ContextCompat.getColor(this, R.color.accent_insecure);
        }
        ImageViewCompat.setImageTintList(securityIconView, ColorStateList.valueOf(tintColor));
    }

    private void updateBookmarkStar(String url) {
        boolean bookmarked = BrowserDataStore.isBookmarked(this, url);
        bookmarkButton.setImageResource(bookmarked ? R.drawable.ic_star_filled : R.drawable.ic_star_outline);
        int tint = ContextCompat.getColor(this, bookmarked ? R.color.accent_star : R.color.text_secondary);
        ImageViewCompat.setImageTintList(bookmarkButton, ColorStateList.valueOf(tint));
    }

    private void updateNavButtons(BrowserTab tab) {
        int activeTint = tab.isPrivate
                ? Color.parseColor("#E8EAED")
                : ContextCompat.getColor(this, R.color.nav_icon_tint);
        int disabledTint = tab.isPrivate
                ? Color.parseColor("#5F6368")
                : ContextCompat.getColor(this, R.color.nav_icon_disabled);

        navBackButton.setEnabled(tab.canGoBack);
        ImageViewCompat.setImageTintList(navBackButton, ColorStateList.valueOf(tab.canGoBack ? activeTint : disabledTint));
        if (tvNavBackButton != null) {
            int tvActive = ContextCompat.getColor(this, R.color.nav_icon_tint);
            int tvDisabled = ContextCompat.getColor(this, R.color.nav_icon_disabled);
            tvNavBackButton.setEnabled(tab.canGoBack);
            ImageViewCompat.setImageTintList(tvNavBackButton, ColorStateList.valueOf(tab.canGoBack ? tvActive : tvDisabled));
        }

        navForwardButton.setEnabled(tab.canGoForward);
        ImageViewCompat.setImageTintList(navForwardButton, ColorStateList.valueOf(tab.canGoForward ? activeTint : disabledTint));
        if (tvNavForwardButton != null) {
            int tvActive = ContextCompat.getColor(this, R.color.nav_icon_tint);
            int tvDisabled = ContextCompat.getColor(this, R.color.nav_icon_disabled);
            tvNavForwardButton.setEnabled(tab.canGoForward);
            ImageViewCompat.setImageTintList(tvNavForwardButton, ColorStateList.valueOf(tab.canGoForward ? tvActive : tvDisabled));
        }

        reloadStopButton.setImageResource(tab.isLoading ? R.drawable.ic_close : R.drawable.ic_refresh);
        reloadStopButton.setContentDescription(getString(tab.isLoading ? R.string.stop_loading : R.string.reload_page));
    }

    private void updateProgressUI(BrowserTab tab) {
        if (BrowserDataStore.START_PAGE_URL.equals(tab.url)) {
            mProgressView.setVisibility(View.GONE);
            mGeckoView.setVisibility(View.INVISIBLE);
            return;
        }
        mProgressView.setProgress(tab.progress);
        if (tab.isLoading && tab.progress > 0 && tab.progress < 100) {
            mProgressView.setVisibility(View.VISIBLE);
            if (hideWhiteSplash) {
                mGeckoView.setVisibility(View.INVISIBLE);
            }
        } else {
            mProgressView.setVisibility(View.GONE);
            mGeckoView.setVisibility(View.VISIBLE);
        }
    }

    private void refreshTabCount() {
        String countText = String.valueOf(tabs.size());
        tabsCountView.setText(countText);
        if (tvTabsCountView != null) {
            tvTabsCountView.setText(countText);
        }
    }

    private void toggleBookmarkForCurrentTab() {
        BrowserTab tab = getCurrentTab();
        if (tab == null || tab.url == null || tab.url.trim().isEmpty()
                || BrowserDataStore.START_PAGE_URL.equals(tab.url)) {
            return;
        }
        if (BrowserDataStore.isBookmarked(this, tab.url)) {
            BrowserDataStore.removeBookmark(this, tab.url);
            Toast.makeText(this, R.string.bookmark_removed, Toast.LENGTH_SHORT).show();
        } else {
            BrowserDataStore.addBookmark(this, tab.title, tab.url);
            Toast.makeText(this, R.string.bookmark_added, Toast.LENGTH_SHORT).show();
        }
        updateBookmarkStar(tab.url);
    }

    private void showMoreMenuDialog() {
        final BrowserTab tab = getCurrentTab();
        final boolean isDesktop = tab != null && tab.desktopMode;

        BrowserDialogs.showChromeMoreMenuSheet(this, isDesktop, new BrowserDialogs.MoreMenuActions() {
            @Override
            public void onOpenBookmarks() {
                BrowserDialogs.showSavedPagesDialog(MainActivity.this, true, MainActivity.this);
            }

            @Override
            public void onOpenHistory() {
                BrowserDialogs.showSavedPagesDialog(MainActivity.this, false, MainActivity.this);
            }

            @Override
            public void onOpenDownloads() {
                BrowserHelper.showDownloadsDialog(MainActivity.this);
            }

            @Override
            public void onOpenSettings() {
                Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
                startActivityForResult(intent, REQUEST_SETTINGS);
            }

            @Override
            public void onNewNormalTab() {
                openNewTab(homepageUrl, false, true);
            }

            @Override
            public void onNewPrivateTab() {
                openNewTab(homepageUrl, true, true);
            }

            @Override
            public void onReopenClosedTab() {
                reopenLastClosedTab();
            }

            @Override
            public void onDuplicateCurrentTab() {
                if (tab != null) {
                    openNewTab(tab.url, tab.isPrivate, true);
                }
            }

            @Override
            public void onOpenFindInPage() {
                openFindBar();
            }

            @Override
            public void onToggleDesktopSite() {
                toggleDesktopModeForCurrentTab();
            }

            @Override
            public void onShareCurrentPage() {
                shareCurrentPage();
            }

            @Override
            public void onCopyCurrentLink() {
                onCopyLink(tab != null ? tab.url : null);
            }

            @Override
            public void onOpenExternalApp() {
                openCurrentPageInExternalApp();
            }

            @Override
            public void onAddToHomeScreen() {
                if (tab != null) {
                    BrowserHelper.addShortcutToHomeScreen(MainActivity.this, tab.title, tab.url);
                }
            }

            @Override
            public void onQuickWipe() {
                confirmQuickWipe();
            }
        });
    }

    private void confirmQuickWipe() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.quick_wipe_confirm_title)
                .setMessage(R.string.quick_wipe_confirm_message)
                .setPositiveButton(R.string.quick_wipe_action, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        BrowserDataStore.clearHistory(MainActivity.this);
                        clearBrowsingSiteData();
                        closedTabs.clear();
                        onCloseAllTabs();
                        Toast.makeText(MainActivity.this, R.string.quick_wipe_done, Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void openCurrentPageInExternalApp() {
        BrowserTab tab = getCurrentTab();
        if (tab == null || tab.url == null || tab.url.isEmpty() || BrowserDataStore.START_PAGE_URL.equals(tab.url)) {
            return;
        }
        try {
            Intent ext = new Intent(Intent.ACTION_VIEW, Uri.parse(tab.url));
            ext.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(Intent.createChooser(ext, getString(R.string.menu_open_external)));
        } catch (Exception e) {
            Toast.makeText(this, R.string.external_app_error, Toast.LENGTH_SHORT).show();
        }
    }

    private void toggleDesktopModeForCurrentTab() {
        BrowserTab tab = getCurrentTab();
        if (tab == null || tab.session == null) {
            return;
        }
        tab.desktopMode = !tab.desktopMode;
        tab.session.getSettings().setUserAgentMode(
                tab.desktopMode ? GeckoSessionSettings.USER_AGENT_MODE_DESKTOP : GeckoSessionSettings.USER_AGENT_MODE_MOBILE
        );
        tab.session.getSettings().setViewportMode(
                tab.desktopMode ? GeckoSessionSettings.VIEWPORT_MODE_DESKTOP : GeckoSessionSettings.VIEWPORT_MODE_MOBILE
        );
        if (!BrowserDataStore.START_PAGE_URL.equals(tab.url)) {
            tab.session.reload();
        }
    }

    private void shareCurrentPage() {
        BrowserTab tab = getCurrentTab();
        if (tab == null || tab.url == null || tab.url.isEmpty() || BrowserDataStore.START_PAGE_URL.equals(tab.url)) {
            return;
        }
        onShareLink(tab.title, tab.url);
    }

    private void openFindBar() {
        findBar.setVisibility(View.VISIBLE);
        findMatchCountText.setVisibility(View.GONE);
        findEditText.requestFocus();
        InputMethodManager imm = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.showSoftInput(findEditText, InputMethodManager.SHOW_IMPLICIT);
        }
    }

    private void closeFindBar() {
        findBar.setVisibility(View.GONE);
        findMatchCountText.setVisibility(View.GONE);
        findEditText.setText("");
        BrowserTab tab = getCurrentTab();
        if (tab != null && tab.session != null) {
            tab.session.getFinder().clear();
        }
        hideKeyboard(this);
    }

    private void runFindInPage(boolean backwards) {
        BrowserTab tab = getCurrentTab();
        if (tab == null || tab.session == null) {
            return;
        }
        String query = findEditText.getText() != null ? findEditText.getText().toString() : "";
        if (query.isEmpty()) {
            tab.session.getFinder().clear();
            findMatchCountText.setVisibility(View.GONE);
            return;
        }
        int flags = backwards ? GeckoSession.FINDER_FIND_BACKWARDS : 0;
        tab.session.getFinder().find(query, flags).accept(new GeckoResult.Consumer<GeckoSession.FinderResult>() {
            @Override
            public void accept(@Nullable GeckoSession.FinderResult res) {
                if (isActivityUnavailable()) {
                    return;
                }
                if (res != null && findBar.getVisibility() == View.VISIBLE) {
                    findMatchCountText.setText(getString(R.string.find_matches, res.current, res.total));
                    findMatchCountText.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    private void applyFullscreenSystemUi(boolean fullScreen) {
        View decor = getWindow().getDecorView();
        if (fullScreen) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                decor.setSystemUiVisibility(
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                | View.SYSTEM_UI_FLAG_FULLSCREEN
                                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                );
            } else {
                decor.setSystemUiVisibility(
                        View.SYSTEM_UI_FLAG_LOW_PROFILE | View.SYSTEM_UI_FLAG_FULLSCREEN
                );
            }
        } else {
            decor.setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
        }
    }

    private void resetFullscreenIfNeeded() {
        if (isFullscreen) {
            isFullscreen = false;
            if (isTvMode) {
                if (tvTopHost != null) {
                    tvTopHost.setVisibility(View.VISIBLE);
                }
                if (toolbarView != null) {
                    toolbarView.setVisibility(View.VISIBLE);
                }
            } else {
                if (phoneBottomDock != null) {
                    phoneBottomDock.setVisibility(View.VISIBLE);
                }
                if (bottomNavContainer != null) {
                    bottomNavContainer.setVisibility(View.VISIBLE);
                }
                BrowserTab cur = getCurrentTab();
                boolean isStartPage = cur != null && BrowserDataStore.START_PAGE_URL.equals(cur.url);
                if (toolbarView != null) {
                    toolbarView.setVisibility((!isStartPage || urlEditText.hasFocus()) ? View.VISIBLE : View.GONE);
                }
            }
            if (fullscreenContainer != null) {
                fullscreenContainer.setVisibility(View.GONE);
            }
            applyFullscreenSystemUi(false);
        }
    }

    // ---- BrowserDialogs.BrowserActions ----

    @Override
    public void onSwitchTab(int index) {
        switchToTab(index, true);
    }

    @Override
    public void onNewTab(String url, boolean isPrivate) {
        openNewTab(url, isPrivate, true);
    }

    @Override
    public void onCloseTabById(long tabId) {
        for (int i = 0; i < tabs.size(); i++) {
            if (tabs.get(i).id == tabId) {
                closeTab(i);
                return;
            }
        }
    }

    @Override
    public void onCloseOtherTabs() {
        BrowserTab current = getCurrentTab();
        if (current == null || tabs.size() <= 1) {
            return;
        }
        for (int i = tabs.size() - 1; i >= 0; i--) {
            BrowserTab t = tabs.get(i);
            if (t != current) {
                if (!t.isPrivate && t.url != null && !t.url.isEmpty()) {
                    closedTabs.add(0, new BrowserDataStore.SavedTab(t.url, t.title, false));
                    if (closedTabs.size() > MAX_CLOSED_TABS) {
                        closedTabs.remove(closedTabs.size() - 1);
                    }
                }
                if (t.session != null) {
                    if (mGeckoView != null && mGeckoView.getSession() == t.session) {
                        mGeckoView.releaseSession();
                    }
                    t.session.close();
                    t.session = null;
                }
                tabs.remove(i);
            }
        }
        activeTabIndex = Math.max(0, tabs.indexOf(current));
        refreshTabCount();
    }

    @Override
    public void onCloseAllTabs() {
        resetFullscreenIfNeeded();
        for (BrowserTab t : tabs) {
            if (t.session != null) {
                if (mGeckoView.getSession() == t.session) {
                    mGeckoView.releaseSession();
                }
                t.session.close();
                t.session = null;
            }
        }
        tabs.clear();
        openNewTab(homepageUrl, BrowserDataStore.isPrivateModeDefault(this), true);
    }

    @Override
    public void onOpenUrlInCurrentTab(String url) {
        commitURL(url);
    }

    @Override
    public void onBookmarkChanged() {
        BrowserTab cur = getCurrentTab();
        if (cur != null) {
            updateBookmarkStar(cur.url);
        }
    }

    @Override
    public void onClearSiteData() {
        clearBrowsingSiteData();
    }

    @Override
    public void onShareLink(String title, String url) {
        if (url == null || url.isEmpty()) {
            return;
        }
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        if (title != null && !title.isEmpty()) {
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, title);
        }
        shareIntent.putExtra(Intent.EXTRA_TEXT, url);
        try {
            startActivity(Intent.createChooser(shareIntent, getString(R.string.share_chooser_title)));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, R.string.external_app_error, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onCopyLink(String url) {
        if (url == null || url.isEmpty()) {
            return;
        }
        ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (cm != null) {
            cm.setPrimaryClip(ClipData.newPlainText("URL", url));
            Toast.makeText(this, R.string.link_copied, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onQuickSitesUpdated() {
        refreshSpeedDial();
    }

    @Override
    public long getActiveTabId() {
        BrowserTab cur = getCurrentTab();
        return cur != null ? cur.id : -1L;
    }

    // ---- BrowserPromptDelegate.FilePickerHost ----

    @Override
    public void onRequestFilePicker(
            @NonNull GeckoSession.PromptDelegate.FilePrompt prompt,
            @NonNull GeckoResult<GeckoSession.PromptDelegate.PromptResponse> result) {
        if (pendingFileResult != null && pendingFilePrompt != null) {
            pendingFileResult.complete(pendingFilePrompt.dismiss());
        }
        pendingFilePrompt = prompt;
        pendingFileResult = result;

        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        if (prompt.mimeTypes != null && prompt.mimeTypes.length > 0) {
            if (prompt.mimeTypes.length == 1) {
                intent.setType(prompt.mimeTypes[0]);
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                intent.putExtra(Intent.EXTRA_MIME_TYPES, prompt.mimeTypes);
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2
                && prompt.type == GeckoSession.PromptDelegate.FilePrompt.Type.MULTIPLE) {
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        }
        try {
            startActivityForResult(Intent.createChooser(intent, prompt.title), REQUEST_FILE_PICKER);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, R.string.file_upload_error, Toast.LENGTH_SHORT).show();
            pendingFileResult.complete(pendingFilePrompt.dismiss());
            pendingFilePrompt = null;
            pendingFileResult = null;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_SETTINGS) {
            loadSettings();
            String newLang = LocaleHelper.getSavedLanguage(this);
            String newTheme = LocaleHelper.getSavedTheme(this);
            String newUiMode = LocaleHelper.getSavedUiMode(this);
            if (!newLang.equals(appliedLanguage) || !newTheme.equals(appliedTheme) || !newUiMode.equals(appliedUiMode)) {
                BrowserDataStore.saveTabs(this, tabs, activeTabIndex);
                recreate();
                return;
            }
            boolean js = BrowserDataStore.isJavascriptEnabled(this);
            boolean tp = BrowserDataStore.isTrackingProtectionEnabled(this);
            String customUa = BrowserDataStore.getCustomUserAgent(this);
            for (BrowserTab tab : tabs) {
                if (tab.session != null) {
                    GeckoSessionSettings s = tab.session.getSettings();
                    s.setAllowJavascript(js);
                    s.setUseTrackingProtection(tp);
                    s.setUserAgentOverride(customUa.isEmpty() ? null : customUa);
                }
            }
        } else if (requestCode == REQUEST_FILE_PICKER && pendingFilePrompt != null && pendingFileResult != null) {
            GeckoSession.PromptDelegate.FilePrompt prompt = pendingFilePrompt;
            GeckoResult<GeckoSession.PromptDelegate.PromptResponse> result = pendingFileResult;
            pendingFilePrompt = null;
            pendingFileResult = null;

            if (resultCode == RESULT_OK && data != null) {
                if (data.getClipData() != null
                        && data.getClipData().getItemCount() > 0) {
                    ClipData clip = data.getClipData();
                    Uri[] uris = new Uri[clip.getItemCount()];
                    for (int i = 0; i < clip.getItemCount(); i++) {
                        uris[i] = clip.getItemAt(i).getUri();
                    }
                    result.complete(prompt.confirm(this, uris));
                } else if (data.getData() != null) {
                    result.complete(prompt.confirm(this, data.getData()));
                } else {
                    result.complete(prompt.dismiss());
                }
            } else {
                result.complete(prompt.dismiss());
            }
        } else if (requestCode == REQUEST_VOICE_SEARCH && resultCode == RESULT_OK && data != null) {
            ArrayList<String> matches = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (matches != null && !matches.isEmpty() && matches.get(0) != null) {
                commitURL(matches.get(0));
            }
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            clearOmniboxFocus();
            showMoreMenuDialog();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onBackPressed() {
        if (suggestionsContainer.getVisibility() == View.VISIBLE) {
            clearOmniboxFocus();
            return;
        }
        if (findBar.getVisibility() == View.VISIBLE) {
            closeFindBar();
            return;
        }
        BrowserTab tab = getCurrentTab();
        if (isFullscreen && tab != null && tab.session != null) {
            tab.session.exitFullScreen();
            return;
        }
        if (tab != null && tab.session != null && tab.canGoBack) {
            tab.session.goBack();
            return;
        }
        super.onBackPressed();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        BrowserDataStore.saveTabs(this, tabs, activeTabIndex);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onPause() {
        BrowserDataStore.saveTabs(this, tabs, activeTabIndex);
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (pendingSuggestionRunnable != null) {
            mainHandler.removeCallbacks(pendingSuggestionRunnable);
        }
        if (pendingFilePrompt != null && pendingFileResult != null) {
            pendingFileResult.complete(pendingFilePrompt.dismiss());
            pendingFilePrompt = null;
            pendingFileResult = null;
        }
        if (mGeckoView != null && mGeckoView.getSession() != null) {
            mGeckoView.releaseSession();
        }
        for (BrowserTab tab : tabs) {
            if (tab.session != null) {
                tab.session.close();
                tab.session = null;
            }
        }
        super.onDestroy();
    }

    private void hideKeyboard(Activity activity) {
        InputMethodManager imm = (InputMethodManager) activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
        if (imm == null) {
            return;
        }
        View view = activity.getCurrentFocus();
        if (view == null) {
            view = new View(activity);
        }
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    // ---- GeckoSession Delegates ----

    private boolean shouldIgnoreBlankReset(BrowserTab tab, @Nullable String url) {
        return (tab.isResettingToHome || BrowserDataStore.START_PAGE_URL.equals(tab.url))
                && "about:blank".equalsIgnoreCase(url);
    }

    private class TabProgressDelegate implements GeckoSession.ProgressDelegate {
        private final BrowserTab tab;

        TabProgressDelegate(BrowserTab tab) {
            this.tab = tab;
        }

        @Override
        public void onPageStart(@NonNull GeckoSession session, @NonNull String url) {
            if (shouldIgnoreBlankReset(tab, url)) {
                return;
            }
            tab.isResettingToHome = false;
            tab.url = url;
            tab.isLoading = true;
            tab.progress = 10;
            tab.blockedTrackers.clear();
            tab.isSecure = url.startsWith("https://");
            tab.certIssuer = null;
            tab.certValidUntil = null;
            scrollAnchorY = 0;
            if (tab == getCurrentTab()) {
                if (!isFullscreen && !isTvMode && phoneBottomDock != null) {
                    phoneBottomDock.setVisibility(View.VISIBLE);
                }
                updateChromeForTab(tab);
            }
        }

        @Override
        public void onPageStop(@NonNull GeckoSession session, boolean success) {
            if (tab.isResettingToHome || BrowserDataStore.START_PAGE_URL.equals(tab.url)) {
                tab.isResettingToHome = false;
                tab.isLoading = false;
                tab.progress = 100;
                if (tab == getCurrentTab()) {
                    updateNavButtons(tab);
                    updateProgressUI(tab);
                }
                return;
            }
            tab.isLoading = false;
            tab.progress = 100;
            if (!tab.isPrivate && success) {
                BrowserDataStore.addHistory(MainActivity.this, tab.title, tab.url);
            }
            if (tab == getCurrentTab()) {
                updateNavButtons(tab);
                updateProgressUI(tab);
            }
        }

        @Override
        public void onProgressChange(@NonNull GeckoSession session, int progress) {
            if (tab.isResettingToHome || BrowserDataStore.START_PAGE_URL.equals(tab.url)) {
                return;
            }
            tab.progress = progress;
            tab.isLoading = progress > 0 && progress < 100;
            if (tab == getCurrentTab()) {
                updateNavButtons(tab);
                updateProgressUI(tab);
            }
        }

        @Override
        public void onSecurityChange(@NonNull GeckoSession session, @NonNull SecurityInformation securityInfo) {
            if (tab.isResettingToHome || BrowserDataStore.START_PAGE_URL.equals(tab.url)) {
                return;
            }
            tab.isSecure = securityInfo.isSecure;
            X509Certificate cert = securityInfo.certificate;
            if (cert != null) {
                tab.certIssuer = cert.getIssuerX500Principal() != null
                        ? cert.getIssuerX500Principal().getName()
                        : null;
                tab.certValidUntil = cert.getNotAfter() != null
                        ? DateFormat.getDateInstance(DateFormat.MEDIUM).format(cert.getNotAfter())
                        : null;
            } else {
                tab.certIssuer = null;
                tab.certValidUntil = null;
            }
            if (tab == getCurrentTab()) {
                updateSecurityIndicator(tab);
            }
        }
    }

    private class TabNavigationDelegate implements GeckoSession.NavigationDelegate {
        private final BrowserTab tab;

        TabNavigationDelegate(BrowserTab tab) {
            this.tab = tab;
        }

        @Override
        public void onLocationChange(@NonNull GeckoSession session, @Nullable String url) {
            if (url != null) {
                if (shouldIgnoreBlankReset(tab, url)) {
                    return;
                }
                tab.isResettingToHome = false;
                tab.url = url;
                tab.isSecure = url.startsWith("https://");
                if (tab == getCurrentTab()) {
                    if (!isTvMode && toolbarView != null) {
                        toolbarView.setVisibility(View.VISIBLE);
                    }
                    if (!urlEditText.hasFocus()) {
                        urlEditText.setGravity(Gravity.CENTER);
                        urlEditText.setText(formatDisplayUrl(url));
                    }
                    startPageContainer.setVisibility(View.GONE);
                    updateOmniboxActionButtons(urlEditText.hasFocus());
                    updateSecurityIndicator(tab);
                    updateBookmarkStar(url);
                }
            }
        }

        @Override
        public void onCanGoBack(@NonNull GeckoSession session, boolean canGoBack) {
            tab.canGoBack = canGoBack;
            if (tab == getCurrentTab()) {
                updateNavButtons(tab);
            }
        }

        @Override
        public void onCanGoForward(@NonNull GeckoSession session, boolean canGoForward) {
            tab.canGoForward = canGoForward;
            if (tab == getCurrentTab()) {
                updateNavButtons(tab);
            }
        }

        @Nullable
        @Override
        public GeckoResult<AllowOrDeny> onLoadRequest(@NonNull GeckoSession session, @NonNull LoadRequest request) {
            String uri = request.uri;
            if (uri != null && !uri.startsWith("http://") && !uri.startsWith("https://")
                    && !uri.startsWith("about:") && !uri.startsWith("file://")
                    && !uri.startsWith("data:") && !uri.startsWith("blob:")
                    && !uri.startsWith("javascript:")) {
                try {
                    Intent ext = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
                    ext.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(ext);
                } catch (ActivityNotFoundException e) {
                    Toast.makeText(MainActivity.this, R.string.external_app_error, Toast.LENGTH_SHORT).show();
                } catch (Exception ignored) {
                }
                return GeckoResult.fromValue(AllowOrDeny.DENY);
            }
            return GeckoResult.fromValue(AllowOrDeny.ALLOW);
        }

        @Nullable
        @Override
        public GeckoResult<GeckoSession> onNewSession(@NonNull GeckoSession session, @NonNull String uri) {
            BrowserTab newTab = openNewTabInternal(uri, tab.isPrivate, true, false);
            if (newTab.session == null) {
                newTab.session = createSessionForTab(newTab);
            }
            return GeckoResult.fromValue(newTab.session);
        }
    }

    private class TabContentDelegate implements GeckoSession.ContentDelegate {
        private final BrowserTab tab;

        TabContentDelegate(BrowserTab tab) {
            this.tab = tab;
        }

        @Override
        public void onTitleChange(@NonNull GeckoSession session, @Nullable String title) {
            if (tab.isResettingToHome || BrowserDataStore.START_PAGE_URL.equals(tab.url)) {
                return;
            }
            if (title != null) {
                tab.title = title;
                if (!tab.isPrivate && tab.url != null && !tab.url.isEmpty()) {
                    BrowserDataStore.addHistory(MainActivity.this, title, tab.url);
                }
            }
        }

        @Override
        public void onFullScreen(@NonNull GeckoSession session, boolean fullScreen) {
            if (tab != getCurrentTab()) {
                return;
            }
            isFullscreen = fullScreen;
            if (isTvMode) {
                if (tvTopHost != null) {
                    tvTopHost.setVisibility(fullScreen ? View.GONE : View.VISIBLE);
                }
                toolbarView.setVisibility(fullScreen ? View.GONE : View.VISIBLE);
            } else {
                if (phoneBottomDock != null) {
                    phoneBottomDock.setVisibility(fullScreen ? View.GONE : View.VISIBLE);
                }
                bottomNavContainer.setVisibility(fullScreen ? View.GONE : View.VISIBLE);
                if (!fullScreen) {
                    boolean isStartPage = BrowserDataStore.START_PAGE_URL.equals(tab.url);
                    toolbarView.setVisibility((!isStartPage || urlEditText.hasFocus()) ? View.VISIBLE : View.GONE);
                }
            }
            if (fullScreen) {
                findBar.setVisibility(View.GONE);
            }
            privateBanner.setVisibility((!fullScreen && tab.isPrivate) ? View.VISIBLE : View.GONE);
            fullscreenContainer.setVisibility(View.GONE);
            applyFullscreenSystemUi(fullScreen);
        }

        @Override
        public void onExternalResponse(@NonNull GeckoSession session, @NonNull GeckoSession.WebResponseInfo response) {
            BrowserHelper.promptAndEnqueueDownload(
                    MainActivity.this,
                    response.uri,
                    response.filename,
                    response.contentType
            );
        }

        @Override
        public void onContextMenu(
                @NonNull GeckoSession session,
                int screenX,
                int screenY,
                @NonNull final ContextElement element) {
            if (isActivityUnavailable()) {
                return;
            }
            final List<String> labels = new ArrayList<>();
            final List<Runnable> actions = new ArrayList<>();

            if (element.linkUri != null && !element.linkUri.isEmpty()) {
                final String link = element.linkUri;
                labels.add(getString(R.string.context_open_new_tab));
                actions.add(new Runnable() {
                    @Override
                    public void run() {
                        openNewTab(link, false, true);
                    }
                });

                labels.add(getString(R.string.context_open_private_tab));
                actions.add(new Runnable() {
                    @Override
                    public void run() {
                        openNewTab(link, true, true);
                    }
                });

                labels.add(getString(R.string.context_copy_link));
                actions.add(new Runnable() {
                    @Override
                    public void run() {
                        onCopyLink(link);
                    }
                });

                labels.add(getString(R.string.context_share_link));
                actions.add(new Runnable() {
                    @Override
                    public void run() {
                        onShareLink(element.title, link);
                    }
                });
            }

            if (element.srcUri != null && !element.srcUri.isEmpty()) {
                final String src = element.srcUri;
                labels.add(getString(R.string.context_open_image));
                actions.add(new Runnable() {
                    @Override
                    public void run() {
                        openNewTab(src, tab.isPrivate, true);
                    }
                });

                labels.add(getString(R.string.context_download_image));
                actions.add(new Runnable() {
                    @Override
                    public void run() {
                        BrowserHelper.promptAndEnqueueDownload(MainActivity.this, src, null, "image/*");
                    }
                });

                labels.add(getString(R.string.context_copy_image_url));
                actions.add(new Runnable() {
                    @Override
                    public void run() {
                        onCopyLink(src);
                    }
                });
            }

            if (labels.isEmpty()) {
                return;
            }

            new AlertDialog.Builder(MainActivity.this)
                    .setTitle(element.linkUri != null ? element.linkUri : element.srcUri)
                    .setItems(labels.toArray(new String[0]), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if (which >= 0 && which < actions.size()) {
                                actions.get(which).run();
                            }
                        }
                    })
                    .show();
        }
    }

    private class TabBlockingDelegate implements ContentBlocking.Delegate {
        private final BrowserTab tab;

        TabBlockingDelegate(BrowserTab tab) {
            this.tab = tab;
        }

        @Override
        public void onContentBlocked(@NonNull GeckoSession session, @NonNull ContentBlocking.BlockEvent event) {
            if (event.uri == null) {
                return;
            }
            try {
                URL url = new URL(event.uri);
                String host = url.getHost();
                if (host != null && !host.isEmpty() && !tab.blockedTrackers.contains(host)) {
                    tab.blockedTrackers.add(host);
                    if (tab == getCurrentTab()) {
                        trackersCountView.setText(String.valueOf(tab.blockedTrackers.size()));
                    }
                }
            } catch (MalformedURLException e) {
                Log.w(TAG, "Invalid blocked URI: " + event.uri, e);
            }
        }
    }

    // ---- Models & Adapters ----

    private static class SuggestionItem {
        static final int TYPE_BOOKMARK = 1;
        static final int TYPE_HISTORY = 2;
        static final int TYPE_SEARCH = 3;
        static final int TYPE_QUICK = 4;

        final int type;
        final String title;
        final String subtitle;
        final String actionValue;

        SuggestionItem(int type, String title, String subtitle, String actionValue) {
            this.type = type;
            this.title = title;
            this.subtitle = subtitle;
            this.actionValue = actionValue;
        }
    }

    private class SuggestionsAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return suggestionItems.size();
        }

        @Override
        public Object getItem(int position) {
            return suggestionItems.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View row = convertView;
            if (row == null) {
                row = LayoutInflater.from(MainActivity.this).inflate(R.layout.item_suggestion, parent, false);
            }
            SuggestionItem item = suggestionItems.get(position);
            AppCompatImageView icon = row.findViewById(R.id.suggestionIcon);
            TextView title = row.findViewById(R.id.suggestionTitle);
            TextView subtitle = row.findViewById(R.id.suggestionSubtitle);

            title.setText(item.title != null && !item.title.isEmpty() ? item.title : item.subtitle);
            subtitle.setText(item.subtitle);

            if (item.type == SuggestionItem.TYPE_BOOKMARK) {
                icon.setImageResource(R.drawable.ic_star_filled);
                ImageViewCompat.setImageTintList(icon, ColorStateList.valueOf(ContextCompat.getColor(MainActivity.this, R.color.accent_star)));
            } else if (item.type == SuggestionItem.TYPE_QUICK) {
                icon.setImageResource(R.drawable.ic_home);
                ImageViewCompat.setImageTintList(icon, ColorStateList.valueOf(ContextCompat.getColor(MainActivity.this, R.color.accent_blue)));
            } else {
                icon.setImageResource(R.drawable.ic_search);
                ImageViewCompat.setImageTintList(icon, ColorStateList.valueOf(ContextCompat.getColor(MainActivity.this, R.color.text_secondary)));
            }
            return row;
        }
    }

    private class SpeedDialAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return quickSites.size();
        }

        @Override
        public Object getItem(int position) {
            return quickSites.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View tile = convertView;
            if (tile == null) {
                tile = LayoutInflater.from(MainActivity.this).inflate(R.layout.item_speed_dial, parent, false);
            }
            BrowserDataStore.SavedPage site = quickSites.get(position);
            View iconBg = tile.findViewById(R.id.speedDialIconBg);
            AppCompatImageView icon = tile.findViewById(R.id.speedDialIcon);
            TextView titleView = tile.findViewById(R.id.speedDialTitle);

            if (iconBg != null) {
                iconBg.setBackgroundResource(currentTabPrivate ? R.drawable.bg_ios_squircle_private : R.drawable.bg_ios_squircle);
            }
            if (icon != null) {
                int iconTint = currentTabPrivate
                        ? Color.parseColor("#A8C7FA")
                        : ContextCompat.getColor(MainActivity.this, R.color.accent_blue);
                ImageViewCompat.setImageTintList(icon, ColorStateList.valueOf(iconTint));
            }
            titleView.setTextColor(currentTabPrivate
                    ? Color.parseColor("#F1F5F9")
                    : ContextCompat.getColor(MainActivity.this, R.color.text_primary));
            titleView.setText(site.title != null && !site.title.isEmpty() ? site.title : site.url);
            return tile;
        }
    }
}
