package com.example.geckobrowser;

import org.mozilla.geckoview.GeckoSession;

import java.util.ArrayList;

public class BrowserTab {
    public final long id;
    public String url;
    public String title;
    public final boolean isPrivate;
    public boolean desktopMode;
    public boolean canGoBack;
    public boolean canGoForward;
    public boolean isLoading;
    public int progress;
    public boolean isSecure;
    public boolean isResettingToHome;
    public String certIssuer;
    public String certValidUntil;
    public final ArrayList<String> blockedTrackers = new ArrayList<>();
    public GeckoSession.HistoryDelegate.HistoryList historyList;
    public GeckoSession session;

    public BrowserTab(long id, String url, String title, boolean isPrivate, boolean desktopMode) {
        this.id = id;
        this.url = url != null ? url : "";
        this.title = title != null ? title : "";
        this.isPrivate = isPrivate;
        this.desktopMode = desktopMode;
        this.isSecure = this.url.startsWith("https://");
    }
}
