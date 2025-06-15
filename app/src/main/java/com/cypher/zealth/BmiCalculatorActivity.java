package com.cypher.zealth;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewPropertyAnimator;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.divider.MaterialDivider;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.Locale;

public class BmiCalculatorActivity extends AppCompatActivity {

    
    private static final double GLOBAL_AVG_BMI_REFERENCE = 24.6;
    private static final double HEALTHY_MIN_BMI = 18.5;
    private static final double HEALTHY_MAX_BMI = 24.9;
    private static final int C_BG = 0xFFF6FAFF;        
    private static final int C_CARD = 0xFFFFFFFF;     
    private static final int C_TEXT = 0xFF0B1B3A;     
    private static final int C_SUB = 0xFF2B5AA6;       
    private static final int C_MUTED = 0xFF64748B;     
    private static final int C_PRIMARY = 0xFF2563EB;   
    private static final int C_PRIMARY_SOFT = 0xFFDBEAFE;
    private static final int C_STROKE = 0x3393C5FD;    
    private static final int C_INPUT_BG = 0xFFF7FBFF;
    private static final int C_INPUT_HINT = 0xFF7AA7E8;
    private static final int C_SPINNER_BG = 0xFFFFFFFF;
    private static final int C_SPINNER_TEXT = 0xFF0B1B3A;
    private MaterialButtonToggleGroup unitToggle;
    private MaterialButton btnMetric, btnImperial;
    private View groupMetric, groupImperial;
    private TextInputLayout tilHeightCm, tilWeightKg, tilHeightFt, tilHeightIn, tilWeightLb;
    private TextInputEditText etHeightCm, etWeightKg, etHeightFt, etHeightIn, etWeightLb;
    private TextInputLayout tilAge;
    private TextInputEditText etAge;
    private MaterialButtonToggleGroup sexToggle;
    private MaterialButton btnMale, btnFemale;
    private Spinner activitySpinner;
    private MaterialButton btnCalculate, btnSharePlan, btnCopyPlan;
    private MaterialCardView resultCard;
    private TextView tvBmiValue, tvCategory, tvCompare, tvHealthyRange, tvGuidance, tvPlanTitle, tvPlanBody, tvDisclaimer;
    private Chip chipCategory;
    private BmiComparisonChartView chartView;
    private ScrollView scrollView;
    private boolean suppressWatchers = false;
    private TextWatcher sharedWatcher;
    private final Handler ui = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FrameLayout root = new FrameLayout(this);
        root.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

    
        View bg = new View(this);
        bg.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        bg.setBackground(makeBackgroundGradient());
        root.addView(bg);

        LinearLayout screen = new LinearLayout(this);
        screen.setOrientation(LinearLayout.VERTICAL);
        screen.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        MaterialCardView toolbarCard = card(16, 12, 16, 8);
        toolbarCard.setCardBackgroundColor(0xFFF2F7FF);
        toolbarCard.setRadius(dp(26));
        toolbarCard.setCardElevation(dp(7));
        toolbarCard.setStrokeColor(C_STROKE);
        toolbarCard.setStrokeWidth(dp(1));

        MaterialToolbar toolbar = new MaterialToolbar(this);
        toolbar.setTitle("");
        toolbar.setSubtitle("");
        toolbar.setBackgroundColor(0x00000000);
        toolbar.setNavigationIcon(R.drawable.ic_back_medical);
        toolbar.setNavigationOnClickListener(v -> finish());

        LinearLayout titleWrap = new LinearLayout(this);
        titleWrap.setOrientation(LinearLayout.VERTICAL);

        TextView tvTitle = new TextView(this);
        tvTitle.setText("BMI Calculator");
        tvTitle.setTextColor(C_TEXT);
        tvTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        tvTitle.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

