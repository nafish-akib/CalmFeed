package com.calmfeed;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
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
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends Activity {
    // Theme Colors
    private static final int BG_COLOR = Color.rgb(11, 17, 32); // Deep midnight slate
    private static final int CARD_BG = Color.rgb(24, 34, 53); // Surface
    private static final int CARD_BORDER = Color.rgb(40, 56, 84);
    private static final int TEXT_PRIMARY = Color.rgb(248, 250, 252);
    private static final int TEXT_SECONDARY = Color.rgb(148, 163, 184);
    private static final int ACCENT_EMERALD = Color.rgb(52, 211, 153);
    private static final int ACCENT_CYAN = Color.rgb(56, 189, 248);
    private static final int ACCENT_PURPLE = Color.rgb(168, 85, 247);
    private static final int ACCENT_ORANGE = Color.rgb(251, 146, 60);

    private static final String[] DAILY_QUOTES = {
            "Small choices make more room for the life happening offline.",
            "Your attention is yours to spend. Save a little for yourself.",
            "You do not have to catch up with everything. Start with what matters.",
            "Every pause is a chance to choose your next step.",
            "Time away from the feed is time back in your hands.",
            "Be where your feet are. The moment you are in is worth noticing.",
            "A good day is made one intentional moment at a time."
    };

    private TextView usageValue;
    private TextView usageSubtitle;
    private ProgressBar usageProgressBar;
    private TextView allowanceLabel;
    private TextView allowanceStatusDetail;
    private SeekBar allowanceSeekBar;
    private Button setAllowanceButton;
    private Button requestApprovalButton;
    private Button trustedCircleButton;
    private EditText searchOmnibox;
    private Map<String, Object> latestPolicy = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(BG_COLOR);
        getWindow().setNavigationBarColor(BG_COLOR);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG_COLOR);

        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);
        scrollView.setVerticalScrollBarEnabled(false);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(18), dp(16), dp(18), dp(32));
        scrollView.addView(content);
        root.addView(scrollView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        // 1. Hero Header & Brand
        buildHeroHeader(content);

        // 2. Instant Omnibox Search Bar (Search or type URL)
        buildInstantSearchBar(content);

        // 3. Quick Action Pills (Incognito, History, Bookmarks, Web)
        buildQuickActionPills(content);

        // 4. Speed Dial / Favorite Sites (Grid with YouTube, Reddit, Google, etc.)
        buildSpeedDialGrid(content);

        // 5. Daily Focus & Allowance Control Center
        buildAllowanceCard(content);

        // 6. Today's Screen Time Analytics
        buildUsageCard(content);

        // 7. Site Permissions & Privacy Controls
        buildSitePermissionsCard(content);

        // 8. Mindful Inspiration Card
        buildQuoteCard(content);

        // 8. Privacy Notice
        buildPrivacyFooter(content);

        setContentView(root);
        refreshUsage();
        refreshPolicy();
    }

    private void buildHeroHeader(LinearLayout container) {
        LinearLayout headerRow = new LinearLayout(this);
        headerRow.setOrientation(LinearLayout.HORIZONTAL);
        headerRow.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout titleCol = new LinearLayout(this);
        titleCol.setOrientation(LinearLayout.VERTICAL);

        LinearLayout badgeRow = new LinearLayout(this);
        badgeRow.setOrientation(LinearLayout.HORIZONTAL);
        badgeRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView brandBadge = new TextView(this);
        brandBadge.setText("CALMFEED");
        brandBadge.setTextSize(11);
        brandBadge.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        brandBadge.setTextColor(ACCENT_EMERALD);
        brandBadge.setLetterSpacing(0.12f);
        badgeRow.addView(brandBadge);

        TextView dot = new TextView(this);
        dot.setText(" • ");
        dot.setTextColor(TEXT_SECONDARY);
        dot.setTextSize(10);
        badgeRow.addView(dot);

        TextView modeBadge = new TextView(this);
        modeBadge.setText("MINDFUL BROWSER");
        modeBadge.setTextSize(10);
        modeBadge.setTextColor(ACCENT_CYAN);
        modeBadge.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        modeBadge.setLetterSpacing(0.08f);
        badgeRow.addView(modeBadge);

        titleCol.addView(badgeRow);

        // Dynamic greeting
        int hour = LocalTime.now().getHour();
        String timeGreeting = hour < 12 ? "Good morning" : (hour < 17 ? "Good afternoon" : "Good evening");

        TextView greeting = new TextView(this);
        greeting.setText(timeGreeting + ",\nexplore with peace.");
        greeting.setTextSize(26);
        greeting.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        greeting.setTextColor(TEXT_PRIMARY);
        greeting.setLineSpacing(dp(2), 1.05f);
        LinearLayout.LayoutParams greetingParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        greetingParams.topMargin = dp(4);
        titleCol.addView(greeting, greetingParams);

        TextView dateLabel = new TextView(this);
        dateLabel.setText(LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMMM d", Locale.getDefault())));
        dateLabel.setTextSize(13);
        dateLabel.setTextColor(TEXT_SECONDARY);
        LinearLayout.LayoutParams dateParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dateParams.topMargin = dp(4);
        titleCol.addView(dateLabel, dateParams);

        headerRow.addView(titleCol, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

        // Streak badge pill
        LinearLayout streakPill = new LinearLayout(this);
        streakPill.setOrientation(LinearLayout.HORIZONTAL);
        streakPill.setGravity(Gravity.CENTER_VERTICAL);
        streakPill.setPadding(dp(12), dp(8), dp(12), dp(8));
        GradientDrawable streakBg = new GradientDrawable();
        streakBg.setColor(Color.rgb(30, 41, 59));
        streakBg.setCornerRadius(dp(20));
        streakBg.setStroke(dp(1), Color.rgb(51, 65, 85));
        streakPill.setBackground(streakBg);

        TextView streakIcon = new TextView(this);
        streakIcon.setText("🔥");
        streakIcon.setTextSize(14);
        streakPill.addView(streakIcon);

        TextView streakText = new TextView(this);
        streakText.setText(" Focus");
        streakText.setTextSize(12);
        streakText.setTextColor(Color.rgb(253, 186, 116));
        streakText.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        streakPill.addView(streakText);

        headerRow.addView(streakPill);

        container.addView(headerRow);
    }

    private void buildInstantSearchBar(LinearLayout container) {
        LinearLayout searchContainer = new LinearLayout(this);
        searchContainer.setOrientation(LinearLayout.HORIZONTAL);
        searchContainer.setGravity(Gravity.CENTER_VERTICAL);
        searchContainer.setPadding(dp(14), dp(4), dp(10), dp(4));

        GradientDrawable searchBg = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{Color.rgb(30, 41, 59), Color.rgb(23, 32, 48)});
        searchBg.setCornerRadius(dp(28));
        searchBg.setStroke(dp(1), Color.rgb(56, 189, 248));
        searchContainer.setBackground(searchBg);

        TextView searchIcon = new TextView(this);
        searchIcon.setText("🔍");
        searchIcon.setTextSize(16);
        searchContainer.addView(searchIcon);

        searchOmnibox = new EditText(this);
        searchOmnibox.setHint("Search Google or enter web address...");
        searchOmnibox.setHintTextColor(Color.rgb(148, 163, 184));
        searchOmnibox.setTextColor(TEXT_PRIMARY);
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

        searchContainer.addView(searchOmnibox, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

        // Go arrow button
        TextView goBtn = new TextView(this);
        goBtn.setText("➔");
        goBtn.setTextSize(16);
        goBtn.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        goBtn.setTextColor(Color.WHITE);
        goBtn.setGravity(Gravity.CENTER);
        GradientDrawable goBg = new GradientDrawable();
        goBg.setColor(ACCENT_CYAN);
        goBg.setCornerRadius(dp(20));
        goBtn.setBackground(goBg);
        goBtn.setPadding(dp(10), dp(6), dp(10), dp(6));
        goBtn.setOnClickListener(v -> submitSearch());
        searchContainer.addView(goBtn);

        LinearLayout.LayoutParams searchParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        searchParams.topMargin = dp(18);
        container.addView(searchContainer, searchParams);
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

    private void buildQuickActionPills(LinearLayout container) {
        HorizontalScrollView hsv = new HorizontalScrollView(this);
        hsv.setHorizontalScrollBarEnabled(false);

        LinearLayout pillsRow = new LinearLayout(this);
        pillsRow.setOrientation(LinearLayout.HORIZONTAL);
        pillsRow.setPadding(0, dp(12), 0, dp(4));

        pillsRow.addView(createPill("🕶️ Incognito Tab", ACCENT_PURPLE, v -> {
            Intent intent = new Intent(this, BrowserActivity.class);
            intent.putExtra(BrowserActivity.EXTRA_INCOGNITO, true);
            intent.putExtra(BrowserActivity.EXTRA_URL, "https://www.google.com");
            startActivity(intent);
        }));

        pillsRow.addView(createPill("🕒 History", ACCENT_CYAN, v -> showHistoryDialog()));
        pillsRow.addView(createPill("⭐ Bookmarks", Color.rgb(250, 204, 21), v -> showBookmarksDialog()));
        pillsRow.addView(createPill("⚙️ Permissions", ACCENT_EMERALD, v -> startActivity(new Intent(this, SiteSettingsActivity.class))));
        pillsRow.addView(createPill("🌐 Open Browser", ACCENT_CYAN, v -> {
            Intent intent = new Intent(this, BrowserActivity.class);
            intent.putExtra(BrowserActivity.EXTRA_URL, "https://www.google.com");
            startActivity(intent);
        }));

        hsv.addView(pillsRow);
        container.addView(hsv);
    }

    private View createPill(String label, int accentColor, View.OnClickListener onClick) {
        LinearLayout pill = new LinearLayout(this);
        pill.setOrientation(LinearLayout.HORIZONTAL);
        pill.setGravity(Gravity.CENTER_VERTICAL);
        pill.setPadding(dp(12), dp(8), dp(12), dp(8));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.rgb(24, 34, 53));
        bg.setCornerRadius(dp(16));
        bg.setStroke(dp(1), Color.rgb(40, 56, 84));
        pill.setBackground(bg);
        pill.setClickable(true);
        pill.setFocusable(true);
        pill.setOnClickListener(onClick);

        TextView tv = new TextView(this);
        tv.setText(label);
        tv.setTextSize(12);
        tv.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        tv.setTextColor(TEXT_PRIMARY);
        pill.addView(tv);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.rightMargin = dp(8);
        pill.setLayoutParams(params);
        return pill;
    }

    private void buildSpeedDialGrid(LinearLayout container) {
        TextView sectionTitle = new TextView(this);
        sectionTitle.setText("Top Sites & Feeds");
        sectionTitle.setTextSize(18);
        sectionTitle.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        sectionTitle.setTextColor(TEXT_PRIMARY);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        titleParams.topMargin = dp(20);
        titleParams.bottomMargin = dp(4);
        container.addView(sectionTitle, titleParams);

        TextView sectionSub = new TextView(this);
        sectionSub.setText("Tap to launch with full screen video and mindful filtering");
        sectionSub.setTextSize(12);
        sectionSub.setTextColor(TEXT_SECONDARY);
        container.addView(sectionSub);

        // 4 x 2 Grid of popular destinations
        GridLayout grid = new GridLayout(this);
        grid.setColumnCount(4);
        grid.setAlignmentMode(GridLayout.ALIGN_BOUNDS);
        grid.setUseDefaultMargins(false);

        String[] labels = {"YouTube", "Google", "Facebook", "TikTok", "Reddit", "Instagram", "X / Twitter", "Wikipedia", "BBC News", "+ Custom"};
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

        int itemWidth = (getResources().getDisplayMetrics().widthPixels - dp(56)) / 4;

        for (int i = 0; i < labels.length; i++) {
            final String targetUrl = urls[i];
            final String label = labels[i];

            LinearLayout item = new LinearLayout(this);
            item.setOrientation(LinearLayout.VERTICAL);
            item.setGravity(Gravity.CENTER);
            item.setPadding(dp(4), dp(10), dp(4), dp(10));
            item.setClickable(true);
            item.setFocusable(true);

            // Trademark Logo ImageView
            ImageView iconView = new ImageView(this);
            iconView.setImageResource(logoDrawables[i]);
            iconView.setScaleType(ImageView.ScaleType.FIT_CENTER);
            iconView.setContentDescription(label + " logo");
            item.addView(iconView, new LinearLayout.LayoutParams(dp(48), dp(48)));

            // Label
            TextView labelView = new TextView(this);
            labelView.setText(label);
            labelView.setTextSize(11);
            labelView.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            labelView.setTextColor(TEXT_PRIMARY);
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
            grid.addView(item, gridParam);
        }

        LinearLayout.LayoutParams gridContainerParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        gridContainerParams.topMargin = dp(12);
        container.addView(grid, gridContainerParams);
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
        Intent intent = new Intent(this, BrowserActivity.class);
        intent.putExtra(BrowserActivity.EXTRA_URL, url);
        startActivity(intent);
    }

    private void buildAllowanceCard(LinearLayout container) {
        LinearLayout card = card();
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cardParams.topMargin = dp(18);
        container.addView(card, cardParams);

        // Header
        LinearLayout headerRow = new LinearLayout(this);
        headerRow.setOrientation(LinearLayout.HORIZONTAL);
        headerRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView eyebrow = new TextView(this);
        eyebrow.setText("SOCIAL MEDIA DAILY LIMIT");
        eyebrow.setTextSize(11);
        eyebrow.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        eyebrow.setTextColor(ACCENT_EMERALD);
        eyebrow.setLetterSpacing(0.08f);
        headerRow.addView(eyebrow, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

        TextView icon = new TextView(this);
        icon.setText("⏱️");
        icon.setTextSize(16);
        headerRow.addView(icon);
        card.addView(headerRow);

        allowanceLabel = new TextView(this);
        allowanceLabel.setText("30 minutes daily");
        allowanceLabel.setTextSize(22);
        allowanceLabel.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        allowanceLabel.setTextColor(TEXT_PRIMARY);
        LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        labelParams.topMargin = dp(6);
        card.addView(allowanceLabel, labelParams);

        TextView targetSites = new TextView(this);
        targetSites.setText("Applies to YouTube, Facebook, TikTok, & Instagram.\nGeneral Browsing Mode (search, news, reading) is unlimited.");
        targetSites.setTextSize(12);
        targetSites.setTextColor(TEXT_SECONDARY);
        LinearLayout.LayoutParams tsParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tsParams.topMargin = dp(2);
        card.addView(targetSites, tsParams);

        // Preset chips: 15m, 30m, 45m, 1h, 2h
        LinearLayout presetsRow = new LinearLayout(this);
        presetsRow.setOrientation(LinearLayout.HORIZONTAL);
        presetsRow.setPadding(0, dp(10), 0, dp(6));

        int[] presetMinutes = {15, 30, 45, 60, 120};
        String[] presetLabels = {"15m", "30m", "45m", "1 hr", "2 hr"};
        for (int i = 0; i < presetMinutes.length; i++) {
            final int min = presetMinutes[i];
            TextView chip = new TextView(this);
            chip.setText(presetLabels[i]);
            chip.setTextSize(12);
            chip.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            chip.setTextColor(TEXT_PRIMARY);
            chip.setGravity(Gravity.CENTER);
            chip.setPadding(dp(10), dp(6), dp(10), dp(6));
            GradientDrawable chipBg = new GradientDrawable();
            chipBg.setColor(Color.rgb(30, 41, 59));
            chipBg.setCornerRadius(dp(12));
            chipBg.setStroke(dp(1), Color.rgb(51, 65, 85));
            chip.setBackground(chipBg);
            chip.setClickable(true);
            chip.setOnClickListener(v -> {
                allowanceSeekBar.setProgress(min - 15);
                allowanceLabel.setText(min + " minutes daily");
            });

            LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
            if (i > 0) cp.leftMargin = dp(6);
            presetsRow.addView(chip, cp);
        }
        card.addView(presetsRow);

        // SeekBar
        allowanceSeekBar = new SeekBar(this);
        allowanceSeekBar.setMax(225); // up to 240m (4hr)
        allowanceSeekBar.setProgress(0);
        allowanceSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int minutes = 15 + Math.round(progress / 5f) * 5;
                allowanceLabel.setText(minutes + " minutes daily");
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        LinearLayout.LayoutParams seekParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(34));
        seekParams.topMargin = dp(4);
        card.addView(allowanceSeekBar, seekParams);

        // Status text
        allowanceStatusDetail = new TextView(this);
        allowanceStatusDetail.setText("CalmFeed helps you stay intentional. Time counts down while browsing.");
        allowanceStatusDetail.setTextSize(12);
        allowanceStatusDetail.setTextColor(TEXT_SECONDARY);
        LinearLayout.LayoutParams statusParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        statusParams.topMargin = dp(6);
        card.addView(allowanceStatusDetail, statusParams);

        // Save Allowance Button
        setAllowanceButton = actionButton("Set Today's Allowance", ACCENT_EMERALD, Color.rgb(6, 78, 59));
        setAllowanceButton.setOnClickListener(v -> saveDailyAllowance());
        LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(48));
        btnParams.topMargin = dp(12);
        card.addView(setAllowanceButton, btnParams);

        // Request extra time
        requestApprovalButton = actionButton("Ask Trusted Person for Extra Time", ACCENT_ORANGE, Color.rgb(124, 45, 18));
        requestApprovalButton.setVisibility(View.GONE);
        requestApprovalButton.setOnClickListener(v -> requestExtraTime());
        card.addView(requestApprovalButton, btnParams);

        // Trusted Circle Button
        trustedCircleButton = actionButton("Accountability Partner & Trusted Circle →", TEXT_PRIMARY, Color.rgb(30, 41, 59));
        trustedCircleButton.setOnClickListener(v -> startActivity(new Intent(this, TrustActivity.class)));
        LinearLayout.LayoutParams tcParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(46));
        tcParams.topMargin = dp(8);
        card.addView(trustedCircleButton, tcParams);
    }

    private void buildUsageCard(LinearLayout container) {
        LinearLayout card = card();
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cardParams.topMargin = dp(16);
        container.addView(card, cardParams);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout textCol = new LinearLayout(this);
        textCol.setOrientation(LinearLayout.VERTICAL);

        TextView usageEyebrow = new TextView(this);
        usageEyebrow.setText("TODAY'S USAGE");
        usageEyebrow.setTextSize(11);
        usageEyebrow.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        usageEyebrow.setTextColor(ACCENT_CYAN);
        usageEyebrow.setLetterSpacing(0.08f);
        textCol.addView(usageEyebrow);

        usageValue = new TextView(this);
        usageValue.setText("0 min");
        usageValue.setTextSize(28);
        usageValue.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        usageValue.setTextColor(TEXT_PRIMARY);
        textCol.addView(usageValue);

        row.addView(textCol, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

        TextView statsPill = new TextView(this);
        statsPill.setText("📊 On-Device");
        statsPill.setTextSize(11);
        statsPill.setTextColor(TEXT_SECONDARY);
        statsPill.setPadding(dp(10), dp(6), dp(10), dp(6));
        GradientDrawable pillBg = new GradientDrawable();
        pillBg.setColor(Color.rgb(30, 41, 59));
        pillBg.setCornerRadius(dp(12));
        statsPill.setBackground(pillBg);
        row.addView(statsPill);

        card.addView(row);

        usageProgressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        usageProgressBar.setMax(60);
        usageProgressBar.setProgress(0);
        usageProgressBar.setProgressTintList(android.content.res.ColorStateList.valueOf(ACCENT_EMERALD));
        usageProgressBar.setProgressBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.rgb(30, 41, 59)));
        LinearLayout.LayoutParams progParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(6));
        progParams.topMargin = dp(12);
        card.addView(usageProgressBar, progParams);

        usageSubtitle = new TextView(this);
        usageSubtitle.setText("Time spent browsing today.");
        usageSubtitle.setTextSize(12);
        usageSubtitle.setTextColor(TEXT_SECONDARY);
        LinearLayout.LayoutParams subParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        subParams.topMargin = dp(8);
        card.addView(usageSubtitle, subParams);
    }

    private void buildSitePermissionsCard(LinearLayout container) {
        LinearLayout card = card();
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cardParams.topMargin = dp(16);
        container.addView(card, cardParams);

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);

        TextView eyebrow = new TextView(this);
        eyebrow.setText("PRIVACY & SENSORS");
        eyebrow.setTextSize(11);
        eyebrow.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        eyebrow.setTextColor(ACCENT_EMERALD);
        eyebrow.setLetterSpacing(0.08f);
        header.addView(eyebrow, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

        TextView icon = new TextView(this);
        icon.setText("🛡️");
        icon.setTextSize(16);
        header.addView(icon);
        card.addView(header);

        TextView title = new TextView(this);
        title.setText("Site Permissions Control");
        title.setTextSize(18);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setTextColor(TEXT_PRIMARY);
        LinearLayout.LayoutParams tParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tParams.topMargin = dp(4);
        card.addView(title, tParams);

        TextView sub = new TextView(this);
        sub.setText("Toggle location, camera, and microphone access per website with custom rules and global defaults.");
        sub.setTextSize(12);
        sub.setTextColor(TEXT_SECONDARY);
        LinearLayout.LayoutParams sParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        sParams.topMargin = dp(4);
        card.addView(sub, sParams);

        // Preview status chips row
        LinearLayout chipsRow = new LinearLayout(this);
        chipsRow.setOrientation(LinearLayout.HORIZONTAL);
        chipsRow.setPadding(0, dp(10), 0, dp(4));

        chipsRow.addView(createPermPill("📍 Location"));
        chipsRow.addView(createPermPill("📷 Camera"));
        chipsRow.addView(createPermPill("🎙️ Microphone"));
        card.addView(chipsRow);

        Button manageBtn = actionButton("Manage Site Permissions →", TEXT_PRIMARY, Color.rgb(30, 41, 59));
        manageBtn.setOnClickListener(v -> startActivity(new Intent(this, SiteSettingsActivity.class)));
        LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(46));
        btnParams.topMargin = dp(10);
        card.addView(manageBtn, btnParams);
    }

    private View createPermPill(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextSize(11);
        tv.setTextColor(ACCENT_CYAN);
        tv.setPadding(dp(8), dp(4), dp(8), dp(4));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.rgb(30, 41, 59));
        bg.setCornerRadius(dp(10));
        tv.setBackground(bg);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.rightMargin = dp(6);
        tv.setLayoutParams(lp);
        return tv;
    }

    private void buildQuoteCard(LinearLayout container) {
        LinearLayout card = card();
        GradientDrawable quoteBg = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{Color.rgb(30, 36, 56), Color.rgb(22, 28, 44)});
        quoteBg.setCornerRadius(dp(20));
        quoteBg.setStroke(dp(1), Color.rgb(49, 58, 86));
        card.setBackground(quoteBg);

        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cardParams.topMargin = dp(16);
        container.addView(card, cardParams);

        TextView quoteEyebrow = new TextView(this);
        quoteEyebrow.setText("DAILY MINDFUL WISDOM");
        quoteEyebrow.setTextSize(11);
        quoteEyebrow.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        quoteEyebrow.setTextColor(ACCENT_PURPLE);
        quoteEyebrow.setLetterSpacing(0.08f);
        card.addView(quoteEyebrow);

        TextView quoteText = new TextView(this);
        quoteText.setText("“" + dailyQuote() + "”");
        quoteText.setTextSize(15);
        quoteText.setTextColor(TEXT_PRIMARY);
        quoteText.setLineSpacing(dp(3), 1.1f);
        LinearLayout.LayoutParams qParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        qParams.topMargin = dp(8);
        card.addView(quoteText, qParams);

        TextView sig = new TextView(this);
        sig.setText("— CalmFeed Reflection");
        sig.setTextSize(11);
        sig.setTextColor(TEXT_SECONDARY);
        sig.setGravity(Gravity.END);
        LinearLayout.LayoutParams sigParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        sigParams.topMargin = dp(8);
        card.addView(sig, sigParams);
    }

    private void buildPrivacyFooter(LinearLayout container) {
        TextView privacy = new TextView(this);
        privacy.setText("🔒 Zero password sharing. Browsing data & timer stay on your phone.");
        privacy.setTextSize(12);
        privacy.setTextColor(TEXT_SECONDARY);
        privacy.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams pParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        pParams.topMargin = dp(24);
        container.addView(privacy, pParams);
    }

    private LinearLayout card() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(18), dp(18), dp(18), dp(18));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(CARD_BG);
        bg.setCornerRadius(dp(20));
        bg.setStroke(dp(1), CARD_BORDER);
        card.setBackground(bg);
        return card;
    }

    private Button actionButton(String text, int textColor, int bgColor) {
        Button btn = new Button(this);
        btn.setText(text);
        btn.setAllCaps(false);
        btn.setTextSize(14);
        btn.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        btn.setTextColor(textColor);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(bgColor);
        bg.setCornerRadius(dp(14));
        btn.setBackground(bg);
        return btn;
    }

    private String dailyQuote() {
        int day = LocalDate.now().getDayOfYear();
        int index = Math.floorMod(day * 31 + LocalDate.now().getYear(), DAILY_QUOTES.length);
        return DAILY_QUOTES[index];
    }

    private void refreshUsage() {
        long seconds = UsageTracker.getTodaySeconds(this);
        long minutes = seconds / 60;
        usageValue.setText(String.format(Locale.getDefault(), "%d min", minutes));
        usageProgressBar.setProgress((int) Math.min(minutes, usageProgressBar.getMax()));
        if (minutes >= 60) {
            usageSubtitle.setText(String.format(Locale.getDefault(), "%d hr %02d min in CalmFeed today.", minutes / 60, minutes % 60));
        } else if (minutes == 1) {
            usageSubtitle.setText("1 minute in CalmFeed today.");
        } else {
            usageSubtitle.setText(String.format(Locale.getDefault(), "%d minutes in CalmFeed today.", minutes));
        }
    }

    private void saveDailyAllowance() {
        int minutes = 15 + Math.round(allowanceSeekBar.getProgress() / 5f) * 5;
        UsageTracker.setTodayAllowance(this, minutes);

        if (FirebaseBridge.isConfigured(this)) {
            Map<String, Object> data = new HashMap<>();
            data.put("minutes", minutes);
            data.put("timeZone", FirebaseBridge.timezoneId());
            setAllowanceButton.setEnabled(false);
            FirebaseBridge.call(this, "setDailyAllowance", data, (result, error) -> {
                if (isFinishing()) return;
                setAllowanceButton.setEnabled(true);
                Toast.makeText(this, "Today's " + minutes + "-minute allowance set.", Toast.LENGTH_SHORT).show();
                refreshPolicy();
            });
        } else {
            Toast.makeText(this, "Today's " + minutes + "-minute allowance set.", Toast.LENGTH_SHORT).show();
            renderOnDevicePolicy(minutes);
        }
    }

    private void requestExtraTime() {
        if (!FirebaseBridge.isConfigured(this)) {
            Toast.makeText(this, "Connect Firebase to enable trusted person approvals.", Toast.LENGTH_LONG).show();
            return;
        }
        if (latestPolicy.get("trustedApprover") == null) {
            startActivity(new Intent(this, TrustActivity.class));
            return;
        }
        requestApprovalButton.setEnabled(false);
        Map<String, Object> data = new HashMap<>();
        data.put("timeZone", FirebaseBridge.timezoneId());
        FirebaseBridge.call(this, "requestExtraTime", data, (result, error) -> {
            if (isFinishing()) return;
            requestApprovalButton.setEnabled(true);
            if (error != null) {
                Toast.makeText(this, error, Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "Request sent. Your trusted person can approve extra time.", Toast.LENGTH_LONG).show();
            }
            refreshPolicy();
        });
    }

    private void refreshPolicy() {
        if (!FirebaseBridge.isConfigured(this)) {
            int onDeviceMin = UsageTracker.getTodayAllowanceMinutes(this);
            renderOnDevicePolicy(onDeviceMin);
            return;
        }

        Map<String, Object> data = new HashMap<>();
        data.put("timeZone", FirebaseBridge.timezoneId());
        FirebaseBridge.call(this, "getPolicy", data, (result, error) -> {
            if (isFinishing()) return;
            if (error != null) {
                int onDeviceMin = UsageTracker.getTodayAllowanceMinutes(this);
                renderOnDevicePolicy(onDeviceMin);
                return;
            }
            latestPolicy = result;
            renderRemotePolicy();
        });
    }

    private void renderOnDevicePolicy(int minutes) {
        if (minutes > 0) {
            long remainingSec = UsageTracker.getDailyRemainingSeconds(this);
            long minutesLeft = (remainingSec + 59) / 60;
            allowanceStatusDetail.setText(String.format(Locale.getDefault(), "Social sites: %d min daily limit · %d min remaining\n(YouTube, Facebook, TikTok, Instagram) · General browsing is unlimited", minutes, minutesLeft));
            allowanceLabel.setText(minutes + " minutes daily");
            setAllowanceButton.setText("Update Social Limit (" + minutes + "m)");
        } else {
            allowanceStatusDetail.setText("Set a daily limit for social media. General Browsing Mode remains unlimited.");
            setAllowanceButton.setText("Set Social Daily Limit");
        }
        requestApprovalButton.setVisibility(View.GONE);
    }

    private void renderRemotePolicy() {
        int dailyMinutes = number(latestPolicy.get("allowanceMinutes"));
        int dailyRemaining = number(latestPolicy.get("remainingSeconds"));
        int extraRemaining = number(latestPolicy.get("extraSeconds"));
        int minutesLeft = (dailyRemaining + extraRemaining + 59) / 60;
        Object approver = latestPolicy.get("trustedApprover");

        if (dailyMinutes > 0) {
            allowanceStatusDetail.setText(String.format(Locale.getDefault(), "Today: %d-minute allowance · %d min remaining", dailyMinutes, minutesLeft));
            allowanceLabel.setText(dailyMinutes + " minutes daily");
            setAllowanceButton.setText("Update Allowance (" + dailyMinutes + "m)");
        }

        if (approver instanceof Map) {
            Object name = ((Map<?, ?>) approver).get("name");
            trustedCircleButton.setText("Trusted Person · " + (name instanceof String ? name : "Connected"));
        } else {
            trustedCircleButton.setText("Accountability Partner & Trusted Circle →");
        }

        if (dailyMinutes > 0 && dailyRemaining <= 0 && extraRemaining <= 0) {
            requestApprovalButton.setVisibility(View.VISIBLE);
        } else {
            requestApprovalButton.setVisibility(View.GONE);
        }
    }

    private void showHistoryDialog() {
        List<BrowserHistoryManager.HistoryItem> history = BrowserHistoryManager.getAll(this);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setBackgroundColor(CARD_BG);
        content.setPadding(dp(16), dp(16), dp(16), dp(16));

        TextView title = new TextView(this);
        title.setText("Browsing History (" + history.size() + ")");
        title.setTextSize(18);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setTextColor(TEXT_PRIMARY);
        content.addView(title);

        if (history.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("No browsing history recorded yet.");
            empty.setTextColor(TEXT_SECONDARY);
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
                itemTitle.setTextColor(TEXT_PRIMARY);
                itemTitle.setTextSize(14);
                itemTitle.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
                row.addView(itemTitle);

                TextView itemUrl = new TextView(MainActivity.this);
                itemUrl.setText(item.url + " · " + item.formattedTime);
                itemUrl.setTextColor(TEXT_SECONDARY);
                itemUrl.setTextSize(12);
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
        content.setBackgroundColor(CARD_BG);
        content.setPadding(dp(16), dp(16), dp(16), dp(16));

        TextView title = new TextView(this);
        title.setText("Bookmarks (" + bookmarks.size() + ")");
        title.setTextSize(18);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setTextColor(TEXT_PRIMARY);
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
                itemTitle.setTextColor(TEXT_PRIMARY);
                itemTitle.setTextSize(14);
                itemTitle.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
                row.addView(itemTitle);

                TextView itemUrl = new TextView(MainActivity.this);
                itemUrl.setText(item.url);
                itemUrl.setTextColor(TEXT_SECONDARY);
                itemUrl.setTextSize(12);
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
