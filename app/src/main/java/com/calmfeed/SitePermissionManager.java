package com.calmfeed;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.text.TextUtils;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class SitePermissionManager {
    private static final String PREF_NAME = "calmfeed_site_permissions";
    private static final String KEY_RULES = "site_rules_json";
    private static final String KEY_INITIALIZED = "rules_initialized";

    // Permission Types
    public static final String PERM_LOCATION = "location";
    public static final String PERM_CAMERA = "camera";
    public static final String PERM_MICROPHONE = "microphone";
    public static final String PERM_NOTIFICATIONS = "notifications";

    // Permission States
    public static final int STATE_ASK = 0;
    public static final int STATE_ALLOW = 1;
    public static final int STATE_BLOCK = 2;

    public static class SiteRule {
        public final String host;
        public final int locationState;
        public final int cameraState;
        public final int micState;
        public final int notificationState;

        public SiteRule(String host, int locationState, int cameraState, int micState, int notificationState) {
            this.host = host;
            this.locationState = locationState;
            this.cameraState = cameraState;
            this.micState = micState;
            this.notificationState = notificationState;
        }

        public int getPermission(String perm) {
            switch (perm) {
                case PERM_LOCATION: return locationState;
                case PERM_CAMERA: return cameraState;
                case PERM_MICROPHONE: return micState;
                case PERM_NOTIFICATIONS: return notificationState;
                default: return STATE_ASK;
            }
        }
    }

    private SitePermissionManager() {}

    public static String cleanHost(String urlOrHost) {
        if (TextUtils.isEmpty(urlOrHost)) return "unknown";
        String s = urlOrHost.trim().toLowerCase(Locale.ROOT);
        if (s.contains("://")) {
            try {
                Uri uri = Uri.parse(s);
                String host = uri.getHost();
                if (host != null) s = host;
            } catch (Exception ignored) {}
        }
        if (s.startsWith("www.")) s = s.substring(4);
        if (s.startsWith("m.")) s = s.substring(2);
        int colonIdx = s.indexOf(':');
        if (colonIdx > 0) s = s.substring(0, colonIdx);
        int slashIdx = s.indexOf('/');
        if (slashIdx > 0) s = s.substring(0, slashIdx);
        return s;
    }

    private static synchronized JSONObject getRootJson(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        if (!prefs.getBoolean(KEY_INITIALIZED, false)) {
            // Seed initial popular sites
            JSONObject init = new JSONObject();
            try {
                JSONObject yt = new JSONObject();
                yt.put(PERM_LOCATION, STATE_BLOCK);
                yt.put(PERM_CAMERA, STATE_BLOCK);
                yt.put(PERM_MICROPHONE, STATE_ALLOW); // Voice search on YouTube
                yt.put(PERM_NOTIFICATIONS, STATE_BLOCK);
                init.put("youtube.com", yt);

                JSONObject google = new JSONObject();
                google.put(PERM_LOCATION, STATE_ALLOW); // Search nearby
                google.put(PERM_CAMERA, STATE_BLOCK);
                google.put(PERM_MICROPHONE, STATE_ALLOW); // Voice search
                google.put(PERM_NOTIFICATIONS, STATE_ASK);
                init.put("google.com", google);

                JSONObject maps = new JSONObject();
                maps.put(PERM_LOCATION, STATE_ALLOW); // Maps navigation
                maps.put(PERM_CAMERA, STATE_BLOCK);
                maps.put(PERM_MICROPHONE, STATE_BLOCK);
                maps.put(PERM_NOTIFICATIONS, STATE_ASK);
                init.put("maps.google.com", maps);

                JSONObject meet = new JSONObject();
                meet.put(PERM_LOCATION, STATE_BLOCK);
                meet.put(PERM_CAMERA, STATE_ALLOW); // Video call
                meet.put(PERM_MICROPHONE, STATE_ALLOW); // Mic
                meet.put(PERM_NOTIFICATIONS, STATE_ALLOW);
                init.put("meet.google.com", meet);

                JSONObject reddit = new JSONObject();
                reddit.put(PERM_LOCATION, STATE_BLOCK);
                reddit.put(PERM_CAMERA, STATE_BLOCK);
                reddit.put(PERM_MICROPHONE, STATE_BLOCK);
                reddit.put(PERM_NOTIFICATIONS, STATE_BLOCK);
                init.put("reddit.com", reddit);

                JSONObject fb = new JSONObject();
                fb.put(PERM_LOCATION, STATE_BLOCK);
                fb.put(PERM_CAMERA, STATE_BLOCK);
                fb.put(PERM_MICROPHONE, STATE_BLOCK);
                fb.put(PERM_NOTIFICATIONS, STATE_BLOCK);
                init.put("facebook.com", fb);

                JSONObject tiktok = new JSONObject();
                tiktok.put(PERM_LOCATION, STATE_BLOCK);
                tiktok.put(PERM_CAMERA, STATE_BLOCK);
                tiktok.put(PERM_MICROPHONE, STATE_BLOCK);
                tiktok.put(PERM_NOTIFICATIONS, STATE_BLOCK);
                init.put("tiktok.com", tiktok);
            } catch (Exception ignored) {}
            prefs.edit().putString(KEY_RULES, init.toString()).putBoolean(KEY_INITIALIZED, true).apply();
            return init;
        }

        String raw = prefs.getString(KEY_RULES, "{}");
        try {
            return new JSONObject(raw);
        } catch (Exception e) {
            return new JSONObject();
        }
    }

    public static synchronized int getSitePermission(Context context, String urlOrHost, String permissionType) {
        if (context == null) return STATE_ASK;
        String host = cleanHost(urlOrHost);
        JSONObject root = getRootJson(context);
        JSONObject siteObj = root.optJSONObject(host);
        if (siteObj != null && siteObj.has(permissionType)) {
            return siteObj.optInt(permissionType, STATE_ASK);
        }
        // Fallback to domain root if subdomain (e.g. sub.domain.com -> domain.com)
        int dot = host.indexOf('.');
        if (dot > 0 && host.indexOf('.', dot + 1) > 0) {
            String parentHost = host.substring(dot + 1);
            JSONObject parentObj = root.optJSONObject(parentHost);
            if (parentObj != null && parentObj.has(permissionType)) {
                return parentObj.optInt(permissionType, STATE_ASK);
            }
        }
        // Fallback to global default
        return getGlobalDefault(context, permissionType);
    }

    public static synchronized void setSitePermission(Context context, String urlOrHost, String permissionType, int state) {
        if (context == null) return;
        String host = cleanHost(urlOrHost);
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        JSONObject root = getRootJson(context);
        try {
            JSONObject siteObj = root.optJSONObject(host);
            if (siteObj == null) {
                siteObj = new JSONObject();
                siteObj.put(PERM_LOCATION, getGlobalDefault(context, PERM_LOCATION));
                siteObj.put(PERM_CAMERA, getGlobalDefault(context, PERM_CAMERA));
                siteObj.put(PERM_MICROPHONE, getGlobalDefault(context, PERM_MICROPHONE));
                siteObj.put(PERM_NOTIFICATIONS, getGlobalDefault(context, PERM_NOTIFICATIONS));
                root.put(host, siteObj);
            }
            siteObj.put(permissionType, state);
            prefs.edit().putString(KEY_RULES, root.toString()).apply();
        } catch (Exception ignored) {}
    }

    public static synchronized List<SiteRule> getAllSiteRules(Context context) {
        List<SiteRule> list = new ArrayList<>();
        if (context == null) return list;
        JSONObject root = getRootJson(context);
        Iterator<String> keys = root.keys();
        while (keys.hasNext()) {
            String host = keys.next();
            JSONObject obj = root.optJSONObject(host);
            if (obj != null) {
                int loc = obj.optInt(PERM_LOCATION, STATE_ASK);
                int cam = obj.optInt(PERM_CAMERA, STATE_ASK);
                int mic = obj.optInt(PERM_MICROPHONE, STATE_ASK);
                int notif = obj.optInt(PERM_NOTIFICATIONS, STATE_ASK);
                list.add(new SiteRule(host, loc, cam, mic, notif));
            }
        }
        Collections.sort(list, (a, b) -> a.host.compareToIgnoreCase(b.host));
        return list;
    }

    public static synchronized void deleteSiteRule(Context context, String urlOrHost) {
        if (context == null) return;
        String host = cleanHost(urlOrHost);
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        JSONObject root = getRootJson(context);
        root.remove(host);
        prefs.edit().putString(KEY_RULES, root.toString()).apply();
    }

    public static synchronized void resetAllRules(Context context) {
        if (context == null) return;
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().remove(KEY_RULES).remove(KEY_INITIALIZED).apply();
        getRootJson(context); // re-seed defaults
    }

    // Global Defaults
    public static int getGlobalDefault(Context context, String permissionType) {
        if (context == null) return STATE_ASK;
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getInt("global_" + permissionType, STATE_ASK);
    }

    public static void setGlobalDefault(Context context, String permissionType, int state) {
        if (context == null) return;
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putInt("global_" + permissionType, state).apply();
    }
}
