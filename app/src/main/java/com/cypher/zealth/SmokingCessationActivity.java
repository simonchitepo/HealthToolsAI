package com.cypher.zealth;

import android.app.AlarmManager;
import android.app.DatePickerDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.text.InputFilter;
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
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Space;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;


public class SmokingCessationActivity extends AppCompatActivity {


    private static final int C_BG = Color.parseColor("#ECFDF5");    
    private static final int C_CARD = Color.parseColor("#FDFDFF");
    private static final int C_STROKE = Color.parseColor("#E5E9F5");
    private static final int C_TEXT = Color.parseColor("#111827");
    private static final int C_SUB = Color.parseColor("#6B7280");

    private static final int C_CHARCOAL = Color.parseColor("#111827"); 
    private static final int C_ASH = Color.parseColor("#EEF2F7");     
    private static final int C_MINT = Color.parseColor("#10B981");    
    private static final int C_EMBER = Color.parseColor("#F97316");    
    private static final int C_SKY = Color.parseColor("#2563EB");    

    private SmokingStore store;


    private final SimpleDateFormat dfDate = new SimpleDateFormat("yyyy-MM-dd", Locale.US);


    private MaterialToolbar toolbar;

    private MaterialButton btnPickQuitDate;
    private MaterialButton btnSavePlan;
    private MaterialButton btnSetReminder;
    private MaterialButton btnWhy;
    private MaterialButton btnNrt;
    private MaterialButton btnCommunity;

    private ChipGroup chipPlanType;
    private Chip chipColdTurkey, chipGradual;
    private TextInputLayout tilCigsPerDay, tilCostPerPack, tilCigsPerPack, tilReductionTarget;
    private TextInputEditText etCigsPerDay, etCostPerPack, etCigsPerPack, etReductionTarget;

    private TextView tvStreak, tvSmokeFreeTime, tvSavings, tvBadges;

    private TextView tvDailySkillTitle, tvDailySkillBody;
    private MaterialButton btnMarkSkillDone;

    private TextView tvCravingIntensity;
    private SeekBar seekCraving;
    private ChipGroup chipMood, chipTriggers;
    private TextInputLayout tilCravingNote;
    private TextInputEditText etCravingNote;
    private MaterialButton btnSaveCraving, btnCravingSOS;

    private RecyclerView rvHistory;
    private HistoryAdapter historyAdapter;

    private MaterialButton btnResetAll;


    private Uri whyPhotoUri = null;

    private ActivityResultLauncher<String> pickImageLauncher;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        store = new SmokingStore(this);
        ensureNotificationChannel();

        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        whyPhotoUri = uri;
                        store.setWhyPhotoUri(uri.toString());
                        Toast.makeText(this, "Saved your photo for motivation.", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        setContentView(buildRoot());

        bindInitialState();
        wireActions();
        renderAll();
    }

