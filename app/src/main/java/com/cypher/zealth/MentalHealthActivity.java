package com.cypher.zealth;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.slider.Slider;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;

import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MentalHealthActivity extends AppCompatActivity {

   
    public static final String PREF_UI = "mental_ui_pref";
    public static final String KEY_HIDE_PREVIEWS = "hide_previews";
    public static final String KEY_APP_LOCK = "app_lock";
    public static final String KEY_REMINDERS = "mood_reminders";
    public static final String KEY_REMINDER_HOUR = "reminder_hour";
    public static final String KEY_REMINDER_MIN = "reminder_min";
    private MentalStorage storage;
    private MentalHistoryAdapter historyAdapter;
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private View root;
    private NestedScrollView scroll;
    private Slider sliderMood;
    private TextView tvMoodValue;
    private TextInputEditText etMoodNote;
    private ChipGroup chipGroupTags;
    private final TextView[] moodEmojis = new TextView[5];

    private TextInputEditText etJournal;
    private TextView tvPrompt;
    private MaterialButton btnNewPrompt;

    private RecyclerView recyclerHistory;
    private Chip chipFilterAll, chipFilterMood, chipFilterJournal;

    private View emptyState;
    private MaterialButton btnEmptyGoMood, btnEmptyGoJournal;

    private TextView tvAvg7, tvStreak;
    private SparklineView sparkline;

    private RecyclerView recyclerWeek;
    private WeekStripAdapter weekAdapter;
    private ChipGroup chipTopTags;

    private SwitchMaterial swHidePreviews;
    private SwitchMaterial swAppLock;

    private SwitchMaterial swMoodReminders;
    private MaterialButton btnReminderTime;
    private TextView tvReminderHint;

    private List<MentalEntry> allItems = new ArrayList<>();
    private MentalEntry lastDeleted = null;

    private final SimpleDateFormat dfDay = new SimpleDateFormat("EEE, dd MMM", Locale.getDefault());

    private ActivityResultLauncher<Intent> exportLauncher;
    private ActivityResultLauncher<Intent> importLauncher;

    private final EmergencyResources emergencyResources = new EmergencyResources();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mental_health);

        root = findViewById(R.id.root);
        scroll = findViewById(R.id.scroll);

        MaterialToolbar tb = findViewById(R.id.toolbar);
        if (tb != null) tb.setNavigationOnClickListener(v -> finish());

        storage = new MentalStorage(this);

        bindViews();
        setupExportImportPdf();
        setupMoodSlider();
        setupEmojiRow();
        setupButtons();
        setupFilters();
        setupHistoryList();
        setupSwipeToDelete();
        setupReminders();
        setupPrivacy();

        if (tvPrompt != null) tvPrompt.setText(getPromptForToday());

        io.execute(() -> {
            try {
                emergencyResources.loadFromAssetsPdf(this, "zealth_emergency_resources.txt");
            } catch (Exception ignored) {
            }
        });

        refreshFromStorage();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (getUiPrefs().getBoolean(KEY_APP_LOCK, false)) {
            requestBiometricGateIfAvailable();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        io.shutdownNow();
    }

    private void bindViews() {
       
        sliderMood = findViewById(R.id.sliderMood);
        tvMoodValue = findViewById(R.id.tvMoodValue);
        etMoodNote = findViewById(R.id.etMoodNote);
        chipGroupTags = findViewById(R.id.chipGroupTags);

        moodEmojis[0] = findViewById(R.id.emoji1);
        moodEmojis[1] = findViewById(R.id.emoji2);
        moodEmojis[2] = findViewById(R.id.emoji3);
        moodEmojis[3] = findViewById(R.id.emoji4);
        moodEmojis[4] = findViewById(R.id.emoji5);


        etJournal = findViewById(R.id.etJournal);
        tvPrompt = findViewById(R.id.tvPrompt);
        btnNewPrompt = findViewById(R.id.btnNewPrompt);

        tvAvg7 = findViewById(R.id.tvAvg7);
        tvStreak = findViewById(R.id.tvStreak);
        sparkline = findViewById(R.id.sparkline);
        recyclerWeek = findViewById(R.id.recyclerWeek);
        chipTopTags = findViewById(R.id.chipTopTags);

        recyclerHistory = findViewById(R.id.recyclerHistory);
        chipFilterAll = findViewById(R.id.chipFilterAll);
        chipFilterMood = findViewById(R.id.chipFilterMood);
        chipFilterJournal = findViewById(R.id.chipFilterJournal);

        emptyState = findViewById(R.id.emptyState);
        btnEmptyGoMood = findViewById(R.id.btnEmptyGoMood);
        btnEmptyGoJournal = findViewById(R.id.btnEmptyGoJournal);

        swHidePreviews = findViewById(R.id.swHidePreviews);
        swAppLock = findViewById(R.id.swAppLock);

        swMoodReminders = findViewById(R.id.swMoodReminders);
        btnReminderTime = findViewById(R.id.btnReminderTime);
        tvReminderHint = findViewById(R.id.tvReminderHint);
    }

    private SharedPreferences getUiPrefs() {
        return getSharedPreferences(PREF_UI, MODE_PRIVATE);
    }

    private void setupExportImportPdf() {
        exportLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() != RESULT_OK || result.getData() == null) return;
                    Uri uri = result.getData().getData();
                    if (uri == null) return;

                    io.execute(() -> {
                        try (OutputStream os = getContentResolver().openOutputStream(uri)) {
                            List<MentalEntry> data = storage.load();
                            PdfBackupUtil.exportToPdf(data, os);
                            runOnUiThread(() -> showSnack("Exported PDF", "Open", v -> openUri(uri)));
                        } catch (Exception e) {
                            runOnUiThread(() -> showSnack("Export failed", null, null));
                        }
                    });
                }
        );

        importLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() != RESULT_OK || result.getData() == null) return;
                    Uri uri = result.getData().getData();
                    if (uri == null) return;

                    io.execute(() -> {
                        try (InputStream is = getContentResolver().openInputStream(uri)) {
                            List<MentalEntry> imported = PdfBackupUtil.importFromPdf(is);
                            replaceAllEntries(imported);
                            runOnUiThread(() -> {
                                refreshFromStorage();
                                showSnack("Imported " + imported.size() + " entries", null, null);
                            });
                        } catch (Exception e) {
                            runOnUiThread(() -> showSnack("Import failed (invalid Zealth PDF)", null, null));
                        }
                    });
                }
        );
    }

    private void replaceAllEntries(List<MentalEntry> imported) {
        if (imported == null) imported = new ArrayList<>();
        try {
            Method m = storage.getClass().getMethod("saveAll", List.class);
            m.invoke(storage, imported);
            return;
        } catch (Exception ignored) {
        }

        try {
            Method m = storage.getClass().getMethod("replaceAll", List.class);
            m.invoke(storage, imported);
            return;
        } catch (Exception ignored) {
        }

        try {
            storage.clear();
            for (MentalEntry e : imported) storage.add(e);
        } catch (Exception ignored) {
        }
    }

    private void setupMoodSlider() {
        if (sliderMood == null) return;

        sliderMood.setValueFrom(1f);
        sliderMood.setValueTo(5f);
        sliderMood.setStepSize(1f);
        sliderMood.setValue(3f);

        updateMoodLabel(3);
        highlightEmoji(3);

        sliderMood.addOnChangeListener((slider, value, fromUser) -> {
            int mood = Math.round(value);
            updateMoodLabel(mood);
            highlightEmoji(mood);
            if (fromUser && root != null) root.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
        });
    }

    private void setupEmojiRow() {
        for (int i = 0; i < moodEmojis.length; i++) {
            final int mood = i + 1;
            TextView tv = moodEmojis[i];
            if (tv == null) continue;
            tv.setOnClickListener(v -> {
                if (sliderMood != null) sliderMood.setValue(mood);
                updateMoodLabel(mood);
                highlightEmoji(mood);
                v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
            });
        }
    }

    private void setupButtons() {
        View btnEmergency = findViewById(R.id.btnEmergencyHelp);
        if (btnEmergency != null) btnEmergency.setOnClickListener(v -> showEmergencyBottomSheet());

        View btnSaveMood = findViewById(R.id.btnSaveMood);
        if (btnSaveMood != null) btnSaveMood.setOnClickListener(v -> saveMood());

        View btnSaveJournal = findViewById(R.id.btnSaveJournal);
        if (btnSaveJournal != null) btnSaveJournal.setOnClickListener(v -> saveJournal());

        View btnClear = findViewById(R.id.btnClearHistory);
        if (btnClear != null) btnClear.setOnClickListener(v -> showClearOptions());

        View btnExport = findViewById(R.id.btnExport);
        if (btnExport != null) btnExport.setOnClickListener(v -> exportPdf());

        View btnImport = findViewById(R.id.btnImport);
        if (btnImport != null) btnImport.setOnClickListener(v -> importPdf());

        if (btnNewPrompt != null) {
            btnNewPrompt.setOnClickListener(v -> {
                if (tvPrompt != null) tvPrompt.setText(getPromptForTodayWithOffset(System.currentTimeMillis()));
                v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
            });
        }

        if (btnEmptyGoMood != null) btnEmptyGoMood.setOnClickListener(v -> scrollTo(R.id.cardMood));
        if (btnEmptyGoJournal != null) btnEmptyGoJournal.setOnClickListener(v -> scrollTo(R.id.cardJournal));
    }

    private void setupFilters() {
        if (chipFilterAll != null) chipFilterAll.setChecked(true);

        View.OnClickListener filterListener = v -> applyFilterAndRender();
        if (chipFilterAll != null) chipFilterAll.setOnClickListener(filterListener);
        if (chipFilterMood != null) chipFilterMood.setOnClickListener(filterListener);
        if (chipFilterJournal != null) chipFilterJournal.setOnClickListener(filterListener);
    }

    private void setupHistoryList() {
        historyAdapter = new MentalHistoryAdapter(new ArrayList<>(),
                () -> getUiPrefs().getBoolean(KEY_HIDE_PREVIEWS, false));

        if (recyclerHistory != null) {
            recyclerHistory.setLayoutManager(new LinearLayoutManager(this));
            recyclerHistory.setAdapter(historyAdapter);
            recyclerHistory.setItemAnimator(null);
        }

        if (recyclerWeek != null) {
            recyclerWeek.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
            weekAdapter = new WeekStripAdapter();
            recyclerWeek.setAdapter(weekAdapter);
            recyclerWeek.setItemAnimator(null);
        }
    }

    private void setupSwipeToDelete() {
        if (recyclerHistory == null) return;

        ItemTouchHelper.SimpleCallback cb =
                new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
                    @Override
                    public boolean onMove(@NonNull RecyclerView recyclerView,
                                          @NonNull RecyclerView.ViewHolder viewHolder,
                                          @NonNull RecyclerView.ViewHolder target) {
                        return false;
                    }

                    @Override
                    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                        int pos = viewHolder.getBindingAdapterPosition();
                        MentalEntry swiped = historyAdapter.getItemAt(pos);

                        if (swiped == null) {
                            historyAdapter.notifyItemChanged(pos);
                            return;
                        }

                        lastDeleted = swiped;
                        storage.deleteById(swiped.id);
                        refreshFromStorage();
                        showSnack("Entry deleted", "Undo", v -> undoDelete());
                    }

                    @Override
                    public void onChildDraw(@NonNull Canvas c,
                                            @NonNull RecyclerView recyclerView,
                                            @NonNull RecyclerView.ViewHolder viewHolder,
                                            float dX, float dY,
                                            int actionState,
                                            boolean isCurrentlyActive) {

                        View item = viewHolder.itemView;

                        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
                        p.setColor(Color.parseColor("#EF4444"));

                        RectF bg = new RectF(
                                dX > 0 ? item.getLeft() : item.getRight() + dX,
                                item.getTop(),
                                dX > 0 ? item.getLeft() + dX : item.getRight(),
                                item.getBottom()
                        );
                        c.drawRoundRect(bg, dp(16), dp(16), p);

                        Paint t = new Paint(Paint.ANTI_ALIAS_FLAG);
                        t.setColor(Color.WHITE);
                        t.setTextSize(sp(14));
                        t.setFakeBoldText(true);

                        String label = "Delete";
                        float textWidth = t.measureText(label);
                        float textX = dX > 0
                                ? item.getLeft() + dp(20)
                                : item.getRight() - dp(20) - textWidth;
                        float textY = item.getTop() + (item.getHeight() / 2f) + (t.getTextSize() / 3f);
                        c.drawText(label, textX, textY, t);

                        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
                    }
                };

        new ItemTouchHelper(cb).attachToRecyclerView(recyclerHistory);
    }

    private void setupReminders() {
        SharedPreferences p = getUiPrefs();
        boolean enabled = p.getBoolean(KEY_REMINDERS, false);
        int hour = p.getInt(KEY_REMINDER_HOUR, 20);
        int min = p.getInt(KEY_REMINDER_MIN, 0);

        if (swMoodReminders != null) swMoodReminders.setChecked(enabled);
        updateReminderTimeLabel(hour, min);

        if (swMoodReminders != null) {
            swMoodReminders.setOnCheckedChangeListener((buttonView, isChecked) -> {
                getUiPrefs().edit().putBoolean(KEY_REMINDERS, isChecked).apply();
                int h = getUiPrefs().getInt(KEY_REMINDER_HOUR, 20);
                int m = getUiPrefs().getInt(KEY_REMINDER_MIN, 0);
                if (isChecked) {
                    scheduleReminder(h, m);
                    showSnack("Mood reminders enabled", null, null);
                } else {
                    cancelReminder();
                    showSnack("Mood reminders disabled", null, null);
                }
            });
        }

        if (btnReminderTime != null) {
            btnReminderTime.setOnClickListener(v -> {
                int curH = getUiPrefs().getInt(KEY_REMINDER_HOUR, 20);
                int curM = getUiPrefs().getInt(KEY_REMINDER_MIN, 0);

                android.app.TimePickerDialog dlg = new android.app.TimePickerDialog(
                        this,
                        (view, selectedHour, selectedMin) -> {
                            getUiPrefs().edit()
                                    .putInt(KEY_REMINDER_HOUR, selectedHour)
                                    .putInt(KEY_REMINDER_MIN, selectedMin)
                                    .apply();

                            updateReminderTimeLabel(selectedHour, selectedMin);

                            if (getUiPrefs().getBoolean(KEY_REMINDERS, false)) {
                                scheduleReminder(selectedHour, selectedMin);
                                showSnack("Reminder time updated", null, null);
                            }
                        },
                        curH, curM, android.text.format.DateFormat.is24HourFormat(this)
                );
                dlg.show();
            });
        }

        if (tvReminderHint != null) {
            tvReminderHint.setOnClickListener(v -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
                    if (am != null && !am.canScheduleExactAlarms()) {
                        try {
                            startActivity(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM));
                        } catch (Exception ignored) {
                        }
                    }
                }
            });
        }
    }

    private void setupPrivacy() {
        SharedPreferences p = getUiPrefs();

        if (swHidePreviews != null) {
            swHidePreviews.setChecked(p.getBoolean(KEY_HIDE_PREVIEWS, false));
            swHidePreviews.setOnCheckedChangeListener((b, checked) -> {
                p.edit().putBoolean(KEY_HIDE_PREVIEWS, checked).apply();
                applyFilterAndRender();
                showSnack(checked ? "Previews hidden" : "Previews shown", null, null);
            });
        }

        if (swAppLock != null) {
            swAppLock.setChecked(p.getBoolean(KEY_APP_LOCK, false));
            swAppLock.setOnCheckedChangeListener((b, checked) -> {
                p.edit().putBoolean(KEY_APP_LOCK, checked).apply();
                if (checked) requestBiometricGateIfAvailable();
                showSnack(checked ? "App lock enabled" : "App lock disabled", null, null);
            });
        }
    }

    private void requestBiometricGateIfAvailable() {
        BiometricManager bm = BiometricManager.from(this);
        int can = bm.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG
                | BiometricManager.Authenticators.DEVICE_CREDENTIAL);

        if (can != BiometricManager.BIOMETRIC_SUCCESS) {
            showSnack("Biometric/device auth not available", null, null);
            getUiPrefs().edit().putBoolean(KEY_APP_LOCK, false).apply();
            if (swAppLock != null) swAppLock.setChecked(false);
            return;
        }

        BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Unlock")
                .setSubtitle("Confirm to access your private mood & journal entries")
                .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG
                        | BiometricManager.Authenticators.DEVICE_CREDENTIAL)
                .build();

        BiometricPrompt prompt = new BiometricPrompt(this,
                ContextCompat.getMainExecutor(this),
                new BiometricPrompt.AuthenticationCallback() {
                    @Override
                    public void onAuthenticationFailed() {
                        showSnack("Not recognized", null, null);
                    }

                    @Override
                    public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                        finish();
                    }

                    @Override
                    public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                        // Access granted
                    }
                });

        prompt.authenticate(promptInfo);
    }

    private void updateReminderTimeLabel(int hour, int min) {
        if (btnReminderTime != null) {
            String t = String.format(Locale.getDefault(), "%02d:%02d", hour, min);
            btnReminderTime.setText("Reminder time: " + t);
        }
    }

    private void scheduleReminder(int hour, int min) {
        AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
        if (am == null) return;

        Calendar c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY, hour);
        c.set(Calendar.MINUTE, min);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);

        if (c.getTimeInMillis() <= System.currentTimeMillis()) c.add(Calendar.DAY_OF_YEAR, 1);

        PendingIntent pi = MoodReminderReceiver.pendingIntent(this);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Exact alarms require permission on Android 12+; fall back to inexact if not allowed
            if (am.canScheduleExactAlarms()) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, c.getTimeInMillis(), pi);
            } else {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, c.getTimeInMillis(), pi);
                showSnack("Exact alarms are off. Tap the hint to enable exact reminders.", null, null);
            }
        } else {
            am.setExact(AlarmManager.RTC_WAKEUP, c.getTimeInMillis(), pi);
        }
    }

    private void cancelReminder() {
        AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
        if (am == null) return;
        am.cancel(MoodReminderReceiver.pendingIntent(this));
    }

    private void updateMoodLabel(int mood1to5) {
        if (tvMoodValue != null) {
            String label;
            switch (mood1to5) {
                case 1:
                    label = "Very low";
                    break;
                case 2:
                    label = "Low";
                    break;
                case 3:
                    label = "Neutral";
                    break;
                case 4:
                    label = "Good";
                    break;
                default:
                    label = "Great";
                    break;
            }
            tvMoodValue.setText("Mood: " + mood1to5 + " / 5  •  " + label);
        }
    }

    private void highlightEmoji(int mood1to5) {
        for (int i = 0; i < moodEmojis.length; i++) {
            TextView tv = moodEmojis[i];
            if (tv == null) continue;

            boolean active = (i == (mood1to5 - 1));

            float targetScale = active ? 1.12f : 1.0f;
            float targetAlpha = active ? 1.0f : 0.55f;

            tv.animate()
                    .scaleX(targetScale)
                    .scaleY(targetScale)
                    .alpha(targetAlpha)
                    .setDuration(160)
                    .setInterpolator(new AccelerateDecelerateInterpolator())
                    .start();

            tv.setTextColor(active ? Color.WHITE : Color.parseColor("#B3B3B3"));
        }
    }

    private void refreshFromStorage() {
        allItems = storage.load();
        applyFilterAndRender();
        renderInsights();
    }

    private void applyFilterAndRender() {
        List<MentalEntry> filtered = new ArrayList<>();

        boolean all = chipFilterAll != null && chipFilterAll.isChecked();
        boolean mood = chipFilterMood != null && chipFilterMood.isChecked();
        boolean journal = chipFilterJournal != null && chipFilterJournal.isChecked();

        for (MentalEntry e : allItems) {
            if (all) filtered.add(e);
            else {
                if (mood && e.type == MentalEntry.Type.MOOD) filtered.add(e);
                if (journal && e.type == MentalEntry.Type.JOURNAL) filtered.add(e);
            }
        }

        historyAdapter.setItems(filtered);

        boolean isEmpty = filtered.isEmpty();
        if (emptyState != null) emptyState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        if (recyclerHistory != null) recyclerHistory.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
    }

    private void saveMood() {
        if (sliderMood == null) return;

        int mood = Math.round(sliderMood.getValue());
        String note = (etMoodNote != null && etMoodNote.getText() != null)
                ? etMoodNote.getText().toString().trim()
                : "";

        List<String> tags = getSelectedTags();

        MentalEntry entry = MentalEntry.moodEntry(System.currentTimeMillis(), mood, note, tags);
        storage.add(entry);

        if (etMoodNote != null) etMoodNote.setText("");
        clearSelectedTags();

        showSnack("Check-in saved", "View", v -> scrollTo(R.id.cardHistory));
        refreshFromStorage();

        if (root != null) root.performHapticFeedback(HapticFeedbackConstants.CONFIRM);
    }

    private void saveJournal() {
        String text = (etJournal != null && etJournal.getText() != null)
                ? etJournal.getText().toString().trim()
                : "";

        if (TextUtils.isEmpty(text)) {
            showSnack("Write something before saving", null, null);
            return;
        }

        storage.add(MentalEntry.journalEntry(System.currentTimeMillis(), text));
        if (etJournal != null) etJournal.setText("");

        showSnack("Journal entry saved", "View", v -> scrollTo(R.id.cardHistory));
        refreshFromStorage();

        if (root != null) root.performHapticFeedback(HapticFeedbackConstants.CONFIRM);
    }

    private void showClearOptions() {
        String[] options = new String[]{
                "Clear mood check-ins",
                "Clear journal entries",
                "Clear last 7 days",
                "Clear all"
        };

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Clear history")
                .setItems(options, (d, which) -> {
                    switch (which) {
                        case 0:
                            storage.clearByType(MentalEntry.Type.MOOD);
                            break;
                        case 1:
                            storage.clearByType(MentalEntry.Type.JOURNAL);
                            break;
                        case 2:
                            storage.clearLastDays(7);
                            break;
                        case 3:
                            storage.clear();
                            break;
                    }
                    refreshFromStorage();
                    showSnack("History updated", null, null);
                })
                .setNegativeButton("Cancel", (dd, w) -> dd.dismiss())
                .show();
    }

    private void undoDelete() {
        if (lastDeleted == null) return;
        storage.add(lastDeleted);
        lastDeleted = null;
        refreshFromStorage();
        showSnack("Restored", null, null);
    }

    private void renderInsights() {
        if (tvAvg7 == null || tvStreak == null) return;

        long now = System.currentTimeMillis();
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(now);
        c.add(Calendar.DAY_OF_YEAR, -6);
        long start = c.getTimeInMillis();

        int count = 0;
        int sum = 0;

        int[] dailyMood = new int[7];
        int[] dailyCount = new int[7];

        for (MentalEntry e : allItems) {
            if (e.type == MentalEntry.Type.MOOD && e.timestampMs >= start) {
                sum += e.mood;
                count++;

                int dayIndex = dayIndexFromStart(start, e.timestampMs);
                if (dayIndex >= 0 && dayIndex < 7) {
                    dailyMood[dayIndex] += e.mood;
                    dailyCount[dayIndex] += 1;
                }
            }
        }

        if (count == 0) tvAvg7.setText("7-day avg mood: —");
        else {
            double avg = sum / (double) count;
            tvAvg7.setText(String.format(Locale.getDefault(), "7-day avg mood: %.1f / 5", avg));
        }

        int streak = calculateEntryStreakDays();
        tvStreak.setText("Streak: " + streak + " day" + (streak == 1 ? "" : "s"));

        float[] points = new float[7];
        for (int i = 0; i < 7; i++) {
            float avg = dailyCount[i] == 0 ? 0f : (dailyMood[i] / (float) dailyCount[i]);
            points[i] = avg;
        }
        if (sparkline != null) sparkline.setValues(points);

        if (weekAdapter != null) {
            List<WeekDot> dots = new ArrayList<>();
            Calendar day = Calendar.getInstance();
            day.setTimeInMillis(start);
            for (int i = 0; i < 7; i++) {
                boolean has = dailyCount[i] > 0;
                dots.add(new WeekDot(dfShortDow(day.getTime()), has));
                day.add(Calendar.DAY_OF_YEAR, 1);
            }
            weekAdapter.setItems(dots);
        }

        if (chipTopTags != null) {
            chipTopTags.removeAllViews();
            Map<String, Integer> tagCounts = new HashMap<>();

            for (MentalEntry e : allItems) {
                if (e.type == MentalEntry.Type.MOOD && e.timestampMs >= start && e.tags != null) {
                    for (String t : e.tags) {
                        if (TextUtils.isEmpty(t)) continue;
                        Integer cur = tagCounts.get(t);
                        tagCounts.put(t, cur == null ? 1 : (cur + 1));
                    }
                }
            }

            List<Map.Entry<String, Integer>> list = new ArrayList<>(tagCounts.entrySet());
            Collections.sort(list, (a, b) -> Integer.compare(b.getValue(), a.getValue()));

            int limit = Math.min(6, list.size());
            if (limit == 0) {
                chipTopTags.addView(makeBadgeChip("No tags this week"));
            } else {
                for (int i = 0; i < limit; i++) {
                    String tag = list.get(i).getKey();
                    int n = list.get(i).getValue();
                    chipTopTags.addView(makeBadgeChip(tag + " · " + n));
                }
            }
        }
    }

    private Chip makeBadgeChip(String text) {
        Chip c = new Chip(this);
        c.setText(text);
        c.setCheckable(false);
        c.setClickable(false);
        c.setTextColor(Color.WHITE);
        c.setChipBackgroundColor(android.content.res.ColorStateList.valueOf(Color.parseColor("#262626")));
        c.setChipStrokeWidth(dp(1));
        c.setChipStrokeColor(android.content.res.ColorStateList.valueOf(Color.parseColor("#3A3A3A")));
        c.setEnsureMinTouchTargetSize(false);
        return c;
    }

    private String dfShortDow(Date date) {
        return new SimpleDateFormat("EEE", Locale.getDefault()).format(date);
    }

    private int dayIndexFromStart(long startMs, long tsMs) {
        Calendar s = Calendar.getInstance();
        s.setTimeInMillis(startMs);
        zeroTime(s);

        Calendar t = Calendar.getInstance();
        t.setTimeInMillis(tsMs);
        zeroTime(t);

        long diff = t.getTimeInMillis() - s.getTimeInMillis();
        return (int) (diff / (24L * 60L * 60L * 1000L));
    }

    private void zeroTime(Calendar c) {
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
    }

    private int calculateEntryStreakDays() {
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        Set<String> days = new HashSet<>();
        for (MentalEntry e : allItems) {
            days.add(df.format(new Date(e.timestampMs)));
        }

        int streak = 0;
        Calendar day = Calendar.getInstance();
        for (int i = 0; i < 365; i++) {
            String key = df.format(day.getTime());
            if (days.contains(key)) {
                streak++;
                day.add(Calendar.DAY_OF_YEAR, -1);
            } else break;
        }
        return streak;
    }

    private String getPromptForToday() {
        return getPromptForTodayWithOffset(System.currentTimeMillis());
    }

    private String getPromptForTodayWithOffset(long seedMs) {
        String[] prompts = new String[]{
                "Prompt: What’s one thing you can control today?",
                "Prompt: Name 3 things you did well recently.",
                "Prompt: What triggered your mood today?",
                "Prompt: What’s one small step that would help right now?",
                "Prompt: Write a kind sentence you would tell a friend.",
                "Prompt: What do you need more of this week?",
                "Prompt: What’s one boundary you can set today?"
        };
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(seedMs);
        int idx = cal.get(Calendar.DAY_OF_YEAR) % prompts.length;
        return prompts[idx] + "  •  " + dfDay.format(cal.getTime());
    }

    private List<String> getSelectedTags() {
        List<String> tags = new ArrayList<>();
        if (chipGroupTags == null) return tags;

        for (int i = 0; i < chipGroupTags.getChildCount(); i++) {
            View v = chipGroupTags.getChildAt(i);
            if (v instanceof Chip) {
                Chip c = (Chip) v;
                if (c.isChecked()) tags.add(String.valueOf(c.getText()));
            }
        }
        return tags;
    }

    private void clearSelectedTags() {
        if (chipGroupTags == null) return;
        for (int i = 0; i < chipGroupTags.getChildCount(); i++) {
            View v = chipGroupTags.getChildAt(i);
            if (v instanceof Chip) ((Chip) v).setChecked(false);
        }
    }

    private void exportPdf() {
        if (exportLauncher == null) return;
        Intent i = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("application/pdf");
        i.putExtra(Intent.EXTRA_TITLE, "zealth_mental_export.pdf");
        exportLauncher.launch(i);
    }

    private void importPdf() {
        if (importLauncher == null) return;
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("application/pdf");
        importLauncher.launch(i);
    }

    private void openUri(Uri uri) {
        try {
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setData(uri);
            i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(i);
        } catch (ActivityNotFoundException ignored) {
        }
    }

    private void showSnack(String msg, @Nullable String action, @Nullable View.OnClickListener onAction) {
        if (root == null) return;
        Snackbar s = Snackbar.make(root, msg, Snackbar.LENGTH_LONG);
        if (!TextUtils.isEmpty(action) && onAction != null) s.setAction(action, onAction);
        s.show();
    }

    private void scrollTo(int viewId) {
        View target = findViewById(viewId);
        if (target == null || scroll == null) return;

        scroll.post(() -> {
            int y = target.getTop() - (int) dp(10);
            scroll.smoothScrollTo(0, Math.max(0, y));
        });
    }

    private float dp(float v) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, getResources().getDisplayMetrics());
    }

    private float sp(float v) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, v, getResources().getDisplayMetrics());
    }

    // ---------------- Emergency (Spotify-like BottomSheet, country auto-detected, PDF-driven) ----------------

    private void showEmergencyBottomSheet() {
        final String iso = guessCountryIso();
        final String emergency = EmergencyResources.emergencyNumberFor(iso);

        // Get country-specific options from the PDF-driven resources; fallback if missing
        List<EmergencyOption> options = emergencyResources.getForCountry(iso);
        if (options == null || options.isEmpty()) {
            options = emergencyResources.getFallbackFor(iso);
        }

        BottomSheetDialog dlg = new BottomSheetDialog(this);

        // Build a Spotify-like bottom sheet programmatically (no extra XML needed)
        LinearLayout shell = new LinearLayout(this);
        shell.setOrientation(LinearLayout.VERTICAL);
        shell.setPadding((int) dp(18), (int) dp(16), (int) dp(18), (int) dp(18));
        shell.setBackgroundColor(Color.parseColor("#121212"));

        TextView title = new TextView(this);
        title.setText("Emergency help");
        title.setTextColor(Color.WHITE);
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        title.setPadding(0, 0, 0, (int) dp(6));
        title.setTypeface(title.getTypeface(), android.graphics.Typeface.BOLD);

        TextView subtitle = new TextView(this);
        subtitle.setText("Region: " + iso + "  •  Emergency: " + emergency);
        subtitle.setTextColor(Color.parseColor("#B3B3B3"));
        subtitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        subtitle.setPadding(0, 0, 0, (int) dp(14));

        MaterialButton btnPrimary = new MaterialButton(this);
        btnPrimary.setText("Call emergency (" + emergency + ")");
        btnPrimary.setAllCaps(false);
        btnPrimary.setCornerRadius((int) dp(999));
        btnPrimary.setTextColor(Color.BLACK);
        btnPrimary.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#1DB954")));
        btnPrimary.setOnClickListener(v -> dial(emergency));

        MaterialButton btnMore = new MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
        btnMore.setText("Find local support resources (online)");
        btnMore.setAllCaps(false);
        btnMore.setCornerRadius((int) dp(999));
        btnMore.setStrokeWidth((int) dp(1));
        btnMore.setStrokeColor(android.content.res.ColorStateList.valueOf(Color.parseColor("#2A2A2A")));
        btnMore.setTextColor(Color.WHITE);
        btnMore.setOnClickListener(v -> openSupportResources());

        TextView hint = new TextView(this);
        hint.setText("If you feel unsafe or may harm yourself or others, seek urgent help now.");
        hint.setTextColor(Color.parseColor("#B3B3B3"));
        hint.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        hint.setPadding(0, (int) dp(12), 0, (int) dp(8));

        RecyclerView rv = new RecyclerView(this);
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setOverScrollMode(View.OVER_SCROLL_NEVER);

        EmergencyOptionAdapter ad = new EmergencyOptionAdapter(opt -> {
            if (!TextUtils.isEmpty(opt.phone)) dial(opt.phone);
            else if (!TextUtils.isEmpty(opt.url)) openUrl(opt.url);
            else openSupportResources();
        });
        rv.setAdapter(ad);
        ad.setItems(options);

        LinearLayout.LayoutParams lpFull = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        );
        LinearLayout.LayoutParams lpList = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        );
        lpList.topMargin = (int) dp(12);

        shell.addView(title, lpFull);
        shell.addView(subtitle, lpFull);
        shell.addView(btnPrimary, lpFull);

        LinearLayout.LayoutParams lpMore = new LinearLayout.LayoutParams(lpFull);
        lpMore.topMargin = (int) dp(10);
        shell.addView(btnMore, lpMore);

        shell.addView(hint, lpFull);
        shell.addView(rv, lpList);

        MaterialButton btnClose = new MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
        btnClose.setText("Close");
        btnClose.setAllCaps(false);
        btnClose.setCornerRadius((int) dp(999));
        btnClose.setStrokeWidth((int) dp(1));
        btnClose.setStrokeColor(android.content.res.ColorStateList.valueOf(Color.parseColor("#2A2A2A")));
        btnClose.setTextColor(Color.parseColor("#B3B3B3"));
        btnClose.setOnClickListener(v -> dlg.dismiss());

        LinearLayout.LayoutParams lpClose = new LinearLayout.LayoutParams(lpFull);
        lpClose.topMargin = (int) dp(12);
        shell.addView(btnClose, lpClose);

        dlg.setContentView(shell);
        dlg.show();
    }

    private void dial(String number) {
        if (TextUtils.isEmpty(number)) return;
        try {
            String n = number.replace(" ", "").replace("-", "");
            startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + Uri.encode(n))));
        } catch (Exception e) {
            showSnack("Could not open dialer", null, null);
        }
    }

    private void openSupportResources() {
        openUrl("https://findahelpline.com/");
    }


    private void openUrl(String url) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (Exception e) {
            showSnack("Could not open link", null, null);
        }
    }

    private String guessCountryIso() {
        String iso = null;
        try {
            TelephonyManager tm = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
            if (tm != null) {
                iso = tm.getNetworkCountryIso();
                if (TextUtils.isEmpty(iso)) iso = tm.getSimCountryIso();
            }
        } catch (Exception ignored) {
        }

        if (TextUtils.isEmpty(iso)) iso = Locale.getDefault().getCountry();
        if (TextUtils.isEmpty(iso)) iso = "US";
        return iso.toUpperCase(Locale.US);
    }


    public static class MoodReminderReceiver extends android.content.BroadcastReceiver {
        private static final int REQ = 7421;

        static PendingIntent pendingIntent(Context ctx) {
            Intent i = new Intent(ctx, MoodReminderReceiver.class);
            int flags = PendingIntent.FLAG_UPDATE_CURRENT;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) flags |= PendingIntent.FLAG_IMMUTABLE;
            return PendingIntent.getBroadcast(ctx, REQ, i, flags);
        }

        @Override
        public void onReceive(Context context, Intent intent) {
         
            Intent open = new Intent(context, MentalHealthActivity.class);
            open.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            try {
                context.startActivity(open);
            } catch (Exception ignored) {
            }

            SharedPreferences p = context.getSharedPreferences(PREF_UI, Context.MODE_PRIVATE);
            if (!p.getBoolean(KEY_REMINDERS, false)) return;

            int hour = p.getInt(KEY_REMINDER_HOUR, 20);
            int min = p.getInt(KEY_REMINDER_MIN, 0);

            AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            if (am == null) return;

            Calendar c = Calendar.getInstance();
            c.set(Calendar.HOUR_OF_DAY, hour);
            c.set(Calendar.MINUTE, min);
            c.set(Calendar.SECOND, 0);
            c.set(Calendar.MILLISECOND, 0);
            c.add(Calendar.DAY_OF_YEAR, 1);

            PendingIntent pi = pendingIntent(context);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (am.canScheduleExactAlarms()) {
                    am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, c.getTimeInMillis(), pi);
                } else {
                    am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, c.getTimeInMillis(), pi);
                }
            } else {
                am.setExact(AlarmManager.RTC_WAKEUP, c.getTimeInMillis(), pi);
            }
        }
    }

    static class WeekDot {
        final String label;
        final boolean has;

        WeekDot(String label, boolean has) {
            this.label = label;
            this.has = has;
        }
    }

    static class WeekStripAdapter extends RecyclerView.Adapter<WeekStripAdapter.VH> {
        private List<WeekDot> items = new ArrayList<>();

        void setItems(List<WeekDot> items) {
            this.items = items == null ? new ArrayList<>() : items;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LinearLayout root = new LinearLayout(parent.getContext());
            root.setOrientation(LinearLayout.VERTICAL);
            root.setPadding((int) dp(parent, 10), (int) dp(parent, 8), (int) dp(parent, 10), (int) dp(parent, 8));
            root.setLayoutParams(new RecyclerView.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

            TextView tv = new TextView(parent.getContext());
            tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
            tv.setTextColor(Color.parseColor("#B3B3B3"));
            tv.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);

            View dot = new View(parent.getContext());
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams((int) dp(parent, 10), (int) dp(parent, 10));
            lp.topMargin = (int) dp(parent, 6);
            lp.gravity = android.view.Gravity.CENTER_HORIZONTAL;
            dot.setLayoutParams(lp);
            dot.setBackground(makeDotBg(false));

            root.addView(tv);
            root.addView(dot);

            return new VH(root, tv, dot);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int position) {
            WeekDot d = items.get(position);
            h.tv.setText(d.label);
            h.dot.setBackground(makeDotBg(d.has));
            h.dot.setAlpha(d.has ? 1f : 0.25f);
        }

        @Override
        public int getItemCount() {
            return items == null ? 0 : items.size();
        }

        static class VH extends RecyclerView.ViewHolder {
            TextView tv;
            View dot;

            VH(@NonNull View itemView, TextView tv, View dot) {
                super(itemView);
                this.tv = tv;
                this.dot = dot;
            }
        }

        private static android.graphics.drawable.Drawable makeDotBg(boolean filled) {
            int color = filled ? Color.parseColor("#1DB954") : Color.parseColor("#2A2A2A");
            android.graphics.drawable.GradientDrawable d = new android.graphics.drawable.GradientDrawable();
            d.setShape(android.graphics.drawable.GradientDrawable.OVAL);
            d.setColor(color);
            return d;
        }

        private static float dp(ViewGroup parent, float v) {
            return TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP, v, parent.getResources().getDisplayMetrics()
            );
        }
    }


    static class EmergencyOption {
        final String title;
        final String subtitle;
        final String phone; 
        final String url;   

        EmergencyOption(String title, String subtitle, String phone, String url) {
            this.title = title;
            this.subtitle = subtitle;
            this.phone = phone;
            this.url = url;
        }
    }

    interface EmergencyClick {
        void onClick(EmergencyOption opt);
    }

    static class EmergencyOptionAdapter extends RecyclerView.Adapter<EmergencyOptionAdapter.VH> {
        private final List<EmergencyOption> items = new ArrayList<>();
        private final EmergencyClick click;

        EmergencyOptionAdapter(EmergencyClick click) {
            this.click = click;
        }

        void setItems(List<EmergencyOption> list) {
            items.clear();
            if (list != null) items.addAll(list);
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LinearLayout row = new LinearLayout(parent.getContext());
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setPadding((int) dp(parent, 14), (int) dp(parent, 12), (int) dp(parent, 14), (int) dp(parent, 12));
            row.setBackgroundColor(Color.parseColor("#181818"));
            RecyclerView.LayoutParams lp = new RecyclerView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            );
            lp.bottomMargin = (int) dp(parent, 10);
            row.setLayoutParams(lp);

            LinearLayout left = new LinearLayout(parent.getContext());
            left.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams lpLeft = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            left.setLayoutParams(lpLeft);

            TextView tvTitle = new TextView(parent.getContext());
            tvTitle.setTextColor(Color.WHITE);
            tvTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
            tvTitle.setTypeface(tvTitle.getTypeface(), android.graphics.Typeface.BOLD);

            TextView tvSub = new TextView(parent.getContext());
            tvSub.setTextColor(Color.parseColor("#B3B3B3"));
            tvSub.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
            tvSub.setPadding(0, (int) dp(parent, 4), 0, 0);

            left.addView(tvTitle);
            left.addView(tvSub);

            MaterialButton btn = new MaterialButton(parent.getContext());
            btn.setAllCaps(false);
            btn.setText("Open");
            btn.setCornerRadius((int) dp(parent, 999));
            btn.setTextColor(Color.BLACK);
            btn.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#1DB954")));

            row.addView(left);
            row.addView(btn);

            return new VH(row, tvTitle, tvSub, btn);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int position) {
            EmergencyOption e = items.get(position);

            h.tvTitle.setText(e.title);

            String subtitle = e.subtitle == null ? "" : e.subtitle;
            if (!TextUtils.isEmpty(e.phone)) subtitle = subtitle + "  •  " + e.phone;
            h.tvSub.setText(subtitle);

            View.OnClickListener l = v -> {
                if (click != null) click.onClick(e);
            };
            h.btn.setOnClickListener(l);
            h.itemView.setOnClickListener(l);
        }


        @Override
        public int getItemCount() {
            return items.size();
        }

        static class VH extends RecyclerView.ViewHolder {
            TextView tvTitle, tvSub;
            MaterialButton btn;

            VH(@NonNull View itemView, TextView tvTitle, TextView tvSub, MaterialButton btn) {
                super(itemView);
                this.tvTitle = tvTitle;
                this.tvSub = tvSub;
                this.btn = btn;
            }
        }

        private static float dp(ViewGroup parent, float v) {
            return TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP, v, parent.getResources().getDisplayMetrics()
            );
        }
    }


    static class EmergencyResources {
        private final Map<String, List<EmergencyOption>> map = new HashMap<>();

        void loadFromAssetsPdf(Context ctx, String assetName) {
            try (InputStream is = ctx.getAssets().open(assetName)) {
                byte[] bytes = readAllBytes(is);
                String blob = new String(bytes, "ISO-8859-1");
                String block = extractBlock(blob, "ZEMERGENCY_BEGIN", "ZEMERGENCY_END");
                if (TextUtils.isEmpty(block)) return;

                for (String raw : block.split("\n")) {
                    String line = raw.trim();
                    if (line.isEmpty()) continue;

                  
                    String[] p = line.split("\\|", -1);
                    if (p.length < 3) continue;
                    String iso = safeUpper(p[0]);
                    String title = p[1].trim();
                    String sub = p[2].trim();
                    String phone = p.length >= 4 ? p[3].trim() : "";
                    String url = p.length >= 5 ? p[4].trim() : "";

                    if (TextUtils.isEmpty(iso) || TextUtils.isEmpty(title)) continue;
                    List<EmergencyOption> list = map.get(iso);
                    if (list == null) list = new ArrayList<>();
                    list.add(new EmergencyOption(title, sub, phone, url));
                    map.put(iso, list);
                }
            } catch (Exception ignored) {
            }
        }

        List<EmergencyOption> getForCountry(String iso) {
            if (TextUtils.isEmpty(iso)) return getFallbackFor("DEFAULT");
            iso = iso.toUpperCase(Locale.US);

            List<EmergencyOption> direct = map.get(iso);
            if (direct != null && !direct.isEmpty()) return direct;

            List<EmergencyOption> def = map.get("DEFAULT");
            if (def != null && !def.isEmpty()) return def;

            return new ArrayList<>();
        }

        List<EmergencyOption> getFallbackFor(String iso) {
            iso = safeUpper(iso);
            List<EmergencyOption> list = new ArrayList<>();

            if ("US".equals(iso) || "CA".equals(iso)) {
                list.add(new EmergencyOption(
                        "988 Suicide & Crisis Lifeline",
                        "24/7 crisis support (call or text 988)",
                        "988",
                        ""
                ));
            }
            if ("GB".equals(iso) || "UK".equals(iso)) {
                list.add(new EmergencyOption(
                        "Samaritans",
                        "24/7 listening line",
                        "116123",
                        ""
                ));
            }

            list.add(new EmergencyOption(
                    "FindAHelpline",
                    "Global directory for local crisis resources",
                    "",
                    "https://findahelpline.com/"
            ));

            return list;
        }

        static String emergencyNumberFor(String iso) {
            iso = safeUpper(iso);
            if ("US".equals(iso) || "CA".equals(iso)) return "911";
            if ("GB".equals(iso) || "UK".equals(iso)) return "999";
            if ("AU".equals(iso)) return "000";
            return "112";
        }

        private static String extractBlock(String blob, String begin, String end) {
            int a = blob.indexOf(begin);
            if (a < 0) return "";
            int b = blob.indexOf(end, a + begin.length());
            if (b < 0) return "";
            return blob.substring(a + begin.length(), b).trim();
        }

        private static String safeUpper(String s) {
            if (s == null) return "";
            String t = s.trim();
            if (t.isEmpty()) return "";
            return t.toUpperCase(Locale.US);
        }

        private static byte[] readAllBytes(InputStream is) throws Exception {
            java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = is.read(buf)) > 0) bos.write(buf, 0, n);
            return bos.toByteArray();
        }
    }


    static class PdfBackupUtil {
        private static final String BEGIN = "ZBACKUP_BEGIN";
        private static final String END = "ZBACKUP_END";

        static void exportToPdf(List<MentalEntry> data, OutputStream os) throws Exception {
            if (data == null) data = new ArrayList<>();

            android.graphics.pdf.PdfDocument doc = new android.graphics.pdf.PdfDocument();
            final int pageW = 595;  // A4-ish at 72dpi
            final int pageH = 842;

            Paint title = new Paint(Paint.ANTI_ALIAS_FLAG);
            title.setColor(Color.BLACK);
            title.setTextSize(18f);
            title.setFakeBoldText(true);

            Paint body = new Paint(Paint.ANTI_ALIAS_FLAG);
            body.setColor(Color.BLACK);
            body.setTextSize(10f);

            Paint mono = new Paint(Paint.ANTI_ALIAS_FLAG);
            mono.setColor(Color.DKGRAY);
            mono.setTextSize(7.5f);
            mono.setTypeface(android.graphics.Typeface.MONOSPACE);

            StringBuilder payload = new StringBuilder();
            payload.append(BEGIN).append("\n");
            for (MentalEntry e : data) {
                payload.append(encodeEntryLine(e)).append("\n");
            }
            payload.append(END).append("\n");

            int pageNum = 1;
            android.graphics.pdf.PdfDocument.Page page = doc.startPage(
                    new android.graphics.pdf.PdfDocument.PageInfo.Builder(pageW, pageH, pageNum).create()
            );
            Canvas c = page.getCanvas();

            float x = 36;
            float y = 48;

            c.drawText("Mood & Journal Backup", x, y, title);
            y += 18;
            c.drawText("This PDF contains your exported entries and a import block.", x, y, body);
            y += 14;
            c.drawText("Do not edit the backup block if you want to re-import.", x, y, body);
            y += 18;
            c.drawText("Wellness journaling only. Not for diagnosis or treatment.", x, y, body);


            String[] lines = payload.toString().split("\n");
            float lineH = 9.5f;
            float maxY = pageH - 36;

            for (String line : lines) {
                if (y + lineH > maxY) {
                    doc.finishPage(page);
                    pageNum++;
                    page = doc.startPage(new android.graphics.pdf.PdfDocument.PageInfo.Builder(pageW, pageH, pageNum).create());
                    c = page.getCanvas();
                    y = 36;
                }
                c.drawText(line, x, y, mono);
                y += lineH;
            }

            doc.finishPage(page);
            doc.writeTo(os);
            doc.close();
        }

        static List<MentalEntry> importFromPdf(InputStream is) throws Exception {
            byte[] bytes = readAllBytes(is);
            String blob = new String(bytes, "ISO-8859-1");

            int a = blob.indexOf(BEGIN);
            if (a < 0) throw new IllegalArgumentException("Missing backup block");
            int b = blob.indexOf(END, a + BEGIN.length());
            if (b < 0) throw new IllegalArgumentException("Missing backup block end");

            String block = blob.substring(a + BEGIN.length(), b).trim();
            List<MentalEntry> out = new ArrayList<>();
            if (block.isEmpty()) return out;

            for (String raw : block.split("\n")) {
                String line = raw.trim();
                if (line.isEmpty()) continue;
                MentalEntry e = decodeEntryLine(line);
                if (e != null) out.add(e);
            }

            Collections.sort(out, (x, y) -> Long.compare(y.timestampMs, x.timestampMs));
            return out;
        }

        private static String encodeEntryLine(MentalEntry e) {
            long id = e.id;
            long ts = e.timestampMs;
            String type = e.type == null ? "MOOD" : e.type.name();
            int mood = e.mood;

            String note = e.note == null ? "" : e.note;
            String journal = e.journalText == null ? "" : e.journalText;

            String tagsJoin = "";
            if (e.tags != null && !e.tags.isEmpty()) {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < e.tags.size(); i++) {
                    if (i > 0) sb.append('\u001F'); // unit separator
                    sb.append(e.tags.get(i));
                }
                tagsJoin = sb.toString();
            }

            String note64 = b64(note);
            String tags64 = b64(tagsJoin);
            String journ64 = b64(journal);

            return id + "|" + ts + "|" + type + "|" + mood + "|" + note64 + "|" + tags64 + "|" + journ64;
        }

        private static MentalEntry decodeEntryLine(String line) {
            try {
                String[] p = line.split("\\|", -1);
                if (p.length < 7) return null;

                long id = Long.parseLong(p[0]);
                long ts = Long.parseLong(p[1]);
                MentalEntry.Type type = MentalEntry.Type.valueOf(p[2]);
                int mood = Integer.parseInt(p[3]);

                String note = ub64(p[4]);
                String tagsJoin = ub64(p[5]);
                String journal = ub64(p[6]);

                MentalEntry e = new MentalEntry();
                e.id = id;
                e.timestampMs = ts;
                e.type = type;
                e.mood = mood;
                e.note = note;

                e.tags = new ArrayList<>();
                if (!TextUtils.isEmpty(tagsJoin)) {
                    String[] parts = tagsJoin.split(String.valueOf('\u001F'), -1);
                    for (String t : parts) {
                        if (!TextUtils.isEmpty(t)) e.tags.add(t);
                    }
                }

                e.journalText = journal;
                return e;
            } catch (Exception ignored) {
                return null;
            }
        }

        private static String b64(String s) {
            if (s == null) s = "";
            byte[] bytes = s.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            return android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP);
        }

        private static String ub64(String b64) {
            if (b64 == null) return "";
            try {
                byte[] bytes = android.util.Base64.decode(b64, android.util.Base64.NO_WRAP);
                return new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
            } catch (Exception e) {
                return "";
            }
        }

        private static byte[] readAllBytes(InputStream is) throws Exception {
            java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = is.read(buf)) > 0) bos.write(buf, 0, n);
            return bos.toByteArray();
        }
    }
}
