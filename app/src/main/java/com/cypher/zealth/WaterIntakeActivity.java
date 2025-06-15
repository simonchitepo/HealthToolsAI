package com.cypher.zealth;

import android.Manifest;
import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.InputType;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Space;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.core.widget.NestedScrollView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import android.content.res.ColorStateList;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.Random;

public class WaterIntakeActivity extends AppCompatActivity {


    private static final String PREF = "water_pref";

    private static final String KEY_GOAL_ML = "daily_goal_ml";
    private static final String KEY_LAST_CUSTOM_ML = "last_custom_ml";

    private static final String KEY_REMINDERS_ENABLED = "reminders_enabled";
    private static final String KEY_REMINDER_START_MIN = "reminder_start_min";
    private static final String KEY_REMINDER_END_MIN = "reminder_end_min";
    private static final String KEY_REMINDER_INTERVAL_MIN = "reminder_interval_min";
    private static final String KEY_REMINDER_TITLE = "reminder_title";
    private static final String KEY_REMINDER_BODY = "reminder_body";

    private static final int DEFAULT_GOAL_ML = 2000;
    private static final int DEFAULT_LAST_CUSTOM_ML = 200;

    private static final int DEFAULT_START_MIN = 9 * 60; 
    private static final int DEFAULT_END_MIN = 21 * 60;    
    private static final int DEFAULT_INTERVAL_MIN = 90;    

    private static final String CHANNEL_ID = "water_reminders";
    private static final String CHANNEL_NAME = "Water reminders";
    private static final int NOTIF_ID = 22001;

    private static final int REQ_CODE_ALARM = 99101;

    private SharedPreferences sp;

    private final SimpleDateFormat keyFmt = new SimpleDateFormat("yyyyMMdd", Locale.US);
    private final SimpleDateFormat dayFmt = new SimpleDateFormat("EEE, dd MMM", Locale.US);
    private String keyToday;
    private int consumedToday;
    private int dailyGoalMl;

    private int lastAddedMl = 0;

  
    private View root;

    private MaterialToolbar toolbar;

    private TextView tvToday, tvBigMl, tvRemaining, tvGoalLabel, tvTip;
    private Chip chipPercent;
    private Chip chipStreak;
    private LinearProgressIndicator progressBar;
    private LinearLayout historyContainer;

    private MaterialButton btnAdd150, btnAdd250, btnAdd330, btnAdd500, btnAdd750, btnAddCustom;
    private MaterialButton btnReset, btnSetGoal;

    private SwitchMaterial switchReminders;
    private TextView tvReminderStatus, tvReminderWindow, tvReminderInterval;
    private MaterialButtonToggleGroup reminderToggleGroup;
    private MaterialButton segStart, segEnd, segInterval;
    private MaterialButton btnReminderTest;
    private MaterialButton btnNotificationSettings;

    private ActivityResultLauncher<String> notifPermissionLauncher;

    private final Handler uiHandler = new Handler(Looper.getMainLooper());

    private static final int C_BG = 0x80F4F6FF;
    private static final int C_SURFACE = 0xFFFFFFFF;
    private static final int C_SURFACE_2 = 0xFFFDFDFF;

    private static final int C_TEXT = 0xFF0F172A;
    private static final int C_SUBTEXT = 0xFF64748B;
    private static final int C_OUTLINE = 0xFFE2E8F0;

    private static final int C_PRIMARY = 0xFF2563EB;
    private static final int C_PRIMARY_SOFT = 0xFFE0ECFF;
    private static final int C_PRIMARY_TEXT = 0xFF1E3A8A;

    private static final int C_SUCCESS = 0xFF16A34A;
    private static final int C_SUCCESS_SOFT = 0xFFDCFCE7;

    private static final int C_TRACK = 0xFFEFF4FF;

    private static final int C_DANGER = 0xFFDC2626;
    private static final int C_DANGER_SOFT = 0xFFFEE2E2;

    private static final int C_STREAK_SOFT = 0xFFFFF7ED;
    private static final int C_STREAK_TEXT = 0xFF9A3412;

    private MaterialButton lastHighlightedQuick = null;
    private final Runnable clearQuickHighlight = new Runnable() {
        @Override public void run() {
            if (lastHighlightedQuick != null) {
                applyQuickButtonStyle(lastHighlightedQuick, false);
                lastHighlightedQuick = null;
            }
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sp = getSharedPreferences(PREF, MODE_PRIVATE);

        notifPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                granted -> {
                    if (granted) {
                        ensureNotificationChannel();
                        Snackbar.make(root, "Notifications enabled.", Snackbar.LENGTH_SHORT).show();
                        if (isRemindersEnabled()) scheduleNextReminder(this, false);
                    } else {
                        Snackbar.make(root, "Notification permission denied. Reminders may not appear.", Snackbar.LENGTH_LONG).show();
                    }
                }
        );

        setContentView(buildUi());
        wireActions();

        renderTip();
        refreshTodayKeyAndLoad();
        renderAll();

        ensureNotificationChannel();
        if (isRemindersEnabled()) scheduleNextReminder(this, false);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshTodayKeyAndLoad();
        renderAll();
    }

