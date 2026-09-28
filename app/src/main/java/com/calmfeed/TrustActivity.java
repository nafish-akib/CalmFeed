package com.calmfeed;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.text.InputFilter;
import android.text.InputType;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.DateFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class TrustActivity extends Activity {
    private static final int BACKGROUND = Color.rgb(12, 20, 24);
    private static final int SURFACE = Color.rgb(24, 35, 39);
    private static final int TEXT = Color.rgb(239, 246, 240);
    private static final int MUTED = Color.rgb(159, 178, 169);
    private static final int ACCENT = Color.rgb(145, 224, 177);

    private final Handler handler = new Handler();
    private LinearLayout content;
    private TextView connectionStatus;
    private TextView linkedStatus;
    private TextView requestList;
    private LinearLayout requestCards;
    private EditText childNameInput;
    private EditText inviteCodeInput;
    private EditText guardianNameInput;
    private Button createInviteButton;
    private Button acceptInviteButton;
    private Button unlinkButton;
    private String inviteCode;
    private boolean polling;

    private final Runnable requestRefresh = new Runnable() {
        @Override
        public void run() {
            if (!polling) return;
            loadGuardianRequests();
            handler.postDelayed(this, 15_000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(BACKGROUND);
        getWindow().setNavigationBarColor(BACKGROUND);

        ScrollView scroll = new ScrollView(this);
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(22), dp(24), dp(22), dp(32));
        content.setBackgroundColor(BACKGROUND);
        scroll.addView(content);

        TextView heading = text("Your trusted circle", 30, TEXT, true);
        content.addView(heading);
        TextView intro = text(
                "Pair with someone you trust. They can review requests and choose how much extra time to grant.",
                14, MUTED, false);
        addTop(intro, 8);

        connectionStatus = text("", 13, Color.rgb(255, 192, 132), false);
        addTop(connectionStatus, 14);

        LinearLayout childCard = card();
        addTop(childCard, 22);
        childCard.addView(text("SET UP YOUR TRUSTED PERSON", 12, ACCENT, true));
        childNameInput = input("Your name or nickname", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        childCard.addView(childNameInput, topParams(dp(52), 12));
        createInviteButton = actionButton("Create an invite code");
        childCard.addView(createInviteButton, topParams(dp(50), 12));
        linkedStatus = text("No trusted person connected yet.", 13, MUTED, false);
        childCard.addView(linkedStatus, topParams(-2, 12));
        unlinkButton = actionButton("Disconnect trusted person");
        childCard.addView(unlinkButton, topParams(dp(48), 12));

        LinearLayout guardianCard = card();
        addTop(guardianCard, 14);
        guardianCard.addView(text("JOIN SOMEONE YOU TRUST", 12, ACCENT, true));
        guardianNameInput = input("Your name for their dashboard", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        guardianCard.addView(guardianNameInput, topParams(dp(52), 12));
        inviteCodeInput = input("32-character invite code", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);
        inviteCodeInput.setFilters(new InputFilter[] {new InputFilter.LengthFilter(32)});
        guardianCard.addView(inviteCodeInput, topParams(dp(52), 10));
        acceptInviteButton = actionButton("Connect as trusted person");
        guardianCard.addView(acceptInviteButton, topParams(dp(50), 12));

        LinearLayout approvalsCard = card();
        addTop(approvalsCard, 14);
        approvalsCard.addView(text("APPROVAL REQUESTS", 12, ACCENT, true));
        requestList = text("Waiting for requests…", 14, MUTED, false);
        requestList.setLineSpacing(dp(5), 1f);
        approvalsCard.addView(requestList, topParams(-2, 10));
        requestCards = new LinearLayout(this);
        requestCards.setOrientation(LinearLayout.VERTICAL);
        approvalsCard.addView(requestCards, topParams(-1, -2, 8));
        TextView pollingNote = text("This screen checks for new requests every 15 seconds.", 12, MUTED, false);
        approvalsCard.addView(pollingNote, topParams(-2, 10));

        createInviteButton.setOnClickListener(view -> createInvite());
        acceptInviteButton.setOnClickListener(view -> acceptInvite());
        unlinkButton.setOnClickListener(view -> unlinkTrustedPerson());

        setContentView(scroll);
        updateFirebaseStatus();
    }

    private void updateFirebaseStatus() {
        if (!FirebaseBridge.isConfigured(this)) {
            connectionStatus.setText(
                    "Firebase setup is needed before pairing or approvals work. See the setup steps in README.md.");
            setControlsEnabled(false);
            linkedStatus.setText("Pairing is unavailable until Firebase is configured.");
            requestList.setText("Approval inbox is unavailable until Firebase is configured.");
            return;
        }
        connectionStatus.setText("Secure Firebase connection will be established when you pair.");
        setControlsEnabled(true);
        refreshPolicy();
    }

    private void refreshPolicy() {
        Map<String, Object> data = new HashMap<>();
        data.put("timeZone", FirebaseBridge.timezoneId());
        FirebaseBridge.call(this, "getPolicy", data, (result, error) -> {
            if (isFinishing()) return;
            if (error != null) {
                connectionStatus.setText(error);
                return;
            }
            Object trusted = result.get("trustedApprover");
            if (trusted instanceof Map) {
                Object name = ((Map<?, ?>) trusted).get("name");
                linkedStatus.setText("Connected to " + (name instanceof String ? name : "your trusted person") + ".");
                createInviteButton.setEnabled(false);
                childNameInput.setEnabled(false);
                unlinkButton.setEnabled(true);
            } else {
                linkedStatus.setText("No trusted person connected yet.");
                createInviteButton.setEnabled(true);
                childNameInput.setEnabled(true);
                unlinkButton.setEnabled(false);
            }
        });
    }

    private void createInvite() {
        Map<String, Object> data = new HashMap<>();
        data.put("childName", childNameInput.getText().toString().trim());
        createInviteButton.setEnabled(false);
        FirebaseBridge.call(this, "createInvite", data, (result, error) -> {
            if (isFinishing()) return;
            createInviteButton.setEnabled(true);
            if (error != null) {
                showError(error);
                refreshPolicy();
                return;
            }
            Object value = result.get("code");
            if (!(value instanceof String)) {
                showError("Firebase returned an invalid pairing code.");
                return;
            }
            inviteCode = (String) value;
            shareInviteCode();
        });
    }

    private void shareInviteCode() {
        String message = "Join my CalmFeed trusted circle with invite code "
                + inviteCode + ". It expires in 10 minutes. Install CalmFeed, open Trusted circle, and enter this code.";
        Intent share = new Intent(Intent.ACTION_SEND);
        share.setType("text/plain");
        share.putExtra(Intent.EXTRA_TEXT, message);
        startActivity(Intent.createChooser(share, "Share your CalmFeed invite"));
    }

    private void acceptInvite() {
        String code = inviteCodeInput.getText().toString().trim().toUpperCase(Locale.ROOT);
        String name = guardianNameInput.getText().toString().trim();
        if (code.length() != 32) {
            showError("Enter the complete 32-character invite code.");
            return;
        }
        if (name.isEmpty()) {
            showError("Enter a name the CalmFeed user will recognize.");
            return;
        }
        Map<String, Object> data = new HashMap<>();
        data.put("code", code);
        data.put("name", name);
        acceptInviteButton.setEnabled(false);
        FirebaseBridge.call(this, "acceptInvite", data, (result, error) -> {
            if (isFinishing()) return;
            acceptInviteButton.setEnabled(true);
            if (error != null) {
                showError(error);
                return;
            }
            inviteCodeInput.setText("");
            Toast.makeText(this, "You are now their trusted person.", Toast.LENGTH_LONG).show();
            refreshPolicy();
        });
    }

    private void unlinkTrustedPerson() {
        FirebaseBridge.call(this, "unlinkTrustedPerson", Collections.emptyMap(), (result, error) -> {
            if (isFinishing()) return;
            if (error != null) {
                showError(error);
                return;
            }
            Toast.makeText(this, "Trusted person disconnected.", Toast.LENGTH_LONG).show();
            refreshPolicy();
        });
    }

    private void loadGuardianRequests() {
        FirebaseBridge.call(this, "listApprovalRequests", Collections.emptyMap(), (result, error) -> {
            if (isFinishing() || !polling) return;
            if (error != null) {
                requestList.setText(error);
                return;
            }
            Object value = result.get("requests");
            if (!(value instanceof List)) {
                requestList.setText("Firebase returned an invalid request list.");
                return;
            }
            showApprovalRequests((List<?>) value);
        });
    }

    private void showApprovalRequests(List<?> requests) {
        requestCards.removeAllViews();
        if (requests.isEmpty()) {
            requestList.setText("No pending requests.");
            return;
        }
        requestList.setText(requests.size() + " pending request" + (requests.size() == 1 ? "." : "s."));

        for (Object entry : requests) {
            if (!(entry instanceof Map)) continue;
            Map<?, ?> item = (Map<?, ?>) entry;
            Object requestDocumentId = item.get("requestDocumentId");
            Object requestId = item.get("requestId");
            Object childName = item.get("childName");
            Object createdAt = item.get("createdAtMillis");
            if (!(requestDocumentId instanceof String) || !(requestId instanceof String)) continue;

            LinearLayout requestCard = card();
            requestCard.setBackground(background(Color.rgb(34, 47, 48), 15));
            requestCard.addView(text(
                    String.valueOf(childName) + " is asking for more time",
                    16, TEXT, true));
            if (createdAt instanceof Number) {
                TextView when = text(DateFormat.getDateTimeInstance(
                        DateFormat.SHORT, DateFormat.SHORT).format(
                        new java.util.Date(((Number) createdAt).longValue())), 12, MUTED, false);
                requestCard.addView(when, topParams(-2, 5));
            }

            EditText extraMinutes = input("Extra minutes (1–120)", InputType.TYPE_CLASS_NUMBER);
            extraMinutes.setText("15");
            requestCard.addView(extraMinutes, topParams(dp(50), 10));
            LinearLayout actions = new LinearLayout(this);
            actions.setOrientation(LinearLayout.HORIZONTAL);
            Button approve = actionButton("Approve");
            Button deny = actionButton("Not now");
            actions.addView(approve, new LinearLayout.LayoutParams(0, dp(48), 1));
            LinearLayout.LayoutParams denyParams = new LinearLayout.LayoutParams(0, dp(48), 1);
            denyParams.leftMargin = dp(8);
            actions.addView(deny, denyParams);
            requestCard.addView(actions, topParams(-1, 10));
            requestCards.addView(requestCard, topParams(-1, 12));

            String documentId = (String) requestDocumentId;
            String id = (String) requestId;
            approve.setOnClickListener(view -> respondToRequest(
                    documentId, id, true, extraMinutes.getText().toString()));
            deny.setOnClickListener(view -> respondToRequest(documentId, id, false, "0"));
        }
    }

    private void respondToRequest(
            String childUid,
            String requestId,
            boolean approved,
            String minutesText) {
        int minutes = 0;
        if (approved) {
            try {
                minutes = Integer.parseInt(minutesText);
            } catch (NumberFormatException exception) {
                showError("Enter how many extra minutes to grant.");
                return;
            }
            if (minutes < 1 || minutes > 120) {
                showError("Choose an extra time allowance from 1 to 120 minutes.");
                return;
            }
        }

        Map<String, Object> data = new HashMap<>();
        data.put("childUid", childUid);
        data.put("requestId", requestId);
        data.put("approved", approved);
        data.put("extraMinutes", minutes);
        final int grantedMinutes = minutes;
        FirebaseBridge.call(this, "respondToRequest", data, (result, error) -> {
            if (isFinishing()) return;
            if (error != null) {
                showError(error);
                return;
            }
            Object status = result.get("status");
            String message;
            if ("expired".equals(status)) {
                message = "This request expired at the daily reset.";
            } else if (approved) {
                message = "Approved " + grantedMinutes + " extra minutes.";
            } else {
                message = "Request declined.";
            }
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
            loadGuardianRequests();
        });
    }

    private void setControlsEnabled(boolean enabled) {
        createInviteButton.setEnabled(enabled);
        acceptInviteButton.setEnabled(enabled);
        inviteCodeInput.setEnabled(enabled);
        guardianNameInput.setEnabled(enabled);
        childNameInput.setEnabled(enabled);
        unlinkButton.setEnabled(enabled);
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    private EditText input(String hint, int inputType) {
        EditText editText = new EditText(this);
        editText.setSingleLine(true);
        editText.setHint(hint);
        editText.setTextColor(TEXT);
        editText.setHintTextColor(MUTED);
        editText.setInputType(inputType);
        editText.setPadding(dp(12), 0, dp(12), 0);
        editText.setBackground(background(Color.rgb(34, 47, 48), 12));
        return editText;
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

    private LinearLayout card() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(16), dp(16), dp(16));
        card.setBackground(background(SURFACE, 18));
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

    private void addTop(android.view.View view, int margin) {
        content.addView(view, topParams(-1, -2, margin));
    }

    private LinearLayout.LayoutParams topParams(int height, int margin) {
        return topParams(-1, height, margin);
    }

    private LinearLayout.LayoutParams topParams(int width, int height, int margin) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(width, height);
        params.topMargin = dp(margin);
        return params;
    }

    private GradientDrawable background(int color, int radius) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(radius));
        return drawable;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onResume() {
        super.onResume();
        polling = true;
        handler.post(requestRefresh);
        refreshPolicy();
    }

    @Override
    protected void onPause() {
        polling = false;
        handler.removeCallbacks(requestRefresh);
        super.onPause();
    }
}
