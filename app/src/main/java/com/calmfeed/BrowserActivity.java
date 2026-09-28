package com.calmfeed;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.mozilla.geckoview.GeckoRuntime;
import org.mozilla.geckoview.GeckoSession;
import org.mozilla.geckoview.GeckoView;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class BrowserActivity extends Activity {
    public static final String EXTRA_URL = "com.calmfeed.URL";
    private static GeckoRuntime runtime;

    private GeckoSession session;
    private LinearLayout browserRoot;
    private TextView timerLabel;
    private boolean canGoBack;
    private long endTime;
    private long sessionDurationMillis;
    private long remainingSessionMillis;
    private long activeSegmentStartElapsed = -1;
    private long completedSessionMillis;
    private long recordedSeconds;
    private String sessionId;
    private String sessionSource;
    private boolean browserVisible;
    private final android.os.Handler handler = new android.os.Handler();
    private final Runnable timerTick = new Runnable() {
        @Override
        public void run() {
            long remaining = endTime - SystemClock.elapsedRealtime();
            if (remaining <= 0) {
                finishSession(true);
                return;
            }
            recordUsage(false);
            long minutes = TimeUnit.MILLISECONDS.toMinutes(remaining);
            long seconds = TimeUnit.MILLISECONDS.toSeconds(remaining) % 60;
            timerLabel.setText(String.format(
                    "Social time · %02d:%02d", minutes, seconds));
            handler.postDelayed(this, 1000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(0xFF0C1418);
        getWindow().setNavigationBarColor(0xFF0C1418);

        browserRoot = new LinearLayout(this);
        browserRoot.setOrientation(LinearLayout.VERTICAL);
        browserRoot.setBackgroundColor(0xFF0C1418);

        LinearLayout toolbar = new LinearLayout(this);
        toolbar.setGravity(Gravity.CENTER_VERTICAL);
        toolbar.setPadding(dp(8), dp(4), dp(8), dp(4));
        timerLabel = new TextView(this);
        timerLabel.setTextColor(0xFF91E0B1);
        timerLabel.setTextSize(14);
        timerLabel.setPadding(dp(8), 0, dp(8), 0);
        timerLabel.setText("Checking today's allowance…");
        toolbar.addView(timerLabel, new LinearLayout.LayoutParams(0, dp(48), 1));

        Button back = toolButton("‹");
        back.setOnClickListener(view -> {
            if (session != null && canGoBack) session.goBack();
            else finish();
        });
        toolbar.addView(back);

        Button home = toolButton("Home");
        home.setOnClickListener(view -> finish());
        toolbar.addView(home);
        browserRoot.addView(toolbar);

        setContentView(browserRoot);
        startProtectedSession();
    }

    private void startProtectedSession() {
        Map<String, Object> request = new HashMap<>();
        request.put("timeZone", FirebaseBridge.timezoneId());
        FirebaseBridge.call(this, "startSession", request, (result, error) -> {
            if (isFinishing()) return;
            if (error != null) {
                showSessionUnavailable(error);
                return;
            }
            Object id = result.get("sessionId");
            Object seconds = result.get("allowedSeconds");
            if (!(id instanceof String) || !(seconds instanceof Number)
                    || ((Number) seconds).intValue() <= 0) {
                showSessionUnavailable("Firebase returned an invalid session response.");
                return;
            }
            sessionId = (String) id;
            Object source = result.get("source");
            sessionSource = source instanceof String ? (String) source : "daily";
            sessionDurationMillis = TimeUnit.SECONDS.toMillis(((Number) seconds).longValue());
            remainingSessionMillis = sessionDurationMillis;
            if (!browserVisible) {
                endRemoteSession();
                finish();
                return;
            }
            openGeckoView();
        });
    }

    private void openGeckoView() {
        if (isFinishing()) return;
        if (runtime == null) runtime = GeckoRuntime.create(getApplicationContext());
        session = new GeckoSession();
        session.open(runtime);
        session.setNavigationDelegate(new GeckoSession.NavigationDelegate() {
            @Override
            public void onCanGoBack(GeckoSession currentSession, boolean allowed) {
                canGoBack = allowed;
            }
        });

        GeckoView geckoView = new GeckoView(this);
        geckoView.setSession(session);
        browserRoot.addView(geckoView, new LinearLayout.LayoutParams(-1, 0, 1));
        installFiltersAndLoad(getIntent().getStringExtra(EXTRA_URL));
        startTimerIfReady();
    }

    private void installFiltersAndLoad(String url) {
        runtime.getWebExtensionController()
                .ensureBuiltIn(
                        "resource://android/assets/extensions/calmfeed/",
                        "calmfeed-filters@calmfeed.app")
                .accept(
                        extension -> runOnUiThread(() -> {
                            if (session != null && !isFinishing()) session.loadUri(url);
                        }),
                        error -> runOnUiThread(() -> {
                            if (isFinishing()) return;
                            Toast.makeText(this, "Feed filters could not start.", Toast.LENGTH_LONG).show();
                            if (session != null) session.loadUri(url);
                        }));
    }

    private void showSessionUnavailable(String error) {
        boolean exhausted = error.toLowerCase(java.util.Locale.ROOT).contains("used up");
        AlertDialog.Builder dialog = new AlertDialog.Builder(this)
                .setTitle(exhausted ? "Today's time is up" : "Browsing is paused")
                .setMessage(error);
        if (exhausted) {
            dialog.setPositiveButton("Ask for more time", (window, which) -> requestMoreTime());
        }
        dialog.setNegativeButton("Home", (window, which) -> finish());
        dialog.setOnCancelListener(window -> finish());
        dialog.show();
    }

    private void requestMoreTime() {
        Map<String, Object> request = new HashMap<>();
        request.put("timeZone", FirebaseBridge.timezoneId());
        FirebaseBridge.call(this, "requestExtraTime", request, (result, error) -> {
            if (isFinishing()) return;
            if (error != null) {
                finish();
                Toast.makeText(this, error, Toast.LENGTH_LONG).show();
                if (error.toLowerCase(java.util.Locale.ROOT).contains("pair")) {
                    startActivity(new Intent(this, TrustActivity.class));
                }
                return;
            }
            finish();
            Toast.makeText(
                    this,
                    "Request sent. Your trusted person can approve extra time in CalmFeed.",
                    Toast.LENGTH_LONG
            ).show();
        });
    }

    private Button toolButton(String label) {
        Button button = new Button(this);
        button.setAllCaps(false);
        button.setText(label);
        button.setTextColor(0xFFEFF6F0);
        button.setMinWidth(dp(48));
        return button;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void recordUsage(boolean force) {
        if (activeSegmentStartElapsed < 0 || sessionDurationMillis <= 0) return;
        long activeMillis = SystemClock.elapsedRealtime() - activeSegmentStartElapsed;
        long elapsed = Math.min(
                completedSessionMillis + activeMillis,
                sessionDurationMillis);
        long elapsedSeconds = TimeUnit.MILLISECONDS.toSeconds(elapsed);
        long unrecordedSeconds = elapsedSeconds - recordedSeconds;
        if (unrecordedSeconds > 0 && (force || unrecordedSeconds >= 30)) {
            UsageTracker.addSessionSeconds(this, unrecordedSeconds, "extra".equals(sessionSource));
            recordedSeconds = elapsedSeconds;
        }
    }

    private void finishSession(boolean timedOut) {
        handler.removeCallbacks(timerTick);
        recordUsage(true);
        endRemoteSession();
        if (!isFinishing()) {
            finish();
            if (timedOut) {
                Toast.makeText(
                        this,
                        "This session is complete. Your remaining daily time stays available in CalmFeed.",
                        Toast.LENGTH_LONG
                ).show();
            }
        }
    }

    private void endRemoteSession() {
        if (sessionId == null) return;
        String endingSessionId = sessionId;
        sessionId = null;
        Map<String, Object> request = new HashMap<>();
        request.put("sessionId", endingSessionId);
        FirebaseBridge.call(this, "endSession", request, (result, error) -> {
            if (error != null) {
                android.util.Log.e("CalmFeed", "Could not return unused time to today's allowance.", errorCause(error));
            }
        });
    }

    private Throwable errorCause(String message) {
        return new IllegalStateException(message);
    }

    @Override
    public void onBackPressed() {
        if (session != null && canGoBack) session.goBack();
        else finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        browserVisible = true;
        startTimerIfReady();
    }

    private void startTimerIfReady() {
        if (!browserVisible || session == null || remainingSessionMillis <= 0
                || activeSegmentStartElapsed >= 0) return;
        activeSegmentStartElapsed = SystemClock.elapsedRealtime();
        endTime = activeSegmentStartElapsed + remainingSessionMillis;
        handler.post(timerTick);
    }

    @Override
    protected void onPause() {
        browserVisible = false;
        handler.removeCallbacks(timerTick);
        recordUsage(true);
        if (activeSegmentStartElapsed >= 0) {
            long now = SystemClock.elapsedRealtime();
            completedSessionMillis = Math.min(
                    sessionDurationMillis,
                    completedSessionMillis + now - activeSegmentStartElapsed);
            remainingSessionMillis = Math.max(0, endTime - now);
            activeSegmentStartElapsed = -1;
        }
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacks(timerTick);
        recordUsage(true);
        endRemoteSession();
        if (session != null) {
            session.close();
            session = null;
        }
        super.onDestroy();
    }
}
