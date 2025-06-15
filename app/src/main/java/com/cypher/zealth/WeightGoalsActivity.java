package com.cypher.zealth;

import android.Manifest;
import android.app.AlarmManager;
import android.app.DatePickerDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
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
import android.view.inputmethod.InputMethodManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Space;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;


public class WeightGoalsActivity extends AppCompatActivity {


    private static final int C_BG = Color.parseColor("#EEF6FF");
    private static final int C_SURFACE = Color.WHITE;
    private static final int C_CARD_STROKE = Color.parseColor("#2A93C5FD");
    private static final int C_TEXT = Color.parseColor("#0B1B3A");
    private static final int C_MUTED = Color.parseColor("#2B5AA6");
    private static final int C_HINT = Color.parseColor("#7AA7E8");
    private static final int C_PRIMARY = Color.parseColor("#2563EB");
    private static final int C_PRIMARY_2 = Color.parseColor("#60A5FA");
    private static final int C_FIELD_BG = Color.parseColor("#F7FBFF");
    private static final int C_TRACK = Color.parseColor("#C7DAFF");
    private static final int C_WARN = Color.parseColor("#B45309");
    private static final int C_WARN_BG = Color.parseColor("#FFFBEB");
    private static final int C_OK = Color.parseColor("#047857");
    private static final int C_OK_BG = Color.parseColor("#ECFDF5");

    private static final String CH_ID = "weight_goals";
    private static final int NOTIF_ID_REMINDER = 71001;
    private static final int NOTIF_ID_GOAL = 71002;

    private static final String PREF = "weight_goals_pref";
    private static final String KEY_START = "start_weight_kg";
    private static final String KEY_TARGET = "target_weight_kg";
    private static final String KEY_TARGET_DATE = "target_date_ms";
    private static final String KEY_LOGS = "logs_json_kg";
    private static final String KEY_REMINDER_ENABLED = "reminder_enabled";
    private static final String KEY_REMINDER_HOUR = "reminder_hour";
    private static final String KEY_REMINDER_MIN = "reminder_min";

    private static final String KEY_UNIT_LB = "unit_lb";                 
    private static final String KEY_TEMPLATE = "goal_template";         
    private static final String KEY_LAST_NUDGE_DISMISS_MS = "nudge_dismiss_ms";

    private static final String KEY_NSV = "nsv_json";
    private static final String KEY_BADGES_ACK = "badges_ack_json"; 
    private static final String KEY_SEX_MALE = "nut_sex_male"; 
    private static final String KEY_AGE = "nut_age";
    private static final String KEY_HEIGHT_CM = "nut_height_cm";
    private static final String KEY_ACTIVITY = "nut_activity"; 

  
    private static final String KEY_BIOMETRIC_LOCK = "biometric_lock_enabled";

 
    private final SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd", Locale.US);


    private MaterialToolbar toolbar;

    private TextInputEditText etStart, etTarget, etLogWeight, etLogNote;
    private MaterialButton btnPickTargetDate, btnSaveGoal;
    private MaterialButton btnPickLogDate, btnSaveLog, btnClearAll;

    private TextView tvSummary, tvPercent, tvEmptyHistory;
    private LinearProgressIndicator progressBar;

    private SwitchMaterial swReminder;
    private MaterialButton btnPickReminderTime;

    private MaterialButton btnUnitKg, btnUnitLb;
    private MaterialButton btnTplFatLoss, btnTplLeanBulk, btnTplMaintenance;
    private TextView tvTplHint;
    private TextView tvNudge;
    private MaterialButton btnNudgeAction;


    private TrendLineView trendView;
    private LinearLayout badgesWrap;
    private TextView tvBadgesHint;

    private MaterialButton btnPickNsvDate, btnSaveNsv;
    private TextInputEditText etNsv;
    private RecyclerView rvNsv;
    private TextView tvEmptyNsv;
    private NsvAdapter nsvAdapter;
    private long nsvDateMs = -1L;


    private MaterialButton btnSexMale, btnSexFemale;
    private MaterialButton btnActSed, btnActLight, btnActMod, btnActVery, btnActExtra;
    private TextInputEditText etAge, etHeight;
    private TextView tvNutritionOut;

    private SwitchMaterial swBiometricLock;

    private RecyclerView rv;
    private WeightLogAdapter adapter;

  
    private SharedPreferences sp;
    private long targetDateMs = -1L;
    private long logDateMs = -1L;

    private ActivityResultLauncher<String> notifPermLauncher;

  
    private boolean biometricAuthedThisSession = false;

    private static final double KG_TO_LB = 2.2046226218;

    private static final int H_PILL_OUTLINED_DP = 44; 
    private static final int H_PILL_FILLED_DP = 48;   
    private static final int H_SMALL_PILL_DP = 34;    

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sp = getSharedPreferences(PREF, MODE_PRIVATE);
        createNotificationChannel();

        notifPermLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                granted -> {
                    if (!granted) {
                        toast("Notifications permission denied.");
                    } else {
                        toast("Notifications enabled.");
                        if (sp.getBoolean(KEY_REMINDER_ENABLED, false)) scheduleDailyReminder();
                    }
                }
        );

        setContentView(buildContentView());

        toolbar.setNavigationOnClickListener(v -> finish());

        if (sp.getBoolean(KEY_BIOMETRIC_LOCK, false)) {
            promptBiometricOrExit();
        } else {
            biometricAuthedThisSession = true;
        }

        if (hasGoal()) {
            etStart.setText(format1(displayFromKg(getStartKg())));
            etTarget.setText(format1(displayFromKg(getTargetKg())));
            targetDateMs = sp.getLong(KEY_TARGET_DATE, -1L);
        }

        logDateMs = startOfDay(System.currentTimeMillis());
        btnPickLogDate.setText("Log date: " + df.format(new Date(logDateMs)));

        nsvDateMs = startOfDay(System.currentTimeMillis());
        btnPickNsvDate.setText("NSV date: " + df.format(new Date(nsvDateMs)));

        updateTargetDateButton();
        refreshHistory();
        refreshNsv();
        renderProgressAndNudges();
        renderBadges();
        renderTrend();
        renderNutrition();

  
        boolean isLb = isLb();
        setUnitButtons(isLb, false);

        btnUnitKg.setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
            if (isLb()) {
                setUnit(false);
                setUnitButtons(false, true);
                convertVisibleFieldsOnUnitChange(true /*lb->kg*/);
                refreshHistory();
                renderProgressAndNudges();
                renderBadges();
                renderTrend();
                renderNutrition();
            }
        });

        btnUnitLb.setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
            if (!isLb()) {
                setUnit(true);
                setUnitButtons(true, true);
                convertVisibleFieldsOnUnitChange(false /*kg->lb*/);
                refreshHistory();
                renderProgressAndNudges();
                renderBadges();
                renderTrend();
                renderNutrition();
            }
        });

        String tpl = sp.getString(KEY_TEMPLATE, "custom");
        setTemplateButtons(tpl, false);
        updateTemplateHint(tpl);

        btnTplFatLoss.setOnClickListener(v -> { v.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK); setTemplate("fat_loss"); });
        btnTplLeanBulk.setOnClickListener(v -> { v.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK); setTemplate("lean_bulk"); });
        btnTplMaintenance.setOnClickListener(v -> { v.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK); setTemplate("maintenance"); });

        boolean remEnabled = sp.getBoolean(KEY_REMINDER_ENABLED, false);
        swReminder.setChecked(remEnabled);
        updateReminderTimeButtonText();

        swReminder.setOnCheckedChangeListener((buttonView, isChecked) -> {
            sp.edit().putBoolean(KEY_REMINDER_ENABLED, isChecked).apply();
            buttonView.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);

            if (isChecked) {
                ensureNotificationPermissionThen(() -> {
                    scheduleDailyReminder();
                    toast("Daily reminder enabled.");
                });
            } else {
                cancelDailyReminder();
                toast("Daily reminder disabled.");
            }
        });

        btnPickReminderTime.setOnClickListener(v -> {
            int h = sp.getInt(KEY_REMINDER_HOUR, 9);
            int m = sp.getInt(KEY_REMINDER_MIN, 0);

            TimePickerDialog tp = new TimePickerDialog(
                    this,
                    (view, hourOfDay, minute) -> {
                        sp.edit().putInt(KEY_REMINDER_HOUR, hourOfDay).putInt(KEY_REMINDER_MIN, minute).apply();
                        updateReminderTimeButtonText();

                        if (sp.getBoolean(KEY_REMINDER_ENABLED, false)) {
                            ensureNotificationPermissionThen(() -> {
                                scheduleDailyReminder();
                                toast("Reminder time updated.");
                            });
                        }
                    },
                    h, m,
                    DateFormat.is24HourFormat(this)
            );
            tp.show();
        });

        btnPickTargetDate.setOnClickListener(v -> pickTargetDate());
        btnPickLogDate.setOnClickListener(v -> pickLogDate());
        btnSaveGoal.setOnClickListener(v -> saveGoal());
        btnSaveLog.setOnClickListener(v -> saveLog());
        btnClearAll.setOnClickListener(v -> clearAll());

       
        btnNudgeAction.setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
            if (shouldOfferRecalibration()) {
                showRecalibrateDialog();
            } else {
                pickTargetDate();
            }
        });

        tvNudge.setOnLongClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
            sp.edit().putLong(KEY_LAST_NUDGE_DISMISS_MS, System.currentTimeMillis()).apply();
            tvNudge.setVisibility(View.GONE);
            btnNudgeAction.setVisibility(View.GONE);
            toast("Nudge dismissed.");
            return true;
        });


        btnPickNsvDate.setOnClickListener(v -> pickNsvDate());
        btnSaveNsv.setOnClickListener(v -> saveNsv());

   
        swBiometricLock.setChecked(sp.getBoolean(KEY_BIOMETRIC_LOCK, false));
        swBiometricLock.setOnCheckedChangeListener((buttonView, isChecked) -> {
            buttonView.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
            if (isChecked) {
                if (!deviceSupportsBiometric()) {
                    toast("Biometrics not available on this device.");
                    swBiometricLock.setChecked(false);
                    return;
                }
                sp.edit().putBoolean(KEY_BIOMETRIC_LOCK, true).apply();
                toast("Biometric lock enabled.");
                promptBiometricOrExit();
            } else {
                sp.edit().putBoolean(KEY_BIOMETRIC_LOCK, false).apply();
                biometricAuthedThisSession = true;
                toast("Biometric lock disabled.");
            }
        });

        btnSexMale.setOnClickListener(v -> { setSex(true); renderNutrition(); });
        btnSexFemale.setOnClickListener(v -> { setSex(false); renderNutrition(); });

        btnActSed.setOnClickListener(v -> { setActivity(0); renderNutrition(); });
        btnActLight.setOnClickListener(v -> { setActivity(1); renderNutrition(); });
        btnActMod.setOnClickListener(v -> { setActivity(2); renderNutrition(); });
        btnActVery.setOnClickListener(v -> { setActivity(3); renderNutrition(); });
        btnActExtra.setOnClickListener(v -> { setActivity(4); renderNutrition(); });

        etAge.setOnFocusChangeListener((v, hasFocus) -> { if (!hasFocus) { persistNutritionInputs(); renderNutrition(); }});
        etHeight.setOnFocusChangeListener((v, hasFocus) -> { if (!hasFocus) { persistNutritionInputs(); renderNutrition(); }});
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (sp.getBoolean(KEY_BIOMETRIC_LOCK, false) && !biometricAuthedThisSession) {
            promptBiometricOrExit();
        }
    }
 
    private View buildContentView() {
        FrameLayout root = new FrameLayout(this);
        root.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));
        root.setBackgroundColor(C_BG);

        View wash = new View(this);
        GradientDrawable gd = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{adjustAlpha(C_PRIMARY, 0.18f), Color.TRANSPARENT}
        );
        wash.setBackground(gd);
        root.addView(wash, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(220)
        ));

        LinearLayout screen = new LinearLayout(this);
        screen.setOrientation(LinearLayout.VERTICAL);
        screen.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));
        root.addView(screen);

        MaterialCardView toolbarCard = card(22, 8);
        LinearLayout.LayoutParams toolbarCardLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        );
        toolbarCardLp.setMargins(dp(16), dp(12), dp(16), dp(8));
        screen.addView(toolbarCard, toolbarCardLp);

        toolbar = new MaterialToolbar(this);
        toolbar.setTitle("Weight Goals");
        toolbar.setSubtitle("Trend, wins, nudges and privacy");
        toolbar.setTitleTextColor(C_TEXT);
        toolbar.setSubtitleTextColor(C_MUTED);
        toolbar.setBackgroundColor(Color.TRANSPARENT);


        try {
            toolbar.setNavigationIcon(R.drawable.ic_back_medical);
        } catch (Throwable t) {
            toolbar.setNavigationIcon(android.R.drawable.ic_media_previous);
        }
        toolbar.setNavigationIconTint(C_PRIMARY);
        toolbarCard.addView(toolbar);

        NestedScrollView nsv = new NestedScrollView(this);
        nsv.setFillViewport(true);
        nsv.setOverScrollMode(View.OVER_SCROLL_NEVER);
        LinearLayout.LayoutParams nsvLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0
        );
        nsvLp.weight = 1f;
        screen.addView(nsv, nsvLp);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(16), dp(8), dp(16), dp(16));
        nsv.addView(content, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        MaterialCardView cardGoal = card(22, 8);
        content.addView(cardGoal, lpMatchWrap(0, 0, 0, 0));

        LinearLayout goalInner = innerColumn();
        cardGoal.addView(goalInner);

        goalInner.addView(sectionTitle("Goal"));

        LinearLayout unitRow = rowH();
        unitRow.setPadding(0, dp(10), 0, 0);
        goalInner.addView(unitRow);

        TextView unitLbl = label("Units");
        LinearLayout.LayoutParams unitLblLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        unitRow.addView(unitLbl, unitLblLp);

        btnUnitKg = smallPill("kg");
        btnUnitLb = smallPill("lb");
        unitRow.addView(btnUnitKg, new LinearLayout.LayoutParams(dp(64), dp(H_SMALL_PILL_DP)));
        Space spcU = new Space(this);
        spcU.setLayoutParams(new LinearLayout.LayoutParams(dp(10), 1));
        unitRow.addView(spcU);
        unitRow.addView(btnUnitLb, new LinearLayout.LayoutParams(dp(64), dp(H_SMALL_PILL_DP)));

        TextView tplTitle = label("Goal template");
        goalInner.addView(tplTitle, lpMatchWrap(0, dp(14), 0, 0));

        LinearLayout tplRow = rowH();
        goalInner.addView(tplRow, lpMatchWrap(0, dp(10), 0, 0));

        btnTplFatLoss = smallPill("Fat loss");
        btnTplLeanBulk = smallPill("Lean bulk");
        btnTplMaintenance = smallPill("Maintenance");

        LinearLayout.LayoutParams tplBtnLp = new LinearLayout.LayoutParams(0, dp(H_SMALL_PILL_DP), 1f);
        tplRow.addView(btnTplFatLoss, tplBtnLp);
        Space spcT1 = new Space(this); spcT1.setLayoutParams(new LinearLayout.LayoutParams(dp(10), 1));
        tplRow.addView(spcT1);
        tplRow.addView(btnTplLeanBulk, tplBtnLp);
        Space spcT2 = new Space(this); spcT2.setLayoutParams(new LinearLayout.LayoutParams(dp(10), 1));
        tplRow.addView(spcT2);
        tplRow.addView(btnTplMaintenance, tplBtnLp);

        tvTplHint = new TextView(this);
        tvTplHint.setTextColor(C_MUTED);
        tvTplHint.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.75f);
        tvTplHint.setLineSpacing(dp(2), 1f);
        goalInner.addView(tvTplHint, lpMatchWrap(0, dp(10), 0, 0));

        etStart = editNumberField(goalInner, "Start weight (" + unitLabel() + ")", "Your starting weight for this goal");
        etTarget = editNumberField(goalInner, "Target weight (" + unitLabel() + ")", "The weight you want to reach");

        btnPickTargetDate = outlinedPill("Target date: Not set");
        goalInner.addView(btnPickTargetDate, lpMatch(dp(H_PILL_OUTLINED_DP), 0, dp(10), 0, 0));

        btnSaveGoal = filledPill("Save goal");
        goalInner.addView(btnSaveGoal, lpMatch(dp(H_PILL_FILLED_DP), 0, dp(10), 0, 0));

        MaterialCardView cardPrivacy = card(22, 8);
        content.addView(cardPrivacy, lpMatchWrap(0, dp(12), 0, 0));
        LinearLayout privacyInner = innerColumn();
        cardPrivacy.addView(privacyInner);

        privacyInner.addView(sectionTitle("Privacy"));

        TextView prNote = new TextView(this);
        prNote.setText("Optional biometric lock keeps your personal logs private on this device.");
        prNote.setTextColor(C_MUTED);
        prNote.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        privacyInner.addView(prNote, lpMatchWrap(0, dp(6), 0, 0));

        LinearLayout prRow = rowH();
        privacyInner.addView(prRow, lpMatchWrap(0, dp(10), 0, 0));

        TextView prLbl = new TextView(this);
        prLbl.setText("Biometric lock");
        prLbl.setTextColor(C_TEXT);
        prLbl.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        prLbl.setTypeface(Typeface.DEFAULT_BOLD);
        LinearLayout.LayoutParams prLblLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        prRow.addView(prLbl, prLblLp);

        swBiometricLock = new SwitchMaterial(this);
        swBiometricLock.setText("");
        prRow.addView(swBiometricLock);

        MaterialCardView cardNotif = card(22, 8);
        content.addView(cardNotif, lpMatchWrap(0, dp(12), 0, 0));

        LinearLayout notifInner = innerColumn();
        cardNotif.addView(notifInner);

        notifInner.addView(sectionTitle("Notifications"));

        TextView note = new TextView(this);
        note.setText("Optional reminders to help you stay consistent.");
        note.setTextColor(C_MUTED);
        note.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        notifInner.addView(note, lpMatchWrap(0, dp(6), 0, 0));

        LinearLayout row = rowH();
        notifInner.addView(row, lpMatchWrap(0, dp(10), 0, 0));

        TextView lbl = new TextView(this);
        lbl.setText("Daily weigh-in reminder");
        lbl.setTextColor(C_TEXT);
        lbl.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        lbl.setTypeface(Typeface.DEFAULT_BOLD);
        LinearLayout.LayoutParams lblLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        row.addView(lbl, lblLp);

        swReminder = new SwitchMaterial(this);
        swReminder.setText("");
        row.addView(swReminder);

        btnPickReminderTime = outlinedPill("Reminder time: 09:00");
        notifInner.addView(btnPickReminderTime, lpMatch(dp(H_PILL_OUTLINED_DP), 0, dp(10), 0, 0));

        MaterialCardView cardProgress = card(22, 8);
        content.addView(cardProgress, lpMatchWrap(0, dp(12), 0, 0));

        LinearLayout progInner = innerColumn();
        cardProgress.addView(progInner);

        progInner.addView(sectionTitle("Progress"));

        tvNudge = new TextView(this);
        tvNudge.setVisibility(View.GONE);
        tvNudge.setTextColor(C_WARN);
        tvNudge.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        tvNudge.setLineSpacing(dp(2), 1f);
        tvNudge.setPadding(dp(12), dp(10), dp(12), dp(10));
        GradientDrawable nudgeBg = new GradientDrawable();
        nudgeBg.setColor(C_WARN_BG);
        nudgeBg.setCornerRadius(dp(16));
        nudgeBg.setStroke(dp(1), adjustAlpha(C_WARN, 0.25f));
        tvNudge.setBackground(nudgeBg);
        progInner.addView(tvNudge, lpMatchWrap(0, dp(10), 0, 0));

        btnNudgeAction = outlinedPill("Adjust target date");
        btnNudgeAction.setVisibility(View.GONE);
        progInner.addView(btnNudgeAction, lpMatch(dp(H_PILL_OUTLINED_DP), 0, dp(8), 0, 0));

        tvSummary = new TextView(this);
        tvSummary.setText("Set a goal to see progress.");
        tvSummary.setTextColor(C_MUTED);
        tvSummary.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        tvSummary.setLineSpacing(dp(2), 1f);
        progInner.addView(tvSummary, lpMatchWrap(0, dp(8), 0, 0));

        progressBar = new LinearProgressIndicator(this);
        progressBar.setTrackThickness(dp(10));
        progressBar.setTrackCornerRadius(dp(8));
        progressBar.setIndicatorColor(C_PRIMARY);
        progressBar.setTrackColor(C_TRACK);
        progressBar.setProgress(0);
        progInner.addView(progressBar, lpMatch(dp(10), 0, dp(8), 0, 0));

        tvPercent = new TextView(this);
        tvPercent.setText("0%");
        tvPercent.setTextColor(C_PRIMARY);
        tvPercent.setTypeface(Typeface.DEFAULT_BOLD);
        tvPercent.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        progInner.addView(tvPercent, lpMatchWrap(0, dp(8), 0, 0));

        trendView = new TrendLineView(this);
        progInner.addView(trendView, lpMatch(dp(140), 0, dp(10), 0, 0));

        tvBadgesHint = new TextView(this);
        tvBadgesHint.setText("Milestones");
        tvBadgesHint.setTextColor(C_TEXT);
        tvBadgesHint.setTypeface(Typeface.DEFAULT_BOLD);
        tvBadgesHint.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f);
        progInner.addView(tvBadgesHint, lpMatchWrap(0, dp(10), 0, 0));

        badgesWrap = new LinearLayout(this);
        badgesWrap.setOrientation(LinearLayout.VERTICAL);
        progInner.addView(badgesWrap, lpMatchWrap(0, dp(8), 0, 0));

        MaterialCardView cardNut = card(22, 8);
        content.addView(cardNut, lpMatchWrap(0, dp(12), 0, 0));
        LinearLayout nutInner = innerColumn();
        cardNut.addView(nutInner);

        nutInner.addView(sectionTitle("Nutrition estimates"));

        TextView nutNote = new TextView(this);
        nutNote.setText("Optional calculator for personal reference. Estimates are based on your inputs and common formulas.");
        nutNote.setTextColor(C_MUTED);
        nutNote.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        nutNote.setLineSpacing(dp(2), 1f);
        nutInner.addView(nutNote, lpMatchWrap(0, dp(6), 0, 0));

        LinearLayout sexRow = rowH();
        nutInner.addView(sexRow, lpMatchWrap(0, dp(10), 0, 0));
        TextView sexLbl = label("Sex");
        sexRow.addView(sexLbl, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        btnSexMale = smallPill("Male");
        btnSexFemale = smallPill("Female");
        sexRow.addView(btnSexMale, new LinearLayout.LayoutParams(dp(80), dp(H_SMALL_PILL_DP)));
        Space sexSp = new Space(this); sexSp.setLayoutParams(new LinearLayout.LayoutParams(dp(10), 1));
        sexRow.addView(sexSp);
        sexRow.addView(btnSexFemale, new LinearLayout.LayoutParams(dp(80), dp(H_SMALL_PILL_DP)));

        etAge = editNumberField(nutInner, "Age (years)", null);
        etHeight = editNumberField(nutInner, "Height (cm)", null);

        TextView actLbl = label("Activity");
        nutInner.addView(actLbl, lpMatchWrap(0, dp(10), 0, 0));

        LinearLayout actRow1 = rowH();
        nutInner.addView(actRow1, lpMatchWrap(0, dp(10), 0, 0));
        btnActSed = smallPill("Sedentary");
        btnActLight = smallPill("Light");
        btnActMod = smallPill("Moderate");
        LinearLayout.LayoutParams actLp = new LinearLayout.LayoutParams(0, dp(H_SMALL_PILL_DP), 1f);
        actRow1.addView(btnActSed, actLp);
        Space a1 = new Space(this); a1.setLayoutParams(new LinearLayout.LayoutParams(dp(10), 1)); actRow1.addView(a1);
        actRow1.addView(btnActLight, actLp);
        Space a2 = new Space(this); a2.setLayoutParams(new LinearLayout.LayoutParams(dp(10), 1)); actRow1.addView(a2);
        actRow1.addView(btnActMod, actLp);

        LinearLayout actRow2 = rowH();
        nutInner.addView(actRow2, lpMatchWrap(0, dp(10), 0, 0));
        btnActVery = smallPill("Very");
        btnActExtra = smallPill("Extra");
        LinearLayout.LayoutParams actLp2 = new LinearLayout.LayoutParams(0, dp(H_SMALL_PILL_DP), 1f);
        actRow2.addView(btnActVery, actLp2);
        Space a3 = new Space(this); a3.setLayoutParams(new LinearLayout.LayoutParams(dp(10), 1)); actRow2.addView(a3);
        actRow2.addView(btnActExtra, actLp2);

        tvNutritionOut = new TextView(this);
        tvNutritionOut.setTextColor(C_TEXT);
        tvNutritionOut.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f);
        tvNutritionOut.setLineSpacing(dp(2), 1f);
        tvNutritionOut.setPadding(dp(12), dp(10), dp(12), dp(10));
        GradientDrawable nutBg = new GradientDrawable();
        nutBg.setColor(adjustAlpha(C_PRIMARY, 0.06f));
        nutBg.setCornerRadius(dp(16));
        nutBg.setStroke(dp(1), adjustAlpha(C_PRIMARY, 0.18f));
        tvNutritionOut.setBackground(nutBg);
        nutInner.addView(tvNutritionOut, lpMatchWrap(0, dp(12), 0, 0));

     
        MaterialCardView cardLog = card(22, 8);
        content.addView(cardLog, lpMatchWrap(0, dp(12), 0, 0));

        LinearLayout logInner = innerColumn();
        cardLog.addView(logInner);

        logInner.addView(sectionTitle("Log a weigh-in"));

        btnPickLogDate = outlinedPill("Log date: Today");
        logInner.addView(btnPickLogDate, lpMatch(dp(H_PILL_OUTLINED_DP), 0, dp(10), 0, 0));

        etLogWeight = editNumberField(logInner, "Weight (" + unitLabel() + ")", null);
        etLogNote = editNoteField(logInner, "Optional note", "Keep it short and specific");

        btnSaveLog = filledPill("Save log");
        logInner.addView(btnSaveLog, lpMatch(dp(H_PILL_FILLED_DP), 0, dp(10), 0, 0));

    
        MaterialCardView cardNsv = card(22, 8);
        content.addView(cardNsv, lpMatchWrap(0, dp(12), 0, 0));
        LinearLayout nsvInner = innerColumn();
        cardNsv.addView(nsvInner);

        nsvInner.addView(sectionTitle("Non-Scale Victories (NSV)"));

        TextView nsvNote = new TextView(this);
        nsvNote.setText("Log wins beyond weight: energy, sleep, strength, clothing fit, routines, consistency.");
        nsvNote.setTextColor(C_MUTED);
        nsvNote.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        nsvInner.addView(nsvNote, lpMatchWrap(0, dp(6), 0, 0));

        btnPickNsvDate = outlinedPill("NSV date: Today");
        nsvInner.addView(btnPickNsvDate, lpMatch(dp(H_PILL_OUTLINED_DP), 0, dp(10), 0, 0));

        etNsv = editNoteField(nsvInner, "NSV note", "Example: \"Jeans fit better\" or \"More energy today\"");
        btnSaveNsv = filledPill("Save NSV");
        nsvInner.addView(btnSaveNsv, lpMatch(dp(H_PILL_FILLED_DP), 0, dp(10), 0, 0));

        tvEmptyNsv = new TextView(this);
        tvEmptyNsv.setText("No NSVs yet. Add your first non-scale win above.");
        tvEmptyNsv.setTextColor(C_MUTED);
        tvEmptyNsv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        tvEmptyNsv.setVisibility(View.GONE);
        nsvInner.addView(tvEmptyNsv, lpMatchWrap(0, dp(10), 0, 0));

        rvNsv = new RecyclerView(this);
        rvNsv.setLayoutManager(new LinearLayoutManager(this));
        rvNsv.setNestedScrollingEnabled(false);
        nsvInner.addView(rvNsv, lpMatchWrap(0, dp(10), 0, 0));

        nsvAdapter = new NsvAdapter(new ArrayList<>(), e -> showNsvActions(e));
        rvNsv.setAdapter(nsvAdapter);

        MaterialCardView cardHistory = card(22, 8);
        content.addView(cardHistory, lpMatchWrap(0, dp(12), 0, 0));

        LinearLayout histInner = innerColumn();
        cardHistory.addView(histInner);

        histInner.addView(sectionTitle("History"));

        TextView hint = new TextView(this);
        hint.setText("Tip: long-press an entry to edit or delete.");
        hint.setTextColor(adjustAlpha(C_MUTED, 0.9f));
        hint.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f);
        histInner.addView(hint, lpMatchWrap(0, dp(6), 0, 0));

        tvEmptyHistory = new TextView(this);
        tvEmptyHistory.setText("No weigh-ins yet. Add your first log above.");
        tvEmptyHistory.setTextColor(C_MUTED);
        tvEmptyHistory.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        tvEmptyHistory.setVisibility(View.GONE);
        histInner.addView(tvEmptyHistory, lpMatchWrap(0, dp(10), 0, 0));

        rv = new RecyclerView(this);
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setNestedScrollingEnabled(false);
        histInner.addView(rv, lpMatchWrap(0, dp(10), 0, 0));

        btnClearAll = outlinedPill("Clear all weight data");
        histInner.addView(btnClearAll, lpMatch(dp(H_PILL_OUTLINED_DP), 0, dp(10), 0, 0));

        Space bottom = new Space(this);
        content.addView(bottom, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(16)
        ));

        adapter = new WeightLogAdapter(new ArrayList<>(), e -> showLogActions(e));
        rv.setAdapter(adapter);

        return root;
    }

    private LinearLayout rowH() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        return row;
    }

    private TextView label(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(C_TEXT);
        tv.setTypeface(Typeface.DEFAULT_BOLD);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f);
        return tv;
    }

  
    private void setTemplate(String tpl) {
        sp.edit().putString(KEY_TEMPLATE, tpl).apply();
        setTemplateButtons(tpl, true);
        updateTemplateHint(tpl);

        if ("maintenance".equals(tpl)) {
            Double startDisp = parseDouble(etStart);
            if (startDisp != null && startDisp > 0) etTarget.setText(format1(startDisp));
        }

        maybeSuggestTargetDateFromTemplate(tpl);
        renderNutrition();
    }

    private void maybeSuggestTargetDateFromTemplate(String tpl) {
        Double startDisp = parseDouble(etStart);
        Double targetDisp = parseDouble(etTarget);
        if (startDisp == null || targetDisp == null || startDisp <= 0 || targetDisp <= 0) return;

        double startKg = kgFromDisplay(startDisp);
        double targetKg = kgFromDisplay(targetDisp);

        if ("maintenance".equals(tpl)) return;

        double changeKg = Math.abs(startKg - targetKg);
        if (changeKg < 0.0001) return;

        double rateKgPerWeek;
        if ("fat_loss".equals(tpl)) rateKgPerWeek = 0.5;
        else if ("lean_bulk".equals(tpl)) rateKgPerWeek = 0.25;
        else return;

        int weeks = (int) Math.ceil(changeKg / rateKgPerWeek);
        int days = Math.max(7, weeks * 7);

        long suggestedMs = startOfDay(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(days));

        if (targetDateMs > 0) {
            long diffDays = daysBetween(startOfDay(System.currentTimeMillis()), targetDateMs);
            if (diffDays >= days - 3) return;
        }

        String msg = "Based on your entries and selected template, an estimated timeline is about " + weeks +
                " weeks.\nSet target date to " + df.format(new Date(suggestedMs)) + "?";

        new AlertDialog.Builder(this)
                .setTitle("Timeline estimate")
                .setMessage(msg)
                .setNegativeButton("No", (d, w) -> d.dismiss())
                .setPositiveButton("Set date", (d, w) -> {
                    targetDateMs = suggestedMs;
                    updateTargetDateButton();
                    renderProgressAndNudges();
                })
                .show();
    }

    private void updateTemplateHint(String tpl) {
        if ("fat_loss".equals(tpl)) {
            tvTplHint.setText("Loss template: uses a conservative reference pace to suggest timelines.");
        } else if ("lean_bulk".equals(tpl)) {
            tvTplHint.setText("Gain template: uses a conservative reference pace to suggest timelines.");
        } else if ("maintenance".equals(tpl)) {
            tvTplHint.setText("Maintenance: helps you monitor trend over time.");
        } else {
            tvTplHint.setText("Custom: choose your own target and timeline.");
        }

    }

    private void setTemplateButtons(String tpl, boolean animate) {
        setSmallPillSelected(btnTplFatLoss, "fat_loss".equals(tpl));
        setSmallPillSelected(btnTplLeanBulk, "lean_bulk".equals(tpl));
        setSmallPillSelected(btnTplMaintenance, "maintenance".equals(tpl));
        if (animate) {
            View v = "fat_loss".equals(tpl) ? btnTplFatLoss : "lean_bulk".equals(tpl) ? btnTplLeanBulk : btnTplMaintenance;
            if (v != null) v.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
        }
    }

    private boolean isLb() {
        return sp.getBoolean(KEY_UNIT_LB, false);
    }

    private void setUnit(boolean lb) {
        sp.edit().putBoolean(KEY_UNIT_LB, lb).apply();
    }

    private void setUnitButtons(boolean lbSelected, boolean animate) {
        setSmallPillSelected(btnUnitKg, !lbSelected);
        setSmallPillSelected(btnUnitLb, lbSelected);
        if (animate) (lbSelected ? btnUnitLb : btnUnitKg).performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
    }

    private void convertVisibleFieldsOnUnitChange(boolean wasLbNowKg) {
        convertField(etStart, wasLbNowKg);
        convertField(etTarget, wasLbNowKg);
        convertField(etLogWeight, wasLbNowKg);

        updateTilHintUnit(etStart);
        updateTilHintUnit(etTarget);
        updateTilHintUnit(etLogWeight);
    }

    private void updateTilHintUnit(TextInputEditText et) {
        if (et == null) return;
        if (et.getParent() instanceof TextInputLayout) {
            TextInputLayout til = (TextInputLayout) et.getParent();
            String hint = til.getHint() == null ? "" : til.getHint().toString();
            if (hint.contains("(") && hint.contains(")")) {
                String base = hint.substring(0, hint.lastIndexOf("(")).trim();
                til.setHint(base + " (" + unitLabel() + ")");
            }
        }
    }

    private void convertField(TextInputEditText et, boolean wasLbNowKg) {
        Double v = parseDouble(et);
        if (v == null) return;
        double out = wasLbNowKg ? (v / KG_TO_LB) : (v * KG_TO_LB);
        et.setText(format1(out));
    }

    private String unitLabel() {
        return isLb() ? "lb" : "kg";
    }

    private double displayFromKg(double kg) {
        return isLb() ? (kg * KG_TO_LB) : kg;
    }

    private double kgFromDisplay(double displayValue) {
        return isLb() ? (displayValue / KG_TO_LB) : displayValue;
    }
   
    private void pickTargetDate() {
        Calendar cal = Calendar.getInstance();
        if (targetDateMs > 0) cal.setTimeInMillis(targetDateMs);

        DatePickerDialog dp = new DatePickerDialog(
                this,
                (view, year, month, day) -> {
                    Calendar chosen = Calendar.getInstance();
                    chosen.set(year, month, day);
                    targetDateMs = startOfDay(chosen.getTimeInMillis());
                    updateTargetDateButton();
                    renderProgressAndNudges();
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
        );
        dp.show();
    }

    private void pickLogDate() {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(logDateMs > 0 ? logDateMs : System.currentTimeMillis());

        DatePickerDialog dp = new DatePickerDialog(
                this,
                (view, year, month, day) -> {
                    Calendar chosen = Calendar.getInstance();
                    chosen.set(year, month, day);
                    logDateMs = startOfDay(chosen.getTimeInMillis());
                    btnPickLogDate.setText("Log date: " + df.format(new Date(logDateMs)));
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
        );
        dp.show();
    }

    private void updateTargetDateButton() {
        if (targetDateMs > 0) btnPickTargetDate.setText("Target date: " + df.format(new Date(targetDateMs)));
        else btnPickTargetDate.setText("Target date: Not set");
    }

    private void saveGoal() {
        Double startDisp = parseDouble(etStart);
        Double targetDisp = parseDouble(etTarget);

        if (startDisp == null || startDisp <= 0) { toast("Enter a valid start weight."); return; }
        if (targetDisp == null || targetDisp <= 0) { toast("Enter a valid target weight."); return; }
        if (targetDateMs <= 0) { toast("Please set a target date."); return; }

        double startKg = kgFromDisplay(startDisp);
        double targetKg = kgFromDisplay(targetDisp);

        if (startKg < 25 || startKg > 400 || targetKg < 25 || targetKg > 400) {
            toast("Weight looks out of range. Please check values.");
            return;
        }

        String tpl = sp.getString(KEY_TEMPLATE, "custom");
        if ("maintenance".equals(tpl)) {
            targetKg = startKg;
            etTarget.setText(format1(displayFromKg(targetKg)));
        }

        long today = startOfDay(System.currentTimeMillis());
        long daysLeft = Math.max(1, daysBetween(today, targetDateMs));
        double totalChange = Math.abs(startKg - targetKg);
        double perWeek = totalChange / Math.max(1.0, (daysLeft / 7.0));

        if (totalChange > 0.0001 && perWeek > 1.25) {
            final double fStartKg = startKg;
            final double fTargetKg = targetKg;
            final long fTargetDateMs = targetDateMs;

            new AlertDialog.Builder(this)
                    .setTitle("Goal pace check")
                    .setMessage("Your target implies about " + String.format(Locale.US, "%.2f", perWeek)
                            + " kg/week. based on the dates entered.\n\nyou can adjust the timeline if you want .")
                    .setNegativeButton("Edit", (d, w) -> d.dismiss())
                    .setPositiveButton("Save anyway", (d, w) -> {
                        persistGoalKg(fStartKg, fTargetKg, fTargetDateMs);
                        afterGoalSaved();
                    })
                    .show();
            return;
        }

        persistGoalKg(startKg, targetKg, targetDateMs);
        afterGoalSaved();
    }

    private void persistGoalKg(double startKg, double targetKg, long dateMs) {
        sp.edit()
                .putFloat(KEY_START, (float) startKg)
                .putFloat(KEY_TARGET, (float) targetKg)
                .putLong(KEY_TARGET_DATE, dateMs)
                .apply();
    }

    private void afterGoalSaved() {
        hideKeyboardAndClearFocus();
        toast("Goal saved.");
        renderProgressAndNudges();
        renderBadges();
        renderTrend();
        renderNutrition();
    }

    private void saveLog() {
        Double wDisp = parseDouble(etLogWeight);
        if (wDisp == null || wDisp <= 0) { toast("Enter a valid weight."); return; }

        double wKg = kgFromDisplay(wDisp);
        if (wKg < 25 || wKg > 400) { toast("Weight looks out of range. Please check."); return; }

        String note = etLogNote.getText() == null ? "" : etLogNote.getText().toString().trim();
        List<WeightLogEntry> logs = loadLogsKg();
        long day = logDateMs > 0 ? logDateMs : startOfDay(System.currentTimeMillis());

        int idx = indexOfDate(logs, day);
        if (idx >= 0) {
            new AlertDialog.Builder(this)
                    .setTitle("Replace log?")
                    .setMessage("A weigh-in already exists for " + df.format(new Date(day)) + ". Replace it?")
                    .setNegativeButton("Cancel", (d, which) -> d.dismiss())
                    .setPositiveButton("Replace", (d, which) -> {
                        logs.set(idx, new WeightLogEntry(day, wKg, note));
                        saveLogsKgSorted(logs);
                        afterLogSaved();
                    })
                    .show();
        } else {
            logs.add(new WeightLogEntry(day, wKg, note));
            saveLogsKgSorted(logs);
            afterLogSaved();
        }
    }

    private void afterLogSaved() {
        etLogWeight.setText("");
        etLogNote.setText("");
        hideKeyboardAndClearFocus();

        refreshHistory();
        renderProgressAndNudges();
        renderBadges();
        renderTrend();
        renderNutrition();
        toast("Log saved.");

        maybeNotifyGoalReached();
    }

  
    private void renderTrend() {
        List<WeightLogEntry> logsDesc = loadLogsKgSortedDesc();
        trendView.setData(logsDesc, hasGoal() ? getStartKg() : Double.NaN, hasGoal() ? getTargetKg() : Double.NaN);
    }

    private void renderBadges() {
        badgesWrap.removeAllViews();

        List<WeightLogEntry> logsDesc = loadLogsKgSortedDesc();
        List<String> badges = computeBadges(logsDesc);

        if (badges.isEmpty()) {
            TextView none = new TextView(this);
            none.setText("No milestones yet — your first weigh-in unlocks badges.");
            none.setTextColor(C_MUTED);
            none.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
            badgesWrap.addView(none);
            return;
        }

        for (String b : badges) {
            badgesWrap.addView(badgeChip(b), lpMatchWrap(0, dp(8), 0, 0));
        }
    }

    private View badgeChip(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(C_OK);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        tv.setTypeface(Typeface.DEFAULT_BOLD);
        tv.setPadding(dp(12), dp(10), dp(12), dp(10));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(C_OK_BG);
        bg.setCornerRadius(dp(16));
        bg.setStroke(dp(1), adjustAlpha(C_OK, 0.22f));
        tv.setBackground(bg);
        return tv;
    }

    private List<String> computeBadges(List<WeightLogEntry> logsDesc) {
        List<String> out = new ArrayList<>();
        if (logsDesc == null || logsDesc.isEmpty()) return out;

        out.add("Logged your first weigh-in");

        int streak = computeLoggingStreakDays(logsDesc);
        if (streak >= 7) out.add("7-day logging streak");
        if (streak >= 14) out.add("14-day logging streak");
        if (streak >= 30) out.add("30-day logging streak");

        if (hasGoal()) {
            double startKg = getStartKg();
            double targetKg = getTargetKg();
            double currentKg = logsDesc.get(0).weightKg;

            if (Math.abs(startKg - targetKg) > 0.0001) {
                double total = Math.abs(startKg - targetKg);
                double prog = Math.abs(startKg - currentKg);
                if (prog >= total * 0.5) out.add("Halfway to goal");
            }

            if (isGoalReachedKg(currentKg, startKg, targetKg)) {
                out.add("Goal reached");
            }

            double delta = currentKg - startKg;
            double absKg = Math.abs(delta);
            double thresholdKg = isLb() ? (5.0 / KG_TO_LB) : 2.0;
            if (absKg >= thresholdKg) {
                if (delta < 0) out.add("First milestone: weight down");
                else out.add("First milestone: weight up");
            }
        }

        return out;
    }

    private int computeLoggingStreakDays(List<WeightLogEntry> logsDesc) {
        if (logsDesc == null || logsDesc.isEmpty()) return 0;
        int streak = 1;
        long prev = logsDesc.get(0).dateMs;
        for (int i = 1; i < logsDesc.size(); i++) {
            long d = logsDesc.get(i).dateMs;
            long diffDays = daysBetween(d, prev);
            if (diffDays == 1) {
                streak++;
                prev = d;
            } else if (diffDays == 0) {
             
            } else {
                break;
            }
        }
        return streak;
    }

    private boolean isGoalReachedKg(double currentKg, double startKg, double targetKg) {
        if (Math.abs(startKg - targetKg) < 0.0001) return false;
        boolean losing = targetKg < startKg;
        return losing ? (currentKg <= targetKg) : (currentKg >= targetKg);
    }

    private void pickNsvDate() {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(nsvDateMs > 0 ? nsvDateMs : System.currentTimeMillis());

        DatePickerDialog dp = new DatePickerDialog(
                this,
                (view, year, month, day) -> {
                    Calendar chosen = Calendar.getInstance();
                    chosen.set(year, month, day);
                    nsvDateMs = startOfDay(chosen.getTimeInMillis());
                    btnPickNsvDate.setText("NSV date: " + df.format(new Date(nsvDateMs)));
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
        );
        dp.show();
    }

    private void saveNsv() {
        String note = etNsv.getText() == null ? "" : etNsv.getText().toString().trim();
        if (note.isEmpty()) { toast("Enter an NSV note."); return; }

        long day = nsvDateMs > 0 ? nsvDateMs : startOfDay(System.currentTimeMillis());
        List<NsvEntry> xs = loadNsv();
        xs.add(new NsvEntry(day, note));
        saveNsvSorted(xs);

        etNsv.setText("");
        hideKeyboardAndClearFocus();
        refreshNsv();
        toast("NSV saved.");
    }

    private void showNsvActions(NsvEntry e) {
        String title = df.format(new Date(e.dateMs));
        String[] opts = new String[]{"Edit", "Delete"};

        new AlertDialog.Builder(this)
                .setTitle("NSV • " + title)
                .setItems(opts, (d, which) -> {
                    if (which == 0) showEditNsvDialog(e);
                    else showDeleteNsvConfirm(e);
                })
                .show();
    }

    private void showEditNsvDialog(NsvEntry e) {
        LinearLayout wrap = new LinearLayout(this);
        wrap.setOrientation(LinearLayout.VERTICAL);
        wrap.setPadding(dp(16), dp(12), dp(16), dp(4));

        TextInputEditText n = new TextInputEditText(this);
        n.setText(e.note == null ? "" : e.note);
        n.setHint("NSV note");
        n.setTextColor(C_TEXT);
        n.setMinLines(2);
        n.setMaxLines(5);
        n.setGravity(Gravity.TOP);
        n.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);

        wrap.addView(n, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        new AlertDialog.Builder(this)
                .setTitle("Edit NSV")
                .setView(wrap)
                .setNegativeButton("Cancel", (d, x) -> d.dismiss())
                .setPositiveButton("Save", (d, x) -> {
                    String nn = n.getText() == null ? "" : n.getText().toString().trim();
                    if (nn.isEmpty()) { toast("Enter an NSV note."); return; }

                    List<NsvEntry> xs = loadNsv();
                    int idx = indexOfNsv(xs, e);
                    if (idx >= 0) {
                        xs.set(idx, new NsvEntry(e.dateMs, nn));
                        saveNsvSorted(xs);
                        refreshNsv();
                        toast("NSV updated.");
                    }
                })
                .show();
    }

    private void showDeleteNsvConfirm(NsvEntry e) {
        new AlertDialog.Builder(this)
                .setTitle("Delete NSV?")
                .setMessage("Delete NSV for " + df.format(new Date(e.dateMs)) + "?")
                .setNegativeButton("Cancel", (d, x) -> d.dismiss())
                .setPositiveButton("Delete", (d, x) -> {
                    List<NsvEntry> xs = loadNsv();
                    int idx = indexOfNsv(xs, e);
                    if (idx >= 0) {
                        xs.remove(idx);
                        saveNsvSorted(xs);
                        refreshNsv();
                        toast("Deleted.");
                    }
                })
                .show();
    }

    private int indexOfNsv(List<NsvEntry> xs, NsvEntry e) {
        for (int i = 0; i < xs.size(); i++) {
            if (xs.get(i).dateMs == e.dateMs && safeEq(xs.get(i).note, e.note)) return i;
        }
        return -1;
    }

    private boolean safeEq(String a, String b) {
        if (a == null && b == null) return true;
        if (a == null) return false;
        return a.equals(b);
    }

    private void refreshNsv() {
        List<NsvEntry> xs = loadNsvSortedDesc();
        nsvAdapter.setItems(xs);
        tvEmptyNsv.setVisibility(xs.isEmpty() ? View.VISIBLE : View.GONE);
    }

    
    private void renderProgressAndNudges() {
        renderProgressCore();
        renderSmartNudges();
    }

    private void renderSmartNudges() {
        long lastDismiss = sp.getLong(KEY_LAST_NUDGE_DISMISS_MS, 0L);
        if (System.currentTimeMillis() - lastDismiss < TimeUnit.HOURS.toMillis(12)) {
            tvNudge.setVisibility(View.GONE);
            btnNudgeAction.setVisibility(View.GONE);
            return;
        }

        List<WeightLogEntry> logs = loadLogsKgSortedDesc();
        long today = startOfDay(System.currentTimeMillis());

        if (logs.isEmpty()) {
            tvNudge.setVisibility(View.GONE);
            btnNudgeAction.setVisibility(View.GONE);
            return;
        }

        long lastLogDay = logs.get(0).dateMs;
        long daysSince = Math.max(0, daysBetween(lastLogDay, today));

        if (daysSince >= 5) {
            tvNudge.setText("No weigh-in in " + daysSince + " days. A single data point can reset motivation — log today’s weigh-in.");
            tvNudge.setVisibility(View.VISIBLE);
            btnNudgeAction.setVisibility(View.GONE);
            return;
        }

        if (shouldOfferRecalibration()) {
            tvNudge.setText("Your target date has passed. Want to recalibrate your timeline based on your recent trend?");
            tvNudge.setVisibility(View.VISIBLE);
            btnNudgeAction.setText("Recalibrate timeline");
            btnNudgeAction.setVisibility(View.VISIBLE);
            return;
        }

        if (!hasGoal() || targetDateMs <= 0) {
            tvNudge.setVisibility(View.GONE);
            btnNudgeAction.setVisibility(View.GONE);
            return;
        }

        double startKg = getStartKg();
        double targetKg = getTargetKg();
        if (Math.abs(startKg - targetKg) < 0.0001) {
            tvNudge.setVisibility(View.GONE);
            btnNudgeAction.setVisibility(View.GONE);
            return;
        }

        double currentKg = logs.get(0).weightKg;
        double totalNeeded = Math.abs(startKg - targetKg);
        double progressed = Math.abs(startKg - currentKg);
        double remaining = Math.max(0, totalNeeded - progressed);

        long daysLeft = Math.max(0, daysBetween(today, targetDateMs));
        if (daysLeft == 0 || remaining == 0) {
            tvNudge.setVisibility(View.GONE);
            btnNudgeAction.setVisibility(View.GONE);
            return;
        }

        double weeksLeft = Math.max(1.0, daysLeft / 7.0);
        double neededPerWeekKg = remaining / weeksLeft;

        TrendMetrics tm = computeTrendKg(logs);
        double observedDeltaKg = tm.weekOverWeekChangeKg;

        boolean losingGoal = targetKg < startKg;
        if (Double.isNaN(observedDeltaKg)) {
            tvNudge.setVisibility(View.GONE);
            btnNudgeAction.setVisibility(View.GONE);
            return;
        }

        double observedPerWeekKg = observedDeltaKg;
        boolean offDirection = losingGoal ? (observedPerWeekKg > 0.10) : (observedPerWeekKg < -0.10);

        double observedMag = Math.abs(observedPerWeekKg);
        boolean tooSlow = observedMag + 0.05 < neededPerWeekKg;

        if (offDirection || tooSlow) {
            String dir = losingGoal ? "loss" : "gain";
            String msg = "You may be off-pace.\n"
                    + "Needed: " + String.format(Locale.US, "%.2f", neededPerWeekKg) + " kg/week (" + dir + ")\n"
                    + "Trend: " + String.format(Locale.US, "%.2f", observedPerWeekKg) + " kg/week\n"
                    + "Consider extending your target date for a more realistic timeline.";
            tvNudge.setText(msg);
            tvNudge.setVisibility(View.VISIBLE);
            btnNudgeAction.setText("Adjust target date");
            btnNudgeAction.setVisibility(View.VISIBLE);
            return;
        }

        tvNudge.setVisibility(View.GONE);
        btnNudgeAction.setVisibility(View.GONE);
    }

    private boolean shouldOfferRecalibration() {
        if (!hasGoal()) return false;
        long td = sp.getLong(KEY_TARGET_DATE, -1L);
        if (td <= 0) return false;

        long today = startOfDay(System.currentTimeMillis());
        if (today <= td) return false;

        List<WeightLogEntry> logs = loadLogsKgSortedDesc();
        if (logs.isEmpty()) return true;

        double startKg = getStartKg();
        double targetKg = getTargetKg();
        double currentKg = logs.get(0).weightKg;

        if (Math.abs(startKg - targetKg) < 0.0001) return false;
        return !isGoalReachedKg(currentKg, startKg, targetKg);
    }

    private void showRecalibrateDialog() {
        if (!hasGoal()) return;

        List<WeightLogEntry> logs = loadLogsKgSortedDesc();
        double startKg = getStartKg();
        double targetKg = getTargetKg();
        double currentKg = logs.isEmpty() ? startKg : logs.get(0).weightKg;

        double remainingKg = Math.max(0, Math.abs(currentKg - targetKg));
        if (remainingKg < 0.0001) {
            toast("You are already at your target.");
            return;
        }

        double suggestedRateKgPerWeek = suggestedSafeRateKgPerWeek();

        TrendMetrics tm = computeTrendKg(logs);
        boolean losingGoal = targetKg < startKg;
        if (!Double.isNaN(tm.weekOverWeekChangeKg)) {
            double obs = tm.weekOverWeekChangeKg;
            boolean correctDirection = losingGoal ? (obs < -0.05) : (obs > 0.05);
            if (correctDirection) {
                double abs = Math.min(Math.abs(obs), 1.25);
                if (abs > 0.05) suggestedRateKgPerWeek = Math.max(0.15, abs);
            }
        }

        int weeks = (int) Math.ceil(remainingKg / suggestedRateKgPerWeek);
        weeks = Math.max(2, weeks);

        long today = startOfDay(System.currentTimeMillis());
        long newDate = startOfDay(today + TimeUnit.DAYS.toMillis(weeks * 7L));

        String msg = "Remaining: " + String.format(Locale.US, "%.1f", remainingKg) + " kg\n"
                + "Suggested pace: " + String.format(Locale.US, "%.2f", suggestedRateKgPerWeek) + " kg/week\n\n"
                + "Set a new target date: " + df.format(new Date(newDate)) + "?";

        new AlertDialog.Builder(this)
                .setTitle("Recalibrate timeline")
                .setMessage(msg)
                .setNegativeButton("Cancel", (d, w) -> d.dismiss())
                .setPositiveButton("Set date", (d, w) -> {
                    targetDateMs = newDate;
                    sp.edit().putLong(KEY_TARGET_DATE, newDate).apply();
                    updateTargetDateButton();
                    renderProgressAndNudges();
                    toast("Timeline recalibrated.");
                })
                .show();
    }

    private double suggestedReferenceRateKgPerWeek()
    {
        String tpl = sp.getString(KEY_TEMPLATE, "custom");
        if ("fat_loss".equals(tpl)) return 0.5;
        if ("lean_bulk".equals(tpl)) return 0.25;
        return 0.4;
    }
    private double suggestedSafeRateKgPerWeek() {

        double base = suggestedReferenceRateKgPerWeek();


        double min = 0.15;  
        double max = 1.25;  


        String tpl = sp.getString(KEY_TEMPLATE, "custom");
        if ("maintenance".equals(tpl)) return 0.25;

     
        if (base < min) base = min;
        if (base > max) base = max;

        return base;
    }

 
    private void renderProgressCore() {
        if (!hasGoal()) {
            tvSummary.setText("Set a goal to see progress.");
            progressBar.setProgress(0);
            tvPercent.setText("0%");
            return;
        }

        double startKg = getStartKg();
        double targetKg = getTargetKg();
        long targetMs = sp.getLong(KEY_TARGET_DATE, -1L);

        if (Math.abs(startKg - targetKg) < 0.0001) {
            List<WeightLogEntry> logs = loadLogsKgSortedDesc();
            TrendMetrics tm = computeTrendKg(logs);

            long today = startOfDay(System.currentTimeMillis());
            long daysLeft = targetMs > 0 ? Math.max(0, daysBetween(today, targetMs)) : 0;
            String when = targetMs > 0
                    ? ("Target date: " + df.format(new Date(targetMs)) + " (" + daysLeft + " days left)")
                    : "Target date not set";

            String trendLine = "";
            if (!Double.isNaN(tm.avg7Kg)) {
                trendLine = "\n7-entry avg: " + format1(displayFromKg(tm.avg7Kg)) + " " + unitLabel();
                if (!Double.isNaN(tm.weekOverWeekChangeKg)) {
                    trendLine += "\nRate of change: " + formatSigned(tm.weekOverWeekChangeKg) + " kg/week";
                }
            }

            tvSummary.setText(String.format(
                    Locale.US,
                    "Maintenance goal\nStart/Target: %s %s\n%s%s\nAdd weigh-ins to monitor consistency.",
                    format1(displayFromKg(startKg)), unitLabel(), when, trendLine
            ));
            progressBar.setProgress(100);
            tvPercent.setText("100%");
            return;
        }

        List<WeightLogEntry> logs = loadLogsKgSortedDesc();
        Double currentKg = logs.isEmpty() ? null : logs.get(0).weightKg;

        if (currentKg == null) {
            tvSummary.setText("Goal saved. Add your first weigh-in to track progress.");
            progressBar.setProgress(0);
            tvPercent.setText("0%");
            return;
        }

        double totalChangeNeeded = Math.abs(startKg - targetKg);
        double changeSoFar = Math.abs(startKg - currentKg);
        double remainingKg = Math.max(0, totalChangeNeeded - changeSoFar);

        int pct = totalChangeNeeded == 0 ? 100 : (int) Math.round((changeSoFar / totalChangeNeeded) * 100.0);
        pct = Math.max(0, Math.min(100, pct));

        long today = startOfDay(System.currentTimeMillis());
        long daysLeft = targetMs > 0 ? Math.max(0, daysBetween(today, targetMs)) : 0;

        String paceLine = "";
        if (daysLeft > 0 && remainingKg > 0) {
            double weeksLeft = Math.max(1.0, daysLeft / 7.0);
            double neededPerWeekKg = remainingKg / weeksLeft;

            String safetyHint = "";
            if (neededPerWeekKg > 1.25) safetyHint = " (aggressive)";
            else if (neededPerWeekKg > 0.9) safetyHint = " (fast)";

            paceLine = String.format(Locale.US, "\nNeeded pace: %.2f kg/week%s", neededPerWeekKg, safetyHint);
        }

        String dateLine = targetMs > 0
                ? String.format(Locale.US, "\nTarget: %s (%d days left)", df.format(new Date(targetMs)), daysLeft)
                : "";

        TrendMetrics tm = computeTrendKg(logs);
        String trend = "";
        if (!Double.isNaN(tm.avg7Kg)) {
            trend = "\n7-entry avg: " + format1(displayFromKg(tm.avg7Kg)) + " " + unitLabel();
            if (!Double.isNaN(tm.weekOverWeekChangeKg)) {
                trend += "\nRate of change: " + formatSigned(tm.weekOverWeekChangeKg) + " kg/week";
            }
        }

        String summary = String.format(
                Locale.US,
                "Start: %s %s  •  Current: %s %s  •  Target: %s %s\nRemaining: %s %s  •  %d%% complete%s%s%s",
                format1(displayFromKg(startKg)), unitLabel(),
                format1(displayFromKg(currentKg)), unitLabel(),
                format1(displayFromKg(targetKg)), unitLabel(),
                format1(displayFromKg(remainingKg)), unitLabel(),
                pct, dateLine, paceLine, trend
        );

        tvSummary.setText(summary);
        progressBar.setProgress(pct);
        tvPercent.setText(pct + "%");
    }

    private String formatSigned(double kgPerWeek) {
        String sign = kgPerWeek > 0 ? "+" : "";
        return sign + String.format(Locale.US, "%.2f", kgPerWeek);
    }

    private TrendMetrics computeTrendKg(List<WeightLogEntry> logsSortedDesc) {
        if (logsSortedDesc == null || logsSortedDesc.isEmpty()) return TrendMetrics.empty();

        List<Double> w7 = new ArrayList<>();
        List<Double> wPrev7 = new ArrayList<>();

        for (int i = 0; i < logsSortedDesc.size(); i++) {
            if (i < 7) w7.add(logsSortedDesc.get(i).weightKg);
            else if (i < 14) wPrev7.add(logsSortedDesc.get(i).weightKg);
            else break;
        }

        double avg7 = average(w7);
        double prevAvg7 = average(wPrev7);

        double delta = (!Double.isNaN(avg7) && !Double.isNaN(prevAvg7)) ? (avg7 - prevAvg7) : Double.NaN;
        return new TrendMetrics(avg7, delta, logsSortedDesc.get(0).dateMs);
    }

    private double average(List<Double> xs) {
        if (xs == null || xs.isEmpty()) return Double.NaN;
        double s = 0;
        for (double x : xs) s += x;
        return s / xs.size();
    }

    private void maybeNotifyGoalReached() {
        if (!hasGoal()) return;
        List<WeightLogEntry> logs = loadLogsKgSortedDesc();
        if (logs.isEmpty()) return;

        double startKg = getStartKg();
        double targetKg = getTargetKg();
        double currentKg = logs.get(0).weightKg;

        if (Math.abs(startKg - targetKg) < 0.0001) return;

        boolean reached = isGoalReachedKg(currentKg, startKg, targetKg);
        if (!reached) return;

        ensureNotificationPermissionThen(() -> {
            NotificationCompat.Builder nb = new NotificationCompat.Builder(this, CH_ID)
                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                    .setContentTitle("Goal reached")
                    .setContentText("You hit your target (" + format1(displayFromKg(targetKg)) + " " + unitLabel() + ").")
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setAutoCancel(true);

            NotificationManagerCompat.from(this).notify(NOTIF_ID_GOAL, nb.build());


        });
    }

    private void showLogActions(WeightLogEntry e) {
        String title = format1(displayFromKg(e.weightKg)) + " " + unitLabel() + " • " + df.format(new Date(e.dateMs));
        String[] opts = new String[]{"Edit", "Delete"};

        new AlertDialog.Builder(this)
                .setTitle(title)
                .setItems(opts, (d, which) -> {
                    if (which == 0) showEditLogDialog(e);
                    else showDeleteLogConfirm(e);
                })
                .show();
    }

    private void showEditLogDialog(WeightLogEntry e) {
        LinearLayout wrap = new LinearLayout(this);
        wrap.setOrientation(LinearLayout.VERTICAL);
        wrap.setPadding(dp(16), dp(12), dp(16), dp(4));

        TextInputEditText w = new TextInputEditText(this);
        w.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        w.setText(format1(displayFromKg(e.weightKg)));
        w.setHint("Weight (" + unitLabel() + ")");
        w.setTextColor(C_TEXT);

        TextInputEditText n = new TextInputEditText(this);
        n.setText(e.note == null ? "" : e.note);
        n.setHint("Optional note");
        n.setTextColor(C_TEXT);

        wrap.addView(w, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        Space s = new Space(this); s.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(10)));
        wrap.addView(s);
        wrap.addView(n, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        new AlertDialog.Builder(this)
                .setTitle("Edit log")
                .setView(wrap)
                .setNegativeButton("Cancel", (d, x) -> d.dismiss())
                .setPositiveButton("Save", (d, x) -> {
                    Double newWDisp = parseDouble(w);
                    if (newWDisp == null || newWDisp <= 0) { toast("Enter a valid weight."); return; }

                    double newWKg = kgFromDisplay(newWDisp);
                    if (newWKg < 25 || newWKg > 400) { toast("Weight looks out of range."); return; }

                    List<WeightLogEntry> logs = loadLogsKg();
                    int idx = indexOfDate(logs, e.dateMs);
                    if (idx >= 0) {
                        logs.set(idx, new WeightLogEntry(e.dateMs, newWKg, n.getText() == null ? "" : n.getText().toString().trim()));
                        saveLogsKgSorted(logs);
                        refreshHistory();
                        renderProgressAndNudges();
                        renderBadges();
                        renderTrend();
                        renderNutrition();
                        toast("Log updated.");
                        maybeNotifyGoalReached();
                    }
                })
                .show();
    }

    private void showDeleteLogConfirm(WeightLogEntry e) {
        new AlertDialog.Builder(this)
                .setTitle("Delete log?")
                .setMessage("Delete weigh-in for " + df.format(new Date(e.dateMs)) + "?")
                .setNegativeButton("Cancel", (d, x) -> d.dismiss())
                .setPositiveButton("Delete", (d, x) -> {
                    List<WeightLogEntry> logs = loadLogsKg();
                    int idx = indexOfDate(logs, e.dateMs);
                    if (idx >= 0) {
                        logs.remove(idx);
                        saveLogsKgSorted(logs);
                        refreshHistory();
                        renderProgressAndNudges();
                        renderBadges();
                        renderTrend();
                        renderNutrition();
                        toast("Deleted.");
                    }
                })
                .show();
    }

    private void refreshHistory() {
        List<WeightLogEntry> logs = loadLogsKgSortedDesc();
        adapter.setItems(logs, isLb());
        tvEmptyHistory.setVisibility(logs.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void clearAll() {
        new AlertDialog.Builder(this)
                .setTitle("Clear all data?")
                .setMessage("This will remove your goal, logs, NSVs, and reminder settings from this device.")
                .setNegativeButton("Cancel", (d, w) -> d.dismiss())
                .setPositiveButton("Clear", (d, w) -> {
                    cancelDailyReminder();
                    sp.edit().clear().apply();

                    targetDateMs = -1L;
                    logDateMs = startOfDay(System.currentTimeMillis());
                    nsvDateMs = startOfDay(System.currentTimeMillis());

                    etStart.setText("");
                    etTarget.setText("");
                    etLogWeight.setText("");
                    etLogNote.setText("");
                    etNsv.setText("");

                    swReminder.setChecked(false);
                    updateReminderTimeButtonText();

                    setUnit(false);
                    setUnitButtons(false, false);
                    sp.edit().putString(KEY_TEMPLATE, "custom").apply();
                    setTemplateButtons("custom", false);
                    updateTemplateHint("custom");

                    sp.edit().putBoolean(KEY_SEX_MALE, true).putInt(KEY_ACTIVITY, 1).apply();
                    etAge.setText("");
                    etHeight.setText("");

                    updateTargetDateButton();
                    btnPickLogDate.setText("Log date: " + df.format(new Date(logDateMs)));
                    btnPickNsvDate.setText("NSV date: " + df.format(new Date(nsvDateMs)));

                    refreshHistory();
                    refreshNsv();
                    renderProgressAndNudges();
                    renderBadges();
                    renderTrend();
                    renderNutrition();
                    toast("Cleared.");
                })
                .show();
    }

   
    private void persistNutritionInputs() {
        Integer age = parseInt(etAge);
        Integer height = parseInt(etHeight);
        SharedPreferences.Editor ed = sp.edit();
        if (age != null) ed.putInt(KEY_AGE, age);
        if (height != null) ed.putInt(KEY_HEIGHT_CM, height);
        ed.apply();
    }

    private void setSex(boolean male) {
        sp.edit().putBoolean(KEY_SEX_MALE, male).apply();
        setSmallPillSelected(btnSexMale, male);
        setSmallPillSelected(btnSexFemale, !male);
    }

    private void setActivity(int idx) {
        sp.edit().putInt(KEY_ACTIVITY, idx).apply();
        setSmallPillSelected(btnActSed, idx == 0);
        setSmallPillSelected(btnActLight, idx == 1);
        setSmallPillSelected(btnActMod, idx == 2);
        setSmallPillSelected(btnActVery, idx == 3);
        setSmallPillSelected(btnActExtra, idx == 4);
    }

    private void renderNutrition() {
        boolean male = sp.getBoolean(KEY_SEX_MALE, true);
        setSex(male);
        int act = sp.getInt(KEY_ACTIVITY, 1);
        setActivity(act);

        int age = sp.getInt(KEY_AGE, 0);
        int height = sp.getInt(KEY_HEIGHT_CM, 0);
        if (TextUtils.isEmpty(textOf(etAge)) && age > 0) etAge.setText(String.valueOf(age));
        if (TextUtils.isEmpty(textOf(etHeight)) && height > 0) etHeight.setText(String.valueOf(height));

        Double weightKg = getBestWeightForNutritionKg();
        Integer ageVal = parseInt(etAge);
        Integer heightVal = parseInt(etHeight);

        if (weightKg == null || ageVal == null || heightVal == null || ageVal <= 0 || heightVal <= 0) {
            tvNutritionOut.setText("Enter age and height to calculate targets. Targets use your latest weigh-in (or start weight).");
            return;
        }

        double w = weightKg;
        int a = ageVal;
        int h = heightVal;

        double bmr = 10.0 * w + 6.25 * h - 5.0 * a + (male ? 5.0 : -161.0);
        double factor = activityFactor(act);
        double tdee = bmr * factor;

        String tpl = sp.getString(KEY_TEMPLATE, "custom");
        double targetCalories = tdee;
        String goalLabel = "Maintenance";
        if ("fat_loss".equals(tpl)) { targetCalories = tdee - 500.0; goalLabel = "Weight loss"; }
        else if ("lean_bulk".equals(tpl)) { targetCalories = tdee + 250.0; goalLabel = "Lean bulk"; }
        else if ("maintenance".equals(tpl)) { targetCalories = tdee; goalLabel = "Maintenance"; }
        else { targetCalories = tdee; goalLabel = "Maintenance (custom)"; }

        if (male && targetCalories < 1500) targetCalories = 1500;
        if (!male && targetCalories < 1200) targetCalories = 1200;

        double proteinG = 1.6 * w;
        double fatG = 0.8 * w;

        double proteinKcal = proteinG * 4.0;
        double fatKcal = fatG * 9.0;
        double remainingKcal = Math.max(0, targetCalories - proteinKcal - fatKcal);
        double carbsG = remainingKcal / 4.0;

        String out = ""
                + "Mode: " + goalLabel + "\n"
                + "Estimated energy (reference): " + (int) Math.round(tdee) + " kcal/day\n"
                + "Suggested intake (reference): " + (int) Math.round(targetCalories) + " kcal/day\n\n"
                + "Macro split (reference)\n"
                + "Protein: " + (int) Math.round(proteinG) + " g\n"
                + "Fat: " + (int) Math.round(fatG) + " g\n"
                + "Carbs: " + (int) Math.round(carbsG) + " g\n\n"
                + "For personal tracking only. Not medical advice.";
        tvNutritionOut.setText(out);

    }

    private double activityFactor(int idx) {
        switch (idx) {
            case 0: return 1.2;
            case 1: return 1.375;
            case 2: return 1.55;
            case 3: return 1.725;
            case 4: return 1.9;
            default: return 1.375;
        }
    }

    private Double getBestWeightForNutritionKg() {
        List<WeightLogEntry> logs = loadLogsKgSortedDesc();
        if (!logs.isEmpty()) return logs.get(0).weightKg;
        if (hasGoal()) return getStartKg();
        return null;
    }
  
    private boolean hasGoal() {
        return sp.contains(KEY_START) && sp.contains(KEY_TARGET);
    }

    private double getStartKg() {
        return sp.getFloat(KEY_START, 0f);
    }

    private double getTargetKg() {
        return sp.getFloat(KEY_TARGET, 0f);
    }

    private List<WeightLogEntry> loadLogsKg() {
        String json = sp.getString(KEY_LOGS, "");
        List<WeightLogEntry> out = new ArrayList<>();
        if (TextUtils.isEmpty(json)) return out;

        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                long d = o.getLong("dateMs");
                double w = o.getDouble("weightKg");
                String n = o.optString("note", "");
                out.add(new WeightLogEntry(d, w, n));
            }
        } catch (Exception ignored) {}
        return out;
    }

    private List<WeightLogEntry> loadLogsKgSortedDesc() {
        List<WeightLogEntry> logs = loadLogsKg();
        Collections.sort(logs, (a, b) -> Long.compare(b.dateMs, a.dateMs));
        return logs;
    }

    private void saveLogsKgSorted(List<WeightLogEntry> logs) {
        Collections.sort(logs, (a, b) -> Long.compare(b.dateMs, a.dateMs));
        JSONArray arr = new JSONArray();
        for (WeightLogEntry e : logs) {
            JSONObject o = new JSONObject();
            try {
                o.put("dateMs", e.dateMs);
                o.put("weightKg", e.weightKg);
                o.put("note", e.note == null ? "" : e.note);
                arr.put(o);
            } catch (JSONException ignored) {}
        }
        sp.edit().putString(KEY_LOGS, arr.toString()).apply();
    }

    private int indexOfDate(List<WeightLogEntry> logs, long dateMs) {
        for (int i = 0; i < logs.size(); i++) {
            if (logs.get(i).dateMs == dateMs) return i;
        }
        return -1;
    }

    // NSV storage
    private List<NsvEntry> loadNsv() {
        String json = sp.getString(KEY_NSV, "");
        List<NsvEntry> out = new ArrayList<>();
        if (TextUtils.isEmpty(json)) return out;

        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                long d = o.getLong("dateMs");
                String n = o.optString("note", "");
                out.add(new NsvEntry(d, n));
            }
        } catch (Exception ignored) {}
        return out;
    }

    private List<NsvEntry> loadNsvSortedDesc() {
        List<NsvEntry> xs = loadNsv();
        Collections.sort(xs, (a, b) -> Long.compare(b.dateMs, a.dateMs));
        return xs;
    }

    private void saveNsvSorted(List<NsvEntry> xs) {
        Collections.sort(xs, (a, b) -> Long.compare(b.dateMs, a.dateMs));
        JSONArray arr = new JSONArray();
        for (NsvEntry e : xs) {
            JSONObject o = new JSONObject();
            try {
                o.put("dateMs", e.dateMs);
                o.put("note", e.note == null ? "" : e.note);
                arr.put(o);
            } catch (JSONException ignored) {}
        }
        sp.edit().putString(KEY_NSV, arr.toString()).apply();
    }

   
    private void ensureNotificationPermissionThen(@NonNull Runnable run) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
                return;
            }
        }
        run.run();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(
                    CH_ID,
                    "Weight Goals",
                    NotificationManager.IMPORTANCE_HIGH
            );
            ch.setDescription("Reminders and updates for Weight Goals.");
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) nm.createNotificationChannel(ch);
        }
    }

    private void updateReminderTimeButtonText() {
        int h = sp.getInt(KEY_REMINDER_HOUR, 9);
        int m = sp.getInt(KEY_REMINDER_MIN, 0);
        String time = String.format(Locale.getDefault(), "%02d:%02d", h, m);
        btnPickReminderTime.setText("Reminder time: " + time);
    }

    private void scheduleDailyReminder() {
        int hour = sp.getInt(KEY_REMINDER_HOUR, 9);
        int min = sp.getInt(KEY_REMINDER_MIN, 0);

        long triggerAt = nextTriggerAt(hour, min);

        AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
        if (am == null) return;

        PendingIntent pi = reminderPendingIntent(this);
        am.cancel(pi);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi);

        } else {
            am.setExact(AlarmManager.RTC_WAKEUP, triggerAt, pi);
        }
    }

    private void cancelDailyReminder() {
        AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
        if (am == null) return;
        am.cancel(reminderPendingIntent(this));
    }

    private long nextTriggerAt(int hour, int min) {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        c.set(Calendar.HOUR_OF_DAY, hour);
        c.set(Calendar.MINUTE, min);

        long now = System.currentTimeMillis();
        if (c.getTimeInMillis() <= now + 2000) c.add(Calendar.DAY_OF_YEAR, 1);
        return c.getTimeInMillis();
    }

    private static PendingIntent reminderPendingIntent(Context ctx) {
        Intent i = new Intent(ctx, ReminderReceiver.class);
        i.setAction("com.cypher.zealth.WEIGHT_REMINDER");
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) flags |= PendingIntent.FLAG_IMMUTABLE;
        return PendingIntent.getBroadcast(ctx, 9911, i, flags);
    }

    public static class ReminderReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            SharedPreferences sp = context.getSharedPreferences(PREF, Context.MODE_PRIVATE);
            boolean enabled = sp.getBoolean(KEY_REMINDER_ENABLED, false);
            if (!enabled) return;

            NotificationCompat.Builder nb = new NotificationCompat.Builder(context, CH_ID)
                    .setSmallIcon(android.R.drawable.ic_popup_reminder)
                    .setContentTitle("Weigh-in reminder")
                    .setContentText("Log today’s weigh-in to stay on track.")
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setAutoCancel(true);

            NotificationManagerCompat.from(context).notify(NOTIF_ID_REMINDER, nb.build());

            AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            if (am != null) {
                int hour = sp.getInt(KEY_REMINDER_HOUR, 9);
                int min = sp.getInt(KEY_REMINDER_MIN, 0);

                Calendar c = Calendar.getInstance();
                c.set(Calendar.SECOND, 0);
                c.set(Calendar.MILLISECOND, 0);
                c.set(Calendar.HOUR_OF_DAY, hour);
                c.set(Calendar.MINUTE, min);
                c.add(Calendar.DAY_OF_YEAR, 1);

                PendingIntent pi = reminderPendingIntent(context);
                am.cancel(pi);

                long triggerAt = c.getTimeInMillis();
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi);
                } else {
                    am.setExact(AlarmManager.RTC_WAKEUP, triggerAt, pi);
                }
            }
        }
    }

    
    private boolean deviceSupportsBiometric() {
        BiometricManager bm = BiometricManager.from(this);
        int can = bm.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG
                | BiometricManager.Authenticators.BIOMETRIC_WEAK);
        return can == BiometricManager.BIOMETRIC_SUCCESS;
    }

    private void promptBiometricOrExit() {
        if (!deviceSupportsBiometric()) {
            toast("Biometrics not available. Disabling lock.");
            sp.edit().putBoolean(KEY_BIOMETRIC_LOCK, false).apply();
            biometricAuthedThisSession = true;
            if (swBiometricLock != null) swBiometricLock.setChecked(false);
            return;
        }

        BiometricPrompt prompt = new BiometricPrompt(
                this,
                ContextCompat.getMainExecutor(this),
                new BiometricPrompt.AuthenticationCallback() {
                    @Override
                    public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                        biometricAuthedThisSession = true;
                        toast("Unlocked.");
                    }

                    @Override
                    public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                        biometricAuthedThisSession = false;
                        toast("Locked: " + errString);
                        finish();
                    }

                    @Override
                    public void onAuthenticationFailed() {
                        // no-op
                    }
                }
        );

        BiometricPrompt.PromptInfo info = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Unlock Weight Goals")
                .setSubtitle("Biometric verification required")
                .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG
                        | BiometricManager.Authenticators.BIOMETRIC_WEAK)
                .setNegativeButtonText("Exit")
                .build();

        prompt.authenticate(info);
    }

  
    public static class WeightLogEntry {
        public final long dateMs;
        public final double weightKg;
        public final String note;

        public WeightLogEntry(long dateMs, double weightKg, String note) {
            this.dateMs = dateMs;
            this.weightKg = weightKg;
            this.note = note == null ? "" : note;
        }
    }

    public static class WeightLogAdapter extends RecyclerView.Adapter<WeightLogAdapter.VH> {

        public interface Listener { void onLongPress(WeightLogEntry e); }

        private final SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        private final Listener listener;
        private List<WeightLogEntry> items;
        private boolean unitLb = false;

        public WeightLogAdapter(List<WeightLogEntry> items, Listener listener) {
            this.items = items;
            this.listener = listener;
            setHasStableIds(true);
        }

        public void setItems(List<WeightLogEntry> items, boolean unitLb) {
            this.items = items;
            this.unitLb = unitLb;
            notifyDataSetChanged();
        }

        @Override public long getItemId(int position) { return items.get(position).dateMs; }

        @NonNull
        @Override public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            Context ctx = parent.getContext();

            MaterialCardView card = new MaterialCardView(ctx);
            card.setCardElevation(dp(ctx, 5));
            card.setRadius(dp(ctx, 18));
            card.setUseCompatPadding(true);
            card.setCardBackgroundColor(Color.WHITE);
            card.setStrokeWidth(dp(ctx, 1));
            card.setStrokeColor(Color.parseColor("#2A93C5FD"));

            LinearLayout row = new LinearLayout(ctx);
            row.setOrientation(LinearLayout.VERTICAL);
            row.setPadding(dp(ctx, 14), dp(ctx, 12), dp(ctx, 14), dp(ctx, 12));
            card.addView(row, new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ));

            TextView tvTitle = new TextView(ctx);
            tvTitle.setTextColor(Color.parseColor("#0B1B3A"));
            tvTitle.setTypeface(Typeface.DEFAULT_BOLD);
            tvTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
            row.addView(tvTitle);

            LinearLayout metaRow = new LinearLayout(ctx);
            metaRow.setOrientation(LinearLayout.HORIZONTAL);
            metaRow.setGravity(Gravity.CENTER_VERTICAL);
            row.addView(metaRow, new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ));

            TextView tvMeta = new TextView(ctx);
            tvMeta.setTextColor(Color.parseColor("#2B5AA6"));
            tvMeta.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
            LinearLayout.LayoutParams metaLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            metaRow.addView(tvMeta, metaLp);

            TextView tvRelative = new TextView(ctx);
            tvRelative.setTextColor(Color.parseColor("#2563EB"));
            tvRelative.setTypeface(Typeface.DEFAULT_BOLD);
            tvRelative.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f);
            metaRow.addView(tvRelative);

            TextView tvNote = new TextView(ctx);
            tvNote.setTextColor(Color.parseColor("#2B5AA6"));
            tvNote.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
            tvNote.setLineSpacing(dp(ctx, 2), 1f);
            tvNote.setPadding(0, dp(ctx, 6), 0, 0);
            row.addView(tvNote);

            RecyclerView.LayoutParams rlp = new RecyclerView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            );
            rlp.setMargins(0, dp(ctx, 10), 0, 0);
            card.setLayoutParams(rlp);

            return new VH(card, tvTitle, tvMeta, tvRelative, tvNote);
        }

        @Override public void onBindViewHolder(@NonNull VH h, int position) {
            WeightLogEntry e = items.get(position);

            double display = unitLb ? (e.weightKg * KG_TO_LB) : e.weightKg;
            String unit = unitLb ? "lb" : "kg";

            h.tvTitle.setText(String.format(Locale.US, "%.1f %s", display, unit));
            h.tvMeta.setText(df.format(new Date(e.dateMs)));

            String rel = relativeLabel(e.dateMs);
            if (rel != null) {
                h.tvRelative.setText(rel);
                h.tvRelative.setVisibility(View.VISIBLE);
            } else {
                h.tvRelative.setVisibility(View.GONE);
            }

            String note = e.note == null ? "" : e.note.trim();
            if (note.isEmpty()) {
                h.tvNote.setVisibility(View.GONE);
            } else {
                h.tvNote.setVisibility(View.VISIBLE);
                h.tvNote.setText(note);
            }

            h.itemView.setOnLongClickListener(v -> {
                v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                if (listener != null) listener.onLongPress(e);
                return true;
            });
        }

        @Override public int getItemCount() { return items == null ? 0 : items.size(); }

        static class VH extends RecyclerView.ViewHolder {
            TextView tvTitle, tvMeta, tvRelative, tvNote;
            VH(@NonNull View itemView, TextView t1, TextView t2, TextView t3, TextView t4) {
                super(itemView);
                tvTitle = t1; tvMeta = t2; tvRelative = t3; tvNote = t4;
            }
        }

        private String relativeLabel(long dateMs) {
            long today = startOfDayStatic(System.currentTimeMillis());
            long yesterday = today - 24L * 60L * 60L * 1000L;
            if (dateMs == today) return "Today";
            if (dateMs == yesterday) return "Yesterday";
            return null;
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

        private static int dp(Context ctx, int v) {
            return (int) TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP, v, ctx.getResources().getDisplayMetrics()
            );
        }
    }

    public static class NsvEntry {
        public final long dateMs;
        public final String note;
        public NsvEntry(long dateMs, String note) {
            this.dateMs = dateMs;
            this.note = note == null ? "" : note;
        }
    }

    public static class NsvAdapter extends RecyclerView.Adapter<NsvAdapter.VH> {
        public interface Listener { void onLongPress(NsvEntry e); }
        private final SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        private List<NsvEntry> items;
        private final Listener listener;

        public NsvAdapter(List<NsvEntry> items, Listener listener) {
            this.items = items;
            this.listener = listener;
            setHasStableIds(false);
        }

        public void setItems(List<NsvEntry> items) {
            this.items = items;
            notifyDataSetChanged();
        }

        @NonNull
        @Override public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            Context ctx = parent.getContext();

            MaterialCardView card = new MaterialCardView(ctx);
            card.setCardElevation(dp(ctx, 5));
            card.setRadius(dp(ctx, 18));
            card.setUseCompatPadding(true);
            card.setCardBackgroundColor(Color.WHITE);
            card.setStrokeWidth(dp(ctx, 1));
            card.setStrokeColor(Color.parseColor("#2A93C5FD"));

            LinearLayout col = new LinearLayout(ctx);
            col.setOrientation(LinearLayout.VERTICAL);
            col.setPadding(dp(ctx, 14), dp(ctx, 12), dp(ctx, 14), dp(ctx, 12));
            card.addView(col, new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ));

            TextView tvDate = new TextView(ctx);
            tvDate.setTextColor(Color.parseColor("#0B1B3A"));
            tvDate.setTypeface(Typeface.DEFAULT_BOLD);
            tvDate.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
            col.addView(tvDate);

            TextView tvNote = new TextView(ctx);
            tvNote.setTextColor(Color.parseColor("#2B5AA6"));
            tvNote.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
            tvNote.setLineSpacing(dp(ctx, 2), 1f);
            tvNote.setPadding(0, dp(ctx, 6), 0, 0);
            col.addView(tvNote);

            RecyclerView.LayoutParams rlp = new RecyclerView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            );
            rlp.setMargins(0, dp(ctx, 10), 0, 0);
            card.setLayoutParams(rlp);

            return new VH(card, tvDate, tvNote);
        }

        @Override public void onBindViewHolder(@NonNull VH h, int position) {
            NsvEntry e = items.get(position);
            h.tvDate.setText(df.format(new Date(e.dateMs)));
            h.tvNote.setText(e.note);

            h.itemView.setOnLongClickListener(v -> {
                v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                if (listener != null) listener.onLongPress(e);
                return true;
            });
        }

        @Override public int getItemCount() { return items == null ? 0 : items.size(); }

        static class VH extends RecyclerView.ViewHolder {
            TextView tvDate, tvNote;
            VH(@NonNull View itemView, TextView d, TextView n) {
                super(itemView);
                tvDate = d; tvNote = n;
            }
        }

        private static int dp(Context ctx, int v) {
            return (int) TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP, v, ctx.getResources().getDisplayMetrics()
            );
        }
    }

    private static class TrendMetrics {
        final double avg7Kg;
        final double weekOverWeekChangeKg;
        final long anchorDayMs;

        TrendMetrics(double avg7Kg, double weekOverWeekChangeKg, long anchorDayMs) {
            this.avg7Kg = avg7Kg;
            this.weekOverWeekChangeKg = weekOverWeekChangeKg;
            this.anchorDayMs = anchorDayMs;
        }

        static TrendMetrics empty() { return new TrendMetrics(Double.NaN, Double.NaN, -1L); }
    }

  
    private static class TrendLineView extends View {
        private final Paint pGrid = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint pLine = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint pLine2 = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint pText = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint pDot = new Paint(Paint.ANTI_ALIAS_FLAG);

        private List<WeightLogEntry> logsDesc = new ArrayList<>();
        private double startKg = Double.NaN;
        private double targetKg = Double.NaN;

        TrendLineView(Context ctx) {
            super(ctx);
            pGrid.setStyle(Paint.Style.STROKE);
            pGrid.setStrokeWidth(dp(ctx, 1));
            pGrid.setColor(Color.parseColor("#224B77FF"));

            pLine.setStyle(Paint.Style.STROKE);
            pLine.setStrokeWidth(dp(ctx, 3));
            pLine.setColor(Color.parseColor("#2563EB"));
            pLine.setStrokeCap(Paint.Cap.ROUND);
            pLine.setStrokeJoin(Paint.Join.ROUND);

            pLine2.setStyle(Paint.Style.STROKE);
            pLine2.setStrokeWidth(dp(ctx, 2));
            pLine2.setColor(Color.parseColor("#604B77FF"));
            pLine2.setStrokeCap(Paint.Cap.ROUND);
            pLine2.setStrokeJoin(Paint.Join.ROUND);

            pText.setColor(Color.parseColor("#2B5AA6"));
            pText.setTextSize(sp(ctx, 11));
            pText.setTypeface(Typeface.DEFAULT_BOLD);

            pDot.setStyle(Paint.Style.FILL);
            pDot.setColor(Color.parseColor("#2563EB"));
        }

        void setData(List<WeightLogEntry> logsDesc, double startKg, double targetKg) {
            this.logsDesc = logsDesc == null ? new ArrayList<>() : logsDesc;
            this.startKg = startKg;
            this.targetKg = targetKg;
            invalidate();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);

            int w = getWidth();
            int h = getHeight();
            if (w <= 0 || h <= 0) return;

            RectF plot = new RectF(dp(getContext(), 10), dp(getContext(), 10),
                    w - dp(getContext(), 10), h - dp(getContext(), 22));
            float r = dp(getContext(), 14);

            Paint bg = new Paint(Paint.ANTI_ALIAS_FLAG);
            bg.setColor(Color.parseColor("#0F2563EB"));
            canvas.drawRoundRect(plot, r, r, bg);

            if (logsDesc == null || logsDesc.size() < 2) {
                canvas.drawText("Trend: add more weigh-ins", plot.left + dp(getContext(), 10), plot.centerY(), pText);
                return;
            }

            List<WeightLogEntry> asc = new ArrayList<>(logsDesc);
            Collections.sort(asc, (a, b) -> Long.compare(a.dateMs, b.dateMs));

            int n = asc.size();
            List<Double> raw = new ArrayList<>(n);
            List<Double> smooth = new ArrayList<>(n);
            for (int i = 0; i < n; i++) raw.add(asc.get(i).weightKg);

            for (int i = 0; i < n; i++) {
                int from = Math.max(0, i - 6);
                double s = 0;
                int cnt = 0;
                for (int j = from; j <= i; j++) { s += raw.get(j); cnt++; }
                smooth.add(s / Math.max(1, cnt));
            }

            double min = Double.MAX_VALUE, max = -Double.MAX_VALUE;
            for (double v : raw) { min = Math.min(min, v); max = Math.max(max, v); }
            for (double v : smooth) { min = Math.min(min, v); max = Math.max(max, v); }

            if (!Double.isNaN(startKg)) { min = Math.min(min, startKg); max = Math.max(max, startKg); }
            if (!Double.isNaN(targetKg)) { min = Math.min(min, targetKg); max = Math.max(max, targetKg); }

            double pad = Math.max(0.5, (max - min) * 0.12);
            min -= pad;
            max += pad;

            canvas.drawRoundRect(plot, r, r, pGrid);
            for (int i = 1; i <= 3; i++) {
                float y = plot.top + (plot.height() * i / 4f);
                canvas.drawLine(plot.left, y, plot.right, y, pGrid);
            }

            float dx = plot.width() / (float) (n - 1);

            Path pathSmooth = new Path();
            for (int i = 0; i < n; i++) {
                float x = plot.left + dx * i;
                float y = mapY(plot, smooth.get(i), min, max);
                if (i == 0) pathSmooth.moveTo(x, y);
                else pathSmooth.lineTo(x, y);
            }
            canvas.drawPath(pathSmooth, pLine);

            Path pathRaw = new Path();
            for (int i = 0; i < n; i++) {
                float x = plot.left + dx * i;
                float y = mapY(plot, raw.get(i), min, max);
                if (i == 0) pathRaw.moveTo(x, y);
                else pathRaw.lineTo(x, y);
            }
            canvas.drawPath(pathRaw, pLine2);

            float xLast = plot.left + dx * (n - 1);
            float yLast = mapY(plot, smooth.get(n - 1), min, max);
            canvas.drawCircle(xLast, yLast, dp(getContext(), 4), pDot);

            canvas.drawText("Smoothed trend", plot.left + dp(getContext(), 10), plot.bottom + dp(getContext(), 16), pText);
        }

        private float mapY(RectF plot, double v, double min, double max) {
            double t = (v - min) / (max - min);
            t = Math.max(0, Math.min(1, t));
            return (float) (plot.bottom - t * plot.height());
        }

        private static int dp(Context ctx, int v) {
            return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, ctx.getResources().getDisplayMetrics());
        }

        private static float sp(Context ctx, int v) {
            return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, v, ctx.getResources().getDisplayMetrics());
        }
    }
    
    private MaterialCardView card(int radiusDp, int elevDp) {
        MaterialCardView c = new MaterialCardView(this);
        c.setCardBackgroundColor(C_SURFACE);
        c.setRadius(dp(radiusDp));
        c.setCardElevation(dp(elevDp));
        c.setUseCompatPadding(true);
        c.setStrokeColor(C_CARD_STROKE);
        c.setStrokeWidth(dp(1));
        return c;
    }

    private LinearLayout innerColumn() {
        LinearLayout ll = new LinearLayout(this);
        ll.setOrientation(LinearLayout.VERTICAL);
        ll.setPadding(dp(16), dp(16), dp(16), dp(16));
        return ll;
    }

    private TextView sectionTitle(String s) {
        TextView tv = new TextView(this);
        tv.setText(s);
        tv.setTextColor(C_TEXT);
        tv.setTypeface(Typeface.DEFAULT_BOLD);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        return tv;
    }

    private TextInputEditText editNumberField(LinearLayout parent, String hint, @Nullable String helper) {
        TextInputLayout til = new TextInputLayout(this);
        til.setHint(hint);
        til.setBoxBackgroundMode(TextInputLayout.BOX_BACKGROUND_OUTLINE);
        til.setBoxBackgroundColor(C_FIELD_BG);
        til.setBoxCornerRadii(dp(18), dp(18), dp(18), dp(18));
        til.setBoxStrokeColor(C_PRIMARY_2);
        til.setBoxStrokeWidth(dp(1));
        til.setBoxStrokeWidthFocused(dp(2));
        if (!TextUtils.isEmpty(helper)) {
            til.setHelperText(helper);
            til.setHelperTextColor(android.content.res.ColorStateList.valueOf(C_MUTED));
        }

        TextInputEditText et = new TextInputEditText(this);
        et.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        et.setTextColor(C_TEXT);
        et.setHintTextColor(C_HINT);
        et.setPadding(dp(14), dp(12), dp(14), dp(12));
        til.addView(et);

        parent.addView(til, lpMatchWrap(0, dp(10), 0, 0));
        return et;
    }

    private TextInputEditText editNoteField(LinearLayout parent, String hint, @Nullable String helper) {
        TextInputLayout til = new TextInputLayout(this);
        til.setHint(hint);
        til.setBoxBackgroundMode(TextInputLayout.BOX_BACKGROUND_OUTLINE);
        til.setBoxBackgroundColor(C_FIELD_BG);
        til.setBoxCornerRadii(dp(18), dp(18), dp(18), dp(18));
        til.setBoxStrokeColor(C_PRIMARY_2);
        til.setBoxStrokeWidth(dp(1));
        til.setBoxStrokeWidthFocused(dp(2));
        if (!TextUtils.isEmpty(helper)) {
            til.setHelperText(helper);
            til.setHelperTextColor(android.content.res.ColorStateList.valueOf(C_MUTED));
        }

        TextInputEditText et = new TextInputEditText(this);
        et.setMinLines(2);
        et.setMaxLines(5);
        et.setGravity(Gravity.TOP);
        et.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        et.setTextColor(C_TEXT);
        et.setHintTextColor(C_HINT);
        et.setPadding(dp(14), dp(12), dp(14), dp(12));
        til.addView(et);

        parent.addView(til, lpMatchWrap(0, dp(10), 0, 0));
        return et;
    }

    // Reduced-height pills: OUTLINED
    private MaterialButton outlinedPill(String text) {
        MaterialButton b = new MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
        b.setText(text);
        b.setAllCaps(false);
        b.setTextColor(C_PRIMARY);
        b.setTypeface(Typeface.DEFAULT_BOLD);

        b.setCornerRadius(dp(18));
        b.setStrokeColor(android.content.res.ColorStateList.valueOf(C_PRIMARY_2));
        b.setStrokeWidth(dp(1));
        b.setRippleColor(android.content.res.ColorStateList.valueOf(adjustAlpha(C_PRIMARY, 0.13f)));

        b.setInsetTop(0);
        b.setInsetBottom(0);
        b.setMinHeight(dp(H_PILL_OUTLINED_DP));
        b.setMinimumHeight(dp(H_PILL_OUTLINED_DP));

        b.setPadding(dp(14), dp(8), dp(14), dp(8));
        return b;
    }


    private MaterialButton filledPill(String text) {
        MaterialButton b = new MaterialButton(this);
        b.setText(text);
        b.setAllCaps(false);
        b.setTextColor(Color.WHITE);
        b.setTypeface(Typeface.DEFAULT_BOLD);

        b.setCornerRadius(dp(18));
        b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(C_PRIMARY));
        b.setRippleColor(android.content.res.ColorStateList.valueOf(adjustAlpha(Color.WHITE, 0.14f)));

        b.setStrokeWidth(0);
        b.setInsetTop(0);
        b.setInsetBottom(0);

        b.setMinHeight(dp(H_PILL_FILLED_DP));
        b.setMinimumHeight(dp(H_PILL_FILLED_DP));

        b.setPadding(dp(14), dp(10), dp(14), dp(10));
        return b;
    }


    private MaterialButton smallPill(String text) {
        MaterialButton b = new MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
        b.setText(text);
        b.setAllCaps(false);
        b.setTextSize(TypedValue.COMPLEX_UNIT_SP, 9f);
        b.setTypeface(Typeface.DEFAULT_BOLD);

        b.setCornerRadius(dp(16));
        b.setStrokeWidth(dp(1));
        b.setInsetTop(0);
        b.setInsetBottom(0);
        b.setMinHeight(dp(H_SMALL_PILL_DP));
        b.setMinimumHeight(dp(H_SMALL_PILL_DP));
        b.setPadding(dp(10), dp(6), dp(10), dp(6));

        b.setStrokeColor(android.content.res.ColorStateList.valueOf(adjustAlpha(C_PRIMARY, 0.18f)));
        b.setTextColor(C_PRIMARY);
        b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.TRANSPARENT));
        b.setRippleColor(android.content.res.ColorStateList.valueOf(adjustAlpha(C_PRIMARY, 0.12f)));

        return b;
    }

    private void setSmallPillSelected(MaterialButton b, boolean selected) {
        if (b == null) return;
        if (selected) {
            b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(adjustAlpha(C_PRIMARY, 0.14f)));
            b.setStrokeColor(android.content.res.ColorStateList.valueOf(adjustAlpha(C_PRIMARY, 0.30f)));
            b.setTextColor(C_PRIMARY);
        } else {
            b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.TRANSPARENT));
            b.setStrokeColor(android.content.res.ColorStateList.valueOf(adjustAlpha(C_PRIMARY, 0.18f)));
            b.setTextColor(C_PRIMARY);
        }
    }

    private LinearLayout.LayoutParams lpMatchWrap(int l, int t, int r, int b) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        lp.setMargins(dp(l), dp(t), dp(r), dp(b));
        return lp;
    }

    private LinearLayout.LayoutParams lpMatch(int heightPx, int l, int t, int r, int b) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                heightPx
        );
        lp.setMargins(dp(l), dp(t), dp(r), dp(b));
        return lp;
    }

    private int dp(int v) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, v, getResources().getDisplayMetrics()
        );
    }

    private int adjustAlpha(int color, float factor) {
        int alpha = Math.round(Color.alpha(color) * factor);
        int red = Color.red(color);
        int green = Color.green(color);
        int blue = Color.blue(color);
        return Color.argb(alpha, red, green, blue);
    }


    private String textOf(TextInputEditText et) {
        return et == null || et.getText() == null ? "" : et.getText().toString().trim();
    }

    private Double parseDouble(TextInputEditText et) {
        if (et == null || et.getText() == null) return null;
        String s = et.getText().toString().trim();
        if (s.isEmpty()) return null;
        s = s.replace(",", "."); // allow EU decimal
        try { return Double.parseDouble(s); }
        catch (Exception e) { return null; }
    }

    private Double parseDouble(TextView tv) {
        if (tv == null) return null;
        String s = tv.getText() == null ? "" : tv.getText().toString().trim();
        if (s.isEmpty()) return null;
        s = s.replace(",", ".");
        try { return Double.parseDouble(s); }
        catch (Exception e) { return null; }
    }

    private Integer parseInt(TextInputEditText et) {
        if (et == null || et.getText() == null) return null;
        String s = et.getText().toString().trim();
        if (s.isEmpty()) return null;
        try { return Integer.parseInt(s); }
        catch (Exception e) { return null; }
    }

    private String format1(double v) {
        return String.format(Locale.US, "%.1f", v);
    }

    private void hideKeyboardAndClearFocus() {
        View v = getCurrentFocus();
        if (v != null) {
            try {
                InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                if (imm != null) imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
            } catch (Exception ignored) {}
            v.clearFocus();
        }
    }

    private void toast(String s) {
        Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
    }

    
    private static long startOfDay(long ms) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(ms);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }

    private static long daysBetween(long startDayMs, long endDayMs) {
        long diff = endDayMs - startDayMs;
        return TimeUnit.MILLISECONDS.toDays(diff);
    }

  
}