    private View buildRoot() {
        FrameLayout root = new FrameLayout(this);
        root.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));
        root.setBackgroundColor(C_BG);

        View veil = new View(this);
        veil.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));
        veil.setBackgroundColor(Color.WHITE);
        veil.setAlpha(0.88f);

        LinearLayout shell = new LinearLayout(this);
        shell.setOrientation(LinearLayout.VERTICAL);
        shell.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));

        MaterialCardView tbCard = card(16, 12, 16, 8);
        tbCard.setRadius(dp(26));
        tbCard.setCardBackgroundColor(C_CARD);
        tbCard.setStrokeWidth(dp(1));
        tbCard.setStrokeColor(C_STROKE);

        toolbar = new MaterialToolbar(this);
        toolbar.setTitle("Smoking Cessation");
        toolbar.setSubtitle("Quit plan, streak, savings, skills");
        toolbar.setTitleTextColor(C_TEXT);
        toolbar.setSubtitleTextColor(C_SUB);
        toolbar.setNavigationIcon(androidx.appcompat.R.drawable.abc_ic_ab_back_material);
        toolbar.setNavigationIconTint(C_TEXT);
        toolbar.setBackgroundColor(Color.TRANSPARENT);
        toolbar.setPadding(dp(8), dp(6), dp(8), dp(6));
        tbCard.addView(toolbar);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        LinearLayout.LayoutParams scrollLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0
        );
        scrollLp.weight = 1f;
        scroll.setLayoutParams(scrollLp);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(16), dp(12), dp(16), dp(18));
        scroll.addView(content, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        MaterialCardView planCard = card(0, 0, 0, 12);
        content.addView(planCard);

        LinearLayout plan = vStack(dp(16));
        planCard.addView(plan);

        plan.addView(sectionTitle("Quit plan"));

        btnPickQuitDate = pillButton("Quit date: Not set", C_SKY, Color.WHITE);
        plan.addView(btnPickQuitDate);

        chipPlanType = new ChipGroup(this);
        chipPlanType.setSingleSelection(true);
        chipPlanType.setSelectionRequired(true);
        chipPlanType.setChipSpacingHorizontal(dp(8));
        chipPlanType.setChipSpacingVertical(dp(8));
        LinearLayout.LayoutParams cgLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        );
        cgLp.topMargin = dp(10);
        chipPlanType.setLayoutParams(cgLp);

        chipColdTurkey = chip("Cold turkey", true);
        chipGradual = chip("Gradual reduction", false);
        chipPlanType.addView(chipColdTurkey);
        chipPlanType.addView(chipGradual);
        plan.addView(chipPlanType);

        tilCigsPerDay = inputLayout("Cigarettes per day");
        etCigsPerDay = inputEditNumber();
        tilCigsPerDay.addView(etCigsPerDay);
        plan.addView(tilCigsPerDay);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        );
        rowLp.topMargin = dp(12);
        row.setLayoutParams(rowLp);

        tilCostPerPack = inputLayout("Cost per pack");
        etCostPerPack = inputEditDecimal();
        tilCostPerPack.addView(etCostPerPack);

        tilCigsPerPack = inputLayout("Cigs per pack");
        etCigsPerPack = inputEditNumber();
        tilCigsPerPack.addView(etCigsPerPack);

        LinearLayout.LayoutParams left = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        LinearLayout.LayoutParams right = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        right.leftMargin = dp(10);

        row.addView(tilCostPerPack, left);
        row.addView(tilCigsPerPack, right);
        plan.addView(row);


        tilReductionTarget = inputLayout("Reduction target (cigs/day by 2 weeks)");
        etReductionTarget = inputEditNumber();
        tilReductionTarget.addView(etReductionTarget);
        LinearLayout.LayoutParams rtLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        );
        rtLp.topMargin = dp(12);
        tilReductionTarget.setLayoutParams(rtLp);
        plan.addView(tilReductionTarget);

        btnSavePlan = pillButton("Save plan", C_SKY, Color.WHITE);
        LinearLayout.LayoutParams spLp = (LinearLayout.LayoutParams) btnSavePlan.getLayoutParams();
        spLp.topMargin = dp(12);
        plan.addView(btnSavePlan);

        LinearLayout planActions = new LinearLayout(this);
        planActions.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams paLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        );
        paLp.topMargin = dp(10);
        planActions.setLayoutParams(paLp);

        btnSetReminder = pillButtonSmall("Reminders", C_ASH, C_CHARCOAL);
        btnWhy = pillButtonSmall("My “Why”", C_ASH, C_CHARCOAL);
        btnNrt = pillButtonSmall("NRT guidance", C_ASH, C_CHARCOAL);
        btnCommunity = pillButtonSmall("Community", C_ASH, C_CHARCOAL);

        LinearLayout.LayoutParams a1 = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        LinearLayout.LayoutParams a2 = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        LinearLayout.LayoutParams a3 = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        LinearLayout.LayoutParams a4 = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        a2.leftMargin = dp(10);
        a3.leftMargin = dp(10);
        a4.leftMargin = dp(10);

        planActions.addView(btnSetReminder, a1);
        planActions.addView(btnWhy, a2);
        planActions.addView(btnNrt, a3);
        planActions.addView(btnCommunity, a4);
        plan.addView(planActions);

        MaterialCardView progressCard = card(0, 0, 0, 12);
        content.addView(progressCard);

        LinearLayout prog = vStack(dp(16));
        progressCard.addView(prog);

        prog.addView(sectionTitle("Progress"));
        tvStreak = bodyText("Streak: —");
        tvSmokeFreeTime = bodyText("Smoke-free time: —");
        tvSavings = bodyText("Savings: —");
        tvBadges = bodyText("Badges: —");
        tvSavings.setLineSpacing(dp(2), 1f);

        prog.addView(space(dp(10)));
        prog.addView(tvStreak);
        prog.addView(space(dp(6)));
        prog.addView(tvSmokeFreeTime);
        prog.addView(space(dp(6)));
        prog.addView(tvSavings);
        prog.addView(space(dp(6)));
        prog.addView(tvBadges);

        MaterialCardView skillCard = card(0, 0, 0, 12);
        content.addView(skillCard);

        LinearLayout skill = vStack(dp(16));
        skillCard.addView(skill);

        skill.addView(sectionTitle("Today’s skill (CBT/ACT)"));

        tvDailySkillTitle = titleText("—");
        tvDailySkillBody = bodyText("—");
        tvDailySkillBody.setLineSpacing(dp(3), 1f);

        btnMarkSkillDone = pillButton("Mark as done", C_MINT, Color.WHITE);
        LinearLayout.LayoutParams msLp = (LinearLayout.LayoutParams) btnMarkSkillDone.getLayoutParams();
        msLp.topMargin = dp(12);

        skill.addView(tvDailySkillTitle);
        skill.addView(space(dp(6)));
        skill.addView(tvDailySkillBody);
        skill.addView(btnMarkSkillDone);

        MaterialCardView cravingCard = card(0, 0, 0, 12);
        content.addView(cravingCard);

        LinearLayout craving = vStack(dp(16));
        cravingCard.addView(craving);

        craving.addView(sectionTitle("Craving check-in (JIT support)"));

        tvCravingIntensity = subText("Intensity: 3 / 5");
        craving.addView(space(dp(10)));
        craving.addView(tvCravingIntensity);

        seekCraving = new SeekBar(this);
        seekCraving.setMax(4);
        seekCraving.setProgress(2);
        LinearLayout.LayoutParams sbLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        );
        sbLp.topMargin = dp(6);
        seekCraving.setLayoutParams(sbLp);
        craving.addView(seekCraving);


        craving.addView(space(dp(10)));
        craving.addView(subtleLabel("Mood"));
        chipMood = new ChipGroup(this);
        chipMood.setSingleSelection(true);
        chipMood.setSelectionRequired(true);
        chipMood.setChipSpacingHorizontal(dp(8));
        chipMood.setChipSpacingVertical(dp(8));
        chipMood.addView(chip("Calm", false));
        chipMood.addView(chip("Stressed", false));
        chipMood.addView(chip("Bored", false));
        chipMood.addView(chip("Anxious", false));
        chipMood.addView(chip("Angry", false));
        chipMood.addView(chip("Social", false));
        craving.addView(chipMood);

        craving.addView(space(dp(10)));
        craving.addView(subtleLabel("Triggers"));
        chipTriggers = new ChipGroup(this);
        chipTriggers.setSingleSelection(false);
        chipTriggers.setChipSpacingHorizontal(dp(8));
        chipTriggers.setChipSpacingVertical(dp(8));
        chipTriggers.addView(chip("Coffee", false));
        chipTriggers.addView(chip("After meal", false));
        chipTriggers.addView(chip("Commute", false));
        chipTriggers.addView(chip("Work break", false));
        chipTriggers.addView(chip("Alcohol", false));
        chipTriggers.addView(chip("Friends", false));
        chipTriggers.addView(chip("Stress", false));
        craving.addView(chipTriggers);

        tilCravingNote = inputLayout("Optional note (what happened?)");
        etCravingNote = inputEditMultiline(2, 400);
        tilCravingNote.addView(etCravingNote);
        LinearLayout.LayoutParams cnLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        );
        cnLp.topMargin = dp(12);
        tilCravingNote.setLayoutParams(cnLp);
        craving.addView(tilCravingNote);

        LinearLayout ctaRow = new LinearLayout(this);
        ctaRow.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams ctaLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        );
        ctaLp.topMargin = dp(12);
        ctaRow.setLayoutParams(ctaLp);

        btnSaveCraving = pillButton("Save check-in", C_SKY, Color.WHITE);
        btnCravingSOS = pillButton("Craving SOS", C_EMBER, Color.WHITE);

        LinearLayout.LayoutParams c1 = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        LinearLayout.LayoutParams c2 = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        c2.leftMargin = dp(10);

        ctaRow.addView(btnSaveCraving, c1);
        ctaRow.addView(btnCravingSOS, c2);
        craving.addView(ctaRow);

        TextView historyLabel = subtleLabel("Check-in history");
        LinearLayout.LayoutParams hlp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        );
        hlp.topMargin = dp(8);
        historyLabel.setLayoutParams(hlp);
        content.addView(historyLabel);

        MaterialCardView historyCard = card(0, 0, 0, 12);
        content.addView(historyCard);

        rvHistory = new RecyclerView(this);
        rvHistory.setLayoutManager(new LinearLayoutManager(this));
        rvHistory.setNestedScrollingEnabled(false);
        rvHistory.setPadding(dp(12), dp(10), dp(12), dp(10));
        rvHistory.setClipToPadding(false);
        historyCard.addView(rvHistory);

        btnResetAll = pillButton("Reset quit plan and logs", C_ASH, C_CHARCOAL);
        content.addView(btnResetAll);

        content.addView(space(dp(18)));

        shell.addView(tbCard);
        shell.addView(scroll);

        root.addView(veil);
        root.addView(shell);
        return root;
    }

    private void bindInitialState() {
        toolbar.setNavigationOnClickListener(v -> finish());

        if (chipMood.getChildCount() > 0) {
            Chip first = (Chip) chipMood.getChildAt(0);
            first.setChecked(true);
        }

        Plan p = store.getPlan();
        if (p.quitDateMs > 0) {
            btnPickQuitDate.setText("Quit date: " + dfDate.format(p.quitDateMs));
        }

        if (p.cigsPerDay > 0) etCigsPerDay.setText(String.valueOf(p.cigsPerDay));
        if (p.costPerPack > 0) etCostPerPack.setText(String.format(Locale.US, "%.2f", p.costPerPack));
        if (p.cigsPerPack > 0) etCigsPerPack.setText(String.valueOf(p.cigsPerPack));
        if (p.reductionTarget >= 0) etReductionTarget.setText(String.valueOf(p.reductionTarget));

        if (p.planType == PlanType.GRADUAL) chipGradual.setChecked(true);
        else chipColdTurkey.setChecked(true);

        updateReductionVisibility();

        whyPhotoUri = safeParseUri(store.getWhyPhotoUri());

        historyAdapter = new HistoryAdapter(new ArrayList<>());
        rvHistory.setAdapter(historyAdapter);

        updateCravingLabel(seekCraving.getProgress() + 1);
    }

    private void wireActions() {
        chipPlanType.setOnCheckedStateChangeListener((group, checkedIds) -> updateReductionVisibility());

        btnPickQuitDate.setOnClickListener(v -> pickQuitDate());
        btnSavePlan.setOnClickListener(v -> savePlan());

        seekCraving.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                updateCravingLabel(progress + 1);
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        btnSaveCraving.setOnClickListener(v -> saveCheckIn());
        btnCravingSOS.setOnClickListener(v -> showCravingSOS());

        btnMarkSkillDone.setOnClickListener(v -> markSkillDone());

        btnSetReminder.setOnClickListener(v -> configureReminder());
        btnWhy.setOnClickListener(v -> editWhy());
        btnNrt.setOnClickListener(v -> showNrtGuidance());
        btnCommunity.setOnClickListener(v -> openCommunity());

        btnResetAll.setOnClickListener(v -> resetAll());
    }

    private void renderAll() {
        renderProgress();
        renderDailySkill();
        renderHistory();
    }

    private void updateReductionVisibility() {
        boolean gradual = isGradualSelected();
        tilReductionTarget.setVisibility(gradual ? View.VISIBLE : View.GONE);
    }

    private boolean isGradualSelected() {
        return chipGradual != null && chipGradual.isChecked();
    }

    private void updateCravingLabel(int intensity1to5) {
        tvCravingIntensity.setText("Intensity: " + intensity1to5 + " / 5");
    }

    private void pickQuitDate() {
        Plan p = store.getPlan();
        Calendar cal = Calendar.getInstance();
        if (p.quitDateMs > 0) cal.setTimeInMillis(p.quitDateMs);

        DatePickerDialog dp = new DatePickerDialog(
                this,
                (view, year, month, day) -> {
                    Calendar chosen = Calendar.getInstance();
                    chosen.set(year, month, day);
                    long ms = startOfDay(chosen.getTimeInMillis());
                    Plan updated = store.getPlan();
                    updated.quitDateMs = ms;
                    store.setPlan(updated);

                    btnPickQuitDate.setText("Quit date: " + dfDate.format(ms));
                    renderProgress();
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
        );
        dp.show();
    }

    private void clearFieldErrors() {
        tilCigsPerDay.setError(null);
        tilCostPerPack.setError(null);
        tilCigsPerPack.setError(null);
        tilReductionTarget.setError(null);
        tilCravingNote.setError(null);
    }

    private void savePlan() {
        clearFieldErrors();
        hideKeyboard();

        Plan p = store.getPlan();

        Integer cpd = parseInt(etCigsPerDay);
        Double cpp = parseDouble(etCostPerPack);
        Integer pack = parseInt(etCigsPerPack);

        boolean gradual = isGradualSelected();
        Integer target = gradual ? parseInt(etReductionTarget) : 0;

        boolean ok = true;

        if (p.quitDateMs <= 0) {
            Toast.makeText(this, "Please set a quit date.", Toast.LENGTH_SHORT).show();
            ok = false;
        }

        if (cpd == null || cpd < 0 || cpd > 200) {
            tilCigsPerDay.setError("Enter 0–200.");
            ok = false;
        }
        if (cpp == null || cpp < 0 || cpp > 500) {
            tilCostPerPack.setError("Enter a realistic cost (0–500).");
            ok = false;
        }
        if (pack == null || pack < 5 || pack > 50) {
            tilCigsPerPack.setError("Enter 5–50.");
            ok = false;
        }
        if (gradual) {
            if (target == null || target < 0 || target > 200) {
                tilReductionTarget.setError("Enter 0–200.");
                ok = false;
            }
            // Mild sanity: reduction target should not exceed current cigs/day
            if (cpd != null && target != null && target > cpd) {
                tilReductionTarget.setError("Target should be ≤ current cigarettes/day.");
                ok = false;
            }
        }

        if (!ok) return;

        p.planType = gradual ? PlanType.GRADUAL : PlanType.COLD_TURKEY;
        p.cigsPerDay = cpd;
        p.costPerPack = cpp;
        p.cigsPerPack = pack;
        p.reductionTarget = gradual ? target : 0;

        store.setPlan(p);

        etCostPerPack.setText(String.format(Locale.US, "%.2f", cpp));

        Toast.makeText(this, "Plan saved.", Toast.LENGTH_SHORT).show();
        renderProgress();
    }

    private void renderProgress() {
        Plan p = store.getPlan();

        if (p.quitDateMs <= 0) {
            tvStreak.setText("Streak: —");
            tvSmokeFreeTime.setText("Smoke-free time: —");
            tvSavings.setText("Savings: —");
            tvBadges.setText("Badges: —");
            return;
        }

        long now = System.currentTimeMillis();

        if (now < p.quitDateMs) {
            long msUntil = p.quitDateMs - now;
            long daysUntil = TimeUnit.MILLISECONDS.toDays(msUntil);
            long hoursUntil = TimeUnit.MILLISECONDS.toHours(msUntil) % 24;

            tvStreak.setText("Streak: Not started");
            tvSmokeFreeTime.setText("Quit date in " + daysUntil + " days, " + hoursUntil + " hours");
            tvSavings.setText("Savings: Starts after your quit date.");
            tvBadges.setText("Badges: Set when you begin.");
            return;
        }

        long today = startOfDay(now);
        long daysSmokeFree = Math.max(0, daysBetween(p.quitDateMs, today));
        long ms = Math.max(0, now - p.quitDateMs);

        long hoursTotal = TimeUnit.MILLISECONDS.toHours(ms);
        long daysTotal = TimeUnit.MILLISECONDS.toDays(ms);
        long remHours = hoursTotal % 24;

        tvStreak.setText("Streak: " + daysSmokeFree + " days smoke-free");
        tvSmokeFreeTime.setText("Smoke-free time: " + daysTotal + " days, " + remHours + " hours");


        if (p.cigsPerDay > 0 && p.costPerPack > 0 && p.cigsPerPack > 0) {
            double costPerCig = p.costPerPack / (double) p.cigsPerPack;

            double avoidedCigs;
            if (p.planType == PlanType.GRADUAL) {
                avoidedCigs = estimateAvoidedCigsGradual(p.cigsPerDay, p.reductionTarget, daysSmokeFree);
            } else {
                avoidedCigs = (double) p.cigsPerDay * (double) daysSmokeFree;
            }

            double saved = avoidedCigs * costPerCig;

            String healthLine = buildHealthLine(daysSmokeFree);

            tvSavings.setText(String.format(
                    Locale.US,
                    "Savings: %.2f\nAvoided cigarettes: %.0f\n%s",
                    saved,
                    avoidedCigs,
                    healthLine
            ));
        } else {
            tvSavings.setText("Savings: Enter cigarettes/day + pack cost to calculate.");
        }


        EnumSet<Badge> earned = computeBadges(daysSmokeFree);
        store.setEarnedBadges(earned);
        tvBadges.setText("Badges: " + (earned.isEmpty() ? "—" : badgesToString(earned)));
    }

    private double estimateAvoidedCigsGradual(int baselineCpd, int targetCpd, long daysSmokeFree) {
        baselineCpd = Math.max(0, baselineCpd);
        targetCpd = Math.max(0, targetCpd);

        final int rampDays = 14;
        double avoided = 0;

        for (int d = 1; d <= daysSmokeFree; d++) {
            double plannedCpd;
            if (d <= rampDays) {
                double t = d / (double) rampDays;
                plannedCpd = baselineCpd - (baselineCpd - targetCpd) * t;
            } else {
                plannedCpd = targetCpd;
            }
       
            double dayAvoided = Math.max(0, baselineCpd - plannedCpd);
            avoided += dayAvoided;
        }

        return avoided;
    }

    private String buildHealthLine(long daysSmokeFree) {

        if (daysSmokeFree < 1) return "Health: You’ve started the shift.";
        if (daysSmokeFree < 2) return "Health: Carbon monoxide levels begin to normalize.";
        if (daysSmokeFree < 3) return "Health: Taste and smell often improve.";
        if (daysSmokeFree < 7) return "Health: Breathing may feel easier this week.";
        if (daysSmokeFree < 14) return "Health: Circulation and lung function can improve.";
        if (daysSmokeFree < 30) return "Health: Coughing/wheezing may decrease.";
        if (daysSmokeFree < 90) return "Health: Cravings often reduce in frequency/intensity.";
        return "Health: Long-term risk continues to drop with each smoke-free month.";
    }

    private EnumSet<Badge> computeBadges(long daysSmokeFree) {
        EnumSet<Badge> earned = EnumSet.noneOf(Badge.class);

        if (daysSmokeFree >= 1) earned.add(Badge.FIRST_24H);
        if (daysSmokeFree >= 3) earned.add(Badge.THREE_DAYS);
        if (daysSmokeFree >= 7) earned.add(Badge.ONE_WEEK);
        if (daysSmokeFree >= 14) earned.add(Badge.TWO_WEEKS);
        if (daysSmokeFree >= 30) earned.add(Badge.ONE_MONTH);
        if (daysSmokeFree >= 90) earned.add(Badge.THREE_MONTHS);

        
        if (store.hasCheckInStreak(7)) earned.add(Badge.SEVEN_DAY_LOG_STREAK);

        return earned;
    }

    private String badgesToString(EnumSet<Badge> badges) {
        List<String> out = new ArrayList<>();
        for (Badge b : badges) out.add(b.label);
        return TextUtils.join(" • ", out);
    }


    private void renderDailySkill() {
        Skill s = store.getTodaySkill();
        tvDailySkillTitle.setText(s.title);
        tvDailySkillBody.setText(s.body);

        boolean done = store.isSkillDoneToday();
        btnMarkSkillDone.setEnabled(!done);
        btnMarkSkillDone.setText(done ? "Done for today" : "Mark as done");
    }

    private void markSkillDone() {
        store.setSkillDoneToday(true);
        Toast.makeText(this, "Nice work—skills compound over time.", Toast.LENGTH_SHORT).show();
        renderDailySkill();
        renderProgress(); 
    }


    private void saveCheckIn() {
        clearFieldErrors();
        hideKeyboard();

        int intensity = seekCraving.getProgress() + 1;

        String mood = getSelectedChipText(chipMood);
        List<String> triggers = getSelectedChipTexts(chipTriggers);

        String note = textOf(etCravingNote);
        if (note.length() > 400) {
            tilCravingNote.setError("Keep notes under 400 characters.");
            return;
        }


        if (intensity >= 4) {
            Toast.makeText(this, "High craving detected — consider using Craving SOS now.", Toast.LENGTH_SHORT).show();
        }

        CheckIn ci = new CheckIn();
        ci.timestampMs = System.currentTimeMillis();
        ci.intensity1to5 = intensity;
        ci.mood = mood;
        ci.triggers = triggers;
        ci.note = note;

        store.addCheckIn(ci);

        etCravingNote.setText("");

        clearChipSelections(chipTriggers);

        renderHistory();
        renderProgress();
        Toast.makeText(this, "Check-in saved.", Toast.LENGTH_SHORT).show();
    }

    private void renderHistory() {
        List<CheckIn> items = store.getCheckIns();
        historyAdapter.setItems(items);
    }

  
    private void showCravingSOS() {
        View v = buildCravingSosView();
        new MaterialAlertDialogBuilder(this)
                .setTitle("Craving SOS")
                .setView(v)
                .setPositiveButton("Close", (d, w) -> d.dismiss())
                .show();
    }

    private View buildCravingSosView() {
        LinearLayout box = vStack(dp(14));
        box.setPadding(dp(18), dp(14), dp(18), dp(4));

        TextView t1 = titleText("1) Breathe (60 seconds)");
        TextView b1 = bodyText("Inhale 4s • Hold 2s • Exhale 6s. Repeat. Cravings peak and fade.");
        b1.setLineSpacing(dp(2), 1f);

        MaterialButton startBreath = pillButtonSmall("Start guided breathing", C_EMBER, Color.WHITE);

        TextView t2 = titleText("2) Urge-surfing (ACT)");
        TextView b2 = bodyText("Name it: “I’m having the urge to smoke.” Rate it 1–5, watch it rise/fall like a wave.");
        b2.setLineSpacing(dp(2), 1f);

        TextView t3 = titleText("3) Quick distractions (2 minutes)");
        TextView b3 = bodyText("Pick one: drink water • 20 squats • text a friend • brush teeth • walk 3 minutes.");
        b3.setLineSpacing(dp(2), 1f);

        TextView t4 = titleText("Your ‘Why’");
        String whyNote = store.getWhyNote();
        TextView b4 = bodyText(TextUtils.isEmpty(whyNote)
                ? "Add a reason that matters to you (health, family, savings)."
                : "“" + whyNote + "”");
        b4.setLineSpacing(dp(2), 1f);

        startBreath.setOnClickListener(v -> runGuidedBreathing());

        box.addView(t1);
        box.addView(b1);
        box.addView(startBreath);
        box.addView(space(dp(10)));
        box.addView(t2);
        box.addView(b2);
        box.addView(space(dp(10)));
        box.addView(t3);
        box.addView(b3);
        box.addView(space(dp(10)));
        box.addView(t4);
        box.addView(b4);

        return box;
    }

    private void runGuidedBreathing() {
      
        final int totalSeconds = 60;
        final long start = SystemClock.elapsedRealtime();

        final AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle("Guided breathing")
                .setMessage("Get comfortable. Starting…")
                .setCancelable(false)
                .setNegativeButton("Stop", (d, w) -> d.dismiss())
                .show();

    
        final android.os.Handler h = new android.os.Handler(getMainLooper());
        h.post(new Runnable() {
            @Override public void run() {
                if (!dialog.isShowing()) return;

                long elapsed = (SystemClock.elapsedRealtime() - start) / 1000L;
                if (elapsed >= totalSeconds) {
                    dialog.setMessage("Done. Notice how the urge shifted. You did not obey it.");
                    return;
                }

                int cycle = (int) (elapsed % 12);
                String phase;
                if (cycle < 4) phase = "Inhale (4)";
                else if (cycle < 6) phase = "Hold (2)";
                else phase = "Exhale (6)";

                dialog.setMessage(phase + "\n" + (totalSeconds - elapsed) + "s remaining");
                h.postDelayed(this, 350);
            }
        });
    }


    private void configureReminder() {

        Calendar now = Calendar.getInstance();
        int h = now.get(Calendar.HOUR_OF_DAY);
        int m = now.get(Calendar.MINUTE);

        android.app.TimePickerDialog tp = new android.app.TimePickerDialog(
                this,
                (view, hourOfDay, minute) -> {
                    store.setReminderTime(hourOfDay, minute);
                    scheduleDailyReminder(hourOfDay, minute);
                    Toast.makeText(this, "Reminder set for " + String.format(Locale.US, "%02d:%02d", hourOfDay, minute), Toast.LENGTH_SHORT).show();
                },
                h, m, DateFormat.is24HourFormat(this)
        );
        tp.show();
    }

    private void scheduleDailyReminder(int hourOfDay, int minute) {
        AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
        if (am == null) return;

        Intent i = new Intent(this, ReminderReceiver.class);
        i.setAction(ReminderReceiver.ACTION_REMINDER);
        PendingIntent pi = PendingIntent.getBroadcast(
                this, 101, i,
                (Build.VERSION.SDK_INT >= 23)
                        ? PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                        : PendingIntent.FLAG_UPDATE_CURRENT
        );

        Calendar next = Calendar.getInstance();
        next.set(Calendar.HOUR_OF_DAY, hourOfDay);
        next.set(Calendar.MINUTE, minute);
        next.set(Calendar.SECOND, 0);
        next.set(Calendar.MILLISECOND, 0);
        if (next.getTimeInMillis() <= System.currentTimeMillis()) {
            next.add(Calendar.DAY_OF_YEAR, 1);
        }

        am.cancel(pi);

        long interval = AlarmManager.INTERVAL_DAY;
        am.setInexactRepeating(AlarmManager.RTC_WAKEUP, next.getTimeInMillis(), interval, pi);
    }


    private void editWhy() {
        LinearLayout box = vStack(dp(12));
        box.setPadding(dp(18), dp(12), dp(18), dp(2));

        TextInputLayout til = inputLayout("Your reason for quitting (shown during cravings)");
        TextInputEditText et = inputEditMultiline(3, 300);
        til.addView(et);

        String existing = store.getWhyNote();
        if (!TextUtils.isEmpty(existing)) et.setText(existing);

        MaterialButton pickPhoto = pillButtonSmall("Pick a photo", C_ASH, C_CHARCOAL);
        MaterialButton clearPhoto = pillButtonSmall("Clear photo", C_ASH, C_CHARCOAL);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        );
        rlp.topMargin = dp(6);
        row.setLayoutParams(rlp);

        LinearLayout.LayoutParams w1 = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        LinearLayout.LayoutParams w2 = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        w2.leftMargin = dp(10);

        row.addView(pickPhoto, w1);
        row.addView(clearPhoto, w2);

        TextView status = subText(photoStatusText());

        pickPhoto.setOnClickListener(v -> pickImageLauncher.launch("image/*"));
        clearPhoto.setOnClickListener(v -> {
            whyPhotoUri = null;
            store.setWhyPhotoUri("");
            status.setText(photoStatusText());
            Toast.makeText(this, "Photo cleared.", Toast.LENGTH_SHORT).show();
        });

        box.addView(til);
        box.addView(row);
        box.addView(space(dp(6)));
        box.addView(status);

        new MaterialAlertDialogBuilder(this)
                .setTitle("My “Why”")
                .setView(box)
                .setNegativeButton("Cancel", (d, w) -> d.dismiss())
                .setPositiveButton("Save", (d, w) -> {
                    store.setWhyNote(textOf(et).trim());
                    Toast.makeText(this, "Saved.", Toast.LENGTH_SHORT).show();
                })
                .show();
    }

    private String photoStatusText() {
        String s = store.getWhyPhotoUri();
        return TextUtils.isEmpty(s) ? "Photo: not set" : "Photo: set";
    }


    private void showNrtGuidance() {
        String msg =
                "Evidence-based options that can improve quit success:\n\n" +
                        "• Nicotine patch: steady baseline support\n" +
                        "• Gum/lozenge: for breakthrough cravings\n" +
                        "• Combination therapy (patch + gum/lozenge) is commonly used\n" +
                        "• Prescription options may be available in your region\n\n" +
                        "Safety notes:\n" +
                        "• Follow product labeling and clinician advice\n" +
                        "• If pregnant, have heart conditions, or take medications, consult a clinician\n\n" +
                        "This app does not provide medical diagnosis—use this as education and discuss with a professional if needed.";

        new MaterialAlertDialogBuilder(this)
                .setTitle("NRT guidance")
                .setMessage(msg)
                .setPositiveButton("OK", (d, w) -> d.dismiss())
                .show();
    }

    private void openCommunity() {

        String url = "https://www.reddit.com/r/stopsmoking/"; 
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (Exception e) {
            Toast.makeText(this, "Unable to open community link.", Toast.LENGTH_SHORT).show();
        }
    }

    private void resetAll() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Reset data?")
                .setMessage("This deletes your quit plan, check-ins, badges, skills, and motivation note from this device.")
                .setNegativeButton("Cancel", (d, w) -> d.dismiss())
                .setPositiveButton("Reset", (d, w) -> {
                    store.clearAll();
                
                    bindInitialState();
                    renderAll();
                    Toast.makeText(this, "Reset complete.", Toast.LENGTH_SHORT).show();
                })
                .show();
    }


    private void ensureNotificationChannel() {
        if (Build.VERSION.SDK_INT < 26) return;
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (nm == null) return;

        NotificationChannel ch = new NotificationChannel(
                ReminderReceiver.CHANNEL_ID,
                "Quit reminders",
                NotificationManager.IMPORTANCE_DEFAULT
        );
        ch.setDescription("Daily motivation and check-in reminders");
        nm.createNotificationChannel(ch);
    }

  
    private MaterialCardView card(int mStart, int mTop, int mEnd, int mBottom) {
        MaterialCardView c = new MaterialCardView(this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        lp.setMargins(dp(mStart), dp(mTop), dp(mEnd), dp(mBottom));
        c.setLayoutParams(lp);
        c.setCardBackgroundColor(C_CARD);
        c.setRadius(dp(26));
        c.setCardElevation(dp(8));
        c.setUseCompatPadding(true);
        c.setStrokeWidth(dp(1));
        c.setStrokeColor(C_STROKE);
        return c;
    }

    private LinearLayout vStack(int padding) {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setPadding(padding, padding, padding, padding);
        return l;
    }

    private TextView sectionTitle(String s) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextColor(C_TEXT);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        return t;
    }

    private TextView titleText(String s) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextColor(C_TEXT);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 9);
        return t;
    }

    private TextView bodyText(String s) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextColor(Color.parseColor("#374151"));
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 9);
        return t;
    }

    private TextView subText(String s) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextColor(C_SUB);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 9);
        return t;
    }

    private TextView subtleLabel(String s) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextColor(C_SUB);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 9);
        return t;
    }

    private Space space(int h) {
        Space s = new Space(this);
        s.setLayoutParams(new LinearLayout.LayoutParams(1, h));
        return s;
    }

    private MaterialButton pillButton(String text, int bg, int fg) {
        MaterialButton b = new MaterialButton(this);
        b.setText(text);
        b.setAllCaps(false);
        b.setTextColor(fg);
        b.setTypeface(Typeface.DEFAULT_BOLD);
        b.setCornerRadius(dp(999));
        b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(bg));
        b.setRippleColor(android.content.res.ColorStateList.valueOf(Color.parseColor("#22000000")));
        b.setElevation(0f);


        int padH = dp(16);
        int padV = dp(12);
        b.setPadding(padH, padV, padH, padV);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        lp.topMargin = dp(12);
        b.setLayoutParams(lp);

     
        b.setMinHeight(dp(56));

        return b;
    }


    private MaterialButton pillButtonSmall(String text, int bg, int fg) {
        MaterialButton b = new MaterialButton(this);
        b.setText(text);
        b.setAllCaps(false);
        b.setTextColor(fg);
        b.setTypeface(Typeface.DEFAULT_BOLD);
        b.setTextSize(TypedValue.COMPLEX_UNIT_SP, 7);
        b.setCornerRadius(dp(999));
        b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(bg));
        b.setRippleColor(android.content.res.ColorStateList.valueOf(Color.parseColor("#22000000")));
        b.setElevation(0f);

        int padH = dp(12);
        int padV = dp(10);
        b.setPadding(padH, padV, padH, padV);

        b.setMinHeight(dp(44));
        b.setMinimumHeight(dp(44));

        return b;
    }


    private Chip chip(String text, boolean checked) {
        Chip c = new Chip(this);
        c.setText(text);
        c.setCheckable(true);
        c.setChecked(checked);
        c.setTextColor(C_CHARCOAL);
        c.setChipBackgroundColor(android.content.res.ColorStateList.valueOf(Color.WHITE));
        c.setChipStrokeColor(android.content.res.ColorStateList.valueOf(C_STROKE));
        c.setChipStrokeWidth(dp(1));
        c.setRippleColor(android.content.res.ColorStateList.valueOf(Color.parseColor("#22000000")));
        return c;
    }

    private TextInputLayout inputLayout(String hint) {
        TextInputLayout til = new TextInputLayout(this);
        til.setHint(hint);
        til.setBoxBackgroundMode(TextInputLayout.BOX_BACKGROUND_OUTLINE);
        til.setBoxBackgroundColor(Color.WHITE);
        til.setBoxStrokeColor(Color.parseColor("#D5DAE6"));
        til.setHintTextColor(android.content.res.ColorStateList.valueOf(Color.parseColor("#9CA3AF")));
        til.setBoxCornerRadii(dp(20), dp(20), dp(20), dp(20));
        return til;
    }

    private TextInputEditText inputEditNumber() {
        TextInputEditText et = new TextInputEditText(this);
        et.setInputType(InputType.TYPE_CLASS_NUMBER);
        et.setTextColor(C_TEXT);
        et.setHintTextColor(Color.parseColor("#9CA3AF"));
        et.setPadding(dp(14), dp(14), dp(14), dp(14));
        et.setBackgroundColor(Color.TRANSPARENT);
        return et;
    }

    private TextInputEditText inputEditDecimal() {
        TextInputEditText et = new TextInputEditText(this);
        et.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        et.setTextColor(C_TEXT);
        et.setHintTextColor(Color.parseColor("#9CA3AF"));
        et.setPadding(dp(14), dp(14), dp(14), dp(14));
        et.setBackgroundColor(Color.TRANSPARENT);
        return et;
    }

    private TextInputEditText inputEditMultiline(int minLines, int maxLen) {
        TextInputEditText et = new TextInputEditText(this);
        et.setMinLines(minLines);
        et.setGravity(Gravity.TOP);
        et.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        et.setFilters(new InputFilter[]{ new InputFilter.LengthFilter(maxLen) });
        et.setTextColor(C_TEXT);
        et.setHintTextColor(Color.parseColor("#9CA3AF"));
        et.setPadding(dp(14), dp(14), dp(14), dp(14));
        et.setBackgroundColor(Color.TRANSPARENT);
        return et;
    }

    private int dp(int v) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                v,
                getResources().getDisplayMetrics()
        );
    }

    private void hideKeyboard() {
        try {
            View v = getCurrentFocus();
            if (v == null) return;
            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            if (imm != null) imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
        } catch (Exception ignored) {}
    }

    private String textOf(TextInputEditText et) {
        return et.getText() == null ? "" : et.getText().toString();
    }

    private Integer parseInt(TextInputEditText et) {
        try {
            String s = textOf(et).trim();
            if (s.isEmpty()) return null;
            return Integer.parseInt(s);
        } catch (Exception e) {
            return null;
        }
    }

    private Double parseDouble(TextInputEditText et) {
        try {
            String s = textOf(et).trim();
            if (s.isEmpty()) return null;
            return Double.parseDouble(s);
        } catch (Exception e) {
            return null;
        }
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

    private long daysBetween(long startMs, long endMs) {
        long diff = endMs - startMs;
        return TimeUnit.MILLISECONDS.toDays(diff);
    }

    private String getSelectedChipText(ChipGroup group) {
        for (int i = 0; i < group.getChildCount(); i++) {
            View v = group.getChildAt(i);
            if (v instanceof Chip) {
                Chip c = (Chip) v;
                if (c.isChecked()) return String.valueOf(c.getText());
            }
        }
        return "—";
    }

    private List<String> getSelectedChipTexts(ChipGroup group) {
        List<String> out = new ArrayList<>();
        for (int i = 0; i < group.getChildCount(); i++) {
            View v = group.getChildAt(i);
            if (v instanceof Chip) {
                Chip c = (Chip) v;
                if (c.isChecked()) out.add(String.valueOf(c.getText()));
            }
        }
        return out;
    }

    private void clearChipSelections(ChipGroup group) {
        for (int i = 0; i < group.getChildCount(); i++) {
            View v = group.getChildAt(i);
            if (v instanceof Chip) ((Chip) v).setChecked(false);
        }
    }

    private Uri safeParseUri(String s) {
        try {
            if (TextUtils.isEmpty(s)) return null;
            return Uri.parse(s);
        } catch (Exception e) {
            return null;
        }
    }
   
    private enum PlanType { COLD_TURKEY, GRADUAL }

    private static class Plan {
        long quitDateMs = -1L;
        PlanType planType = PlanType.COLD_TURKEY;

        int cigsPerDay = 0;
        double costPerPack = 0.0;
        int cigsPerPack = 20;

        int reductionTarget = 0;
    }

    private static class CheckIn {
        long timestampMs;
        int intensity1to5;
        String mood;
        List<String> triggers = new ArrayList<>();
        String note;
    }

    private static class Skill {
        String id;    
        String title;
        String body;
    }

    private enum Badge {
        FIRST_24H("First 24h"),
        THREE_DAYS("3 days"),
        ONE_WEEK("1 week"),
        TWO_WEEKS("2 weeks"),
        ONE_MONTH("1 month"),
        THREE_MONTHS("3 months"),
        SEVEN_DAY_LOG_STREAK("7-day logging streak");

        final String label;
        Badge(String label) { this.label = label; }
    }


    private static class SmokingStore {
        private static final String PREF = "smoking_cessation_pref_v2";

        private static final String K_PLAN = "plan_json";
        private static final String K_CHECKINS = "checkins_json";

        private static final String K_BADGES = "badges_json";

        private static final String K_WHY_NOTE = "why_note";
        private static final String K_WHY_PHOTO = "why_photo_uri";

        private static final String K_REMINDER_H = "reminder_h";
        private static final String K_REMINDER_M = "reminder_m";

        private static final String K_SKILL_DONE_DAY = "skill_done_day"; // yyyyMMdd
        private static final String K_SKILL_DONE = "skill_done";

        private final SharedPreferences sp;

        SmokingStore(Context ctx) {
            sp = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE);
        }

        Plan getPlan() {
            String json = sp.getString(K_PLAN, null);
            if (TextUtils.isEmpty(json)) return new Plan();
            try {
                JSONObject o = new JSONObject(json);
                Plan p = new Plan();
                p.quitDateMs = o.optLong("quitDateMs", -1L);

                String pt = o.optString("planType", PlanType.COLD_TURKEY.name());
                p.planType = PlanType.valueOf(pt);

                p.cigsPerDay = o.optInt("cigsPerDay", 0);
                p.costPerPack = o.optDouble("costPerPack", 0.0);
                p.cigsPerPack = o.optInt("cigsPerPack", 20);
                p.reductionTarget = o.optInt("reductionTarget", 0);
                return p;
            } catch (Exception e) {
                return new Plan();
            }
        }

        void setPlan(Plan p) {
            try {
                JSONObject o = new JSONObject();
                o.put("quitDateMs", p.quitDateMs);
                o.put("planType", p.planType.name());
                o.put("cigsPerDay", p.cigsPerDay);
                o.put("costPerPack", p.costPerPack);
                o.put("cigsPerPack", p.cigsPerPack);
                o.put("reductionTarget", p.reductionTarget);
                sp.edit().putString(K_PLAN, o.toString()).apply();
            } catch (Exception ignored) {}
        }

        List<CheckIn> getCheckIns() {
            String json = sp.getString(K_CHECKINS, null);
            if (TextUtils.isEmpty(json)) return new ArrayList<>();
            try {
                JSONArray arr = new JSONArray(json);
                List<CheckIn> out = new ArrayList<>();
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject o = arr.getJSONObject(i);
                    CheckIn c = new CheckIn();
                    c.timestampMs = o.optLong("ts", 0);
                    c.intensity1to5 = o.optInt("intensity", 3);
                    c.mood = o.optString("mood", "—");
                    c.note = o.optString("note", "");
                    JSONArray tr = o.optJSONArray("triggers");
                    if (tr != null) {
                        for (int t = 0; t < tr.length(); t++) c.triggers.add(tr.optString(t, ""));
                    }
                    out.add(c);
                }
                // newest first
                Collections.sort(out, (a, b) -> Long.compare(b.timestampMs, a.timestampMs));
                return out;
            } catch (Exception e) {
                return new ArrayList<>();
            }
        }

        void addCheckIn(CheckIn ci) {
            List<CheckIn> items = getCheckIns();
            items.add(ci);
            Collections.sort(items, (a, b) -> Long.compare(b.timestampMs, a.timestampMs));

            JSONArray arr = new JSONArray();
            try {
                for (CheckIn c : items) {
                    JSONObject o = new JSONObject();
                    o.put("ts", c.timestampMs);
                    o.put("intensity", c.intensity1to5);
                    o.put("mood", c.mood);
                    o.put("note", c.note);

                    JSONArray tr = new JSONArray();
                    for (String s : c.triggers) tr.put(s);
                    o.put("triggers", tr);

                    arr.put(o);
                }
            } catch (Exception ignored) {}

            sp.edit().putString(K_CHECKINS, arr.toString()).apply();
        }

        void setEarnedBadges(EnumSet<Badge> badges) {
            JSONArray arr = new JSONArray();
            for (Badge b : badges) arr.put(b.name());
            sp.edit().putString(K_BADGES, arr.toString()).apply();
        }

        boolean hasCheckInStreak(int days) {

            List<CheckIn> items = getCheckIns();
            if (items.isEmpty()) return false;

            java.util.HashSet<Integer> dayKeys = new java.util.HashSet<>();
            Calendar c = Calendar.getInstance();

            for (CheckIn ci : items) {
                c.setTimeInMillis(ci.timestampMs);
                int key = dayKey(c);
                dayKeys.add(key);
            }

            Calendar cur = Calendar.getInstance();
            for (int i = 0; i < days; i++) {
                int key = dayKey(cur);
                if (!dayKeys.contains(key)) return false;
                cur.add(Calendar.DAY_OF_YEAR, -1);
            }
            return true;
        }

        Skill getTodaySkill() {
        
            List<Skill> skills = defaultSkills();
            Calendar c = Calendar.getInstance();
            int dayIndex = dayKey(c) % skills.size();
            Skill s = skills.get(dayIndex);

       
            s.id = "skill_" + dayKey(c);
            return s;
        }

        boolean isSkillDoneToday() {
            Calendar c = Calendar.getInstance();
            int today = dayKey(c);
            int savedDay = sp.getInt(K_SKILL_DONE_DAY, -1);
            if (savedDay != today) return false;
            return sp.getBoolean(K_SKILL_DONE, false);
        }

        void setSkillDoneToday(boolean done) {
            Calendar c = Calendar.getInstance();
            int today = dayKey(c);
            sp.edit()
                    .putInt(K_SKILL_DONE_DAY, today)
                    .putBoolean(K_SKILL_DONE, done)
                    .apply();
        }

        void setWhyNote(String note) {
            sp.edit().putString(K_WHY_NOTE, note == null ? "" : note).apply();
        }

        String getWhyNote() {
            return sp.getString(K_WHY_NOTE, "");
        }

        void setWhyPhotoUri(String uri) {
            sp.edit().putString(K_WHY_PHOTO, uri == null ? "" : uri).apply();
        }

        String getWhyPhotoUri() {
            return sp.getString(K_WHY_PHOTO, "");
        }

        void setReminderTime(int hour, int minute) {
            sp.edit().putInt(K_REMINDER_H, hour).putInt(K_REMINDER_M, minute).apply();
        }

        int getReminderHour() { return sp.getInt(K_REMINDER_H, 9); }
        int getReminderMinute() { return sp.getInt(K_REMINDER_M, 0); }

        void clearAll() {
            sp.edit().clear().apply();
        }

        private static int dayKey(Calendar c) {
         
            int y = c.get(Calendar.YEAR);
            int m = c.get(Calendar.MONTH) + 1;
            int d = c.get(Calendar.DAY_OF_MONTH);
            return y * 10000 + m * 100 + d;
        }

        private static List<Skill> defaultSkills() {
            ArrayList<Skill> list = new ArrayList<>();

            list.add(skill("Trigger mapping",
                    "Write 1 trigger you faced today. Then write 1 alternative action you’ll use next time. "
                            + "Goal: make the plan automatic before the craving hits."));

            list.add(skill("Delay & distract (CBT)",
                    "When you want a cigarette, delay 10 minutes and do a short task: water, walk, stretch, or message someone. "
                            + "Cravings peak and fade—practice riding the peak."));

            list.add(skill("Urge surfing (ACT)",
                    "Label the experience: “I’m noticing an urge.” Rate it 1–5. Observe it like a wave rising and falling, without obeying it."));

            list.add(skill("Implementation intention",
                    "Fill this: IF I feel ____ (trigger), THEN I will ____ (replacement). "
                            + "Example: IF commute craving, THEN I chew gum + play a podcast."));

            list.add(skill("Self-compassion reset",
                    "If you slipped, avoid shame. Write: “A slip is data, not a verdict.” Then list 1 small change for tomorrow."));

            list.add(skill("Values reminder",
                    "Write 1 reason you’re quitting that reflects your values: health, family, freedom, money, or performance. "
                            + "Values outlast motivation."));

            return list;
        }

        private static Skill skill(String title, String body) {
            Skill s = new Skill();
            s.title = title;
            s.body = body;
            return s;
        }
    }

   
    private static class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.VH> {
        private final SimpleDateFormat df = new SimpleDateFormat("MMM d, HH:mm", Locale.US);
        private List<CheckIn> items;

        HistoryAdapter(List<CheckIn> items) {
            this.items = items == null ? new ArrayList<>() : items;
        }

        void setItems(List<CheckIn> newItems) {
            this.items = newItems == null ? new ArrayList<>() : newItems;
            notifyDataSetChanged();
        }

        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            Context ctx = parent.getContext();

            LinearLayout row = new LinearLayout(ctx);
            row.setOrientation(LinearLayout.VERTICAL);
            row.setPadding(dp(ctx, 12), dp(ctx, 10), dp(ctx, 12), dp(ctx, 10));

            GradientDrawable bg = new GradientDrawable();
            bg.setColor(Color.WHITE);
            bg.setCornerRadius(dp(ctx, 18));
            bg.setStroke(dp(ctx, 1), Color.parseColor("#E5E9F5"));
            row.setBackground(bg);

            RecyclerView.LayoutParams lp = new RecyclerView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            lp.bottomMargin = dp(ctx, 10);
            row.setLayoutParams(lp);

            TextView top = new TextView(ctx);
            top.setTextColor(Color.parseColor("#111827"));
            top.setTypeface(Typeface.DEFAULT_BOLD);
            top.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);

            TextView mid = new TextView(ctx);
            mid.setTextColor(Color.parseColor("#374151"));
            mid.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);

            TextView bot = new TextView(ctx);
            bot.setTextColor(Color.parseColor("#6B7280"));
            bot.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);

            row.addView(top);
            row.addView(mid);
            row.addView(bot);

            return new VH(row, top, mid, bot);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int position) {
            CheckIn c = items.get(position);
            String when = df.format(c.timestampMs);

            h.top.setText("Intensity " + c.intensity1to5 + " / 5 • " + when);

            String trig = (c.triggers == null || c.triggers.isEmpty())
                    ? "Triggers: —"
                    : "Triggers: " + TextUtils.join(", ", c.triggers);

            h.mid.setText("Mood: " + (TextUtils.isEmpty(c.mood) ? "—" : c.mood) + " • " + trig);

            String note = TextUtils.isEmpty(c.note) ? "Note: —" : "Note: " + c.note;
            h.bot.setText(note);
        }

        @Override
        public int getItemCount() {
            return items == null ? 0 : items.size();
        }

        static class VH extends RecyclerView.ViewHolder {
            final TextView top, mid, bot;
            VH(@NonNull View itemView, TextView top, TextView mid, TextView bot) {
                super(itemView);
                this.top = top;
                this.mid = mid;
                this.bot = bot;
            }
        }

        private static int dp(Context ctx, int v) {
            return (int) TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP, v, ctx.getResources().getDisplayMetrics()
            );
        }
    }

   
    public static class ReminderReceiver extends BroadcastReceiver {
        public static final String ACTION_REMINDER = "com.cypher.zealth.ACTION_SMOKING_REMINDER";
        public static final String CHANNEL_ID = "quit_reminders_channel";

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null) return;
            if (!ACTION_REMINDER.equals(intent.getAction())) return;

            NotificationCompat.Builder b = new NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                    .setContentTitle("Quit check-in")
                    .setContentText("Log a quick craving check-in. Small actions protect your streak.")
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setAutoCancel(true);

            NotificationManagerCompat nm = NotificationManagerCompat.from(context);
            try {
                nm.notify(202, b.build());
            } catch (SecurityException ignored) {
                
            }
        }
    }
}
