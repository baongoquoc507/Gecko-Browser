package com.example.geckobrowser;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.core.content.FileProvider;
import androidx.core.content.pm.ShortcutInfoCompat;
import androidx.core.content.pm.ShortcutManagerCompat;
import androidx.core.graphics.drawable.IconCompat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

public final class BrowserHelper {

    private BrowserHelper() {
    }

    public static String sanitizeFilename(String rawName) {
        if (rawName == null || rawName.trim().isEmpty()) {
            return "download";
        }
        String clean = rawName.trim().replaceAll("[\\\\/:*?\"<>|\\s]+", "_");
        if (clean.length() > 80) {
            clean = clean.substring(0, 80);
        }
        if (clean.isEmpty() || ".".equals(clean) || "..".equals(clean)) {
            return "download";
        }
        return clean;
    }

    public static String guessFilename(String url, String suggestedFilename, String mimeType) {
        if (suggestedFilename != null && !suggestedFilename.trim().isEmpty()) {
            return sanitizeFilename(suggestedFilename);
        }
        String name = "download";
        if (url != null && !url.startsWith("data:")) {
            String path = Uri.parse(url).getLastPathSegment();
            if (path != null && !path.trim().isEmpty()) {
                name = path.trim();
            }
        }
        name = sanitizeFilename(name);
        if (!name.contains(".") && mimeType != null) {
            String ext = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType);
            if (ext != null && !ext.isEmpty()) {
                name = name + "." + ext;
            }
        }
        return name;
    }

    public static void promptAndEnqueueDownload(
            final Activity activity,
            final String url,
            final String suggestedFilename,
            final String mimeType) {
        if (url == null || url.trim().isEmpty() || activity.isFinishing()) {
            return;
        }
        final String filename = guessFilename(url, suggestedFilename, mimeType);
        new AlertDialog.Builder(activity)
                .setTitle(R.string.download_confirm_title)
                .setMessage(activity.getString(R.string.download_confirm_message, filename))
                .setPositiveButton(R.string.download_action, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        enqueueDownload(activity, url, filename, mimeType);
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    public static void enqueueDownload(Context context, String url, String filename, String mimeType) {
        if (url == null || url.trim().isEmpty()) {
            Toast.makeText(context, R.string.download_failed, Toast.LENGTH_SHORT).show();
            return;
        }
        String cleanUrl = url.trim();
        if (cleanUrl.startsWith("data:")) {
            saveDataUriDownload(context, cleanUrl, filename, mimeType);
            return;
        }
        if (!cleanUrl.startsWith("http://") && !cleanUrl.startsWith("https://")) {
            Toast.makeText(context, R.string.download_unsupported_uri, Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            DownloadManager dm = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
            if (dm == null) {
                Toast.makeText(context, R.string.download_failed, Toast.LENGTH_SHORT).show();
                return;
            }
            String safeName = sanitizeFilename(filename);
            DownloadManager.Request req = new DownloadManager.Request(Uri.parse(cleanUrl));
            if (mimeType != null && !mimeType.trim().isEmpty()) {
                req.setMimeType(mimeType);
            }
            req.setTitle(safeName);
            req.setDescription(cleanUrl);
            req.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            req.setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, safeName);
            long id = dm.enqueue(req);
            BrowserDataStore.addDownload(
                    context,
                    new BrowserDataStore.DownloadRecord(id, safeName, cleanUrl, mimeType != null ? mimeType : "", System.currentTimeMillis())
            );
            Toast.makeText(context, context.getString(R.string.download_started, safeName), Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(context, R.string.download_failed, Toast.LENGTH_SHORT).show();
        }
    }

    private static void saveDataUriDownload(Context context, String dataUri, String filename, String fallbackMime) {
        FileOutputStream fos = null;
        try {
            int comma = dataUri.indexOf(',');
            if (comma < 0) {
                Toast.makeText(context, R.string.download_unsupported_uri, Toast.LENGTH_SHORT).show();
                return;
            }
            String header = dataUri.substring(5, comma);
            String payload = dataUri.substring(comma + 1);
            boolean isBase64 = header.toLowerCase(Locale.ROOT).contains(";base64");
            String detectedMime = header.contains(";") ? header.substring(0, header.indexOf(';')) : header;
            String mime = !detectedMime.isEmpty() ? detectedMime : (fallbackMime != null ? fallbackMime : "application/octet-stream");
            byte[] bytes = isBase64 ? Base64.decode(payload, Base64.DEFAULT) : Uri.decode(payload).getBytes("UTF-8");

            File dir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
            if (dir == null) {
                dir = context.getFilesDir();
            }
            if (!dir.exists()) {
                dir.mkdirs();
            }
            String safeName = sanitizeFilename(guessFilename(null, filename, mime));
            File outFile = new File(dir, System.currentTimeMillis() + "_" + safeName);
            fos = new FileOutputStream(outFile);
            fos.write(bytes);
            BrowserDataStore.addDownload(
                    context,
                    new BrowserDataStore.DownloadRecord(-1L, safeName, outFile.getAbsolutePath(), mime, System.currentTimeMillis())
            );
            Toast.makeText(context, context.getString(R.string.download_started, safeName), Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(context, R.string.download_failed, Toast.LENGTH_SHORT).show();
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    public static void showDownloadsDialog(final Activity activity) {
        View panel = LayoutInflater.from(activity).inflate(R.layout.dialog_list_panel, null);
        final TextView emptyText = panel.findViewById(R.id.panelEmptyText);
        final ListView listView = panel.findViewById(R.id.panelListView);
        emptyText.setText(R.string.downloads_empty);

        final List<BrowserDataStore.DownloadRecord> items = BrowserDataStore.getDownloads(activity);
        final DownloadsAdapter adapter = new DownloadsAdapter(activity, items, emptyText, listView);
        listView.setAdapter(adapter);
        adapter.refreshEmptyState();

        final AlertDialog dialog = new AlertDialog.Builder(activity)
                .setTitle(R.string.menu_downloads)
                .setView(panel)
                .setPositiveButton(android.R.string.ok, null)
                .setNegativeButton(R.string.clear_all, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int which) {
                        BrowserDataStore.clearDownloads(activity);
                        Toast.makeText(activity, R.string.downloads_cleared, Toast.LENGTH_SHORT).show();
                    }
                })
                .create();

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (position >= 0 && position < items.size()) {
                    openDownloadedFile(activity, items.get(position));
                }
            }
        });

        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                if (position < 0 || position >= items.size()) {
                    return false;
                }
                final BrowserDataStore.DownloadRecord rec = items.get(position);
                String[] opts = new String[]{
                        activity.getString(R.string.download_open),
                        activity.getString(R.string.download_copy_url),
                        activity.getString(R.string.delete_item)
                };
                new AlertDialog.Builder(activity)
                        .setTitle(rec.filename)
                        .setItems(opts, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface d, int which) {
                                if (which == 0) {
                                    openDownloadedFile(activity, rec);
                                } else if (which == 1) {
                                    ClipboardManager cm = (ClipboardManager) activity.getSystemService(Context.CLIPBOARD_SERVICE);
                                    if (cm != null && rec.url != null) {
                                        cm.setPrimaryClip(ClipData.newPlainText("URL", rec.url));
                                        Toast.makeText(activity, R.string.link_copied, Toast.LENGTH_SHORT).show();
                                    }
                                } else if (which == 2) {
                                    BrowserDataStore.removeDownloadByRecord(activity, rec);
                                    items.remove(rec);
                                    adapter.notifyDataSetChanged();
                                    adapter.refreshEmptyState();
                                }
                            }
                        })
                        .show();
                return true;
            }
        });

        dialog.show();
    }

    private static void openDownloadedFile(Context context, BrowserDataStore.DownloadRecord record) {
        try {
            String mime = record.mimeType != null && !record.mimeType.isEmpty() ? record.mimeType : "*/*";
            Uri fileUri = null;
            if (record.downloadId >= 0) {
                DownloadManager dm = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
                if (dm != null) {
                    fileUri = dm.getUriForDownloadedFile(record.downloadId);
                }
            } else if (record.url != null && !record.url.isEmpty()) {
                File localFile = new File(record.url);
                if (localFile.exists()) {
                    fileUri = FileProvider.getUriForFile(
                            context,
                            context.getPackageName() + ".fileprovider",
                            localFile
                    );
                }
            }
            if (fileUri != null) {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(fileUri, mime);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
                return;
            }
            Toast.makeText(context, R.string.download_open_error, Toast.LENGTH_SHORT).show();
        } catch (ActivityNotFoundException e) {
            Toast.makeText(context, R.string.download_open_error, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(context, R.string.download_open_error, Toast.LENGTH_SHORT).show();
        }
    }

    private static String getDownloadStatusLabel(Context context, BrowserDataStore.DownloadRecord rec) {
        if (rec.downloadId < 0) {
            return context.getString(R.string.download_status_done);
        }
        DownloadManager dm = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        if (dm == null) {
            return "";
        }
        Cursor cursor = null;
        try {
            cursor = dm.query(new DownloadManager.Query().setFilterById(rec.downloadId));
            if (cursor != null && cursor.moveToFirst()) {
                int col = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS);
                if (col >= 0) {
                    int status = cursor.getInt(col);
                    if (status == DownloadManager.STATUS_SUCCESSFUL) {
                        return context.getString(R.string.download_status_done);
                    } else if (status == DownloadManager.STATUS_FAILED) {
                        return context.getString(R.string.download_status_failed);
                    } else {
                        return context.getString(R.string.download_status_running);
                    }
                }
            }
        } catch (Exception ignored) {
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return "";
    }

    public static void addShortcutToHomeScreen(Context context, String title, String url) {
        if (url == null || url.trim().isEmpty()) {
            return;
        }
        if (!ShortcutManagerCompat.isRequestPinShortcutSupported(context)) {
            Toast.makeText(context, R.string.shortcut_unsupported, Toast.LENGTH_SHORT).show();
            return;
        }
        String label = (title != null && !title.trim().isEmpty()) ? title.trim() : url;
        Intent intent = new Intent(context, MainActivity.class);
        intent.setAction(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(url));

        ShortcutInfoCompat shortcut = new ShortcutInfoCompat.Builder(context, "gecko_" + url.hashCode())
                .setShortLabel(label.length() > 25 ? label.substring(0, 25) : label)
                .setLongLabel(label)
                .setIcon(IconCompat.createWithResource(context, R.mipmap.ic_launcher))
                .setIntent(intent)
                .build();

        ShortcutManagerCompat.requestPinShortcut(context, shortcut, null);
        Toast.makeText(context, R.string.shortcut_added, Toast.LENGTH_SHORT).show();
    }

    private static class DownloadsAdapter extends BaseAdapter {
        private final Activity activity;
        private final List<BrowserDataStore.DownloadRecord> items;
        private final TextView emptyView;
        private final ListView listView;

        DownloadsAdapter(
                Activity activity,
                List<BrowserDataStore.DownloadRecord> items,
                TextView emptyView,
                ListView listView) {
            this.activity = activity;
            this.items = items;
            this.emptyView = emptyView;
            this.listView = listView;
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
            final BrowserDataStore.DownloadRecord rec = items.get(position);
            TextView titleView = row.findViewById(R.id.savedPageTitle);
            TextView urlView = row.findViewById(R.id.savedPageUrl);
            AppCompatImageButton deleteBtn = row.findViewById(R.id.savedPageDeleteButton);

            titleView.setText(rec.filename);
            String status = getDownloadStatusLabel(activity, rec);
            urlView.setText(status.isEmpty() ? rec.url : (status + " • " + rec.url));

            deleteBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    BrowserDataStore.removeDownloadByRecord(activity, rec);
                    items.remove(rec);
                    notifyDataSetChanged();
                    refreshEmptyState();
                }
            });
            return row;
        }
    }
}
