package com.cypher.zealth;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class WeightLogAdapter extends RecyclerView.Adapter<WeightLogAdapter.VH> {

    private final SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
    private List<WeightLogEntry> items;

    public WeightLogAdapter(List<WeightLogEntry> items) {
        this.items = items;
        setHasStableIds(true); 
    }

    public void setItems(List<WeightLogEntry> items) {
        this.items = items;
        notifyDataSetChanged();
    }

    @Override
    public long getItemId(int position) {

        return items.get(position).dateMs;
    }

    @NonNull
    @Override public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_weight_log, parent, false);
        return new VH(v);
    }

    @Override public void onBindViewHolder(@NonNull VH h, int position) {
        WeightLogEntry e = items.get(position);

        h.tvTitle.setText(String.format(Locale.US, "%.1f kg", e.weightKg));
        h.tvMeta.setText(df.format(new Date(e.dateMs)));

        String rel = relativeLabel(e.dateMs);
        if (rel != null) {
            h.tvRelative.setText(rel);
            h.tvRelative.setVisibility(View.VISIBLE);
        } else {
            h.tvRelative.setVisibility(View.GONE);
        }

        String note = (e.note == null) ? "" : e.note.trim();
        if (note.isEmpty()) {
            h.tvNote.setVisibility(View.GONE);
        } else {
            h.tvNote.setVisibility(View.VISIBLE);
            h.tvNote.setText(note);
        }
    }

    @Override public int getItemCount() { return items == null ? 0 : items.size(); }

    private String relativeLabel(long dateMs) {
        long today = startOfDay(System.currentTimeMillis());
        long yesterday = today - 24L * 60L * 60L * 1000L;

        if (dateMs == today) return "Today";
        if (dateMs == yesterday) return "Yesterday";
        return null;
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

    static class VH extends RecyclerView.ViewHolder {
        TextView tvTitle, tvMeta, tvRelative, tvNote;

        VH(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvMeta = itemView.findViewById(R.id.tvMeta);
            tvRelative = itemView.findViewById(R.id.tvRelative);
            tvNote = itemView.findViewById(R.id.tvNote);
        }
    }
}
