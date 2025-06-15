package com.cypher.zealth;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.List;

public class HealthyGroceriesActivity extends AppCompatActivity {

    private GroceryStorage storage;

    private final List<GroceryCategory> categories = new ArrayList<>();
    private GroceryCategoryAdapter adapter;

    private TextViewCompat tvStats; 
    private LinearProgressIndicator progress;

    private TextInputEditText etSearch;
    private ChipGroup chipGroup;
    private FloatingActionButton fabAdd;

    private MaterialAutoCompleteTextView actCategoryJump;

    private GroceryItemAdapter.Filter filter = GroceryItemAdapter.Filter.ALL;
    private String query = "";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_healthy_groceries);

        applySpotifySystemBars();

        // Toolbar
        MaterialToolbar tb = findViewById(R.id.toolbar);
        if (tb != null) tb.setNavigationOnClickListener(v -> finish());

        tvStats = new TextViewCompat(findViewById(R.id.tvStats));
        progress = findViewById(R.id.progressCompletion);

        etSearch = findViewById(R.id.etSearch);
        chipGroup = findViewById(R.id.chipGroup);
        fabAdd = findViewById(R.id.fabAdd);
        actCategoryJump = findViewById(R.id.actCategory);

        storage = new GroceryStorage(this);

        categories.clear();
        categories.addAll(storage.loadOrDefault());

        RecyclerView rv = findViewById(R.id.recyclerCategories);
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setItemAnimator(null);

        adapter = new GroceryCategoryAdapter(categories, new GroceryCategoryAdapter.Listener() {
            @Override
            public void onDataChanged() {
                storage.save(categories);
                updateStats();
                setupCategoryJumpDropdown();
            }

            @Override
            public void onItemEditDeleteRequested(GroceryCategory category, GroceryItem item) {
                showItemMenu(category, item);
            }
        });

        rv.setAdapter(adapter);

        setupFilters();
        setupSearch();
        setupActions();
        setupCategoryJumpDropdown();

        adapter.applyFilter(filter, query);
        updateStats();
    }

    private void applySpotifySystemBars() {
        getWindow().setStatusBarColor(Color.parseColor("#121212"));
        View decor = getWindow().getDecorView();
        WindowInsetsControllerCompat controller = ViewCompat.getWindowInsetsController(decor);
        if (controller != null) controller.setAppearanceLightStatusBars(false);
    }

    private void setupActions() {
        View clear = findViewById(R.id.btnClearAll);
        if (clear != null) clear.setOnClickListener(v -> confirmClearAll());

        if (fabAdd != null) fabAdd.setOnClickListener(v -> showAddItemDialog());
    }

    private void setupFilters() {
        if (chipGroup == null) return;

        Chip cAll = findViewById(R.id.chipAll);
        Chip cUnchecked = findViewById(R.id.chipUnchecked);
        Chip cChecked = findViewById(R.id.chipChecked);

        chipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            int id = checkedIds.isEmpty() ? R.id.chipAll : checkedIds.get(0);
            if (id == R.id.chipUnchecked) filter = GroceryItemAdapter.Filter.UNCHECKED;
            else if (id == R.id.chipChecked) filter = GroceryItemAdapter.Filter.CHECKED;
            else filter = GroceryItemAdapter.Filter.ALL;

            adapter.applyFilter(filter, query);
            updateStats();
        });

        if (cAll != null) cAll.setChecked(true);
    }

    private void setupSearch() {
        if (etSearch == null) return;

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                query = (s == null) ? "" : s.toString().trim();
                adapter.applyFilter(filter, query);
                updateStats();
                updateEmptyState();
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void setupCategoryJumpDropdown() {
        if (actCategoryJump == null) return;

        List<String> titles = new ArrayList<>();
        for (GroceryCategory c : categories) {
            if (!TextUtils.isEmpty(c.title)) titles.add(c.title);
        }

        ArrayAdapter<String> a = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, titles);
        actCategoryJump.setAdapter(a);

        actCategoryJump.setDropDownBackgroundDrawable(new ColorDrawable(Color.parseColor("#1E1E1E")));

        actCategoryJump.setInputType(0);
        actCategoryJump.setKeyListener(null);

        actCategoryJump.setOnClickListener(v -> actCategoryJump.showDropDown());
        actCategoryJump.setOnFocusChangeListener((v, hasFocus) -> { if (hasFocus) actCategoryJump.showDropDown(); });

        actCategoryJump.setOnItemClickListener((parent, view, position, id) -> {
            String chosen = titles.get(position);
            int posInAdapter = adapter.positionOfCategoryTitle(chosen);
            if (posInAdapter >= 0) {
                RecyclerView rv = findViewById(R.id.recyclerCategories);
                if (rv != null) rv.smoothScrollToPosition(posInAdapter);
            }
        });

        if ((actCategoryJump.getText() == null || actCategoryJump.getText().toString().trim().isEmpty()) && !titles.isEmpty()) {
            actCategoryJump.setText(titles.get(0), false);
        }
    }

    private void updateEmptyState() {
        View empty = findViewById(R.id.emptyState);
        if (empty == null) return;

        boolean show = adapter.getItemCount() == 0;
        empty.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private void updateStats() {
        if (!tvStats.ok() || progress == null) return;

        int checked = 0, total = 0;
        for (GroceryCategory c : categories) {
            if (c.items == null) continue;
            total += c.items.size();
            for (GroceryItem it : c.items) if (it.checked) checked++;
        }

        int percent = (total <= 0) ? 0 : Math.round((checked * 100f) / total);
        tvStats.setText(checked + " / " + total + " in cart • " + percent + "%");

        progress.setMax(Math.max(total, 1));
        progress.setProgressCompat(Math.min(checked, progress.getMax()), true);

        updateEmptyState();
    }

    private void confirmClearAll() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Reset checklist?")
                .setMessage("This will clear your saved selections and restore the default list.")
                .setNegativeButton("Cancel", (d, w) -> d.dismiss())
                .setPositiveButton("Reset", (d, w) -> clearAllWithUndo())
                .show();
    }

    private void clearAllWithUndo() {
        final List<GroceryCategory> before = deepCopy(categories);

        storage.resetToDefault();
        categories.clear();
        categories.addAll(storage.loadOrDefault());

        storage.save(categories);
        setupCategoryJumpDropdown();

        adapter.applyFilter(filter, query);
        updateStats();

        View root = findViewById(R.id.root);
        if (root == null) root = getWindow().getDecorView();

        Snackbar.make(root, "Checklist reset.", Snackbar.LENGTH_LONG)
                .setAction("Undo", v -> {
                    categories.clear();
                    categories.addAll(before);
                    storage.save(categories);
                    setupCategoryJumpDropdown();
                    adapter.applyFilter(filter, query);
                    updateStats();
                    Toast.makeText(this, "Restored.", Toast.LENGTH_SHORT).show();
                })
                .show();
    }

    private List<GroceryCategory> deepCopy(List<GroceryCategory> src) {
        List<GroceryCategory> out = new ArrayList<>();
        for (GroceryCategory c : src) {
            GroceryCategory nc = new GroceryCategory();
            nc.title = c.title;
            nc.expanded = c.expanded;
            nc.items = new ArrayList<>();
            if (c.items != null) {
                for (GroceryItem it : c.items) {
                    nc.items.add(GroceryItem.of(it.name, it.checked));
                }
            }
            out.add(nc);
        }
        return out;
    }

    private void showAddItemDialog() {
        View dialog = getLayoutInflater().inflate(R.layout.dialog_add_grocery_item, null, false);

        TextInputLayout tilName = dialog.findViewById(R.id.tilItemName);
        TextInputEditText etName = dialog.findViewById(R.id.etItemName);

        MaterialAutoCompleteTextView acCategory = dialog.findViewById(R.id.acCategory);

        List<String> catTitles = new ArrayList<>();
        for (GroceryCategory c : categories) if (!TextUtils.isEmpty(c.title)) catTitles.add(c.title);

        if (acCategory != null) {
            acCategory.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, catTitles));
            acCategory.setDropDownBackgroundDrawable(new ColorDrawable(Color.parseColor("#1E1E1E")));
            acCategory.setInputType(0);
            acCategory.setKeyListener(null);
            acCategory.setOnClickListener(v -> acCategory.showDropDown());
            acCategory.setOnFocusChangeListener((v, hasFocus) -> { if (hasFocus) acCategory.showDropDown(); });
            if (!catTitles.isEmpty()) acCategory.setText(catTitles.get(0), false);
        }

        AlertDialog dlg = new MaterialAlertDialogBuilder(this)
                .setTitle("Add item")
                .setView(dialog)
                .setNegativeButton("Cancel", (d, w) -> d.dismiss())
                .setPositiveButton("Add", null)
                .create();

        dlg.setOnShowListener(d -> dlg.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String name = (etName == null || etName.getText() == null) ? "" : etName.getText().toString().trim();
            String cat = (acCategory == null || acCategory.getText() == null) ? "" : acCategory.getText().toString().trim();

            if (TextUtils.isEmpty(name)) {
                if (tilName != null) tilName.setError("Enter an item name");
                return;
            } else if (tilName != null) tilName.setError(null);

            GroceryCategory target = null;
            for (GroceryCategory c : categories) {
                if (TextUtils.equals(c.title, cat)) { target = c; break; }
            }

            if (target == null) {
                Toast.makeText(this, "Select a category.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (target.items == null) target.items = new ArrayList<>();

            for (GroceryItem it : target.items) {
                if (it != null && it.name != null && it.name.equalsIgnoreCase(name)) {
                    Toast.makeText(this, "Item already exists.", Toast.LENGTH_SHORT).show();
                    return;
                }
            }

            target.items.add(0, GroceryItem.of(name, false));
            target.expanded = true;

            storage.save(categories);
            setupCategoryJumpDropdown();

            adapter.applyFilter(filter, query);
            updateStats();

            View root = findViewById(R.id.root);
            if (root == null) root = getWindow().getDecorView();
            Snackbar.make(root, "Added: " + name, Snackbar.LENGTH_SHORT).show();

            dlg.dismiss();
        }));

        dlg.show();
    }

    private void showItemMenu(GroceryCategory category, GroceryItem item) {
        String[] options = new String[]{"Edit", "Delete"};
        new MaterialAlertDialogBuilder(this)
                .setTitle(item.name)
                .setItems(options, (d, which) -> {
                    if (which == 0) showEditItemDialog(item);
                    else confirmDeleteItem(category, item);
                })
                .show();
    }

    private void showEditItemDialog(GroceryItem item) {
        TextInputLayout til = new TextInputLayout(this);
        til.setBoxBackgroundMode(TextInputLayout.BOX_BACKGROUND_FILLED);
        til.setBoxBackgroundColor(Color.parseColor("#1E1E1E"));
        til.setHint("Item name");
        til.setHintTextColor(android.content.res.ColorStateList.valueOf(Color.parseColor("#B3B3B3")));

        TextInputEditText et = new TextInputEditText(this);
        et.setText(item.name);
        et.setTextColor(Color.WHITE);
        et.setHintTextColor(Color.parseColor("#B3B3B3"));
        til.addView(et);

        new MaterialAlertDialogBuilder(this)
                .setTitle("Edit item")
                .setView(til)
                .setNegativeButton("Cancel", (d, w) -> d.dismiss())
                .setPositiveButton("Save", (d, w) -> {
                    String newName = (et.getText() == null) ? "" : et.getText().toString().trim();
                    if (TextUtils.isEmpty(newName)) return;
                    item.name = newName;
                    storage.save(categories);
                    adapter.applyFilter(filter, query);
                    updateStats();
                })
                .show();
    }

    private void confirmDeleteItem(GroceryCategory category, GroceryItem item) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Delete item?")
                .setMessage("Remove \"" + item.name + "\" from this checklist?")
                .setNegativeButton("Cancel", (d, w) -> d.dismiss())
                .setPositiveButton("Delete", (d, w) -> {
                    if (category.items != null) category.items.remove(item);
                    storage.save(categories);
                    setupCategoryJumpDropdown();
                    adapter.applyFilter(filter, query);
                    updateStats();

                    View root = findViewById(R.id.root);
                    if (root == null) root = getWindow().getDecorView();
                    Snackbar.make(root, "Item deleted.", Snackbar.LENGTH_SHORT).show();
                })
                .show();
    }

    static class TextViewCompat {
        private final android.widget.TextView tv;
        TextViewCompat(android.widget.TextView tv) { this.tv = tv; }
        boolean ok() { return tv != null; }
        void setText(String s) { if (tv != null) tv.setText(s); }
    }
}
