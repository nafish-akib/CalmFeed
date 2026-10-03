package com.calmfeed;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemClock;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.widget.TextView;
import android.widget.Toast;

import org.mozilla.geckoview.AllowOrDeny;
import org.mozilla.geckoview.ContentBlocking;
import org.mozilla.geckoview.GeckoResult;
import org.mozilla.geckoview.GeckoRuntime;
import org.mozilla.geckoview.GeckoSession;
import org.mozilla.geckoview.GeckoSessionSettings;
import org.mozilla.geckoview.GeckoView;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class BrowserActivity extends Activity {
    public static final String EXTRA_URL = "com.calmfeed.URL";
    public static final String EXTRA_INCOGNITO = "com.calmfeed.INCOGNITO";

    private static synchronized GeckoRuntime getSharedRuntime(Context context) {
        GeckoRuntime runtime = GeckoRuntime.getDefault(context.getApplicationContext());
        try {
            ContentBlocking.Settings cb = runtime.getSettings().getContentBlocking();
            cb.setEnhancedTrackingProtectionLevel(ContentBlocking.EtpLevel.STRICT);
            cb.setStrictSocialTrackingProtection(true);
            cb.setCookieBehavior(ContentBlocking.CookieBehavior.ACCEPT_NON_TRACKERS);
        } catch (Exception ignored) {}
        return runtime;
    }

    // Theme Colors
    private static final int BG_COLOR = Color.rgb(15, 23, 42); // Slate 900
    private static final int INCOGNITO_BG = Color.rgb(18, 19, 28); // Midnight
    private static final int SURFACE_COLOR = Color.rgb(30, 41, 59); // Slate 800
    private static final int TEXT_COLOR = Color.rgb(241, 245, 249);
    private static final int MUTED_COLOR = Color.rgb(148, 163, 184);
    private static final int ACCENT_COLOR = Color.rgb(52, 211, 153); // Emerald/Mint
    private static final int ACCENT_BLUE = Color.rgb(56, 189, 248); // Sky blue
    private static final int WARNING_COLOR = Color.rgb(251, 146, 60);

    private GeckoSession session;
    private GeckoView geckoView;
    private FrameLayout containerView;
    private LinearLayout browserRoot;
    private LinearLayout appBarLayout;
    private LinearLayout bottomBar;
    private EditText urlInput;
    private TextView securityIcon;
    private TextView clearUrlButton;
    private TextView reloadStopButton;
    private TextView readerButton;
    private TextView tabsButton;
    private TextView menuButton;
    private ProgressBar pageProgressBar;
    private TextView timerLabel;
    private Button backButton;
    private Button forwardButton;
    private Button homeButton;
    private Button bookmarkButton;
    private TextView scrollToTopButton;

    // Tab System
    public static class TabItem {
        public String id;
        public String url;
        public String title;
        public GeckoSession session;
        public int blockedTrackersCount = 0;
        public int lastSecurityStatus = 0;

        public TabItem(String id, String url, String title, GeckoSession session) {
            this.id = id;
            this.url = url;
            this.title = title;
            this.session = session;
            this.blockedTrackersCount = 0;
            this.lastSecurityStatus = 0;
        }
    }

    private final List<TabItem> tabs = new ArrayList<>();
    private int activeTabIndex = 0;

    private boolean isIncognito = false;
    private boolean isDesktopMode = false;
    private boolean isFullScreen = false;
    private boolean isReaderMode = false;
    private boolean canGoBack = false;
    private boolean canGoForward = false;
    private boolean isLoading = false;
    private String currentUrl = "";
    private String currentTitle = "";

    // Timer & usage tracking
    private long endTime;
    private long sessionDurationMillis = 0;
    private long remainingSessionMillis = 0;
    private long activeSegmentStartElapsed = -1;
    private long completedSessionMillis = 0;
    private long recordedSeconds = 0;
    private long activeBrowsingStartElapsed = -1;

    // Find in Page
    private LinearLayout findInPageLayout;
    private EditText findQueryInput;
    private TextView findMatchCount;

    private String sessionId;
    private String sessionSource;
    private boolean browserVisible;
    private final android.os.Handler handler = new android.os.Handler();
    private final Runnable timerTick = new Runnable() {
        @Override
        public void run() {
            if (endTime <= 0) return;
            long remaining = endTime - SystemClock.elapsedRealtime();
            if (remaining <= 0) {
                finishSession(true);
                return;
            }
            recordUsage(false);
            long minutes = TimeUnit.MILLISECONDS.toMinutes(remaining);
            long seconds = TimeUnit.MILLISECONDS.toSeconds(remaining) % 60;
            timerLabel.setText(String.format(Locale.getDefault(), "⏱️ %02d:%02d (Social)", minutes, seconds));
            timerLabel.setTextColor(Color.rgb(251, 146, 60));
            handler.postDelayed(this, 1000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        isIncognito = getIntent().getBooleanExtra(EXTRA_INCOGNITO, false);
        int currentBg = isIncognito ? INCOGNITO_BG : BG_COLOR;

        getWindow().setStatusBarColor(currentBg);
        getWindow().setNavigationBarColor(currentBg);

        browserRoot = new LinearLayout(this);
        browserRoot.setOrientation(LinearLayout.VERTICAL);
        browserRoot.setBackgroundColor(currentBg);

        // 1. Omnibox / Top App Bar
        buildTopAppBar();
        browserRoot.addView(appBarLayout);

        // 1b. Find in Page Bar
        buildFindInPageBar();
        browserRoot.addView(findInPageLayout);

        // 2. Loading Progress Bar
        pageProgressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        pageProgressBar.setProgress(0);
        pageProgressBar.setMax(100);
        pageProgressBar.setVisibility(View.GONE);
        pageProgressBar.setProgressTintList(android.content.res.ColorStateList.valueOf(isIncognito ? ACCENT_BLUE : ACCENT_COLOR));
        browserRoot.addView(pageProgressBar, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(3)));

        // 3. Middle container for GeckoView
        containerView = new FrameLayout(this);
        browserRoot.addView(containerView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f));

        // 4. Chrome-like Bottom Navigation Bar
        buildBottomBar();
        browserRoot.addView(bottomBar);

        setContentView(browserRoot);

        initSessionAllowance();
    }

    private void buildTopAppBar() {
        appBarLayout = new LinearLayout(this);
        appBarLayout.setOrientation(LinearLayout.HORIZONTAL);
        appBarLayout.setGravity(Gravity.CENTER_VERTICAL);
        appBarLayout.setPadding(dp(8), dp(8), dp(8), dp(6));
        appBarLayout.setBackgroundColor(isIncognito ? INCOGNITO_BG : BG_COLOR);

        // Address bar capsule
        LinearLayout addressPill = new LinearLayout(this);
        addressPill.setOrientation(LinearLayout.HORIZONTAL);
        addressPill.setGravity(Gravity.CENTER_VERTICAL);
        addressPill.setPadding(dp(12), dp(2), dp(8), dp(2));
        GradientDrawable pillBg = new GradientDrawable();
        pillBg.setColor(isIncognito ? Color.rgb(28, 30, 44) : SURFACE_COLOR);
        pillBg.setCornerRadius(dp(22));
        pillBg.setStroke(dp(1), isIncognito ? Color.rgb(55, 65, 81) : Color.rgb(51, 65, 85));
        addressPill.setBackground(pillBg);

        // Security / Incognito Icon - tap to view and edit site permissions
        securityIcon = new TextView(this);
        securityIcon.setTextSize(14);
        securityIcon.setPadding(0, 0, dp(6), 0);
        securityIcon.setText(isIncognito ? "🕶️" : "🔒");
        securityIcon.setClickable(true);
        securityIcon.setContentDescription("Site security and permissions");
        securityIcon.setOnClickListener(v -> showQuickSitePermissionsDialog());
        addressPill.addView(securityIcon, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        // URL / Search input
        urlInput = new EditText(this);
        urlInput.setSingleLine(true);
        urlInput.setTextSize(14);
        urlInput.setTextColor(TEXT_COLOR);
        urlInput.setHintTextColor(MUTED_COLOR);
        urlInput.setHint(isIncognito ? "Search privately or enter URL" : "Search or enter web address");
        urlInput.setBackground(null);
        urlInput.setImeOptions(EditorInfo.IME_ACTION_GO);
        urlInput.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_URI);
        urlInput.setPadding(dp(4), dp(8), dp(4), dp(8));

        urlInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_GO || actionId == EditorInfo.IME_ACTION_SEARCH
                    || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                String input = urlInput.getText().toString().trim();
                navigateToQueryOrUrl(input);
                urlInput.clearFocus();
                hideKeyboard();
                return true;
            }
            return false;
        });

        urlInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                clearUrlButton.setVisibility(urlInput.getText().length() > 0 ? View.VISIBLE : View.GONE);
                urlInput.selectAll();
            } else {
                clearUrlButton.setVisibility(View.GONE);
                updateDisplayUrl(currentUrl);
            }
        });

        urlInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (urlInput.hasFocus()) {
                    clearUrlButton.setVisibility(s.length() > 0 ? View.VISIBLE : View.GONE);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        LinearLayout.LayoutParams inputParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        addressPill.addView(urlInput, inputParams);

        // Clear button (✕)
        clearUrlButton = new TextView(this);
        clearUrlButton.setText("✕");
        clearUrlButton.setTextSize(14);
        clearUrlButton.setTextColor(MUTED_COLOR);
        clearUrlButton.setPadding(dp(8), dp(6), dp(8), dp(6));
        clearUrlButton.setVisibility(View.GONE);
        clearUrlButton.setOnClickListener(v -> urlInput.setText(""));
        addressPill.addView(clearUrlButton);

        // Clean Reader View button (📖) - for distraction-free reading
        readerButton = new TextView(this);
        readerButton.setText("📖");
        readerButton.setTextSize(16);
        readerButton.setPadding(dp(6), dp(4), dp(6), dp(4));
        readerButton.setContentDescription("Toggle clean Reader View");
        readerButton.setOnClickListener(v -> toggleReaderView());
        addressPill.addView(readerButton);

        // Reload / Stop button
        reloadStopButton = new TextView(this);
        reloadStopButton.setText("↻");
        reloadStopButton.setTextSize(18);
        reloadStopButton.setTextColor(TEXT_COLOR);
        reloadStopButton.setPadding(dp(6), dp(4), dp(6), dp(4));
        reloadStopButton.setOnClickListener(v -> {
            if (session != null) {
                if (isLoading) {
                    session.stop();
                } else {
                    session.reload();
                }
            }
        });
        addressPill.addView(reloadStopButton);

        appBarLayout.addView(addressPill, new LinearLayout.LayoutParams(0, dp(44), 1.0f));

        // Tab Tray Counter Button [ 1 ]
        tabsButton = new TextView(this);
        tabsButton.setText("1");
        tabsButton.setTextSize(12);
        tabsButton.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        tabsButton.setTextColor(TEXT_COLOR);
        tabsButton.setGravity(Gravity.CENTER);
        tabsButton.setPadding(dp(6), dp(4), dp(6), dp(4));
        GradientDrawable tabBadgeBg = new GradientDrawable();
        tabBadgeBg.setColor(SURFACE_COLOR);
        tabBadgeBg.setCornerRadius(dp(8));
        tabBadgeBg.setStroke(dp(1), Color.rgb(51, 65, 85));
        tabsButton.setBackground(tabBadgeBg);
        tabsButton.setContentDescription("Open Tab Switcher Tray");
        tabsButton.setOnClickListener(v -> showTabTray());
        LinearLayout.LayoutParams tabBadgeParams = new LinearLayout.LayoutParams(dp(32), dp(32));
        tabBadgeParams.leftMargin = dp(6);
        appBarLayout.addView(tabsButton, tabBadgeParams);

        // Overflow Menu button (⋮)
        menuButton = new TextView(this);
        menuButton.setText("⋮");
        menuButton.setTextSize(22);
        menuButton.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        menuButton.setTextColor(TEXT_COLOR);
        menuButton.setGravity(Gravity.CENTER);
        menuButton.setPadding(dp(12), dp(4), dp(8), dp(4));
        menuButton.setOnClickListener(this::showOverflowMenu);
        appBarLayout.addView(menuButton, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(44)));
    }

    private void buildFindInPageBar() {
        findInPageLayout = new LinearLayout(this);
        findInPageLayout.setOrientation(LinearLayout.HORIZONTAL);
        findInPageLayout.setGravity(Gravity.CENTER_VERTICAL);
        findInPageLayout.setPadding(dp(10), dp(4), dp(8), dp(4));
        findInPageLayout.setBackgroundColor(Color.rgb(15, 23, 42));
        findInPageLayout.setVisibility(View.GONE);

        TextView icon = new TextView(this);
        icon.setText("🔍");
        icon.setTextSize(14);
        icon.setPadding(0, 0, dp(6), 0);
        findInPageLayout.addView(icon);

        findQueryInput = new EditText(this);
        findQueryInput.setHint("Find in page…");
        findQueryInput.setHintTextColor(MUTED_COLOR);
        findQueryInput.setTextColor(TEXT_COLOR);
        findQueryInput.setTextSize(14);
        findQueryInput.setBackgroundColor(Color.TRANSPARENT);
        findQueryInput.setSingleLine(true);
        findQueryInput.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
        findQueryInput.setOnEditorActionListener((v, actionId, event) -> {
            executeFind(false);
            return true;
        });
        findQueryInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                executeFind(false);
            }
        });
        findInPageLayout.addView(findQueryInput, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

        findMatchCount = new TextView(this);
        findMatchCount.setText("0/0");
        findMatchCount.setTextColor(ACCENT_COLOR);
        findMatchCount.setTextSize(12);
        findMatchCount.setPadding(dp(4), 0, dp(6), 0);
        findInPageLayout.addView(findMatchCount);

        Button prevBtn = navButton("▲", "Previous Match");
        prevBtn.setTextSize(14);
        prevBtn.setOnClickListener(v -> executeFind(true));
        findInPageLayout.addView(prevBtn);

        Button nextBtn = navButton("▼", "Next Match");
        nextBtn.setTextSize(14);
        nextBtn.setOnClickListener(v -> executeFind(false));
        findInPageLayout.addView(nextBtn);

        Button closeBtn = navButton("✕", "Close Find");
        closeBtn.setTextSize(14);
        closeBtn.setOnClickListener(v -> hideFindInPage());
        findInPageLayout.addView(closeBtn);
    }

    private void showFindInPage() {
        if (findInPageLayout == null) return;
        findInPageLayout.setVisibility(View.VISIBLE);
        findQueryInput.requestFocus();
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null) imm.showSoftInput(findQueryInput, InputMethodManager.SHOW_IMPLICIT);
        executeFind(false);
    }

    private void hideFindInPage() {
        if (findInPageLayout == null) return;
        findInPageLayout.setVisibility(View.GONE);
        if (session != null) {
            session.getFinder().clear();
        }
        findMatchCount.setText("0/0");
        findQueryInput.clearFocus();
        hideKeyboard();
    }

    private void executeFind(boolean backwards) {
        if (session == null || findQueryInput == null) return;
        String query = findQueryInput.getText().toString();
        if (TextUtils.isEmpty(query)) {
            session.getFinder().clear();
            findMatchCount.setText("0/0");
            return;
        }
        int flags = backwards ? GeckoSession.FINDER_FIND_BACKWARDS : 0;
        session.getFinder().find(query, flags).then(res -> {
            runOnUiThread(() -> {
                if (res != null && res.found) {
                    findMatchCount.setText(res.current + "/" + res.total);
                } else {
                    findMatchCount.setText("0/0");
                }
            });
            return null;
        });
    }

    private void buildBottomBar() {
        bottomBar = new LinearLayout(this);
        bottomBar.setOrientation(LinearLayout.HORIZONTAL);
        bottomBar.setGravity(Gravity.CENTER_VERTICAL);
        bottomBar.setPadding(dp(4), dp(2), dp(4), dp(2));
        bottomBar.setBackgroundColor(isIncognito ? INCOGNITO_BG : BG_COLOR);

        backButton = navButton("‹", "Back");
        backButton.setEnabled(false);
        backButton.setAlpha(0.4f);
        backButton.setOnClickListener(v -> {
            if (session != null && canGoBack) session.goBack();
        });
        bottomBar.addView(backButton);

        forwardButton = navButton("›", "Forward");
        forwardButton.setEnabled(false);
        forwardButton.setAlpha(0.4f);
        forwardButton.setOnClickListener(v -> {
            if (session != null && canGoForward) session.goForward();
        });
        bottomBar.addView(forwardButton);

        homeButton = navButton("🏠", "Home");
        homeButton.setOnClickListener(v -> finish());
        bottomBar.addView(homeButton);

        bookmarkButton = navButton("☆", "Bookmark");
        bookmarkButton.setOnClickListener(v -> toggleBookmarkCurrentPage());
        bottomBar.addView(bookmarkButton);

        // Timer badge or focus pill
        timerLabel = new TextView(this);
        timerLabel.setTextColor(isIncognito ? ACCENT_BLUE : ACCENT_COLOR);
        timerLabel.setTextSize(12);
        timerLabel.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        timerLabel.setGravity(Gravity.CENTER_VERTICAL | Gravity.END);
        timerLabel.setPadding(dp(8), dp(4), dp(12), dp(4));
        timerLabel.setText(isIncognito ? "🕶️ Private" : "🌿 Calm");
        bottomBar.addView(timerLabel, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));
    }

    private Button navButton(String text, String contentDescription) {
        Button btn = new Button(this);
        btn.setText(text);
        btn.setTextSize(18);
        btn.setTextColor(TEXT_COLOR);
        btn.setBackgroundColor(Color.TRANSPARENT);
        btn.setContentDescription(contentDescription);
        btn.setPadding(dp(6), dp(4), dp(6), dp(4));
        btn.setMinWidth(dp(44));
        btn.setMinHeight(dp(44));
        return btn;
    }

    private void initSessionAllowance() {
        String target = getIntent().getStringExtra(EXTRA_URL);
        if (target == null) target = "https://www.google.com";
        currentUrl = target;
        updateSiteMode(target);
        openGeckoView();
    }

    private void updateSiteMode(String url) {
        if (isFinishing()) return;
        boolean isSocial = UsageTracker.isSiteLimited(this, url);

        if (isSocial) {
            recordBrowsingUsage();
            activeBrowsingStartElapsed = -1;
            // SOCIAL MEDIA: Check and enforce daily limit!
            long remainingSec = UsageTracker.getDailyRemainingSeconds(this) + UsageTracker.getExtraRemainingSeconds(this);
            int allowanceMin = UsageTracker.getTodayAllowanceMinutes(this);

            if (remainingSec <= 0) {
                // Out of time for social media today!
                pauseSocialTimer();
                showSocialLimitExhaustedDialog();
                return;
            }

            sessionDurationMillis = TimeUnit.MINUTES.toMillis(allowanceMin);
            remainingSessionMillis = TimeUnit.SECONDS.toMillis(remainingSec);
            sessionSource = "device";

            if (browserVisible) {
                startTimerIfReady();
            }
        } else {
            // BROWSING MODE: Unlimited! No timer cut-offs
            pauseSocialTimer();
            sessionDurationMillis = 0;
            remainingSessionMillis = 0;
            if (browserVisible && !isIncognito) {
                activeBrowsingStartElapsed = SystemClock.elapsedRealtime();
            }
            timerLabel.setText(isIncognito ? "🕶️ Private Browsing" : "🌐 Browsing Mode");
            timerLabel.setTextColor(isIncognito ? ACCENT_BLUE : ACCENT_COLOR);
        }

        if (readerButton != null) {
            readerButton.setVisibility(isSocial ? View.GONE : View.VISIBLE);
        }
    }

    private void toggleReaderView() {
        if (session != null) {
            isReaderMode = !isReaderMode;
            if (readerButton != null) {
                readerButton.setTextColor(isReaderMode ? ACCENT_COLOR : TEXT_COLOR);
            }
            session.loadUri("javascript:(function(){" +
                    "if(typeof window.__calmfeed_toggle_reader==='function'){" +
                    "  window.__calmfeed_toggle_reader();" +
                    "}else{" +
                    "  window.dispatchEvent(new CustomEvent('calmfeed:toggle-reader'));" +
                    "}" +
                    "})();");
        }
    }

    private void pauseSocialTimer() {
        handler.removeCallbacks(timerTick);
        recordUsage(true);
        if (activeSegmentStartElapsed >= 0) {
            long now = SystemClock.elapsedRealtime();
            completedSessionMillis = Math.min(sessionDurationMillis, completedSessionMillis + now - activeSegmentStartElapsed);
            remainingSessionMillis = Math.max(0, endTime - now);
            activeSegmentStartElapsed = -1;
        }
    }

    private void showSocialLimitExhaustedDialog() {
        if (session != null) {
            session.stop();
        }
        runOnUiThread(() -> {
            if (isFinishing()) return;
            new AlertDialog.Builder(BrowserActivity.this)
                    .setTitle("🛑 Daily Social Limit Reached")
                    .setMessage("You have reached your daily focus allowance for social sites (YouTube, Facebook, TikTok, Instagram).\n\nGeneral Browsing Mode (Google, Wikipedia, articles, news) is completely unlimited.")
                    .setPositiveButton("🌐 Browsing Mode (Google)", (d, w) -> {
                        if (session != null) {
                            session.loadUri("https://www.google.com");
                        }
                    })
                    .setNeutralButton("🧘 1-Min Mindful Breath", (d, w) -> {
                        MindfulBreathingHelper.show(BrowserActivity.this, () -> {
                            if (session != null) {
                                session.loadUri("https://www.google.com");
                            }
                        });
                    })
                    .setNegativeButton("🏠 Return Home", (d, w) -> finish())
                    .setCancelable(false)
                    .show();
        });
    }

    private void openGeckoView() {
        if (isFinishing()) return;

        geckoView = new GeckoView(this);
        containerView.removeAllViews();
        containerView.addView(geckoView, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        // Floating Scroll To Top Pill
        scrollToTopButton = new TextView(this);
        scrollToTopButton.setText("↑ Top");
        scrollToTopButton.setTextSize(12);
        scrollToTopButton.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        scrollToTopButton.setTextColor(Color.WHITE);
        scrollToTopButton.setPadding(dp(12), dp(8), dp(12), dp(8));
        GradientDrawable sttBg = new GradientDrawable();
        sttBg.setColor(Color.argb(215, 15, 23, 42));
        sttBg.setCornerRadius(dp(18));
        sttBg.setStroke(dp(1), ACCENT_COLOR);
        scrollToTopButton.setBackground(sttBg);
        scrollToTopButton.setVisibility(View.GONE);
        scrollToTopButton.setOnClickListener(v -> {
            if (session != null) {
                session.loadUri("javascript:window.scrollTo({top:0,behavior:'smooth'});");
            }
        });
        FrameLayout.LayoutParams sttParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        sttParams.gravity = Gravity.BOTTOM | Gravity.END;
        sttParams.bottomMargin = dp(20);
        sttParams.rightMargin = dp(16);
        containerView.addView(scrollToTopButton, sttParams);

        // Load starting URL
        String targetUrl = getIntent().getStringExtra(EXTRA_URL);
        if (TextUtils.isEmpty(targetUrl)) {
            targetUrl = "https://www.google.com";
        }

        createNewTab(targetUrl);
    }

    private GeckoSession createGeckoSession(TabItem tab) {
        GeckoRuntime runtime = getSharedRuntime(this);

        GeckoSessionSettings.Builder settingsBuilder = new GeckoSessionSettings.Builder();
        if (isIncognito) {
            settingsBuilder.usePrivateMode(true);
        }
        if (isDesktopMode) {
            settingsBuilder.userAgentMode(GeckoSessionSettings.USER_AGENT_MODE_DESKTOP);
        }

        GeckoSession s = new GeckoSession(settingsBuilder.build());
        s.open(runtime);

        // 1. ContentDelegate for FullScreen, Title, Context Menu, and Crash Recovery
        s.setContentDelegate(new GeckoSession.ContentDelegate() {
            @Override
            public void onFullScreen(GeckoSession geckoSession, boolean fullScreen) {
                runOnUiThread(() -> handleFullScreen(fullScreen));
            }

            @Override
            public void onTitleChange(GeckoSession geckoSession, String title) {
                runOnUiThread(() -> {
                    tab.title = title != null ? title : "";
                    if (s == session) {
                        currentTitle = tab.title;
                        if (!urlInput.hasFocus()) {
                            updateDisplayUrl(currentUrl);
                        }
                    }
                });
            }

            @Override
            public void onContextMenu(GeckoSession geckoSession, int screenX, int screenY, GeckoSession.ContentDelegate.ContextElement element) {
                runOnUiThread(() -> showWebContextMenu(element));
            }

            @Override
            public void onCrash(GeckoSession geckoSession) {
                runOnUiThread(() -> handleContentCrash(s));
            }

            @Override
            public void onKill(GeckoSession geckoSession) {
                runOnUiThread(() -> handleContentCrash(s));
            }
        });

        // 2. ContentBlocking.Delegate for Real-Time Enhanced Tracking Protection (ETP)
        s.setContentBlockingDelegate(new ContentBlocking.Delegate() {
            @Override
            public void onContentBlocked(GeckoSession geckoSession, ContentBlocking.BlockEvent event) {
                runOnUiThread(() -> {
                    tab.blockedTrackersCount++;
                    if (s == session) {
                        updateSecurityBadge();
                    }
                });
            }
        });

        // 3. ScrollDelegate for Smooth Scroll Depth & Fast "Back to Top"
        s.setScrollDelegate(new GeckoSession.ScrollDelegate() {
            @Override
            public void onScrollChanged(GeckoSession geckoSession, int scrollX, int scrollY) {
                runOnUiThread(() -> {
                    if (geckoSession == session && scrollToTopButton != null) {
                        scrollToTopButton.setVisibility(scrollY > dp(350) ? View.VISIBLE : View.GONE);
                    }
                });
            }
        });

        // 4. HistoryDelegate for SPA client-side route tracking
        s.setHistoryDelegate(new GeckoSession.HistoryDelegate() {
            @Override
            public GeckoResult<Boolean> onVisited(GeckoSession geckoSession, String url, String lastVisitedUrl, int flags) {
                runOnUiThread(() -> {
                    if (geckoSession == session && !TextUtils.isEmpty(url)) {
                        currentUrl = url;
                        tab.url = url;
                        if (!urlInput.hasFocus()) {
                            updateDisplayUrl(url);
                        }
                        updateSiteMode(url);
                        if (!isIncognito) {
                            String titleToSave = !TextUtils.isEmpty(currentTitle) ? currentTitle : url;
                            BrowserHistoryManager.add(BrowserActivity.this, titleToSave, url);
                        }
                    }
                });
                return GeckoResult.fromValue(true);
            }
        });

        // 5. PromptDelegate for Web Alert, Confirm, Prompt, and BeforeUnload Dialogs
        s.setPromptDelegate(new GeckoSession.PromptDelegate() {
            @Override
            public GeckoResult<PromptResponse> onAlertPrompt(GeckoSession geckoSession, AlertPrompt prompt) {
                GeckoResult<PromptResponse> res = new GeckoResult<>();
                runOnUiThread(() -> {
                    if (isFinishing()) {
                        res.complete(prompt.dismiss());
                        return;
                    }
                    new AlertDialog.Builder(BrowserActivity.this)
                            .setTitle(!TextUtils.isEmpty(prompt.title) ? prompt.title : "Web Alert")
                            .setMessage(prompt.message != null ? prompt.message : "")
                            .setPositiveButton("OK", (d, w) -> res.complete(prompt.dismiss()))
                            .setOnCancelListener(d -> res.complete(prompt.dismiss()))
                            .show();
                });
                return res;
            }

            @Override
            public GeckoResult<PromptResponse> onButtonPrompt(GeckoSession geckoSession, ButtonPrompt prompt) {
                GeckoResult<PromptResponse> res = new GeckoResult<>();
                runOnUiThread(() -> {
                    if (isFinishing()) {
                        res.complete(prompt.dismiss());
                        return;
                    }
                    new AlertDialog.Builder(BrowserActivity.this)
                            .setTitle(!TextUtils.isEmpty(prompt.title) ? prompt.title : "Confirm Action")
                            .setMessage(prompt.message != null ? prompt.message : "")
                            .setPositiveButton("OK", (d, w) -> res.complete(prompt.confirm(0)))
                            .setNegativeButton("Cancel", (d, w) -> res.complete(prompt.dismiss()))
                            .setOnCancelListener(d -> res.complete(prompt.dismiss()))
                            .show();
                });
                return res;
            }

            @Override
            public GeckoResult<PromptResponse> onTextPrompt(GeckoSession geckoSession, TextPrompt prompt) {
                GeckoResult<PromptResponse> res = new GeckoResult<>();
                runOnUiThread(() -> {
                    if (isFinishing()) {
                        res.complete(prompt.dismiss());
                        return;
                    }
                    EditText input = new EditText(BrowserActivity.this);
                    input.setTextColor(TEXT_COLOR);
                    input.setPadding(dp(16), dp(12), dp(16), dp(12));
                    if (prompt.defaultValue != null) {
                        input.setText(prompt.defaultValue);
                        input.setSelection(prompt.defaultValue.length());
                    }
                    new AlertDialog.Builder(BrowserActivity.this)
                            .setTitle(!TextUtils.isEmpty(prompt.title) ? prompt.title : "Input Required")
                            .setMessage(prompt.message != null ? prompt.message : "")
                            .setView(input)
                            .setPositiveButton("Submit", (d, w) -> res.complete(prompt.confirm(input.getText().toString())))
                            .setNegativeButton("Cancel", (d, w) -> res.complete(prompt.dismiss()))
                            .setOnCancelListener(d -> res.complete(prompt.dismiss()))
                            .show();
                });
                return res;
            }

            @Override
            public GeckoResult<PromptResponse> onBeforeUnloadPrompt(GeckoSession geckoSession, BeforeUnloadPrompt prompt) {
                GeckoResult<PromptResponse> res = new GeckoResult<>();
                runOnUiThread(() -> {
                    if (isFinishing()) {
                        res.complete(prompt.dismiss());
                        return;
                    }
                    new AlertDialog.Builder(BrowserActivity.this)
                            .setTitle("Leave Web Page?")
                            .setMessage("You have unsaved changes that may be lost.")
                            .setPositiveButton("Leave", (d, w) -> res.complete(prompt.confirm(AllowOrDeny.ALLOW)))
                            .setNegativeButton("Stay", (d, w) -> res.complete(prompt.confirm(AllowOrDeny.DENY)))
                            .setOnCancelListener(d -> res.complete(prompt.dismiss()))
                            .show();
                });
                return res;
            }
        });

        // 6. NavigationDelegate for Back/Forward state and App Scheme blocking
        s.setNavigationDelegate(new GeckoSession.NavigationDelegate() {
            @Override
            public GeckoResult<AllowOrDeny> onLoadRequest(GeckoSession geckoSession, GeckoSession.NavigationDelegate.LoadRequest request) {
                if (request != null && request.uri != null) {
                    String lower = request.uri.toLowerCase();
                    // Block app intents and custom schemes so TikTok/Instagram stream inside browser
                    if (lower.startsWith("snssdk") || lower.startsWith("tiktok:") || lower.startsWith("intent:") || lower.startsWith("market:")) {
                        return GeckoResult.fromValue(AllowOrDeny.DENY);
                    }
                }
                return GeckoResult.fromValue(AllowOrDeny.ALLOW);
            }

            @Override
            public void onCanGoBack(GeckoSession geckoSession, boolean allowed) {
                runOnUiThread(() -> {
                    if (s == session) {
                        canGoBack = allowed;
                        backButton.setEnabled(allowed);
                        backButton.setAlpha(allowed ? 1.0f : 0.4f);
                    }
                });
            }

            @Override
            public void onCanGoForward(GeckoSession geckoSession, boolean allowed) {
                runOnUiThread(() -> {
                    if (s == session) {
                        canGoForward = allowed;
                        forwardButton.setEnabled(allowed);
                        forwardButton.setAlpha(allowed ? 1.0f : 0.4f);
                    }
                });
            }
        });

        // 7. ProgressDelegate for Page Load Progress & History recording
        s.setProgressDelegate(new GeckoSession.ProgressDelegate() {
            @Override
            public void onPageStart(GeckoSession geckoSession, String url) {
                runOnUiThread(() -> {
                    tab.url = url;
                    tab.blockedTrackersCount = 0;
                    if (s == session) {
                        isLoading = true;
                        currentUrl = url;
                        updateSiteMode(url);
                        updateSecurityBadge();
                        reloadStopButton.setText("✕");
                        pageProgressBar.setVisibility(View.VISIBLE);
                        pageProgressBar.setProgress(15);
                        if (!urlInput.hasFocus()) {
                            updateDisplayUrl(url);
                        }
                        updateBookmarkIcon();
                    }
                });
            }

            @Override
            public void onProgressChange(GeckoSession geckoSession, int progress) {
                runOnUiThread(() -> {
                    if (s == session) {
                        pageProgressBar.setProgress(progress);
                    }
                });
            }

            @Override
            public void onPageStop(GeckoSession geckoSession, boolean success) {
                runOnUiThread(() -> {
                    if (s == session) {
                        isLoading = false;
                        reloadStopButton.setText("↻");
                        pageProgressBar.setVisibility(View.GONE);
                        if (!urlInput.hasFocus()) {
                            updateDisplayUrl(currentUrl);
                        }
                        updateBookmarkIcon();
                        updateSecurityBadge();

                        // Record to history if NOT incognito
                        if (!isIncognito && !TextUtils.isEmpty(currentUrl)) {
                            String titleToSave = !TextUtils.isEmpty(currentTitle) ? currentTitle : currentUrl;
                            BrowserHistoryManager.add(BrowserActivity.this, titleToSave, currentUrl);
                        }
                    }
                });
            }

            @Override
            public void onSecurityChange(GeckoSession geckoSession, SecurityInformation securityInfo) {
                runOnUiThread(() -> {
                    if (s == session) {
                        tab.lastSecurityStatus = (securityInfo != null && securityInfo.isSecure) ? 1 : 0;
                        updateSecurityBadge();
                    }
                });
            }
        });

        // 8. PermissionDelegate for Site-Specific Location, Camera & Microphone
        s.setPermissionDelegate(new GeckoSession.PermissionDelegate() {
            @Override
            public GeckoResult<Integer> onContentPermissionRequest(
                    GeckoSession geckoSession,
                    GeckoSession.PermissionDelegate.ContentPermission perm) {
                return handleContentPermission(perm);
            }

            @Override
            public void onMediaPermissionRequest(
                    GeckoSession geckoSession,
                    String uri,
                    GeckoSession.PermissionDelegate.MediaSource[] video,
                    GeckoSession.PermissionDelegate.MediaSource[] audio,
                    GeckoSession.PermissionDelegate.MediaCallback callback) {
                handleMediaPermission(uri, video, audio, callback);
            }
        });

        return s;
    }

    private void updateSecurityBadge() {
        if (isFinishing() || activeTabIndex < 0 || activeTabIndex >= tabs.size()) return;
        TabItem activeTab = tabs.get(activeTabIndex);
        if (isIncognito) {
            securityIcon.setText("🕶️");
            securityIcon.setTextColor(ACCENT_BLUE);
        } else if (activeTab.blockedTrackersCount > 0) {
            securityIcon.setText("🛡️ " + activeTab.blockedTrackersCount);
            securityIcon.setTextColor(ACCENT_COLOR);
        } else if (activeTab.lastSecurityStatus == 1) {
            securityIcon.setText("🔒");
            securityIcon.setTextColor(TEXT_COLOR);
        } else {
            securityIcon.setText("🌐");
            securityIcon.setTextColor(MUTED_COLOR);
        }
    }

    private void handleContentCrash(GeckoSession crashingSession) {
        if (isFinishing()) return;
        Toast.makeText(this, "⚠️ Web content process recovered. Reloading page…", Toast.LENGTH_SHORT).show();
        if (crashingSession != null) {
            crashingSession.reload();
        }
    }

    private void showWebContextMenu(GeckoSession.ContentDelegate.ContextElement element) {
        if (element == null || isFinishing()) return;
        List<String> items = new ArrayList<>();
        List<Runnable> actions = new ArrayList<>();

        if (!TextUtils.isEmpty(element.linkUri)) {
            items.add("🔗  Open Link in New Tab");
            actions.add(() -> createNewTab(element.linkUri));

            items.add("📋  Copy Link Address");
            actions.add(() -> {
                ClipboardManager cm = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                if (cm != null) {
                    cm.setPrimaryClip(ClipData.newPlainText("Link", element.linkUri));
                    Toast.makeText(this, "Link copied to clipboard", Toast.LENGTH_SHORT).show();
                }
            });

            items.add("↗️  Share Link (Clean)");
            actions.add(() -> {
                Intent share = new Intent(Intent.ACTION_SEND);
                share.setType("text/plain");
                share.putExtra(Intent.EXTRA_TEXT, stripTrackingParameters(element.linkUri));
                startActivity(Intent.createChooser(share, "Share Link"));
            });
        }

        if (!TextUtils.isEmpty(element.srcUri) && element.type == GeckoSession.ContentDelegate.ContextElement.TYPE_IMAGE) {
            items.add("🖼️  Open Image in New Tab");
            actions.add(() -> createNewTab(element.srcUri));

            items.add("📋  Copy Image URL");
            actions.add(() -> {
                ClipboardManager cm = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                if (cm != null) {
                    cm.setPrimaryClip(ClipData.newPlainText("Image URL", element.srcUri));
                    Toast.makeText(this, "Image URL copied", Toast.LENGTH_SHORT).show();
                }
            });

            items.add("↗️  Share Image URL");
            actions.add(() -> {
                Intent share = new Intent(Intent.ACTION_SEND);
                share.setType("text/plain");
                share.putExtra(Intent.EXTRA_TEXT, element.srcUri);
                startActivity(Intent.createChooser(share, "Share Image"));
            });
        }

        if (items.isEmpty()) return;

        AlertDialog.Builder b = new AlertDialog.Builder(this);
        String headerTitle = !TextUtils.isEmpty(element.title) ? element.title :
                (!TextUtils.isEmpty(element.linkText) ? element.linkText :
                (!TextUtils.isEmpty(element.linkUri) ? element.linkUri : "Web Element"));
        if (headerTitle.length() > 42) headerTitle = headerTitle.substring(0, 39) + "…";
        b.setTitle(headerTitle);
        b.setItems(items.toArray(new String[0]), (d, which) -> {
            if (which >= 0 && which < actions.size()) {
                actions.get(which).run();
            }
        });
        b.show();
    }

    private void createNewTab(String url) {
        String id = "tab_" + System.currentTimeMillis();
        TabItem newTab = new TabItem(id, url, "New Tab", null);
        newTab.session = createGeckoSession(newTab);
        tabs.add(newTab);
        switchToTab(tabs.size() - 1);
        installFiltersAndLoad(url);
        startTimerIfReady();
        updateTabsBadge();
    }

    private void switchToTab(int index) {
        if (index < 0 || index >= tabs.size()) return;
        activeTabIndex = index;
        TabItem tab = tabs.get(index);
        session = tab.session;
        if (geckoView != null) {
            geckoView.setSession(tab.session);
        }
        currentUrl = tab.url != null ? tab.url : "https://www.google.com";
        currentTitle = tab.title != null ? tab.title : "";
        updateDisplayUrl(currentUrl);
        updateSiteMode(currentUrl);
        updateBookmarkIcon();
        updateTabsBadge();
        updateSecurityBadge();
    }

    private void closeTab(int index) {
        if (index < 0 || index >= tabs.size()) return;
        TabItem removed = tabs.remove(index);
        if (removed.session != null) {
            removed.session.close();
        }
        if (tabs.isEmpty()) {
            createNewTab("https://www.google.com");
        } else {
            if (activeTabIndex >= tabs.size()) {
                activeTabIndex = tabs.size() - 1;
            }
            switchToTab(activeTabIndex);
        }
        updateTabsBadge();
    }

    private void updateTabsBadge() {
        if (tabsButton != null) {
            tabsButton.setText(String.valueOf(tabs.size()));
        }
    }

    private void showTabTray() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(18), dp(16), dp(18), dp(16));
        content.setBackgroundColor(SURFACE_COLOR);

        // Header: Tabs count + New Tab
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(0, 0, 0, dp(14));

        TextView title = new TextView(this);
        title.setText("Open Tabs (" + tabs.size() + ")");
        title.setTextSize(17);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setTextColor(TEXT_COLOR);
        header.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

        TextView newTabBtn = new TextView(this);
        newTabBtn.setText("➕ New Tab");
        newTabBtn.setTextSize(12);
        newTabBtn.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        newTabBtn.setTextColor(Color.WHITE);
        newTabBtn.setPadding(dp(12), dp(6), dp(12), dp(6));
        GradientDrawable nBg = new GradientDrawable();
        nBg.setColor(ACCENT_COLOR);
        nBg.setCornerRadius(dp(12));
        newTabBtn.setBackground(nBg);
        newTabBtn.setClickable(true);
        header.addView(newTabBtn);
        content.addView(header);

        // List of Tabs
        LinearLayout tabListContainer = new LinearLayout(this);
        tabListContainer.setOrientation(LinearLayout.VERTICAL);

        AlertDialog dialog = builder.setView(content).create();

        newTabBtn.setOnClickListener(v -> {
            dialog.dismiss();
            createNewTab("https://www.google.com");
            urlInput.requestFocus();
            urlInput.setText("");
            showKeyboard();
        });

        for (int i = 0; i < tabs.size(); i++) {
            final int tabIdx = i;
            final TabItem item = tabs.get(i);
            boolean isActive = (i == activeTabIndex);

            LinearLayout tabCard = new LinearLayout(this);
            tabCard.setOrientation(LinearLayout.HORIZONTAL);
            tabCard.setGravity(Gravity.CENTER_VERTICAL);
            tabCard.setPadding(dp(12), dp(10), dp(12), dp(10));
            GradientDrawable cBg = new GradientDrawable();
            cBg.setColor(isActive ? Color.rgb(28, 42, 65) : Color.rgb(18, 24, 38));
            cBg.setCornerRadius(dp(12));
            cBg.setStroke(dp(1), isActive ? ACCENT_COLOR : Color.rgb(40, 56, 84));
            tabCard.setBackground(cBg);
            tabCard.setClickable(true);

            // Icon
            ImageView icon = new ImageView(this);
            int logoRes = getLogoForUrl(item.url);
            if (logoRes != 0) {
                icon.setImageResource(logoRes);
            } else {
                icon.setImageResource(android.R.drawable.ic_menu_compass);
            }
            tabCard.addView(icon, new LinearLayout.LayoutParams(dp(26), dp(26)));

            // Text Col
            LinearLayout tCol = new LinearLayout(this);
            tCol.setOrientation(LinearLayout.VERTICAL);
            tCol.setPadding(dp(10), 0, dp(10), 0);

            TextView tabTitle = new TextView(this);
            tabTitle.setText(!TextUtils.isEmpty(item.title) ? item.title : "Tab " + (i + 1));
            tabTitle.setTextSize(13);
            tabTitle.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            tabTitle.setTextColor(TEXT_COLOR);
            tabTitle.setSingleLine(true);
            tCol.addView(tabTitle);

            TextView tabHost = new TextView(this);
            tabHost.setText(SitePermissionManager.cleanHost(item.url) + (isActive ? " • Active" : ""));
            tabHost.setTextSize(11);
            tabHost.setTextColor(isActive ? ACCENT_COLOR : MUTED_COLOR);
            tCol.addView(tabHost);

            tabCard.addView(tCol, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

            // Close Tab Button (✕)
            TextView closeBtn = new TextView(this);
            closeBtn.setText("✕");
            closeBtn.setTextSize(14);
            closeBtn.setTextColor(MUTED_COLOR);
            closeBtn.setPadding(dp(8), dp(6), dp(8), dp(6));
            closeBtn.setClickable(true);
            closeBtn.setOnClickListener(v -> {
                dialog.dismiss();
                closeTab(tabIdx);
                if (!tabs.isEmpty()) {
                    showTabTray();
                }
            });
            tabCard.addView(closeBtn);

            tabCard.setOnClickListener(v -> {
                dialog.dismiss();
                switchToTab(tabIdx);
            });

            LinearLayout.LayoutParams tcParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            tcParams.bottomMargin = dp(8);
            tabListContainer.addView(tabCard, tcParams);
        }

        ScrollView scroll = new ScrollView(this);
        scroll.addView(tabListContainer);
        content.addView(scroll);

        dialog.show();
    }

    /**
     * Handles Fullscreen Video mode for YouTube and other video players.
     */
    private void handleFullScreen(boolean fullScreen) {
        isFullScreen = fullScreen;
        if (fullScreen) {
            // Hide toolbars to give full screen to video
            appBarLayout.setVisibility(View.GONE);
            bottomBar.setVisibility(View.GONE);
            pageProgressBar.setVisibility(View.GONE);

            // Immersive Sticky Mode
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            );

            // YouTube videos look best in landscape full screen
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
        } else {
            // Restore regular browser layout
            appBarLayout.setVisibility(View.VISIBLE);
            bottomBar.setVisibility(View.VISIBLE);

            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
        }
    }

    private void installFiltersAndLoad(String url) {
        GeckoRuntime runtime = getSharedRuntime(this);
        runtime.getWebExtensionController()
                .ensureBuiltIn(
                        "resource://android/assets/extensions/calmfeed/",
                        "calmfeed-filters@calmfeed.app")
                .accept(
                        extension -> runOnUiThread(() -> {
                            if (session != null && !isFinishing()) {
                                session.loadUri(url);
                            }
                        }),
                        error -> runOnUiThread(() -> {
                            if (isFinishing()) return;
                            if (session != null) {
                                session.loadUri(url);
                            }
                        }));
    }

    private void navigateToQueryOrUrl(String input) {
        if (TextUtils.isEmpty(input)) return;

        String destinationUrl;
        String trimmed = input.trim();

        if (trimmed.startsWith("http://") || trimmed.startsWith("https://") || trimmed.startsWith("about:")) {
            destinationUrl = trimmed;
        } else if (!trimmed.contains(" ") && trimmed.contains(".") && !trimmed.endsWith(".")) {
            destinationUrl = "https://" + trimmed;
        } else {
            // Google Search
            try {
                destinationUrl = "https://www.google.com/search?q=" + URLEncoder.encode(trimmed, "UTF-8");
            } catch (Exception e) {
                destinationUrl = "https://www.google.com/search?q=" + trimmed;
            }
        }

        if (session != null) {
            session.loadUri(destinationUrl);
        }
    }

    private void updateDisplayUrl(String url) {
        if (urlInput.hasFocus()) return;
        if (TextUtils.isEmpty(url)) {
            urlInput.setText("");
            return;
        }

        try {
            Uri parsed = Uri.parse(url);
            String host = parsed.getHost();
            if (host != null) {
                String path = parsed.getPath();
                if (path != null && path.length() > 1) {
                    urlInput.setText(host + (path.length() > 18 ? path.substring(0, 18) + "…" : path));
                } else {
                    urlInput.setText(host);
                }
            } else {
                urlInput.setText(url);
            }
        } catch (Exception e) {
            urlInput.setText(url);
        }
    }

    private void updateBookmarkIcon() {
        boolean bookmarked = BookmarkManager.isBookmarked(this, currentUrl);
        bookmarkButton.setText(bookmarked ? "★" : "☆");
        bookmarkButton.setTextColor(bookmarked ? Color.rgb(250, 204, 21) : TEXT_COLOR);
    }

    private void toggleBookmarkCurrentPage() {
        if (TextUtils.isEmpty(currentUrl)) return;
        String titleToSave = !TextUtils.isEmpty(currentTitle) ? currentTitle : currentUrl;
        boolean nowBookmarked = BookmarkManager.toggleBookmark(this, titleToSave, currentUrl);
        updateBookmarkIcon();
        Toast.makeText(this, nowBookmarked ? "Bookmark added" : "Bookmark removed", Toast.LENGTH_SHORT).show();
    }

    private void showOverflowMenu(View anchor) {
        PopupMenu popup = new PopupMenu(this, anchor);
        popup.getMenu().add(0, 1, 0, "➕  New Tab");
        popup.getMenu().add(0, 13, 1, "🔍  Find in Page");
        popup.getMenu().add(0, 12, 1, "🗂️  Tab Switcher (" + tabs.size() + ")");
        popup.getMenu().add(0, 2, 2, isIncognito ? "🌐  Exit Incognito Mode" : "🕶️  New Incognito Tab");
        if (!UsageTracker.isSiteLimited(this, currentUrl)) {
            popup.getMenu().add(0, 10, 2, "📖  Clean Reader View");
        }
        boolean currentSiteLimited = UsageTracker.isSiteLimited(this, currentUrl);
        popup.getMenu().add(0, 11, 3, currentSiteLimited ? "⏱️  Make Site Unlimited" : "⏱️  Add to Focus Daily Limit");
        popup.getMenu().add(0, 3, 4, BookmarkManager.isBookmarked(this, currentUrl) ? "★  Remove Bookmark" : "☆  Add Bookmark");
        popup.getMenu().add(0, 4, 4, "🕒  Browsing History");
        popup.getMenu().add(0, 5, 5, "🔖  Bookmarks");
        popup.getMenu().add(0, 6, 6, (isDesktopMode ? "☑️ " : "☐ ") + "Desktop Site");
        popup.getMenu().add(0, 7, 7, "⚙️  Site Permissions & Settings");
        popup.getMenu().add(0, 8, 8, "↗️  Share Webpage");
        popup.getMenu().add(0, 9, 9, "🗑️  Clear Browsing History");

        popup.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case 10:
                    toggleReaderView();
                    return true;
                case 11:
                    String cHost = SitePermissionManager.cleanHost(currentUrl);
                    boolean nowLim = UsageTracker.toggleSiteLimit(this, cHost);
                    Toast.makeText(this, cHost + (nowLim ? " added to Focus Daily Limit." : " set to Unlimited Browsing."), Toast.LENGTH_SHORT).show();
                    updateSiteMode(currentUrl);
                    return true;
                case 1:
                    createNewTab("https://www.google.com");
                    urlInput.requestFocus();
                    urlInput.setText("");
                    showKeyboard();
                    return true;
                case 12:
                    showTabTray();
                    return true;
                case 13:
                    showFindInPage();
                    return true;
                case 2:
                    Intent incognitoIntent = new Intent(this, BrowserActivity.class);
                    incognitoIntent.putExtra(EXTRA_INCOGNITO, !isIncognito);
                    incognitoIntent.putExtra(EXTRA_URL, "https://www.google.com");
                    startActivity(incognitoIntent);
                    return true;
                case 3:
                    toggleBookmarkCurrentPage();
                    return true;
                case 4:
                    showHistoryDialog();
                    return true;
                case 5:
                    showBookmarksDialog();
                    return true;
                case 6:
                    toggleDesktopMode();
                    return true;
                case 7:
                    startActivity(new Intent(this, SiteSettingsActivity.class));
                    return true;
                case 8:
                    shareCurrentPage();
                    return true;
                case 9:
                    confirmClearHistory();
                    return true;
            }
            return false;
        });
        popup.show();
    }

    private GeckoResult<Integer> handleContentPermission(GeckoSession.PermissionDelegate.ContentPermission perm) {
        String host = SitePermissionManager.cleanHost(perm.uri != null ? perm.uri : currentUrl);
        String permType;
        if (perm.permission == GeckoSession.PermissionDelegate.PERMISSION_GEOLOCATION) {
            permType = SitePermissionManager.PERM_LOCATION;
        } else if (perm.permission == GeckoSession.PermissionDelegate.PERMISSION_DESKTOP_NOTIFICATION) {
            permType = SitePermissionManager.PERM_NOTIFICATIONS;
        } else {
            return GeckoResult.fromValue(GeckoSession.PermissionDelegate.ContentPermission.VALUE_ALLOW);
        }

        int state = SitePermissionManager.getSitePermission(this, host, permType);
        if (state == SitePermissionManager.STATE_BLOCK) {
            return GeckoResult.fromValue(GeckoSession.PermissionDelegate.ContentPermission.VALUE_DENY);
        }

        if (state == SitePermissionManager.STATE_ALLOW) {
            if (perm.permission == GeckoSession.PermissionDelegate.PERMISSION_GEOLOCATION) {
                if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 101);
                }
            }
            return GeckoResult.fromValue(GeckoSession.PermissionDelegate.ContentPermission.VALUE_ALLOW);
        }

        // STATE_ASK: Prompt the user
        GeckoResult<Integer> result = new GeckoResult<>();
        runOnUiThread(() -> {
            String name = perm.permission == GeckoSession.PermissionDelegate.PERMISSION_GEOLOCATION ? "device location" : "send notifications";
            new AlertDialog.Builder(BrowserActivity.this)
                    .setTitle("Permission Request")
                    .setMessage(host + " wants to access your " + name + ".")
                    .setPositiveButton("Allow Always", (d, w) -> {
                        SitePermissionManager.setSitePermission(BrowserActivity.this, host, permType, SitePermissionManager.STATE_ALLOW);
                        if (perm.permission == GeckoSession.PermissionDelegate.PERMISSION_GEOLOCATION &&
                                checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 101);
                        }
                        result.complete(GeckoSession.PermissionDelegate.ContentPermission.VALUE_ALLOW);
                    })
                    .setNeutralButton("Allow Once", (d, w) -> {
                        result.complete(GeckoSession.PermissionDelegate.ContentPermission.VALUE_ALLOW);
                    })
                    .setNegativeButton("Block", (d, w) -> {
                        SitePermissionManager.setSitePermission(BrowserActivity.this, host, permType, SitePermissionManager.STATE_BLOCK);
                        result.complete(GeckoSession.PermissionDelegate.ContentPermission.VALUE_DENY);
                    })
                    .setCancelable(false)
                    .show();
        });
        return result;
    }

    private void handleMediaPermission(String uri,
                                       GeckoSession.PermissionDelegate.MediaSource[] video,
                                       GeckoSession.PermissionDelegate.MediaSource[] audio,
                                       GeckoSession.PermissionDelegate.MediaCallback callback) {
        String host = SitePermissionManager.cleanHost(uri != null ? uri : currentUrl);
        boolean requestsVideo = video != null && video.length > 0;
        boolean requestsAudio = audio != null && audio.length > 0;

        int camState = requestsVideo ? SitePermissionManager.getSitePermission(this, host, SitePermissionManager.PERM_CAMERA) : SitePermissionManager.STATE_ALLOW;
        int micState = requestsAudio ? SitePermissionManager.getSitePermission(this, host, SitePermissionManager.PERM_MICROPHONE) : SitePermissionManager.STATE_ALLOW;

        if (camState == SitePermissionManager.STATE_BLOCK || micState == SitePermissionManager.STATE_BLOCK) {
            callback.reject();
            return;
        }

        if (camState == SitePermissionManager.STATE_ALLOW && micState == SitePermissionManager.STATE_ALLOW) {
            grantMediaIfSystemPermitted(video, audio, callback);
            return;
        }

        // STATE_ASK: Prompt the user
        runOnUiThread(() -> {
            String what = requestsVideo && requestsAudio ? "camera and microphone" : (requestsVideo ? "camera" : "microphone");
            new AlertDialog.Builder(BrowserActivity.this)
                    .setTitle("Media Permission")
                    .setMessage(host + " wants to use your " + what + ".")
                    .setPositiveButton("Allow Always", (d, w) -> {
                        if (requestsVideo) SitePermissionManager.setSitePermission(BrowserActivity.this, host, SitePermissionManager.PERM_CAMERA, SitePermissionManager.STATE_ALLOW);
                        if (requestsAudio) SitePermissionManager.setSitePermission(BrowserActivity.this, host, SitePermissionManager.PERM_MICROPHONE, SitePermissionManager.STATE_ALLOW);
                        grantMediaIfSystemPermitted(video, audio, callback);
                    })
                    .setNeutralButton("Allow Once", (d, w) -> {
                        grantMediaIfSystemPermitted(video, audio, callback);
                    })
                    .setNegativeButton("Block", (d, w) -> {
                        if (requestsVideo) SitePermissionManager.setSitePermission(BrowserActivity.this, host, SitePermissionManager.PERM_CAMERA, SitePermissionManager.STATE_BLOCK);
                        if (requestsAudio) SitePermissionManager.setSitePermission(BrowserActivity.this, host, SitePermissionManager.PERM_MICROPHONE, SitePermissionManager.STATE_BLOCK);
                        callback.reject();
                    })
                    .setCancelable(false)
                    .show();
        });
    }

    private void grantMediaIfSystemPermitted(GeckoSession.PermissionDelegate.MediaSource[] video,
                                             GeckoSession.PermissionDelegate.MediaSource[] audio,
                                             GeckoSession.PermissionDelegate.MediaCallback callback) {
        boolean hasCamera = checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
        boolean hasAudio = checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;

        if ((video != null && video.length > 0 && !hasCamera) || (audio != null && audio.length > 0 && !hasAudio)) {
            requestPermissions(new String[]{Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO}, 102);
        }
        callback.grant(video != null && video.length > 0 ? video[0] : null,
                       audio != null && audio.length > 0 ? audio[0] : null);
    }

    private void showQuickSitePermissionsDialog() {
        String host = SitePermissionManager.cleanHost(currentUrl);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setBackgroundColor(SURFACE_COLOR);
        content.setPadding(dp(18), dp(18), dp(18), dp(18));

        TextView title = new TextView(this);
        title.setText("Security & Permissions: " + host);
        title.setTextSize(17);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setTextColor(TEXT_COLOR);
        content.addView(title);

        TextView secBadge = new TextView(this);
        secBadge.setText("🔒 Connection is secure (HTTPS)");
        secBadge.setTextSize(12);
        secBadge.setTextColor(ACCENT_COLOR);
        secBadge.setPadding(0, dp(4), 0, dp(10));
        content.addView(secBadge);

        TabItem currentTab = (activeTabIndex >= 0 && activeTabIndex < tabs.size()) ? tabs.get(activeTabIndex) : null;
        int blockedCount = (currentTab != null) ? currentTab.blockedTrackersCount : 0;

        LinearLayout etpCard = new LinearLayout(this);
        etpCard.setOrientation(LinearLayout.VERTICAL);
        etpCard.setPadding(dp(12), dp(10), dp(12), dp(10));
        GradientDrawable etpBg = new GradientDrawable();
        etpBg.setColor(Color.rgb(15, 23, 42));
        etpBg.setCornerRadius(dp(10));
        etpBg.setStroke(dp(1), Color.rgb(16, 185, 129));
        etpCard.setBackground(etpBg);

        TextView etpTitle = new TextView(this);
        etpTitle.setText("🛡️ Enhanced Tracking Protection (Strict)");
        etpTitle.setTextSize(13);
        etpTitle.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        etpTitle.setTextColor(ACCENT_COLOR);
        etpCard.addView(etpTitle);

        TextView etpStats = new TextView(this);
        etpStats.setText(blockedCount > 0 ?
                ("GeckoView blocked " + blockedCount + " tracking scripts, cookies & beacons on this page.") :
                "Strict protection active. Cross-site tracking cookies & fingerprinters blocked.");
        etpStats.setTextSize(11);
        etpStats.setTextColor(Color.rgb(203, 213, 225));
        etpStats.setPadding(0, dp(4), 0, 0);
        etpCard.addView(etpStats);

        LinearLayout.LayoutParams etpParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        etpParams.bottomMargin = dp(14);
        content.addView(etpCard, etpParams);

        content.addView(createQuickPermRow(host, "📍 Location Access", SitePermissionManager.PERM_LOCATION));
        content.addView(createQuickPermRow(host, "📷 Camera Access", SitePermissionManager.PERM_CAMERA));
        content.addView(createQuickPermRow(host, "🎙️ Microphone Access", SitePermissionManager.PERM_MICROPHONE));
        content.addView(createQuickPermRow(host, "🔔 Notifications", SitePermissionManager.PERM_NOTIFICATIONS));

        // Focus & Daily Limit Toggle for this site
        boolean isLimited = UsageTracker.isSiteLimited(this, host);
        LinearLayout limitRow = new LinearLayout(this);
        limitRow.setOrientation(LinearLayout.HORIZONTAL);
        limitRow.setGravity(Gravity.CENTER_VERTICAL);
        limitRow.setPadding(0, dp(10), 0, dp(6));

        TextView limitLabel = new TextView(this);
        limitLabel.setText("⏱️ Daily Focus Limit");
        limitLabel.setTextSize(13);
        limitLabel.setTextColor(TEXT_COLOR);
        limitRow.addView(limitLabel, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

        TextView limitToggleBtn = new TextView(this);
        limitToggleBtn.setText(isLimited ? "Limited (Timed)" : "Unlimited");
        limitToggleBtn.setTextSize(11);
        limitToggleBtn.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        limitToggleBtn.setTextColor(Color.WHITE);
        limitToggleBtn.setPadding(dp(10), dp(6), dp(10), dp(6));
        GradientDrawable ltBg = new GradientDrawable();
        ltBg.setColor(isLimited ? Color.rgb(251, 146, 60) : Color.rgb(40, 56, 84));
        ltBg.setCornerRadius(dp(8));
        limitToggleBtn.setBackground(ltBg);
        limitToggleBtn.setClickable(true);
        limitToggleBtn.setOnClickListener(v -> {
            boolean nowLimited = UsageTracker.toggleSiteLimit(BrowserActivity.this, host);
            ltBg.setColor(nowLimited ? Color.rgb(251, 146, 60) : Color.rgb(40, 56, 84));
            limitToggleBtn.setText(nowLimited ? "Limited (Timed)" : "Unlimited");
            Toast.makeText(BrowserActivity.this, host + (nowLimited ? " added to Focus Daily Limit." : " set to Unlimited Browsing."), Toast.LENGTH_SHORT).show();
            updateSiteMode(currentUrl);
        });
        limitRow.addView(limitToggleBtn);
        content.addView(limitRow);

        Button allSettingsBtn = new Button(this);
        allSettingsBtn.setText("Manage All Sites Settings →");
        allSettingsBtn.setAllCaps(false);
        allSettingsBtn.setTextSize(13);
        allSettingsBtn.setTextColor(TEXT_COLOR);
        GradientDrawable btnBg = new GradientDrawable();
        btnBg.setColor(Color.rgb(40, 56, 84));
        btnBg.setCornerRadius(dp(12));
        allSettingsBtn.setBackground(btnBg);

        LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(44));
        btnParams.topMargin = dp(14);
        content.addView(allSettingsBtn, btnParams);

        AlertDialog dialog = builder.setView(content)
                .setPositiveButton("Reload Page", (d, w) -> {
                    if (session != null) session.reload();
                })
                .setNegativeButton("Done", null)
                .create();

        allSettingsBtn.setOnClickListener(v -> {
            dialog.dismiss();
            startActivity(new Intent(BrowserActivity.this, SiteSettingsActivity.class));
        });

        dialog.show();
    }

    private View createQuickPermRow(String host, String label, String permType) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(6), 0, dp(6));

        TextView tv = new TextView(this);
        tv.setText(label);
        tv.setTextSize(13);
        tv.setTextColor(TEXT_COLOR);
        row.addView(tv, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

        int currentState = SitePermissionManager.getSitePermission(this, host, permType);

        LinearLayout selector = new LinearLayout(this);
        selector.setOrientation(LinearLayout.HORIZONTAL);

        selector.addView(createQuickSegmentBtn("Ask", currentState == SitePermissionManager.STATE_ASK, v -> {
            SitePermissionManager.setSitePermission(this, host, permType, SitePermissionManager.STATE_ASK);
            Toast.makeText(this, host + ": " + label + " → Ask", Toast.LENGTH_SHORT).show();
        }));
        selector.addView(createQuickSegmentBtn("Allow", currentState == SitePermissionManager.STATE_ALLOW, v -> {
            SitePermissionManager.setSitePermission(this, host, permType, SitePermissionManager.STATE_ALLOW);
            Toast.makeText(this, host + ": " + label + " → Allow", Toast.LENGTH_SHORT).show();
        }));
        selector.addView(createQuickSegmentBtn("Block", currentState == SitePermissionManager.STATE_BLOCK, v -> {
            SitePermissionManager.setSitePermission(this, host, permType, SitePermissionManager.STATE_BLOCK);
            Toast.makeText(this, host + ": " + label + " → Block", Toast.LENGTH_SHORT).show();
        }));

        row.addView(selector);
        return row;
    }

    private View createQuickSegmentBtn(String text, boolean isSelected, View.OnClickListener onClick) {
        TextView btn = new TextView(this);
        btn.setText(text);
        btn.setTextSize(11);
        btn.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        btn.setTextColor(isSelected ? Color.WHITE : MUTED_COLOR);
        btn.setPadding(dp(8), dp(4), dp(8), dp(4));
        GradientDrawable bg = new GradientDrawable();
        if (isSelected) {
            bg.setColor("Block".equals(text) ? Color.rgb(239, 68, 68) : ("Allow".equals(text) ? ACCENT_COLOR : ACCENT_BLUE));
        } else {
            bg.setColor(Color.rgb(24, 34, 53));
        }
        bg.setCornerRadius(dp(8));
        btn.setBackground(bg);
        btn.setClickable(true);
        btn.setOnClickListener(onClick);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.leftMargin = dp(4);
        btn.setLayoutParams(params);
        return btn;
    }

    private void toggleDesktopMode() {
        isDesktopMode = !isDesktopMode;
        Toast.makeText(this, isDesktopMode ? "Desktop view requested" : "Mobile view requested", Toast.LENGTH_SHORT).show();
        if (session != null) {
            session.reload();
        }
    }

    private void shareCurrentPage() {
        if (TextUtils.isEmpty(currentUrl)) return;
        String cleanUrl = stripTrackingParameters(currentUrl);
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, currentTitle);
        shareIntent.putExtra(Intent.EXTRA_TEXT, cleanUrl);
        startActivity(Intent.createChooser(shareIntent, "Share webpage (clean link)"));
    }

    private static String stripTrackingParameters(String url) {
        try {
            Uri uri = Uri.parse(url);
            if (uri.getQuery() == null) return url;
            Uri.Builder builder = uri.buildUpon().clearQuery();
            for (String param : uri.getQueryParameterNames()) {
                String lower = param.toLowerCase();
                if (lower.startsWith("utm_") || lower.equals("fbclid") || lower.equals("gclid")
                        || lower.equals("si") || lower.equals("igshid") || lower.equals("mc_cid")
                        || lower.equals("mc_eid") || lower.equals("ref") || lower.equals("ref_src")) {
                    continue; // Skip tracking param
                }
                for (String val : uri.getQueryParameters(param)) {
                    builder.appendQueryParameter(param, val);
                }
            }
            return builder.build().toString();
        } catch (Exception e) {
            return url;
        }
    }

    private void showHistoryDialog() {
        List<BrowserHistoryManager.HistoryItem> history = BrowserHistoryManager.getAll(this);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setBackgroundColor(SURFACE_COLOR);
        content.setPadding(dp(16), dp(16), dp(16), dp(16));

        TextView title = new TextView(this);
        title.setText("Browsing History (" + history.size() + ")");
        title.setTextSize(18);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setTextColor(TEXT_COLOR);
        content.addView(title);

        if (history.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("No browsing history recorded yet.");
            empty.setTextColor(MUTED_COLOR);
            empty.setPadding(0, dp(24), 0, dp(24));
            content.addView(empty);
        } else {
            ListView listView = new ListView(this);
            listView.setDividerHeight(dp(1));
            listView.setAdapter(new BaseAdapter() {
                @Override
                public int getCount() { return history.size(); }
                @Override
                public Object getItem(int pos) { return history.get(pos); }
                @Override
                public long getItemId(int pos) { return history.get(pos).id; }

                @Override
                public View getView(int pos, View convertView, ViewGroup parent) {
                    LinearLayout row = new LinearLayout(BrowserActivity.this);
                    row.setOrientation(LinearLayout.HORIZONTAL);
                    row.setGravity(Gravity.CENTER_VERTICAL);
                    row.setPadding(dp(8), dp(8), dp(8), dp(8));

                    BrowserHistoryManager.HistoryItem item = history.get(pos);
                    int logoRes = getLogoForUrl(item.url);

                    if (logoRes != 0) {
                        ImageView logoView = new ImageView(BrowserActivity.this);
                        logoView.setImageResource(logoRes);
                        logoView.setScaleType(ImageView.ScaleType.FIT_CENTER);
                        row.addView(logoView, new LinearLayout.LayoutParams(dp(28), dp(28)));
                    } else {
                        TextView icon = new TextView(BrowserActivity.this);
                        icon.setText("🌐");
                        icon.setTextSize(16);
                        icon.setGravity(Gravity.CENTER);
                        row.addView(icon, new LinearLayout.LayoutParams(dp(28), dp(28)));
                    }

                    LinearLayout textCol = new LinearLayout(BrowserActivity.this);
                    textCol.setOrientation(LinearLayout.VERTICAL);
                    textCol.setPadding(dp(10), 0, 0, 0);

                    TextView itemTitle = new TextView(BrowserActivity.this);
                    itemTitle.setText(item.title);
                    itemTitle.setTextColor(TEXT_COLOR);
                    itemTitle.setTextSize(13);
                    itemTitle.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
                    textCol.addView(itemTitle);

                    TextView itemUrl = new TextView(BrowserActivity.this);
                    itemUrl.setText(item.url + " · " + item.formattedTime);
                    itemUrl.setTextColor(MUTED_COLOR);
                    itemUrl.setTextSize(11);
                    textCol.addView(itemUrl);

                    row.addView(textCol, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));
                    return row;
                }
            });

            AlertDialog dialog = builder.setView(content)
                    .setNegativeButton("Close", null)
                    .setNeutralButton("Clear All", (d, w) -> confirmClearHistory())
                    .create();

            listView.setOnItemClickListener((parent, view, position, id) -> {
                dialog.dismiss();
                BrowserHistoryManager.HistoryItem item = history.get(position);
                if (session != null) {
                    session.loadUri(item.url);
                }
            });

            content.addView(listView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(350)));
            dialog.show();
            return;
        }

        builder.setView(content)
                .setNegativeButton("Close", null)
                .show();
    }

    private void showBookmarksDialog() {
        List<BookmarkManager.BookmarkItem> bookmarks = BookmarkManager.getAll(this);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setBackgroundColor(SURFACE_COLOR);
        content.setPadding(dp(16), dp(16), dp(16), dp(16));

        TextView title = new TextView(this);
        title.setText("Bookmarks (" + bookmarks.size() + ")");
        title.setTextSize(18);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setTextColor(TEXT_COLOR);
        content.addView(title);

        ListView listView = new ListView(this);
        listView.setDividerHeight(dp(1));
        listView.setAdapter(new BaseAdapter() {
            @Override
            public int getCount() { return bookmarks.size(); }
            @Override
            public Object getItem(int pos) { return bookmarks.get(pos); }
            @Override
            public long getItemId(int pos) { return pos; }

            @Override
            public View getView(int pos, View convertView, ViewGroup parent) {
                LinearLayout row = new LinearLayout(BrowserActivity.this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setGravity(Gravity.CENTER_VERTICAL);
                row.setPadding(dp(8), dp(8), dp(8), dp(8));

                BookmarkManager.BookmarkItem item = bookmarks.get(pos);
                int logoRes = getLogoForUrl(item.url);

                if (logoRes != 0) {
                    ImageView logoView = new ImageView(BrowserActivity.this);
                    logoView.setImageResource(logoRes);
                    logoView.setScaleType(ImageView.ScaleType.FIT_CENTER);
                    row.addView(logoView, new LinearLayout.LayoutParams(dp(28), dp(28)));
                } else {
                    TextView icon = new TextView(BrowserActivity.this);
                    icon.setText("⭐");
                    icon.setTextSize(16);
                    icon.setGravity(Gravity.CENTER);
                    row.addView(icon, new LinearLayout.LayoutParams(dp(28), dp(28)));
                }

                LinearLayout textCol = new LinearLayout(BrowserActivity.this);
                textCol.setOrientation(LinearLayout.VERTICAL);
                textCol.setPadding(dp(10), 0, 0, 0);

                TextView itemTitle = new TextView(BrowserActivity.this);
                itemTitle.setText(item.title);
                itemTitle.setTextColor(TEXT_COLOR);
                itemTitle.setTextSize(13);
                itemTitle.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
                textCol.addView(itemTitle);

                TextView itemUrl = new TextView(BrowserActivity.this);
                itemUrl.setText(item.url);
                itemUrl.setTextColor(MUTED_COLOR);
                itemUrl.setTextSize(11);
                textCol.addView(itemUrl);

                row.addView(textCol, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));
                return row;
            }
        });

        AlertDialog dialog = builder.setView(content)
                .setPositiveButton("Bookmark Current", (d, w) -> toggleBookmarkCurrentPage())
                .setNegativeButton("Close", null)
                .create();

        listView.setOnItemClickListener((parent, view, position, id) -> {
            dialog.dismiss();
            BookmarkManager.BookmarkItem item = bookmarks.get(position);
            if (session != null) {
                session.loadUri(item.url);
            }
        });

        content.addView(listView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(350)));
        dialog.show();
    }

    private void confirmClearHistory() {
        new AlertDialog.Builder(this)
                .setTitle("Clear Browsing History")
                .setMessage("Are you sure you want to clear all browsing history from this device?")
                .setPositiveButton("Clear", (dialog, which) -> {
                    BrowserHistoryManager.clear(BrowserActivity.this);
                    Toast.makeText(BrowserActivity.this, "Browsing history cleared", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null && urlInput != null) {
            imm.hideSoftInputFromWindow(urlInput.getWindowToken(), 0);
        }
    }

    private void showKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null && urlInput != null) {
            imm.showSoftInput(urlInput, InputMethodManager.SHOW_IMPLICIT);
        }
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void recordUsage(boolean force) {
        recordBrowsingUsage();
        if (activeSegmentStartElapsed < 0 || sessionDurationMillis <= 0) return;
        long activeMillis = SystemClock.elapsedRealtime() - activeSegmentStartElapsed;
        long elapsed = Math.min(completedSessionMillis + activeMillis, sessionDurationMillis);
        long elapsedSeconds = TimeUnit.MILLISECONDS.toSeconds(elapsed);
        long unrecordedSeconds = elapsedSeconds - recordedSeconds;
        if (unrecordedSeconds > 0 && (force || unrecordedSeconds >= 30)) {
            UsageTracker.recordSiteSeconds(this, currentUrl, unrecordedSeconds, true, "extra".equals(sessionSource));
            recordedSeconds = elapsedSeconds;
        }
    }

    private void recordBrowsingUsage() {
        if (activeBrowsingStartElapsed < 0 || isIncognito) return;
        long now = SystemClock.elapsedRealtime();
        long elapsedSec = TimeUnit.MILLISECONDS.toSeconds(now - activeBrowsingStartElapsed);
        if (elapsedSec > 0) {
            UsageTracker.recordSiteSeconds(this, currentUrl, elapsedSec, false, false);
            activeBrowsingStartElapsed = now;
        }
    }

    private void finishSession(boolean timedOut) {
        handler.removeCallbacks(timerTick);
        recordUsage(true);
        endRemoteSession();
        if (timedOut) {
            showSocialLimitExhaustedDialog();
        } else if (!isFinishing()) {
            finish();
        }
    }

    private void endRemoteSession() {
        if (sessionId == null) return;
        String endingSessionId = sessionId;
        sessionId = null;
        Map<String, Object> request = new HashMap<>();
        request.put("sessionId", endingSessionId);
        FirebaseBridge.call(this, "endSession", request, (result, error) -> {});
    }

    @Override
    public void onBackPressed() {
        if (findInPageLayout != null && findInPageLayout.getVisibility() == View.VISIBLE) {
            hideFindInPage();
            return;
        }
        if (isFullScreen) {
            if (session != null) {
                session.exitFullScreen();
            }
            return;
        }
        if (urlInput != null && urlInput.hasFocus()) {
            urlInput.clearFocus();
            hideKeyboard();
            updateDisplayUrl(currentUrl);
            return;
        }
        if (session != null && canGoBack) {
            session.goBack();
            return;
        }
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        browserVisible = true;
        if (UsageTracker.isSiteLimited(this, currentUrl)) {
            startTimerIfReady();
        } else if (!isIncognito) {
            activeBrowsingStartElapsed = SystemClock.elapsedRealtime();
        }
    }

    private void startTimerIfReady() {
        if (!browserVisible || session == null || remainingSessionMillis <= 0 || activeSegmentStartElapsed >= 0) return;
        if (!UsageTracker.isSiteLimited(this, currentUrl)) return; // Only run timer on limited focus sites!
        activeSegmentStartElapsed = SystemClock.elapsedRealtime();
        endTime = activeSegmentStartElapsed + remainingSessionMillis;
        handler.post(timerTick);
    }

    @Override
    protected void onPause() {
        browserVisible = false;
        handler.removeCallbacks(timerTick);
        recordUsage(true);
        activeBrowsingStartElapsed = -1;
        if (activeSegmentStartElapsed >= 0) {
            long now = SystemClock.elapsedRealtime();
            completedSessionMillis = Math.min(sessionDurationMillis, completedSessionMillis + now - activeSegmentStartElapsed);
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
        for (TabItem t : tabs) {
            if (t.session != null) {
                t.session.close();
            }
        }
        tabs.clear();
        session = null;
        super.onDestroy();
    }

    private int getLogoForUrl(String url) {
        if (url == null) return 0;
        String u = url.toLowerCase();
        if (u.contains("youtube")) return R.drawable.ic_logo_youtube;
        if (u.contains("google")) return R.drawable.ic_logo_google;
        if (u.contains("facebook") || u.contains("fb.com")) return R.drawable.ic_logo_facebook;
        if (u.contains("tiktok")) return R.drawable.ic_logo_tiktok;
        if (u.contains("reddit")) return R.drawable.ic_logo_reddit;
        if (u.contains("instagram")) return R.drawable.ic_logo_instagram;
        if (u.contains("wikipedia")) return R.drawable.ic_logo_wikipedia;
        if (u.contains("x.com") || u.contains("twitter")) return R.drawable.ic_logo_x;
        if (u.contains("bbc")) return R.drawable.ic_logo_bbc;
        return 0;
    }
}
