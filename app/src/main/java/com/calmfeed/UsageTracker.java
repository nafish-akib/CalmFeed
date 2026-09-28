package com.calmfeed;

import android.content.Context;
import android.content.SharedPreferences;

import java.time.LocalDate;

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

    private UsageTracker() {}

    static long getTodaySeconds(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);
        if (!LocalDate.now().toString().equals(preferences.getString(USAGE_DATE, ""))) return 0;
        return preferences.getLong(USAGE_SECONDS, 0);
    }

    static void addSessionSeconds(Context context, long seconds, boolean extraTime) {
        if (seconds <= 0) return;
        SharedPreferences preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);
        String today = LocalDate.now().toString();
        long existingSeconds = today.equals(preferences.getString(USAGE_DATE, ""))
                ? preferences.getLong(USAGE_SECONDS, 0)
                : 0;
        SharedPreferences.Editor editor = preferences.edit()
                .putString(USAGE_DATE, today)
                .putLong(USAGE_SECONDS, existingSeconds + seconds);
        if (extraTime) {
            long existingExtra = today.equals(preferences.getString(EXTRA_DATE, ""))
                    ? preferences.getLong(EXTRA_USED, 0)
                    : 0;
            editor.putString(EXTRA_DATE, today)
                    .putLong(EXTRA_USED, existingExtra + seconds);
        }
        editor.apply();
    }

    static boolean setTodayAllowance(Context context, int minutes) {
        SharedPreferences preferences = preferences(context);
        String today = LocalDate.now().toString();
        if (today.equals(preferences.getString(ALLOWANCE_DATE, ""))) return false;
        return preferences.edit()
                .putString(ALLOWANCE_DATE, today)
                .putInt(ALLOWANCE_MINUTES, minutes)
                .putString(EXTRA_DATE, today)
                .putLong(EXTRA_GRANTED, 0)
                .putLong(EXTRA_USED, 0)
                .putString(LAST_GRANT_ID, "")
                .commit();
    }

    static int getTodayAllowanceMinutes(Context context) {
        SharedPreferences preferences = preferences(context);
        return LocalDate.now().toString().equals(preferences.getString(ALLOWANCE_DATE, ""))
                ? preferences.getInt(ALLOWANCE_MINUTES, 0)
                : 0;
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
