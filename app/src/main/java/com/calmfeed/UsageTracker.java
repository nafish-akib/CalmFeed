package com.calmfeed;

import android.content.Context;
import android.content.SharedPreferences;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

final class UsageTracker {
    private static final String PREFERENCES = "calmfeed_usage";
    private static final String USAGE_DATE = "usage_date";
    private static final String USAGE_SECONDS = "usage_seconds";
    private static final String ALLOWANCE_DATE = "allowance_date";
    private static final String ALLOWANCE_MINUTES = "allowance_minutes";
    private static final String EXTRA_DATE = "extra_date";
    private static final String EXTRA_GRANTED = "extra_granted";
    private static final String EXTRA_USED = "extra_used";
    private static final String LAST_GRANT_ID = "last_grant_id";
    private static final String PREF_CUSTOM_DISTRACTIONS = "custom_distraction_sites";

    private static final Set<String> DEFAULT_DISTRACTIONS = new HashSet<>(Arrays.asList(
            "youtube.com", "youtu.be",
            "facebook.com", "fb.com",
            "tiktok.com",
            "instagram.com"
    ));

    private static final String PREF_BROWSING_PREFIX = "browsing_seconds_";
    private static final String PREF_DOMAIN_PREFIX = "domain_sec_";

    private UsageTracker() {}

    static String normalizeDomain(String rawDomain) {
        if (rawDomain == null) return "browsing";
        String u = rawDomain.toLowerCase().trim();
        if (u.contains("youtube.com") || u.contains("youtu.be")) return "youtube.com";
        if (u.contains("tiktok.com")) return "tiktok.com";
        if (u.contains("facebook.com") || u.contains("fb.com")) return "facebook.com";
        if (u.contains("instagram.com")) return "instagram.com";
        u = u.replace("https://", "").replace("http://", "").replace("www.", "");
        int slash = u.indexOf('/');
        if (slash > 0) u = u.substring(0, slash);
        return u.isEmpty() ? "browsing" : u;
    }