    private View buildUi() {
        FrameLayout frame = new FrameLayout(this);
        frame.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
        ));
        frame.setBackgroundColor(C_BG);
        root = frame;

        CoordinatorLayout coordinator = new CoordinatorLayout(this);
        coordinator.setLayoutParams(new CoordinatorLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
        ));

        MaterialCardView toolbarCard = card();
        CoordinatorLayout.LayoutParams tlp = new CoordinatorLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        );
        tlp.setMargins(dp(16), dp(12), dp(16), 0);
        toolbarCard.setLayoutParams(tlp);

        toolbar = new MaterialToolbar(this);
        toolbar.setTitle("Water Intake");
        toolbar.setTitleTextColor(C_TEXT);
        toolbar.setNavigationIcon(R.drawable.ic_back_medical);
        toolbar.setNavigationIconTint(C_TEXT);
        toolbar.setBackgroundColor(0x00000000);
        toolbarCard.addView(toolbar);

        NestedScrollView scroll = new NestedScrollView(this);
        CoordinatorLayout.LayoutParams slp = new CoordinatorLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
        );
        slp.topMargin = dp(92);
        scroll.setLayoutParams(slp);
        scroll.setFillViewport(true);
        scroll.setPadding(dp(16), dp(16), dp(16), dp(18));
        scroll.setClipToPadding(false);
        scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        MaterialCardView summary = card();
        LinearLayout summaryInner = vStack(dp(16));

        LinearLayout topRow = hRowCenter();
        tvToday = text("Today", 16, C_TEXT, true);
        LinearLayout.LayoutParams tvTodayLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        tvToday.setLayoutParams(tvTodayLp);

        chipPercent = new Chip(this);
        chipPercent.setText("0%");
        chipPercent.setTextColor(Color.WHITE);
        chipPercent.setChipBackgroundColor(ColorStateList.valueOf(C_PRIMARY));
        chipPercent.setCheckable(false);
        chipPercent.setClickable(false);

        topRow.addView(tvToday);
        topRow.addView(chipPercent);

        tvBigMl = text("0 ml", 32, C_TEXT, true);
        LinearLayout.LayoutParams bigLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        );
        bigLp.topMargin = dp(10);
        tvBigMl.setLayoutParams(bigLp);

        tvRemaining = text("0 ml left", 14, C_SUBTEXT, false);
        LinearLayout.LayoutParams remLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        );
        remLp.topMargin = dp(6);
        tvRemaining.setLayoutParams(remLp);

        progressBar = new LinearProgressIndicator(this);
        LinearLayout.LayoutParams barLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(10)
        );
        barLp.topMargin = dp(12);
        progressBar.setLayoutParams(barLp);
        progressBar.setMax(100);
        progressBar.setTrackCornerRadius(dp(12));
        progressBar.setIndicatorColor(C_PRIMARY);
        progressBar.setTrackColor(C_TRACK);

        LinearLayout goalRow = hRowCenter();
        LinearLayout.LayoutParams goalRowLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        );
        goalRowLp.topMargin = dp(14);
        goalRow.setLayoutParams(goalRowLp);

        tvGoalLabel = text("Daily goal: 2000 ml", 13, C_SUBTEXT, false);
        LinearLayout.LayoutParams tvGoalLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        tvGoalLabel.setLayoutParams(tvGoalLp);

        btnSetGoal = pillButton("Edit goal", C_PRIMARY_SOFT, C_PRIMARY_TEXT);
        btnSetGoal.setMinHeight(dp(44));

        goalRow.addView(tvGoalLabel);
        goalRow.addView(btnSetGoal);

        summaryInner.addView(topRow);
        summaryInner.addView(tvBigMl);
        summaryInner.addView(tvRemaining);
        summaryInner.addView(progressBar);
        summaryInner.addView(goalRow);
        summary.addView(summaryInner);

   
        MaterialCardView quick = card();
        LinearLayout.LayoutParams qlp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        );
        qlp.topMargin = dp(12);
        quick.setLayoutParams(qlp);

        LinearLayout quickInner = vStack(dp(16));
        LinearLayout quickTop = hRowCenter();

        TextView quickTitle = text("Quick add", 14, C_TEXT, true);
        LinearLayout.LayoutParams qtLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        quickTitle.setLayoutParams(qtLp);

        chipStreak = new Chip(this);
        chipStreak.setText("Streak • 0 days");
        chipStreak.setTextColor(C_STREAK_TEXT);
        chipStreak.setChipBackgroundColor(ColorStateList.valueOf(C_STREAK_SOFT));
        chipStreak.setCheckable(false);
        chipStreak.setClickable(false);

        quickTop.addView(quickTitle);
        quickTop.addView(chipStreak);

        LinearLayout row1 = hRow();
        LinearLayout row2 = hRow();
        row1.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        LinearLayout.LayoutParams r2lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        );
        r2lp.topMargin = dp(10);
        row2.setLayoutParams(r2lp);

        btnAdd150 = quickSegment("+150 ml");
        btnAdd250 = quickSegment("+250 ml");
        btnAdd330 = quickSegment("+330 ml");
        btnAdd500 = quickSegment("+500 ml");
        btnAdd750 = quickSegment("+750 ml");
        btnAddCustom = quickSegment("+ Custom");

        row1.addView(weighted(btnAdd150));
        row1.addView(spaceW(dp(10)));
        row1.addView(weighted(btnAdd250));
        row1.addView(spaceW(dp(10)));
        row1.addView(weighted(btnAdd330));

        row2.addView(weighted(btnAdd500));
        row2.addView(spaceW(dp(10)));
        row2.addView(weighted(btnAdd750));
        row2.addView(spaceW(dp(10)));
        row2.addView(weighted(btnAddCustom));

        btnReset = dangerOutlinedButton("Reset today");
        LinearLayout.LayoutParams rslp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(52)
        );
        rslp.topMargin = dp(12);
        btnReset.setLayoutParams(rslp);

        quickInner.addView(quickTop);
        quickInner.addView(row1);
        quickInner.addView(row2);
        quickInner.addView(btnReset);
        quick.addView(quickInner);

      
        MaterialCardView reminders = card();
        LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        );
        rlp.topMargin = dp(12);
        reminders.setLayoutParams(rlp);

        LinearLayout remInner = vStack(dp(16));

        LinearLayout remTop = hRowCenter();
        TextView remTitle = text("Reminders", 14, C_TEXT, true);
        LinearLayout.LayoutParams remTitleLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        remTitle.setLayoutParams(remTitleLp);

        switchReminders = new SwitchMaterial(this);
        switchReminders.setText(" ");
        switchReminders.setShowText(false);

        remTop.addView(remTitle);
        remTop.addView(switchReminders);

        tvReminderStatus = text("Off", 13, C_SUBTEXT, false);
        LinearLayout.LayoutParams remStatusLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        );
        remStatusLp.topMargin = dp(8);
        tvReminderStatus.setLayoutParams(remStatusLp);

        tvReminderWindow = text("Window: 09:00 – 21:00", 13, C_SUBTEXT, false);
        tvReminderInterval = text("Repeat: every 90 min", 13, C_SUBTEXT, false);

        reminderToggleGroup = new MaterialButtonToggleGroup(this);
        LinearLayout.LayoutParams tglLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        );
        tglLp.topMargin = dp(12);
        reminderToggleGroup.setLayoutParams(tglLp);
        reminderToggleGroup.setSingleSelection(true);
        reminderToggleGroup.setSelectionRequired(false);

        segStart = segmentButton("Start");
        segEnd = segmentButton("End");
        segInterval = segmentButton("Interval");

        reminderToggleGroup.addView(segStart);
        reminderToggleGroup.addView(segEnd);
        reminderToggleGroup.addView(segInterval);

        btnReminderTest = primaryFullWidthButton("Send test notification");
        LinearLayout.LayoutParams testLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(52)
        );
        testLp.topMargin = dp(12);
        btnReminderTest.setLayoutParams(testLp);

        btnNotificationSettings = filledSoftButton("Notification settings");
        LinearLayout.LayoutParams sysLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(52)
        );
        sysLp.topMargin = dp(10);
        btnNotificationSettings.setLayoutParams(sysLp);

        remInner.addView(remTop);
        remInner.addView(tvReminderStatus);
        remInner.addView(tvReminderWindow);
        remInner.addView(tvReminderInterval);
        remInner.addView(reminderToggleGroup);
        remInner.addView(btnReminderTest);
        remInner.addView(btnNotificationSettings);
        reminders.addView(remInner);

  
        MaterialCardView tip = card();
        LinearLayout.LayoutParams tlp2 = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        );
        tlp2.topMargin = dp(12);
        tip.setLayoutParams(tlp2);

        LinearLayout tipInner = vStack(dp(16));
        TextView tipTitle = text("Hydration tip", 14, C_TEXT, true);
        tvTip = text("Tip text", 13, C_SUBTEXT, false);
        LinearLayout.LayoutParams tipTextLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        );
        tipTextLp.topMargin = dp(6);
        tvTip.setLayoutParams(tipTextLp);

        tipInner.addView(tipTitle);
        tipInner.addView(tvTip);
        tip.addView(tipInner);

       
        MaterialCardView history = card();
        LinearLayout.LayoutParams hlp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        );
        hlp.topMargin = dp(12);
        history.setLayoutParams(hlp);

        LinearLayout histInner = vStack(dp(16));
        TextView histTitle = text("Last 7 days", 14, C_TEXT, true);

        historyContainer = new LinearLayout(this);
        historyContainer.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams hclp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        );
        hclp.topMargin = dp(10);
        historyContainer.setLayoutParams(hclp);

        histInner.addView(histTitle);
        histInner.addView(historyContainer);
        history.addView(histInner);

        Space bottom = new Space(this);
        bottom.setLayoutParams(new LinearLayout.LayoutParams(1, dp(18)));

        content.addView(summary);
        content.addView(quick);
        content.addView(reminders);
        content.addView(tip);
        content.addView(history);
        content.addView(bottom);

        scroll.addView(content);

        coordinator.addView(toolbarCard);
        coordinator.addView(scroll);

        frame.addView(coordinator);
        return frame;
    }

    private void wireActions() {
        toolbar.setNavigationOnClickListener(v -> finish());

        btnAdd150.setOnClickListener(v -> quickAddPressed(btnAdd150, 150));
        btnAdd250.setOnClickListener(v -> quickAddPressed(btnAdd250, 250));
        btnAdd330.setOnClickListener(v -> quickAddPressed(btnAdd330, 330));
        btnAdd500.setOnClickListener(v -> quickAddPressed(btnAdd500, 500));
        btnAdd750.setOnClickListener(v -> quickAddPressed(btnAdd750, 750));
        btnAddCustom.setOnClickListener(v -> {
            flashQuickHighlight(btnAddCustom);
            v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
            showCustomAddDialog();
        });

        btnSetGoal.setOnClickListener(v -> showSetGoalDialog());
        btnReset.setOnClickListener(v -> resetTodayWithConfirm());

        switchReminders.setChecked(isRemindersEnabled());
        switchReminders.setOnCheckedChangeListener((buttonView, isChecked) -> {
            setRemindersEnabled(isChecked);

            if (isChecked) {
                ensureNotificationPermissionIfNeeded();
                ensureNotificationChannel();
                scheduleNextReminder(this, true);
                Snackbar.make(root, "Reminders enabled.", Snackbar.LENGTH_SHORT).show();
            } else {
                cancelReminders(this);
                Snackbar.make(root, "Reminders disabled.", Snackbar.LENGTH_SHORT).show();
            }

            renderReminderUi();
        });

        reminderToggleGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) return;

            if (!isRemindersEnabled()) {
                Snackbar.make(root, "Enable reminders first.", Snackbar.LENGTH_SHORT).show();
                group.clearChecked();
                return;
            }

            View tapped = group.findViewById(checkedId);
            if (tapped != null) tapped.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);

            if (checkedId == segStart.getId()) pickTime(true);
            else if (checkedId == segEnd.getId()) pickTime(false);
            else if (checkedId == segInterval.getId()) pickInterval();

            group.postDelayed(group::clearChecked, 180);
        });

        btnReminderTest.setOnClickListener(v -> {
            ensureNotificationPermissionIfNeeded();
            ensureNotificationChannel();
            v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
            sendReminderNotification(this, true);
        });

        btnNotificationSettings.setOnClickListener(v -> openSystemNotificationSettings());
    }

    private void quickAddPressed(MaterialButton b, int ml) {
        flashQuickHighlight(b);
        b.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
        add(ml);
    }

    private void flashQuickHighlight(MaterialButton b) {
        if (b == null) return;

        uiHandler.removeCallbacks(clearQuickHighlight);

        if (lastHighlightedQuick != null && lastHighlightedQuick != b) {
            applyQuickButtonStyle(lastHighlightedQuick, false);
        }

        applyQuickButtonStyle(b, true);
        lastHighlightedQuick = b;

        uiHandler.postDelayed(clearQuickHighlight, 220);
    }

    private void applyQuickButtonStyle(MaterialButton b, boolean highlighted) {
        if (b == null) return;

        if (highlighted) {
            b.setBackgroundTintList(ColorStateList.valueOf(C_PRIMARY_SOFT));
            b.setStrokeColor(ColorStateList.valueOf(0xFFA7C5FF));
            b.setTextColor(C_PRIMARY_TEXT);
        } else {
            b.setBackgroundTintList(ColorStateList.valueOf(C_SURFACE));
            b.setStrokeColor(ColorStateList.valueOf(0xFFD5DAE6));
            b.setTextColor(C_TEXT);
        }
    }

  
    private void refreshTodayKeyAndLoad() {
        keyToday = "water_" + keyFmt.format(Calendar.getInstance().getTime());
        dailyGoalMl = sp.getInt(KEY_GOAL_ML, DEFAULT_GOAL_ML);
        consumedToday = sp.getInt(keyToday, 0);
    }

    private void renderAll() {
        renderHeader();
        renderProgress();
        renderStreak();
        renderHistory7Days();
        renderReminderUi();
    }

    private void renderHeader() {
        String todayLabel = "Today • " + dayFmt.format(Calendar.getInstance().getTime());
        tvToday.setText(todayLabel);
        tvGoalLabel.setText("Daily goal: " + dailyGoalMl + " ml");
        btnSetGoal.setText("Edit goal");
    }

    private void renderProgress() {
        tvBigMl.setText(consumedToday + " ml");

        int remaining = dailyGoalMl - consumedToday;

        if (remaining > 0) {
            tvRemaining.setText(remaining + " ml left");
            tvRemaining.setTextColor(C_SUBTEXT);

            chipPercent.setChipBackgroundColor(ColorStateList.valueOf(C_PRIMARY));
            chipPercent.setTextColor(Color.WHITE);

            progressBar.setIndicatorColor(C_PRIMARY);
            progressBar.setTrackColor(C_TRACK);

        } else {
            int over = Math.abs(remaining);
            tvRemaining.setText(over + " ml over goal");
            tvRemaining.setTextColor(C_SUCCESS);

            chipPercent.setChipBackgroundColor(ColorStateList.valueOf(C_SUCCESS));
            chipPercent.setTextColor(Color.WHITE);

            progressBar.setIndicatorColor(C_SUCCESS);
            progressBar.setTrackColor(C_SUCCESS_SOFT);
        }

        int pct = (int) Math.round((consumedToday * 100.0) / Math.max(dailyGoalMl, 1));
        int pctForUi = Math.min(Math.max(pct, 0), 100);

        chipPercent.setText(pctForUi + "%");
        progressBar.setProgressCompat(pctForUi, true);
    }

    private void renderStreak() {
        int streak = calculateGoalStreakDays();
        chipStreak.setText("Streak • " + streak + " day" + (streak == 1 ? "" : "s"));
    }

    private void renderTip() {
        String[] tips = new String[]{
                "Try spacing drinks across the day if it helps you stay consistent.",
                "If your day is more active or warm, you can adjust your target if you want.",
                "Some people also log water-rich foods separately, if that’s useful for them.",
                "A simple routine can help: add a glass after waking and with meals.",
                "If you track drinks, consider logging what you actually choose—no judgment."
        };

        tvTip.setText(tips[new Random().nextInt(tips.length)]);
    }

    private void renderHistory7Days() {
        historyContainer.removeAllViews();

        for (int i = 6; i >= 0; i--) {
            Calendar c = Calendar.getInstance();
            c.add(Calendar.DAY_OF_YEAR, -i);

            String key = "water_" + keyFmt.format(c.getTime());
            int v = sp.getInt(key, 0);

            addHistoryRow(dayFmt.format(c.getTime()), v, dailyGoalMl);
        }
    }

    private void addHistoryRow(String dayLabel, int ml, int goal) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        LinearLayout top = hRowCenter();

        TextView tvDay = text(dayLabel, 13, C_TEXT, false);
        LinearLayout.LayoutParams lpDay = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        tvDay.setLayoutParams(lpDay);

        TextView tvVal = text(ml + " ml", 13, C_SUBTEXT, false);

        top.addView(tvDay);
        top.addView(tvVal);

        LinearProgressIndicator bar = new LinearProgressIndicator(this);
        LinearLayout.LayoutParams lpBar = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(8)
        );
        lpBar.topMargin = dp(6);
        bar.setLayoutParams(lpBar);

        int pct = (int) Math.round((ml * 100.0) / Math.max(goal, 1));
        pct = Math.min(Math.max(pct, 0), 100);

        bar.setTrackThickness(dp(6));
        bar.setProgressCompat(pct, false);
        bar.setTrackColor(C_OUTLINE);
        bar.setIndicatorColor(C_PRIMARY);
        bar.setTrackCornerRadius(dp(12));

        View spacer = new View(this);
        spacer.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(10)
        ));

        row.addView(top);
        row.addView(bar);
        row.addView(spacer);

        historyContainer.addView(row);
    }

    private void renderReminderUi() {
        boolean enabled = isRemindersEnabled();
        int start = sp.getInt(KEY_REMINDER_START_MIN, DEFAULT_START_MIN);
        int end = sp.getInt(KEY_REMINDER_END_MIN, DEFAULT_END_MIN);
        int interval = sp.getInt(KEY_REMINDER_INTERVAL_MIN, DEFAULT_INTERVAL_MIN);

        tvReminderStatus.setText(enabled ? "On" : "Off");
        tvReminderWindow.setText("Window: " + formatMinutes(start) + " – " + formatMinutes(end));
        tvReminderInterval.setText("Repeat: every " + interval + " min");

        btnReminderTest.setEnabled(true);
    }


    private void add(int ml) {
        if (ml <= 0) return;

        consumedToday = Math.max(0, consumedToday + ml);
        lastAddedMl = ml;

        sp.edit().putInt(keyToday, consumedToday).apply();
        renderAll();

        Snackbar.make(root, "+" + ml + " ml added", Snackbar.LENGTH_LONG)
                .setAction("Undo", v -> undoLastAdd())
                .show();
    }

    private void undoLastAdd() {
        if (lastAddedMl <= 0) return;

        consumedToday = Math.max(consumedToday - lastAddedMl, 0);
        sp.edit().putInt(keyToday, consumedToday).apply();
        renderAll();

        Snackbar.make(root, "Undone", Snackbar.LENGTH_SHORT).show();
        lastAddedMl = 0;
    }

    private void resetTodayWithConfirm() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Reset today?")
                .setMessage("This will set today’s water intake back to 0 ml.")
                .setNegativeButton("Cancel", (d, w) -> d.dismiss())
                .setPositiveButton("Reset", (d, w) -> resetToday())
                .show();
    }

    private void resetToday() {
        consumedToday = 0;
        lastAddedMl = 0;
        sp.edit().putInt(keyToday, 0).apply();
        renderAll();
        Snackbar.make(root, "Reset completed", Snackbar.LENGTH_SHORT).show();
    }


    private void showSetGoalDialog() {
        TextInputLayout til = new TextInputLayout(this);
        til.setHint("Daily goal (ml)");
        til.setHelperText("Set a daily target for personal tracking.");
        til.setHelperTextEnabled(true);

        til.setBoxBackgroundMode(TextInputLayout.BOX_BACKGROUND_OUTLINE);
        til.setBoxStrokeColor(C_OUTLINE);
        til.setBoxBackgroundColor(C_SURFACE);
        til.setPadding(dp(2), dp(6), dp(2), dp(2));

        TextInputEditText et = new TextInputEditText(this);
        et.setInputType(InputType.TYPE_CLASS_NUMBER);
        et.setText(String.valueOf(dailyGoalMl));
        et.setTextColor(C_TEXT);
        et.setHintTextColor(ColorStateList.valueOf(C_SUBTEXT));
        til.addView(et);

        androidx.appcompat.app.AlertDialog dialog =
                new MaterialAlertDialogBuilder(this)
                        .setTitle("Set daily goal")
                        .setView(til)
                        .setNegativeButton("Cancel", (d, w) -> d.dismiss())
                        .setPositiveButton("Save", null)
                        .create();

        dialog.setOnShowListener(d -> {
            android.widget.Button positive = dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE);
            positive.setOnClickListener(v -> {
                Integer goal = parseInt(et);
                if (goal == null || goal < 500 || goal > 6000) {
                    til.setError("Enter a valid goal (500–6000 ml)");
                    return;
                }
                til.setError(null);

                dailyGoalMl = goal;
                sp.edit().putInt(KEY_GOAL_ML, dailyGoalMl).apply();

                renderAll();
                Snackbar.make(root, "Goal updated", Snackbar.LENGTH_SHORT).show();
                dialog.dismiss();
            });
        });

        dialog.show();
    }

    private void showCustomAddDialog() {
        int last = sp.getInt(KEY_LAST_CUSTOM_ML, DEFAULT_LAST_CUSTOM_ML);

        TextInputLayout til = new TextInputLayout(this);
        til.setHint("Amount (ml)");
        til.setHelperText("Example: 200");
        til.setHelperTextEnabled(true);

        til.setBoxBackgroundMode(TextInputLayout.BOX_BACKGROUND_OUTLINE);
        til.setBoxStrokeColor(C_OUTLINE);
        til.setBoxBackgroundColor(C_SURFACE);
        til.setPadding(dp(2), dp(6), dp(2), dp(2));

        TextInputEditText et = new TextInputEditText(this);
        et.setInputType(InputType.TYPE_CLASS_NUMBER);
        et.setText(String.valueOf(last));
        et.setTextColor(C_TEXT);
        et.setHintTextColor(ColorStateList.valueOf(C_SUBTEXT));
        til.addView(et);

        androidx.appcompat.app.AlertDialog dialog =
                new MaterialAlertDialogBuilder(this)
                        .setTitle("Add custom amount")
                        .setView(til)
                        .setNegativeButton("Cancel", (d, w) -> d.dismiss())
                        .setPositiveButton("Add", null)
                        .create();

        dialog.setOnShowListener(d -> {
            android.widget.Button positive = dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE);
            positive.setOnClickListener(v -> {
                Integer ml = parseInt(et);
                if (ml == null || ml <= 0 || ml > 5000) {
                    til.setError("Enter a valid amount (1–5000 ml)");
                    return;
                }
                til.setError(null);

                sp.edit().putInt(KEY_LAST_CUSTOM_ML, ml).apply();
                add(ml);
                dialog.dismiss();
            });
        });

        dialog.show();
    }

   
    private boolean isRemindersEnabled() {
        return sp.getBoolean(KEY_REMINDERS_ENABLED, false);
    }

    private void setRemindersEnabled(boolean enabled) {
        sp.edit().putBoolean(KEY_REMINDERS_ENABLED, enabled).apply();
    }

    private void pickTime(boolean isStart) {
        int currentMin = sp.getInt(isStart ? KEY_REMINDER_START_MIN : KEY_REMINDER_END_MIN,
                isStart ? DEFAULT_START_MIN : DEFAULT_END_MIN);

        int hour = currentMin / 60;
        int minute = currentMin % 60;

        boolean is24 = DateFormat.is24HourFormat(this);

        TimePickerDialog dlg = new TimePickerDialog(this, (view, h, m) -> {
            int minOfDay = h * 60 + m;

            if (isStart) sp.edit().putInt(KEY_REMINDER_START_MIN, minOfDay).apply();
            else sp.edit().putInt(KEY_REMINDER_END_MIN, minOfDay).apply();

            normalizeReminderWindowIfNeeded();
            renderReminderUi();
            scheduleNextReminder(this, true);
        }, hour, minute, is24);

        dlg.setTitle(isStart ? "Start time" : "End time");
        dlg.show();
    }

    private void pickInterval() {
        final int[] options = new int[]{30, 45, 60, 90, 120, 180};
        String[] labels = new String[]{
                "Every 30 min", "Every 45 min", "Every 60 min", "Every 90 min", "Every 120 min", "Every 180 min"
        };

        new MaterialAlertDialogBuilder(this)
                .setTitle("Reminder interval")
                .setItems(labels, (d, which) -> {
                    int chosen = options[Math.max(0, Math.min(which, options.length - 1))];
                    sp.edit().putInt(KEY_REMINDER_INTERVAL_MIN, chosen).apply();
                    renderReminderUi();
                    scheduleNextReminder(this, true);
                })
                .setNegativeButton("Cancel", (d, w) -> d.dismiss())
                .show();
    }

    private void normalizeReminderWindowIfNeeded() {
        int start = sp.getInt(KEY_REMINDER_START_MIN, DEFAULT_START_MIN);
        int end = sp.getInt(KEY_REMINDER_END_MIN, DEFAULT_END_MIN);

        if (end <= start) {
            int newEnd = Math.min(start + 60, (23 * 60) + 59);
            sp.edit().putInt(KEY_REMINDER_END_MIN, newEnd).apply();
        }

        int interval = sp.getInt(KEY_REMINDER_INTERVAL_MIN, DEFAULT_INTERVAL_MIN);
        if (interval < 15) sp.edit().putInt(KEY_REMINDER_INTERVAL_MIN, 15).apply();
    }

    private void ensureNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= 33) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        }
    }

    private void ensureNotificationChannel() {
        if (Build.VERSION.SDK_INT < 26) return;

        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) return;

        NotificationChannel existing = nm.getNotificationChannel(CHANNEL_ID);
        if (existing != null) return;

        NotificationChannel ch = new NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT);
        ch.setDescription("Daily hydration reminders");
        nm.createNotificationChannel(ch);
    }

    private void openSystemNotificationSettings() {
        try {
            Intent intent = new Intent();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                intent.setAction(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
                intent.putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
            } else {
                intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                intent.setData(Uri.parse("package:" + getPackageName()));
            }
            startActivity(intent);
        } catch (Exception e) {
            Snackbar.make(root, "Unable to open notification settings.", Snackbar.LENGTH_SHORT).show();
        }
    }

  
    public static void scheduleNextReminder(Context context, boolean showToastOrSnack) {
        SharedPreferences sp = context.getSharedPreferences(PREF, MODE_PRIVATE);
        if (!sp.getBoolean(KEY_REMINDERS_ENABLED, false)) return;

        int start = sp.getInt(KEY_REMINDER_START_MIN, DEFAULT_START_MIN);
        int end = sp.getInt(KEY_REMINDER_END_MIN, DEFAULT_END_MIN);
        int interval = sp.getInt(KEY_REMINDER_INTERVAL_MIN, DEFAULT_INTERVAL_MIN);

        Calendar now = Calendar.getInstance();
        Calendar next = computeNextTrigger(now, start, end, interval);

        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;

        PendingIntent pi = reminderPendingIntent(context);
        long triggerAt = next.getTimeInMillis();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi);
        } else {
            am.set(AlarmManager.RTC_WAKEUP, triggerAt, pi);
        }
    }

    public static void cancelReminders(Context context) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;
        am.cancel(reminderPendingIntent(context));
    }

    private static PendingIntent reminderPendingIntent(Context context) {
        Intent i = new Intent(context, WaterIntakeActivity.ReminderReceiver.class);
        i.setAction("com.cypher.zealth.WATER_REMINDER");
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= 23) flags |= PendingIntent.FLAG_IMMUTABLE;
        return PendingIntent.getBroadcast(context, REQ_CODE_ALARM, i, flags);
    }

    private static Calendar computeNextTrigger(Calendar now, int startMin, int endMin, int intervalMin) {
        Calendar next = (Calendar) now.clone();

        int nowMin = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE);

        if (nowMin < startMin) {
            setToMinuteOfDay(next, startMin);
            next.set(Calendar.SECOND, 0);
            next.set(Calendar.MILLISECOND, 0);
            return next;
        }

        if (nowMin >= endMin) {
            next.add(Calendar.DAY_OF_YEAR, 1);
            setToMinuteOfDay(next, startMin);
            next.set(Calendar.SECOND, 0);
            next.set(Calendar.MILLISECOND, 0);
            return next;
        }

        int minutesFromStart = Math.max(0, nowMin - startMin);
        int buckets = (minutesFromStart / Math.max(intervalMin, 1)) + 1;
        int nextMin = startMin + (buckets * intervalMin);

        if (nextMin > endMin) {
            next.add(Calendar.DAY_OF_YEAR, 1);
            setToMinuteOfDay(next, startMin);
        } else {
            setToMinuteOfDay(next, nextMin);
        }

        next.set(Calendar.SECOND, 0);
        next.set(Calendar.MILLISECOND, 0);
        return next;
    }

    private static void setToMinuteOfDay(Calendar c, int minuteOfDay) {
        int h = minuteOfDay / 60;
        int m = minuteOfDay % 60;
        c.set(Calendar.HOUR_OF_DAY, h);
        c.set(Calendar.MINUTE, m);
    }

    public static void sendReminderNotification(Context context, boolean isTest) {
        SharedPreferences sp = context.getSharedPreferences(PREF, MODE_PRIVATE);

        String title = sp.getString(KEY_REMINDER_TITLE, "Hydration reminder");
        String body = sp.getString(KEY_REMINDER_BODY, "Time to drink water and stay on track with your goal.");

        if (TextUtils.isEmpty(title)) title = "Hydration reminder";
        if (TextUtils.isEmpty(body)) body = "Time to drink water and stay on track with your goal.";

        if (isTest) body = body + " (test)";

        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) return;

        NotificationCompat.Builder b = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        nm.notify(NOTIF_ID, b.build());
    }

    public static class ReminderReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            SharedPreferences sp = context.getSharedPreferences(PREF, MODE_PRIVATE);
            if (!sp.getBoolean(KEY_REMINDERS_ENABLED, false)) return;

            if (Build.VERSION.SDK_INT >= 26) {
                NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                if (nm != null && nm.getNotificationChannel(CHANNEL_ID) == null) {
                    NotificationChannel ch = new NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT);
                    ch.setDescription("Daily hydration reminders");
                    nm.createNotificationChannel(ch);
                }
            }

            sendReminderNotification(context, false);
            scheduleNextReminder(context, false);
        }
    }

    public static class BootReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null) return;
            String a = intent.getAction();
            if (Intent.ACTION_BOOT_COMPLETED.equals(a) || Intent.ACTION_LOCKED_BOOT_COMPLETED.equals(a)) {
                scheduleNextReminder(context, false);
            }
        }
    }

   
    private int calculateGoalStreakDays() {
        int streak = 0;
        for (int i = 0; i < 365; i++) {
            Calendar c = Calendar.getInstance();
            c.add(Calendar.DAY_OF_YEAR, -i);
            String key = "water_" + keyFmt.format(c.getTime());
            int v = sp.getInt(key, 0);

            if (v >= dailyGoalMl && dailyGoalMl > 0) streak++;
            else break;
        }
        return streak;
    }

    private Integer parseInt(TextInputEditText et) {
        if (et == null || et.getText() == null) return null;
        String s = et.getText().toString().trim();
        if (TextUtils.isEmpty(s)) return null;
        try {
            return Integer.parseInt(s);
        } catch (Exception ignored) {
            return null;
        }
    }

    private String formatMinutes(int minuteOfDay) {
        int h = minuteOfDay / 60;
        int m = minuteOfDay % 60;
        Calendar c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY, h);
        c.set(Calendar.MINUTE, m);
        java.text.DateFormat df = DateFormat.getTimeFormat(this);
        return df.format(c.getTime());
    }

    private MaterialCardView card() {
        MaterialCardView cv = new MaterialCardView(this);
        cv.setCardBackgroundColor(C_SURFACE_2);
        cv.setCardElevation(dp(8));
        cv.setRadius(dp(26));
        cv.setStrokeColor(C_OUTLINE);
        cv.setStrokeWidth(dp(1));
        cv.setUseCompatPadding(true);
        cv.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        return cv;
    }

    private LinearLayout vStack(int pad) {
        LinearLayout ll = new LinearLayout(this);
        ll.setOrientation(LinearLayout.VERTICAL);
        ll.setPadding(pad, pad, pad, pad);
        ll.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        return ll;
    }

    private LinearLayout hRow() {
        LinearLayout ll = new LinearLayout(this);
        ll.setOrientation(LinearLayout.HORIZONTAL);
        ll.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        return ll;
    }

    private LinearLayout hRowCenter() {
        LinearLayout ll = hRow();
        ll.setGravity(Gravity.CENTER_VERTICAL);
        return ll;
    }

    private TextView text(String s, int spSize, int color, boolean bold) {
        TextView tv = new TextView(this);
        tv.setText(s);
        tv.setTextSize(spSize);
        tv.setTextColor(color);
        if (bold) tv.setTypeface(tv.getTypeface(), android.graphics.Typeface.BOLD);
        return tv;
    }

    private MaterialButton pillButton(String label, int bgColor, int textColor) {
        MaterialButton b = new MaterialButton(this);
        b.setText(label);
        b.setAllCaps(false);
        b.setTextColor(textColor);
        b.setCornerRadius(dp(999));
        b.setBackgroundTintList(ColorStateList.valueOf(bgColor));
        b.setStrokeColor(ColorStateList.valueOf(0xFFA7C5FF));
        b.setStrokeWidth(dp(1));
        b.setMinHeight(dp(48));
        b.setPadding(dp(18), dp(12), dp(18), dp(12));
        return b;
    }

    private MaterialButton filledSoftButton(String label) {
        MaterialButton b = new MaterialButton(this);
        b.setText(label);
        b.setAllCaps(false);
        b.setTextColor(C_PRIMARY_TEXT);
        b.setCornerRadius(dp(18));
        b.setBackgroundTintList(ColorStateList.valueOf(C_PRIMARY_SOFT));
        b.setStrokeColor(ColorStateList.valueOf(0xFFA7C5FF));
        b.setStrokeWidth(dp(1));
        b.setMinHeight(dp(52));
        b.setPadding(dp(16), dp(12), dp(16), dp(12));
        return b;
    }

    private MaterialButton primaryFullWidthButton(String label) {
        MaterialButton b = new MaterialButton(this);
        b.setText(label);
        b.setAllCaps(false);
        b.setTextColor(Color.WHITE);
        b.setCornerRadius(dp(18));
        b.setBackgroundTintList(ColorStateList.valueOf(C_PRIMARY));
        b.setMinHeight(dp(52));
        b.setPadding(dp(16), dp(12), dp(16), dp(12));
        return b;
    }

    private MaterialButton quickSegment(String label) {
        MaterialButton b = new MaterialButton(
                this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle
        );
        b.setId(View.generateViewId());
        b.setText(label);
        b.setAllCaps(false);
        b.setCornerRadius(dp(18));
        b.setMinHeight(dp(52));
        b.setPadding(dp(12), dp(12), dp(12), dp(12));
        b.setStrokeColor(ColorStateList.valueOf(0xFFD5DAE6));
        b.setStrokeWidth(dp(1));
        b.setBackgroundTintList(ColorStateList.valueOf(C_SURFACE));
        b.setTextColor(C_TEXT);
        return b;
    }

    private MaterialButton dangerOutlinedButton(String label) {
        MaterialButton b = new MaterialButton(
                this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle
        );
        b.setText(label);
        b.setAllCaps(false);
        b.setCornerRadius(dp(18));
        b.setMinHeight(dp(52));
        b.setPadding(dp(16), dp(12), dp(16), dp(12));
        b.setTextColor(C_DANGER);
        b.setStrokeColor(ColorStateList.valueOf(C_DANGER));
        b.setStrokeWidth(dp(1));
        b.setBackgroundTintList(ColorStateList.valueOf(C_DANGER_SOFT));
        return b;
    }

    private MaterialButton segmentButton(String label) {
        MaterialButton b = new MaterialButton(
                this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle
        );

        b.setId(View.generateViewId());
        b.setText(label);
        b.setAllCaps(false);
        b.setCornerRadius(dp(16));
        b.setMinHeight(dp(48));
        b.setPadding(dp(12), dp(12), dp(12), dp(12));
        b.setCheckable(true);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f
        );
        lp.rightMargin = dp(8);
        b.setLayoutParams(lp);

        int[][] states = new int[][]{
                new int[]{android.R.attr.state_checked},
                new int[]{-android.R.attr.state_checked}
        };
        int[] bg = new int[]{C_PRIMARY_SOFT, C_SURFACE};
        int[] text = new int[]{C_PRIMARY_TEXT, C_TEXT};
        int[] stroke = new int[]{0xFFA7C5FF, 0xFFD5DAE6};

        b.setBackgroundTintList(new ColorStateList(states, bg));
        b.setTextColor(new ColorStateList(states, text));
        b.setStrokeColor(new ColorStateList(states, stroke));
        b.setStrokeWidth(dp(1));

        return b;
    }

    private View weighted(View v) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        v.setLayoutParams(lp);
        return v;
    }

    private View spaceW(int px) {
        Space s = new Space(this);
        s.setLayoutParams(new LinearLayout.LayoutParams(px, 1));
        return s;
    }

    private int dp(int v) {
        float d = getResources().getDisplayMetrics().density;
        return (int) (v * d);
    }
}
