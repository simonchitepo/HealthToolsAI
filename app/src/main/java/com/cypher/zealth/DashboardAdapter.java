package com.cypher.zealth;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.LruCache;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DashboardAdapter extends RecyclerView.Adapter<DashboardAdapter.VH> {

    private final Context appContext;

    private final List<DashboardItem> allItems;
    private final List<DashboardItem> shownItems;

    private final Random random = new Random(7);

    private final ExecutorService decodeExecutor = Executors.newFixedThreadPool(2);
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private final LruCache<String, Bitmap> bitmapCache;

   
    private final ConcurrentHashMap<String, Boolean> inFlight = new ConcurrentHashMap<>();

  
    private final Drawable glossDrawable;
    private final Drawable scrimDrawable;

    public DashboardAdapter(Context context, List<DashboardItem> items) {
        this.appContext = context.getApplicationContext();
        this.allItems = new ArrayList<>(items);
        this.shownItems = new ArrayList<>(items);

        int maxKb = (int) (Runtime.getRuntime().maxMemory() / 1024);
        int cacheKb = Math.max(8 * 1024, maxKb / 8); // min 8MB, else 1/8 heap

        this.bitmapCache = new LruCache<String, Bitmap>(cacheKb) {
            @Override
            protected int sizeOf(@NonNull String key, @NonNull Bitmap value) {
                return value.getByteCount() / 1024;
            }
        };

        this.glossDrawable = makeGlossDrawable();
        this.scrimDrawable = makeBottomScrimDrawable();

        setHasStableIds(true);
    }

    @Override
    public long getItemId(int position) {
        DashboardItem item = shownItems.get(position);
        String k = (item.type == DashboardItem.TYPE_HEADER)
                ? ("H|" + safe(item.headerText))
                : ("T|" + safe(item.title) + "|" + safe(item.subtitle) + "|" + safe(item.backgroundDrawableName) + "|" + item.style);
        return k.hashCode();
    }

    public int getSpanSize(int position, int spanCount) {
        return shownItems.get(position).spanSize(spanCount);
    }

    @Override
    public int getItemViewType(int position) {
        return shownItems.get(position).type;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_dashboard_tile, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        DashboardItem item = shownItems.get(position);

        h.tileCard.setOnClickListener(null);
        h.bgImage.setImageDrawable(null);
        h.bgImage.setTag(null);

        if (item.type == DashboardItem.TYPE_HEADER) {
            h.headerText.setVisibility(View.VISIBLE);
            h.tileCard.setVisibility(View.GONE);
            h.headerText.setText(item.headerText);
            return;
        }


        h.headerText.setVisibility(View.GONE);
        h.tileCard.setVisibility(View.VISIBLE);

        h.title.setText(item.title);
        h.subtitle.setText(item.subtitle);

        ViewGroup.LayoutParams lp = h.tileCard.getLayoutParams();
        lp.height = dpToPx(item.heightDp());
        h.tileCard.setLayoutParams(lp);

        h.gloss.setBackground(glossDrawable);
        h.bottomScrim.setBackground(scrimDrawable);

        h.tileCard.setOnClickListener(v -> {
            if (item.targetActivity != null) {
                Context c = v.getContext();
                c.startActivity(new Intent(c, item.targetActivity));
            }
        });

        applyTileBackgroundAsync(h.bgImage, item.backgroundDrawableName);
    }

    @Override
    public int getItemCount() {
        return shownItems.size();
    }

    public void filter(String query) {
        String q = (query == null) ? "" : query.trim().toLowerCase(Locale.ROOT);
        shownItems.clear();

        if (q.isEmpty()) {
            shownItems.addAll(allItems);
            notifyDataSetChanged();
            return;
        }

        DashboardItem pendingHeader = null;
        boolean headerAdded = false;

        for (DashboardItem item : allItems) {
            if (item.type == DashboardItem.TYPE_HEADER) {
                pendingHeader = item;
                headerAdded = false;
                continue;
            }

            String hay = (safe(item.title) + " " + safe(item.subtitle)).toLowerCase(Locale.ROOT);
            if (hay.contains(q)) {
                if (pendingHeader != null && !headerAdded) {
                    shownItems.add(pendingHeader);
                    headerAdded = true;
                }
                shownItems.add(item);
            }
        }

        notifyDataSetChanged();
    }

    private void applyTileBackgroundAsync(ImageView target, String drawableName) {
        if (TextUtils.isEmpty(drawableName)) {
            target.setImageDrawable(makeFallbackGradient());
            return;
        }

        final String name = drawableName.trim();
        final int resId = target.getResources().getIdentifier(name, "drawable", appContext.getPackageName());

        if (resId == 0) {
            target.setImageDrawable(makeFallbackGradient());
            return;
        }

        target.setImageDrawable(makeFallbackGradient());

       
        int vw = target.getWidth();
        int vh = target.getHeight();
        if (vw <= 0 || vh <= 0) {
            target.post(() -> applyTileBackgroundAsync(target, name));
            return;
        }

      
        final int reqW = clamp(vw, 64, 1400);
        final int reqH = clamp(vh, 64, 1400);

        final String cacheKey = name + "@" + reqW + "x" + reqH;

        target.setTag(cacheKey);

      
        Bitmap cached = bitmapCache.get(cacheKey);
        if (cached != null && !cached.isRecycled()) {
            target.setImageBitmap(cached);
            return;
        }

        if (inFlight.putIfAbsent(cacheKey, true) != null) {
            return;
        }

        WeakReference<ImageView> ref = new WeakReference<>(target);

        decodeExecutor.execute(() -> {
            Bitmap bmp = null;
            try {
                bmp = decodeSampledBitmapFromResource(resId, reqW, reqH);
            } catch (Throwable ignored) {}

            final Bitmap finalBmp = bmp;

            mainHandler.post(() -> {
                inFlight.remove(cacheKey);

                ImageView iv = ref.get();
                if (iv == null) return;

                Object tag = iv.getTag();
                if (!(tag instanceof String) || !cacheKey.equals(tag)) return;

                if (finalBmp != null && !finalBmp.isRecycled()) {
                    bitmapCache.put(cacheKey, finalBmp);
                    iv.setImageBitmap(finalBmp);
                } else {
                    iv.setImageDrawable(makeFallbackGradient());
                }
            });
        });
    }

    private Bitmap decodeSampledBitmapFromResource(int resId, int reqWidth, int reqHeight) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeResource(appContext.getResources(), resId, options);

        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

        options.inJustDecodeBounds = false;
        options.inPreferredConfig = Bitmap.Config.RGB_565; // lower memory footprint
        options.inDither = true;

        return BitmapFactory.decodeResource(appContext.getResources(), resId, options);
    }

    private int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;

        int inSampleSize = 1;
        if (height > reqHeight || width > reqWidth) {
            int halfHeight = height / 2;
            int halfWidth = width / 2;

            while ((halfHeight / inSampleSize) >= reqHeight
                    && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }
        return Math.max(1, inSampleSize);
    }

    private Drawable makeFallbackGradient() {
        int c1 = Color.HSVToColor(new float[]{random.nextInt(360), 0.30f, 0.90f});
        int c2 = Color.HSVToColor(new float[]{random.nextInt(360), 0.35f, 0.70f});
        GradientDrawable gd = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{c1, c2}
        );
        gd.setCornerRadius(dpToPx(18));
        return gd;
    }

    private Drawable makeGlossDrawable() {
        GradientDrawable gd = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{
                        Color.argb(110, 255, 255, 255),
                        Color.argb(35, 255, 255, 255),
                        Color.argb(0, 255, 255, 255)
                }
        );
        gd.setCornerRadius(dpToPx(18));
        return gd;
    }

    private Drawable makeBottomScrimDrawable() {
        return new GradientDrawable(
                GradientDrawable.Orientation.BOTTOM_TOP,
                new int[]{
                        Color.argb(0, 0, 0, 0),
                        Color.argb(0, 0, 0, 0),
                        Color.argb(0, 0, 0, 0)
                }
        );
    }

    private int dpToPx(int dp) {
        float d = appContext.getResources().getDisplayMetrics().density;
        return (int) (dp * d + 0.5f);
    }

    private static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }

   
    public void shutdown() {
        decodeExecutor.shutdownNow();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView headerText;

        MaterialCardView tileCard;
        ImageView bgImage;
        View gloss;
        View bottomScrim;
        TextView title;
        TextView subtitle;

        VH(@NonNull View itemView) {
            super(itemView);

            headerText = itemView.findViewById(R.id.txtHeader);

            tileCard = itemView.findViewById(R.id.cardTile);
            bgImage = itemView.findViewById(R.id.imgBg);
            gloss = itemView.findViewById(R.id.viewGloss);
            bottomScrim = itemView.findViewById(R.id.viewBottomScrim);
            title = itemView.findViewById(R.id.txtTitle);
            subtitle = itemView.findViewById(R.id.txtSubtitle);
        }
    }
}
