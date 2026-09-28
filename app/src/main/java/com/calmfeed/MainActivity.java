package com.calmfeed;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.ScrollView;
import android.widget.TextView;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends Activity {
    private static final int BACKGROUND = Color.rgb(12, 20, 24);
    private static final int SURFACE = Color.rgb(24, 35, 39);
    private static final int TEXT = Color.rgb(239, 246, 240);
    private static final int MUTED = Color.rgb(159, 178, 169);
    private static final int ACCENT = Color.rgb(145, 224, 177);
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
    private ProgressBar usageProgress;
    private TextView allowanceLabel;
    private TextView policyStatus;
    private TextView sessionDetail;
    private SeekBar allowancePicker;
    private Button setAllowanceButton;
    private Button requestApprovalButton;
    private Button trustedCircleButton;
    private LinearLayout siteShortcuts;
    private Map<String, Object> latestPolicy = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(BACKGROUND);
        getWindow().setNavigationBarColor(BACKGROUND);
        getWindow().getDecorView().setSystemUiVisibility(0);

        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setBackgroundColor(BACKGROUND);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(22), dp(20), dp(22), dp(28));
        scroll.addView(content);
        page.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));

        TextView brand = text("CALMFEED", 12, ACCENT, true);
        brand.setLetterSpacing(0.16f);
        content.addView(brand);

        TextView greeting = text("Make room\nfor what matters.", 34, TEXT, true);
        greeting.setLineSpacing(dp(1), 1f);
        LinearLayout.LayoutParams greetingParams = params(-1, -2);
        greetingParams.topMargin = dp(14);
        content.addView(greeting, greetingParams);

        TextView date = text(
                LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMMM d", Locale.getDefault())),
                14, MUTED, false);
        LinearLayout.LayoutParams dateParams = params(-1, -2);
        dateParams.topMargin = dp(8);
        content.addView(date, dateParams);

        LinearLayout sessionCard = card();
        sessionCard.setBackground(gradient(
                Color.rgb(34, 73, 66), Color.rgb(34, 52, 62), 22));
        LinearLayout.LayoutParams sessionParams = params(-1, -2);
        sessionParams.topMargin = dp(22);
        content.addView(sessionCard, sessionParams);
        TextView sessionEyebrow = text("A LITTLE MORE INTENTION", 11, ACCENT, true);
        sessionEyebrow.setLetterSpacing(0.08f);
        sessionCard.addView(sessionEyebrow);
        TextView sessionTitle = text("15-minute sessions. Your pace.", 22, TEXT, true);
        LinearLayout.LayoutParams sessionTitleParams = params(-1, -2);
        sessionTitleParams.topMargin = dp(10);
        sessionCard.addView(sessionTitle, sessionTitleParams);
        sessionDetail = text(
                "Choose a daily allowance. CalmFeed tracks time on this device; trusted-person approvals sync through Firebase.",
                14, Color.rgb(204, 222, 214), false);
        LinearLayout.LayoutParams sessionDetailParams = params(-1, -2);
        sessionDetailParams.topMargin = dp(7);
        sessionCard.addView(sessionDetail, sessionDetailParams);

        LinearLayout allowanceCard = card();
        LinearLayout.LayoutParams allowanceParams = params(-1, -2);
        allowanceParams.topMargin = dp(14);
        content.addView(allowanceCard, allowanceParams);
        allowanceCard.addView(text("TODAY'S TIME ALLOWANCE", 11, ACCENT, true));
        allowanceLabel = text("15 minutes", 24, TEXT, true);
        allowanceCard.addView(allowanceLabel, topMarginParams(-1, -2, 8));
        allowancePicker = new SeekBar(this);
        allowancePicker.setMax(225);
        allowancePicker.setProgress(0);
        allowanceCard.addView(allowancePicker, topMarginParams(-1, dp(32), 4));
        TextView range = text("15 min  ·  up to 4 hours", 12, MUTED, false);
        allowanceCard.addView(range, topMarginParams(-1, -2, 0));
        setAllowanceButton = actionButton("Set today's allowance");
        allowanceCard.addView(setAllowanceButton, topMarginParams(-1, dp(50), 14));
        policyStatus = text("", 13, MUTED, false);
        allowanceCard.addView(policyStatus, topMarginParams(-1, -2, 10));
        requestApprovalButton = actionButton("Ask my trusted person for extra time");
        allowanceCard.addView(requestApprovalButton, topMarginParams(-1, dp(50), 10));

        trustedCircleButton = actionButton("Set up trusted person  →");
        LinearLayout.LayoutParams trustedParams = params(-1, dp(52));
        trustedParams.topMargin = dp(14);
        content.addView(trustedCircleButton, trustedParams);

        TextView sitesTitle = text("Choose your corner", 20, TEXT, true);
        LinearLayout.LayoutParams sitesTitleParams = params(-1, -2);
        sitesTitleParams.topMargin = dp(26);
        content.addView(sitesTitle, sitesTitleParams);

        TextView sitesSubtitle = text("Sign in on the real site, right here in your browser.", 13, MUTED, false);
        LinearLayout.LayoutParams sitesSubtitleParams = params(-1, -2);
        sitesSubtitleParams.topMargin = dp(5);
        content.addView(sitesSubtitle, sitesSubtitleParams);

        siteShortcuts = new LinearLayout(this);
        siteShortcuts.setOrientation(LinearLayout.HORIZONTAL);
        String[] labels = {"YouTube", "Instagram", "Facebook"};
        String[] hosts = {"youtube.com", "instagram.com", "facebook.com"};
        String[] initials = {"Y", "ig", "f"};
        int[] colors = {Color.rgb(255, 83, 78), Color.rgb(195, 101, 195), Color.rgb(89, 149, 245)};
        for (int index = 0; index < labels.length; index++) {
            siteShortcuts.addView(createSiteCard(labels[index], hosts[index], initials[index], colors[index]),
                    new LinearLayout.LayoutParams(0, dp(112), 1));
            if (index < labels.length - 1) {
                View spacer = new View(this);
                siteShortcuts.addView(spacer, new LinearLayout.LayoutParams(dp(10), 1));
            }
        }
        LinearLayout.LayoutParams sitesParams = params(-1, dp(112));
        sitesParams.topMargin = dp(14);
        content.addView(siteShortcuts, sitesParams);

        TextView usageHeading = text("Your time, today", 20, TEXT, true);
        LinearLayout.LayoutParams usageHeadingParams = params(-1, -2);
        usageHeadingParams.topMargin = dp(28);
        content.addView(usageHeading, usageHeadingParams);

        LinearLayout usageCard = card();
        LinearLayout.LayoutParams usageCardParams = params(-1, -2);
        usageCardParams.topMargin = dp(12);
        content.addView(usageCard, usageCardParams);
        LinearLayout usageRow = new LinearLayout(this);
        usageRow.setGravity(Gravity.CENTER_VERTICAL);
        usageRow.setOrientation(LinearLayout.HORIZONTAL);
        usageCard.addView(usageRow);
        usageValue = text("0 min", 32, TEXT, true);
        usageRow.addView(usageValue, new LinearLayout.LayoutParams(0, -2, 1));
        TextView dailyLabel = text("IN CALMFEED", 10, ACCENT, true);
        dailyLabel.setLetterSpacing(0.08f);
        usageRow.addView(dailyLabel);

        usageProgress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        usageProgress.setMax(60);
        usageProgress.setProgressTintList(android.content.res.ColorStateList.valueOf(ACCENT));
        usageProgress.setProgressBackgroundTintList(
                android.content.res.ColorStateList.valueOf(Color.rgb(52, 68, 67)));
        LinearLayout.LayoutParams progressParams = params(-1, dp(5));
        progressParams.topMargin = dp(16);
        usageCard.addView(usageProgress, progressParams);

        usageSubtitle = text("Time spent in the CalmFeed browser today.", 12, MUTED, false);
        LinearLayout.LayoutParams usageSubtitleParams = params(-1, -2);
        usageSubtitleParams.topMargin = dp(10);
        usageCard.addView(usageSubtitle, usageSubtitleParams);

        LinearLayout quoteCard = card();
        quoteCard.setBackground(gradient(Color.rgb(37, 43, 59), Color.rgb(36, 39, 48), 20));
        LinearLayout.LayoutParams quoteCardParams = params(-1, -2);
        quoteCardParams.topMargin = dp(18);
        content.addView(quoteCard, quoteCardParams);
        TextView quoteEyebrow = text("A THOUGHT FOR TODAY", 11, ACCENT, true);
        quoteEyebrow.setLetterSpacing(0.08f);
        quoteCard.addView(quoteEyebrow);
        TextView quote = text(dailyQuote(), 18, TEXT, false);
        quote.setLineSpacing(dp(4), 1.05f);
        LinearLayout.LayoutParams quoteParams = params(-1, -2);
        quoteParams.topMargin = dp(12);
        quoteCard.addView(quote, quoteParams);
        TextView signature = text("— A CalmFeed reminder", 12, MUTED, false);
        signature.setGravity(Gravity.END);
        LinearLayout.LayoutParams signatureParams = params(-1, -2);
        signatureParams.topMargin = dp(12);
        quoteCard.addView(signature, signatureParams);

        TextView privacyNote = text(
                "Your activity time is stored on this phone. CalmFeed never asks for your social passwords.",
                12, MUTED, false);
        LinearLayout.LayoutParams privacyParams = params(-1, -2);
        privacyParams.topMargin = dp(18);
        content.addView(privacyNote, privacyParams);

        allowancePicker.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int minutes = 15 + Math.round(progress / 5f) * 5;
                allowanceLabel.setText(minutes + " minutes");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        setAllowanceButton.setOnClickListener(view -> saveDailyAllowance());
        requestApprovalButton.setOnClickListener(view -> requestExtraTime());
        trustedCircleButton.setOnClickListener(view ->
                startActivity(new Intent(this, TrustActivity.class)));

        setContentView(page);
        refreshUsage();
    }

    private View createSiteCard(String label, String host, String initials, int iconColor) {
        LinearLayout site = new LinearLayout(this);
        site.setGravity(Gravity.CENTER);
        site.setOrientation(LinearLayout.VERTICAL);
        site.setPadding(dp(4), dp(10), dp(4), dp(10));
        site.setBackground(background(SURFACE, 18));
        site.setClickable(true);
        site.setFocusable(true);
        site.setContentDescription("Open " + label);
        site.setOnClickListener(view -> {
            if (!FirebaseBridge.isConfigured(this)) {
                showMessage("Finish Firebase setup before using social sites.");
                return;
            }
            if (number(latestPolicy.get("allowanceMinutes")) <= 0) {
                showMessage("Set today's time allowance first.");
                return;
            }
            Intent intent = new Intent(this, BrowserActivity.class);
            intent.putExtra(BrowserActivity.EXTRA_URL, "https://www." + host);
            startActivity(intent);
        });

        TextView icon = text(initials, initials.length() > 1 ? 14 : 18, Color.WHITE, true);
        icon.setGravity(Gravity.CENTER);
        icon.setBackground(background(iconColor, 50));
        site.addView(icon, new LinearLayout.LayoutParams(dp(36), dp(36)));

        TextView title = text(label, 11, TEXT, true);
        title.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams titleParams = params(-1, -2);
        titleParams.topMargin = dp(8);
        site.addView(title, titleParams);
        return site;
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
        usageProgress.setProgress((int) Math.min(minutes, usageProgress.getMax()));
        if (minutes >= 60) {
            usageSubtitle.setText(String.format(
                    Locale.getDefault(), "%d hr %02d min in the CalmFeed browser today.",
                    minutes / 60, minutes % 60));
        } else if (minutes == 1) {
            usageSubtitle.setText("1 minute in the CalmFeed browser today.");
        } else {
            usageSubtitle.setText(String.format(
                    Locale.getDefault(), "%d minutes in the CalmFeed browser today.", minutes));
        }
    }

    private void saveDailyAllowance() {
            int minutes = 15 + Math.round(allowancePicker.getProgress() / 5f) * 5;
            Map<String, Object> data = new HashMap<>();
            data.put("minutes", minutes);
            data.put("timeZone", FirebaseBridge.timezoneId());
            setAllowanceButton.setEnabled(false);
            FirebaseBridge.call(this, "setDailyAllowance", data, (result, error) -> {
                if (isFinishing()) return;
                setAllowanceButton.setEnabled(true);
                if (error != null) {
                    showMessage(error);
                    refreshPolicy();
                    return;
                }
                showMessage("Today's " + minutes + "-minute allowance is set.");
                refreshPolicy();
            });
        }

        private void requestExtraTime() {
            if (latestPolicy.get("trustedApprover") == null) {
                startActivity(new Intent(this, TrustActivity.class));
                return;
            }
            requestApprovalButton.setEnabled(false);
            FirebaseBridge.call(this, "requestExtraTime", policyInput(), (result, error) -> {
                if (isFinishing()) return;
                requestApprovalButton.setEnabled(true);
                if (error != null) {
                    showMessage(error);
                    refreshPolicy();
                    return;
                }
                showMessage("Request sent. Your trusted person can review it in CalmFeed.");
                refreshPolicy();
            });
        }

        private void refreshPolicy() {
            if (!FirebaseBridge.isConfigured(this)) {
                setPolicyForMissingFirebase();
                return;
            }
            FirebaseBridge.call(this, "getPolicy", policyInput(), (result, error) -> {
                if (isFinishing()) return;
                if (error != null) {
                    policyStatus.setText(error);
                    setShortcutsEnabled(false);
                    setAllowanceButton.setEnabled(false);
                    allowancePicker.setEnabled(false);
                    requestApprovalButton.setVisibility(View.GONE);
                    return;
                }
                latestPolicy = result;
                renderPolicy();
            });
        }

        private void setPolicyForMissingFirebase() {
            latestPolicy.clear();
            policyStatus.setText("Connect Firebase to set up trusted-person approvals. Setup steps are in README.md.");
            allowancePicker.setEnabled(false);
            setAllowanceButton.setEnabled(false);
            requestApprovalButton.setVisibility(View.GONE);
            trustedCircleButton.setEnabled(true);
            setShortcutsEnabled(false);
            sessionDetail.setText("The allowance timer stays on this device; trusted-person approvals need Firebase setup.");
        }

        private void renderPolicy() {
            int dailyMinutes = number(latestPolicy.get("allowanceMinutes"));
            int dailyRemaining = number(latestPolicy.get("remainingSeconds"));
            int extraRemaining = number(latestPolicy.get("extraSeconds"));
            int minutesLeft = (dailyRemaining + extraRemaining + 59) / 60;
            Object approver = latestPolicy.get("trustedApprover");
            Object pendingId = latestPolicy.get("pendingRequestId");
            boolean hasAllowance = dailyMinutes > 0;

            allowancePicker.setEnabled(!hasAllowance);
            setAllowanceButton.setVisibility(hasAllowance ? View.GONE : View.VISIBLE);
            allowancePicker.setVisibility(hasAllowance ? View.GONE : View.VISIBLE);
            allowanceLabel.setVisibility(hasAllowance ? View.GONE : View.VISIBLE);
            if (hasAllowance) {
                policyStatus.setText(String.format(
                        Locale.getDefault(), "Today: %d-minute allowance · %d minutes remaining",
                        dailyMinutes, minutesLeft));
                sessionDetail.setText(String.format(
                        Locale.getDefault(),
                        "Your on-device allowance is set to %d minutes. %d minutes remain today; it resets tomorrow. Device changes can bypass this timer.",
                        dailyMinutes, minutesLeft));
            } else {
                policyStatus.setText("Choose today's allowance. You can set it once, and it resets tomorrow.");
                sessionDetail.setText(
                        "Set one daily on-device time allowance. After it's used, ask your trusted person for extra time.");
            }

            if (approver instanceof Map) {
                Object name = ((Map<?, ?>) approver).get("name");
                trustedCircleButton.setText("Trusted person · " + (name instanceof String ? name : "connected"));
            } else {
                trustedCircleButton.setText("Set up trusted person  →");
            }

            if (hasAllowance && dailyRemaining <= 0 && extraRemaining <= 0) {
                requestApprovalButton.setVisibility(View.VISIBLE);
                if (pendingId instanceof String) {
                    requestApprovalButton.setText("Approval requested · check back soon");
                    requestApprovalButton.setEnabled(false);
                } else if (latestPolicy.get("pendingStatus") instanceof String
                        && !"approved".equals(latestPolicy.get("pendingStatus"))
                        && approver instanceof Map) {
                    requestApprovalButton.setText("Request answered · refresh your allowance");
                    requestApprovalButton.setEnabled(false);
                } else if (approver instanceof Map) {
                    requestApprovalButton.setText("Ask my trusted person for extra time");
                    requestApprovalButton.setEnabled(true);
                } else {
                    requestApprovalButton.setText("Connect a trusted person to request more time");
                    requestApprovalButton.setEnabled(true);
                }
            } else {
                requestApprovalButton.setVisibility(View.GONE);
            }
            setAllowanceButton.setEnabled(!hasAllowance);
            setShortcutsEnabled(hasAllowance && minutesLeft > 0);
        }

        private Map<String, Object> policyInput() {
            Map<String, Object> data = new HashMap<>();
            data.put("timeZone", FirebaseBridge.timezoneId());
            return data;
        }

        private void setShortcutsEnabled(boolean enabled) {
            for (int index = 0; index < siteShortcuts.getChildCount(); index++) {
                siteShortcuts.getChildAt(index).setEnabled(enabled);
                siteShortcuts.getChildAt(index).setAlpha(enabled ? 1f : 0.48f);
            }
        }

        private int number(Object value) {
            return value instanceof Number ? ((Number) value).intValue() : 0;
        }

        private void showMessage(String message) {
            android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_LONG).show();
        }

    private LinearLayout card() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(18), dp(18), dp(18), dp(18));
        card.setBackground(background(SURFACE, 20));
        return card;
    }

    private TextView text(String value, int size, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        if (bold) view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return view;
    }

    private GradientDrawable background(int color, int radius) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(radius));
        return drawable;
    }

    private GradientDrawable gradient(int start, int end, int radius) {
        GradientDrawable drawable = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR, new int[] {start, end});
        drawable.setCornerRadius(dp(radius));
        return drawable;
    }

    private LinearLayout.LayoutParams params(int width, int height) {
        return new LinearLayout.LayoutParams(width, height);
    }

    private LinearLayout.LayoutParams topMarginParams(int width, int height, int margin) {
        LinearLayout.LayoutParams params = params(width, height);
        params.topMargin = dp(margin);
        return params;
    }

    private Button actionButton(String label) {
        Button button = new Button(this);
        button.setAllCaps(false);
        button.setText(label);
        button.setTextColor(TEXT);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setBackground(background(Color.rgb(49, 77, 67), 12));
        return button;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (usageValue != null) refreshUsage();
        if (policyStatus != null) refreshPolicy();
    }
}
