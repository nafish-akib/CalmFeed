package com.calmfeed;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public final class BrowserHistoryManager {
    private static final String PREF_NAME = "calmfeed_browser_history";
    private static final String KEY_ITEMS = "history_items";
    private static final int MAX_ITEMS = 250;

    public static class HistoryItem {
        public final long id;
        public final String title;
        public final String url;
        public final long timestamp;
        public final String formattedTime;

        public HistoryItem(long id, String title, String url, long timestamp, String formattedTime) {
            this.id = id;
            this.title = title != null && !title.trim().isEmpty() ? title.trim() : (url != null ? url : "Web Page");
            this.url = url != null ? url : "";
            this.timestamp = timestamp;
            this.formattedTime = formattedTime != null ? formattedTime : "";
        }
    }

    private BrowserHistoryManager() {}

    public static synchronized void add(Context context, String title, String url) {
        if (context == null || url == null || url.trim().isEmpty()) return;
        if (url.startsWith("about:") || url.startsWith("resource:") || url.startsWith("javascript:")) return;

        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        List<HistoryItem> items = getAll(context);

        // Avoid adding duplicate if previous URL is the same
        if (!items.isEmpty() && items.get(0).url.equalsIgnoreCase(url)) {
            // Update title if needed
            if (title != null && !title.trim().isEmpty() && !title.equals(items.get(0).title)) {
                items.set(0, new HistoryItem(items.get(0).id, title, url, items.get(0).timestamp, items.get(0).formattedTime));
                save(prefs, items);
            }
            return;
        }

        long now = System.currentTimeMillis();
        SimpleDateFormat sdf = new SimpleDateFormat("MMM d, h:mm a", Locale.getDefault());
        String timeStr = sdf.format(new Date(now));

        HistoryItem newItem = new HistoryItem(now, title, url, now, timeStr);
        items.add(0, newItem);

        if (items.size() > MAX_ITEMS) {
            items = items.subList(0, MAX_ITEMS);
        }

        save(prefs, items);
    }

    public static synchronized List<HistoryItem> getAll(Context context) {
        List<HistoryItem> list = new ArrayList<>();
        if (context == null) return list;

        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String raw = prefs.getString(KEY_ITEMS, "[]");
        try {
            JSONArray arr = new JSONArray(raw);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                list.add(new HistoryItem(
                        obj.optLong("id", 0),
                        obj.optString("title", ""),
                        obj.optString("url", ""),
                        obj.optLong("timestamp", 0),
                        obj.optString("time", "")
                ));
            }
        } catch (Exception ignored) {}
        return list;
    }

    public static synchronized void deleteItem(Context context, long id) {
        if (context == null) return;
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        List<HistoryItem> items = getAll(context);
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).id == id) {
                items.remove(i);
                break;
            }
        }
        save(prefs, items);
    }

    public static synchronized void clear(Context context) {
        if (context == null) return;
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().remove(KEY_ITEMS).apply();
    }

    private static void save(SharedPreferences prefs, List<HistoryItem> items) {
        JSONArray arr = new JSONArray();
        for (HistoryItem item : items) {
            try {
                JSONObject obj = new JSONObject();
                obj.put("id", item.id);
                obj.put("title", item.title);
                obj.put("url", item.url);
                obj.put("timestamp", item.timestamp);
                obj.put("time", item.formattedTime);
                arr.put(obj);
            } catch (Exception ignored) {}
        }
        prefs.edit().putString(KEY_ITEMS, arr.toString()).apply();
    }
}