        TextView tvSub = new TextView(this);
        tvSub.setText("General information for personal reference");
        tvSub.setTextColor(C_SUB);
        tvSub.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);

        titleWrap.addView(tvTitle);
        titleWrap.addView(tvSub);

        MaterialToolbar.LayoutParams lp = new MaterialToolbar.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        lp.gravity = Gravity.START | Gravity.CENTER_VERTICAL;
        titleWrap.setLayoutParams(lp);

        toolbar.addView(titleWrap);
        toolbarCard.addView(toolbar);
        scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);
        scrollView.setOverScrollMode(View.OVER_SCROLL_NEVER);
        LinearLayout.LayoutParams svLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0);
        svLp.weight = 1f;
        scrollView.setLayoutParams(svLp);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(16), dp(10), dp(16), dp(16));
        scrollView.addView(content, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        content.addView(buildUnitToggleCard());
        groupMetric = buildMetricInputs();
        groupImperial = buildImperialInputs();
        groupImperial.setVisibility(View.GONE);
        content.addView(groupMetric);
        content.addView(groupImperial);

        content.addView(buildPlanInputsCard());

        resultCard = buildResultCard();
        resultCard.setVisibility(View.GONE);
        content.addView(resultCard);
        LinearLayout bottomBar = new LinearLayout(this);
        bottomBar.setOrientation(LinearLayout.VERTICAL);
        bottomBar.setPadding(dp(16), dp(8), dp(16), dp(14));

        btnCalculate = new MaterialButton(this);
        btnCalculate.setText("Calculate BMI");
        btnCalculate.setAllCaps(false);
        btnCalculate.setTextColor(0xFFFFFFFF);
        btnCalculate.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        btnCalculate.setCornerRadius(dp(999));
        btnCalculate.setBackgroundTintList(android.content.res.ColorStateList.valueOf(C_PRIMARY));
        btnCalculate.setRippleColor(android.content.res.ColorStateList.valueOf(0x22FFFFFF));
        btnCalculate.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(56)));
        btnCalculate.setElevation(dp(2));

        bottomBar.addView(btnCalculate);

        screen.addView(toolbarCard);
        screen.addView(scrollView);
        screen.addView(bottomBar);
        root.addView(screen);

        setContentView(root);

        btnCalculate.setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.CONFIRM);
            calculate();
        });

        attachImeDone(etWeightKg);
        attachImeDone(etWeightLb);

        sharedWatcher = new SimpleWatcher(this::updateCalculateEnabled);
        addWatcher(etHeightCm, sharedWatcher);
        addWatcher(etWeightKg, sharedWatcher);
        addWatcher(etHeightFt, sharedWatcher);
        addWatcher(etHeightIn, sharedWatcher);
        addWatcher(etWeightLb, sharedWatcher);
        addWatcher(etAge, sharedWatcher);

        unitToggle.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) return;

            clearErrors();
            hideResult();

            boolean metricSelected = (checkedId == btnMetric.getId());

            if (metricSelected) convertImperialToMetricIfPossible();
            else convertMetricToImperialIfPossible();

            setUnitMode(metricSelected);
            updateCalculateEnabled();
            group.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
        });

        sexToggle.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) return;
            applySexToggleStyles();
            updateCalculateEnabled();
        });

        applySexToggleStyles();
        updateCalculateEnabled();
    }

    private View buildUnitToggleCard() {
        MaterialCardView card = card(0, 10, 0, 10);
        card.setRadius(dp(26));
        card.setCardElevation(dp(7));
        card.setCardBackgroundColor(C_CARD);
        card.setStrokeColor(C_STROKE);
        card.setStrokeWidth(dp(1));

        LinearLayout wrap = new LinearLayout(this);
        wrap.setOrientation(LinearLayout.VERTICAL);
        wrap.setPadding(dp(14), dp(14), dp(14), dp(14));

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);

        TextView label = t("Units", 13, true, C_SUB);
        LinearLayout.LayoutParams lpl = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        header.addView(label, lpl);

        TextView hint = t("We auto-convert when switching.", 12, false, C_MUTED);
        header.addView(hint);

        wrap.addView(header);
        wrap.addView(spacer(10));

        unitToggle = new MaterialButtonToggleGroup(this);
        unitToggle.setSingleSelection(true);
        unitToggle.setSelectionRequired(true);

        btnMetric = new MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
        btnMetric.setId(ViewCompat.generateViewId());
        btnMetric.setText("Metric (cm, kg)");
        btnMetric.setAllCaps(false);
        btnMetric.setCornerRadius(dp(18));

        btnImperial = new MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
        btnImperial.setId(ViewCompat.generateViewId());
        btnImperial.setText("Imperial (ft, lb)");
        btnImperial.setAllCaps(false);
        btnImperial.setCornerRadius(dp(18));

        applyToggleButtonStateStyle(btnMetric);
        applyToggleButtonStateStyle(btnImperial);

        LinearLayout.LayoutParams btnLp = new LinearLayout.LayoutParams(0, dp(48), 1f);
        btnMetric.setLayoutParams(btnLp);
        btnImperial.setLayoutParams(btnLp);

        unitToggle.addView(btnMetric);
        unitToggle.addView(btnImperial);
        unitToggle.check(btnMetric.getId());

        wrap.addView(unitToggle, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        card.addView(wrap);
        return card;
    }

    private View buildMetricInputs() {
        MaterialCardView card = card(0, 10, 0, 10);
        styleSectionCard(card);

        LinearLayout wrap = new LinearLayout(this);
        wrap.setOrientation(LinearLayout.VERTICAL);
        wrap.setPadding(dp(16), dp(16), dp(16), dp(16));

        wrap.addView(sectionHeader("Your measurements"));
        wrap.addView(sectionSub("Enter height and weight. We’ll compute BMI and a healthy range."));

        wrap.addView(spacer(12));
        wrap.addView(t("Height", 13, true, C_SUB));

        tilHeightCm = til("Height (cm)");
        etHeightCm = etNumber(true);
        etHeightCm.setImeOptions(EditorInfo.IME_ACTION_NEXT);
        tilHeightCm.addView(etHeightCm);
        wrap.addView(tilHeightCm);

        wrap.addView(spacer(12));
        wrap.addView(t("Weight", 13, true, C_SUB));

        tilWeightKg = til("Weight (kg)");
        etWeightKg = etNumber(true);
        etWeightKg.setImeOptions(EditorInfo.IME_ACTION_DONE);
        tilWeightKg.addView(etWeightKg);
        wrap.addView(tilWeightKg);

        card.addView(wrap);
        return card;
    }

    private View buildImperialInputs() {
        MaterialCardView card = card(0, 10, 0, 10);
        styleSectionCard(card);

        LinearLayout wrap = new LinearLayout(this);
        wrap.setOrientation(LinearLayout.VERTICAL);
        wrap.setPadding(dp(16), dp(16), dp(16), dp(16));

        wrap.addView(sectionHeader("Your measurements"));
        wrap.addView(sectionSub("Enter height and weight. We’ll compute BMI and a healthy range."));

        wrap.addView(spacer(12));
        wrap.addView(t("Height", 13, true, C_SUB));

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);

        tilHeightFt = til("Feet (ft)");
        etHeightFt = etNumber(false);
        etHeightFt.setFilters(new InputFilter[]{new InputFilter.LengthFilter(1)});
        tilHeightFt.addView(etHeightFt);

        tilHeightIn = til("Inches (in)");
        etHeightIn = etNumber(false);
        etHeightIn.setFilters(new InputFilter[]{new InputFilter.LengthFilter(2)});
        tilHeightIn.addView(etHeightIn);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        row.addView(tilHeightFt, lp);
        row.addView(spacerH(10));
        row.addView(tilHeightIn, lp);

        wrap.addView(row);

        wrap.addView(spacer(12));
        wrap.addView(t("Weight", 13, true, C_SUB));

        tilWeightLb = til("Weight (lb)");
        etWeightLb = etNumber(true);
        etWeightLb.setImeOptions(EditorInfo.IME_ACTION_DONE);
        tilWeightLb.addView(etWeightLb);
        wrap.addView(tilWeightLb);

        card.addView(wrap);
        return card;
    }

    private View buildPlanInputsCard() {
        MaterialCardView card = card(0, 10, 0, 10);
        styleSectionCard(card);

        LinearLayout wrap = new LinearLayout(this);
        wrap.setOrientation(LinearLayout.VERTICAL);
        wrap.setPadding(dp(16), dp(16), dp(16), dp(16));

        wrap.addView(sectionHeader("Optional details"));
        wrap.addView(sectionSub("Optional. Used only to show general, non-medical estimates."));


        wrap.addView(spacer(12));

        tilAge = til("Age (years)");
        etAge = etNumber(false);
        etAge.setFilters(new InputFilter[]{new InputFilter.LengthFilter(3)});
        tilAge.addView(etAge);
        wrap.addView(tilAge);

        wrap.addView(spacer(12));

        wrap.addView(t("Sex (optional, for general estimate)", 13, true, C_SUB));


        sexToggle = new MaterialButtonToggleGroup(this);
        sexToggle.setSingleSelection(true);
        sexToggle.setSelectionRequired(true);

        btnMale = new MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
        btnMale.setId(ViewCompat.generateViewId());
        btnMale.setText("Male");
        btnMale.setAllCaps(false);
        btnMale.setCornerRadius(dp(18));

        btnFemale = new MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
        btnFemale.setId(ViewCompat.generateViewId());
        btnFemale.setText("Female");
        btnFemale.setAllCaps(false);
        btnFemale.setCornerRadius(dp(18));

        applyToggleButtonStateStyle(btnMale);
        applyToggleButtonStateStyle(btnFemale);

        LinearLayout.LayoutParams sLp = new LinearLayout.LayoutParams(0, dp(46), 1f);
        btnMale.setLayoutParams(sLp);
        btnFemale.setLayoutParams(sLp);

        sexToggle.addView(btnMale);
        sexToggle.addView(btnFemale);

        sexToggle.check(btnMale.getId());
        applySexToggleStyles();

        wrap.addView(sexToggle);

        wrap.addView(spacer(12));

        wrap.addView(t("Activity level", 13, true, C_SUB));
        activitySpinner = new Spinner(this);

        String[] levels = new String[]{
                "Sedentary (little exercise)",
                "Light (1–3 days/week)",
                "Moderate (3–5 days/week)",
                "Active (6–7 days/week)",
                "Very active (hard training / physical job)"
        };

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, levels) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                TextView tv = (TextView) super.getView(position, convertView, parent);
                tv.setTextColor(C_TEXT);
                tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
                tv.setPadding(dp(12), dp(12), dp(12), dp(12));
                return tv;
            }

            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                TextView tv = (TextView) super.getDropDownView(position, convertView, parent);
                tv.setTextColor(C_SPINNER_TEXT);
                tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
                tv.setPadding(dp(14), dp(12), dp(14), dp(12));
                tv.setBackgroundColor(C_SPINNER_BG);
                return tv;
            }
        };
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        activitySpinner.setAdapter(adapter);

        GradientDrawable popupBg = new GradientDrawable();
        popupBg.setColor(C_SPINNER_BG);
        popupBg.setCornerRadius(dp(14));
        activitySpinner.setPopupBackgroundDrawable(popupBg);

        wrap.addView(activitySpinner);

        card.addView(wrap);
        return card;
    }

    private MaterialCardView buildResultCard() {
        MaterialCardView card = card(0, 10, 0, 10);
        styleSectionCard(card);

        LinearLayout wrap = new LinearLayout(this);
        wrap.setOrientation(LinearLayout.VERTICAL);
        wrap.setPadding(dp(16), dp(16), dp(16), dp(16));

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);

        TextView header = t("Your results", 14, true, C_TEXT);
        LinearLayout.LayoutParams hlp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        top.addView(header, hlp);

        chipCategory = new Chip(this);
        chipCategory.setText("—");
        chipCategory.setTextColor(C_PRIMARY);
        chipCategory.setChipBackgroundColor(android.content.res.ColorStateList.valueOf(C_PRIMARY_SOFT));
        chipCategory.setClickable(false);
        chipCategory.setCheckable(false);
        top.addView(chipCategory);

        wrap.addView(top);
        wrap.addView(spacer(10));

        tvBmiValue = t("—", 22, true, C_TEXT);
        wrap.addView(tvBmiValue);

        tvCategory = t("—", 14, true, C_PRIMARY);
        tvCategory.setPadding(0, dp(6), 0, 0);
        wrap.addView(tvCategory);

        chartView = new BmiComparisonChartView(this);
        LinearLayout.LayoutParams cLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(130));
        cLp.topMargin = dp(12);
        wrap.addView(chartView, cLp);

        tvCompare = t("—", 13, false, C_SUB);
        tvCompare.setPadding(0, dp(10), 0, 0);
        wrap.addView(tvCompare);

        tvHealthyRange = t("—", 13, false, C_SUB);
        tvHealthyRange.setPadding(0, dp(6), 0, 0);
        wrap.addView(tvHealthyRange);

        wrap.addView(spacer(12));
        wrap.addView(new MaterialDivider(this));

        tvGuidance = t("—", 14, false, C_TEXT);
        tvGuidance.setLineSpacing(dp(2), 1f);
        tvGuidance.setPadding(0, dp(12), 0, 0);
        wrap.addView(tvGuidance);

        wrap.addView(spacer(12));
        wrap.addView(new MaterialDivider(this));

        tvPlanTitle = t("Personal plan", 16, true, C_TEXT);
        tvPlanTitle.setPadding(0, dp(12), 0, dp(6));
        wrap.addView(tvPlanTitle);

        tvPlanBody = t("—", 13, false, C_TEXT);
        tvPlanBody.setLineSpacing(dp(2), 1f);
        wrap.addView(tvPlanBody);

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setPadding(0, dp(12), 0, 0);

        btnCopyPlan = new MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
        btnCopyPlan.setText("Copy plan");
        btnCopyPlan.setAllCaps(false);
        btnCopyPlan.setCornerRadius(dp(16));

        btnSharePlan = new MaterialButton(this);
        btnSharePlan.setText("Share");
        btnSharePlan.setAllCaps(false);
        btnSharePlan.setTextColor(0xFFFFFFFF);
        btnSharePlan.setBackgroundTintList(android.content.res.ColorStateList.valueOf(C_PRIMARY));
        btnSharePlan.setCornerRadius(dp(999));

        LinearLayout.LayoutParams aLp = new LinearLayout.LayoutParams(0, dp(46), 1f);
        actions.addView(btnCopyPlan, aLp);
        actions.addView(spacerH(10));
        actions.addView(btnSharePlan, aLp);

        wrap.addView(actions);

        tvDisclaimer = t(
                "Disclaimer: This feature is for general informational purposes only and is not a medical device. "
                        + "It does not diagnose, treat, cure, or prevent any medical condition. "
                        + "BMI is a population-level indicator and may not reflect individual health. "
                        + "For medical advice or concerns, consult a qualified healthcare professional.",
                12, false, C_MUTED
        );

        tvDisclaimer.setPadding(0, dp(14), 0, 0);
        wrap.addView(tvDisclaimer);

        card.addView(wrap);

        btnCopyPlan.setOnClickListener(v -> {
            CharSequence plan = tvPlanBody.getText();
            if (!TextUtils.isEmpty(plan) && !"—".contentEquals(plan)) {
                ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                if (cm != null) {
                    cm.setPrimaryClip(ClipData.newPlainText("BMI Plan", plan));
                    Toast.makeText(this, "Plan copied to clipboard.", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Calculate first.", Toast.LENGTH_SHORT).show();
            }
        });

        btnSharePlan.setOnClickListener(v -> sharePlan());

        return card;
    }

    private void styleSectionCard(MaterialCardView card) {
        card.setRadius(dp(26));
        card.setCardElevation(dp(7));
        card.setCardBackgroundColor(C_CARD);
        card.setStrokeColor(C_STROKE);
        card.setStrokeWidth(dp(1));
    }

    private TextView sectionHeader(String text) {
        TextView tv = t(text, 14, true, C_TEXT);
        tv.setPadding(0, 0, 0, dp(4));
        return tv;
    }

    private TextView sectionSub(String text) {
        TextView tv = t(text, 12, false, C_MUTED);
        tv.setLineSpacing(dp(1), 1f);
        return tv;
    }

    private GradientDrawable makeBackgroundGradient() {
  
        GradientDrawable gd = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{0xFFF6FAFF, 0xFFF3F8FF, 0xFFFFFFFF}
        );
        gd.setGradientType(GradientDrawable.LINEAR_GRADIENT);
        return gd;
    }

    private void convertMetricToImperialIfPossible() {
        Double cm = parseDouble(etHeightCm);
        Double kg = parseDouble(etWeightKg);
        if (cm == null && kg == null) return;

        suppressWatchers = true;
        try {
            if (cm != null && cm > 0) {
                double totalInches = cm / 2.54;
                int ft = (int) Math.floor(totalInches / 12.0);
                int in = (int) Math.round(totalInches - (ft * 12.0));
                if (in == 12) { ft += 1; in = 0; }

                setText(etHeightFt, String.valueOf(ft));
                setText(etHeightIn, String.valueOf(in));
            }
            if (kg != null && kg > 0) {
                double lb = kg / 0.45359237;
                setText(etWeightLb, String.format(Locale.US, "%.1f", lb));
            }
        } finally {
            suppressWatchers = false;
        }
    }

    private void convertImperialToMetricIfPossible() {
        Integer ft = parseInt(etHeightFt);
        Integer in = parseInt(etHeightIn);
        Double lb = parseDouble(etWeightLb);
        if (ft == null && in == null && lb == null) return;

        suppressWatchers = true;
        try {
            if (ft != null || in != null) {
                int safeFt = (ft == null) ? 0 : ft;
                int safeIn = (in == null) ? 0 : in;
                int totalInches = (safeFt * 12) + safeIn;
                if (totalInches > 0) {
                    double cm = totalInches * 2.54;
                    setText(etHeightCm, String.format(Locale.US, "%.1f", cm));
                }
            }
            if (lb != null && lb > 0) {
                double kg = lb * 0.45359237;
                setText(etWeightKg, String.format(Locale.US, "%.1f", kg));
            }
        } finally {
            suppressWatchers = false;
        }
    }

    private void setText(TextInputEditText et, String value) {
        if (et == null) return;
        et.setText(value);
        if (et.getText() != null) et.setSelection(et.getText().length());
    }

   
    private void applyToggleButtonStateStyle(MaterialButton b) {
        final int[][] states = new int[][]{
                new int[]{android.R.attr.state_checked},
                new int[]{-android.R.attr.state_checked}
        };

        final int[] bgColors = new int[]{
                C_PRIMARY,
                0xFFE7F0FF
        };

        final int[] textColors = new int[]{
                0xFFFFFFFF,
                C_TEXT
        };

        final int[] strokeColors = new int[]{
                C_PRIMARY,
                0xFF93C5FD
        };

        b.setBackgroundTintList(new android.content.res.ColorStateList(states, bgColors));
        b.setTextColor(new android.content.res.ColorStateList(states, textColors));
        b.setStrokeColor(new android.content.res.ColorStateList(states, strokeColors));
        b.setStrokeWidth(dp(1));
        b.setRippleColor(android.content.res.ColorStateList.valueOf(0x22FFFFFF));
        b.setElevation(0);
    }

    private void applySexToggleStyles() {
        if (btnMale != null) btnMale.refreshDrawableState();
        if (btnFemale != null) btnFemale.refreshDrawableState();
    }
    private void setUnitMode(boolean metric) {
        groupMetric.setVisibility(metric ? View.VISIBLE : View.GONE);
        groupImperial.setVisibility(metric ? View.GONE : View.VISIBLE);
    }

    private void calculate() {
        clearErrors();

        boolean metric = (unitToggle.getCheckedButtonId() == btnMetric.getId());

        Double heightM;
        Double weightKg;

        if (metric) {
            Double cm = parseDouble(etHeightCm);
            Double kg = parseDouble(etWeightKg);

            if (cm == null) tilHeightCm.setError("Enter your height in centimeters.");
            else if (cm < 80 || cm > 250) tilHeightCm.setError("Check height (expected 80–250 cm).");

            if (kg == null) tilWeightKg.setError("Enter your weight in kilograms.");
            else if (kg < 20 || kg > 400) tilWeightKg.setError("Check weight (expected 20–400 kg).");

            if (tilHeightCm.getError() != null || tilWeightKg.getError() != null) return;

            heightM = cm / 100.0;
            weightKg = kg;

        } else {
            Integer ft = parseInt(etHeightFt);
            Integer in = parseInt(etHeightIn);
            Double lb = parseDouble(etWeightLb);

            if (ft == null) tilHeightFt.setError("Feet required.");
            else if (ft < 3 || ft > 8) tilHeightFt.setError("Check feet (expected 3–8).");

            if (in == null) tilHeightIn.setError("Inches required.");
            else if (in < 0 || in > 11) tilHeightIn.setError("Inches must be 0–11.");

            if (lb == null) tilWeightLb.setError("Enter your weight in pounds.");
            else if (lb < 44 || lb > 880) tilWeightLb.setError("Check weight (expected 44–880 lb).");

            if (tilHeightFt.getError() != null || tilHeightIn.getError() != null || tilWeightLb.getError() != null) return;

            int totalInches = (ft * 12) + in;
            heightM = totalInches * 0.0254;
            weightKg = lb * 0.45359237;
        }

        if (heightM == null || weightKg == null || heightM <= 0 || weightKg <= 0) return;

        double bmi = weightKg / (heightM * heightM);
        BmiCategory cat = classify(bmi);

        double minKg = HEALTHY_MIN_BMI * (heightM * heightM);
        double maxKg = HEALTHY_MAX_BMI * (heightM * heightM);

        double diff = bmi - GLOBAL_AVG_BMI_REFERENCE;
        double pct = (diff / GLOBAL_AVG_BMI_REFERENCE) * 100.0;
        String sign = (diff >= 0) ? "+" : "−";

        Integer age = parseInt(etAge);
        boolean male = (sexToggle.getCheckedButtonId() == btnMale.getId());
        int activityIdx = activitySpinner.getSelectedItemPosition();

        CalorieEstimate ce = estimateCaloriesOrNull(age, male, heightM, weightKg, activityIdx);
        Plan plan = buildPlan(bmi, cat, heightM, weightKg, minKg, maxKg, ce);

        // Populate UI
        tvBmiValue.setText(String.format(Locale.US, "BMI %.1f", bmi));
        tvCategory.setText(cat.longLabel);

        chipCategory.setText(cat.chip);
        chipCategory.setChipBackgroundColor(android.content.res.ColorStateList.valueOf(cat.chipBg));
        chipCategory.setTextColor(cat.chipText);

        tvHealthyRange.setText(String.format(
                Locale.US,
                "Healthy weight range for your height: %.1f–%.1f kg (%.0f–%.0f lb)",
                minKg, maxKg, minKg / 0.45359237, maxKg / 0.45359237
        ));

        tvCompare.setText(String.format(
                Locale.US,
                "Compared to a reference average (%.1f): %s%.1f BMI (%s%.0f%%)",
                GLOBAL_AVG_BMI_REFERENCE,
                sign, Math.abs(diff),
                sign, Math.abs(pct)
        ));

        chartView.setValues(bmi, GLOBAL_AVG_BMI_REFERENCE);
        tvGuidance.setText(plan.summary);
        tvPlanBody.setText(plan.details);

        showResultAnimated();

        scrollView.post(() -> scrollView.smoothScrollTo(0, resultCard.getTop() - dp(12)));
    }

    private void showResultAnimated() {
        if (resultCard.getVisibility() == View.VISIBLE) return;

        resultCard.setVisibility(View.VISIBLE);
        resultCard.setAlpha(0f);
        resultCard.setTranslationY(dp(8));

        ViewPropertyAnimator a = resultCard.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(220)
                .setInterpolator(new android.view.animation.AccelerateDecelerateInterpolator());

        a.start();
    }

    private void hideResult() {
        resultCard.setVisibility(View.GONE);
    }

    private void clearErrors() {
        if (tilHeightCm != null) tilHeightCm.setError(null);
        if (tilWeightKg != null) tilWeightKg.setError(null);
        if (tilHeightFt != null) tilHeightFt.setError(null);
        if (tilHeightIn != null) tilHeightIn.setError(null);
        if (tilWeightLb != null) tilWeightLb.setError(null);
        if (tilAge != null) tilAge.setError(null);
    }

    private void updateCalculateEnabled() {
        if (suppressWatchers) return;

        boolean metric = (unitToggle.getCheckedButtonId() == btnMetric.getId());

        boolean ok;
        if (metric) {
            ok = parseDouble(etHeightCm) != null && parseDouble(etWeightKg) != null;
        } else {
            ok = parseInt(etHeightFt) != null && parseInt(etHeightIn) != null && parseDouble(etWeightLb) != null;
        }

        Integer age = parseInt(etAge);
        if (age != null && (age < 10 || age > 120)) {
            tilAge.setError("Check age (expected 10–120).");
            ok = false;
        } else if (tilAge != null) {
            tilAge.setError(null);
        }

        btnCalculate.setEnabled(ok);
        btnCalculate.setAlpha(ok ? 1f : 0.55f);
    }

    private Plan buildPlan(double bmi, BmiCategory cat, double heightM, double weightKg,
                           double healthyMinKg, double healthyMaxKg, @Nullable CalorieEstimate ce) {

        StringBuilder summary = new StringBuilder();
        summary.append("Category: ").append(cat.longLabel).append("\n")
                .append("Use this information for personal reference only.\n");

        StringBuilder d = new StringBuilder();

        d.append(section("Your measurements (summary)"));
        d.append(String.format(Locale.US,
                "• BMI: %.1f (%s)\n• Height: %.0f cm\n• Weight: %.1f kg (%.0f lb)\n",
                bmi, cat.longLabel, heightM * 100.0, weightKg, weightKg / 0.45359237));

        d.append(String.format(Locale.US,
                "• Reference healthy range for your height (BMI %.1f–%.1f): %.1f–%.1f kg\n",
                HEALTHY_MIN_BMI, HEALTHY_MAX_BMI, healthyMinKg, healthyMaxKg));

        d.append("\n").append(section("How to interpret BMI"));
        d.append("• BMI is a general screening number and does not measure body fat, fitness, or overall health.\n");
        d.append("• It can be less representative for athletes, pregnancy, some older adults, and different body compositions.\n");
        d.append("• Consider using BMI alongside how you feel and other non-medical indicators (energy, sleep, activity consistency).\n");

        d.append("\n").append(section("General wellbeing tips (non-medical)"));
        d.append("• Nutrition: Focus on balanced meals (protein + high-fiber foods + minimally processed options).\n");
        d.append("• Movement: Aim for regular movement you can sustain (walking, cycling, resistance exercises).\n");
        d.append("• Sleep: Keep a consistent sleep schedule when possible.\n");
        d.append("• Consistency: Small, repeatable habits typically work better than aggressive short-term changes.\n");

        d.append("\n").append(section("Optional energy estimate"));
        if (ce != null) {
            d.append(String.format(Locale.US,
                    "• Estimated resting energy (BMR): %d kcal/day\n• Estimated daily needs (TDEE): %d kcal/day\n",
                    ce.bmr, ce.tdee));
            d.append("• These are generalized estimates and can vary significantly between individuals.\n");
        } else {
            d.append("• Add Age + Sex + Activity to show a general estimate.\n");
        }

        d.append("\n").append(section("When to seek professional advice"));
        d.append("• If you have medical concerns, a history of eating disorders, or major unexplained weight changes, consult a qualified healthcare professional.\n");

        return new Plan(summary.toString().trim(), d.toString().trim());
    }

    private CalorieEstimate estimateCaloriesOrNull(Integer age, boolean male, double heightM, double weightKg, int activityIdx) {
        if (age == null) return null;
        if (age < 10 || age > 120) return null;

        double heightCm = heightM * 100.0;
        double bmr = (10.0 * weightKg) + (6.25 * heightCm) - (5.0 * age) + (male ? 5.0 : -161.0);

        double multiplier;
        switch (activityIdx) {
            case 0: multiplier = 1.2; break;
            case 1: multiplier = 1.375; break;
            case 2: multiplier = 1.55; break;
            case 3: multiplier = 1.725; break;
            case 4: multiplier = 1.9; break;
            default: multiplier = 1.2; break;
        }

        int tdee = (int) Math.round(bmr * multiplier);
        int bmrInt = (int) Math.round(bmr);
        return new CalorieEstimate(bmrInt, tdee);
    }

    private BmiCategory classify(double bmi) {
        if (bmi < 18.5) return new BmiCategory("Below healthy range", "LOW", 0xFFDFF6FF, 0xFF0B1B3A);
        if (bmi < 25.0) return new BmiCategory("Healthy range", "OK", 0xFFD1FAE5, 0xFF0B1B3A);
        if (bmi < 30.0) return new BmiCategory("Above healthy range", "ELEV", 0xFFFEF3C7, 0xFF0B1B3A);
        return new BmiCategory("Higher BMI range", "HIGH", 0xFFFEE2E2, 0xFF0B1B3A);
    }


    private void sharePlan() {
        String body = String.valueOf(tvPlanBody.getText());
        if (TextUtils.isEmpty(body) || "—".contentEquals(body)) {
            Toast.makeText(this, "Calculate first.", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent i = new Intent(Intent.ACTION_SEND);
        i.setType("text/plain");
        i.putExtra(Intent.EXTRA_SUBJECT, "BMI Plan");
        i.putExtra(Intent.EXTRA_TEXT, body);
        startActivity(Intent.createChooser(i, "Share plan"));
    }

    // ---------- Helpers ----------
    private void attachImeDone(TextInputEditText et) {
        if (et == null) return;
        et.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_GO) {
                if (btnCalculate.isEnabled()) calculate();
                return true;
            }
            return false;
        });
    }

    private void addWatcher(TextInputEditText et, TextWatcher w) {
        if (et != null) et.addTextChangedListener(w);
    }

    private Double parseDouble(TextInputEditText et) {
        if (et == null || et.getText() == null) return null;
        String s = et.getText().toString().trim();
        if (TextUtils.isEmpty(s)) return null;
        try { return Double.parseDouble(s); } catch (Exception ignored) { return null; }
    }

    private Integer parseInt(TextInputEditText et) {
        if (et == null || et.getText() == null) return null;
        String s = et.getText().toString().trim();
        if (TextUtils.isEmpty(s)) return null;
        try { return Integer.parseInt(s); } catch (Exception ignored) { return null; }
    }

    private MaterialCardView card(int ml, int mt, int mr, int mb) {
        MaterialCardView c = new MaterialCardView(this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(dp(ml), dp(mt), dp(mr), dp(mb));
        c.setLayoutParams(lp);
        return c;
    }

    private TextView t(String s, int sp, boolean bold, int color) {
        TextView tv = new TextView(this);
        tv.setText(s);
        tv.setTextColor(color);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        tv.setTypeface(Typeface.create(Typeface.DEFAULT, bold ? Typeface.BOLD : Typeface.NORMAL));
        return tv;
    }

    private TextInputLayout til(String hint) {
        TextInputLayout til = new TextInputLayout(this);
        til.setHint(hint);
        til.setBoxBackgroundMode(TextInputLayout.BOX_BACKGROUND_OUTLINE);
        til.setBoxBackgroundColor(C_INPUT_BG);
        til.setBoxCornerRadii(dp(20), dp(20), dp(20), dp(20));
        til.setBoxStrokeColor(0xFF93C5FD);
        til.setBoxStrokeWidth(dp(1));
        til.setBoxStrokeWidthFocused(dp(2));
        til.setHintTextColor(android.content.res.ColorStateList.valueOf(C_PRIMARY));
        til.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return til;
    }

    private TextInputEditText etNumber(boolean decimal) {
        TextInputEditText et = new TextInputEditText(this);
        et.setBackground(null);
        et.setTextColor(C_TEXT);
        et.setHintTextColor(C_INPUT_HINT);
        et.setPadding(dp(14), dp(14), dp(14), dp(14));
        et.setInputType(decimal
                ? (android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL)
                : android.text.InputType.TYPE_CLASS_NUMBER);
        et.setImeOptions(EditorInfo.IME_ACTION_NEXT);
        return et;
    }

    private View spacer(int dp) {
        View v = new View(this);
        v.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(dp)));
        return v;
    }

    private View spacerH(int dp) {
        View v = new View(this);
        v.setLayoutParams(new LinearLayout.LayoutParams(
                dp(dp), ViewGroup.LayoutParams.MATCH_PARENT));
        return v;
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density);
    }

    private String section(String title) {
        return title + "\n" + "------------------------------\n";
    }

    private enum Goal { LOSE, GAIN, MAINTAIN }

    private static class BmiCategory {
        final String longLabel;
        final String chip;
        final int chipBg;
        final int chipText;

        BmiCategory(String longLabel, String chip, int chipBg, int chipText) {
            this.longLabel = longLabel;
            this.chip = chip;
            this.chipBg = chipBg;
            this.chipText = chipText;
        }
    }

    private static class Plan {
        final String summary;
        final String details;

        Plan(String summary, String details) {
            this.summary = summary;
            this.details = details;
        }
    }

    private static class CalorieEstimate {
        final int bmr;
        final int tdee;

        CalorieEstimate(int bmr, int tdee) {
            this.bmr = bmr;
            this.tdee = tdee;
        }
    }


    private static class SimpleWatcher implements TextWatcher {
        private final Runnable r;
        SimpleWatcher(Runnable r) { this.r = r; }
        @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
        @Override public void onTextChanged(CharSequence s, int start, int before, int count) { }
        @Override public void afterTextChanged(Editable s) { if (r != null) r.run(); }
    }

    public static class BmiComparisonChartView extends View {

        private static final double MIN_BMI = 10.0;
        private static final double MAX_BMI = 40.0;
        private static final double HEALTHY_MIN = 18.5;
        private static final double HEALTHY_MAX = 24.9;

        private double userBmi = Double.NaN;
        private double refBmi = Double.NaN;

        private final Paint pBg = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint pTrack = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint pBand = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint pTick = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint pUser = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint pRef = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint pText = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint pLabel = new Paint(Paint.ANTI_ALIAS_FLAG);

        private final RectF r = new RectF();

        public BmiComparisonChartView(Context context) {
            super(context);
            init();
        }

        private void init() {
            pBg.setColor(0xFFF8FAFC);
            pTrack.setColor(0xFFE2E8F0);
            pBand.setColor(0xFFD1FAE5);
            pTick.setColor(0xFFCBD5E1);

            pUser.setColor(0xFF2563EB);
            pRef.setColor(0xFF64748B);

            pText.setColor(0xFF64748B);
            pText.setTextSize(sp(12));

            pLabel.setColor(0xFF0B1B3A);
            pLabel.setTextSize(sp(12));
            pLabel.setFakeBoldText(true);
        }

        public void setValues(double userBmi, double referenceBmi) {
            this.userBmi = userBmi;
            this.refBmi = referenceBmi;
            invalidate();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);

            float w = getWidth();
            float h = getHeight();

            float pad = dp(12);
            float left = pad;
            float right = w - pad;

            float trackH = dp(16);
            float trackTop = h * 0.42f;
            float trackBottom = trackTop + trackH;

            float radius = dp(12);

            r.set(0, 0, w, h);
            canvas.drawRoundRect(r, dp(14), dp(14), pBg);

            r.set(left, trackTop, right, trackBottom);
            canvas.drawRoundRect(r, radius, radius, pTrack);

            float xHMin = mapToX(HEALTHY_MIN, left, right);
            float xHMax = mapToX(HEALTHY_MAX, left, right);
            r.set(xHMin, trackTop, xHMax, trackBottom);
            canvas.drawRoundRect(r, radius, radius, pBand);

            drawTick(canvas, 10, left, right, trackTop, trackBottom);
            drawTick(canvas, 20, left, right, trackTop, trackBottom);
            drawTick(canvas, 30, left, right, trackTop, trackBottom);
            drawTick(canvas, 40, left, right, trackTop, trackBottom);

            float labelY = trackTop - dp(10);
            canvas.drawText("10", mapToX(10, left, right) - dp(8), labelY, pText);
            canvas.drawText("20", mapToX(20, left, right) - dp(8), labelY, pText);
            canvas.drawText("30", mapToX(30, left, right) - dp(8), labelY, pText);
            canvas.drawText("40", mapToX(40, left, right) - dp(8), labelY, pText);

            float cy = (trackTop + trackBottom) / 2f;
            float markerR = dp(7);

            float textBaseY = trackBottom + dp(28);

            if (!Double.isNaN(refBmi)) {
                float x = mapToX(refBmi, left, right);
                canvas.drawCircle(x, cy, markerR, pRef);
                canvas.drawText("Avg", clampXForText(x, left, right, "Avg", pText), textBaseY, pText);
            }

            if (!Double.isNaN(userBmi)) {
                float x = mapToX(userBmi, left, right);
                canvas.drawCircle(x, cy, markerR, pUser);
                canvas.drawText("You", clampXForText(x, left, right, "You", pLabel), textBaseY + dp(18), pLabel);
            }

            String band = "Healthy";
            float bw = pText.measureText(band);
            float bx = (xHMin + xHMax) / 2f - bw / 2f;
            canvas.drawText(band, bx, trackBottom + dp(54), pText);
        }

        private void drawTick(Canvas canvas, double bmi, float left, float right, float trackTop, float trackBottom) {
            float x = mapToX(bmi, left, right);
            canvas.drawLine(x, trackTop - dp(7), x, trackBottom + dp(7), pTick);
        }

        private float mapToX(double bmi, float left, float right) {
            double clamped = Math.max(MIN_BMI, Math.min(MAX_BMI, bmi));
            double t = (clamped - MIN_BMI) / (MAX_BMI - MIN_BMI);
            return (float) (left + (right - left) * t);
        }

        private float clampXForText(float x, float left, float right, String text, Paint p) {
            float textW = p.measureText(text);
            float min = left;
            float max = right - textW;
            return Math.max(min, Math.min(max, x - (textW / 2f)));
        }

        private float dp(float v) {
            return v * getResources().getDisplayMetrics().density;
        }

        private float sp(float v) {
            return v * getResources().getDisplayMetrics().scaledDensity;
        }
    }
}
