package com.cypher.zealth;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.SeekBar;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import eightbitlab.com.blurview.BlurView;

public class BodyMapActivity extends AppCompatActivity {

    private final SimpleDateFormat dfDate = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

    private BodyMapStorage storage;
    private BodyEntryAdapter adapter;

    private MaterialAutoCompleteTextView spArea;
    private SeekBar seekSeverity;
    private android.widget.TextView tvSeverity;
    private TextInputEditText etNote;

    private com.google.android.material.button.MaterialButton btnPickStartDate;
    private long startedDateMs;

    private TabLayout tabLayout;
    private View frontContainer, backContainer, historySection;
    private ChipGroup chipGroupAreas;

    private BlurView blurTabs, blurActions;

    private static final String[] AREAS = new String[]{
            "Head", "Neck", "Chest", "Abdomen", "Back", "Arms", "Legs", "Skin", "Other"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_body_map);

        MaterialToolbar tb = findViewById(R.id.toolbar);
        tb.setNavigationOnClickListener(v -> finish());

        storage = new BodyMapStorage(this);

        tabLayout = findViewById(R.id.tabLayout);
        frontContainer = findViewById(R.id.frontContainer);
        backContainer = findViewById(R.id.backContainer);
        historySection = findViewById(R.id.historySection);
        chipGroupAreas = findViewById(R.id.chipGroupAreas);

        spArea = findViewById(R.id.spArea);
        seekSeverity = findViewById(R.id.seekSeverity);
        tvSeverity = findViewById(R.id.tvSeverity);
        etNote = findViewById(R.id.etNote);
        btnPickStartDate = findViewById(R.id.btnPickStartDate);

        blurTabs = findViewById(R.id.blurTabs);
        blurActions = findViewById(R.id.blurActions);

        startedDateMs = startOfDay(System.currentTimeMillis());
        btnPickStartDate.setText("Started: " + dfDate.format(new Date(startedDateMs)));

        setupTabs();
        setupAreaDropdown();
        setupChips();
        setupBodyTapRegions();
        setupSeverity();
        setupBlur();

        androidx.recyclerview.widget.RecyclerView rv = findViewById(R.id.recyclerHistory);
        rv.setLayoutManager(new LinearLayoutManager(this));

        adapter = new BodyEntryAdapter(storage.load(), createdAtMs -> {
            storage.deleteByCreatedAt(createdAtMs);
            refresh();
            Toast.makeText(this, "Entry deleted.", Toast.LENGTH_SHORT).show();
        });
        rv.setAdapter(adapter);

        btnPickStartDate.setOnClickListener(v -> pickStartDate());
        findViewById(R.id.btnSaveEntry).setOnClickListener(v -> saveEntry());
        findViewById(R.id.btnClearAll).setOnClickListener(v -> clearAll());

