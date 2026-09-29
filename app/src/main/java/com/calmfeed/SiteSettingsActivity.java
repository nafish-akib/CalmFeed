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
import android.provider.Settings;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SiteSettingsActivity extends Activity {
    // Theme Colors
    private static final int BG_COLOR = Color.rgb(11, 17, 32);       // Deep Slate
    private static final int CARD_BG = Color.rgb(24, 34, 53);        // Surface
    private static final int CARD_BORDER = Color.rgb(40, 56, 84);
    private static final int TEXT_PRIMARY = Color.rgb(248, 250, 252);
    private static final int TEXT_SECONDARY = Color.rgb(148, 163, 184);
    private static final int ACCENT_EMERALD = Color.rgb(52, 211, 153);
    private static final int ACCENT_CYAN = Color.rgb(56, 189, 248);
    private static final int ACCENT_RED = Color.rgb(248, 113, 113);
    private static final int ACCENT_AMBER = Color.rgb(251, 191, 36);

    private LinearLayout sitesContainer;
    private TextView sitesCountBadge;
    private EditText searchInput;
    private String currentSearchFilter = "";
    private final Set<String> expandedSites = new HashSet<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(BG_COLOR);
        getWindow().setNavigationBarColor(BG_COLOR);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG_COLOR);

        // 1. App Bar
        buildAppBar(root);

        // 2. Scrollable Content
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setVerticalScrollBarEnabled(false);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(16), dp(12), dp(16), dp(32));
        scroll.addView(content);
        root.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        // 3. Search & Quick Add bar
        buildSearchBar(content);

        // 4. Global Default Permissions Card
        buildGlobalDefaultsCard(content);

        // 5. Configured Sites Card
        buildSitesCard(content);

        // 6. Device Hardware Permissions Status
        buildSystemPermissionsCard(content);

        setContentView(root);
        refreshSitesList();
    }

    private void buildAppBar(LinearLayout root) {
        LinearLayout appBar = new LinearLayout(this);
        appBar.setOrientation(LinearLayout.HORIZONTAL);
        appBar.setGravity(Gravity.CENTER_VERTICAL);
        appBar.setPadding(dp(12), dp(12), dp(12), dp(12));
        appBar.setBackgroundColor(BG_COLOR);

        // Back button
        TextView backBtn = new TextView(this);
        backBtn.setText("←");
        backBtn.setTextSize(22);
        backBtn.setTextColor(TEXT_PRIMARY);
        backBtn.setPadding(dp(8), dp(4), dp(12), dp(4));
        backBtn.setClickable(true);
        backBtn.setOnClickListener(v -> finish());
        appBar.addView(backBtn);

        LinearLayout titleCol = new LinearLayout(this);
        titleCol.setOrientation(LinearLayout.VERTICAL);

        TextView title = new TextView(this);
        title.setText("Site Permissions");
        title.setTextSize(18);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setTextColor(TEXT_PRIMARY);
        titleCol.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("Location, Camera & Microphone Access");
        subtitle.setTextSize(11);
        subtitle.setTextColor(TEXT_SECONDARY);
        titleCol.addView(subtitle);

        appBar.addView(titleCol, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

        // Reset All button
        TextView resetBtn = new TextView(this);
        resetBtn.setText("Reset All");
        resetBtn.setTextSize(12);
        resetBtn.setTextColor(ACCENT_RED);
        resetBtn.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        resetBtn.setPadding(dp(10), dp(6), dp(10), dp(6));
        GradientDrawable rBg = new GradientDrawable();
        rBg.setColor(Color.rgb(30, 41, 59));
        rBg.setCornerRadius(dp(12));
        resetBtn.setBackground(rBg);
        resetBtn.setClickable(true);
        resetBtn.setOnClickListener(v -> confirmResetAll());
        appBar.addView(resetBtn);

        root.addView(appBar);
    }

    private void buildSearchBar(LinearLayout container) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(12), dp(4), dp(8), dp(4));
        GradientDrawable sBg = new GradientDrawable();
        sBg.setColor(CARD_BG);
        sBg.setCornerRadius(dp(16));
        sBg.setStroke(dp(1), CARD_BORDER);
        row.setBackground(sBg);

        TextView icon = new TextView(this);
        icon.setText("🔍");
        icon.setTextSize(14);
        row.addView(icon);

        searchInput = new EditText(this);
        searchInput.setHint("Filter sites by domain...");
        searchInput.setHintTextColor(TEXT_SECONDARY);
        searchInput.setTextColor(TEXT_PRIMARY);
        searchInput.setTextSize(13);
        searchInput.setBackground(null);
        searchInput.setSingleLine(true);
        searchInput.setPadding(dp(8), dp(10), dp(8), dp(10));
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearchFilter = s.toString().trim().toLowerCase();
                refreshSitesList();
            }
            @Override public void afterTextChanged(Editable s) {}
        });
        row.addView(searchInput, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

        // Add site button
        TextView addSiteBtn = new TextView(this);
        addSiteBtn.setText("＋ Add Site");
        addSiteBtn.setTextSize(12);
        addSiteBtn.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        addSiteBtn.setTextColor(Color.WHITE);
        GradientDrawable addBg = new GradientDrawable();
        addBg.setColor(ACCENT_CYAN);
        addBg.setCornerRadius(dp(12));
        addSiteBtn.setBackground(addBg);
        addSiteBtn.setPadding(dp(10), dp(6), dp(10), dp(6));
        addSiteBtn.setClickable(true);
        addSiteBtn.setOnClickListener(v -> promptAddNewSite());
        row.addView(addSiteBtn);

        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rowParams.bottomMargin = dp(14);
        container.addView(row, rowParams);
    }

    private void buildGlobalDefaultsCard(LinearLayout container) {
        LinearLayout card = card();
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cardParams.bottomMargin = dp(16);
        container.addView(card, cardParams);

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);

        TextView title = new TextView(this);
        title.setText("Global Permission Defaults");
        title.setTextSize(15);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setTextColor(TEXT_PRIMARY);
        header.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

        TextView icon = new TextView(this);
        icon.setText("🛡️");
        icon.setTextSize(16);
        header.addView(icon);
        card.addView(header);

        TextView sub = new TextView(this);
        sub.setText("Applies to any site not explicitly configured below:");
        sub.setTextSize(12);
        sub.setTextColor(TEXT_SECONDARY);
        LinearLayout.LayoutParams subParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        subParams.topMargin = dp(4);
        subParams.bottomMargin = dp(12);
        card.addView(sub, subParams);

        // 4 Global Rows
        card.addView(createGlobalPermRow("📍 Location Access", SitePermissionManager.PERM_LOCATION));
        card.addView(createGlobalPermRow("📷 Camera Access", SitePermissionManager.PERM_CAMERA));
        card.addView(createGlobalPermRow("🎙️ Microphone Access", SitePermissionManager.PERM_MICROPHONE));
        card.addView(createGlobalPermRow("🔔 Notifications", SitePermissionManager.PERM_NOTIFICATIONS));
    }

    private View createGlobalPermRow(String label, String permType) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(8), 0, dp(8));

        TextView tv = new TextView(this);
        tv.setText(label);
        tv.setTextSize(13);
        tv.setTextColor(TEXT_PRIMARY);
        row.addView(tv, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

        int currentState = SitePermissionManager.getGlobalDefault(this, permType);

        LinearLayout selector = new LinearLayout(this);
        selector.setOrientation(LinearLayout.HORIZONTAL);

        selector.addView(createSegmentButton("Ask", currentState == SitePermissionManager.STATE_ASK, v -> {
            SitePermissionManager.setGlobalDefault(this, permType, SitePermissionManager.STATE_ASK);
            Toast.makeText(this, label + ": Default set to Ask", Toast.LENGTH_SHORT).show();
            recreateLayout();
        }));
        selector.addView(createSegmentButton("Allow", currentState == SitePermissionManager.STATE_ALLOW, v -> {
            SitePermissionManager.setGlobalDefault(this, permType, SitePermissionManager.STATE_ALLOW);
            Toast.makeText(this, label + ": Default set to Allow", Toast.LENGTH_SHORT).show();
            recreateLayout();
        }));
        selector.addView(createSegmentButton("Block", currentState == SitePermissionManager.STATE_BLOCK, v -> {
            SitePermissionManager.setGlobalDefault(this, permType, SitePermissionManager.STATE_BLOCK);
            Toast.makeText(this, label + ": Default set to Block", Toast.LENGTH_SHORT).show();
            recreateLayout();
        }));

        row.addView(selector);
        return row;
    }

    private View createSegmentButton(String text, boolean isSelected, View.OnClickListener onClick) {
        TextView btn = new TextView(this);
        btn.setText(text);
        btn.setTextSize(11);
        btn.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        btn.setTextColor(isSelected ? Color.WHITE : TEXT_SECONDARY);
        btn.setPadding(dp(8), dp(4), dp(8), dp(4));
        btn.setGravity(Gravity.CENTER);
        GradientDrawable bg = new GradientDrawable();
        if (isSelected) {
            bg.setColor("Block".equals(text) ? ACCENT_RED : ("Allow".equals(text) ? ACCENT_EMERALD : ACCENT_CYAN));
        } else {
            bg.setColor(Color.rgb(30, 41, 59));
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

    private void buildSitesCard(LinearLayout container) {
        LinearLayout card = card();
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cardParams.bottomMargin = dp(16);
        container.addView(card, cardParams);

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);

        TextView title = new TextView(this);
        title.setText("Configured Websites");
        title.setTextSize(15);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setTextColor(TEXT_PRIMARY);
        header.addView(title);

        sitesCountBadge = new TextView(this);
        sitesCountBadge.setTextSize(11);
        sitesCountBadge.setTextColor(ACCENT_EMERALD);
        sitesCountBadge.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        sitesCountBadge.setPadding(dp(8), dp(2), dp(8), dp(2));
        GradientDrawable badgeBg = new GradientDrawable();
        badgeBg.setColor(Color.rgb(6, 78, 59));
        badgeBg.setCornerRadius(dp(10));
        sitesCountBadge.setBackground(badgeBg);
        LinearLayout.LayoutParams badgeParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        badgeParams.leftMargin = dp(8);
        header.addView(sitesCountBadge, badgeParams);

        View spacer = new View(this);
        header.addView(spacer, new LinearLayout.LayoutParams(0, 1, 1.0f));

        card.addView(header);

        TextView sub = new TextView(this);
        sub.setText("Tap any website to expand and toggle its permissions live:");
        sub.setTextSize(12);
        sub.setTextColor(TEXT_SECONDARY);
        LinearLayout.LayoutParams subParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        subParams.topMargin = dp(4);
        subParams.bottomMargin = dp(12);
        card.addView(sub, subParams);

        sitesContainer = new LinearLayout(this);
        sitesContainer.setOrientation(LinearLayout.VERTICAL);
        card.addView(sitesContainer);
    }

    private void refreshSitesList() {
        if (sitesContainer == null) return;
        sitesContainer.removeAllViews();

        List<SitePermissionManager.SiteRule> rules = SitePermissionManager.getAllSiteRules(this);
        int matchCount = 0;

        for (SitePermissionManager.SiteRule rule : rules) {
            if (!TextUtils.isEmpty(currentSearchFilter) && !rule.host.contains(currentSearchFilter)) {
                continue;
            }
            matchCount++;
            sitesContainer.addView(createSiteCard(rule));
        }

        if (sitesCountBadge != null) {
            sitesCountBadge.setText(String.valueOf(rules.size()));
        }

        if (matchCount == 0) {
            TextView empty = new TextView(this);
            empty.setText(TextUtils.isEmpty(currentSearchFilter) ? "No site rules saved yet." : "No websites matching '" + currentSearchFilter + "'");
            empty.setTextSize(13);
            empty.setTextColor(TEXT_SECONDARY);
            empty.setPadding(dp(4), dp(16), dp(4), dp(16));
            sitesContainer.addView(empty);
        }
    }

    private View createSiteCard(SitePermissionManager.SiteRule rule) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(12), dp(12), dp(12), dp(12));
        GradientDrawable boxBg = new GradientDrawable();
        boxBg.setColor(Color.rgb(30, 41, 59));
        boxBg.setCornerRadius(dp(16));
        boxBg.setStroke(dp(1), Color.rgb(51, 65, 85));
        box.setBackground(boxBg);

        LinearLayout.LayoutParams boxParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        boxParams.bottomMargin = dp(10);
        box.setLayoutParams(boxParams);

        // Header Row: Favicon initial, Host, Status pills, Expand arrow, Delete
        LinearLayout topRow = new LinearLayout(this);
        topRow.setOrientation(LinearLayout.HORIZONTAL);
        topRow.setGravity(Gravity.CENTER_VERTICAL);

        int logoRes = getLogoDrawableForHost(rule.host);
        if (logoRes != 0) {
            ImageView logoView = new ImageView(this);
            logoView.setImageResource(logoRes);
            logoView.setScaleType(ImageView.ScaleType.FIT_CENTER);
            logoView.setContentDescription(rule.host + " logo");
            topRow.addView(logoView, new LinearLayout.LayoutParams(dp(32), dp(32)));
        } else {
            TextView icon = new TextView(this);
            String initial = rule.host.substring(0, Math.min(2, rule.host.length())).toUpperCase();
            icon.setText(initial);
            icon.setTextSize(12);
            icon.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            icon.setTextColor(Color.WHITE);
            icon.setGravity(Gravity.CENTER);
            GradientDrawable iconBg = new GradientDrawable();
            iconBg.setShape(GradientDrawable.OVAL);
            iconBg.setColor(getColorForHost(rule.host));
            icon.setBackground(iconBg);
            topRow.addView(icon, new LinearLayout.LayoutParams(dp(32), dp(32)));
        }

        LinearLayout hostCol = new LinearLayout(this);
        hostCol.setOrientation(LinearLayout.VERTICAL);
        hostCol.setPadding(dp(10), 0, dp(8), 0);

        TextView hostTv = new TextView(this);
        hostTv.setText(rule.host);
        hostTv.setTextSize(14);
        hostTv.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        hostTv.setTextColor(TEXT_PRIMARY);
        hostCol.addView(hostTv);

        // Summary chips row (e.g. 📍 Allow · 📷 Block · 🎙️ Allow)
        TextView summary = new TextView(this);
        summary.setText(getSummaryText(rule));
        summary.setTextSize(11);
        summary.setTextColor(TEXT_SECONDARY);
        hostCol.addView(summary);

        topRow.addView(hostCol, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

        boolean isExpanded = expandedSites.contains(rule.host);

        // Delete button (🗑️)
        TextView deleteBtn = new TextView(this);
        deleteBtn.setText("✕");
        deleteBtn.setTextSize(14);
        deleteBtn.setTextColor(TEXT_SECONDARY);
        deleteBtn.setPadding(dp(8), dp(4), dp(8), dp(4));
        deleteBtn.setClickable(true);
        deleteBtn.setOnClickListener(v -> {
            SitePermissionManager.deleteSiteRule(this, rule.host);
            expandedSites.remove(rule.host);
            Toast.makeText(this, rule.host + " rule deleted", Toast.LENGTH_SHORT).show();
            refreshSitesList();
        });
        topRow.addView(deleteBtn);

        // Expand chevron (⌄ / ⌃)
        TextView expandBtn = new TextView(this);
        expandBtn.setText(isExpanded ? "▲" : "▼");
        expandBtn.setTextSize(12);
        expandBtn.setTextColor(ACCENT_CYAN);
        expandBtn.setPadding(dp(6), dp(4), dp(4), dp(4));
        topRow.addView(expandBtn);

        topRow.setClickable(true);
        topRow.setOnClickListener(v -> {
            if (expandedSites.contains(rule.host)) {
                expandedSites.remove(rule.host);
            } else {
                expandedSites.add(rule.host);
            }
            refreshSitesList();
        });

        box.addView(topRow);

        // Detailed toggles when expanded
        if (isExpanded) {
            View divider = new View(this);
            divider.setBackgroundColor(Color.rgb(51, 65, 85));
            LinearLayout.LayoutParams divParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(1));
            divParams.topMargin = dp(10);
            divParams.bottomMargin = dp(10);
            box.addView(divider, divParams);

            box.addView(createSiteToggleRow(rule.host, "📍 Location Access", SitePermissionManager.PERM_LOCATION, rule.locationState));
            box.addView(createSiteToggleRow(rule.host, "📷 Camera Access", SitePermissionManager.PERM_CAMERA, rule.cameraState));
            box.addView(createSiteToggleRow(rule.host, "🎙️ Microphone Access", SitePermissionManager.PERM_MICROPHONE, rule.micState));
            box.addView(createSiteToggleRow(rule.host, "🔔 Notifications", SitePermissionManager.PERM_NOTIFICATIONS, rule.notificationState));
        }

        return box;
    }

    private View createSiteToggleRow(String host, String label, String permType, int currentState) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(6), 0, dp(6));

        TextView tv = new TextView(this);
        tv.setText(label);
        tv.setTextSize(13);
        tv.setTextColor(TEXT_PRIMARY);
        row.addView(tv, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

        LinearLayout selector = new LinearLayout(this);
        selector.setOrientation(LinearLayout.HORIZONTAL);

        selector.addView(createSegmentButton("Ask", currentState == SitePermissionManager.STATE_ASK, v -> {
            SitePermissionManager.setSitePermission(this, host, permType, SitePermissionManager.STATE_ASK);
            Toast.makeText(this, host + ": " + label + " → Ask", Toast.LENGTH_SHORT).show();
            refreshSitesList();
        }));

        selector.addView(createSegmentButton("Allow", currentState == SitePermissionManager.STATE_ALLOW, v -> {
            SitePermissionManager.setSitePermission(this, host, permType, SitePermissionManager.STATE_ALLOW);
            Toast.makeText(this, host + ": " + label + " → Allow", Toast.LENGTH_SHORT).show();
            refreshSitesList();
        }));

        selector.addView(createSegmentButton("Block", currentState == SitePermissionManager.STATE_BLOCK, v -> {
            SitePermissionManager.setSitePermission(this, host, permType, SitePermissionManager.STATE_BLOCK);
            Toast.makeText(this, host + ": " + label + " → Block", Toast.LENGTH_SHORT).show();
            refreshSitesList();
        }));

        row.addView(selector);
        return row;
    }

    private String getSummaryText(SitePermissionManager.SiteRule rule) {
        StringBuilder sb = new StringBuilder();
        sb.append("📍 ").append(rule.locationState == 1 ? "Allow" : (rule.locationState == 2 ? "Block" : "Ask"));
        sb.append(" · ");
        sb.append("📷 ").append(rule.cameraState == 1 ? "Allow" : (rule.cameraState == 2 ? "Block" : "Ask"));
        sb.append(" · ");
        sb.append("🎙️ ").append(rule.micState == 1 ? "Allow" : (rule.micState == 2 ? "Block" : "Ask"));
        return sb.toString();
    }

    private void buildSystemPermissionsCard(LinearLayout container) {
        LinearLayout card = card();
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cardParams.bottomMargin = dp(16);
        container.addView(card, cardParams);

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);

        TextView title = new TextView(this);
        title.setText("Device System Permissions");
        title.setTextSize(15);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setTextColor(TEXT_PRIMARY);
        header.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

        TextView icon = new TextView(this);
        icon.setText("📱");
        icon.setTextSize(16);
        header.addView(icon);
        card.addView(header);

        TextView sub = new TextView(this);
        sub.setText("System-level permission status on this Android device:");
        sub.setTextSize(12);
        sub.setTextColor(TEXT_SECONDARY);
        LinearLayout.LayoutParams subParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        subParams.topMargin = dp(4);
        subParams.bottomMargin = dp(12);
        card.addView(sub, subParams);

        card.addView(createSystemStatusRow("Location Hardware", checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED));
        card.addView(createSystemStatusRow("Camera Hardware", checkSelfPermission(android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED));
        card.addView(createSystemStatusRow("Microphone Hardware", checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED));

        Button appSettingsBtn = new Button(this);
        appSettingsBtn.setText("Open Android System Settings");
        appSettingsBtn.setAllCaps(false);
        appSettingsBtn.setTextSize(13);
        appSettingsBtn.setTextColor(TEXT_PRIMARY);
        GradientDrawable btnBg = new GradientDrawable();
        btnBg.setColor(Color.rgb(30, 41, 59));
        btnBg.setCornerRadius(dp(12));
        appSettingsBtn.setBackground(btnBg);
        appSettingsBtn.setOnClickListener(v -> {
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.fromParts("package", getPackageName(), null));
            startActivity(intent);
        });

        LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(44));
        btnParams.topMargin = dp(12);
        card.addView(appSettingsBtn, btnParams);
    }

    private View createSystemStatusRow(String label, boolean isGranted) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(6), 0, dp(6));

        TextView tv = new TextView(this);
        tv.setText(label);
        tv.setTextSize(13);
        tv.setTextColor(TEXT_PRIMARY);
        row.addView(tv, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

        TextView status = new TextView(this);
        status.setText(isGranted ? "✓ Allowed by OS" : "⚠️ Needs OS Permission");
        status.setTextSize(11);
        status.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        status.setTextColor(isGranted ? ACCENT_EMERALD : ACCENT_AMBER);
        status.setPadding(dp(8), dp(4), dp(8), dp(4));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(isGranted ? Color.rgb(6, 78, 59) : Color.rgb(120, 53, 15));
        bg.setCornerRadius(dp(8));
        status.setBackground(bg);
        row.addView(status);

        return row;
    }

    private void promptAddNewSite() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add Website Rule");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(20), dp(12), dp(20), dp(12));

        TextView promptTv = new TextView(this);
        promptTv.setText("Enter the domain name (e.g. zoom.us or github.com):");
        promptTv.setTextSize(13);
        promptTv.setTextColor(Color.WHITE);
        promptTv.setPadding(0, 0, 0, dp(8));
        layout.addView(promptTv);

        final EditText domainInput = new EditText(this);
        domainInput.setHint("e.g. discord.com");
        domainInput.setSingleLine(true);
        domainInput.setPadding(dp(12), dp(12), dp(12), dp(12));
        layout.addView(domainInput);

        builder.setView(layout);
        builder.setPositiveButton("Add", (dialog, which) -> {
            String domain = domainInput.getText().toString().trim();
            if (!TextUtils.isEmpty(domain)) {
                String clean = SitePermissionManager.cleanHost(domain);
                // Initialize with global defaults
                SitePermissionManager.setSitePermission(this, clean, SitePermissionManager.PERM_LOCATION, SitePermissionManager.getGlobalDefault(this, SitePermissionManager.PERM_LOCATION));
                expandedSites.add(clean);
                refreshSitesList();
                Toast.makeText(this, "Added rule for " + clean, Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void confirmResetAll() {
        new AlertDialog.Builder(this)
                .setTitle("Reset Site Permissions")
                .setMessage("Reset all custom website permissions back to default settings?")
                .setPositiveButton("Reset All", (dialog, which) -> {
                    SitePermissionManager.resetAllRules(this);
                    expandedSites.clear();
                    Toast.makeText(this, "All site permissions reset to defaults", Toast.LENGTH_SHORT).show();
                    recreateLayout();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void recreateLayout() {
        recreate();
    }

    private LinearLayout card() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(16), dp(16), dp(16));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(CARD_BG);
        bg.setCornerRadius(dp(20));
        bg.setStroke(dp(1), CARD_BORDER);
        card.setBackground(bg);
        return card;
    }

    private int getColorForHost(String host) {
        int hash = Math.abs(host.hashCode());
        int[] palette = {
                Color.rgb(239, 68, 68), Color.rgb(59, 130, 246), Color.rgb(16, 185, 129),
                Color.rgb(249, 115, 22), Color.rgb(168, 85, 247), Color.rgb(236, 72, 153),
                Color.rgb(20, 184, 166), Color.rgb(99, 102, 241)
        };
        return palette[hash % palette.length];
    }

    private int getLogoDrawableForHost(String host) {
        if (host == null) return 0;
        String h = host.toLowerCase();
        if (h.contains("youtube")) return R.drawable.ic_logo_youtube;
        if (h.contains("google") || h.contains("maps") || h.contains("meet")) return R.drawable.ic_logo_google;
        if (h.contains("facebook") || h.contains("fb.com")) return R.drawable.ic_logo_facebook;
        if (h.contains("tiktok")) return R.drawable.ic_logo_tiktok;
        if (h.contains("reddit")) return R.drawable.ic_logo_reddit;
        if (h.contains("instagram")) return R.drawable.ic_logo_instagram;
        if (h.contains("wikipedia")) return R.drawable.ic_logo_wikipedia;
        if (h.equals("x.com") || h.contains("twitter")) return R.drawable.ic_logo_x;
        if (h.contains("bbc")) return R.drawable.ic_logo_bbc;
        return 0;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
