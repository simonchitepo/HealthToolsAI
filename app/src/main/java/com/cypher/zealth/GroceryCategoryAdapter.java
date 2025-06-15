package com.cypher.zealth;

import android.text.TextUtils;
import android.transition.AutoTransition;
import android.transition.TransitionManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;

public class GroceryCategoryAdapter extends RecyclerView.Adapter<GroceryCategoryAdapter.VH> {

    public interface Listener {
        void onDataChanged();
        void onItemEditDeleteRequested(GroceryCategory category, GroceryItem item);
    }

    private final List<GroceryCategory> all;
    private final List<GroceryCategory> display = new ArrayList<>();
    private final Listener listener;

    private GroceryItemAdapter.Filter filter = GroceryItemAdapter.Filter.ALL;
    private String query = "";

    private final RecyclerView.RecycledViewPool sharedPool = new RecyclerView.RecycledViewPool();

    public GroceryCategoryAdapter(List<GroceryCategory> categories, Listener listener) {
        this.all = (categories == null) ? new ArrayList<>() : categories;
        this.listener = listener;
        setHasStableIds(true);
        rebuildDisplay();
    }

    @Override
    public long getItemId(int position) {
        GroceryCategory c = display.get(position);
        return (c.title == null ? ("cat#" + position) : ("cat#" + c.title)).hashCode();
    }

    public int positionOfCategoryTitle(String title) {
        if (TextUtils.isEmpty(title)) return -1;
        for (int i = 0; i < display.size(); i++) {
            GroceryCategory c = display.get(i);
            if (c != null && TextUtils.equals(title, c.title)) return i;
        }
        return -1;
    }

    public void applyFilter(GroceryItemAdapter.Filter f, String q) {
        this.filter = (f == null) ? GroceryItemAdapter.Filter.ALL : f;
        this.query = (q == null) ? "" : q.trim().toLowerCase();
        rebuildDisplay();
        notifyDataSetChanged();
    }

    private boolean filteringActive() {
        return filter != GroceryItemAdapter.Filter.ALL || !TextUtils.isEmpty(query);
    }

    private void rebuildDisplay() {
        display.clear();

        for (GroceryCategory c : all) {
            if (c == null) continue;
            if (!filteringActive()) {
                display.add(c);
                continue;
            }
            if (hasAnyMatch(c)) display.add(c);
        }
    }

    private boolean hasAnyMatch(GroceryCategory c) {
        if (c.items == null) return false;
        for (GroceryItem it : c.items) {
            if (matches(it)) return true;
        }
        return false;
    }

    private boolean matches(GroceryItem it) {
        if (it == null) return false;
        if (filter == GroceryItemAdapter.Filter.UNCHECKED && it.checked) return false;
        if (filter == GroceryItemAdapter.Filter.CHECKED && !it.checked) return false;

        if (!TextUtils.isEmpty(query)) {
            String n = (it.name == null) ? "" : it.name.toLowerCase();
            return n.contains(query);
        }
        return true;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_grocery_category, parent, false);
        return new VH(v, sharedPool);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        GroceryCategory cat = display.get(position);

        h.tvTitle.setText(TextUtils.isEmpty(cat.title) ? "Category" : cat.title);

        int checked = cat.checkedCount();
        int total = cat.totalCount();
        h.tvCount.setText(checked + "/" + total);

        // Force open during filtering so results are immediately visible
        boolean expandedUi = filteringActive() || cat.expanded;

        h.content.setVisibility(expandedUi ? View.VISIBLE : View.GONE);
        h.addRow.setVisibility(expandedUi ? View.VISIBLE : View.GONE);

        h.ivChevron.setRotation(expandedUi ? 180f : 0f);