        refresh();
    }

    private void setupTabs() {
        tabLayout.removeAllTabs();
        tabLayout.addTab(tabLayout.newTab().setText("Front"));
        tabLayout.addTab(tabLayout.newTab().setText("Back"));
        tabLayout.addTab(tabLayout.newTab().setText("History"));

        setTab(0);

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override public void onTabSelected(TabLayout.Tab tab) { setTab(tab.getPosition()); }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) { setTab(tab.getPosition()); }
        });
    }

    private void setTab(int pos) {
        boolean showFront = (pos == 0);
        boolean showBack = (pos == 1);
        boolean showHistory = (pos == 2);

        frontContainer.setVisibility(showFront ? View.VISIBLE : View.GONE);
        backContainer.setVisibility(showBack ? View.VISIBLE : View.GONE);
        historySection.setVisibility(showHistory ? View.VISIBLE : View.GONE);
    }

    private void setupAreaDropdown() {
        ArrayAdapter<String> a = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, AREAS);
        spArea.setAdapter(a);

        spArea.setDropDownBackgroundResource(R.drawable.glass_rounded);

        spArea.setText("Other", false);
    }

    private void setupChips() {
        chipGroupAreas.removeAllViews();

        String[] quick = new String[]{"Head", "Chest", "Abdomen", "Back", "Arms", "Legs", "Skin", "Other"};

        for (String label : quick) {
            Chip chip = new Chip(this);
            chip.setText(label);
            chip.setCheckable(true);
            chip.setClickable(true);
            chip.setTextColor(0xFFFFFFFF);
            chip.setChipBackgroundColorResource(android.R.color.transparent);
            chip.setChipStrokeWidth(1f);
            chip.setChipStrokeColor(android.content.res.ColorStateList.valueOf(0x66FFFFFF));
            chip.setRippleColor(android.content.res.ColorStateList.valueOf(0x22FFFFFF));
            chip.setEnsureMinTouchTargetSize(true);

            chip.setOnClickListener(v -> setSelectedArea(label));
            chipGroupAreas.addView(chip);
        }
    }

    private void setupBodyTapRegions() {
        bindRegionTap(R.id.regionHeadFront, "Head");
        bindRegionTap(R.id.regionChestFront, "Chest");
        bindRegionTap(R.id.regionAbdomenFront, "Abdomen");
        bindRegionTap(R.id.regionBackFront, "Back");
        bindRegionTap(R.id.regionArmsFront, "Arms");
        bindRegionTap(R.id.regionLegsFront, "Legs");

        bindRegionTap(R.id.regionHeadBack, "Head");
        bindRegionTap(R.id.regionBackBack, "Back");
        bindRegionTap(R.id.regionArmsBack, "Arms");
        bindRegionTap(R.id.regionLegsBack, "Legs");
    }

    private void bindRegionTap(int viewId, String area) {
        View region = findViewById(viewId);
        if (region == null) return; 
        region.setOnClickListener(v -> {
            setSelectedArea(area);
            Toast.makeText(this, area + " selected", Toast.LENGTH_SHORT).show();
        });
    }

    private void setSelectedArea(String area) {
        spArea.setText(area, false);

        for (int i = 0; i < chipGroupAreas.getChildCount(); i++) {
            View child = chipGroupAreas.getChildAt(i);
            if (child instanceof Chip) {
                Chip c = (Chip) child;
                c.setChecked(area.equalsIgnoreCase(String.valueOf(c.getText())));
            }
        }
    }

    private void setupSeverity() {
        seekSeverity.setMax(4);
        seekSeverity.setProgress(2);
        updateSeverityLabel(3);

        seekSeverity.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                updateSeverityLabel(progress + 1);
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    private void updateSeverityLabel(int s1to5) {
        tvSeverity.setText("Severity: " + s1to5 + " / 5");
    }

    private void setupBlur() {
        View root = findViewById(R.id.root);
        BlurUtil.setupBlur(this, blurTabs, root, 18f);
        BlurUtil.setupBlur(this, blurActions, root, 22f);
    }

    private void pickStartDate() {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(startedDateMs);

        DatePickerDialog dp = new DatePickerDialog(
                this,
                (view, year, month, day) -> {
                    Calendar chosen = Calendar.getInstance();
                    chosen.set(year, month, day);
                    startedDateMs = startOfDay(chosen.getTimeInMillis());
                    btnPickStartDate.setText("Started: " + dfDate.format(new Date(startedDateMs)));
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
        );
        dp.show();
    }

    private void saveEntry() {
        String area = spArea.getText() == null ? "Other" : spArea.getText().toString().trim();
        if (area.isEmpty()) area = "Other";

        int severity = seekSeverity.getProgress() + 1;

        String note = etNote.getText() == null ? "" : etNote.getText().toString().trim();
        if (note.isEmpty()) {
            Toast.makeText(this, "Please describe your symptoms.", Toast.LENGTH_SHORT).show();
            return;
        }

        long createdAt = System.currentTimeMillis();

        storage.add(BodyEntry.of(area, severity, note, startedDateMs, createdAt));
        etNote.setText("");

        refresh();
        Toast.makeText(this, "Entry saved.", Toast.LENGTH_SHORT).show();
    }

    private void refresh() {
        List<BodyEntry> items = storage.load();
        adapter.setItems(items);
    }

    private void clearAll() {
        new AlertDialog.Builder(this)
                .setTitle("Clear all entries?")
                .setMessage("This will remove all Body Map entries saved on this device.")
                .setNegativeButton("Cancel", (d, w) -> d.dismiss())
                .setPositiveButton("Clear", (d, w) -> {
                    storage.clear();
                    refresh();
                    Toast.makeText(this, "Cleared.", Toast.LENGTH_SHORT).show();
                })
                .show();
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
}
