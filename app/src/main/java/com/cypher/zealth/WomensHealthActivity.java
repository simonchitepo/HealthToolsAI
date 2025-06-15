package com.cypher.zealth;

import android.Manifest;
import android.app.TimePickerDialog;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.MaterialDatePicker;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class WomensHealthActivity extends AppCompatActivity implements PeriodHistoryAdapter.Listener {

    
    private static final int CYCLE_MIN = 20;
    private static final int CYCLE_MAX = 45;
    private static final int PERIOD_MIN = 2;
    private static final int PERIOD_MAX = 10;
    private static final int LUTEAL_MIN = 10;
    private static final int LUTEAL_MAX = 18;

    private final SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    private WomensStorage storage;
    private PeriodHistoryAdapter adapter;
    private TextInputLayout tilCycleLen, tilPeriodLen, tilLutealLen;
    private TextInputEditText etCycleLen, etPeriodLen, etLutealLen;
    private TextView tvNextPeriod, tvOvulation, tvFertile;
    private TextView tvInsightAvg, tvInsightVar, tvInsightCount;
    private TextView tvTodayHeader, tvTodayPhase, tvTodayTips;
    private TextView tvMoodValue, tvEnergyValue;
    private SeekBar sliderMood, sliderEnergy;
    private TextInputEditText etDailyNote;
    private ChipGroup chipGroupFlow, chipGroupSymptoms;
    private SwitchMaterial swCycleNotifs, swGuideNotifs, swEncourageNotifs;
    private View btnPickNotifTime;
    private TextView tvNotifTime;
    private MaterialButton btnPickLastPeriod; 
    private View btnSaveSettings;
    private View btnLogPeriodToday;
    private View btnSaveDailyLog;
    private View btnClearHistory;

    private long lastPeriodMs = -1L;

    private ActivityResultLauncher<String> notifPermissionLauncher;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_womens_health);

        MaterialToolbar tb = findViewById(R.id.toolbar);
        if (tb != null) tb.setNavigationOnClickListener(v -> finish());

        storage = new WomensStorage(this);

       
        notifPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (!isGranted) {

                        storage.setNotifCycleEnabled(false);
                        storage.setNotifGuideEnabled(false);
                        storage.setNotifEncourageEnabled(false);

                        if (swCycleNotifs != null) swCycleNotifs.setChecked(false);
                        if (swGuideNotifs != null) swGuideNotifs.setChecked(false);
                        if (swEncourageNotifs != null) swEncourageNotifs.setChecked(false);

                        WomensNotificationScheduler.cancelDaily(this);
                        Toast.makeText(this, "Notifications permission denied. Reminders are off.", Toast.LENGTH_LONG).show();
                    } else {
                      
                        rescheduleNotificationsIfNeeded();
                    }
                }
        );

     
        btnPickLastPeriod = findViewById(R.id.btnPickLastPeriod);
        btnSaveSettings = findViewById(R.id.btnSaveSettings);
        btnLogPeriodToday = findViewById(R.id.btnLogPeriodToday);
        btnSaveDailyLog = findViewById(R.id.btnSaveDailyLog);
        btnClearHistory = findViewById(R.id.btnClearHistory);

        tilCycleLen = findViewById(R.id.tilCycleLength);
        tilPeriodLen = findViewById(R.id.tilPeriodLength);
        tilLutealLen = findViewById(R.id.tilLutealLength);

        etCycleLen = findViewById(R.id.etCycleLength);
        etPeriodLen = findViewById(R.id.etPeriodLength);
        etLutealLen = findViewById(R.id.etLutealLength);


        tvNextPeriod = findViewById(R.id.tvNextPeriod);
        tvOvulation = findViewById(R.id.tvOvulation);
        tvFertile = findViewById(R.id.tvFertileWindow);


        tvInsightAvg = findViewById(R.id.tvInsightAvg);
        tvInsightVar = findViewById(R.id.tvInsightVar);
        tvInsightCount = findViewById(R.id.tvInsightCount);

        tvTodayHeader = findViewById(R.id.tvTodayHeader);
        tvTodayPhase = findViewById(R.id.tvTodayPhase);
        tvTodayTips = findViewById(R.id.tvTodayTips);

        chipGroupFlow = findViewById(R.id.chipGroupFlow);
        chipGroupSymptoms = findViewById(R.id.chipGroupSymptoms);


        if (chipGroupFlow != null) chipGroupFlow.setSingleSelection(true);
        if (chipGroupSymptoms != null) chipGroupSymptoms.setSingleSelection(false);

        tvMoodValue = findViewById(R.id.tvMoodValue);
        tvEnergyValue = findViewById(R.id.tvEnergyValue);
        sliderMood = findViewById(R.id.sliderMood);
        sliderEnergy = findViewById(R.id.sliderEnergy);
        etDailyNote = findViewById(R.id.etDailyNote);

        swCycleNotifs = findViewById(R.id.swCycleNotifs);
        swGuideNotifs = findViewById(R.id.swGuideNotifs);
        swEncourageNotifs = findViewById(R.id.swEncourageNotifs);
        btnPickNotifTime = findViewById(R.id.btnPickNotifTime);
        tvNotifTime = findViewById(R.id.tvNotifTime);

        RecyclerView rv = findViewById(R.id.recyclerHistory);
        if (rv != null) {
            rv.setLayoutManager(new LinearLayoutManager(this));
            adapter = new PeriodHistoryAdapter(storage.loadHistory(), this);
            rv.setAdapter(adapter);
        } else {
            
            adapter = new PeriodHistoryAdapter(new ArrayList<>(), this);
        }

        lastPeriodMs = storage.getLastPeriodMs();
        if (etCycleLen != null) etCycleLen.setText(String.valueOf(storage.getCycleLen()));
        if (etPeriodLen != null) etPeriodLen.setText(String.valueOf(storage.getPeriodLen()));
        if (etLutealLen != null) etLutealLen.setText(String.valueOf(storage.getLutealLen()));

        if (swCycleNotifs != null) swCycleNotifs.setChecked(storage.getNotifCycleEnabled());
        if (swGuideNotifs != null) swGuideNotifs.setChecked(storage.getNotifGuideEnabled());
        if (swEncourageNotifs != null) swEncourageNotifs.setChecked(storage.getNotifEncourageEnabled());
        updateNotifTimeLabel(storage.getNotifHour(), storage.getNotifMinute());

        if (sliderMood != null) sliderMood.setMax(4);
        if (sliderEnergy != null) sliderEnergy.setMax(4);

        updateLastPeriodButton();
        renderPredictions();
        renderInsights();
        loadTodayLogIntoUi();
        renderTodayPanel();
        refreshHistory();

        if (sliderMood != null) {
            sliderMood.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (tvMoodValue != null) tvMoodValue.setText("Mood: " + (progress + 1) + " / 5");
                }
                @Override public void onStartTrackingTouch(SeekBar seekBar) {}
                @Override public void onStopTrackingTouch(SeekBar seekBar) {}
            });
        }

        if (sliderEnergy != null) {
            sliderEnergy.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (tvEnergyValue != null) tvEnergyValue.setText("Energy: " + (progress + 1) + " / 5");
                }
                @Override public void onStartTrackingTouch(SeekBar seekBar) {}
                @Override public void onStopTrackingTouch(SeekBar seekBar) {}
            });
        }

        if (btnPickLastPeriod != null) btnPickLastPeriod.setOnClickListener(v -> pickDate());
        if (btnSaveSettings != null) btnSaveSettings.setOnClickListener(v -> saveSettings());

        if (btnLogPeriodToday != null) {
            btnLogPeriodToday.setOnClickListener(v -> {
                long today = startOfDay(System.currentTimeMillis());
                lastPeriodMs = today;

                Integer cycle = parseInt(etCycleLen);
                Integer period = parseInt(etPeriodLen);
                Integer luteal = parseInt(etLutealLen);

                if (cycle == null) cycle = storage.getCycleLen();
                if (period == null) period = storage.getPeriodLen();
                if (luteal == null) luteal = storage.getLutealLen();

                storage.saveSettings(lastPeriodMs, cycle, period, luteal);
                storage.upsertHistoryStart(today);

                updateLastPeriodButton();
                renderPredictions();
                renderInsights();
                renderTodayPanel();
                refreshHistory();
                rescheduleNotificationsIfNeeded();

                Toast.makeText(this, "Saved: period start date.", Toast.LENGTH_SHORT).show();
            });
        }

        if (btnSaveDailyLog != null) {
            btnSaveDailyLog.setOnClickListener(v -> {
                saveDailyLog();
                renderTodayPanel();         
                rescheduleNotificationsIfNeeded();
            });
        }

        if (btnClearHistory != null) btnClearHistory.setOnClickListener(v -> clearHistory());

        if (swCycleNotifs != null) {
            swCycleNotifs.setOnCheckedChangeListener((buttonView, isChecked) -> {
                storage.setNotifCycleEnabled(isChecked);
                rescheduleNotificationsIfNeeded();
            });
        }

        if (swGuideNotifs != null) {
            swGuideNotifs.setOnCheckedChangeListener((buttonView, isChecked) -> {
                storage.setNotifGuideEnabled(isChecked);
                rescheduleNotificationsIfNeeded();
            });
        }

        if (swEncourageNotifs != null) {
            swEncourageNotifs.setOnCheckedChangeListener((buttonView, isChecked) -> {
                storage.setNotifEncourageEnabled(isChecked);
                rescheduleNotificationsIfNeeded();
            });
        }

        if (btnPickNotifTime != null) btnPickNotifTime.setOnClickListener(v -> pickNotifTime());
    }

    @Override
    protected void onResume() {
        super.onResume();
        lastPeriodMs = storage.getLastPeriodMs();
        updateLastPeriodButton();
        renderPredictions();
        renderInsights();
        loadTodayLogIntoUi();
        renderTodayPanel();
        refreshHistory();
    }

    private void pickNotifTime() {
        int h = storage.getNotifHour();
        int m = storage.getNotifMinute();

        boolean is24h = DateFormat.is24HourFormat(this);

        TimePickerDialog tp = new TimePickerDialog(
                this,
                (view, hourOfDay, minute) -> {
                    storage.setNotifTime(hourOfDay, minute);
                    updateNotifTimeLabel(hourOfDay, minute);
                    rescheduleNotificationsIfNeeded();
                },
                h, m, is24h
        );
        tp.show();
    }

    private void updateNotifTimeLabel(int hour, int minute) {
        if (tvNotifTime == null) return;
        String mm = (minute < 10) ? ("0" + minute) : String.valueOf(minute);
        String hh = (hour < 10) ? ("0" + hour) : String.valueOf(hour);
        tvNotifTime.setText(hh + ":" + mm);
    }

    private void rescheduleNotificationsIfNeeded() {
        boolean any = storage.getNotifCycleEnabled() || storage.getNotifGuideEnabled() || storage.getNotifEncourageEnabled();
        if (!any) {
            WomensNotificationScheduler.cancelDaily(this);
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!hasPostNotificationsPermission()) {
                notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
                return;
            }
        }

        WomensNotificationScheduler.scheduleDaily(this);
    }

    private boolean hasPostNotificationsPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void saveSettings() {
        clearInputErrors();

        Integer cycle = parseInt(etCycleLen);
        Integer period = parseInt(etPeriodLen);
        Integer luteal = parseInt(etLutealLen);

        boolean ok = true;

        if (lastPeriodMs <= 0) {
            Toast.makeText(this, "Please set your last period start date.", Toast.LENGTH_SHORT).show();
            ok = false;
        }
        if (cycle == null || cycle < CYCLE_MIN || cycle > CYCLE_MAX) {
            if (tilCycleLen != null) tilCycleLen.setError("Cycle length should be " + CYCLE_MIN + "–" + CYCLE_MAX + " days.");
            ok = false;
        }
        if (period == null || period < PERIOD_MIN || period > PERIOD_MAX) {
            if (tilPeriodLen != null) tilPeriodLen.setError("Period length should be " + PERIOD_MIN + "–" + PERIOD_MAX + " days.");
            ok = false;
        }
        if (luteal == null || luteal < LUTEAL_MIN || luteal > LUTEAL_MAX) {
            if (tilLutealLen != null) tilLutealLen.setError("Luteal phase should be " + LUTEAL_MIN + "–" + LUTEAL_MAX + " days.");
            ok = false;
        }

        if (!ok) return;

        storage.saveSettings(lastPeriodMs, cycle, period, luteal);
        Toast.makeText(this, "Settings saved.", Toast.LENGTH_SHORT).show();

        renderPredictions();
        renderInsights();
        renderTodayPanel();
        rescheduleNotificationsIfNeeded();
    }

    private void clearInputErrors() {
        if (tilCycleLen != null) tilCycleLen.setError(null);
        if (tilPeriodLen != null) tilPeriodLen.setError(null);
        if (tilLutealLen != null) tilLutealLen.setError(null);
    }

    private void saveDailyLog() {
        long today = startOfDay(System.currentTimeMillis());

        WomensDailyLogEntry log = WomensDailyLogEntry.of(today);

        log.flow = getSelectedChipText(chipGroupFlow);
        log.symptoms = getSelectedChipTexts(chipGroupSymptoms);

        log.mood = (sliderMood != null ? sliderMood.getProgress() : 0) + 1;
        log.energy = (sliderEnergy != null ? sliderEnergy.getProgress() : 0) + 1;

        if (etDailyNote != null && etDailyNote.getText() != null) {
            log.note = etDailyNote.getText().toString().trim();
        }

        storage.upsertDailyLog(log);
        Toast.makeText(this, "Saved for your reference.", Toast.LENGTH_SHORT).show();
    }

 
    private void loadTodayLogIntoUi() {
        long today = startOfDay(System.currentTimeMillis());
        WomensDailyLogEntry log = storage.getDailyLogForDay(today);
        if (log == null) return;

        if (chipGroupFlow != null && log.flow != null) {
            setChipCheckedByText(chipGroupFlow, log.flow, true);
        }

        if (chipGroupSymptoms != null && log.symptoms != null) {
            for (String s : log.symptoms) {
                setChipCheckedByText(chipGroupSymptoms, s, false);
            }
        }

        if (sliderMood != null) sliderMood.setProgress(clamp(log.mood - 1, 0, 4));
        if (sliderEnergy != null) sliderEnergy.setProgress(clamp(log.energy - 1, 0, 4));

        if (tvMoodValue != null && sliderMood != null) {
            tvMoodValue.setText("Mood: " + (sliderMood.getProgress() + 1) + " / 5");
        }
        if (tvEnergyValue != null && sliderEnergy != null) {
            tvEnergyValue.setText("Energy: " + (sliderEnergy.getProgress() + 1) + " / 5");
        }

        if (etDailyNote != null) {
            etDailyNote.setText(log.note != null ? log.note : "");
        }
    }

    private int clamp(int v, int lo, int hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    private void setChipCheckedByText(ChipGroup group, String text, boolean clearOthersFirst) {
        if (group == null || text == null) return;

        if (clearOthersFirst) {
            group.clearCheck();
         
            for (int i = 0; i < group.getChildCount(); i++) {
                View v = group.getChildAt(i);
                if (v instanceof Chip) ((Chip) v).setChecked(false);
            }
        }

        for (int i = 0; i < group.getChildCount(); i++) {
            View v = group.getChildAt(i);
            if (v instanceof Chip) {
                Chip c = (Chip) v;
                if (c.getText() != null && text.equalsIgnoreCase(c.getText().toString().trim())) {
                    c.setChecked(true);
                    return;
                }
            }
        }
    }

    private void pickDate() {

        long todayUtc = MaterialDatePicker.todayInUtcMilliseconds();

        CalendarConstraints constraints = new CalendarConstraints.Builder()
                .setEnd(todayUtc) 
                .build();

        
        long selectionUtc = (lastPeriodMs > 0)
                ? localStartOfDayToUtcMidnight(lastPeriodMs)
                : todayUtc;

        MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Select last period start")
                .setSelection(selectionUtc)
                .setCalendarConstraints(constraints)
                .setTheme(R.style.ThemeOverlay_Zealth_MaterialDatePicker) 
                .build();

        picker.addOnPositiveButtonClickListener(utcMidnightMs -> {
            
            long chosenLocal = utcMidnightToLocalStartOfDay(utcMidnightMs);
            lastPeriodMs = chosenLocal;

            Integer cycle = parseInt(etCycleLen);
            Integer period = parseInt(etPeriodLen);
            Integer luteal = parseInt(etLutealLen);

            if (cycle == null) cycle = storage.getCycleLen();
            if (period == null) period = storage.getPeriodLen();
            if (luteal == null) luteal = storage.getLutealLen();

            storage.saveSettings(lastPeriodMs, cycle, period, luteal);
            storage.upsertHistoryStart(lastPeriodMs);

            updateLastPeriodButton();
            renderPredictions();
            renderInsights();
            renderTodayPanel();
            refreshHistory();
            rescheduleNotificationsIfNeeded();
        });

        picker.show(getSupportFragmentManager(), "last_period_picker");
    }

    private long utcMidnightToLocalStartOfDay(long utcMidnightMs) {
        Calendar utc = Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"));
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

        Calendar utc = Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"));
        utc.set(Calendar.YEAR, local.get(Calendar.YEAR));
        utc.set(Calendar.MONTH, local.get(Calendar.MONTH));
        utc.set(Calendar.DAY_OF_MONTH, local.get(Calendar.DAY_OF_MONTH));
        utc.set(Calendar.HOUR_OF_DAY, 0);
        utc.set(Calendar.MINUTE, 0);
        utc.set(Calendar.SECOND, 0);
        utc.set(Calendar.MILLISECOND, 0);
        return utc.getTimeInMillis();
    }


    private void updateLastPeriodButton() {
        if (btnPickLastPeriod == null) return;

        if (lastPeriodMs > 0) {
            btnPickLastPeriod.setText("Last period start: " + df.format(new Date(lastPeriodMs)));
        } else {
            btnPickLastPeriod.setText("Last period start: Not set");
        }
    }

    private void renderPredictions() {
        if (tvNextPeriod == null) return;

    
        if (tvOvulation != null) tvOvulation.setText("Fertility estimates: Off");
        if (tvFertile != null) tvFertile.setText("Fertility estimates: Off");

        if (lastPeriodMs <= 0) {
            tvNextPeriod.setText("Next period (estimate): —");
            return;
        }

        int cycleLen = storage.getCycleLen();
        int periodLen = storage.getPeriodLen();

        long nextStart = addDays(lastPeriodMs, cycleLen);
        long nextEnd = addDays(nextStart, Math.max(periodLen - 1, 0));

        tvNextPeriod.setText(
                "Next period (estimate): " + df.format(new Date(nextStart)) +
                        " to " + df.format(new Date(nextEnd))
        );
    }


    private void renderInsights() {
        if (tvInsightCount == null || tvInsightAvg == null || tvInsightVar == null) return;

        int count = storage.getLoggedCyclesCount();
        tvInsightCount.setText("Logged cycles: " + count);

        double avg = storage.getAverageCycleDays();
        if (avg > 0) {
            tvInsightAvg.setText("Average cycle: " + String.format(Locale.US, "%.1f", avg) + " days");
        } else {
            tvInsightAvg.setText("Average cycle: —");
        }

        double sd = storage.getCycleStdDevDays();
        if (sd > 0) {
            String label = (sd < 2.0) ? "Stable" : (sd < 4.0) ? "Moderate" : "Variable";
            tvInsightVar.setText("Variability: " + label + " (±" + String.format(Locale.US, "%.1f", sd) + " days)");
        } else {
            tvInsightVar.setText("Variability: —");
        }
    }

    private void renderTodayPanel() {
        if (tvTodayHeader == null || tvTodayPhase == null || tvTodayTips == null) return;

        if (lastPeriodMs <= 0) {
            tvTodayHeader.setText("Today");
            tvTodayPhase.setText("Set your last period start date to see today’s estimate.");
            tvTodayTips.setText("Tip: Log optional notes for personal reference");
            return;
        }

        long today = startOfDay(System.currentTimeMillis());
        int cycleLen = storage.getCycleLen();
        int periodLen = storage.getPeriodLen();
        int lutealLen = storage.getLutealLen();

        WomensCycleModel.CycleInfo info = WomensCycleModel.getCycleInfo(
                today, lastPeriodMs, cycleLen, periodLen, lutealLen
        );

        tvTodayHeader.setText("Today • Cycle day " + info.cycleDay + " of ~" + cycleLen);
        tvTodayPhase.setText(info.phaseTitle);

        WomensDailyLogEntry todaysLog = storage.getDailyLogForDay(today);
        List<String> symptoms = (todaysLog != null && todaysLog.symptoms != null) ? todaysLog.symptoms : new ArrayList<>();
        String flow = (todaysLog != null) ? todaysLog.flow : null;

        String tips = WomensGuidance.buildGeneralWellnessTips(info, symptoms, flow);
        tvTodayTips.setText(tips);

    }

    private void refreshHistory() {
        if (adapter != null) adapter.setItems(storage.loadHistory());
    }

    private void clearHistory() {
        new AlertDialog.Builder(this)
                .setTitle("Clear history?")
                .setMessage("This will remove saved period history from this device.")
                .setNegativeButton("Cancel", (d, w) -> d.dismiss())
                .setPositiveButton("Clear", (d, w) -> {
                    storage.clearHistory();
                    refreshHistory();
                    renderInsights();
                    renderTodayPanel();
                    rescheduleNotificationsIfNeeded();
                    Toast.makeText(this, "History cleared.", Toast.LENGTH_SHORT).show();
                })
                .show();
    }

    private Integer parseInt(TextInputEditText et) {
        try {
            if (et == null || et.getText() == null) return null;
            String s = et.getText().toString().trim();
            if (s.isEmpty()) return null;
            return Integer.parseInt(s);
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

    private long addDays(long ms, int days) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(ms);
        c.add(Calendar.DAY_OF_YEAR, days);
        return startOfDay(c.getTimeInMillis());
    }

    private String getSelectedChipText(ChipGroup group) {
        if (group == null) return null;
        int id = group.getCheckedChipId();
        if (id == View.NO_ID) return null;
        Chip chip = group.findViewById(id);
        if (chip == null || chip.getText() == null) return null;
        return chip.getText().toString();
    }

    private List<String> getSelectedChipTexts(ChipGroup group) {
        List<String> out = new ArrayList<>();
        if (group == null) return out;

        for (int i = 0; i < group.getChildCount(); i++) {
            View v = group.getChildAt(i);
            if (v instanceof Chip) {
                Chip c = (Chip) v;
                if (c.isChecked() && c.getText() != null) out.add(c.getText().toString());
            }
        }
        return out;
    }

   
    @Override
    public void onEndPeriodClicked(WomensPeriodEntry entry) {
        if (entry == null) return;
        if (entry.endDateMs > 0) return;

        long today = startOfDay(System.currentTimeMillis());
        if (today < entry.startDateMs) {
            Toast.makeText(this, "End date cannot be before start date.", Toast.LENGTH_SHORT).show();
            return;
        }

        storage.markPeriodEnded(entry.startDateMs, today);
        refreshHistory();
        Toast.makeText(this, "Marked period as ended.", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDeleteClicked(WomensPeriodEntry entry) {
        if (entry == null) return;

        new AlertDialog.Builder(this)
                .setTitle("Delete entry?")
                .setMessage("This will remove this period entry from history.")
                .setNegativeButton("Cancel", (d, w) -> d.dismiss())
                .setPositiveButton("Delete", (d, w) -> {
                    storage.deleteHistory(entry.startDateMs);
                    refreshHistory();
                    renderInsights();
                    renderTodayPanel();
                    rescheduleNotificationsIfNeeded();
                    Toast.makeText(this, "Entry deleted.", Toast.LENGTH_SHORT).show();
                })
                .show();
    }
}