    static long getTodaySeconds(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);
        if (!LocalDate.now().toString().equals(preferences.getString(USAGE_DATE, ""))) return 0;
        return preferences.getLong(USAGE_SECONDS, 0);
    }

    static long getTodayBrowsingSeconds(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);
        String today = LocalDate.now().toString();
        return preferences.getLong(PREF_BROWSING_PREFIX + today, 0);
    }

    static long getTodayDomainSeconds(Context context, String domain) {
        SharedPreferences preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);
        String today = LocalDate.now().toString();
        String norm = normalizeDomain(domain);
        return preferences.getLong(PREF_DOMAIN_PREFIX + norm + "_" + today, 0);
    }

    static void recordSiteSeconds(Context context, String domain, long seconds, boolean isSocial, boolean extraTime) {
        if (seconds <= 0) return;
        SharedPreferences preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);
        String today = LocalDate.now().toString();
        SharedPreferences.Editor editor = preferences.edit();

        if (isSocial) {
            long existingSeconds = today.equals(preferences.getString(USAGE_DATE, ""))
                    ? preferences.getLong(USAGE_SECONDS, 0)
                    : 0;
            editor.putString(USAGE_DATE, today)
                    .putLong(USAGE_SECONDS, existingSeconds + seconds);
            if (extraTime) {
                long existingExtra = today.equals(preferences.getString(EXTRA_DATE, ""))
                        ? preferences.getLong(EXTRA_USED, 0)
                        : 0;
                editor.putString(EXTRA_DATE, today)
                        .putLong(EXTRA_USED, existingExtra + seconds);
            }
        } else {
            long existingBrowsing = preferences.getLong(PREF_BROWSING_PREFIX + today, 0);
            editor.putLong(PREF_BROWSING_PREFIX + today, existingBrowsing + seconds);
        }

        String norm = normalizeDomain(domain);
        long existingDomain = preferences.getLong(PREF_DOMAIN_PREFIX + norm + "_" + today, 0);
        editor.putLong(PREF_DOMAIN_PREFIX + norm + "_" + today, existingDomain + seconds);

        editor.apply();
    }

    static void addSessionSeconds(Context context, long seconds, boolean extraTime) {
        recordSiteSeconds(context, "social", seconds, true, extraTime);
    }

    static Set<String> getLimitedSites(Context context) {
        SharedPreferences preferences = preferences(context);
        Set<String> custom = preferences.getStringSet(PREF_CUSTOM_DISTRACTIONS, null);
        Set<String> all = new HashSet<>(DEFAULT_DISTRACTIONS);
        if (custom != null) {
            all.addAll(custom);
        }
        return all;
    }

    static Set<String> getCustomDistractions(Context context) {
        SharedPreferences preferences = preferences(context);
        Set<String> custom = preferences.getStringSet(PREF_CUSTOM_DISTRACTIONS, null);
        return custom != null ? new HashSet<>(custom) : new HashSet<>();
    }

    static boolean isSiteLimited(Context context, String url) {
        if (url == null) return false;
        String u = url.toLowerCase();
        Set<String> limited = getLimitedSites(context);
        for (String domain : limited) {
            if (u.contains(domain)) {
                return true;
            }
        }
        return false;
    }

    static boolean toggleSiteLimit(Context context, String domain) {
        if (domain == null || domain.trim().isEmpty()) return false;
        String clean = domain.trim().toLowerCase().replace("https://", "").replace("http://", "").replace("www.", "");
        int slash = clean.indexOf('/');
        if (slash > 0) clean = clean.substring(0, slash);

        SharedPreferences preferences = preferences(context);
        Set<String> current = new HashSet<>(getCustomDistractions(context));
        boolean wasLimited = isSiteLimited(context, clean);
        if (wasLimited) {
            current.remove(clean);
        } else {
            current.add(clean);
        }
        preferences.edit().putStringSet(PREF_CUSTOM_DISTRACTIONS, current).apply();
        return !wasLimited;
    }

    static boolean isSocialSite(String url) {
        if (url == null) return false;
        String u = url.toLowerCase();
        for (String domain : DEFAULT_DISTRACTIONS) {
            if (u.contains(domain)) return true;
        }
        return false;
    }

    static boolean setTodayAllowance(Context context, int minutes) {
        SharedPreferences preferences = preferences(context);
        String today = LocalDate.now().toString();
        return preferences.edit()
                .putString(ALLOWANCE_DATE, today)
                .putInt(ALLOWANCE_MINUTES, minutes)
                .commit();
    }

    static int getTodayAllowanceMinutes(Context context) {
        SharedPreferences preferences = preferences(context);
        String today = LocalDate.now().toString();
        if (today.equals(preferences.getString(ALLOWANCE_DATE, ""))) {
            return preferences.getInt(ALLOWANCE_MINUTES, 30);
        }
        return 30; // Default 30 minutes social media focus allowance
    }

    static long getDailyRemainingSeconds(Context context) {
        int minutes = getTodayAllowanceMinutes(context);
        return Math.max(0, minutes * 60L - getTodaySeconds(context));
    }

    static boolean addExtraGrant(Context context, String requestId, long seconds) {
        SharedPreferences preferences = preferences(context);
        String today = LocalDate.now().toString();
        if (!today.equals(preferences.getString(ALLOWANCE_DATE, ""))
                || requestId.equals(preferences.getString(LAST_GRANT_ID, ""))) {
            return today.equals(preferences.getString(ALLOWANCE_DATE, ""));
        }
        long granted = today.equals(preferences.getString(EXTRA_DATE, ""))
                ? preferences.getLong(EXTRA_GRANTED, 0)
                : 0;
        long used = today.equals(preferences.getString(EXTRA_DATE, ""))
                ? preferences.getLong(EXTRA_USED, 0)
                : 0;
        return preferences.edit()
                .putString(EXTRA_DATE, today)
                .putLong(EXTRA_GRANTED, granted + seconds)
                .putLong(EXTRA_USED, used)
                .putString(LAST_GRANT_ID, requestId)
                .commit();
    }

    static long getExtraRemainingSeconds(Context context) {
        SharedPreferences preferences = preferences(context);
        String today = LocalDate.now().toString();
        if (!today.equals(preferences.getString(ALLOWANCE_DATE, ""))
                || !today.equals(preferences.getString(EXTRA_DATE, ""))) return 0;
        return Math.max(0, preferences.getLong(EXTRA_GRANTED, 0)
                - preferences.getLong(EXTRA_USED, 0));
    }

    private static SharedPreferences preferences(Context context) {
        return context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);
    }
}
