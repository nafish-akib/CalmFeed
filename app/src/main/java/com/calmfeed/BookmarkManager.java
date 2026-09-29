package com.calmfeed;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public final class BookmarkManager {
    private static final String PREF_NAME = "calmfeed_browser_bookmarks";
    private static final String KEY_BOOKMARKS = "bookmarks";
    private static final String KEY_INITIALIZED = "initialized";

    public static class BookmarkItem {
        public final String title;
        public final String url;

        public BookmarkItem(String title, String url) {
            this.title = title != null && !title.trim().isEmpty() ? title.trim() : (url != null ? url : "Bookmark");
            this.url = url != null ? url : "";
        }
    }

    private BookmarkManager() {}

    public static synchronized List<BookmarkItem> getAll(Context context) {
        List<BookmarkItem> list = new ArrayList<>();
        if (context == null) return list;

        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        if (!prefs.getBoolean(KEY_INITIALIZED, false)) {
            // Seed defaults
            list.add(new BookmarkItem("YouTube", "https://www.youtube.com"));
            list.add(new BookmarkItem("Google Search", "https://www.google.com"));
            list.add(new BookmarkItem("Wikipedia", "https://www.wikipedia.org"));
            list.add(new BookmarkItem("Reddit", "https://www.reddit.com"));
            list.add(new BookmarkItem("GitHub", "https://www.github.com"));
            save(prefs, list);
            prefs.edit().putBoolean(KEY_INITIALIZED, true).apply();
            return list;
        }

        String raw = prefs.getString(KEY_BOOKMARKS, "[]");
        try {
            JSONArray arr = new JSONArray(raw);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                list.add(new BookmarkItem(
                        obj.optString("title", ""),
                        obj.optString("url", "")
                ));
            }
        } catch (Exception ignored) {}
        return list;
    }

    public static synchronized boolean isBookmarked(Context context, String url) {
        if (context == null || url == null || url.trim().isEmpty()) return false;
        String clean = normalizeUrl(url);
        for (BookmarkItem item : getAll(context)) {
            if (normalizeUrl(item.url).equalsIgnoreCase(clean)) {
                return true;
            }
        }
        return false;
    }

    public static synchronized boolean toggleBookmark(Context context, String title, String url) {
        if (context == null || url == null || url.trim().isEmpty()) return false;
        if (isBookmarked(context, url)) {
            remove(context, url);
            return false; // Removed
        } else {
            add(context, title, url);
            return true; // Added
        }
    }

    public static synchronized void add(Context context, String title, String url) {
        if (context == null || url == null || url.trim().isEmpty()) return;
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        List<BookmarkItem> list = getAll(context);

        // Remove existing if duplicate
        String clean = normalizeUrl(url);
        for (int i = 0; i < list.size(); i++) {
            if (normalizeUrl(list.get(i).url).equalsIgnoreCase(clean)) {
                list.remove(i);
                break;
            }
        }

        list.add(0, new BookmarkItem(title, url));
        save(prefs, list);
    }

    public static synchronized void remove(Context context, String url) {
        if (context == null || url == null) return;
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        List<BookmarkItem> list = getAll(context);
        String clean = normalizeUrl(url);
        for (int i = 0; i < list.size(); i++) {
            if (normalizeUrl(list.get(i).url).equalsIgnoreCase(clean)) {
                list.remove(i);
                break;
            }
        }
        save(prefs, list);
    }

    private static String normalizeUrl(String url) {
        if (url == null) return "";
        String trimmed = url.trim();
        if (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }

    private static void save(SharedPreferences prefs, List<BookmarkItem> items) {
        JSONArray arr = new JSONArray();
        for (BookmarkItem item : items) {
            try {
                JSONObject obj = new JSONObject();
                obj.put("title", item.title);
                obj.put("url", item.url);
                arr.put(obj);
            } catch (Exception ignored) {}
        }
        prefs.edit().putString(KEY_BOOKMARKS, arr.toString()).apply();
    }
}
