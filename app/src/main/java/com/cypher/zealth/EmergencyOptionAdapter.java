package com.cypher.zealth;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

public class EmergencyOptionAdapter extends RecyclerView.Adapter<EmergencyOptionAdapter.VH> {

    public interface OnCall {
        void call(EmergencyOption option);
    }

    private final List<EmergencyOption> items = new ArrayList<>();
    private final OnCall onCall;

    public EmergencyOptionAdapter(OnCall onCall) {
        this.onCall = onCall;
    }

    public void setItems(List<EmergencyOption> list) {
        items.clear();
        if (list != null) items.addAll(list);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_emergency_option, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        EmergencyOption e = items.get(position);
        h.tvTitle.setText(e.title);
        h.tvSubtitle.setText(e.subtitle);
        h.btnCall.setOnClickListener(v -> {
            if (onCall != null) onCall.call(e);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvTitle, tvSubtitle;
        MaterialButton btnCall;

        VH(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvSubtitle = itemView.findViewById(R.id.tvSubtitle);
            btnCall = itemView.findViewById(R.id.btnCall);
        }
    }
}
