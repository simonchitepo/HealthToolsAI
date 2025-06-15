package com.cypher.zealth;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class PregnancyWeekAdapter extends RecyclerView.Adapter<PregnancyWeekAdapter.VH> {

    private final List<PregnancyWeekItem> items = new ArrayList<>();

    public PregnancyWeekAdapter(List<PregnancyWeekItem> initial) {
        if (initial != null) items.addAll(initial);
    }

    public void setItems(List<PregnancyWeekItem> newItems) {
        items.clear();
        if (newItems != null) items.addAll(newItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_pregnancy_week, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        PregnancyWeekItem it = items.get(position);

        h.tvTitle.setText("Week " + it.week + " • Trimester " + it.trimester);
        h.tvTip.setText(it.tip);

        h.badgeCurrent.setVisibility(it.isCurrent ? View.VISIBLE : View.GONE);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvTitle, tvTip, badgeCurrent;

        VH(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvWeekTitle);
            tvTip = itemView.findViewById(R.id.tvWeekTip);
            badgeCurrent = itemView.findViewById(R.id.tvCurrentBadge);
        }
    }
}
