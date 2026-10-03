package com.calmfeed;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.os.Vibrator;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public final class MindfulBreathingHelper {

    private MindfulBreathingHelper() {}

    public static void show(Activity activity, Runnable onCompleted) {
        if (activity == null || activity.isFinishing()) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        LinearLayout root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setBackgroundColor(Color.rgb(15, 23, 42)); // Deep calm midnight
        int p = dp(activity, 20);
        root.setPadding(p, p, p, p);

        // Header Title
        TextView title = new TextView(activity);
        title.setText("🧘 1-Minute Mindful Reset");
        title.setTextSize(18);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setTextColor(Color.rgb(52, 211, 153)); // Emerald
        title.setGravity(Gravity.CENTER);
        root.addView(title);

        TextView sub = new TextView(activity);
        sub.setText("Box Breathing: 4s Inhale • 4s Hold • 4s Exhale • 4s Rest");
        sub.setTextSize(12);
        sub.setTextColor(Color.rgb(148, 163, 184)); // Slate-400
        sub.setGravity(Gravity.CENTER);
        sub.setPadding(0, dp(activity, 4), 0, dp(activity, 14));
        root.addView(sub);

        // Breathing Circle Visual View
        BreathingCircleView circleView = new BreathingCircleView(activity);
        int circleSize = dp(activity, 160);
        LinearLayout.LayoutParams circleParams = new LinearLayout.LayoutParams(circleSize, circleSize);
        circleParams.gravity = Gravity.CENTER_HORIZONTAL;
        root.addView(circleView, circleParams);

        // Phase prompt
        TextView phaseText = new TextView(activity);
        phaseText.setText("Get ready to breathe...");
        phaseText.setTextSize(15);
        phaseText.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        phaseText.setTextColor(Color.rgb(241, 245, 249));
        phaseText.setGravity(Gravity.CENTER);
        phaseText.setPadding(0, dp(activity, 16), 0, dp(activity, 4));
        root.addView(phaseText);

        // Cycle Counter
        TextView cycleText = new TextView(activity);
        cycleText.setText("Cycle 1 of 4 • 60 seconds");
        cycleText.setTextSize(12);
        cycleText.setTextColor(Color.rgb(56, 189, 248)); // Cyan
        cycleText.setGravity(Gravity.CENTER);
        root.addView(cycleText);

        // Close / Done button
        Button actionBtn = new Button(activity);
        actionBtn.setText("Finish Early");
        actionBtn.setAllCaps(false);
        actionBtn.setTextSize(13);
        actionBtn.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        actionBtn.setTextColor(Color.rgb(203, 213, 225));
        GradientDrawable btnBg = new GradientDrawable();
        btnBg.setColor(Color.rgb(30, 41, 59));
        btnBg.setCornerRadius(dp(activity, 12));
        actionBtn.setBackground(btnBg);

        LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(activity, 44));
        btnParams.topMargin = dp(activity, 16);
        root.addView(actionBtn, btnParams);

        AlertDialog dialog = builder.setView(root).create();

        // Animation and Breathing Engine
        Handler handler = new Handler(Looper.getMainLooper());
        final int TOTAL_CYCLES = 4;
        final int[] currentCycle = {1};
        final int[] phase = {0}; // 0: Inhale(4s), 1: Hold(4s), 2: Exhale(4s), 3: Rest(4s)
        final ValueAnimator animator = ValueAnimator.ofFloat(0.4f, 1.0f);
        animator.setDuration(4000);
        animator.setInterpolator(new AccelerateDecelerateInterpolator());

        Vibrator vibrator = (Vibrator) activity.getSystemService(Context.VIBRATOR_SERVICE);

        Runnable triggerHaptic = () -> {
            try {
                if (vibrator != null && vibrator.hasVibrator()) {
                    vibrator.vibrate(35);
                }
            } catch (Exception ignored) {}
        };

        Runnable phaseTick = new Runnable() {
            @Override
            public void run() {
                if (activity.isFinishing()) return;

                triggerHaptic.run();

                switch (phase[0]) {
                    case 0: // INHALE
                        phaseText.setText("🌿 Inhale slowly through your nose");
                        phaseText.setTextColor(Color.rgb(52, 211, 153));
                        circleView.setPhaseColor(Color.rgb(16, 185, 129));
                        cycleText.setText("Cycle " + currentCycle[0] + " of " + TOTAL_CYCLES + " • Inhale (4s)");
                        animator.removeAllUpdateListeners();
                        animator.setFloatValues(0.45f, 1.0f);
                        animator.addUpdateListener(a -> circleView.setScale((float) a.getAnimatedValue()));
                        animator.start();
                        phase[0] = 1;
                        handler.postDelayed(this, 4000);
                        break;

                    case 1: // HOLD after inhale
                        phaseText.setText("✨ Hold your breath gently");
                        phaseText.setTextColor(Color.rgb(56, 189, 248));
                        circleView.setPhaseColor(Color.rgb(14, 165, 233));
                        cycleText.setText("Cycle " + currentCycle[0] + " of " + TOTAL_CYCLES + " • Hold (4s)");
                        circleView.setScale(1.0f);
                        phase[0] = 2;
                        handler.postDelayed(this, 4000);
                        break;

                    case 2: // EXHALE
                        phaseText.setText("🍃 Exhale completely through your mouth");
                        phaseText.setTextColor(Color.rgb(192, 132, 252));
                        circleView.setPhaseColor(Color.rgb(168, 85, 247));
                        cycleText.setText("Cycle " + currentCycle[0] + " of " + TOTAL_CYCLES + " • Exhale (4s)");
                        animator.removeAllUpdateListeners();
                        animator.setFloatValues(1.0f, 0.45f);
                        animator.addUpdateListener(a -> circleView.setScale((float) a.getAnimatedValue()));
                        animator.start();
                        phase[0] = 3;
                        handler.postDelayed(this, 4000);
                        break;

                    case 3: // REST / HOLD after exhale
                        phaseText.setText("🧘 Rest in quiet stillness");
                        phaseText.setTextColor(Color.rgb(251, 191, 36));
                        circleView.setPhaseColor(Color.rgb(245, 158, 11));
                        cycleText.setText("Cycle " + currentCycle[0] + " of " + TOTAL_CYCLES + " • Rest (4s)");
                        circleView.setScale(0.45f);

                        if (currentCycle[0] >= TOTAL_CYCLES) {
                            // Completed all 4 cycles!
                            handler.postDelayed(() -> {
                                if (activity.isFinishing()) return;
                                triggerHaptic.run();
                                phaseText.setText("🎉 Mind is calm, peaceful & refreshed!");
                                phaseText.setTextColor(Color.rgb(52, 211, 153));
                                cycleText.setText("Dopamine reset complete • Great job!");
                                actionBtn.setText("Return with Clarity ➔");
                                GradientDrawable ab = new GradientDrawable();
                                ab.setColor(Color.rgb(16, 185, 129));
                                ab.setCornerRadius(dp(activity, 12));
                                actionBtn.setBackground(ab);
                                actionBtn.setTextColor(Color.WHITE);
                                if (onCompleted != null) onCompleted.run();
                            }, 4000);
                        } else {
                            currentCycle[0]++;
                            phase[0] = 0;
                            handler.postDelayed(this, 4000);
                        }
                        break;
                }
            }
        };

        // Start breathing rhythm after brief 1s pause
        handler.postDelayed(phaseTick, 1000);

        actionBtn.setOnClickListener(v -> {
            handler.removeCallbacks(phaseTick);
            animator.cancel();
            dialog.dismiss();
            if (onCompleted != null && currentCycle[0] >= TOTAL_CYCLES) {
                onCompleted.run();
            }
        });

        dialog.setOnDismissListener(d -> {
            handler.removeCallbacks(phaseTick);
            animator.cancel();
        });

        dialog.show();
    }

    private static int dp(Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }

    private static class BreathingCircleView extends View {
        private final Paint innerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint outerGlowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private float scale = 0.45f;
        private int currentColor = Color.rgb(16, 185, 129);

        public BreathingCircleView(Context context) {
            super(context);
            innerPaint.setStyle(Paint.Style.FILL);
            outerGlowPaint.setStyle(Paint.Style.STROKE);
            outerGlowPaint.setStrokeWidth(dp(context, 4));
        }

        public void setScale(float s) {
            this.scale = Math.max(0.2f, Math.min(1.0f, s));
            invalidate();
        }

        public void setPhaseColor(int color) {
            this.currentColor = color;
            invalidate();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float cx = getWidth() / 2f;
            float cy = getHeight() / 2f;
            float maxR = Math.min(cx, cy) - dp(getContext(), 8);
            float currentR = maxR * scale;

            // Outer subtle wave ring
            outerGlowPaint.setColor(currentColor);
            outerGlowPaint.setAlpha(60);
            canvas.drawCircle(cx, cy, currentR + dp(getContext(), 6), outerGlowPaint);

            // Core filled calm circle
            innerPaint.setColor(currentColor);
            innerPaint.setAlpha(180);
            canvas.drawCircle(cx, cy, currentR, innerPaint);
        }

        private int dp(Context context, int value) {
            return Math.round(value * context.getResources().getDisplayMetrics().density);
        }
    }
}