        // Nested adapter (keep one instance per VH)
        if (h.itemAdapter == null) {
            h.itemAdapter = new GroceryItemAdapter(cat.items, new GroceryItemAdapter.Listener() {
                @Override
                public void onItemToggled(GroceryItem item) {
                    if (listener != null) listener.onDataChanged();
                    // If filtering is active, toggling may affect visibility; rebuild.
                    if (filteringActive()) {
                        rebuildDisplay();
                        notifyDataSetChanged();
                    } else {
                        notifyItemChanged(h.getBindingAdapterPosition());
                    }
                }

                @Override
                public void onItemLongPressed(GroceryItem item) {
                    if (listener != null) listener.onItemEditDeleteRequested(cat, item);
                }
            });
            h.recyclerItems.setAdapter(h.itemAdapter);
        } else {
            // Update backing list reference safely if category object changed in this holder
            // (in practice stable ids keep it consistent, but we handle it anyway)
            h.recyclerItems.setAdapter(h.itemAdapter);
        }

        // Apply filter to nested
        h.itemAdapter.submitFilter(filter, query);

        View.OnClickListener toggle = v -> {
            int pos = h.getBindingAdapterPosition();
            if (pos == RecyclerView.NO_POSITION) return;

            // If filtering is active, we keep open (Spotify style: search expands results).
            if (filteringActive()) return;

            GroceryCategory c = display.get(pos);
            c.expanded = !c.expanded;

            TransitionManager.beginDelayedTransition(h.cardRoot, new AutoTransition());

            h.content.setVisibility(c.expanded ? View.VISIBLE : View.GONE);
            h.addRow.setVisibility(c.expanded ? View.VISIBLE : View.GONE);

            h.ivChevron.animate()
                    .rotation(c.expanded ? 180f : 0f)
                    .setDuration(160)
                    .start();

            if (listener != null) listener.onDataChanged();
        };

        h.headerRow.setOnClickListener(toggle);
        h.cardRoot.setOnClickListener(toggle);

        h.btnAdd.setOnClickListener(v -> {
            int pos = h.getBindingAdapterPosition();
            if (pos == RecyclerView.NO_POSITION) return;

            GroceryCategory c = display.get(pos);
            String name = (h.etAddItem.getText() == null) ? "" : h.etAddItem.getText().toString().trim();

            if (TextUtils.isEmpty(name)) {
                Toast.makeText(h.itemView.getContext(), "Enter an item name.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (c.items == null) c.items = new ArrayList<>();

            for (GroceryItem it : c.items) {
                if (it != null && it.name != null && it.name.equalsIgnoreCase(name)) {
                    Toast.makeText(h.itemView.getContext(), "Item already exists.", Toast.LENGTH_SHORT).show();
                    return;
                }
            }

            c.items.add(0, GroceryItem.of(name, false));
            h.etAddItem.setText("");

            if (listener != null) listener.onDataChanged();

            // Re-apply filter so the new item appears correctly
            applyFilter(filter, query);
        });
    }

    @Override
    public int getItemCount() {
        return display.size();
    }

    static class VH extends RecyclerView.ViewHolder {

        ViewGroup cardRoot;
        View headerRow;
        TextView tvTitle, tvCount;
        ImageView ivChevron;

        View content;
        RecyclerView recyclerItems;

        TextInputEditText etAddItem;
        MaterialButton btnAdd;
        ViewGroup addRow;

        GroceryItemAdapter itemAdapter;

        VH(@NonNull View itemView, RecyclerView.RecycledViewPool pool) {
            super(itemView);

            cardRoot = itemView.findViewById(R.id.cardRoot);
            headerRow = itemView.findViewById(R.id.headerRow);
            tvTitle = itemView.findViewById(R.id.tvCategoryTitle);
            tvCount = itemView.findViewById(R.id.tvCategoryCount);
            ivChevron = itemView.findViewById(R.id.ivChevron);

            content = itemView.findViewById(R.id.content);
            recyclerItems = itemView.findViewById(R.id.recyclerItems);
            recyclerItems.setLayoutManager(new LinearLayoutManager(itemView.getContext()));
            recyclerItems.setRecycledViewPool(pool);
            recyclerItems.setNestedScrollingEnabled(false);

            addRow = itemView.findViewById(R.id.addRow);
            etAddItem = itemView.findViewById(R.id.etAddItem);
            btnAdd = itemView.findViewById(R.id.btnAdd);
        }
    }
}
