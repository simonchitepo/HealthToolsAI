package com.cypher.zealth;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PeriodHistoryAdapter extends RecyclerView.Adapter<PeriodHistoryAdapter.VH> {

    public interface Listener {
        void onEndPeriodClicked(WomensPeriodEntry entry);
        void onDeleteClicked(WomensPeriodEntry entry);
    }

    private final SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

    private final Listener listener;
    private final List<WomensPeriodEntry> items = new ArrayList<>();

    public PeriodHistoryAdapter(List<WomensPeriodEntry> initial, Listener listener) {
        this.listener = listener;
        if (initial != null) items.addAll(initial);
    }

    public void setItems(List<WomensPeriodEntry> newItems) {
        items.clear();
        if (newItems != null) items.addAll(newItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.row_period_history, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        WomensPeriodEntry e = items.get(position);

        String start = df.format(new Date(e.startDateMs));
        String end = (e.endDateMs > 0) ? df.format(new Date(e.endDateMs)) : "—";

        h.tvTitle.setText("Start: " + start);
        h.tvSub.setText("End: " + end);

        boolean ended = e.endDateMs > 0;
        h.btnEnd.setEnabled(!ended);
        h.btnEnd.setAlpha(ended ? 0.5f : 1f);

        h.btnEnd.setOnClickListener(v -> {
            if (listener != null) listener.onEndPeriodClicked(e);
        });

        h.btnDelete.setOnClickListener(v -> {
            if (listener != null) listener.onDeleteClicked(e);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvTitle, tvSub;
        MaterialButton btnEnd, btnDelete;

        VH(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvRowTitle);
            tvSub = itemView.findViewById(R.id.tvRowSub);
            btnEnd = itemView.findViewById(R.id.btnRowEnd);
            btnDelete = itemView.findViewById(R.id.btnRowDelete);
        }
    }
}
