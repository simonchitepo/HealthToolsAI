package com.cypher.zealth;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MentalHistoryAdapter extends RecyclerView.Adapter<MentalHistoryAdapter.VH> {

    public interface BoolSupplier { boolean get(); }

    private final SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
    private List<MentalEntry> items;
    private final BoolSupplier hideSupplier;

    public MentalHistoryAdapter(List<MentalEntry> items, BoolSupplier hideSupplier) {
        this.items = items;
        this.hideSupplier = hideSupplier;
    }

    public void setItems(List<MentalEntry> items) {
        this.items = items;
        notifyDataSetChanged();
    }

    public MentalEntry getItemAt(int position) {
        if (items == null) return null;
        if (position < 0 || position >= items.size()) return null;
        return items.get(position);
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_mental_history, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        MentalEntry e = items.get(position);
        String date = df.format(new Date(e.timestampMs));
        boolean hide = hideSupplier != null && hideSupplier.get();

        if (e.type == MentalEntry.Type.MOOD) {
            h.tvTitle.setText("Mood check-in");
            h.tvMeta.setText(date + "  •  Mood: " + e.mood + "/5");

            String body;
            if (hide) body = "Preview hidden.";
            else {
                body = (e.note == null || e.note.trim().isEmpty()) ? "No note." : e.note.trim();
                if (e.tags != null && !e.tags.isEmpty()) {
                    body = body + "\nTags: " + TextUtils.join(", ", e.tags);
                }
            }
            h.tvBody.setText(body);

            h.chipType.setText("MOOD");
            h.chipType.setTextColor(Color.parseColor("#0B0B0B"));
            h.chipType.setChipBackgroundColor(ColorStateList.valueOf(moodChipColor(e.mood)));
        } else {
            h.tvTitle.setText("Journal entry");
            h.tvMeta.setText(date);

            String body = hide ? "Preview hidden." : (e.journalText == null ? "" : e.journalText.trim());
            h.tvBody.setText(body.isEmpty() ? "—" : body);

            h.chipType.setText("JOURNAL");
            h.chipType.setTextColor(Color.parseColor("#0B0B0B"));
            h.chipType.setChipBackgroundColor(ColorStateList.valueOf(Color.parseColor("#1DB954")));
        }
    }

    private int moodChipColor(int mood) {
        if (mood <= 2) return Color.parseColor("#FB7185"); // rose
        if (mood == 3) return Color.parseColor("#9CA3AF"); // gray
        return Color.parseColor("#1DB954"); // spotify green
    }

    @Override
    public int getItemCount() {
        return items == null ? 0 : items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvTitle, tvMeta, tvBody;
        Chip chipType;

        VH(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvMeta = itemView.findViewById(R.id.tvMeta);
            tvBody = itemView.findViewById(R.id.tvBody);
            chipType = itemView.findViewById(R.id.chipType);
        }
    }
}
