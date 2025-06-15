package com.cypher.zealth;

import android.graphics.Paint;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.checkbox.MaterialCheckBox;

import java.util.ArrayList;
import java.util.List;

public class GroceryItemAdapter extends RecyclerView.Adapter<GroceryItemAdapter.VH> {

    public enum Filter { ALL, UNCHECKED, CHECKED }

    public interface Listener {
        void onItemToggled(GroceryItem item);
        void onItemLongPressed(GroceryItem item);
    }

    private final List<GroceryItem> items;
    private final List<Integer> visible = new ArrayList<>();
    private final Listener listener;

    private Filter filter = Filter.ALL;
    private String query = "";

    public GroceryItemAdapter(List<GroceryItem> items, Listener listener) {
        this.items = (items == null) ? new ArrayList<>() : items;
        this.listener = listener;
        setHasStableIds(true);
        rebuildVisible();
    }

    @Override
    public long getItemId(int position) {
        int idx = visible.get(position);
        GroceryItem it = items.get(idx);
        String key = (it.name == null ? "item" : it.name) + "#" + idx;
        return key.hashCode();
    }

    public void submitFilter(Filter f, String q) {
        this.filter = (f == null) ? Filter.ALL : f;
        this.query = (q == null) ? "" : q.trim().toLowerCase();
        rebuildVisible();
        notifyDataSetChanged();
    }

    private void rebuildVisible() {
        visible.clear();
        for (int i = 0; i < items.size(); i++) {
            GroceryItem it = items.get(i);
            if (!matches(it)) continue;
            visible.add(i);
        }
    }

    private boolean matches(GroceryItem it) {
        if (it == null) return false;

        if (filter == Filter.UNCHECKED && it.checked) return false;
        if (filter == Filter.CHECKED && !it.checked) return false;

        if (!TextUtils.isEmpty(query)) {
            String n = (it.name == null) ? "" : it.name.toLowerCase();
            return n.contains(query);
        }
        return true;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_grocery_check, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        int idx = visible.get(position);
        GroceryItem it = items.get(idx);

        h.cb.setOnCheckedChangeListener(null);
        h.cb.setChecked(it.checked);

        h.tvTitle.setText(it.name == null ? "Item" : it.name);
        applyCheckedStyle(h.tvTitle, it.checked);

        View.OnClickListener toggle = v -> {
            boolean newVal = !it.checked;
            it.checked = newVal;
            h.cb.setChecked(newVal);
            applyCheckedStyle(h.tvTitle, newVal);
            if (listener != null) listener.onItemToggled(it);
        };

        h.itemView.setOnClickListener(toggle);

        h.itemView.setOnLongClickListener(v -> {
            if (listener != null) listener.onItemLongPressed(it);
            return true;
        });

        h.cb.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (it.checked != isChecked) {
                it.checked = isChecked;
                applyCheckedStyle(h.tvTitle, isChecked);
                if (listener != null) listener.onItemToggled(it);
            }
        });
    }

    private void applyCheckedStyle(TextView tv, boolean checked) {
        tv.setAlpha(checked ? 0.55f : 1f);
        if (checked) tv.setPaintFlags(tv.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        else tv.setPaintFlags(tv.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
    }

    @Override
    public int getItemCount() {
        return visible.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        MaterialCheckBox cb;
        TextView tvTitle;

        VH(@NonNull View itemView) {
            super(itemView);
            cb = itemView.findViewById(R.id.cbItem);
            tvTitle = itemView.findViewById(R.id.tvItemTitle);
        }
    }
}
