package com.cypher.zealth;

import android.Manifest;
import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Space;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

public class PregnancyTrackerActivity extends AppCompatActivity {

    private static final int GESTATION_DAYS = 280; // 40 weeks
    private static final int MAX_WEEKS_UI = 42;
    private static final int MODE_DUE_DATE = 0;
    private static final int MODE_LMP = 1;

    private static final String CHANNEL_ID = "pregnancy_tracker";
    private static final int REQ_DAILY = 7001;
    private static final int REQ_WEEKLY = 7002;

    private static final String ACTION_DAILY = "com.cypher.zealth.PREGNANCY_DAILY";
    private static final String ACTION_WEEKLY = "com.cypher.zealth.PREGNANCY_WEEKLY";

    private static final String SP = "pregnancy_tracker_sp";
    private static final String K_DUE_MS = "due_ms";
    private static final String K_MODE = "mode";
    private static final String K_NOTES = "notes";
    private static final String K_DAILY_ENABLED = "daily_enabled";
    private static final String K_DAILY_HOUR = "daily_hour";
    private static final String K_DAILY_MIN = "daily_min";
    private static final String K_LAST_NOTIFIED_WEEK = "last_notified_week";

    private static final String K_CLINIC_NAME = "clinic_name";
    private static final String K_CLINIC_PHONE = "clinic_phone";
    private static final String K_CLINIC_ADDRESS = "clinic_address";
    private static final String K_CLINIC_NOTES = "clinic_notes";

    private static final int C_BG = 0xFFFFF7FB;          
    private static final int C_SHEET = 0xFFFFFFFF;       
    private static final int C_TEXT = 0xFF2A1B2E;        
    private static final int C_MUTED = 0xFF6B4E6B;       
    private static final int C_ACCENT = 0xFFDB2777;      
    private static final int C_ACCENT_SOFT = 0xFFFCE7F3; 
    private static final int C_LAV_SOFT = 0xFFF3E8FF;    
    private static final int C_STROKE = 0xFFE9D5FF;      
    private static final int C_PROGRESS_TRACK = 0xFFF5D0E2;
    private static final int C_PROGRESS = 0xFFDB2777;

