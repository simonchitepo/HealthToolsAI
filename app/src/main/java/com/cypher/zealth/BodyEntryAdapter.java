package com.cypher.zealth;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.graphics.ColorUtils;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.color.MaterialColors;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class BodyEntryAdapter extends RecyclerView.Adapter<BodyEntryAdapter.VH> {

    public interface Listener {
        void onDelete(long createdAtMs);
    }

    private final SimpleDateFormat dfDate = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
    private final SimpleDateFormat dfDateTime = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US);

    private List<BodyEntry> items;
    private final Listener listener;

    public BodyEntryAdapter(List<BodyEntry> items, Listener listener) {
        this.items = items;
        this.listener = listener;
    }

    public void setItems(List<BodyEntry> items) {
        this.items = items;
        notifyDataSetChanged();
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_body_entry, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        BodyEntry e = items.get(position);

        String area = (e.area == null || e.area.trim().isEmpty()) ? "Other" : e.area.trim();
        int sev = Math.max(1, Math.min(5, e.severity));

        String title = area + " • Severity " + sev + "/5";
        h.tvTitle.setText(title);

        String meta = "Logged: " + dfDateTime.format(new Date(e.createdAtMs))
                + " • Started: " + dfDate.format(new Date(e.startedDateMs));
        h.tvMeta.setText(meta);

        String note = e.note == null ? "" : e.note.trim();
        h.tvPreview.setText(note.isEmpty() ? "No details." : note);

        // Severity badge
        h.tvSeverityBadge.setText(String.valueOf(sev));
        tintSeverityBadge(h.itemView.getContext(), h.tvSeverityBadge, sev);

        h.itemView.setOnClickListener(v -> {
            AlertDialog.Builder b = new AlertDialog.Builder(h.itemView.getContext());
            b.setTitle(title);
            b.setMessage(meta + "\n\n" + (note.isEmpty() ? "No details." : note));
            b.setPositiveButton("Close", (d, w) -> d.dismiss());
            b.show();
        });

        h.itemView.setOnLongClickListener(v -> {
            new AlertDialog.Builder(h.itemView.getContext())
                    .setTitle("Delete entry?")
                    .setMessage("This will remove the selected entry from this device.")
                    .setNegativeButton("Cancel", (d, w) -> d.dismiss())
                    .setPositiveButton("Delete", (d, w) -> {
                        if (listener != null) listener.onDelete(e.createdAtMs);
                    })
                    .show();
            return true;
        });
    }

    private void tintSeverityBadge(Context ctx, TextView badge, int sev1to5) {
      
        int primary = MaterialColors.getColor(badge, com.google.android.material.R.attr.colorOnPrimary, Color.WHITE);
        int surface = MaterialColors.getColor(badge, com.google.android.material.R.attr.colorSurface, Color.BLACK);

        float t;
        switch (sev1to5) {
            case 1: t = 0.25f; break;
            case 2: t = 0.35f; break;
            case 3: t = 0.50f; break;
            case 4: t = 0.65f; break;
            default: t = 0.80f; break;
        }

        int blended = ColorUtils.blendARGB(surface, primary, t);
      
        int withAlpha = ColorUtils.setAlphaComponent(blended, 210);
        badge.setBackgroundTintList(android.content.res.ColorStateList.valueOf(withAlpha));
    }

    @Override public int getItemCount() { return items == null ? 0 : items.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvTitle, tvMeta, tvPreview, tvSeverityBadge;
        VH(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvMeta = itemView.findViewById(R.id.tvMeta);
            tvPreview = itemView.findViewById(R.id.tvPreview);
            tvSeverityBadge = itemView.findViewById(R.id.tvSeverityBadge);
        }
    }
}
