package com.calmfeed;

import android.content.Context;
import android.text.TextUtils;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.Source;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

final class FirebaseBridge {
    interface Callback {
        void onComplete(Map<String, Object> result, String errorMessage);
    }

    private FirebaseBridge() {}

    static boolean isConfigured(Context context) {
        if (!FirebaseApp.getApps(context).isEmpty()) return true;
        return FirebaseApp.initializeApp(context) != null;
    }

    static String timezoneId() {
        return ZoneId.systemDefault().getId();
    }

    static void call(Context context, String operation, Map<String, Object> data, Callback callback) {
        if (!isConfigured(context)) {
            callback.onComplete(null, "Firebase is not configured yet. Add google-services.json and finish the Firebase setup.");
            return;
        }

        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() != null) {
            execute(context, auth.getUid(), operation, data, callback);
            return;
        }

        auth.signInAnonymously().addOnCompleteListener(task -> {
            if (!task.isSuccessful() || task.getResult() == null
                    || task.getResult().getUser() == null) {
                callback.onComplete(null, readableError(task.getException()));
                return;
            }
            execute(context, task.getResult().getUser().getUid(), operation, data, callback);
        });
    }

    private static void execute(
            Context context,
            String uid,
            String operation,
            Map<String, Object> data,
            Callback callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        switch (operation) {
            case "getPolicy":
                loadPolicy(context, db, uid, callback);
                break;
            case "setDailyAllowance":
                setDailyAllowance(context, data, callback);
                break;
            case "createInvite":
                createInvite(db, uid, data, callback);
                break;
            case "acceptInvite":
                acceptInvite(db, uid, data, callback);
                break;
            case "unlinkTrustedPerson":
                unlinkTrustedPerson(db, uid, callback);
                break;
            case "requestExtraTime":
                requestExtraTime(context, db, uid, callback);
                break;
            case "listApprovalRequests":
                listApprovalRequests(db, uid, callback);
                break;
            case "respondToRequest":
                respondToRequest(db, uid, data, callback);
                break;
            case "startSession":
                startSession(context, db, uid, callback);
                break;
            case "endSession":
                callback.onComplete(result("ended", true), null);
                break;
            default:
                callback.onComplete(null, "Unknown CalmFeed operation.");
        }
    }

    private static void loadPolicy(
            Context context,
            FirebaseFirestore db,
            String uid,
            Callback callback) {
        DocumentReference relationship = db.collection("relationships").document(uid);
        DocumentReference approval = db.collection("accessRequests").document(uid);
        relationship.get(Source.SERVER).addOnSuccessListener(link ->
                approval.get(Source.SERVER).addOnSuccessListener(request -> {
                    String requestId = request.getString("requestId");
                    String status = request.getString("status");
                    String budgetDate = request.getString("budgetDate");
                    String today = LocalDate.now().toString();

                    if ("approved".equals(status) && today.equals(budgetDate)) {
                        long minutes = number(request.get("extraMinutes"));
                        if (requestId == null || minutes < 1 || minutes > 120) {
                            callback.onComplete(null, "The trusted-person approval has invalid details.");
                            return;
                        }
                        if (!UsageTracker.addExtraGrant(context, requestId, minutes * 60)) {
                            callback.onComplete(null, "Could not save the approved time on this device.");
                            return;
                        }
                        approval.update("status", "consumed")
                                .addOnSuccessListener(ignored ->
                                        completePolicy(context, link, requestId, "consumed", callback))
                                .addOnFailureListener(error ->
                                        callback.onComplete(null, readableError(error)));
                        return;
                    }

                    String visibleStatus = status;
                    if ("approved".equals(status) && !today.equals(budgetDate)) {
                        visibleStatus = "expired";
                    }
                    completePolicy(context, link, requestId, visibleStatus, callback);
                }).addOnFailureListener(error -> callback.onComplete(null, readableError(error)))
        ).addOnFailureListener(error -> callback.onComplete(null, readableError(error)));
    }

    private static void completePolicy(
            Context context,
            DocumentSnapshot relationship,
            String requestId,
            String status,
            Callback callback) {
        Map<String, Object> policy = new HashMap<>();
        policy.put("day", LocalDate.now().toString());
        policy.put("allowanceMinutes", UsageTracker.getTodayAllowanceMinutes(context));
        policy.put("remainingSeconds", UsageTracker.getDailyRemainingSeconds(context));
        policy.put("extraSeconds", UsageTracker.getExtraRemainingSeconds(context));
        if (relationship.exists()) {
            Map<String, Object> approver = new HashMap<>();
            approver.put("uid", relationship.getString("guardianUid"));
            approver.put("name", relationship.getString("guardianName"));
            policy.put("trustedApprover", approver);
        }
        if ("pending".equals(status) && requestId != null) {
            policy.put("pendingRequestId", requestId);
        }
        policy.put("pendingStatus", status);
        callback.onComplete(policy, null);
    }

    private static void setDailyAllowance(
            Context context,
            Map<String, Object> data,
            Callback callback) {
        long minutes = number(data.get("minutes"));
        if (minutes < 15 || minutes > 240 || minutes % 5 != 0) {
            callback.onComplete(null, "Daily allowance must be from 15 to 240 minutes in 5-minute steps.");
            return;
        }
        if (!UsageTracker.setTodayAllowance(context, (int) minutes)) {
            String error = UsageTracker.getTodayAllowanceMinutes(context) > 0
                    ? "Today's time allowance has already been set."
                    : "Could not save today's time allowance on this device.";
            callback.onComplete(null, error);
            return;
        }
        Map<String, Object> result = new HashMap<>();
        result.put("day", LocalDate.now().toString());
        result.put("allowanceMinutes", minutes);
        result.put("remainingSeconds", minutes * 60);
        callback.onComplete(result, null);
    }

    private static void createInvite(
            FirebaseFirestore db,
            String uid,
            Map<String, Object> data,
            Callback callback) {
        DocumentReference relationship = db.collection("relationships").document(uid);
        relationship.get(Source.SERVER).addOnSuccessListener(link -> {
            if (link.exists()) {
                callback.onComplete(null, "Disconnect your current trusted person before pairing another.");
                return;
            }
            String code = UUID.randomUUID().toString().replace("-", "").toUpperCase(Locale.ROOT);
            DocumentReference invite = db.collection("invites").document(code);
            String childName = stringValue(data.get("childName")).trim();
            if (childName.length() > 32) childName = childName.substring(0, 32);
            Map<String, Object> values = new HashMap<>();
            values.put("childUid", uid);
            values.put("childName", childName.isEmpty() ? "CalmFeed user" : childName);
            values.put("createdAt", FieldValue.serverTimestamp());
            values.put("expiresAt", new Timestamp(new java.util.Date(System.currentTimeMillis() + 10 * 60_000L)));
            values.put("used", false);
            invite.set(values).addOnSuccessListener(ignored -> {
                Map<String, Object> result = new HashMap<>();
                result.put("code", code);
                result.put("expiresInSeconds", 10 * 60);
                callback.onComplete(result, null);
            }).addOnFailureListener(error -> callback.onComplete(null, readableError(error)));
        }).addOnFailureListener(error -> callback.onComplete(null, readableError(error)));
    }

    private static void acceptInvite(
            FirebaseFirestore db,
            String guardianUid,
            Map<String, Object> data,
            Callback callback) {
        String code = stringValue(data.get("code")).trim().toUpperCase(Locale.ROOT);
        String name = stringValue(data.get("name")).trim();
        if (code.length() != 32) {
            callback.onComplete(null, "Enter the complete 32-character invite code.");
            return;
        }
        if (name.isEmpty() || name.length() > 32) {
            callback.onComplete(null, "Enter a name the CalmFeed user will recognize (up to 32 characters).");
            return;
        }

        DocumentReference invite = db.collection("invites").document(code);
        db.runTransaction(transaction -> {
            DocumentSnapshot invitation = transaction.get(invite);
            if (!invitation.exists()) throw new IllegalStateException("That pairing code is not valid.");
            Timestamp expiresAt = invitation.getTimestamp("expiresAt");
            if (Boolean.TRUE.equals(invitation.getBoolean("used"))
                    || expiresAt == null || expiresAt.toDate().getTime() <= System.currentTimeMillis()) {
                throw new IllegalStateException("That pairing code has expired or was already used.");
            }
            String childUid = invitation.getString("childUid");
            if (childUid == null || childUid.equals(guardianUid)) {
                throw new IllegalStateException("Use the trusted-person role on a different device.");
            }
            DocumentReference relationship = db.collection("relationships").document(childUid);
            String childName = invitation.getString("childName");
            Map<String, Object> link = new HashMap<>();
            link.put("childUid", childUid);
            link.put("childName", childName == null ? "CalmFeed user" : childName);
            link.put("guardianUid", guardianUid);
            link.put("guardianName", name);
            link.put("inviteId", code);
            link.put("linkedAt", FieldValue.serverTimestamp());
            transaction.update(invite, "used", true, "acceptedBy", guardianUid);
            transaction.set(relationship, link);
            return childName == null ? "CalmFeed user" : childName;
        }).addOnSuccessListener(childName -> {
            Map<String, Object> result = new HashMap<>();
            result.put("childName", childName);
            callback.onComplete(result, null);
        }).addOnFailureListener(error -> callback.onComplete(null, readableError(error)));
    }

    private static void unlinkTrustedPerson(
            FirebaseFirestore db,
            String uid,
            Callback callback) {
        DocumentReference relationship = db.collection("relationships").document(uid);
        DocumentReference request = db.collection("accessRequests").document(uid);
        relationship.get(Source.SERVER).addOnSuccessListener(link -> {
            if (!link.exists()) {
                callback.onComplete(null, "There is no trusted person to disconnect.");
                return;
            }
            db.runTransaction(transaction -> {
                DocumentSnapshot requestSnapshot = transaction.get(request);
                transaction.delete(relationship);
                if (requestSnapshot.exists()) transaction.delete(request);
                return true;
            }).addOnSuccessListener(ignored ->
                    callback.onComplete(result("disconnected", true), null))
                    .addOnFailureListener(error -> callback.onComplete(null, readableError(error)));
        }).addOnFailureListener(error -> callback.onComplete(null, readableError(error)));
    }

    private static void requestExtraTime(
            Context context,
            FirebaseFirestore db,
            String uid,
            Callback callback) {
        if (UsageTracker.getTodayAllowanceMinutes(context) == 0) {
            callback.onComplete(null, "Set today's time allowance before requesting more time.");
            return;
        }
        if (UsageTracker.getDailyRemainingSeconds(context) > 0
                || UsageTracker.getExtraRemainingSeconds(context) > 0) {
            callback.onComplete(null, "There is still time available in today's allowance.");
            return;
        }

        DocumentReference relationship = db.collection("relationships").document(uid);
        DocumentReference request = db.collection("accessRequests").document(uid);
        String requestId = UUID.randomUUID().toString();
        db.runTransaction(transaction -> {
            DocumentSnapshot link = transaction.get(relationship);
            if (!link.exists() || link.getString("guardianUid") == null) {
                throw new IllegalStateException("Pair with a trusted person before requesting more time.");
            }
            DocumentSnapshot previous = transaction.get(request);
            if (previous.exists() && "pending".equals(previous.getString("status"))) {
                throw new IllegalStateException("An approval request is already waiting.");
            }
            Map<String, Object> values = new HashMap<>();
            values.put("requestId", requestId);
            values.put("childUid", uid);
            values.put("childName", link.getString("childName"));
            values.put("guardianUid", link.getString("guardianUid"));
            values.put("status", "pending");
            values.put("budgetDate", LocalDate.now().toString());
            values.put("createdAt", FieldValue.serverTimestamp());
            transaction.set(request, values);
            return requestId;
        }).addOnSuccessListener(createdId -> {
            Map<String, Object> result = new HashMap<>();
            result.put("requestId", createdId);
            result.put("status", "pending");
            callback.onComplete(result, null);
        }).addOnFailureListener(error -> callback.onComplete(null, readableError(error)));
    }

    private static void listApprovalRequests(
            FirebaseFirestore db,
            String guardianUid,
            Callback callback) {
        db.collection("accessRequests")
                .whereEqualTo("guardianUid", guardianUid)
                .limit(50)
                .get(Source.SERVER)
                .addOnSuccessListener(snapshot -> {
                    List<Map<String, Object>> requests = new ArrayList<>();
                    for (QueryDocumentSnapshot document : snapshot) {
                        if (!"pending".equals(document.getString("status"))) continue;
                        Map<String, Object> item = new HashMap<>();
                        item.put("requestDocumentId", document.getId());
                        item.put("requestId", document.getString("requestId"));
                        item.put("childName", document.getString("childName"));
                        Timestamp createdAt = document.getTimestamp("createdAt");
                        item.put("createdAtMillis", createdAt == null
                                ? System.currentTimeMillis()
                                : createdAt.toDate().getTime());
                        requests.add(item);
                    }
                    Map<String, Object> result = new HashMap<>();
                    result.put("requests", requests);
                    callback.onComplete(result, null);
                })
                .addOnFailureListener(error -> callback.onComplete(null, readableError(error)));
    }

    private static void respondToRequest(
            FirebaseFirestore db,
            String guardianUid,
            Map<String, Object> data,
            Callback callback) {
        String requestId = stringValue(data.get("requestId"));
        boolean approved = Boolean.TRUE.equals(data.get("approved"));
        long minutes = approved ? number(data.get("extraMinutes")) : 0;
        if (minutes < 0 || minutes > 120 || (approved && minutes == 0)) {
            callback.onComplete(null, "Choose an extra time allowance from 1 to 120 minutes.");
            return;
        }

        String childUid = stringValue(data.get("childUid"));
        if (childUid.isEmpty()) {
            callback.onComplete(null, "The approval request is invalid.");
            return;
        }
        DocumentReference request = db.collection("accessRequests").document(childUid);
        db.runTransaction(transaction -> {
            DocumentSnapshot snapshot = transaction.get(request);
            if (!snapshot.exists() || !requestId.equals(snapshot.getString("requestId"))) {
                throw new IllegalStateException("This approval request no longer exists.");
            }
            if (!guardianUid.equals(snapshot.getString("guardianUid"))) {
                throw new IllegalStateException("Only the selected trusted person can respond.");
            }
            if (!"pending".equals(snapshot.getString("status"))) {
                throw new IllegalStateException("This approval request has already been answered.");
            }
            transaction.update(request,
                    "status", approved ? "approved" : "denied",
                    "extraMinutes", minutes,
                    "respondedAt", FieldValue.serverTimestamp());
            return approved ? "approved" : "denied";
        }).addOnSuccessListener(status -> {
            Map<String, Object> result = new HashMap<>();
            result.put("status", status);
            result.put("extraMinutes", approved ? minutes : 0);
            callback.onComplete(result, null);
        }).addOnFailureListener(error -> callback.onComplete(null, readableError(error)));
    }

    private static void startSession(
            Context context,
            FirebaseFirestore db,
            String uid,
            Callback callback) {
        loadPolicy(context, db, uid, (policy, error) -> {
            if (error != null) {
                callback.onComplete(null, error);
                return;
            }
            long allowance = number(policy.get("allowanceMinutes"));
            long dailySeconds = number(policy.get("remainingSeconds"));
            long extraSeconds = number(policy.get("extraSeconds"));
            if (allowance == 0) {
                callback.onComplete(null, "Set today's allowance before opening social sites.");
                return;
            }
            String source = dailySeconds > 0 ? "daily" : "extra";
            long available = dailySeconds > 0 ? dailySeconds : extraSeconds;
            if (available <= 0) {
                callback.onComplete(null, "Today's allowance is used up. Request approval for more time.");
                return;
            }
            Map<String, Object> result = new HashMap<>();
            result.put("sessionId", UUID.randomUUID().toString());
            result.put("allowedSeconds", Math.min(15 * 60, available));
            result.put("source", source);
            callback.onComplete(result, null);
        });
    }

    private static Map<String, Object> result(String key, Object value) {
        Map<String, Object> result = new HashMap<>();
        result.put(key, value);
        return result;
    }

    private static long number(Object value) {
        return value instanceof Number ? ((Number) value).longValue() : 0;
    }

    private static String stringValue(Object value) {
        return value instanceof String ? (String) value : "";
    }

    private static String readableError(Exception error) {
        if (error == null) return "The Firebase request did not complete.";
        String message = error.getLocalizedMessage();
        return TextUtils.isEmpty(message) ? "Check your internet connection and Firebase setup." : message;
    }
}
