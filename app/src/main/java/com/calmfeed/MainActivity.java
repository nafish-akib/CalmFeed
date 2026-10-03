package com.calmfeed;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
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
import android.widget.GridLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.net.URLEncoder;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class MainActivity extends Activity {
    // ==========================================
    // MATERIAL DESIGN 3 COLOR PALETTE (Tokens)
    // ==========================================
    private static final int M3_SURFACE = Color.rgb(15, 23, 42);                  // Base Slate 900
    private static final int M3_SURFACE_CONTAINER = Color.rgb(30, 41, 59);        // Card Surface (Slate 800)
    private static final int M3_SURFACE_CONTAINER_HIGH = Color.rgb(51, 65, 85);   // Interactive Container (Slate 700)
    private static final int M3_SURFACE_CONTAINER_LOW = Color.rgb(20, 29, 47);    // Recessed Container
    private static final int M3_PRIMARY = Color.rgb(56, 189, 248);                // Sky 400
    private static final int M3_PRIMARY_CONTAINER = Color.rgb(12, 74, 110);       // Sky 900
    private static final int M3_ON_PRIMARY_CONTAINER = Color.rgb(224, 242, 254);  // Sky 100
    private static final int M3_SECONDARY = Color.rgb(52, 211, 153);              // Emerald 400
    private static final int M3_SECONDARY_CONTAINER = Color.rgb(6, 78, 59);       // Emerald 900
    private static final int M3_TERTIARY = Color.rgb(168, 85, 247);               // Purple 400
    private static final int M3_OUTLINE = Color.rgb(71, 85, 105);                 // Slate 600
    private static final int M3_OUTLINE_VARIANT = Color.rgb(51, 65, 85);          // Slate 700
    private static final int M3_ON_SURFACE = Color.rgb(248, 250, 252);            // Slate 50
    private static final int M3_ON_SURFACE_VARIANT = Color.rgb(148, 163, 184);    // Slate 400
    private static final int M3_WARNING = Color.rgb(251, 146, 60);                // Orange 400

    private static final String[] DAILY_QUOTES = {
            "Small choices make room for real moments offline.",
            "Your attention is yours to spend intentionally.",
            "You do not have to consume everything. Choose what matters.",
            "Every pause is an opportunity to choose your next step.",
            "Time away from algorithms is time back in your life.",
            "Be present in the moment you are currently living.",
            "Great days are made one intentional choice at a time."
    };

    // Navigation & Feed Containers
    private int currentTab = 0;
    private FrameLayout contentArea;
    private View tabBrowseFeed;
    private View tabFocusFeed;
    private View tabShieldFeed;
    private View tabCalmFeed;
    private final LinearLayout[] navTabItems = new LinearLayout[4];
    private final FrameLayout[] navPillContainers = new FrameLayout[4];
    private final TextView[] navIconViews = new TextView[4];
    private final TextView[] navLabelViews = new TextView[4];

    // Shared State Elements
    private EditText searchOmnibox;
    private TextView focusGlanceStatus;
    private TextView usageValue;
    private TextView usageSubtitle;
    private ProgressBar usageProgressBar;
    private LinearLayout usageBreakdownContainer;
    private TextView allowanceLabel;
    private SeekBar allowanceSeekBar;
    private Button setAllowanceButton;
    private Button requestApprovalButton;
    private Map<String, Object> latestPolicy = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(M3_SURFACE);
        getWindow().setNavigationBarColor(M3_SURFACE_CONTAINER_LOW);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(M3_SURFACE);

        // 1. Content Area holding the 4 card feeds
        contentArea = new FrameLayout(this);
        root.addView(contentArea, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f));

        tabBrowseFeed = createBrowseFeed();
        tabFocusFeed = createFocusFeed();
        tabShieldFeed = createShieldFeed();
        tabCalmFeed = createCalmFeed();

        contentArea.addView(tabBrowseFeed);
        contentArea.addView(tabFocusFeed);
        contentArea.addView(tabShieldFeed);
        contentArea.addView(tabCalmFeed);

        // 2. Material Design 3 Bottom Navigation Bar
        root.addView(createM3BottomNavBar());

        setContentView(root);
        switchTab(0);

        refreshUsage();
        refreshPolicy();
    }

    // =========================================================================
    // MATERIAL DESIGN 3 BOTTOM NAVIGATION BAR (80dp height, active indicator pill)
    // =========================================================================
    private View createM3BottomNavBar() {
        LinearLayout navBar = new LinearLayout(this);
        navBar.setOrientation(LinearLayout.HORIZONTAL);
        navBar.setGravity(Gravity.CENTER_VERTICAL);
        navBar.setBackgroundColor(M3_SURFACE_CONTAINER_LOW);

        // 80dp standard M3 navigation bar height
        LinearLayout.LayoutParams navParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(80));
        navBar.setLayoutParams(navParams);

        // 1dp top divider stroke
        GradientDrawable navBg = new GradientDrawable();
        navBg.setColor(M3_SURFACE_CONTAINER_LOW);
        navBg.setStroke(dp(1), M3_OUTLINE_VARIANT);
        navBar.setBackground(navBg);

        String[] icons = {"🏠", "⏱️", "🛡️", "🧘"};
        String[] titles = {"Browse", "Focus", "Shield", "Calm"};

        for (int i = 0; i < 4; i++) {
            final int index = i;
            LinearLayout item = new LinearLayout(this);
            item.setOrientation(LinearLayout.VERTICAL);
            item.setGravity(Gravity.CENTER);
            item.setClickable(true);
            item.setFocusable(true);
            item.setPadding(0, dp(8), 0, dp(8));

            // Active indicator container (M3 64dp x 32dp pill)
            FrameLayout pill = new FrameLayout(this);
            pill.setLayoutParams(new LinearLayout.LayoutParams(dp(64), dp(32)));

            TextView iconView = new TextView(this);
            iconView.setText(icons[i]);
            iconView.setTextSize(18);
            iconView.setGravity(Gravity.CENTER);
            pill.addView(iconView, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

            item.addView(pill);

            // M3 Label (12sp Medium)
            TextView labelView = new TextView(this);
            labelView.setText(titles[i]);
            labelView.setTextSize(12);
            labelView.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.topMargin = dp(4);
            item.addView(labelView, lp);

            item.setOnClickListener(v -> switchTab(index));

            navTabItems[i] = item;
            navPillContainers[i] = pill;
            navIconViews[i] = iconView;
            navLabelViews[i] = labelView;

            LinearLayout.LayoutParams ip = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1.0f);
            navBar.addView(item, ip);
        }

        return navBar;
    }

    private void switchTab(int index) {
        currentTab = index;

        tabBrowseFeed.setVisibility(index == 0 ? View.VISIBLE : View.GONE);
        tabFocusFeed.setVisibility(index == 1 ? View.VISIBLE : View.GONE);
        tabShieldFeed.setVisibility(index == 2 ? View.VISIBLE : View.GONE);
        tabCalmFeed.setVisibility(index == 3 ? View.VISIBLE : View.GONE);

        for (int i = 0; i < 4; i++) {
            FrameLayout pill = navPillContainers[i];
            TextView iconView = navIconViews[i];
            TextView labelView = navLabelViews[i];
            if (pill == null || iconView == null || labelView == null) continue;

            if (i == index) {
                // Active M3 Indicator Pill
                GradientDrawable activePillBg = new GradientDrawable();
                activePillBg.setColor(M3_PRIMARY_CONTAINER);
                activePillBg.setCornerRadius(dp(16));
                pill.setBackground(activePillBg);

                labelView.setTextColor(M3_ON_SURFACE);
                labelView.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
                iconView.setAlpha(1.0f);
            } else {
                // Inactive M3 Destination
                pill.setBackground(null);
                labelView.setTextColor(M3_ON_SURFACE_VARIANT);
                labelView.setTypeface(Typeface.DEFAULT, Typeface.NORMAL);
                iconView.setAlpha(0.65f);
            }
        }

        if (index == 1) {
            refreshUsage();
        }
    }

    // =========================================================================
    // FEED 0: BROWSE (CARD-BASED FEED LAYOUT)
    // =========================================================================
    private View createBrowseFeed() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setVerticalScrollBarEnabled(false);

        LinearLayout feed = new LinearLayout(this);
        feed.setOrientation(LinearLayout.VERTICAL);
        feed.setPadding(dp(16), dp(16), dp(16), dp(24));
        scroll.addView(feed);

        // Card 1: Hero Search & Brand Card
        LinearLayout heroCard = m3Card();
        feed.addView(heroCard);

        LinearLayout brandRow = new LinearLayout(this);
        brandRow.setOrientation(LinearLayout.HORIZONTAL);
        brandRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView brandBadge = new TextView(this);
        brandBadge.setText("CALMFEED");
        brandBadge.setTextSize(13);
        brandBadge.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        brandBadge.setTextColor(M3_SECONDARY);
        brandBadge.setLetterSpacing(0.12f);
        brandRow.addView(brandBadge, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

        // Private / Incognito Chip Button
        TextView incognitoChip = new TextView(this);
        incognitoChip.setText("🕶️ Incognito");
        incognitoChip.setTextSize(12);
        incognitoChip.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        incognitoChip.setTextColor(M3_TERTIARY);
        incognitoChip.setPadding(dp(12), dp(6), dp(12), dp(6));
        GradientDrawable inBg = new GradientDrawable();
        inBg.setColor(M3_SURFACE_CONTAINER_HIGH);
        inBg.setCornerRadius(dp(16));
        inBg.setStroke(dp(1), M3_OUTLINE_VARIANT);
        incognitoChip.setBackground(inBg);
        incognitoChip.setClickable(true);
        incognitoChip.setOnClickListener(v -> {
            Intent intent = new Intent(this, BrowserActivity.class);
            intent.putExtra(BrowserActivity.EXTRA_INCOGNITO, true);
            intent.putExtra(BrowserActivity.EXTRA_URL, "https://www.google.com");
            startActivity(intent);
        });
        brandRow.addView(incognitoChip);
        heroCard.addView(brandRow);

        // M3 Search Field Capsule
        LinearLayout searchCapsule = new LinearLayout(this);
        searchCapsule.setOrientation(LinearLayout.HORIZONTAL);
        searchCapsule.setGravity(Gravity.CENTER_VERTICAL);
        searchCapsule.setPadding(dp(14), dp(2), dp(6), dp(2));
        GradientDrawable sBg = new GradientDrawable();
        sBg.setColor(M3_SURFACE_CONTAINER_LOW);
        sBg.setCornerRadius(dp(28));
        sBg.setStroke(dp(1), M3_PRIMARY);
        searchCapsule.setBackground(sBg);

        TextView searchIcon = new TextView(this);
        searchIcon.setText("🔍");
        searchIcon.setTextSize(16);
        searchCapsule.addView(searchIcon);

        searchOmnibox = new EditText(this);
        searchOmnibox.setHint("Search or enter address…");
        searchOmnibox.setHintTextColor(M3_ON_SURFACE_VARIANT);
        searchOmnibox.setTextColor(M3_ON_SURFACE);
        searchOmnibox.setTextSize(14);
        searchOmnibox.setSingleLine(true);
        searchOmnibox.setBackground(null);
        searchOmnibox.setImeOptions(EditorInfo.IME_ACTION_GO);
        searchOmnibox.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_URI);
        searchOmnibox.setPadding(dp(10), dp(12), dp(8), dp(12));
        searchOmnibox.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_GO || actionId == EditorInfo.IME_ACTION_SEARCH
                    || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                submitSearch();
                return true;
            }
            return false;
        });
        searchCapsule.addView(searchOmnibox, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

        TextView goBtn = new TextView(this);
        goBtn.setText("➔");
        goBtn.setTextSize(15);
        goBtn.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        goBtn.setTextColor(Color.WHITE);
        goBtn.setGravity(Gravity.CENTER);
        GradientDrawable goBg = new GradientDrawable();
        goBg.setColor(M3_PRIMARY);
        goBg.setCornerRadius(dp(20));
        goBtn.setBackground(goBg);
        goBtn.setPadding(dp(12), dp(6), dp(12), dp(6));
        goBtn.setOnClickListener(v -> submitSearch());
        searchCapsule.addView(goBtn);

        LinearLayout.LayoutParams scParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        scParams.topMargin = dp(14);
        heroCard.addView(searchCapsule, scParams);

        // Card 2: Quick Tools Chips Row (Horizontal Scroll)
        HorizontalScrollView hsv = new HorizontalScrollView(this);
        hsv.setHorizontalScrollBarEnabled(false);
        LinearLayout filterChips = new LinearLayout(this);
        filterChips.setOrientation(LinearLayout.HORIZONTAL);
        filterChips.setPadding(0, dp(12), 0, dp(4));

        filterChips.addView(createM3FilterChip("🕒 History", M3_PRIMARY, v -> showHistoryDialog()));
        filterChips.addView(createM3FilterChip("⭐ Bookmarks", Color.rgb(250, 204, 21), v -> showBookmarksDialog()));
        filterChips.addView(createM3FilterChip("🧘 1-Min Reset", M3_SECONDARY, v -> MindfulBreathingHelper.show(this, null)));
        filterChips.addView(createM3FilterChip("🎯 Focus Sites", M3_WARNING, v -> showFocusSitesDialog()));
        hsv.addView(filterChips);
        feed.addView(hsv);

        // Card 3: Top Sites Feed Card (Speed Dial)
        LinearLayout sitesCard = m3Card();
        LinearLayout.LayoutParams scardParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        scardParams.topMargin = dp(12);
        feed.addView(sitesCard, scardParams);

        TextView sitesCardTitle = new TextView(this);
        sitesCardTitle.setText("Top Sites");
        sitesCardTitle.setTextSize(16);
        sitesCardTitle.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        sitesCardTitle.setTextColor(M3_ON_SURFACE);
        sitesCard.addView(sitesCardTitle);

        GridLayout grid = new GridLayout(this);
        grid.setColumnCount(4);
        grid.setAlignmentMode(GridLayout.ALIGN_BOUNDS);
        grid.setUseDefaultMargins(false);

        String[] labels = {"YouTube", "Google", "Facebook", "TikTok", "Reddit", "Instagram", "X", "Wikipedia", "BBC News", "+ Custom"};
        String[] urls = {
                "https://www.youtube.com",
                "https://www.google.com",
                "https://m.facebook.com",
                "https://www.tiktok.com",
                "https://www.reddit.com",
                "https://www.instagram.com",
                "https://x.com",
                "https://www.wikipedia.org",
                "https://www.bbc.com/news",
                "custom"
        };
        int[] logoDrawables = {
                R.drawable.ic_logo_youtube,
                R.drawable.ic_logo_google,
                R.drawable.ic_logo_facebook,
                R.drawable.ic_logo_tiktok,
                R.drawable.ic_logo_reddit,
                R.drawable.ic_logo_instagram,
                R.drawable.ic_logo_x,
                R.drawable.ic_logo_wikipedia,
                R.drawable.ic_logo_bbc,
                R.drawable.ic_logo_custom
        };

        int itemWidth = (getResources().getDisplayMetrics().widthPixels - dp(64)) / 4;

        for (int i = 0; i < labels.length; i++) {
            final String targetUrl = urls[i];
            final String label = labels[i];

            LinearLayout item = new LinearLayout(this);
            item.setOrientation(LinearLayout.VERTICAL);
            item.setGravity(Gravity.CENTER);
            item.setPadding(dp(4), dp(10), dp(4), dp(10));
            GradientDrawable itemBg = new GradientDrawable();
            itemBg.setColor(M3_SURFACE_CONTAINER_HIGH);
            itemBg.setCornerRadius(dp(14));
            itemBg.setStroke(dp(1), M3_OUTLINE_VARIANT);
            item.setBackground(itemBg);
            item.setClickable(true);
            item.setFocusable(true);

            ImageView iconView = new ImageView(this);
            iconView.setImageResource(logoDrawables[i]);
            iconView.setScaleType(ImageView.ScaleType.FIT_CENTER);
            iconView.setContentDescription(label);
            item.addView(iconView, new LinearLayout.LayoutParams(dp(42), dp(42)));

            TextView labelView = new TextView(this);
            labelView.setText(label);
            labelView.setTextSize(11);
            labelView.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            labelView.setTextColor(M3_ON_SURFACE);
            labelView.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.topMargin = dp(6);
            item.addView(labelView, lp);

            item.setOnClickListener(v -> {
                if ("custom".equals(targetUrl)) {
                    promptCustomUrl();
                } else {
                    launchBrowser(targetUrl);
                }
            });

            GridLayout.LayoutParams gridParam = new GridLayout.LayoutParams();
            gridParam.width = itemWidth;
            gridParam.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            gridParam.setMargins(dp(3), dp(4), dp(3), dp(4));
            grid.addView(item, gridParam);
        }

        LinearLayout.LayoutParams gp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        gp.topMargin = dp(10);
        sitesCard.addView(grid, gp);

        // Card 4: Daily Focus Glance Feed Card
        LinearLayout glanceCard = m3Card();
        glanceCard.setOrientation(LinearLayout.HORIZONTAL);
        glanceCard.setGravity(Gravity.CENTER_VERTICAL);
        glanceCard.setPadding(dp(16), dp(14), dp(16), dp(14));
        glanceCard.setClickable(true);
        glanceCard.setOnClickListener(v -> switchTab(1));

        TextView gIcon = new TextView(this);
        gIcon.setText("⏱️");
        gIcon.setTextSize(20);
        glanceCard.addView(gIcon);

        LinearLayout gText = new LinearLayout(this);
        gText.setOrientation(LinearLayout.VERTICAL);
        gText.setPadding(dp(12), 0, dp(12), 0);

        TextView gTitle = new TextView(this);
        gTitle.setText("Daily Focus Allowance");
        gTitle.setTextSize(14);
        gTitle.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        gTitle.setTextColor(M3_ON_SURFACE);
        gText.addView(gTitle);

        focusGlanceStatus = new TextView(this);
        focusGlanceStatus.setText("30 min daily goal active • Tap to manage");
        focusGlanceStatus.setTextSize(12);
        focusGlanceStatus.setTextColor(M3_SECONDARY);
        gText.addView(focusGlanceStatus);

        glanceCard.addView(gText, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

        TextView gArrow = new TextView(this);
        gArrow.setText("➔");
        gArrow.setTextSize(15);
        gArrow.setTextColor(M3_ON_SURFACE_VARIANT);
        glanceCard.addView(gArrow);

        LinearLayout.LayoutParams gcParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        gcParams.topMargin = dp(12);
        feed.addView(glanceCard, gcParams);

        return scroll;
    }

    private View createM3FilterChip(String label, int accentColor, View.OnClickListener onClick) {
        TextView chip = new TextView(this);
        chip.setText(label);
        chip.setTextSize(12);
        chip.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        chip.setTextColor(M3_ON_SURFACE);
        chip.setPadding(dp(14), dp(8), dp(14), dp(8));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(M3_SURFACE_CONTAINER);
        bg.setCornerRadius(dp(8));
        bg.setStroke(dp(1), M3_OUTLINE_VARIANT);
        chip.setBackground(bg);
        chip.setClickable(true);
        chip.setFocusable(true);
        chip.setOnClickListener(onClick);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.rightMargin = dp(8);
        chip.setLayoutParams(lp);
        return chip;
    }

    // =========================================================================
    // FEED 1: FOCUS (CARD-BASED FEED LAYOUT)
    // =========================================================================
    private View createFocusFeed() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setVerticalScrollBarEnabled(false);

        LinearLayout feed = new LinearLayout(this);
        feed.setOrientation(LinearLayout.VERTICAL);
        feed.setPadding(dp(16), dp(16), dp(16), dp(24));
        scroll.addView(feed);

        // Card 1: Today's Focus Metrics Hero Card
        LinearLayout heroCard = m3Card();
        feed.addView(heroCard);

        TextView heroTitle = new TextView(this);
        heroTitle.setText("Today's Focus Status");
        heroTitle.setTextSize(14);
        heroTitle.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        heroTitle.setTextColor(M3_PRIMARY);
        heroTitle.setLetterSpacing(0.06f);
        heroCard.addView(heroTitle);

        usageValue = new TextView(this);
        usageValue.setText("0 min");
        usageValue.setTextSize(36);
        usageValue.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        usageValue.setTextColor(M3_ON_SURFACE);
        LinearLayout.LayoutParams uvParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        uvParams.topMargin = dp(4);
        heroCard.addView(usageValue, uvParams);

        usageProgressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        usageProgressBar.setMax(30);
        usageProgressBar.setProgress(0);
        usageProgressBar.setProgressTintList(android.content.res.ColorStateList.valueOf(M3_SECONDARY));
        usageProgressBar.setProgressBackgroundTintList(android.content.res.ColorStateList.valueOf(M3_SURFACE_CONTAINER_HIGH));
        LinearLayout.LayoutParams upParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(8));
        upParams.topMargin = dp(8);
        heroCard.addView(usageProgressBar, upParams);

        usageSubtitle = new TextView(this);
        usageSubtitle.setText("0 min used today • 30 min focus limit remaining");
        usageSubtitle.setTextSize(12);
        usageSubtitle.setTextColor(M3_ON_SURFACE_VARIANT);
        LinearLayout.LayoutParams usParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        usParams.topMargin = dp(8);
        heroCard.addView(usageSubtitle, usParams);

        // Card 2: Set Daily Goal Feed Card (1-Tap M3 Chips)
        LinearLayout goalCard = m3Card();
        LinearLayout.LayoutParams gcParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        gcParams.topMargin = dp(12);
        feed.addView(goalCard, gcParams);

        TextView goalTitle = new TextView(this);
        goalTitle.setText("Daily Goal Preset");
        goalTitle.setTextSize(14);
        goalTitle.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        goalTitle.setTextColor(M3_ON_SURFACE);
        goalCard.addView(goalTitle);

        LinearLayout chipsRow = new LinearLayout(this);
        chipsRow.setOrientation(LinearLayout.HORIZONTAL);
        chipsRow.setPadding(0, dp(10), 0, dp(4));

        int[] presetMinutes = {15, 30, 45, 60, 120};
        String[] presetLabels = {"15m", "30m", "45m", "1 hr", "2 hr"};

        for (int i = 0; i < presetMinutes.length; i++) {
            final int min = presetMinutes[i];
            TextView chip = new TextView(this);
            chip.setText(presetLabels[i]);
            chip.setTextSize(13);
            chip.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            chip.setTextColor(M3_ON_SURFACE);
            chip.setGravity(Gravity.CENTER);
            chip.setPadding(dp(8), dp(10), dp(8), dp(10));
            GradientDrawable chipBg = new GradientDrawable();
            chipBg.setColor(M3_SURFACE_CONTAINER_HIGH);
            chipBg.setCornerRadius(dp(8));
            chipBg.setStroke(dp(1), M3_OUTLINE_VARIANT);
            chip.setBackground(chipBg);
            chip.setClickable(true);
            chip.setOnClickListener(v -> setAllowanceDirectly(min));

            LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
            if (i > 0) cp.leftMargin = dp(6);
            chipsRow.addView(chip, cp);
        }
        goalCard.addView(chipsRow);

        allowanceLabel = new TextView(this);
        allowanceLabel.setText("30 minutes daily");
        allowanceLabel.setTextSize(13);
        allowanceLabel.setTextColor(M3_SECONDARY);
        allowanceLabel.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        LinearLayout.LayoutParams alParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        alParams.topMargin = dp(10);
        goalCard.addView(allowanceLabel, alParams);

        allowanceSeekBar = new SeekBar(this);
        allowanceSeekBar.setMax(225);
        allowanceSeekBar.setProgress(15);
        allowanceSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int minutes = 15 + Math.round(progress / 5f) * 5;
                allowanceLabel.setText(minutes + " minutes daily");
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                int minutes = 15 + Math.round(seekBar.getProgress() / 5f) * 5;
                setAllowanceDirectly(minutes);
            }
        });
        goalCard.addView(allowanceSeekBar);

        setAllowanceButton = m3Button("Save Allowance Goal", M3_SECONDARY, M3_SECONDARY_CONTAINER);
        setAllowanceButton.setVisibility(View.GONE);
        setAllowanceButton.setOnClickListener(v -> saveDailyAllowance());
        goalCard.addView(setAllowanceButton);

        requestApprovalButton = m3Button("Request Approval from Partner", M3_WARNING, Color.rgb(124, 45, 18));
        requestApprovalButton.setVisibility(View.GONE);
        requestApprovalButton.setOnClickListener(v -> requestExtraTime());
        goalCard.addView(requestApprovalButton);

        // Card 3: Platform Breakdown Feed Card
        LinearLayout breakdownCard = m3Card();
        LinearLayout.LayoutParams bcParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        bcParams.topMargin = dp(12);
        feed.addView(breakdownCard, bcParams);

        TextView bdTitle = new TextView(this);
        bdTitle.setText("Platform Breakdown");
        bdTitle.setTextSize(15);
        bdTitle.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        bdTitle.setTextColor(M3_ON_SURFACE);
        breakdownCard.addView(bdTitle);

        usageBreakdownContainer = new LinearLayout(this);
        usageBreakdownContainer.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams ubParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        ubParams.topMargin = dp(8);
        breakdownCard.addView(usageBreakdownContainer, ubParams);

        // Card 4: Action Feed Card
        LinearLayout actionCard = m3Card();
        LinearLayout.LayoutParams acParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        acParams.topMargin = dp(12);
        feed.addView(actionCard, acParams);

        Button customizeBtn = m3Button("🎯 Customize Distraction Sites", M3_ON_SURFACE, M3_SURFACE_CONTAINER_HIGH);
        customizeBtn.setOnClickListener(v -> showFocusSitesDialog());
        actionCard.addView(customizeBtn, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(48)));

        Button partnerBtn = m3Button("👥 Accountability Partner & Circle →", M3_ON_PRIMARY_CONTAINER, M3_PRIMARY_CONTAINER);
        partnerBtn.setOnClickListener(v -> startActivity(new Intent(this, TrustActivity.class)));
        LinearLayout.LayoutParams pbParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(48));
        pbParams.topMargin = dp(8);
        actionCard.addView(partnerBtn, pbParams);

        return scroll;
    }

    private void setAllowanceDirectly(int minutes) {
        allowanceSeekBar.setProgress(minutes - 15);
        allowanceLabel.setText(minutes + " minutes daily");
        UsageTracker.setTodayAllowance(this, minutes);
        Toast.makeText(this, "Daily goal set to " + minutes + " min", Toast.LENGTH_SHORT).show();
        refreshUsage();
        refreshPolicy();
    }

    // =========================================================================
    // FEED 2: SHIELD (CARD-BASED FEED LAYOUT)
    // =========================================================================
    private View createShieldFeed() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setVerticalScrollBarEnabled(false);

        LinearLayout feed = new LinearLayout(this);
        feed.setOrientation(LinearLayout.VERTICAL);
        feed.setPadding(dp(16), dp(16), dp(16), dp(24));
        scroll.addView(feed);

        // Card 1: GeckoView Strict Shield Hero Card
        LinearLayout heroShield = m3Card();
        GradientDrawable sBg = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{Color.rgb(15, 34, 45), M3_SURFACE_CONTAINER});
        sBg.setCornerRadius(dp(16));
        sBg.setStroke(dp(1), M3_SECONDARY);
        heroShield.setBackground(sBg);
        feed.addView(heroShield);

        TextView sTitle = new TextView(this);
        sTitle.setText("🛡️ GeckoView Strict Protection");
        sTitle.setTextSize(16);
        sTitle.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        sTitle.setTextColor(M3_SECONDARY);
        heroShield.addView(sTitle);

        TextView sSub = new TextView(this);
        sSub.setText("• Cross-site tracking cookies blocked\n• Fingerprinting & cryptomining stopped\n• Real-time on-device Gecko engine filtering");
        sSub.setTextSize(12);
        sSub.setTextColor(M3_ON_SURFACE);
        sSub.setLineSpacing(dp(3), 1.15f);
        LinearLayout.LayoutParams ssParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        ssParams.topMargin = dp(8);
        heroShield.addView(sSub, ssParams);

        // Card 2: Hardware Sensor Permissions Card
        LinearLayout sensorsCard = m3Card();
        LinearLayout.LayoutParams scParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        scParams.topMargin = dp(12);
        feed.addView(sensorsCard, scParams);

        TextView sensTitle = new TextView(this);
        sensTitle.setText("Hardware Sensor Permissions");
        sensTitle.setTextSize(15);
        sensTitle.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        sensTitle.setTextColor(M3_ON_SURFACE);
        sensorsCard.addView(sensTitle);

        sensorsCard.addView(createSensorRow("📍 Location Access", "GPS and coarse location"));
        sensorsCard.addView(createSensorRow("📷 Camera Access", "Video and photo input"));
        sensorsCard.addView(createSensorRow("🎙️ Microphone Access", "Audio stream recording"));
        sensorsCard.addView(createSensorRow("🔔 Web Notifications", "Website push notifications"));

        Button manageSitesBtn = m3Button("Manage Per-Site Rules →", M3_ON_PRIMARY_CONTAINER, M3_PRIMARY_CONTAINER);
        manageSitesBtn.setOnClickListener(v -> startActivity(new Intent(this, SiteSettingsActivity.class)));
        LinearLayout.LayoutParams msParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(48));
        msParams.topMargin = dp(14);
        sensorsCard.addView(manageSitesBtn, msParams);

        return scroll;
    }

    private View createSensorRow(String title, String subtitle) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(10), 0, dp(10));

        LinearLayout textCol = new LinearLayout(this);
        textCol.setOrientation(LinearLayout.VERTICAL);

        TextView tView = new TextView(this);
        tView.setText(title);
        tView.setTextSize(13);
        tView.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        tView.setTextColor(M3_ON_SURFACE);
        textCol.addView(tView);

        TextView sView = new TextView(this);
        sView.setText(subtitle);
        sView.setTextSize(11);
        sView.setTextColor(M3_ON_SURFACE_VARIANT);
        textCol.addView(sView);

        row.addView(textCol, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

        TextView badge = new TextView(this);
        badge.setText("Protected");
        badge.setTextSize(11);
        badge.setTextColor(M3_SECONDARY);
        badge.setPadding(dp(10), dp(4), dp(10), dp(4));
        GradientDrawable bBg = new GradientDrawable();
        bBg.setColor(M3_SURFACE_CONTAINER_HIGH);
        bBg.setCornerRadius(dp(8));
        badge.setBackground(bBg);
        row.addView(badge);

        return row;
    }

    // =========================================================================
    // FEED 3: CALM (CARD-BASED FEED LAYOUT)
    // =========================================================================
    private View createCalmFeed() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setVerticalScrollBarEnabled(false);

        LinearLayout feed = new LinearLayout(this);
        feed.setOrientation(LinearLayout.VERTICAL);
        feed.setPadding(dp(16), dp(16), dp(16), dp(24));
        scroll.addView(feed);

        // Card 1: Box Breathing Feed Card
        LinearLayout breathCard = m3Card();
        feed.addView(breathCard);

        TextView bTitle = new TextView(this);
        bTitle.setText("🧘 1-Minute Box Breathing Reset");
        bTitle.setTextSize(16);
        bTitle.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        bTitle.setTextColor(M3_SECONDARY);
        breathCard.addView(bTitle);

        TextView bSub = new TextView(this);
        bSub.setText("4s Inhale • 4s Hold • 4s Exhale • 4s Rest");
        bSub.setTextSize(12);
        bSub.setTextColor(M3_ON_SURFACE_VARIANT);
        bSub.setPadding(0, dp(4), 0, dp(12));
        breathCard.addView(bSub);

        Button startBreathBtn = m3Button("Start Breathing Reset ➔", Color.WHITE, M3_SECONDARY);
        startBreathBtn.setOnClickListener(v -> MindfulBreathingHelper.show(this, null));
        LinearLayout.LayoutParams sbbParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(48));
        breathCard.addView(startBreathBtn, sbbParams);

        // Card 2: Daily Reflection Feed Card
        LinearLayout quoteCard = m3Card();
        LinearLayout.LayoutParams qcParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        qcParams.topMargin = dp(12);
        feed.addView(quoteCard, qcParams);

        TextView qTitle = new TextView(this);
        qTitle.setText("DAILY REFLECTION");
        qTitle.setTextSize(11);
        qTitle.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        qTitle.setTextColor(M3_TERTIARY);
        qTitle.setLetterSpacing(0.08f);
        quoteCard.addView(qTitle);

        TextView quoteText = new TextView(this);
        quoteText.setText("“" + dailyQuote() + "”");
        quoteText.setTextSize(15);
        quoteText.setTextColor(M3_ON_SURFACE);
        quoteText.setLineSpacing(dp(3), 1.15f);
        LinearLayout.LayoutParams qtParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        qtParams.topMargin = dp(8);
        quoteCard.addView(quoteText, qtParams);

        // Card 3: Mindful Pause Settings Feed Card
        LinearLayout pauseCard = m3Card();
        LinearLayout.LayoutParams pcParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        pcParams.topMargin = dp(12);
        feed.addView(pauseCard, pcParams);

        LinearLayout pRow = new LinearLayout(this);
        pRow.setOrientation(LinearLayout.HORIZONTAL);
        pRow.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout pText = new LinearLayout(this);
        pText.setOrientation(LinearLayout.VERTICAL);

        TextView pTitle = new TextView(this);
        pTitle.setText("Mindful Pause on Launch");
        pTitle.setTextSize(14);
        pTitle.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        pTitle.setTextColor(M3_ON_SURFACE);
        pText.addView(pTitle);

        TextView pSub = new TextView(this);
        pSub.setText("Take a 3-second breath before opening social feeds");
        pSub.setTextSize(11);
        pSub.setTextColor(M3_ON_SURFACE_VARIANT);
        pText.addView(pSub);

        pRow.addView(pText, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

        TextView pauseToggle = new TextView(this);
        boolean isPause = isMindfulPauseEnabled();
        pauseToggle.setText(isPause ? "ON" : "OFF");
        pauseToggle.setTextSize(12);
        pauseToggle.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        pauseToggle.setTextColor(Color.WHITE);
        pauseToggle.setPadding(dp(14), dp(6), dp(14), dp(6));
        GradientDrawable ptBg = new GradientDrawable();
        ptBg.setColor(isPause ? M3_SECONDARY : M3_SURFACE_CONTAINER_HIGH);
        ptBg.setCornerRadius(dp(12));
        pauseToggle.setBackground(ptBg);
        pauseToggle.setClickable(true);
        pauseToggle.setOnClickListener(v -> {
            boolean next = !isMindfulPauseEnabled();
            setMindfulPauseEnabled(next);
            pauseToggle.setText(next ? "ON" : "OFF");
            ptBg.setColor(next ? M3_SECONDARY : M3_SURFACE_CONTAINER_HIGH);
            Toast.makeText(MainActivity.this, next ? "Mindful pause enabled" : "Mindful pause disabled", Toast.LENGTH_SHORT).show();
        });
        pRow.addView(pauseToggle);
        pauseCard.addView(pRow);

        return scroll;
    }

    // =========================================================================
    // MINDFUL PAUSE DIALOG (CLEAN & FAST M3 DIALOG)
    // =========================================================================
    private void showMindfulPauseDialog(String url) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(22), dp(20), dp(22), dp(20));
        content.setBackgroundColor(M3_SURFACE_CONTAINER);

        TextView icon = new TextView(this);
        icon.setText("🌿 Mindful Pause");
        icon.setTextSize(17);
        icon.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        icon.setTextColor(M3_SECONDARY);
        content.addView(icon);

        TextView hostLabel = new TextView(this);
        hostLabel.setText("Opening " + SitePermissionManager.cleanHost(url));
        hostLabel.setTextSize(13);
        hostLabel.setTextColor(M3_ON_SURFACE_VARIANT);
        hostLabel.setPadding(0, dp(4), 0, dp(12));
        content.addView(hostLabel);

        // Fast Intention Chips
        LinearLayout intentions = new LinearLayout(this);
        intentions.setOrientation(LinearLayout.VERTICAL);
        intentions.setPadding(0, 0, 0, dp(12));

        String[] choices = {
                "🔍 Find specific content",
                "💬 Check a message",
                "⏱️ Quick 5-minute break"
        };

        for (String choice : choices) {
            TextView chip = new TextView(this);
            chip.setText(choice);
            chip.setTextSize(12);
            chip.setTextColor(M3_ON_SURFACE);
            chip.setPadding(dp(12), dp(8), dp(12), dp(8));
            GradientDrawable chipBg = new GradientDrawable();
            chipBg.setColor(M3_SURFACE_CONTAINER_HIGH);
            chipBg.setCornerRadius(dp(10));
            chipBg.setStroke(dp(1), M3_OUTLINE_VARIANT);
            chip.setBackground(chipBg);
            chip.setClickable(true);
            chip.setOnClickListener(v -> {
                for (int j = 0; j < intentions.getChildCount(); j++) {
                    View c = intentions.getChildAt(j);
                    GradientDrawable b = new GradientDrawable();
                    b.setCornerRadius(dp(10));
                    if (c == chip) {
                        b.setColor(M3_SECONDARY);
                        b.setStroke(dp(1), M3_SECONDARY);
                    } else {
                        b.setColor(M3_SURFACE_CONTAINER_HIGH);
                        b.setStroke(dp(1), M3_OUTLINE_VARIANT);
                    }
                    c.setBackground(b);
                }
            });

            LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            cp.bottomMargin = dp(6);
            intentions.addView(chip, cp);
        }
        content.addView(intentions);

        Button proceedBtn = m3Button("Proceed (3s)", Color.WHITE, M3_PRIMARY);
        proceedBtn.setEnabled(false);
        proceedBtn.setAlpha(0.6f);
        content.addView(proceedBtn);

        Button stepAwayBtn = m3Button("Stay Present ✕", M3_ON_SURFACE_VARIANT, Color.TRANSPARENT);
        LinearLayout.LayoutParams saParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(40));
        saParams.topMargin = dp(4);
        content.addView(stepAwayBtn, saParams);

        AlertDialog dialog = builder.setView(content).create();

        Handler h = new Handler(Looper.getMainLooper());
        final int[] countdown = {3};
        Runnable countTick = new Runnable() {
            @Override
            public void run() {
                countdown[0]--;
                if (countdown[0] > 0) {
                    proceedBtn.setText("Proceed (" + countdown[0] + "s)");
                    h.postDelayed(this, 1000);
                } else {
                    proceedBtn.setEnabled(true);
                    proceedBtn.setAlpha(1.0f);
                    proceedBtn.setText("Proceed ➔");
                }
            }
        };
        h.postDelayed(countTick, 1000);

        proceedBtn.setOnClickListener(v -> {
            h.removeCallbacks(countTick);
            dialog.dismiss();
            directLaunchBrowser(url);
        });

        stepAwayBtn.setOnClickListener(v -> {
            h.removeCallbacks(countTick);
            dialog.dismiss();
        });

        dialog.setOnDismissListener(d -> h.removeCallbacks(countTick));
        dialog.show();
    }

    // =========================================================================
    // M3 COMPONENT BUILDERS (Cards, Buttons, Formatting)
    // =========================================================================
    private LinearLayout m3Card() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(16), dp(16), dp(16));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(M3_SURFACE_CONTAINER);
        bg.setCornerRadius(dp(16)); // M3 Medium Shape Corner (16dp)
        bg.setStroke(dp(1), M3_OUTLINE_VARIANT);
        card.setBackground(bg);
        return card;
    }

    private Button m3Button(String text, int textColor, int bgColor) {
        Button btn = new Button(this);
        btn.setText(text);
        btn.setAllCaps(false);
        btn.setTextSize(13);
        btn.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        btn.setTextColor(textColor);
        btn.setMinHeight(dp(48)); // M3 48dp minimum interactive touch target
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(bgColor);
        bg.setCornerRadius(dp(12));
        btn.setBackground(bg);
        return btn;
    }

    private String dailyQuote() {
        int day = LocalDate.now().getDayOfYear();
        int index = Math.floorMod(day * 31 + LocalDate.now().getYear(), DAILY_QUOTES.length);
        return DAILY_QUOTES[index];
    }

    private void submitSearch() {
        String query = searchOmnibox.getText().toString().trim();
        if (TextUtils.isEmpty(query)) {
            Toast.makeText(this, "Enter a search term or website", Toast.LENGTH_SHORT).show();
            return;
        }

        String targetUrl;
        if (query.startsWith("http://") || query.startsWith("https://") || query.startsWith("about:")) {
            targetUrl = query;
        } else if (!query.contains(" ") && query.contains(".") && !query.endsWith(".")) {
            targetUrl = "https://" + query;
        } else {
            try {
                targetUrl = "https://www.google.com/search?q=" + URLEncoder.encode(query, "UTF-8");
            } catch (Exception e) {
                targetUrl = "https://www.google.com/search?q=" + query;
            }
        }

        searchOmnibox.setText("");
        hideKeyboard(searchOmnibox);

        Intent intent = new Intent(this, BrowserActivity.class);
        intent.putExtra(BrowserActivity.EXTRA_URL, targetUrl);
        startActivity(intent);
    }

    private void promptCustomUrl() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Open Website");

        final EditText input = new EditText(this);
        input.setHint("e.g. github.com or cnn.com");
        input.setSingleLine(true);
        input.setPadding(dp(20), dp(14), dp(20), dp(14));
        builder.setView(input);

        builder.setPositiveButton("Open", (dialog, which) -> {
            String url = input.getText().toString().trim();
            if (!TextUtils.isEmpty(url)) {
                if (!url.startsWith("http://") && !url.startsWith("https://")) {
                    url = "https://" + url;
                }
                launchBrowser(url);
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void launchBrowser(String url) {
        if (UsageTracker.isSiteLimited(this, url) && isMindfulPauseEnabled()) {
            showMindfulPauseDialog(url);
        } else {
            directLaunchBrowser(url);
        }
    }

    private void directLaunchBrowser(String url) {
        Intent intent = new Intent(this, BrowserActivity.class);
        intent.putExtra(BrowserActivity.EXTRA_URL, url);
        startActivity(intent);
    }

    private boolean isMindfulPauseEnabled() {
        return getSharedPreferences("calmfeed_settings", MODE_PRIVATE).getBoolean("mindful_pause", true);
    }

    private void setMindfulPauseEnabled(boolean enabled) {
        getSharedPreferences("calmfeed_settings", MODE_PRIVATE).edit().putBoolean("mindful_pause", enabled).apply();
    }

    private void refreshUsage() {
        long socialSeconds = UsageTracker.getTodaySeconds(this);
        long browsingSeconds = UsageTracker.getTodayBrowsingSeconds(this);
        long socialMinutes = socialSeconds / 60;
        long browsingMinutes = browsingSeconds / 60;
        int allowanceMin = UsageTracker.getTodayAllowanceMinutes(this);

        if (usageValue != null) {
            usageValue.setText(String.format(Locale.getDefault(), "%d min", socialMinutes));
        }
        if (usageProgressBar != null) {
            usageProgressBar.setMax(Math.max(allowanceMin, 30));
            usageProgressBar.setProgress((int) Math.min(socialMinutes, usageProgressBar.getMax()));
        }

        long remainingSec = UsageTracker.getDailyRemainingSeconds(this);
        long remainingMin = (remainingSec + 59) / 60;

        if (usageSubtitle != null) {
            usageSubtitle.setText(String.format(Locale.getDefault(), "%d min used today • %d min focus limit remaining", socialMinutes, remainingMin));
        }

        if (focusGlanceStatus != null) {
            focusGlanceStatus.setText(String.format(Locale.getDefault(), "%d min used of %d min daily goal", socialMinutes, allowanceMin));
        }

        if (usageBreakdownContainer != null) {
            usageBreakdownContainer.removeAllViews();

            long ytSec = UsageTracker.getTodayDomainSeconds(this, "youtube.com");
            long ttSec = UsageTracker.getTodayDomainSeconds(this, "tiktok.com");
            long fbSec = UsageTracker.getTodayDomainSeconds(this, "facebook.com");
            long igSec = UsageTracker.getTodayDomainSeconds(this, "instagram.com");

            usageBreakdownContainer.addView(createUsageRow("YouTube", "🔴", ytSec / 60, Color.rgb(239, 68, 68)));
            usageBreakdownContainer.addView(createUsageRow("TikTok", "🎵", ttSec / 60, Color.rgb(236, 72, 153)));
            usageBreakdownContainer.addView(createUsageRow("Facebook", "🔵", fbSec / 60, Color.rgb(59, 130, 246)));
            usageBreakdownContainer.addView(createUsageRow("Instagram", "🟣", igSec / 60, M3_TERTIARY));
            usageBreakdownContainer.addView(createUsageRow("General Browsing", "🌐", browsingMinutes, M3_PRIMARY));

            Set<String> customSites = UsageTracker.getCustomDistractions(this);
            for (String s : customSites) {
                long cSec = UsageTracker.getTodayDomainSeconds(this, s);
                if (cSec > 0) {
                    usageBreakdownContainer.addView(createUsageRow(s, "🎯", cSec / 60, M3_WARNING));
                }
            }
        }
    }

    private View createUsageRow(String label, String icon, long minutes, int accentColor) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(4), dp(6), dp(4), dp(6));

        TextView iconTv = new TextView(this);
        iconTv.setText(icon);
        iconTv.setTextSize(14);
        row.addView(iconTv);

        TextView labelTv = new TextView(this);
        labelTv.setText(" " + label);
        labelTv.setTextSize(13);
        labelTv.setTextColor(M3_ON_SURFACE);
        row.addView(labelTv, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

        TextView minTv = new TextView(this);
        minTv.setText(minutes + " min");
        minTv.setTextSize(12);
        minTv.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        minTv.setTextColor(minutes > 0 ? accentColor : M3_ON_SURFACE_VARIANT);
        minTv.setPadding(dp(8), dp(3), dp(8), dp(3));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(M3_SURFACE_CONTAINER_HIGH);
        bg.setCornerRadius(dp(8));
        minTv.setBackground(bg);
        row.addView(minTv);

        return row;
    }

    private void saveDailyAllowance() {
        int minutes = 15 + Math.round(allowanceSeekBar.getProgress() / 5f) * 5;
        UsageTracker.setTodayAllowance(this, minutes);

        if (FirebaseBridge.isConfigured(this)) {
            Map<String, Object> data = new HashMap<>();
            data.put("minutes", minutes);
            data.put("timeZone", FirebaseBridge.timezoneId());
            if (setAllowanceButton != null) setAllowanceButton.setEnabled(false);
            FirebaseBridge.call(this, "setDailyAllowance", data, (result, error) -> {
                if (isFinishing()) return;
                if (setAllowanceButton != null) setAllowanceButton.setEnabled(true);
                Toast.makeText(this, "Daily goal set to " + minutes + " min", Toast.LENGTH_SHORT).show();
                refreshPolicy();
            });
        } else {
            Toast.makeText(this, "Daily goal set to " + minutes + " min", Toast.LENGTH_SHORT).show();
            renderOnDevicePolicy(minutes);
        }
    }

    private void requestExtraTime() {
        if (!FirebaseBridge.isConfigured(this)) {
            Toast.makeText(this, "Connect Firebase to enable trusted person approvals.", Toast.LENGTH_LONG).show();
            return;
        }

        Map<String, Object> data = new HashMap<>();
        data.put("requestedMinutes", 15);
        data.put("timeZone", FirebaseBridge.timezoneId());
        requestApprovalButton.setEnabled(false);
        FirebaseBridge.call(this, "requestExtraTime", data, (result, error) -> {
            if (isFinishing()) return;
            requestApprovalButton.setEnabled(true);
            if (error != null) {
                Toast.makeText(this, error, Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "Sent extra-time request to your accountability circle.", Toast.LENGTH_LONG).show();
                refreshPolicy();
            }
        });
    }

    private void refreshPolicy() {
        int localAllowance = UsageTracker.getTodayAllowanceMinutes(this);
        if (allowanceSeekBar != null) {
            allowanceSeekBar.setProgress(Math.max(0, localAllowance - 15));
        }
        if (allowanceLabel != null) {
            allowanceLabel.setText(localAllowance + " minutes daily");
        }

        if (!FirebaseBridge.isConfigured(this)) {
            renderOnDevicePolicy(localAllowance);
            return;
        }

        Map<String, Object> data = new HashMap<>();
        data.put("timeZone", FirebaseBridge.timezoneId());
        FirebaseBridge.call(this, "getDailyPolicy", data, (result, error) -> {
            if (isFinishing() || result == null) {
                renderOnDevicePolicy(localAllowance);
                return;
            }
            latestPolicy = result;
            int allowance = number(result.get("dailyAllowanceMinutes"));
            if (allowance > 0) {
                UsageTracker.setTodayAllowance(this, allowance);
                if (allowanceSeekBar != null) {
                    allowanceSeekBar.setProgress(Math.max(0, allowance - 15));
                }
                if (allowanceLabel != null) {
                    allowanceLabel.setText(allowance + " minutes daily");
                }
            }
        });
    }

    private void renderOnDevicePolicy(int allowance) {
        if (allowanceSeekBar != null) {
            allowanceSeekBar.setProgress(Math.max(0, allowance - 15));
        }
        if (allowanceLabel != null) {
            allowanceLabel.setText(allowance + " minutes daily");
        }
    }

    private void showHistoryDialog() {
        List<BrowserHistoryManager.HistoryItem> history = BrowserHistoryManager.getAll(this);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setBackgroundColor(M3_SURFACE_CONTAINER);
        content.setPadding(dp(16), dp(16), dp(16), dp(16));

        TextView title = new TextView(this);
        title.setText("Browsing History (" + history.size() + ")");
        title.setTextSize(17);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setTextColor(M3_ON_SURFACE);
        content.addView(title);

        if (history.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("No browsing history recorded yet.");
            empty.setTextColor(M3_ON_SURFACE_VARIANT);
            empty.setPadding(0, dp(24), 0, dp(24));
            content.addView(empty);
            builder.setView(content).setNegativeButton("Close", null).show();
            return;
        }

        ListView listView = new ListView(this);
        listView.setAdapter(new BaseAdapter() {
            @Override public int getCount() { return history.size(); }
            @Override public Object getItem(int pos) { return history.get(pos); }
            @Override public long getItemId(int pos) { return history.get(pos).id; }
            @Override
            public View getView(int pos, View convertView, ViewGroup parent) {
                LinearLayout row = new LinearLayout(MainActivity.this);
                row.setOrientation(LinearLayout.VERTICAL);
                row.setPadding(dp(8), dp(8), dp(8), dp(8));

                BrowserHistoryManager.HistoryItem item = history.get(pos);
                TextView itemTitle = new TextView(MainActivity.this);
                itemTitle.setText(item.title);
                itemTitle.setTextColor(M3_ON_SURFACE);
                itemTitle.setTextSize(13);
                itemTitle.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
                row.addView(itemTitle);

                TextView itemUrl = new TextView(MainActivity.this);
                itemUrl.setText(item.url + " · " + item.formattedTime);
                itemUrl.setTextColor(M3_ON_SURFACE_VARIANT);
                itemUrl.setTextSize(11);
                row.addView(itemUrl);
                return row;
            }
        });

        AlertDialog dialog = builder.setView(content)
                .setNegativeButton("Close", null)
                .setNeutralButton("Clear History", (d, w) -> {
                    BrowserHistoryManager.clear(this);
                    Toast.makeText(this, "History cleared", Toast.LENGTH_SHORT).show();
                })
                .create();

        listView.setOnItemClickListener((parent, view, position, id) -> {
            dialog.dismiss();
            launchBrowser(history.get(position).url);
        });

        content.addView(listView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(350)));
        dialog.show();
    }

    private void showBookmarksDialog() {
        List<BookmarkManager.BookmarkItem> bookmarks = BookmarkManager.getAll(this);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setBackgroundColor(M3_SURFACE_CONTAINER);
        content.setPadding(dp(16), dp(16), dp(16), dp(16));

        TextView title = new TextView(this);
        title.setText("Bookmarks (" + bookmarks.size() + ")");
        title.setTextSize(17);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setTextColor(M3_ON_SURFACE);
        content.addView(title);

        ListView listView = new ListView(this);
        listView.setAdapter(new BaseAdapter() {
            @Override public int getCount() { return bookmarks.size(); }
            @Override public Object getItem(int pos) { return bookmarks.get(pos); }
            @Override public long getItemId(int pos) { return pos; }
            @Override
            public View getView(int pos, View convertView, ViewGroup parent) {
                LinearLayout row = new LinearLayout(MainActivity.this);
                row.setOrientation(LinearLayout.VERTICAL);
                row.setPadding(dp(8), dp(8), dp(8), dp(8));

                BookmarkManager.BookmarkItem item = bookmarks.get(pos);
                TextView itemTitle = new TextView(MainActivity.this);
                itemTitle.setText("⭐ " + item.title);
                itemTitle.setTextColor(M3_ON_SURFACE);
                itemTitle.setTextSize(13);
                itemTitle.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
                row.addView(itemTitle);

                TextView itemUrl = new TextView(MainActivity.this);
                itemUrl.setText(item.url);
                itemUrl.setTextColor(M3_ON_SURFACE_VARIANT);
                itemUrl.setTextSize(11);
                row.addView(itemUrl);
                return row;
            }
        });

        AlertDialog dialog = builder.setView(content)
                .setNegativeButton("Close", null)
                .create();

        listView.setOnItemClickListener((parent, view, position, id) -> {
            dialog.dismiss();
            launchBrowser(bookmarks.get(position).url);
        });

        content.addView(listView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(350)));
        dialog.show();
    }

    private void showFocusSitesDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(20), dp(18), dp(20), dp(18));
        content.setBackgroundColor(M3_SURFACE_CONTAINER);

        TextView title = new TextView(this);
        title.setText("🎯 Focus & Distraction Sites");
        title.setTextSize(17);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setTextColor(M3_ON_SURFACE);
        content.addView(title);

        TextView sub = new TextView(this);
        sub.setText("These sites count toward your daily focus limit.");
        sub.setTextSize(12);
        sub.setTextColor(M3_ON_SURFACE_VARIANT);
        sub.setPadding(0, dp(4), 0, dp(14));
        content.addView(sub);

        // Additional Popular Distractions
        String[] popularDomains = {"reddit.com", "x.com", "linkedin.com", "pinterest.com", "twitch.tv"};
        String[] popularNames = {"Reddit", "X (Twitter)", "LinkedIn", "Pinterest", "Twitch"};

        LinearLayout popularGrid = new LinearLayout(this);
        popularGrid.setOrientation(LinearLayout.VERTICAL);

        for (int i = 0; i < popularDomains.length; i++) {
            final String domain = popularDomains[i];
            final String name = popularNames[i];
            boolean isLim = UsageTracker.isSiteLimited(this, domain);

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(0, dp(4), 0, dp(4));

            TextView rowName = new TextView(this);
            rowName.setText(name + " (" + domain + ")");
            rowName.setTextSize(13);
            rowName.setTextColor(M3_ON_SURFACE);
            row.addView(rowName, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

            TextView btn = new TextView(this);
            btn.setText(isLim ? "✓ Limited" : "+ Add");
            btn.setTextSize(11);
            btn.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            btn.setTextColor(Color.WHITE);
            btn.setPadding(dp(10), dp(5), dp(10), dp(5));
            GradientDrawable bg = new GradientDrawable();
            bg.setColor(isLim ? M3_WARNING : M3_SURFACE_CONTAINER_HIGH);
            bg.setCornerRadius(dp(8));
            btn.setBackground(bg);
            btn.setClickable(true);
            btn.setOnClickListener(v -> {
                boolean nowLim = UsageTracker.toggleSiteLimit(MainActivity.this, domain);
                bg.setColor(nowLim ? M3_WARNING : M3_SURFACE_CONTAINER_HIGH);
                btn.setText(nowLim ? "✓ Limited" : "+ Add");
                Toast.makeText(MainActivity.this, name + (nowLim ? " added to Focus Daily Limit." : " removed from Focus Daily Limit."), Toast.LENGTH_SHORT).show();
            });
            row.addView(btn);
            popularGrid.addView(row);
        }
        content.addView(popularGrid);

        // Custom Domain Input
        LinearLayout inputRow = new LinearLayout(this);
        inputRow.setOrientation(LinearLayout.HORIZONTAL);
        inputRow.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams irParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        irParams.topMargin = dp(14);

        EditText customInput = new EditText(this);
        customInput.setHint("Add site: e.g. netflix.com");
        customInput.setHintTextColor(M3_ON_SURFACE_VARIANT);
        customInput.setTextColor(M3_ON_SURFACE);
        customInput.setTextSize(13);
        customInput.setSingleLine(true);
        customInput.setPadding(dp(10), dp(8), dp(10), dp(8));
        GradientDrawable inBg = new GradientDrawable();
        inBg.setColor(M3_SURFACE_CONTAINER_LOW);
        inBg.setCornerRadius(dp(8));
        inBg.setStroke(dp(1), M3_OUTLINE_VARIANT);
        customInput.setBackground(inBg);
        inputRow.addView(customInput, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

        TextView addCustomBtn = new TextView(this);
        addCustomBtn.setText("Add");
        addCustomBtn.setTextSize(12);
        addCustomBtn.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        addCustomBtn.setTextColor(Color.WHITE);
        addCustomBtn.setPadding(dp(14), dp(8), dp(14), dp(8));
        GradientDrawable addBg = new GradientDrawable();
        addBg.setColor(M3_SECONDARY);
        addBg.setCornerRadius(dp(8));
        addCustomBtn.setBackground(addBg);
        addCustomBtn.setClickable(true);

        inputRow.addView(addCustomBtn, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        LinearLayout.LayoutParams addParams = (LinearLayout.LayoutParams) addCustomBtn.getLayoutParams();
        addParams.leftMargin = dp(8);
        content.addView(inputRow, irParams);

        ScrollView scroll = new ScrollView(this);
        scroll.addView(content);

        AlertDialog dialog = builder.setView(scroll)
                .setPositiveButton("Done", (d, w) -> refreshPolicy())
                .create();

        addCustomBtn.setOnClickListener(v -> {
            String site = customInput.getText().toString().trim();
            if (!TextUtils.isEmpty(site)) {
                UsageTracker.toggleSiteLimit(MainActivity.this, site);
                Toast.makeText(MainActivity.this, site + " added to Focus Daily Limit.", Toast.LENGTH_SHORT).show();
                customInput.setText("");
                dialog.dismiss();
                showFocusSitesDialog();
            }
        });

        dialog.show();
    }

    private void hideKeyboard(View view) {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null && view != null) {
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    private int number(Object value) {
        return value instanceof Number ? ((Number) value).intValue() : 0;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshUsage();
        refreshPolicy();
    }
}
