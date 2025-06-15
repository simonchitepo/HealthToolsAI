package com.cypher.zealth;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class FacilityAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public interface OnFacilityClick {
        void onOpenOnMap(Facility facility);
    }

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_ITEM = 1;

    private final List<Row> rows = new ArrayList<>();
    private final OnFacilityClick listener;

    public FacilityAdapter(OnFacilityClick listener) {
        this.listener = listener;
    }

    private static class Row {
        final boolean isHeader;
        final String headerTitle;
        final Facility facility;

        Row(String headerTitle) {
            this.isHeader = true;
            this.headerTitle = headerTitle;
            this.facility = null;
        }

        Row(Facility facility) {
            this.isHeader = false;
            this.headerTitle = null;
            this.facility = facility;
        }
    }

    public void setData(List<Facility> featured, List<Facility> results) {
        rows.clear();

        if (featured != null && !featured.isEmpty()) {
            rows.add(new Row("Major hospitals (featured)"));
            for (Facility f : featured) rows.add(new Row(f));
        }

        rows.add(new Row("Search results"));
        if (results != null) {
            for (Facility f : results) rows.add(new Row(f));
        }

        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        return rows.get(position).isHeader ? TYPE_HEADER : TYPE_ITEM;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inf = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_HEADER) {
            View v = inf.inflate(android.R.layout.simple_list_item_1, parent, false);
            return new HeaderVH(v);
        } else {
            View v = inf.inflate(R.layout.item_facility_rich, parent, false);
            return new FacilityVH(v);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Row r = rows.get(position);

        if (holder instanceof HeaderVH) {
            ((HeaderVH) holder).title.setText(r.headerTitle);
            ((HeaderVH) holder).title.setTextColor(0xEFFFFFFF);
            ((HeaderVH) holder).title.setTextSize(14f);
            return;
        }

        FacilityVH h = (FacilityVH) holder;
        Facility f = r.facility;

        h.tvName.setText(f.name);

        String typeLabel = formatType(f.type);
        String dist = (f.distanceKm != null) ? String.format(Locale.ROOT, " • %.1fkm", f.distanceKm) : "";
        h.tvMeta.setText(typeLabel + dist);

        setOptionalText(h.tvAddress, f.address);
        setOptionalText(h.tvPhone, f.phone);

        if (f.capacityBeds != null) {
            h.tvCapacity.setText(String.format(Locale.ROOT, "Capacity: %d beds", f.capacityBeds));
        } else {
            h.tvCapacity.setText("Capacity: Not provided");
        }

        if (f.equipmentIndex != null) {
            h.progEquipment.setVisibility(View.VISIBLE);
            h.progEquipment.setProgressCompat(clamp01_100(f.equipmentIndex), true);
        } else {
            h.progEquipment.setVisibility(View.GONE);
        }

        if (f.stockIndex != null) {
            h.progStock.setVisibility(View.VISIBLE);
            h.progStock.setProgressCompat(clamp01_100(f.stockIndex), true);
        } else {
            h.progStock.setVisibility(View.GONE);
        }


        h.chipsDrugs.removeAllViews();
        if (f.majorDrugs != null && !f.majorDrugs.isEmpty()) {
            h.chipsDrugs.setVisibility(View.VISIBLE);
            Context ctx = h.itemView.getContext();
            for (String d : f.majorDrugs) {
                Chip c = new Chip(ctx);
                c.setText(d);
                c.setCheckable(false);
                c.setChipBackgroundColorResource(android.R.color.transparent);
                c.setChipStrokeWidth(1f);
                c.setChipStrokeColorResource(android.R.color.white);
                c.setTextColor(0xEFFFFFFF);
                h.chipsDrugs.addView(c);
            }
        } else {
            h.chipsDrugs.setVisibility(View.GONE);
        }

        h.btnOpenMap.setOnClickListener(v -> {
            if (listener != null) listener.onOpenOnMap(f);
        });

        h.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onOpenOnMap(f);
        });
    }

    private void setOptionalText(TextView tv, String value) {
        if (value == null || value.trim().isEmpty()) {
            tv.setVisibility(View.GONE);
        } else {
            tv.setVisibility(View.VISIBLE);
            tv.setText(value);
        }
    }

    private int clamp01_100(int v) {
        return Math.max(0, Math.min(100, v));
    }

    private String formatType(String t) {
        if (t == null) return "Facility";
        switch (t) {
            case "hospital": return "Hospital";
            case "clinic": return "Clinic";
            case "pharmacy": return "Pharmacy";
            default: return "Facility";
        }
    }

    @Override
    public int getItemCount() {
        return rows.size();
    }

    static class HeaderVH extends RecyclerView.ViewHolder {
        TextView title;
        HeaderVH(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(android.R.id.text1);
        }
    }

    static class FacilityVH extends RecyclerView.ViewHolder {
        TextView tvName, tvMeta, tvAddress, tvPhone, tvCapacity;
        MaterialButton btnOpenMap;
        LinearProgressIndicator progEquipment, progStock;
        ChipGroup chipsDrugs;

        FacilityVH(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvName);
            tvMeta = itemView.findViewById(R.id.tvMeta);
            tvAddress = itemView.findViewById(R.id.tvAddress);
            tvPhone = itemView.findViewById(R.id.tvPhone);
            tvCapacity = itemView.findViewById(R.id.tvCapacity);
            btnOpenMap = itemView.findViewById(R.id.btnOpenMap);
            progEquipment = itemView.findViewById(R.id.progEquipment);
            progStock = itemView.findViewById(R.id.progStock);
            chipsDrugs = itemView.findViewById(R.id.chipsDrugs);
        }
    }
}