    private final SimpleDateFormat df = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());

    private long dueDateMs = -1L;
    private int mode = MODE_DUE_DATE;

    private MaterialToolbar toolbar;
    private MaterialButton btnModeDueDate, btnModeLmp;

    private MaterialCardView cardEmpty, cardSummary, cardInsights, cardNotes, cardActions, cardSettings;
    private MaterialButton btnPickDateEmpty, btnPickDate;
    private TextView tvWeekHeader;

    private TextView tvProgressLabel, tvGestation, tvTrimester, tvDaysRemaining, tvConception, tvDueDateLine;
    private LinearProgressIndicator progress;

    private RecyclerView rvWeeks;
    private WeekAdapter adapter;

    private TextInputEditText etNotes;

    private SwitchMaterial swDaily;
    private MaterialButton btnPickReminderTime;
    private TextView tvReminderTime;

   
    private TextView tvClinicSummary;


    private ActivityResultLauncher<String> notifPermissionLauncher;

    private ActivityResultLauncher<String> callPermissionLauncher;
    private String pendingCallPhone = null;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        notifPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                granted -> {
                    if (!granted) {
                        Toast.makeText(this, "Notifications are off. You can enable them in Settings later.", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(this, "Notifications enabled.", Toast.LENGTH_SHORT).show();
                        if (getDailyEnabled()) scheduleDailyIfEnabled();
                        scheduleWeeklyMilestoneIfPossible();
                    }
                }
        );

      
        callPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                granted -> {
                    if (TextUtils.isEmpty(pendingCallPhone)) return;

                    if (granted) {
                        startDirectCall(pendingCallPhone);
                    } else {
                        Toast.makeText(this, "Call permission denied. Opening dialer instead.", Toast.LENGTH_SHORT).show();
                        startDial(pendingCallPhone);
                    }
                    pendingCallPhone = null;
                }
        );

        createNotificationChannel();

        dueDateMs = sp().getLong(K_DUE_MS, -1L);
        mode = sp().getInt(K_MODE, MODE_DUE_DATE);
        if (savedInstanceState != null) {
            dueDateMs = savedInstanceState.getLong("dueDateMs", dueDateMs);
            mode = savedInstanceState.getInt("mode", mode);
        }

  
        setContentView(buildRoot());

        setMode(mode, false);
        renderAll();

        scheduleDailyIfEnabled();
        scheduleWeeklyMilestoneIfPossible();

        if (dueDateMs > 0) {
            int currentWeek = getCurrentWeek(dueDateMs);
            rvWeeks.post(() -> rvWeeks.smoothScrollToPosition(Math.max(0, currentWeek - 1)));
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong("dueDateMs", dueDateMs);
        outState.putInt("mode", mode);
    }



    private View buildRoot() {
        FrameLayout root = new FrameLayout(this);
        root.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        root.setBackgroundColor(C_BG);

   
        View topWash = new View(this);
        FrameLayout.LayoutParams washLp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(220));
        washLp.gravity = Gravity.TOP;
        topWash.setLayoutParams(washLp);
        topWash.setBackgroundColor(C_LAV_SOFT);
        topWash.setAlpha(0.60f);
        root.addView(topWash);

        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        root.addView(container);

        toolbar = new MaterialToolbar(this);
        toolbar.setTitle("Pregnancy Tracker");
        toolbar.setSubtitle("Due date, progress, and weekly guidance");
        toolbar.setTitleTextColor(C_TEXT);
        toolbar.setSubtitleTextColor(C_MUTED);
        toolbar.setNavigationIcon(R.drawable.ic_back_medical);
        toolbar.setNavigationIconTint(C_ACCENT);
        toolbar.setBackgroundColor(0x00000000);
        toolbar.setNavigationOnClickListener(v -> finish());
        toolbar.setPadding(dp(8), dp(10), dp(8), dp(6));
        container.addView(toolbar, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        ScrollView sv = new ScrollView(this);
        sv.setFillViewport(true);
        sv.setOverScrollMode(View.OVER_SCROLL_NEVER);
        LinearLayout.LayoutParams svLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        container.addView(sv, svLp);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(16), dp(10), dp(16), dp(18));
        sv.addView(content, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        content.addView(buildCard(
                "Note",
                "This feature is for personal tracking and planning. It provides date estimates and general, non-medical information. " +
                        "It does not diagnose, treat, or prevent any condition. If you have health questions or concerns, contact a qualified " +
                        "healthcare professional. If you believe you may be experiencing an emergency, contact local emergency services.",
                C_SHEET
        ));


 
        content.addView(space(dp(12)));
        content.addView(buildModeToggleCard());


        content.addView(space(dp(12)));
        cardEmpty = buildEmptyStateCard();
        content.addView(cardEmpty);

  
        content.addView(space(dp(12)));
        cardSummary = buildSummaryCard();
        content.addView(cardSummary);

        content.addView(space(dp(12)));
        cardInsights = buildInsightsCard();
        content.addView(cardInsights);

        content.addView(space(dp(12)));
        cardNotes = buildNotesCard();
        content.addView(cardNotes);

        content.addView(space(dp(12)));
        cardSettings = buildNotificationSettingsCard();
        content.addView(cardSettings);

        content.addView(space(dp(14)));
        tvWeekHeader = new TextView(this);
        tvWeekHeader.setText("Week-by-week");
        tvWeekHeader.setTextColor(C_MUTED);
        tvWeekHeader.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        tvWeekHeader.setTypeface(Typeface.DEFAULT_BOLD);
        content.addView(tvWeekHeader);

        content.addView(space(dp(8)));

        rvWeeks = new RecyclerView(this);
        rvWeeks.setLayoutManager(new LinearLayoutManager(this));
        rvWeeks.setNestedScrollingEnabled(false);
        rvWeeks.setOverScrollMode(View.OVER_SCROLL_NEVER);
        adapter = new WeekAdapter();
        rvWeeks.setAdapter(adapter);
        content.addView(rvWeeks, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        content.addView(space(dp(12)));
        cardActions = buildActionsCard();
        content.addView(cardActions);

        content.addView(space(dp(18)));

        return root;
    }

    private MaterialCardView buildModeToggleCard() {
        MaterialCardView card = cardBase();

        LinearLayout wrap = new LinearLayout(this);
        wrap.setOrientation(LinearLayout.VERTICAL);
        wrap.setPadding(dp(16), dp(16), dp(16), dp(16));
        card.addView(wrap);

        wrap.addView(title("How would you like to calculate?"));
        wrap.addView(space(dp(10)));

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);

        int pillH = dp(44);

        btnModeDueDate = new MaterialButton(this);
        btnModeDueDate.setText("I know my due date");
        styleModePill(btnModeDueDate, pillH);

        btnModeLmp = new MaterialButton(this);
        btnModeLmp.setText("I know my LMP");
        styleModePill(btnModeLmp, pillH);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, pillH, 1f);
        row.addView(btnModeDueDate, lp);
        row.addView(spaceH(dp(10)));
        row.addView(btnModeLmp, lp);

        wrap.addView(row);

        btnModeDueDate.setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
            setMode(MODE_DUE_DATE, true);
        });

        btnModeLmp.setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
            setMode(MODE_LMP, true);
        });

        return card;
    }

    private void styleModePill(MaterialButton b, int heightPx) {
        b.setAllCaps(false);
        b.setSingleLine(true);
        b.setMaxLines(1);
        b.setEllipsize(TextUtils.TruncateAt.END);
        b.setGravity(Gravity.CENTER);
        b.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        b.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);

        b.setCornerRadius(dp(16));
        b.setInsetTop(0);
        b.setInsetBottom(0);

        int padH = dp(12);
        b.setPadding(padH, 0, padH, 0);

        b.setMinHeight(heightPx);
        b.setMinimumHeight(heightPx);
    }

    private MaterialCardView buildEmptyStateCard() {
        MaterialCardView card = cardBase();

        LinearLayout wrap = new LinearLayout(this);
        wrap.setOrientation(LinearLayout.VERTICAL);
        wrap.setPadding(dp(16), dp(16), dp(16), dp(16));
        card.addView(wrap);

        wrap.addView(title("Set your date to begin"));
        wrap.addView(space(dp(6)));

        TextView body = body("Choose either your due date or the start date of your last menstrual period (LMP). " +
                "We’ll calculate date estimates and show week-by-week check-ins plus reminders.");

        wrap.addView(body);

        wrap.addView(space(dp(12)));

        btnPickDateEmpty = primaryButton("Set date");
        btnPickDateEmpty.setIconResource(android.R.drawable.ic_menu_my_calendar);
        btnPickDateEmpty.setOnClickListener(v -> pickDate());
        wrap.addView(btnPickDateEmpty, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(56)));

        return card;
    }

    private MaterialCardView buildSummaryCard() {
        MaterialCardView card = cardBase();

        LinearLayout wrap = new LinearLayout(this);
        wrap.setOrientation(LinearLayout.VERTICAL);
        wrap.setPadding(dp(16), dp(16), dp(16), dp(16));
        card.addView(wrap);

        wrap.addView(title("Pregnancy overview"));

        tvDueDateLine = new TextView(this);
        tvDueDateLine.setTextColor(C_MUTED);
        tvDueDateLine.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        tvDueDateLine.setPadding(0, dp(6), 0, 0);
        wrap.addView(tvDueDateLine);

        wrap.addView(space(dp(12)));

        btnPickDate = primaryButton("Set date");
        btnPickDate.setIconResource(android.R.drawable.ic_menu_my_calendar);
        btnPickDate.setOnClickListener(v -> pickDate());
        wrap.addView(btnPickDate, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(56)));

        wrap.addView(space(dp(12)));

        tvProgressLabel = new TextView(this);
        tvProgressLabel.setText("Week — of 40");
        tvProgressLabel.setTextColor(C_MUTED);
        tvProgressLabel.setTypeface(Typeface.DEFAULT_BOLD);
        tvProgressLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        wrap.addView(tvProgressLabel);

        progress = new LinearProgressIndicator(this);
        progress.setTrackThickness(dp(10));
        progress.setTrackCornerRadius(dp(999));
        progress.setIndicatorColor(C_PROGRESS);
        progress.setTrackColor(C_PROGRESS_TRACK);
        LinearLayout.LayoutParams pLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(10));
        pLp.topMargin = dp(8);
        wrap.addView(progress, pLp);

        wrap.addView(space(dp(12)));

        tvGestation = infoLine();
        tvTrimester = infoLine();
        tvDaysRemaining = infoLine();
        tvConception = infoLine();

        wrap.addView(tvGestation);
        wrap.addView(space(dp(6)));
        wrap.addView(tvTrimester);
        wrap.addView(space(dp(6)));
        wrap.addView(tvDaysRemaining);
        wrap.addView(space(dp(6)));
        wrap.addView(tvConception);

        return card;
    }

    private MaterialCardView buildInsightsCard() {
        MaterialCardView card = cardBase();
        card.setCardBackgroundColor(C_SHEET);

        LinearLayout wrap = new LinearLayout(this);
        wrap.setOrientation(LinearLayout.VERTICAL);
        wrap.setPadding(dp(16), dp(16), dp(16), dp(16));
        card.addView(wrap);

        wrap.addView(title("General wellbeing tips"));
        wrap.addView(space(dp(10)));

        wrap.addView(sectionLabel("Helpful habits"));
        wrap.addView(bullets(new String[]{
                "Keep a simple routine for sleep, hydration, and meals that feels sustainable.",
                "Write down questions or topics you want to discuss at your next appointment (if you have one).",
                "Use notes to track patterns you notice over time for your own reference.",
                "Choose gentle activity you feel comfortable with, if it fits your situation."
        }));

        wrap.addView(space(dp(12)));

        wrap.addView(sectionLabel("Things to consider"));
        wrap.addView(bullets(new String[]{
                "Be cautious with supplements, medications, and new wellness products—check reliable sources or a professional.",
                "Follow standard food-safety practices and personal comfort preferences.",
                "Avoid extreme heat or overexertion if it makes you feel unwell.",
                "If something doesn’t feel right, consider contacting a qualified healthcare professional."
        }));

        wrap.addView(space(dp(12)));

        wrap.addView(sectionLabel("When to get help"));
        wrap.addView(bullets(new String[]{
                "If you are worried about symptoms, sudden changes, or anything that feels urgent, seek professional help.",
                "If you believe it may be an emergency, contact local emergency services."
        }));


        return card;
    }

    private MaterialCardView buildNotesCard() {
        MaterialCardView card = cardBase();

        LinearLayout wrap = new LinearLayout(this);
        wrap.setOrientation(LinearLayout.VERTICAL);
        wrap.setPadding(dp(16), dp(16), dp(16), dp(16));
        card.addView(wrap);

        wrap.addView(title("Notes"));
        wrap.addView(space(dp(8)));

        TextInputLayout til = new TextInputLayout(this);
        til.setBoxBackgroundMode(TextInputLayout.BOX_BACKGROUND_OUTLINE);
        til.setBoxBackgroundColor(C_BG);
        til.setBoxCornerRadii(dp(18), dp(18), dp(18), dp(18));
        til.setBoxStrokeColor(C_STROKE);
        til.setHintTextColor(android.content.res.ColorStateList.valueOf(C_MUTED));
        til.setHint("Optional notes (symptoms, questions, reminders)");

        etNotes = new TextInputEditText(this);
        etNotes.setMinLines(3);
        etNotes.setTextColor(C_TEXT);
        etNotes.setHintTextColor(0xAA6B4E6B);
        etNotes.setPadding(dp(14), dp(14), dp(14), dp(14));
        etNotes.setBackgroundColor(0x00000000);
        etNotes.setText(sp().getString(K_NOTES, ""));

        til.addView(etNotes, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        wrap.addView(til);

        wrap.addView(space(dp(12)));

        MaterialButton btnSaveNotes = primaryButton("Save notes");
        btnSaveNotes.setIconResource(android.R.drawable.ic_menu_save);
        btnSaveNotes.setOnClickListener(v -> saveNotes());
        wrap.addView(btnSaveNotes, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(56)));

        return card;
    }

    private MaterialCardView buildNotificationSettingsCard() {
        MaterialCardView card = cardBase();

        LinearLayout wrap = new LinearLayout(this);
        wrap.setOrientation(LinearLayout.VERTICAL);
        wrap.setPadding(dp(16), dp(16), dp(16), dp(16));
        card.addView(wrap);

        wrap.addView(title("Reminders"));
        wrap.addView(space(dp(6)));

        wrap.addView(body("Enable a daily check-in reminder and weekly milestones. You can adjust the daily time."));
        wrap.addView(space(dp(10)));

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);

        TextView lbl = new TextView(this);
        lbl.setText("Daily check-in reminder");
        lbl.setTextColor(C_TEXT);
        lbl.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        lbl.setTypeface(Typeface.DEFAULT_BOLD);

        swDaily = new SwitchMaterial(this);
        swDaily.setChecked(getDailyEnabled());
        swDaily.setOnCheckedChangeListener((buttonView, isChecked) -> {
            setDailyEnabled(isChecked);
            ensureNotifPermissionIfNeeded();
            scheduleDailyIfEnabled();
        });

        LinearLayout.LayoutParams lblLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        row.addView(lbl, lblLp);
        row.addView(swDaily);
        wrap.addView(row);

        wrap.addView(space(dp(10)));

        LinearLayout timeRow = new LinearLayout(this);
        timeRow.setOrientation(LinearLayout.HORIZONTAL);
        timeRow.setGravity(Gravity.CENTER_VERTICAL);

        tvReminderTime = new TextView(this);
        tvReminderTime.setTextColor(C_MUTED);
        tvReminderTime.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        tvReminderTime.setText("Time: " + formatReminderTime(getDailyHour(), getDailyMin()));

        btnPickReminderTime = secondaryButton("Change time");
        btnPickReminderTime.setOnClickListener(v -> pickReminderTime());

        LinearLayout.LayoutParams t1 = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        timeRow.addView(tvReminderTime, t1);
        timeRow.addView(btnPickReminderTime);
        wrap.addView(timeRow);

        return card;
    }

    private MaterialCardView buildActionsCard() {
        MaterialCardView card = cardBase();

        LinearLayout wrap = new LinearLayout(this);
        wrap.setOrientation(LinearLayout.VERTICAL);
        wrap.setPadding(dp(16), dp(16), dp(16), dp(16));
        card.addView(wrap);

        wrap.addView(title("Actions"));
        wrap.addView(space(dp(10)));

        tvClinicSummary = new TextView(this);
        tvClinicSummary.setTextColor(C_MUTED);
        tvClinicSummary.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        tvClinicSummary.setLineSpacing(0f, 1.15f);
        wrap.addView(tvClinicSummary);

        wrap.addView(space(dp(10)));

        MaterialButton btnClinic = secondaryButton("Your clinic");
        btnClinic.setIconResource(android.R.drawable.ic_menu_call);
        btnClinic.setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
            onClinicButton();
        });

        MaterialButton btnClear = dangerButton("Clear pregnancy data");
        btnClear.setIconResource(android.R.drawable.ic_menu_delete);
        btnClear.setOnClickListener(v -> clearAll());

        wrap.addView(btnClinic, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(52)));
        wrap.addView(space(dp(10)));
        wrap.addView(btnClear, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(56)));

        return card;
    }


    private void renderAll() {
        boolean hasDate = (dueDateMs > 0);

        cardEmpty.setVisibility(hasDate ? View.GONE : View.VISIBLE);
        cardSummary.setVisibility(hasDate ? View.VISIBLE : View.GONE);

        cardInsights.setVisibility(View.VISIBLE);
        cardNotes.setVisibility(View.VISIBLE);
        cardSettings.setVisibility(View.VISIBLE);

        tvWeekHeader.setVisibility(hasDate ? View.VISIBLE : View.GONE);
        rvWeeks.setVisibility(hasDate ? View.VISIBLE : View.GONE);

        cardActions.setVisibility(View.VISIBLE);

        updateClinicSummaryUI();

        if (!hasDate) {
            if (tvGestation != null) tvGestation.setText("Gestation: —");
            if (tvTrimester != null) tvTrimester.setText("Trimester: —");
            if (tvDaysRemaining != null) tvDaysRemaining.setText("Days remaining: —");
            if (tvConception != null) tvConception.setText("Estimated conception: —");
            if (tvProgressLabel != null) tvProgressLabel.setText("Week — of 40");
            if (progress != null) progress.setProgressCompat(0, false);
            if (tvDueDateLine != null) tvDueDateLine.setText("");
            adapter.setItems(new ArrayList<>());
            setPickDateLabels();
            return;
        }

        setPickDateLabels();
        renderSummary();
        renderWeeks();
    }

    private void renderSummary() {
        long today = startOfDay(System.currentTimeMillis());
        long lmpMs = addDays(dueDateMs, -GESTATION_DAYS);
        long conceptionMs = addDays(lmpMs, 14);

        long daysPregnant = daysBetween(lmpMs, today);
        int week = (int) Math.floor(daysPregnant / 7.0) + 1;
        int dayInWeek = (int) (daysPregnant % 7);

        if (week < 1) week = 1;
        if (week > MAX_WEEKS_UI) week = MAX_WEEKS_UI;

        long daysRemaining = daysBetween(today, dueDateMs);
        if (daysRemaining < 0) daysRemaining = 0;

        int trimester = (week <= 13) ? 1 : (week <= 27) ? 2 : 3;

        tvGestation.setText("Gestation: Week " + week + " + " + Math.max(0, dayInWeek) + " days");
        tvTrimester.setText("Trimester: " + trimester);
        tvDaysRemaining.setText("Days remaining: " + daysRemaining);

        long windowStart = addDays(conceptionMs, -2);
        long windowEnd = addDays(conceptionMs, 2);
        tvConception.setText("Estimated conception: " + df.format(new Date(windowStart)) + " to " + df.format(new Date(windowEnd)));

        int weekForProgress = Math.min(Math.max(week, 1), 40);
        int pct = (int) Math.round((weekForProgress / 40.0) * 100.0);

        tvProgressLabel.setText("Week " + weekForProgress + " of 40 (" + pct + "%)");
        progress.setProgressCompat(pct, true);

        tvDueDateLine.setText("Due date: " + df.format(new Date(dueDateMs)));

        scheduleWeeklyMilestoneIfPossible();
    }

    private void renderWeeks() {
        List<WeekItem> items = new ArrayList<>();
        int currentWeek = getCurrentWeek(dueDateMs);

        for (int w = 1; w <= MAX_WEEKS_UI; w++) {
            int trimester = (w <= 13) ? 1 : (w <= 27) ? 2 : 3;
            boolean isCurrent = (w == currentWeek);
            items.add(new WeekItem(w, trimester, tipForWeek(w), isCurrent));
        }
        adapter.setItems(items);
    }


    private void onClinicButton() {
        if (!hasClinicDetails()) {
            showClinicSetupDialog(this::showClinicActionsDialog);
        } else {
            showClinicActionsDialog();
        }
    }

    private void showClinicActionsDialog() {
        final String[] actions = new String[]{
                "Call clinic",
                "Navigate to clinic",
                "Edit clinic details",
                "Clear clinic details"
        };

        String title = "Your clinic";
        String subtitle = buildClinicSummaryLine();

        new MaterialAlertDialogBuilder(this)
                .setTitle(title)
                .setMessage(subtitle)
                .setItems(actions, (d, which) -> {
                    if (which == 0) {
                        callClinic(); // NOW: direct call (or dialer fallback)
                    } else if (which == 1) {
                        navigateToClinic();
                    } else if (which == 2) {
                        showClinicSetupDialog(null);
                    } else if (which == 3) {
                        clearClinicDetails();
                        Toast.makeText(this, "Clinic details cleared.", Toast.LENGTH_SHORT).show();
                        updateClinicSummaryUI();
                    }
                })
                .setNegativeButton("Close", (d, w) -> d.dismiss())
                .show();
    }

    private void showClinicSetupDialog(@Nullable Runnable afterSave) {
        LinearLayout wrap = new LinearLayout(this);
        wrap.setOrientation(LinearLayout.VERTICAL);
        wrap.setPadding(dp(20), dp(10), dp(20), dp(0));

        TextView helper = body("Add your clinic details once. Then you can call and navigate from the “Your clinic” button.");
        wrap.addView(helper);
        wrap.addView(space(dp(10)));

        TextInputLayout tilName = outlinedTil("Clinic name (optional)");
        TextInputEditText etName = new TextInputEditText(this);
        etName.setInputType(InputType.TYPE_CLASS_TEXT);
        etName.setText(getClinicName());
        tilName.addView(etName, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        wrap.addView(tilName);

        wrap.addView(space(dp(10)));

        TextInputLayout tilPhone = outlinedTil("Clinic phone (for calling)");
        TextInputEditText etPhone = new TextInputEditText(this);
        etPhone.setInputType(InputType.TYPE_CLASS_PHONE);
        etPhone.setText(getClinicPhone());
        tilPhone.addView(etPhone, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        wrap.addView(tilPhone);

        wrap.addView(space(dp(10)));

        TextInputLayout tilAddr = outlinedTil("Clinic address (for navigation)");
        TextInputEditText etAddr = new TextInputEditText(this);
        etAddr.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_POSTAL_ADDRESS);
        etAddr.setText(getClinicAddress());
        tilAddr.addView(etAddr, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        wrap.addView(tilAddr);

        wrap.addView(space(dp(10)));

        TextInputLayout tilNotes = outlinedTil("Notes (optional)");
        TextInputEditText etClinicNotes = new TextInputEditText(this);
        etClinicNotes.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        etClinicNotes.setMinLines(2);
        etClinicNotes.setText(getClinicNotes());
        tilNotes.addView(etClinicNotes, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        wrap.addView(tilNotes);

        MaterialAlertDialogBuilder b = new MaterialAlertDialogBuilder(this)
                .setTitle("Add clinic details")
                .setView(wrap)
                .setNegativeButton("Cancel", (d, w) -> d.dismiss())
                .setPositiveButton("Save", null);

        final androidx.appcompat.app.AlertDialog dialog = b.create();
        dialog.setOnShowListener(di -> {
            dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String name = safeText(etName);
                String phone = safeText(etPhone);
                String addr = safeText(etAddr);
                String notes = safeText(etClinicNotes);

                boolean hasAny = !TextUtils.isEmpty(name) || !TextUtils.isEmpty(phone) || !TextUtils.isEmpty(addr);
                if (!hasAny) {
                    tilName.setError("Add at least a name, phone, or address");
                    tilPhone.setError("Add at least a name, phone, or address");
                    tilAddr.setError("Add at least a name, phone, or address");
                    return;
                }

                tilName.setError(null);
                tilPhone.setError(null);
                tilAddr.setError(null);

                saveClinicDetails(name, phone, addr, notes);
                updateClinicSummaryUI();
                Toast.makeText(this, "Clinic details saved.", Toast.LENGTH_SHORT).show();
                dialog.dismiss();

                if (afterSave != null) afterSave.run();
            });
        });

        dialog.show();
    }


    private void callClinic() {
        String phone = getClinicPhone();
        if (TextUtils.isEmpty(phone)) {
            Toast.makeText(this, "Add a phone number first.", Toast.LENGTH_SHORT).show();
            showClinicSetupDialog(null);
            return;
        }

        pendingCallPhone = phone;

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE)
                == PackageManager.PERMISSION_GRANTED) {
            startDirectCall(phone);
            pendingCallPhone = null;
        } else {
            callPermissionLauncher.launch(Manifest.permission.CALL_PHONE);
        }
    }

    private void startDirectCall(String phone) {
        Uri tel = Uri.parse("tel:" + Uri.encode(phone));
        Intent i = new Intent(Intent.ACTION_CALL, tel);
        startSafeActivity(i, "No phone app found on this device.");
    }

    private void startDial(String phone) {
        Uri tel = Uri.parse("tel:" + Uri.encode(phone));
        Intent i = new Intent(Intent.ACTION_DIAL, tel);
        startSafeActivity(i, "No dialer app found on this device.");
    }

    private void navigateToClinic() {
        String name = getClinicName();
        String addr = getClinicAddress();

        String query;
        if (!TextUtils.isEmpty(addr) && !TextUtils.isEmpty(name)) query = name + " " + addr;
        else if (!TextUtils.isEmpty(addr)) query = addr;
        else if (!TextUtils.isEmpty(name)) query = name;
        else query = "";

        if (TextUtils.isEmpty(query)) {
            Toast.makeText(this, "Add a clinic name or address first.", Toast.LENGTH_SHORT).show();
            showClinicSetupDialog(null);
            return;
        }

        Uri geo = Uri.parse("geo:0,0?q=" + Uri.encode(query));
        Intent map = new Intent(Intent.ACTION_VIEW, geo);

        try {
            map.setPackage("com.google.android.apps.maps");
            startActivity(map);
            return;
        } catch (Exception ignore) { /* fall through */ }

        try {
            map.setPackage(null);
            startActivity(map);
            return;
        } catch (Exception ignore) { /* fall through */ }

        Uri web = Uri.parse("https://www.google.com/maps/search/?api=1&query=" + Uri.encode(query));
        Intent webIntent = new Intent(Intent.ACTION_VIEW, web);
        startSafeActivity(webIntent, "No maps app or browser found.");
    }

    private void updateClinicSummaryUI() {
        if (tvClinicSummary == null) return;

        if (!hasClinicDetails()) {
            tvClinicSummary.setText("Clinic: Not set. Tap “Your clinic” to add details for calling and navigation.");
            return;
        }
        tvClinicSummary.setText("Clinic: " + buildClinicSummaryLine());
    }

    private String buildClinicSummaryLine() {
        String name = getClinicName();
        String phone = getClinicPhone();
        String addr = getClinicAddress();

        List<String> parts = new ArrayList<>();
        if (!TextUtils.isEmpty(name)) parts.add(name);
        if (!TextUtils.isEmpty(phone)) parts.add(phone);
        if (!TextUtils.isEmpty(addr)) parts.add(addr);

        if (parts.isEmpty()) return "Not set";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.size(); i++) {
            sb.append(parts.get(i));
            if (i < parts.size() - 1) sb.append(" • ");
        }
        return sb.toString();
    }

    private boolean hasClinicDetails() {
        return !TextUtils.isEmpty(getClinicName())
                || !TextUtils.isEmpty(getClinicPhone())
                || !TextUtils.isEmpty(getClinicAddress());
    }

    private void saveClinicDetails(String name, String phone, String address, String notes) {
        sp().edit()
                .putString(K_CLINIC_NAME, safeTrim(name))
                .putString(K_CLINIC_PHONE, safeTrim(phone))
                .putString(K_CLINIC_ADDRESS, safeTrim(address))
                .putString(K_CLINIC_NOTES, safeTrim(notes))
                .apply();
    }

    private void clearClinicDetails() {
        sp().edit()
                .remove(K_CLINIC_NAME)
                .remove(K_CLINIC_PHONE)
                .remove(K_CLINIC_ADDRESS)
                .remove(K_CLINIC_NOTES)
                .apply();
    }

    private String getClinicName() {
        return sp().getString(K_CLINIC_NAME, "");
    }

    private String getClinicPhone() {
        return sp().getString(K_CLINIC_PHONE, "");
    }

    private String getClinicAddress() {
        return sp().getString(K_CLINIC_ADDRESS, "");
    }

    private String getClinicNotes() {
        return sp().getString(K_CLINIC_NOTES, "");
    }

    private void startSafeActivity(Intent i, String failMessage) {
        try {
            startActivity(i);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, failMessage, Toast.LENGTH_SHORT).show();
        } catch (SecurityException se) {

            Toast.makeText(this, "Permission required. Opening dialer.", Toast.LENGTH_SHORT).show();
            if (pendingCallPhone != null) startDial(pendingCallPhone);
        }
    }

    private TextInputLayout outlinedTil(String hint) {
        TextInputLayout til = new TextInputLayout(this);
        til.setBoxBackgroundMode(TextInputLayout.BOX_BACKGROUND_OUTLINE);
        til.setBoxBackgroundColor(C_BG);
        til.setBoxCornerRadii(dp(18), dp(18), dp(18), dp(18));
        til.setBoxStrokeColor(C_STROKE);
        til.setHintTextColor(android.content.res.ColorStateList.valueOf(C_MUTED));
        til.setHint(hint);
        return til;
    }

    private String safeText(TextInputEditText et) {
        if (et == null || et.getText() == null) return "";
        return et.getText().toString().trim();
    }

    private String safeTrim(String s) {
        return s == null ? "" : s.trim();
    }


    private void setMode(int newMode, boolean persist) {
        mode = newMode;

        if (mode == MODE_DUE_DATE) {
            btnModeDueDate.setBackgroundTintList(android.content.res.ColorStateList.valueOf(C_ACCENT_SOFT));
            btnModeDueDate.setTextColor(C_TEXT);

            btnModeLmp.setBackgroundTintList(android.content.res.ColorStateList.valueOf(C_LAV_SOFT));
            btnModeLmp.setTextColor(C_MUTED);
        } else {
            btnModeLmp.setBackgroundTintList(android.content.res.ColorStateList.valueOf(C_ACCENT_SOFT));
            btnModeLmp.setTextColor(C_TEXT);

            btnModeDueDate.setBackgroundTintList(android.content.res.ColorStateList.valueOf(C_LAV_SOFT));
            btnModeDueDate.setTextColor(C_MUTED);
        }

        if (persist) {
            sp().edit().putInt(K_MODE, mode).apply();
            setPickDateLabels();
            renderAll();
        }
    }

    private void setPickDateLabels() {
        if (mode == MODE_DUE_DATE) {
            String text = (dueDateMs > 0)
                    ? "Due date: " + df.format(new Date(dueDateMs))
                    : "Set due date";
            if (btnPickDate != null) btnPickDate.setText(text);
            if (btnPickDateEmpty != null) btnPickDateEmpty.setText("Set due date");
        } else {
            if (dueDateMs > 0) {
                long lmpMs = addDays(dueDateMs, -GESTATION_DAYS);
                String text = "LMP: " + df.format(new Date(lmpMs));
                if (btnPickDate != null) btnPickDate.setText(text);
            } else {
                if (btnPickDate != null) btnPickDate.setText("Set LMP (last period start)");
            }
            if (btnPickDateEmpty != null) btnPickDateEmpty.setText("Set LMP (last period start)");
        }
    }

    private void pickDate() {
        long todayUtc = MaterialDatePicker.todayInUtcMilliseconds();

        long startUtc = todayUtc - TimeUnit.DAYS.toMillis(700);
        long endUtc = (mode == MODE_LMP)
                ? todayUtc
                : todayUtc + TimeUnit.DAYS.toMillis(500);

        CalendarConstraints constraints = new CalendarConstraints.Builder()
                .setStart(startUtc)
                .setEnd(endUtc)
                .build();

        long selectionLocal;
        if (mode == MODE_DUE_DATE) {
            selectionLocal = (dueDateMs > 0) ? dueDateMs : startOfDay(System.currentTimeMillis());
        } else {
            selectionLocal = (dueDateMs > 0)
                    ? addDays(dueDateMs, -GESTATION_DAYS)
                    : startOfDay(System.currentTimeMillis());
        }

        long selectionUtc = localStartOfDayToUtcMidnight(selectionLocal);
        if (selectionUtc < startUtc) selectionUtc = startUtc;
        if (selectionUtc > endUtc) selectionUtc = endUtc;

        MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
                .setTitleText(mode == MODE_DUE_DATE ? "Select due date" : "Select LMP (last period start)")
                .setSelection(selectionUtc)
                .setCalendarConstraints(constraints)
                .setTheme(R.style.ThemeOverlay_Zealth_PregnancyDatePicker)
                .build();

        picker.addOnPositiveButtonClickListener(utcMidnightMs -> {
            long pickedLocal = utcMidnightToLocalStartOfDay(utcMidnightMs);

            if (mode == MODE_DUE_DATE) {
                dueDateMs = pickedLocal;
            } else {
                dueDateMs = addDays(pickedLocal, GESTATION_DAYS);
            }

            warnIfUnusual(dueDateMs);

            sp().edit()
                    .putLong(K_DUE_MS, dueDateMs)
                    .putInt(K_MODE, mode)
                    .putInt(K_LAST_NOTIFIED_WEEK, 0)
                    .apply();

            setPickDateLabels();
            renderAll();

            Toast.makeText(this, "Date saved.", Toast.LENGTH_SHORT).show();

            int currentWeek = getCurrentWeek(dueDateMs);
            rvWeeks.post(() -> rvWeeks.smoothScrollToPosition(Math.max(0, currentWeek - 1)));

            ensureNotifPermissionIfNeeded();
            scheduleDailyIfEnabled();
            scheduleWeeklyMilestoneIfPossible();
        });

        picker.show(getSupportFragmentManager(), "pregnancy_date_picker");
    }

    private long utcMidnightToLocalStartOfDay(long utcMidnightMs) {
        Calendar utc = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        utc.setTimeInMillis(utcMidnightMs);

        Calendar local = Calendar.getInstance();
        local.set(Calendar.YEAR, utc.get(Calendar.YEAR));
        local.set(Calendar.MONTH, utc.get(Calendar.MONTH));
        local.set(Calendar.DAY_OF_MONTH, utc.get(Calendar.DAY_OF_MONTH));
        local.set(Calendar.HOUR_OF_DAY, 0);
        local.set(Calendar.MINUTE, 0);
        local.set(Calendar.SECOND, 0);
        local.set(Calendar.MILLISECOND, 0);
        return local.getTimeInMillis();
    }

    private long localStartOfDayToUtcMidnight(long localStartOfDayMs) {
        Calendar local = Calendar.getInstance();
        local.setTimeInMillis(localStartOfDayMs);

        Calendar utc = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        utc.set(Calendar.YEAR, local.get(Calendar.YEAR));
        utc.set(Calendar.MONTH, local.get(Calendar.MONTH));
        utc.set(Calendar.DAY_OF_MONTH, local.get(Calendar.DAY_OF_MONTH));
        utc.set(Calendar.HOUR_OF_DAY, 0);
        utc.set(Calendar.MINUTE, 0);
        utc.set(Calendar.SECOND, 0);
        utc.set(Calendar.MILLISECOND, 0);
        return utc.getTimeInMillis();
    }

    private void warnIfUnusual(long dueMs) {
        long today = startOfDay(System.currentTimeMillis());
        long daysToDue = daysBetween(today, dueMs);

        if (daysToDue > 330) {
            Toast.makeText(this, "This due date seems far in the future. Please double-check.", Toast.LENGTH_LONG).show();
        } else if (daysToDue < -60) {
            Toast.makeText(this, "This due date is quite far in the past. Please double-check.", Toast.LENGTH_LONG).show();
        }
    }

 
    private void saveNotes() {
        String notes = (etNotes == null || etNotes.getText() == null) ? "" : etNotes.getText().toString().trim();
        sp().edit().putString(K_NOTES, notes).apply();
        Toast.makeText(this, "Notes saved.", Toast.LENGTH_SHORT).show();
    }

    private void clearAll() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Clear pregnancy data?")
                .setMessage("This will remove the due date/LMP, notes, and reminder scheduling saved on this device. Clinic details are kept.")
                .setNegativeButton("Cancel", (d, w) -> d.dismiss())
                .setPositiveButton("Clear", (d, w) -> {

                    sp().edit()
                            .remove(K_DUE_MS)
                            .remove(K_MODE)
                            .remove(K_NOTES)
                            .remove(K_DAILY_ENABLED)
                            .remove(K_DAILY_HOUR)
                            .remove(K_DAILY_MIN)
                            .remove(K_LAST_NOTIFIED_WEEK)
                            .apply();

                    dueDateMs = -1L;
                    mode = MODE_DUE_DATE;

                    if (etNotes != null) etNotes.setText("");
                    setMode(mode, false);
                    renderAll();

                    cancelDaily();
                    cancelWeekly();

                    Toast.makeText(this, "Pregnancy data cleared.", Toast.LENGTH_SHORT).show();
                })
                .show();
    }


    private void ensureNotifPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= 33) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel ch = new NotificationChannel(
                    CHANNEL_ID,
                    "Pregnancy Tracker",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            ch.setDescription("Daily check-ins and weekly pregnancy milestones");
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) nm.createNotificationChannel(ch);
        }
    }

    private void scheduleDailyIfEnabled() {
        if (!getDailyEnabled()) {
            cancelDaily();
            return;
        }
        if (!canPostNotifs()) return;

        AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
        if (am == null) return;

        int hour = getDailyHour();
        int min = getDailyMin();

        Calendar next = Calendar.getInstance();
        next.setTimeInMillis(System.currentTimeMillis());
        next.set(Calendar.HOUR_OF_DAY, hour);
        next.set(Calendar.MINUTE, min);
        next.set(Calendar.SECOND, 0);
        next.set(Calendar.MILLISECOND, 0);

        if (next.getTimeInMillis() <= System.currentTimeMillis() + 5_000) {
            next.add(Calendar.DAY_OF_YEAR, 1);
        }

        PendingIntent pi = PendingIntent.getBroadcast(
                this,
                REQ_DAILY,
                new Intent(this, PregnancyReminderReceiver.class).setAction(ACTION_DAILY),
                pendingFlags()
        );

        if (Build.VERSION.SDK_INT >= 23) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next.getTimeInMillis(), pi);
        } else {
            am.setExact(AlarmManager.RTC_WAKEUP, next.getTimeInMillis(), pi);
        }

        if (tvReminderTime != null) {
            tvReminderTime.setText("Time: " + formatReminderTime(hour, min));
        }
    }

    private void scheduleWeeklyMilestoneIfPossible() {
        if (dueDateMs <= 0) {
            cancelWeekly();
            return;
        }
        if (!canPostNotifs()) return;

        int currentWeek = getCurrentWeek(dueDateMs);
        int lastNotified = sp().getInt(K_LAST_NOTIFIED_WEEK, 0);

        if (currentWeek > 0 && currentWeek <= MAX_WEEKS_UI && currentWeek != lastNotified) {
            postWeeklyMilestoneNow(currentWeek);
            sp().edit().putInt(K_LAST_NOTIFIED_WEEK, currentWeek).apply();
        }

        AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
        if (am == null) return;

        long nextMidnight = startOfDay(System.currentTimeMillis()) + TimeUnit.DAYS.toMillis(1);

        PendingIntent pi = PendingIntent.getBroadcast(
                this,
                REQ_WEEKLY,
                new Intent(this, PregnancyReminderReceiver.class).setAction(ACTION_WEEKLY),
                pendingFlags()
        );

        if (Build.VERSION.SDK_INT >= 23) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, nextMidnight, pi);
        } else {
            am.setExact(AlarmManager.RTC_WAKEUP, nextMidnight, pi);
        }
    }

    private void postWeeklyMilestoneNow(int week) {
        String title = "Week " + Math.min(week, 40) + " update";
        String msg = tipForWeek(week);

        NotificationCompat.Builder b = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_menu_info_details)
                .setContentTitle(title)
                .setContentText(msg)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(msg))
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        NotificationManagerCompat.from(this).notify(9000 + week, b.build());
    }

    private void cancelDaily() {
        AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
        if (am == null) return;
        PendingIntent pi = PendingIntent.getBroadcast(
                this,
                REQ_DAILY,
                new Intent(this, PregnancyReminderReceiver.class).setAction(ACTION_DAILY),
                pendingFlags()
        );
        am.cancel(pi);
    }

    private void cancelWeekly() {
        AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
        if (am == null) return;
        PendingIntent pi = PendingIntent.getBroadcast(
                this,
                REQ_WEEKLY,
                new Intent(this, PregnancyReminderReceiver.class).setAction(ACTION_WEEKLY),
                pendingFlags()
        );
        am.cancel(pi);
    }

    private boolean canPostNotifs() {
        if (Build.VERSION.SDK_INT >= 33) {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }

    private int pendingFlags() {
        int f = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) f |= PendingIntent.FLAG_IMMUTABLE;
        return f;
    }

    public static class PregnancyReminderReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent == null ? null : intent.getAction();
            if (TextUtils.isEmpty(action)) return;

            SharedPreferences sp = context.getSharedPreferences(SP, Context.MODE_PRIVATE);

            if (ACTION_DAILY.equals(action)) {
                String msg = "Daily check-in: update your notes and review today’s plan. " +
                        "If you have health concerns, consider contacting a qualified professional.";


                NotificationCompat.Builder b = new NotificationCompat.Builder(context, CHANNEL_ID)
                        .setSmallIcon(android.R.drawable.ic_menu_my_calendar)
                        .setContentTitle("Pregnancy check-in")
                        .setContentText(msg)
                        .setStyle(new NotificationCompat.BigTextStyle().bigText(msg))
                        .setAutoCancel(true)
                        .setPriority(NotificationCompat.PRIORITY_DEFAULT);

                NotificationManagerCompat.from(context).notify(7100, b.build());

                boolean enabled = sp.getBoolean(K_DAILY_ENABLED, false);
                if (enabled) {
                    int hour = sp.getInt(K_DAILY_HOUR, 9);
                    int min = sp.getInt(K_DAILY_MIN, 0);

                    AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
                    if (am != null) {
                        Calendar next = Calendar.getInstance();
                        next.setTimeInMillis(System.currentTimeMillis());
                        next.set(Calendar.HOUR_OF_DAY, hour);
                        next.set(Calendar.MINUTE, min);
                        next.set(Calendar.SECOND, 0);
                        next.set(Calendar.MILLISECOND, 0);
                        next.add(Calendar.DAY_OF_YEAR, 1);

                        PendingIntent pi = PendingIntent.getBroadcast(
                                context,
                                REQ_DAILY,
                                new Intent(context, PregnancyReminderReceiver.class).setAction(ACTION_DAILY),
                                (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                                        ? (PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE)
                                        : PendingIntent.FLAG_UPDATE_CURRENT
                        );

                        if (Build.VERSION.SDK_INT >= 23) {
                            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next.getTimeInMillis(), pi);
                        } else {
                            am.setExact(AlarmManager.RTC_WAKEUP, next.getTimeInMillis(), pi);
                        }
                    }
                }
                return;
            }

            if (ACTION_WEEKLY.equals(action)) {
                long dueMs = sp.getLong(K_DUE_MS, -1L);
                if (dueMs <= 0) return;

                long today = startOfDayStatic(System.currentTimeMillis());
                long lmp = addDaysStatic(dueMs, -GESTATION_DAYS);
                long daysPregnant = TimeUnit.MILLISECONDS.toDays(today - lmp);
                int week = (int) Math.floor(daysPregnant / 7.0) + 1;
                if (week < 1) week = 1;
                if (week > MAX_WEEKS_UI) week = MAX_WEEKS_UI;

                int last = sp.getInt(K_LAST_NOTIFIED_WEEK, 0);
                if (week != last) {
                    String msg = tipForWeekStatic(week);
                    NotificationCompat.Builder b = new NotificationCompat.Builder(context, CHANNEL_ID)
                            .setSmallIcon(android.R.drawable.ic_menu_info_details)
                            .setContentTitle("Week " + Math.min(week, 40) + " update")
                            .setContentText(msg)
                            .setStyle(new NotificationCompat.BigTextStyle().bigText(msg))
                            .setAutoCancel(true)
                            .setPriority(NotificationCompat.PRIORITY_DEFAULT);

                    NotificationManagerCompat.from(context).notify(9000 + week, b.build());
                    sp.edit().putInt(K_LAST_NOTIFIED_WEEK, week).apply();
                }

                AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
                if (am != null) {
                    long nextMidnight = startOfDayStatic(System.currentTimeMillis()) + TimeUnit.DAYS.toMillis(1);
                    PendingIntent pi = PendingIntent.getBroadcast(
                            context,
                            REQ_WEEKLY,
                            new Intent(context, PregnancyReminderReceiver.class).setAction(ACTION_WEEKLY),
                            (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                                    ? (PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE)
                                    : PendingIntent.FLAG_UPDATE_CURRENT
                    );
                    if (Build.VERSION.SDK_INT >= 23) {
                        am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, nextMidnight, pi);
                    } else {
                        am.setExact(AlarmManager.RTC_WAKEUP, nextMidnight, pi);
                    }
                }
            }
        }

        private static long startOfDayStatic(long ms) {
            Calendar c = Calendar.getInstance();
            c.setTimeInMillis(ms);
            c.set(Calendar.HOUR_OF_DAY, 0);
            c.set(Calendar.MINUTE, 0);
            c.set(Calendar.SECOND, 0);
            c.set(Calendar.MILLISECOND, 0);
            return c.getTimeInMillis();
        }

        private static long addDaysStatic(long ms, int days) {
            Calendar c = Calendar.getInstance();
            c.setTimeInMillis(ms);
            c.add(Calendar.DAY_OF_YEAR, days);
            return startOfDayStatic(c.getTimeInMillis());
        }

        private static String tipForWeekStatic(int w) {
            if (w <= 4)  return "Early stage: rest, hydration, and symptom tracking. If you have concerns, seek clinical guidance.";
            if (w <= 8)  return "Common: nausea/fatigue. Try small frequent meals and gentle pacing of your day.";
            if (w <= 12) return "First trimester: plan routine prenatal care where available. Prioritize sleep and balanced meals.";
            if (w <= 16) return "Energy may improve. Consider light movement and a simple weekly check-in note.";
            if (w <= 20) return "Mid-pregnancy: review upcoming appointments and write down questions for your clinician.";
            if (w <= 24) return "Comfort focus: posture, hydration, and gentle stretching can help. Note any new symptoms.";
            if (w <= 28) return "Plan ahead: transport, support contacts, and essentials. Keep your plan simple and practical.";
            if (w <= 32) return "Sleep and comfort: experiment with pillows and routines. Discuss any worries with a clinician.";
            if (w <= 36) return "Preparation phase: confirm your plan and support network. Keep essentials organized.";
            if (w <= 40) return "Final weeks: know when/where to seek care. Keep your phone charged and plan ready.";
            return "Post-due window: follow clinical guidance and scheduled assessments where available.";
        }
    }

    private void pickReminderTime() {
        int hour = getDailyHour();
        int min = getDailyMin();

        android.app.TimePickerDialog tp = new android.app.TimePickerDialog(
                this,
                (view, hourOfDay, minute) -> {
                    sp().edit()
                            .putInt(K_DAILY_HOUR, hourOfDay)
                            .putInt(K_DAILY_MIN, minute)
                            .apply();

                    tvReminderTime.setText("Time: " + formatReminderTime(hourOfDay, minute));

                    ensureNotifPermissionIfNeeded();
                    scheduleDailyIfEnabled();

                    Toast.makeText(this, "Reminder time updated.", Toast.LENGTH_SHORT).show();
                },
                hour, min, DateFormat.is24HourFormat(this)
        );
        tp.show();
    }

    private String formatReminderTime(int hour, int min) {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY, hour);
        c.set(Calendar.MINUTE, min);
        return DateFormat.format(DateFormat.is24HourFormat(this) ? "HH:mm" : "h:mm a", c).toString();
    }

  
    private String tipForWeek(int w) {
        if (w <= 4)  return "Week-by-week check-in: set a simple routine and start a notes list for questions and reminders.";
        if (w <= 8)  return "Check-in: keep meals and rest consistent. Use notes to track what supports your daily routine.";
        if (w <= 12) return "Check-in: plan ahead for appointments or tasks you want to schedule, if applicable to you.";
        if (w <= 16) return "Check-in: review your goals for sleep, movement, and stress management for the week.";
        if (w <= 20) return "Check-in: write down questions or topics you want to discuss at your next visit, if you have one.";
        if (w <= 24) return "Check-in: keep planning simple—prepare a short list of essentials and reminders.";
        if (w <= 28) return "Check-in: confirm your support contacts and plans for the coming weeks.";
        if (w <= 32) return "Check-in: adjust your routine for comfort—sleep setup, breaks, and pacing.";
        if (w <= 36) return "Check-in: organize essentials and make a short, practical plan for the next steps.";
        if (w <= 40) return "Check-in: keep plans flexible and note any questions for a qualified professional if needed.";
        return "Check-in: follow your planned next steps and seek professional guidance if you have concerns.";
    }


  

    private int getCurrentWeek(long dueDateMs) {
        long today = startOfDay(System.currentTimeMillis());
        long lmpMs = addDays(dueDateMs, -GESTATION_DAYS);

        long daysPregnant = daysBetween(lmpMs, today);
        int week = (int) Math.floor(daysPregnant / 7.0) + 1;

        if (week < 1) week = 1;
        if (week > MAX_WEEKS_UI) week = MAX_WEEKS_UI;
        return week;
    }

    private long startOfDay(long ms) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(ms);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }

    private long addDays(long ms, int days) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(ms);
        c.add(Calendar.DAY_OF_YEAR, days);
        return startOfDay(c.getTimeInMillis());
    }

    private long daysBetween(long startMs, long endMs) {
        long diff = endMs - startMs;
        return TimeUnit.MILLISECONDS.toDays(diff);
    }

 

    private SharedPreferences sp() {
        return getSharedPreferences(SP, MODE_PRIVATE);
    }

    private boolean getDailyEnabled() {
        return sp().getBoolean(K_DAILY_ENABLED, false);
    }

    private void setDailyEnabled(boolean enabled) {
        sp().edit().putBoolean(K_DAILY_ENABLED, enabled).apply();
    }

    private int getDailyHour() {
        return sp().getInt(K_DAILY_HOUR, 9);
    }

    private int getDailyMin() {
        return sp().getInt(K_DAILY_MIN, 0);
    }

  

    static class WeekItem {
        final int week;
        final int trimester;
        final String tip;
        final boolean isCurrent;

        WeekItem(int week, int trimester, String tip, boolean isCurrent) {
            this.week = week;
            this.trimester = trimester;
            this.tip = tip;
            this.isCurrent = isCurrent;
        }
    }

    class WeekAdapter extends RecyclerView.Adapter<WeekAdapter.VH> {
        private final List<WeekItem> items = new ArrayList<>();

        void setItems(List<WeekItem> newItems) {
            items.clear();
            if (newItems != null) items.addAll(newItems);
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            MaterialCardView card = new MaterialCardView(parent.getContext());
            card.setRadius(dp(22));
            card.setCardBackgroundColor(C_SHEET);
            card.setStrokeWidth(dp(1));
            card.setStrokeColor(C_STROKE);
            card.setCardElevation(dp(6));
            card.setUseCompatPadding(true);

            LinearLayout wrap = new LinearLayout(parent.getContext());
            wrap.setOrientation(LinearLayout.VERTICAL);
            wrap.setPadding(dp(14), dp(14), dp(14), dp(14));
            card.addView(wrap);

            LinearLayout top = new LinearLayout(parent.getContext());
            top.setOrientation(LinearLayout.HORIZONTAL);
            top.setGravity(Gravity.CENTER_VERTICAL);

            TextView tvTitle = new TextView(parent.getContext());
            tvTitle.setTextColor(C_TEXT);
            tvTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
            tvTitle.setTypeface(Typeface.DEFAULT_BOLD);

            TextView badge = new TextView(parent.getContext());
            badge.setText("Current");
            badge.setTextColor(C_ACCENT);
            badge.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
            badge.setTypeface(Typeface.DEFAULT_BOLD);
            badge.setPadding(dp(10), dp(6), dp(10), dp(6));
            badge.setBackgroundColor(C_ACCENT_SOFT);

            LinearLayout.LayoutParams tLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            top.addView(tvTitle, tLp);
            top.addView(badge);

            TextView tvTip = new TextView(parent.getContext());
            tvTip.setTextColor(C_MUTED);
            tvTip.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
            tvTip.setPadding(0, dp(8), 0, 0);
            tvTip.setLineSpacing(0f, 1.15f);

            wrap.addView(top);
            wrap.addView(tvTip);

            RecyclerView.LayoutParams lp = new RecyclerView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.bottomMargin = dp(10);
            card.setLayoutParams(lp);

            return new VH(card, tvTitle, tvTip, badge);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int position) {
            WeekItem it = items.get(position);
            h.tvTitle.setText("Week " + it.week + " • Trimester " + it.trimester);
            h.tvTip.setText(it.tip);
            h.badge.setVisibility(it.isCurrent ? View.VISIBLE : View.GONE);

            if (it.isCurrent) {
                h.card.setStrokeColor(C_ACCENT);
                h.card.setCardBackgroundColor(0xFFFFFBFD);
            } else {
                h.card.setStrokeColor(C_STROKE);
                h.card.setCardBackgroundColor(C_SHEET);
            }
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class VH extends RecyclerView.ViewHolder {
            final MaterialCardView card;
            final TextView tvTitle, tvTip, badge;

            VH(@NonNull View itemView, TextView tvTitle, TextView tvTip, TextView badge) {
                super(itemView);
                this.card = (MaterialCardView) itemView;
                this.tvTitle = tvTitle;
                this.tvTip = tvTip;
                this.badge = badge;
            }
        }
    }


    private MaterialCardView cardBase() {
        MaterialCardView card = new MaterialCardView(this);
        card.setRadius(dp(26));
        card.setCardBackgroundColor(C_SHEET);
        card.setStrokeWidth(dp(1));
        card.setStrokeColor(C_STROKE);
        card.setCardElevation(dp(8));
        card.setUseCompatPadding(true);
        return card;
    }

    private MaterialCardView buildCard(String title, String body, int bg) {
        MaterialCardView card = cardBase();
        card.setCardBackgroundColor(bg);

        LinearLayout wrap = new LinearLayout(this);
        wrap.setOrientation(LinearLayout.VERTICAL);
        wrap.setPadding(dp(16), dp(16), dp(16), dp(16));
        card.addView(wrap);

        wrap.addView(title(title));
        wrap.addView(space(dp(6)));
        wrap.addView(body(body));
        return card;
    }

    private TextView title(String t) {
        TextView tv = new TextView(this);
        tv.setText(t);
        tv.setTextColor(C_TEXT);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        tv.setTypeface(Typeface.DEFAULT_BOLD);
        return tv;
    }

    private TextView body(String t) {
        TextView tv = new TextView(this);
        tv.setText(t);
        tv.setTextColor(C_MUTED);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        tv.setLineSpacing(0f, 1.15f);
        return tv;
    }

    private TextView sectionLabel(String t) {
        TextView tv = new TextView(this);
        tv.setText(t);
        tv.setTextColor(C_TEXT);
        tv.setTypeface(Typeface.DEFAULT_BOLD);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        return tv;
    }

    private TextView infoLine() {
        TextView tv = new TextView(this);
        tv.setTextColor(C_MUTED);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        return tv;
    }

    private View bullets(String[] lines) {
        LinearLayout wrap = new LinearLayout(this);
        wrap.setOrientation(LinearLayout.VERTICAL);
        wrap.setPadding(0, dp(6), 0, 0);

        for (String s : lines) {
            TextView tv = new TextView(this);
            tv.setText("• " + s);
            tv.setTextColor(C_MUTED);
            tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
            tv.setLineSpacing(0f, 1.15f);
            tv.setPadding(0, dp(4), 0, 0);
            wrap.addView(tv);
        }
        return wrap;
    }

    private MaterialButton primaryButton(String text) {
        MaterialButton b = new MaterialButton(this);
        b.setText(text);
        b.setAllCaps(false);
        b.setTextColor(0xFFFFFFFF);
        b.setCornerRadius(dp(999));
        b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(C_ACCENT));
        b.setRippleColor(android.content.res.ColorStateList.valueOf(0x22FFFFFF));

        b.setInsetTop(0);
        b.setInsetBottom(0);
        b.setPadding(0, 0, 0, 0);
        b.setMinHeight(0);
        b.setMinimumHeight(0);

        b.setIconTint(android.content.res.ColorStateList.valueOf(0xFFFFFFFF));
        b.setIconPadding(dp(10));
        return b;
    }

    private MaterialButton secondaryButton(String text) {
        MaterialButton b = new MaterialButton(this);
        b.setText(text);
        b.setAllCaps(false);
        b.setTextColor(C_ACCENT);
        b.setCornerRadius(dp(999));
        b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(C_ACCENT_SOFT));
        b.setRippleColor(android.content.res.ColorStateList.valueOf(0x22000000));

        b.setInsetTop(0);
        b.setInsetBottom(0);
        b.setPadding(0, 0, 0, 0);

        b.setIconTint(android.content.res.ColorStateList.valueOf(C_ACCENT));
        b.setIconPadding(dp(10));
        return b;
    }

    private MaterialButton dangerButton(String text) {
        MaterialButton b = new MaterialButton(this);
        b.setText(text);
        b.setAllCaps(false);
        b.setTextColor(C_TEXT);
        b.setCornerRadius(dp(999));

        b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFFFFE4E6));
        b.setRippleColor(android.content.res.ColorStateList.valueOf(0x22000000));

        b.setInsetTop(0);
        b.setInsetBottom(0);
        b.setPadding(0, 0, 0, 0);

        b.setIconTint(android.content.res.ColorStateList.valueOf(C_TEXT));
        b.setIconPadding(dp(10));
        return b;
    }

    private View space(int hDp) {
        Space s = new Space(this);
        s.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, hDp));
        return s;
    }

    private View spaceH(int wDp) {
        Space s = new Space(this);
        s.setLayoutParams(new LinearLayout.LayoutParams(
                wDp, ViewGroup.LayoutParams.MATCH_PARENT));
        return s;
    }

    private int dp(int v) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, v, getResources().getDisplayMetrics());
    }
}


