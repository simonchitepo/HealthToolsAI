package com.cypher.zealth;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CravingAdapter extends RecyclerView.Adapter<CravingAdapter.VH> {

    private final SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US);
    private final List<CravingEntry> items = new ArrayList<>();

    public CravingAdapter(List<CravingEntry> initial) {
        if (initial != null) items.addAll(initial);
    }

    public void setItems(List<CravingEntry> newItems) {
        items.clear();
        if (newItems != null) items.addAll(newItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_craving_log, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        CravingEntry e = items.get(position);

        h.tvTitle.setText("Craving intensity: " + e.intensity1to5 + "/5");
        h.tvMeta.setText(df.format(new Date(e.timestampMs)));

        String note = e.note == null ? "" : e.note.trim();
        h.tvNote.setText(note.isEmpty() ? "No note" : note);
        h.tvNote.setAlpha(note.isEmpty() ? 0.75f : 1f);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvTitle, tvMeta, tvNote;

        VH(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvMeta = itemView.findViewById(R.id.tvMeta);
            tvNote = itemView.findViewById(R.id.tvNote);
        }
    }
}
