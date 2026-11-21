package com.example.geckobrowser;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.core.content.ContextCompat;
import androidx.core.widget.ImageViewCompat;

import org.mozilla.geckoview.GeckoSession;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class BrowserDialogs {

    public interface BrowserActions {
        void onSwitchTab(int index);

        void onNewTab(String url, boolean isPrivate);

        void onCloseTabById(long tabId);

        void onCloseOtherTabs();

        void onCloseAllTabs();

        void onOpenUrlInCurrentTab(String url);

        void onBookmarkChanged();

        void onClearSiteData();

        void onShareLink(String title, String url);

        void onCopyLink(String url);

        void onQuickSitesUpdated();

        long getActiveTabId();
    }

    public interface MoreMenuActions {
        void onOpenBookmarks();

        void onOpenHistory();

        void onOpenDownloads();

        void onOpenSettings();

        void onNewNormalTab();

        void onNewPrivateTab();

        void onReopenClosedTab();

        void onDuplicateCurrentTab();

        void onOpenFindInPage();

        void onToggleDesktopSite();

        void onShareCurrentPage();

        void onCopyCurrentLink();

        void onOpenExternalApp();

        void onAddToHomeScreen();

        void onQuickWipe();
    }

    private BrowserDialogs() {
    }

    private static boolean isActivityUnavailable(Activity activity) {
        if (activity == null || activity.isFinishing()) {
            return true;
        }
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 && activity.isDestroyed();
    }

    public static void showChromeMoreMenuSheet(
            final Activity activity,
            boolean isDesktopMode,
            final MoreMenuActions menuActions) {
        if (isActivityUnavailable(activity) || menuActions == null) {
            return;
        }
        final Dialog dialog = new Dialog(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_chrome_more_sheet);

        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            int screenWidthDp = activity.getResources().getConfiguration().screenWidthDp;
            int width = ViewGroup.LayoutParams.MATCH_PARENT;
            if (screenWidthDp >= 600) {
                width = Math.round(540 * activity.getResources().getDisplayMetrics().density);
            }
            window.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
            window.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
            window.setWindowAnimations(R.style.IosSheetAnimation);
        }

        TextView desktopLabel = dialog.findViewById(R.id.sheetDesktopLabel);
        if (desktopLabel != null) {
            desktopLabel.setText(isDesktopMode ? R.string.menu_desktop_site_on : R.string.menu_desktop_site_off);
        }

        bindSheetAction(dialog, R.id.sheetActionBookmarks, new Runnable() {
            @Override
            public void run() {
                menuActions.onOpenBookmarks();
            }
        });
        bindSheetAction(dialog, R.id.sheetActionHistory, new Runnable() {
            @Override
            public void run() {
                menuActions.onOpenHistory();
            }
        });
        bindSheetAction(dialog, R.id.sheetActionDownloads, new Runnable() {
            @Override
            public void run() {
                menuActions.onOpenDownloads();
            }
        });
        bindSheetAction(dialog, R.id.sheetActionSettings, new Runnable() {
            @Override
            public void run() {
                menuActions.onOpenSettings();
            }
        });
        bindSheetAction(dialog, R.id.sheetRowNewTab, new Runnable() {
            @Override
            public void run() {
                menuActions.onNewNormalTab();
            }
        });
        bindSheetAction(dialog, R.id.sheetRowPrivateTab, new Runnable() {
            @Override
            public void run() {
                menuActions.onNewPrivateTab();
            }
        });
        bindSheetAction(dialog, R.id.sheetRowReopenTab, new Runnable() {
            @Override
            public void run() {
                menuActions.onReopenClosedTab();
            }
        });
        bindSheetAction(dialog, R.id.sheetRowDuplicateTab, new Runnable() {
            @Override
            public void run() {
                menuActions.onDuplicateCurrentTab();
            }
        });
        bindSheetAction(dialog, R.id.sheetRowFindInPage, new Runnable() {
            @Override
            public void run() {
                menuActions.onOpenFindInPage();
            }
        });
        bindSheetAction(dialog, R.id.sheetRowDesktopSite, new Runnable() {
            @Override
            public void run() {
                menuActions.onToggleDesktopSite();
            }
        });
        bindSheetAction(dialog, R.id.sheetRowShare, new Runnable() {
            @Override
            public void run() {
                menuActions.onShareCurrentPage();
            }
        });
        bindSheetAction(dialog, R.id.sheetRowCopyLink, new Runnable() {
            @Override
            public void run() {
                menuActions.onCopyCurrentLink();
            }
        });
        bindSheetAction(dialog, R.id.sheetRowOpenExternal, new Runnable() {
            @Override
            public void run() {
                menuActions.onOpenExternalApp();
            }
        });
        bindSheetAction(dialog, R.id.sheetRowAddHome, new Runnable() {
            @Override
            public void run() {
                menuActions.onAddToHomeScreen();
            }
        });
        bindSheetAction(dialog, R.id.sheetRowQuickWipe, new Runnable() {
            @Override
            public void run() {
                menuActions.onQuickWipe();
            }
        });

        dialog.show();
    }

    private static void bindSheetAction(final Dialog dialog, int viewId, final Runnable action) {
        View v = dialog.findViewById(viewId);
        if (v != null) {
            v.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    dialog.dismiss();
                    action.run();
                }
            });
        }
    }

    public static void showSecurityDialog(final Activity activity, BrowserTab tab, final BrowserActions actions) {
        if (tab == null || isActivityUnavailable(activity)) {
            return;
        }
        boolean secure = tab.isSecure || (tab.url != null && (tab.url.startsWith("https://") || BrowserDataStore.START_PAGE_URL.equals(tab.url)));
        String status = activity.getString(secure ? R.string.connection_secure : R.string.connection_insecure);

        StringBuilder message = new StringBuilder();
        if (tab.url != null && !tab.url.isEmpty()) {
            message.append(tab.url).append("\n\n");
        }
        message.append(status);
        if (tab.certIssuer != null && !tab.certIssuer.isEmpty()) {
            message.append("\n").append(activity.getString(R.string.cert_issuer_label)).append(": ").append(tab.certIssuer);
        }
        if (tab.certValidUntil != null && !tab.certValidUntil.isEmpty()) {
            message.append("\n").append(activity.getString(R.string.cert_valid_until_label)).append(": ").append(tab.certValidUntil);
        }
        message.append("\n\n");
        message.append(activity.getString(R.string.trackers_blocked)).append(": ").append(tab.blockedTrackers.size());
        if (tab.blockedTrackers.isEmpty()) {
            message.append("\n").append(activity.getString(R.string.no_trackers_blocked));
        } else {
            for (String host : tab.blockedTrackers) {
                message.append("\n• ").append(host);
            }
        }

        new AlertDialog.Builder(activity)
                .setTitle(R.string.security_info_title)
                .setMessage(message.toString())
                .setPositiveButton(android.R.string.ok, null)
                .setNeutralButton(R.string.clear_site_data_btn, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        actions.onClearSiteData();
                        Toast.makeText(activity, R.string.site_data_cleared, Toast.LENGTH_SHORT).show();
                    }
                })
                .show();
    }

    public static void showTabSessionHistoryDialog(Activity activity, final BrowserTab tab) {
        if (isActivityUnavailable(activity) || tab == null || tab.session == null || tab.historyList == null || tab.historyList.isEmpty()) {
            return;
        }
        final int size = tab.historyList.size();
        String[] labels = new String[size];
        for (int i = 0; i < size; i++) {
            GeckoSession.HistoryDelegate.HistoryItem item = tab.historyList.get(i);
            String title = item.getTitle();
            String uri = item.getUri();
            labels[i] = (title != null && !title.isEmpty()) ? title : uri;
        }
        int currentIdx = tab.historyList.getCurrentIndex();
        new AlertDialog.Builder(activity)
                .setTitle(R.string.tab_history_title)
                .setSingleChoiceItems(labels, currentIdx, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        tab.session.gotoHistoryIndex(which);
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    public static void showTabsDialog(
            final Activity activity,
            final List<BrowserTab> tabs,
            final int activeTabIndex,
            final String homepageUrl,
            final BrowserActions actions) {
        if (isActivityUnavailable(activity)) {
            return;
        }
        final Dialog dialog = new Dialog(activity, R.style.AppTheme);
        dialog.setContentView(R.layout.dialog_tabs_grid);

        Window window = dialog.getWindow();
        if (window != null) {
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            window.setWindowAnimations(R.style.IosSheetAnimation);
        }

        GridView gridView = dialog.findViewById(R.id.tabsGridView);
        final TextView countBadge = dialog.findViewById(R.id.tabsGridCountBadge);
        View doneBtn = dialog.findViewById(R.id.tabsDoneButton);
        View closeAllBtn = dialog.findViewById(R.id.tabsCloseAllBtn);
        View newTabBtn = dialog.findViewById(R.id.tabsNewTabBtn);
        View newPrivateBtn = dialog.findViewById(R.id.tabsNewPrivateBtn);

        int screenWidthDp = activity.getResources().getConfiguration().screenWidthDp;
        if (screenWidthDp >= 600) {
            gridView.setNumColumns(3);
        } else {
            gridView.setNumColumns(2);
        }
        countBadge.setText(String.valueOf(tabs.size()));

        final TabCardsAdapter adapter = new TabCardsAdapter(activity, tabs, countBadge, dialog, actions);
        gridView.setAdapter(adapter);

        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                dialog.dismiss();
                actions.onSwitchTab(position);
            }
        });
        gridView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                if (tabs.size() > 1 && position >= 0 && position < tabs.size()) {
                    final int targetPos = position;
                    new AlertDialog.Builder(activity)
                            .setTitle(R.string.close_other_tabs)
                            .setPositiveButton(R.string.close_other_tabs, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface d, int which) {
                                    actions.onSwitchTab(targetPos);
                                    actions.onCloseOtherTabs();
                                    dialog.dismiss();
                                }
                            })
                            .setNegativeButton(android.R.string.cancel, null)
                            .show();
                    return true;
                }
                return false;
            }
        });

        doneBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        closeAllBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                actions.onCloseAllTabs();
            }
        });
        newTabBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                actions.onNewTab(homepageUrl, false);
            }
        });
        newPrivateBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                actions.onNewTab(homepageUrl, true);
            }
        });

        dialog.show();
    }

    public static void showSavedPagesDialog(
            final Activity activity,
            final boolean isBookmarks,
            final BrowserActions actions) {
        if (isActivityUnavailable(activity)) {
            return;
        }
        View panel = LayoutInflater.from(activity).inflate(R.layout.dialog_list_panel, null);
        EditText filterInput = panel.findViewById(R.id.panelFilterInput);
        final TextView emptyText = panel.findViewById(R.id.panelEmptyText);
        final ListView listView = panel.findViewById(R.id.panelListView);

        filterInput.setVisibility(View.VISIBLE);
        emptyText.setText(isBookmarks ? R.string.bookmarks_empty : R.string.history_empty);

        final List<BrowserDataStore.SavedPage> allItems = isBookmarks
                ? BrowserDataStore.getBookmarks(activity)
                : BrowserDataStore.getHistory(activity);
        final List<BrowserDataStore.SavedPage> filteredItems = new ArrayList<>(allItems);

        final AlertDialog dialog = new AlertDialog.Builder(activity)
                .setTitle(isBookmarks ? R.string.menu_bookmarks : R.string.menu_history)
                .setView(panel)
                .setPositiveButton(android.R.string.ok, null)
                .setNegativeButton(R.string.clear_all, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int which) {
                        if (isBookmarks) {
                            BrowserDataStore.clearBookmarks(activity);
                            actions.onBookmarkChanged();
                            Toast.makeText(activity, R.string.bookmarks_cleared, Toast.LENGTH_SHORT).show();
                        } else {
                            BrowserDataStore.clearHistory(activity);
                            Toast.makeText(activity, R.string.history_cleared, Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .create();

        final SavedPagesAdapter adapter = new SavedPagesAdapter(
                activity,
                filteredItems,
                allItems,
                isBookmarks,
                emptyText,
                listView,
                actions
        );
        listView.setAdapter(adapter);
        adapter.refreshEmptyState();

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (position >= 0 && position < filteredItems.size()) {
                    String targetUrl = filteredItems.get(position).url;
                    dialog.dismiss();
                    actions.onOpenUrlInCurrentTab(targetUrl);
                }
            }
        });

        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                if (position < 0 || position >= filteredItems.size() || isActivityUnavailable(activity)) {
                    return false;
                }
                final BrowserDataStore.SavedPage page = filteredItems.get(position);
                String[] opts = new String[]{
                        activity.getString(R.string.context_open_new_tab),
                        activity.getString(R.string.context_open_private_tab),
                        activity.getString(R.string.context_copy_link),
                        activity.getString(R.string.context_share_link)
                };
                new AlertDialog.Builder(activity)
                        .setTitle(page.title != null && !page.title.isEmpty() ? page.title : page.url)
                        .setItems(opts, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface d, int which) {
                                if (which == 0) {
                                    dialog.dismiss();
                                    actions.onNewTab(page.url, false);
                                } else if (which == 1) {
                                    dialog.dismiss();
                                    actions.onNewTab(page.url, true);
                                } else if (which == 2) {
                                    actions.onCopyLink(page.url);
                                } else if (which == 3) {
                                    actions.onShareLink(page.title, page.url);
                                }
                            }
                        })
                        .show();
                return true;
            }
        });

        filterInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s != null ? s.toString().trim().toLowerCase(Locale.ROOT) : "";
                filteredItems.clear();
                for (BrowserDataStore.SavedPage p : allItems) {
                    if (query.isEmpty()
                            || (p.title != null && p.title.toLowerCase(Locale.ROOT).contains(query))
                            || (p.url != null && p.url.toLowerCase(Locale.ROOT).contains(query))) {
                        filteredItems.add(p);
                    }
                }
                adapter.notifyDataSetChanged();
                adapter.refreshEmptyState();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        dialog.show();
    }

    public static void showAddQuickSiteDialog(final Activity activity, final BrowserActions actions) {
        if (isActivityUnavailable(activity)) {
            return;
        }
        int pad = (int) (16 * activity.getResources().getDisplayMetrics().density);
        LinearLayout layout = new LinearLayout(activity);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(pad, pad / 2, pad, 0);

        final EditText titleInput = new EditText(activity);
        titleInput.setHint(R.string.bookmark_title_hint);
        layout.addView(titleInput);

        final EditText urlInput = new EditText(activity);
        urlInput.setHint(R.string.bookmark_url_hint);
        layout.addView(urlInput);

        new AlertDialog.Builder(activity)
                .setTitle(R.string.add_quick_site)
                .setView(layout)
                .setPositiveButton(R.string.save, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String u = urlInput.getText().toString().trim();
                        if (!u.isEmpty()) {
                            BrowserDataStore.addQuickSite(activity, titleInput.getText().toString().trim(), u);
                            actions.onQuickSitesUpdated();
                            Toast.makeText(activity, R.string.quick_site_added, Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private static void showEditBookmarkDialog(
            final Activity activity,
            final BrowserDataStore.SavedPage oldItem,
            final List<BrowserDataStore.SavedPage> backingAll,
            final List<BrowserDataStore.SavedPage> filteredItems,
            final SavedPagesAdapter adapter,
            final BrowserActions actions) {
        if (isActivityUnavailable(activity)) {
            return;
        }
        int pad = (int) (16 * activity.getResources().getDisplayMetrics().density);
        LinearLayout layout = new LinearLayout(activity);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(pad, pad / 2, pad, 0);

        final EditText titleInput = new EditText(activity);
        titleInput.setHint(R.string.bookmark_title_hint);
        titleInput.setText(oldItem.title);
        layout.addView(titleInput);

        final EditText urlInput = new EditText(activity);
        urlInput.setHint(R.string.bookmark_url_hint);
        urlInput.setText(oldItem.url);
        layout.addView(urlInput);

        new AlertDialog.Builder(activity)
                .setTitle(R.string.edit_bookmark)
                .setView(layout)
                .setPositiveButton(R.string.save, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String newTitle = titleInput.getText().toString().trim();
                        String newUrl = urlInput.getText().toString().trim();
                        if (newUrl.isEmpty()) {
                            return;
                        }
                        BrowserDataStore.updateBookmark(activity, oldItem.url, newTitle, newUrl);
                        BrowserDataStore.SavedPage updated = new BrowserDataStore.SavedPage(
                                newTitle.isEmpty() ? newUrl : newTitle,
                                newUrl,
                                oldItem.timestamp
                        );
                        int allIdx = backingAll.indexOf(oldItem);
                        if (allIdx >= 0) {
                            backingAll.set(allIdx, updated);
                        }
                        int filIdx = filteredItems.indexOf(oldItem);
                        if (filIdx >= 0) {
                            filteredItems.set(filIdx, updated);
                        }
                        adapter.notifyDataSetChanged();
                        actions.onBookmarkChanged();
                        Toast.makeText(activity, R.string.bookmark_updated, Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private static String extractCompactUrl(String rawUrl) {
        if (rawUrl == null || rawUrl.isEmpty()) {
            return "";
        }
        if (BrowserDataStore.START_PAGE_URL.equals(rawUrl)) {
            return rawUrl;
        }
        try {
            String host = Uri.parse(rawUrl).getHost();
            if (host != null && !host.isEmpty()) {
                return host.startsWith("www.") ? host.substring(4) : host;
            }
        } catch (Exception ignored) {
        }
        return rawUrl;
    }

    private static class TabCardsAdapter extends BaseAdapter {
        private final Activity activity;
        private final List<BrowserTab> tabs;
        private final TextView countBadge;
        private final Dialog parentDialog;
        private final BrowserActions actions;

        TabCardsAdapter(
                Activity activity,
                List<BrowserTab> tabs,
                TextView countBadge,
                Dialog parentDialog,
                BrowserActions actions) {
            this.activity = activity;
            this.tabs = tabs;
            this.countBadge = countBadge;
            this.parentDialog = parentDialog;
            this.actions = actions;
        }

        @Override
        public int getCount() {
            return tabs.size();
        }

        @Override
        public Object getItem(int position) {
            return tabs.get(position);
        }

        @Override
        public long getItemId(int position) {
            return tabs.get(position).id;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View card = convertView;
            if (card == null) {
                card = LayoutInflater.from(activity).inflate(R.layout.item_tab_card, parent, false);
            }
            final BrowserTab tab = tabs.get(position);
            View cardRoot = card.findViewById(R.id.tabCardRoot);
            View divider = card.findViewById(R.id.tabCardDivider);
            View previewBg = card.findViewById(R.id.tabPreviewBadgeBg);
            AppCompatImageView icon = card.findViewById(R.id.tabIcon);
            AppCompatImageView previewIcon = card.findViewById(R.id.tabPreviewIcon);
            TextView titleView = card.findViewById(R.id.tabTitleText);
            TextView urlView = card.findViewById(R.id.tabUrlText);
            TextView desktopBadge = card.findViewById(R.id.tabDesktopBadge);
            TextView privateBadge = card.findViewById(R.id.tabPrivateBadge);
            AppCompatImageButton closeBtn = card.findViewById(R.id.tabCloseButton);

            boolean isActive = (tab.id == actions.getActiveTabId());
            String title = (tab.title != null && !tab.title.trim().isEmpty())
                    ? tab.title
                    : activity.getString(R.string.untitled_tab);
            titleView.setText(title);
            urlView.setText(extractCompactUrl(tab.url));
            desktopBadge.setVisibility(tab.desktopMode ? View.VISIBLE : View.GONE);

            if (tab.isPrivate) {
                cardRoot.setBackgroundResource(isActive ? R.drawable.bg_tab_card_private_active : R.drawable.bg_tab_card_private);
                divider.setBackgroundColor(Color.parseColor("#3C4043"));
                previewBg.setBackgroundResource(R.drawable.bg_ios_squircle_private);
                titleView.setTextColor(Color.parseColor("#F1F5F9"));
                urlView.setTextColor(Color.parseColor("#9AA0A6"));
                icon.setImageResource(R.drawable.ic_shield_private);
                previewIcon.setImageResource(R.drawable.ic_incognito_glasses);
                int privTint = Color.parseColor("#A8C7FA");
                ImageViewCompat.setImageTintList(icon, ColorStateList.valueOf(privTint));
                ImageViewCompat.setImageTintList(previewIcon, ColorStateList.valueOf(Color.parseColor("#E8EAED")));
                ImageViewCompat.setImageTintList(closeBtn, ColorStateList.valueOf(Color.parseColor("#9AA0A6")));
                privateBadge.setVisibility(View.VISIBLE);
            } else {
                cardRoot.setBackgroundResource(isActive ? R.drawable.bg_tab_card_active : R.drawable.bg_tab_card);
                divider.setBackgroundColor(ContextCompat.getColor(activity, R.color.border_subtle));
                previewBg.setBackgroundResource(R.drawable.bg_ios_squircle);
                titleView.setTextColor(ContextCompat.getColor(activity, isActive ? R.color.accent_blue : R.color.text_primary));
                urlView.setTextColor(ContextCompat.getColor(activity, R.color.text_secondary));
                boolean secure = tab.isSecure || BrowserDataStore.START_PAGE_URL.equals(tab.url);
                icon.setImageResource(secure ? R.drawable.ic_lock_secure : R.drawable.ic_lock_insecure);
                int c = ContextCompat.getColor(activity, secure ? R.color.accent_secure : R.color.accent_insecure);
                ImageViewCompat.setImageTintList(icon, ColorStateList.valueOf(c));
                previewIcon.setImageResource(BrowserDataStore.START_PAGE_URL.equals(tab.url)
                        ? R.drawable.ic_chrome_ring
                        : R.drawable.ic_public);
                ImageViewCompat.setImageTintList(previewIcon, BrowserDataStore.START_PAGE_URL.equals(tab.url)
                        ? null
                        : ColorStateList.valueOf(ContextCompat.getColor(activity, R.color.accent_blue)));
                ImageViewCompat.setImageTintList(closeBtn, ColorStateList.valueOf(ContextCompat.getColor(activity, R.color.text_secondary)));
                privateBadge.setVisibility(View.GONE);
            }

            closeBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    actions.onCloseTabById(tab.id);
                    if (tabs.isEmpty()) {
                        parentDialog.dismiss();
                    } else {
                        countBadge.setText(String.valueOf(tabs.size()));
                        notifyDataSetChanged();
                    }
                }
            });
            return card;
        }
    }

    private static class SavedPagesAdapter extends BaseAdapter {
        private final Activity activity;
        private final List<BrowserDataStore.SavedPage> items;
        private final List<BrowserDataStore.SavedPage> backingAll;
        private final boolean isBookmarks;
        private final TextView emptyView;
        private final ListView listView;
        private final BrowserActions actions;

        SavedPagesAdapter(
                Activity activity,
                List<BrowserDataStore.SavedPage> items,
                List<BrowserDataStore.SavedPage> backingAll,
                boolean isBookmarks,
                TextView emptyView,
                ListView listView,
                BrowserActions actions) {
            this.activity = activity;
            this.items = items;
            this.backingAll = backingAll;
            this.isBookmarks = isBookmarks;
            this.emptyView = emptyView;
            this.listView = listView;
            this.actions = actions;
        }

        void refreshEmptyState() {
            boolean empty = items.isEmpty();
            emptyView.setVisibility(empty ? View.VISIBLE : View.GONE);
            listView.setVisibility(empty ? View.GONE : View.VISIBLE);
        }

        @Override
        public int getCount() {
            return items.size();
        }

        @Override
        public Object getItem(int position) {
            return items.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View row = convertView;
            if (row == null) {
                row = LayoutInflater.from(activity).inflate(R.layout.item_saved_page, parent, false);
            }
            final BrowserDataStore.SavedPage item = items.get(position);
            TextView titleView = row.findViewById(R.id.savedPageTitle);
            TextView urlView = row.findViewById(R.id.savedPageUrl);
            AppCompatImageButton editBtn = row.findViewById(R.id.savedPageEditButton);
            AppCompatImageButton deleteBtn = row.findViewById(R.id.savedPageDeleteButton);

            titleView.setText(item.title != null && !item.title.isEmpty() ? item.title : item.url);
            urlView.setText(item.url);

            editBtn.setVisibility(isBookmarks ? View.VISIBLE : View.GONE);
            editBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showEditBookmarkDialog(activity, item, backingAll, items, SavedPagesAdapter.this, actions);
                }
            });

            deleteBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (isBookmarks) {
                        BrowserDataStore.removeBookmark(activity, item.url);
                        actions.onBookmarkChanged();
                    } else {
                        BrowserDataStore.removeHistoryByUrl(activity, item.url);
                    }
                    backingAll.remove(item);
                    items.remove(item);
                    notifyDataSetChanged();
                    refreshEmptyState();
                }
            });
            return row;
        }
    }
}
