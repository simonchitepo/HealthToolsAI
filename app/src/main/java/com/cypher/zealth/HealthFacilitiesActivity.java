package com.cypher.zealth;
import android.view.ViewGroup;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.graphics.ColorUtils;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.AsyncListDiffer;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.Cache;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;
import retrofit2.http.Url;

public class HealthFacilitiesActivity extends AppCompatActivity {


    private static final int C_BG        = Color.parseColor("#121212");
    private static final int C_SURFACE   = Color.parseColor("#181818");
    private static final int C_SURFACE_2 = Color.parseColor("#202020");
    private static final int C_STROKE    = Color.parseColor("#2A2A2A");

    private static final int C_TEXT  = Color.WHITE;
    private static final int C_SUB   = Color.parseColor("#B3B3B3");
    private static final int C_MUTED = Color.parseColor("#8A8A8A");

    private static final int C_GREEN      = Color.parseColor("#1DB954");
    private static final int C_GREEN_DARK = Color.parseColor("#14833E");

    private static final int REQ_LOCATION = 3011;

    private static final int[] RADIUS_STEPS_M = new int[]{
            2000, 5000, 10000, 15000, 25000, 50000, 75000, 100000
    };
    private int radiusIndex = 4; 
    private static final int AUTO_EXPAND_MAX_STEPS = 2;

    private static final int MIN_RESULTS_SOFT = 8;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable pendingSearch;
    private static final long DEBOUNCE_MS = 420;

    private View root;
    private View topGlow;
    private MaterialToolbar toolbar;
    private TextView titleText;
    private TextView metaText;
    private SearchView searchView;
    private ChipGroup chipGroup;
    private Chip chipHospitals, chipClinics, chipPharmacies;
    private RecyclerView recycler;
    private View emptyState;
    private TextView emptyTitle;
    private TextView emptySubtitle;
    private View loadingOverlay;
    private TextView loadingText;
    private CircularProgressIndicator loadingSpinner;
    private LinearProgressIndicator topProgress;
    private FacilityAdapter adapter;
    private NominatimApiService nominatim;
    private final List<OverpassApiService> overpassServices = new ArrayList<>();
    private int overpassIndex = 0;

    private final List<Call<?>> inFlight = new ArrayList<>();

    private final ExecutorService worker = Executors.newSingleThreadExecutor();

    private final List<Facility> results = new ArrayList<>();
    private final Map<String, Facility> dedupe = new HashMap<>();

    private final Map<String, List<Facility>> cache = new HashMap<>();

    private double currentLat = Double.NaN;
    private double currentLon = Double.NaN;
    private String currentAnchorLabel = null;

    private boolean isLoading = false;
    private boolean loadingMore = false;

    private static final String PREFS = "hf_spotify_cache";
    private static final String KEY_LAST_SNAPSHOT = "last_snapshot_json";
    private static final long SNAPSHOT_MAX_AGE_MS = 6L * 60L * 60L * 1000L; // 6 hours
    private final Gson gson = new Gson();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_health_facilities_spotify);

        bindUi();
        styleSpotifyChrome();
        initRecycler();
        initNetworking();

        if (!handleIncomingCenterExtras()) {

            prefillFromLastSnapshotIfFresh();
        }

        adapter.setFooterState(canLoadMore(), false);
        updateEmptyState();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cancelAllInFlight();
        worker.shutdownNow();
    }


    private void bindUi() {
        root = findViewById(R.id.root);
        topGlow = findViewById(R.id.topGlow);

        toolbar = findViewById(R.id.toolbar);
        titleText = findViewById(R.id.titleText);
        metaText = findViewById(R.id.metaText);

        searchView = findViewById(R.id.searchView);
        chipGroup = findViewById(R.id.chipGroup);
        chipHospitals = findViewById(R.id.chipHospitals);
        chipClinics = findViewById(R.id.chipClinics);
        chipPharmacies = findViewById(R.id.chipPharmacies);

        recycler = findViewById(R.id.recyclerFacilities);

        emptyState = findViewById(R.id.emptyState);
        emptyTitle = findViewById(R.id.emptyTitle);
        emptySubtitle = findViewById(R.id.emptySubtitle);

        loadingOverlay = findViewById(R.id.loadingOverlay);
        loadingText = findViewById(R.id.loadingText);
        loadingSpinner = findViewById(R.id.progress);

        topProgress = findViewById(R.id.topProgress);

        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets sys = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            View appBarContainer = findViewById(R.id.appBarContainer);
            if (appBarContainer != null) {
                appBarContainer.setPadding(
                        appBarContainer.getPaddingLeft(),
                        sys.top + dp(8),
                        appBarContainer.getPaddingRight(),
                        appBarContainer.getPaddingBottom()
                );
            }
            View contentContainer = findViewById(R.id.contentContainer);
            if (contentContainer != null) {
                contentContainer.setPadding(
                        contentContainer.getPaddingLeft(),
                        contentContainer.getPaddingTop(),
                        contentContainer.getPaddingRight(),
                        sys.bottom + dp(10)
                );
            }
            return insets;
        });

        toolbar.setNavigationIcon(R.drawable.ic_back_medical);
        toolbar.setNavigationIconTint(C_TEXT);
        toolbar.setTitle("");
        toolbar.setOnClickListener(null);
        toolbar.setNavigationOnClickListener(v -> finish());

  
        toolbar.inflateMenu(R.menu.menu_health_facilities_spotify);
        toolbar.getMenu().findItem(R.id.action_near_me).setOnMenuItemClickListener(item -> {
            startNearMeFlow();
            return true;
        });

        titleText.setText("Facilities");
        metaText.setText("Search a city/country, then filter hospitals/clinics/pharmacies.");

        setupSearchView();
        setupChips();
    }

    private void styleSpotifyChrome() {
        root.setBackgroundColor(C_BG);

        GradientDrawable glow = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{
                        ColorUtils.setAlphaComponent(C_GREEN, 90),
                        ColorUtils.setAlphaComponent(C_GREEN, 0)
                }
        );
        glow.setCornerRadius(0);
        topGlow.setBackground(glow);
    }

    private void setupSearchView() {
        searchView.setIconifiedByDefault(false);
        searchView.setMaxWidth(Integer.MAX_VALUE);
        searchView.setQueryHint("Search city, country, or region (e.g., London)");
        forceSearchViewColors();

        try {
            TextView searchText = searchView.findViewById(androidx.appcompat.R.id.search_src_text);
            if (searchText != null) {
                searchText.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
                searchText.setSingleLine(true);
                searchText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
                searchText.setTypeface(Typeface.DEFAULT);
                searchText.setOnEditorActionListener((v, actionId, event) -> {
                    boolean isSearch = actionId == EditorInfo.IME_ACTION_SEARCH;
                    boolean isEnter = event != null
                            && event.getKeyCode() == KeyEvent.KEYCODE_ENTER
                            && event.getAction() == KeyEvent.ACTION_DOWN;
                    if (isSearch || isEnter) {
                        submitSearchNow();
                        return true;
                    }
                    return false;
                });
            }
        } catch (Exception ignored) { }

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override public boolean onQueryTextSubmit(String query) {
                submitSearchNow();
                return true;
            }
            @Override public boolean onQueryTextChange(String newText) {
                scheduleDebouncedSearch(newText);
                return true;
            }
        });
    }

    private void forceSearchViewColors() {
        try {
            TextView src = searchView.findViewById(androidx.appcompat.R.id.search_src_text);
            if (src != null) {
                src.setTextColor(C_TEXT);
                src.setHintTextColor(ColorUtils.setAlphaComponent(C_TEXT, 140));
            }
            View plate = searchView.findViewById(androidx.appcompat.R.id.search_plate);
            if (plate != null) plate.setBackgroundColor(Color.TRANSPARENT);

            ImageView mag = searchView.findViewById(androidx.appcompat.R.id.search_mag_icon);
            if (mag != null) mag.setImageTintList(ColorStateList.valueOf(C_SUB));

            ImageView close = searchView.findViewById(androidx.appcompat.R.id.search_close_btn);
            if (close != null) close.setImageTintList(ColorStateList.valueOf(C_SUB));
        } catch (Exception ignored) { }
    }

    private void setupChips() {
        chipGroup.setSingleSelection(true);
        chipGroup.setSelectionRequired(true);

        styleChip(chipHospitals, true);
        styleChip(chipClinics, false);
        styleChip(chipPharmacies, false);

        View.OnClickListener chipListener = v -> {
            if (Double.isNaN(currentLat) || Double.isNaN(currentLon)) {
                String q = getQuery().trim();
                if (!q.isEmpty()) runGlobalSearchOptimized(q);
                else Toast.makeText(this, "Search a location or use Near Me.", Toast.LENGTH_SHORT).show();
                return;
            }


            cancelAllInFlight();
            results.clear();
            dedupe.clear();
            cache.clear();

            setLoading(true, "Refreshing…");
            fetchAroundWithRetryAndAutoExpand(currentLat, currentLon, radiusIndex, false, 0, null);
        };

        chipHospitals.setOnClickListener(chipListener);
        chipClinics.setOnClickListener(chipListener);
        chipPharmacies.setOnClickListener(chipListener);
    }

    private void styleChip(Chip chip, boolean checked) {
        chip.setCheckable(true);
        chip.setChecked(checked);
        chip.setEnsureMinTouchTargetSize(false);

        int[][] states = new int[][]{
                new int[]{android.R.attr.state_checked},
                new int[]{}
        };

        int checkedBg = ColorUtils.setAlphaComponent(C_GREEN, 255);
        int uncheckedBg = C_SURFACE_2;

        int checkedFg = Color.BLACK;
        int uncheckedFg = C_TEXT;

        int[] bg = new int[]{checkedBg, uncheckedBg};
        int[] fg = new int[]{checkedFg, uncheckedFg};

        chip.setChipBackgroundColor(new ColorStateList(states, bg));
        chip.setTextColor(new ColorStateList(states, fg));
        chip.setChipStrokeColor(ColorStateList.valueOf(C_STROKE));
        chip.setChipStrokeWidth(dpF(1));
        chip.setRippleColor(ColorStateList.valueOf(ColorUtils.setAlphaComponent(Color.WHITE, 40)));
        chip.setCheckedIconVisible(false);
        chip.setChipIconVisible(false);
        chip.setCloseIconVisible(false);
    }

    private void initRecycler() {
        recycler.setLayoutManager(new LinearLayoutManager(this));
        recycler.setHasFixedSize(true);
        recycler.setItemViewCacheSize(16);

        adapter = new FacilityAdapter(
                this::onFacilityClicked,
                this::openTurnByTurnNavigation,
                this::callPhone,
                this::openWebsite,
                this::sendEmail,
                this::loadMore
        );
        recycler.setAdapter(adapter);
    }


    private void initNetworking() {
     
        Cache cache = new Cache(getCacheDir(), 10L * 1024L * 1024L);

        OkHttpClient client = new OkHttpClient.Builder()
                .cache(cache)
                .addInterceptor((Interceptor) chain -> {
                    Request req = chain.request().newBuilder()
                            .header("User-Agent", "Zealth/1.0 (Android)")
                            .build();
                    return chain.proceed(req);
                })
                .build();

        nominatim = new Retrofit.Builder()
                .baseUrl("https://nominatim.openstreetmap.org/")
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(NominatimApiService.class);

        overpassServices.clear();
        overpassServices.add(new Retrofit.Builder()
                .baseUrl("https://overpass-api.de/")
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(OverpassApiService.class));

        overpassServices.add(new Retrofit.Builder()
                .baseUrl("https://overpass.kumi.systems/")
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(OverpassApiService.class));

        overpassServices.add(new Retrofit.Builder()
                .baseUrl("https://overpass.nchc.org.tw/")
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(OverpassApiService.class));
    }
    private boolean handleIncomingCenterExtras() {
        Intent i = getIntent();
        if (i == null) return false;

        if (i.hasExtra("center_lat") && i.hasExtra("center_lon")) {
            double lat = i.getDoubleExtra("center_lat", Double.NaN);
            double lon = i.getDoubleExtra("center_lon", Double.NaN);
            int radius = i.getIntExtra("radius_m", 15000);

            if (!Double.isNaN(lat) && !Double.isNaN(lon)) {
                currentLat = lat;
                currentLon = lon;
                currentAnchorLabel = "map center";
                radiusIndex = pickNearestRadiusIndex(radius);

                cancelAllInFlight();
                results.clear();
                dedupe.clear();
                cache.clear();

                setMetaText("Searching near map center • " + (RADIUS_STEPS_M[radiusIndex] / 1000) + " km");
                setLoading(true, "Searching around map center…");
                fetchAroundWithRetryAndAutoExpand(currentLat, currentLon, radiusIndex, false, 0, null);
                return true;
            }
        }
        return false;
    }

    private int pickNearestRadiusIndex(int radiusM) {
        int bestIdx = 0;
        int bestDiff = Integer.MAX_VALUE;
        for (int idx = 0; idx < RADIUS_STEPS_M.length; idx++) {
            int diff = Math.abs(RADIUS_STEPS_M[idx] - radiusM);
            if (diff < bestDiff) {
                bestDiff = diff;
                bestIdx = idx;
            }
        }
        return bestIdx;
    }


    private void scheduleDebouncedSearch(String text) {
        if (pendingSearch != null) handler.removeCallbacks(pendingSearch);

        final String q = text == null ? "" : text.trim();
        if (q.isEmpty()) return;

        pendingSearch = () -> runGlobalSearchOptimized(q);
        handler.postDelayed(pendingSearch, DEBOUNCE_MS);
    }

    private void submitSearchNow() {
        String q = getQuery().trim();
        if (q.isEmpty()) return;
        runGlobalSearchOptimized(q);
        searchView.clearFocus();
    }

    private String getQuery() {
        return (searchView.getQuery() == null) ? "" : searchView.getQuery().toString();
    }


    private void runGlobalSearchOptimized(String query) {
        if (isLoading) return;

        cancelAllInFlight();

        results.clear();
        dedupe.clear();
        cache.clear();
        adapter.submitData(Collections.emptyList(), canLoadMore(), false, true);
        updateEmptyState();

        setMetaText("Geocoding: " + safe(query));
        setLoading(true, "Finding best match…");

        Call<List<NominatimPlace>> c = nominatim.search(query, "json", 3);
        track(c);
        c.enqueue(new Callback<List<NominatimPlace>>() {
            @Override
            public void onResponse(@NonNull Call<List<NominatimPlace>> call,
                                   @NonNull Response<List<NominatimPlace>> response) {
                untrack(call);

                if (!response.isSuccessful() || response.body() == null || response.body().isEmpty()) {
                    setLoading(false, null);
                    setEmptyMessage("Location not found", "Try a bigger city, country, or region.");
                    adapter.submitData(Collections.emptyList(), false, false, false);
                    updateEmptyState();
                    return;
                }

                tryCandidate(response.body(), 0);
            }

            @Override
            public void onFailure(@NonNull Call<List<NominatimPlace>> call, @NonNull Throwable t) {
                untrack(call);
                setLoading(false, null);
                setEmptyMessage("Network error", "Check internet and try again.");
                adapter.submitData(Collections.emptyList(), false, false, false);
                updateEmptyState();
            }
        });
    }

    private void tryCandidate(List<NominatimPlace> candidates, int index) {
        if (index >= candidates.size()) {
            setLoading(false, null);
            setEmptyMessage("No facilities found", "Try another nearby city or use Near Me.");
            adapter.submitData(new ArrayList<>(results), canLoadMore(), false, false);
            updateEmptyState();
            return;
        }

        NominatimPlace p = candidates.get(index);
        double lat, lon;
        try {
            lat = Double.parseDouble(p.lat);
            lon = Double.parseDouble(p.lon);
        } catch (Exception e) {
            tryCandidate(candidates, index + 1);
            return;
        }

        currentLat = lat;
        currentLon = lon;
        currentAnchorLabel = p.display_name;
        radiusIndex = 3;

        results.clear();
        dedupe.clear();

        setMetaText("Searching near: " + safe(p.display_name) + " • " + (RADIUS_STEPS_M[radiusIndex] / 1000) + " km");
        setLoading(true, "Searching facilities…");

        fetchAroundWithRetryAndAutoExpand(lat, lon, radiusIndex, false, 0, () -> {

            if (results.isEmpty()) tryCandidate(candidates, index + 1);
        });
    }


    private void fetchAroundWithRetryAndAutoExpand(double lat, double lon, int radiusIdx,
                                                   boolean append, int autoStep, Runnable done) {
        if (radiusIdx < 0) radiusIdx = 0;
        if (radiusIdx >= RADIUS_STEPS_M.length) radiusIdx = RADIUS_STEPS_M.length - 1;

        isLoading = true;

        FacilityFilter filter = selectedFilter();
        int radiusMeters = RADIUS_STEPS_M[radiusIdx];

        String cacheKey = cacheKey(lat, lon, radiusMeters, filter);
        List<Facility> cached = cache.get(cacheKey);

        if (cached != null) {
            isLoading = false;
            setLoading(false, null);

            if (!append) {
                results.clear();
                dedupe.clear();
            }
            for (Facility f : cached) {
                if (!dedupe.containsKey(f.id)) {
                    dedupe.put(f.id, f);
                    results.add(f);
                }
            }

            updateMeta(radiusMeters, 0);
            pushResultsToUi(false);
            if (done != null) done.run();
            return;
        }

        String ql = buildOverpassQuery(lat, lon, radiusMeters, filter);

        overpassIndex = 0;
        int finalRadiusIdx = radiusIdx;

        callOverpassWithFallback(ql, new OverpassCallback() {
            @Override public void onSuccess(List<OverpassElement> elements) {
          
                worker.execute(() -> {
                    List<Facility> newFacilities = new ArrayList<>();
                    int added = 0;

                    if (elements != null) {
                        for (OverpassElement e : elements) {
                            if (e == null || e.tags == null) continue;

                            String id = "osm_" + e.type + "_" + e.id;
                            if (!append && dedupe.containsKey(id)) continue; // if appending, dedupe still applies

                            double fLat = e.getLat();
                            double fLon = e.getLon();
                            if (fLat == 0d && fLon == 0d) continue;

                            Map<String, String> tags = e.tags;

                            String name = bestName(tags);
                            String type = forceTypeByChip(bestType(tags));
                            String addr = buildAddress(tags);

                            String phone = bestPhone(tags);
                            String website = bestWebsite(tags);
                            String email = bestEmail(tags);

                            double distM = haversineMeters(lat, lon, fLat, fLon);

                            Facility f = new Facility(id, name, type, fLat, fLon, addr, phone, website, email, distM);
                            newFacilities.add(f);
                        }
                    }

                    runOnUiThread(() -> {
                        isLoading = false;
                        setLoading(false, null);

                        if (!append) {
                            results.clear();
                            dedupe.clear();
                        }

                        int aded = 0; 

                        for (Facility f : newFacilities) {
                            if (!dedupe.containsKey(f.id)) {
                                dedupe.put(f.id, f);
                                results.add(f);
                                aded++;
                            }
                        }

                        Collections.sort(results, (a, b) -> {
                            int d = Double.compare(a.distanceMeters, b.distanceMeters);
                            if (d != 0) return d;
                            String an = (a.name == null ? "" : a.name);
                            String bn = (b.name == null ? "" : b.name);
                            return an.compareToIgnoreCase(bn);
                        });

                        cache.put(cacheKey, new ArrayList<>(newFacilities));

                        updateMeta(radiusMeters, added);

                        if (results.isEmpty() && autoStep < AUTO_EXPAND_MAX_STEPS && finalRadiusIdx < RADIUS_STEPS_M.length - 1) {
                            int nextIdx = Math.min(finalRadiusIdx + 1, RADIUS_STEPS_M.length - 1);
                            setLoading(true, "No results yet… expanding radius…");
                            fetchAroundWithRetryAndAutoExpand(lat, lon, nextIdx, append, autoStep + 1, done);
                            return;
                        }


                        if (!append && results.size() < MIN_RESULTS_SOFT && autoStep == 0 && finalRadiusIdx < RADIUS_STEPS_M.length - 1) {
                            int nextIdx = Math.min(finalRadiusIdx + 1, RADIUS_STEPS_M.length - 1);
                            setLoading(true, "Finding more nearby places…");
                            fetchAroundWithRetryAndAutoExpand(lat, lon, nextIdx, false, autoStep + 1, done);
                            return;
                        }

                        pushResultsToUi(false);
                        persistLastSnapshot();
                        if (done != null) done.run();
                    });

                });
            }

            @Override public void onFailure(String msg) {
                isLoading = false;
                setLoading(false, null);

                setEmptyMessage("Service busy", msg == null ? "Try again in a moment." : msg);
                pushResultsToUi(false);
                if (done != null) done.run();
            }
        });
    }

    private void pushResultsToUi(boolean skeletonLoading) {
        adapter.submitData(new ArrayList<>(results), canLoadMore(), loadingMore, skeletonLoading);
        updateEmptyState();
    }

    private void callOverpassWithFallback(String ql, OverpassCallback cb) {
        if (overpassServices.isEmpty()) {
            cb.onFailure("No Overpass servers configured.");
            return;
        }
        if (overpassIndex >= overpassServices.size()) {
            cb.onFailure("Overpass servers are busy. Try again shortly.");
            return;
        }

        OverpassApiService svc = overpassServices.get(overpassIndex);

        Call<OverpassResponse> c = svc.interpreter(ql);
        track(c);
        c.enqueue(new Callback<OverpassResponse>() {
            @Override
            public void onResponse(@NonNull Call<OverpassResponse> call,
                                   @NonNull Response<OverpassResponse> response) {
                untrack(call);

                if (!response.isSuccessful() || response.body() == null) {
                    overpassIndex++;
                    callOverpassWithFallback(ql, cb);
                    return;
                }
                cb.onSuccess(response.body().elements);
            }

            @Override
            public void onFailure(@NonNull Call<OverpassResponse> call, @NonNull Throwable t) {
                untrack(call);
                overpassIndex++;
                callOverpassWithFallback(ql, cb);
            }
        });
    }

    private interface OverpassCallback {
        void onSuccess(List<OverpassElement> elements);
        void onFailure(String msg);
    }

    private boolean canLoadMore() {
        return radiusIndex < RADIUS_STEPS_M.length - 1
                && !Double.isNaN(currentLat) && !Double.isNaN(currentLon);
    }

    private void loadMore() {
        if (isLoading || loadingMore) return;

        if (Double.isNaN(currentLat) || Double.isNaN(currentLon)) {
            Toast.makeText(this, "Search a location or use Near Me.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (radiusIndex >= RADIUS_STEPS_M.length - 1) {
            Toast.makeText(this, "Max radius reached.", Toast.LENGTH_SHORT).show();
            adapter.setFooterState(false, false);
            return;
        }

        radiusIndex++;
        loadingMore = true;
        adapter.setFooterState(canLoadMore(), true);

        setLoading(true, "Loading more (" + (RADIUS_STEPS_M[radiusIndex] / 1000) + "km)…");
        fetchAroundWithRetryAndAutoExpand(currentLat, currentLon, radiusIndex, true, 0, () -> {
            loadingMore = false;
            adapter.setFooterState(canLoadMore(), false);
        });
    }

    private void onFacilityClicked(Facility f) {
        try {
            Intent i = new Intent(this, MapActivity.class);
            i.putExtra("focus_lat", f.lat);
            i.putExtra("focus_lon", f.lon);
            i.putExtra("focus_zoom", 15.2);
            i.putExtra("focus_title", f.name);
            i.putExtra("focus_open_nav", false);
            startActivity(i);
        } catch (Throwable ignored) {
            openTurnByTurnNavigation(f);
        }
    }

    private void openTurnByTurnNavigation(Facility f) {
        Uri uri = Uri.parse("google.navigation:q=" + f.lat + "," + f.lon + "&mode=d");
        Intent nav = new Intent(Intent.ACTION_VIEW, uri);
        nav.setPackage("com.google.android.apps.maps");
        try {
            startActivity(nav);
        } catch (Exception e) {
            String geo = "geo:" + f.lat + "," + f.lon + "?q=" + f.lat + "," + f.lon + "(" + Uri.encode(f.name) + ")";
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(geo)));
        }
    }

    private void callPhone(Facility f) {
        if (TextUtils.isEmpty(f.phone)) {
            Toast.makeText(this, "No phone number listed.", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent i = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + Uri.encode(f.phone.trim())));
        try {
            startActivity(i);
        } catch (Exception e) {
            Toast.makeText(this, "No dialer app found.", Toast.LENGTH_SHORT).show();
        }
    }

    private void openWebsite(Facility f) {
        if (TextUtils.isEmpty(f.website)) {
            Toast.makeText(this, "No website listed.", Toast.LENGTH_SHORT).show();
            return;
        }
        String url = normalizeUrl(f.website.trim());
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (Exception e) {
            Toast.makeText(this, "Unable to open website.", Toast.LENGTH_SHORT).show();
        }
    }

    private void sendEmail(Facility f) {
        if (TextUtils.isEmpty(f.email)) {
            Toast.makeText(this, "No email listed.", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent i = new Intent(Intent.ACTION_SENDTO);
        i.setData(Uri.parse("mailto:" + Uri.encode(f.email.trim())));
        i.putExtra(Intent.EXTRA_SUBJECT, "Inquiry: " + f.name);
        try {
            startActivity(i);
        } catch (Exception e) {
            Toast.makeText(this, "No email app found.", Toast.LENGTH_SHORT).show();
        }
    }

    private String normalizeUrl(String url) {
        if (TextUtils.isEmpty(url)) return url;
        if (url.startsWith("http://") || url.startsWith("https://")) return url;
        return "https://" + url;
    }

    private void startNearMeFlow() {
        boolean fine = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
        boolean coarse = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;

        if (fine || coarse) {
            useLastKnownLocationAndSearch();
            return;
        }

        ActivityCompat.requestPermissions(
                this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                REQ_LOCATION
        );
    }

    private void useLastKnownLocationAndSearch() {
        Location loc = getBestLastKnownLocation(this);
        if (loc == null) {
            Toast.makeText(this, "Unable to get location. Search a city instead.", Toast.LENGTH_SHORT).show();
            return;
        }

        cancelAllInFlight();

        currentLat = loc.getLatitude();
        currentLon = loc.getLongitude();
        currentAnchorLabel = "your location";

     
        radiusIndex = 2;

        results.clear();
        dedupe.clear();
        cache.clear();

        setMetaText("Searching near you • " + (RADIUS_STEPS_M[radiusIndex] / 1000) + " km");
        setLoading(true, "Searching near you…");

        adapter.submitData(Collections.emptyList(), false, false, true);
        fetchAroundWithRetryAndAutoExpand(currentLat, currentLon, radiusIndex, false, 0, null);
    }

    private static Location getBestLastKnownLocation(Context ctx) {
        try {
            LocationManager lm = (LocationManager) ctx.getSystemService(Context.LOCATION_SERVICE);
            if (lm == null) return null;

            boolean fine = ActivityCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED;
            boolean coarse = ActivityCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_COARSE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED;
            if (!fine && !coarse) return null;

            List<String> providers = lm.getProviders(true);
            Location best = null;
            for (String p : providers) {
                Location l = lm.getLastKnownLocation(p);
                if (l == null) continue;
                if (best == null || l.getAccuracy() < best.getAccuracy()) best = l;
            }
            return best;
        } catch (Exception ignored) {
            return null;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_LOCATION) {
            boolean granted = false;
            for (int g : grantResults) if (g == PackageManager.PERMISSION_GRANTED) granted = true;
            if (granted) useLastKnownLocationAndSearch();
            else Toast.makeText(this, "Location permission denied.", Toast.LENGTH_SHORT).show();
        }
    }

    private static class FacilityFilter {
        final String amenityRegex;
        final String healthcareRegex;
        FacilityFilter(String amenityRegex, String healthcareRegex) {
            this.amenityRegex = amenityRegex;
            this.healthcareRegex = healthcareRegex;
        }
    }

    private FacilityFilter selectedFilter() {
        if (chipPharmacies.isChecked()) {
            return new FacilityFilter("pharmacy", "pharmacy");
        }
        if (chipClinics.isChecked()) {
            return new FacilityFilter("clinic|doctors", "doctor|clinic|centre|center");
        }
        return new FacilityFilter("hospital", "hospital");
    }

    private String forceTypeByChip(String type) {
        if (chipPharmacies.isChecked()) return "pharmacy";
        if (chipClinics.isChecked()) return "clinic";
        if (chipHospitals.isChecked()) return "hospital";
        return type == null ? "facility" : type;
    }


    private void setLoading(boolean loading, String status) {
        isLoading = loading;

        topProgress.setVisibility(loading ? View.VISIBLE : View.GONE);


        boolean showOverlay = loading && results.isEmpty();
        loadingOverlay.setVisibility(showOverlay ? View.VISIBLE : View.GONE);

        if (showOverlay && status != null) loadingText.setText(status);
        if (!showOverlay && status != null) {
  
            setMetaText(status);
        }
    }

    private void updateEmptyState() {
        boolean showEmpty = !isLoading && results.isEmpty();
        emptyState.setVisibility(showEmpty ? View.VISIBLE : View.GONE);
        recycler.setVisibility(showEmpty ? View.INVISIBLE : View.VISIBLE);
    }

    private void setEmptyMessage(String title, String subtitle) {
        if (!TextUtils.isEmpty(title)) emptyTitle.setText(title);
        if (subtitle != null) emptySubtitle.setText(subtitle);
    }

    private void setMetaText(String s) {
        if (metaText != null) metaText.setText(s == null ? "" : s);
    }

    private void updateMeta(int radiusMeters, int added) {
        String anchor = (currentAnchorLabel == null) ? "area" : currentAnchorLabel;
        String base = String.format(Locale.ROOT,
                "%d found • %dkm • near %s",
                results.size(),
                (radiusMeters / 1000),
                safe(anchor));

        if (added > 0) base = base + "  (+ " + added + ")";
        setMetaText(base);
    }

    private String safe(String s) {
        if (s == null) return "";
        s = s.trim();
        return s.length() > 44 ? s.substring(0, 44) + "…" : s;
    }

    private String bestName(Map<String, String> tags) {
        String name = tag(tags, "name");
        if (name != null) return name;
        String op = tag(tags, "operator");
        if (op != null) return op;
        String brand = tag(tags, "brand");
        if (brand != null) return brand;
        return "Unnamed facility";
    }

    private String bestType(Map<String, String> tags) {
        String amenity = tag(tags, "amenity");
        String healthcare = tag(tags, "healthcare");

        String v = (amenity != null) ? amenity : healthcare;
        if (v == null) return "facility";

        v = v.toLowerCase(Locale.ROOT);
        if (v.contains("hospital")) return "hospital";
        if (v.contains("pharmacy")) return "pharmacy";
        if (v.contains("doctor") || v.contains("doctors") || v.contains("clinic") || v.contains("centre") || v.contains("center"))
            return "clinic";
        return "facility";
    }

    private String bestPhone(Map<String, String> tags) {
        String[] keys = new String[]{"phone", "contact:phone", "telephone", "contact:telephone"};
        for (String k : keys) {
            String v = tag(tags, k);
            if (v != null) return v;
        }
        return null;
    }

    private String bestWebsite(Map<String, String> tags) {
        String[] keys = new String[]{"website", "contact:website", "url", "contact:url"};
        for (String k : keys) {
            String v = tag(tags, k);
            if (v != null) return v;
        }
        return null;
    }

    private String bestEmail(Map<String, String> tags) {
        String[] keys = new String[]{"email", "contact:email"};
        for (String k : keys) {
            String v = tag(tags, k);
            if (v != null) return v;
        }
        return null;
    }

    private String tag(Map<String, String> tags, String key) {
        if (tags == null) return null;
        String v = tags.get(key);
        if (v == null) return null;
        v = v.trim();
        return v.isEmpty() ? null : v;
    }

    private String buildAddress(Map<String, String> tags) {
        if (tags == null) return null;

        String street = tag(tags, "addr:street");
        String city = tag(tags, "addr:city");
        String num = tag(tags, "addr:housenumber");
        String state = tag(tags, "addr:state");
        String postcode = tag(tags, "addr:postcode");
        String country = tag(tags, "addr:country");

        StringBuilder sb = new StringBuilder();
        if (street != null) {
            if (num != null) sb.append(num).append(" ");
            sb.append(street);
        }
        appendComma(sb, city);
        appendComma(sb, state);
        appendComma(sb, postcode);
        appendComma(sb, country);

        String out = sb.toString().trim();
        return out.isEmpty() ? null : out;
    }

    private void appendComma(StringBuilder sb, String part) {
        if (part == null || part.trim().isEmpty()) return;
        if (sb.length() > 0) sb.append(", ");
        sb.append(part.trim());
    }

    private String cacheKey(double lat, double lon, int radius, FacilityFilter f) {
        double rLat = Math.round(lat * 10000d) / 10000d;
        double rLon = Math.round(lon * 10000d) / 10000d;
        return rLat + "|" + rLon + "|" + radius + "|" + f.amenityRegex + "|" + f.healthcareRegex;
    }

    private String buildOverpassQuery(double lat, double lon, int radiusMeters, FacilityFilter filter) {
 
        return "[out:json][timeout:45];(" +
                "nwr[\"amenity\"~\"" + filter.amenityRegex + "\"](around:" + radiusMeters + "," + lat + "," + lon + ");" +
                "nwr[\"healthcare\"~\"" + filter.healthcareRegex + "\"](around:" + radiusMeters + "," + lat + "," + lon + ");" +
                ");out center tags qt;";
    }

    private void track(Call<?> c) {
        synchronized (inFlight) {
            inFlight.add(c);
        }
    }

    private void untrack(Call<?> c) {
        synchronized (inFlight) {
            inFlight.remove(c);
        }
    }

    private void cancelAllInFlight() {
        synchronized (inFlight) {
            for (Call<?> c : inFlight) {
                try { c.cancel(); } catch (Exception ignored) {}
            }
            inFlight.clear();
        }
    }


    private static double haversineMeters(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6371000.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    private static String formatDistance(double meters) {
        if (meters < 1000) return String.format(Locale.ROOT, "%.0fm", meters);
        return String.format(Locale.ROOT, "%.1fkm", meters / 1000.0);
    }

    private void persistLastSnapshot() {
        try {
            LastSnapshot snap = new LastSnapshot();
            snap.ts = System.currentTimeMillis();
            snap.lat = currentLat;
            snap.lon = currentLon;
            snap.radiusIndex = radiusIndex;
            snap.anchor = currentAnchorLabel;
            snap.filter = filterKey(selectedFilter());
            int cap = Math.min(results.size(), 120);
            snap.items = new ArrayList<>(results.subList(0, cap));

            String json = gson.toJson(snap);
            SharedPreferences sp = getSharedPreferences(PREFS, MODE_PRIVATE);
            sp.edit().putString(KEY_LAST_SNAPSHOT, json).apply();
        } catch (Exception ignored) { }
    }

    private void prefillFromLastSnapshotIfFresh() {
        try {
            SharedPreferences sp = getSharedPreferences(PREFS, MODE_PRIVATE);
            String json = sp.getString(KEY_LAST_SNAPSHOT, null);
            if (json == null) return;

            LastSnapshot snap = gson.fromJson(json, LastSnapshot.class);
            if (snap == null) return;

            if (System.currentTimeMillis() - snap.ts > SNAPSHOT_MAX_AGE_MS) return;
            if (snap.items == null || snap.items.isEmpty()) return;

            currentLat = snap.lat;
            currentLon = snap.lon;
            radiusIndex = clampRadiusIndex(snap.radiusIndex);
            currentAnchorLabel = snap.anchor;

            results.clear();
            dedupe.clear();
            for (Facility f : snap.items) {
                if (!dedupe.containsKey(f.id)) {
                    dedupe.put(f.id, f);
                    results.add(f);
                }
            }

            // Keep sorted (distance then name)
            Collections.sort(results, (a, b) -> {
                int d = Double.compare(a.distanceMeters, b.distanceMeters);
                if (d != 0) return d;
                return (a.name == null ? "" : a.name).compareToIgnoreCase(b.name == null ? "" : b.name);
            });

            setMetaText("Last results • " + results.size() + " places");
            adapter.submitData(new ArrayList<>(results), canLoadMore(), false, false);
            updateEmptyState();
        } catch (Exception ignored) { }
    }

    private int clampRadiusIndex(int idx) {
        if (idx < 0) return 0;
        if (idx >= RADIUS_STEPS_M.length) return RADIUS_STEPS_M.length - 1;
        return idx;
    }

    private String filterKey(FacilityFilter f) {
        return f.amenityRegex + "|" + f.healthcareRegex;
    }

    static class LastSnapshot {
        long ts;
        double lat;
        double lon;
        int radiusIndex;
        String anchor;
        String filter;
        List<Facility> items;
    }


    static class Facility {
        final String id;
        final String name;
        final String type;
        final double lat, lon;
        final String address;
        final String phone;
        final String website;
        final String email;
        final double distanceMeters;

        Facility(String id, String name, String type,
                 double lat, double lon,
                 String address, String phone, String website, String email,
                 double distanceMeters) {
            this.id = id;
            this.name = name;
            this.type = type;
            this.lat = lat;
            this.lon = lon;
            this.address = address;
            this.phone = phone;
            this.website = website;
            this.email = email;
            this.distanceMeters = distanceMeters;
        }
    }

    private static final int VT_SKELETON = 0;
    private static final int VT_ITEM = 1;
    private static final int VT_FOOTER = 2;

    private class FacilityAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        private final Runnable loadMore;
        private final FacilityAction onItemClick;
        private final FacilityAction onNav;
        private final FacilityAction onCall;
        private final FacilityAction onWeb;
        private final FacilityAction onEmail;

        private boolean showFooter = false;
        private boolean footerLoading = false;
        private boolean showSkeleton = false;

        private final AsyncListDiffer<Facility> differ = new AsyncListDiffer<>(this, new DiffUtil.ItemCallback<Facility>() {
            @Override public boolean areItemsTheSame(@NonNull Facility oldItem, @NonNull Facility newItem) {
                return oldItem.id.equals(newItem.id);
            }
            @Override public boolean areContentsTheSame(@NonNull Facility o, @NonNull Facility n) {
                return safeEq(o.name, n.name)
                        && safeEq(o.type, n.type)
                        && safeEq(o.address, n.address)
                        && safeEq(o.phone, n.phone)
                        && safeEq(o.website, n.website)
                        && safeEq(o.email, n.email)
                        && Double.compare(o.distanceMeters, n.distanceMeters) == 0;
            }
            private boolean safeEq(String a, String b) {
                if (a == null && b == null) return true;
                if (a == null || b == null) return false;
                return a.equals(b);
            }
        });

        FacilityAdapter(FacilityAction onItemClick,
                        FacilityAction onNav,
                        FacilityAction onCall,
                        FacilityAction onWeb,
                        FacilityAction onEmail,
                        Runnable loadMore) {
            this.onItemClick = onItemClick;
            this.onNav = onNav;
            this.onCall = onCall;
            this.onWeb = onWeb;
            this.onEmail = onEmail;
            this.loadMore = loadMore;
            setHasStableIds(true);
        }

        void submitData(List<Facility> data, boolean canLoadMore, boolean loadingMore, boolean skeletonLoading) {
            this.showFooter = canLoadMore;
            this.footerLoading = loadingMore;
            this.showSkeleton = skeletonLoading && (data == null || data.isEmpty());
            differ.submitList(data == null ? Collections.emptyList() : data);
            notifyDataSetChanged();
        }

        void setFooterState(boolean canLoadMore, boolean loadingMore) {
            this.showFooter = canLoadMore;
            this.footerLoading = loadingMore;
            notifyDataSetChanged();
        }

        @Override public long getItemId(int position) {
            if (getItemViewType(position) == VT_ITEM) {
                Facility f = differ.getCurrentList().get(position);
                return (long) f.id.hashCode();
            }
            return super.getItemId(position);
        }

        @Override public int getItemViewType(int position) {
            if (showSkeleton) return VT_SKELETON;

            int size = differ.getCurrentList().size();
            if (position < size) return VT_ITEM;
            return VT_FOOTER;
        }

        @NonNull
        @Override public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            if (viewType == VT_SKELETON) {
                View v = getLayoutInflater().inflate(R.layout.row_facility_skeleton_spotify, parent, false);
                return new SkeletonVH(v);
            }
            if (viewType == VT_FOOTER) {
                View v = getLayoutInflater().inflate(R.layout.row_facility_footer_spotify, parent, false);
                return new FooterVH(v);
            }
            View v = getLayoutInflater().inflate(R.layout.row_facility_spotify, parent, false);
            return new ItemVH(v);
        }

        @Override public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            int vt = getItemViewType(position);

            if (vt == VT_SKELETON) return;

            if (vt == VT_FOOTER) {
                FooterVH h = (FooterVH) holder;
                h.bind(showFooter, footerLoading, loadMore);
                return;
            }

            Facility f = differ.getCurrentList().get(position);
            ItemVH h = (ItemVH) holder;
            h.bind(f);
        }

        @Override public int getItemCount() {
            if (showSkeleton) return 6;
            int size = differ.getCurrentList().size();
            return size + (showFooter ? 1 : 0);
        }

        class SkeletonVH extends RecyclerView.ViewHolder {
            SkeletonVH(@NonNull View itemView) { super(itemView); }
        }

        class FooterVH extends RecyclerView.ViewHolder {
            final TextView loadMoreBtn;
            final LinearProgressIndicator pb;
            FooterVH(@NonNull View itemView) {
                super(itemView);
                loadMoreBtn = itemView.findViewById(R.id.loadMoreBtn);
                pb = itemView.findViewById(R.id.loadMoreProgress);
            }
            void bind(boolean show, boolean loading, Runnable onLoadMore) {
                itemView.setVisibility(show ? View.VISIBLE : View.GONE);
                pb.setVisibility(loading ? View.VISIBLE : View.GONE);
                loadMoreBtn.setAlpha(loading ? 0.6f : 1f);
                loadMoreBtn.setOnClickListener(v -> {
                    if (!loading) onLoadMore.run();
                });
            }
        }

        class ItemVH extends RecyclerView.ViewHolder {
            final TextView name;
            final TextView subtitle;
            final TextView meta;
            final TextView pill;
            final View iconDot;

            final View bMap;
            final View bNav;
            final View bCall;
            final View bWeb;
            final View bEmail;

            ItemVH(@NonNull View itemView) {
                super(itemView);
                name = itemView.findViewById(R.id.facilityName);
                subtitle = itemView.findViewById(R.id.facilitySubtitle);
                meta = itemView.findViewById(R.id.facilityMeta);
                pill = itemView.findViewById(R.id.facilityPill);
                iconDot = itemView.findViewById(R.id.iconDot);

                bMap = itemView.findViewById(R.id.actionMap);
                bNav = itemView.findViewById(R.id.actionNav);
                bCall = itemView.findViewById(R.id.actionCall);
                bWeb = itemView.findViewById(R.id.actionWeb);
                bEmail = itemView.findViewById(R.id.actionEmail);
            }

            void bind(Facility f) {
                name.setText(TextUtils.isEmpty(f.name) ? "Facility" : f.name);

                String type = (TextUtils.isEmpty(f.type) ? "facility" : f.type).toUpperCase(Locale.ROOT);
                pill.setText(type);

                // Icon dot tint depends on type
                int dot = C_GREEN;
                if ("HOSPITAL".equals(type)) dot = C_GREEN;
                else if ("CLINIC".equals(type)) dot = Color.parseColor("#2E77D0");
                else if ("PHARMACY".equals(type)) dot = Color.parseColor("#E0B100");
                iconDot.setBackground(makeDot(dot));

                String dist = formatDistance(f.distanceMeters);
                String addr = TextUtils.isEmpty(f.address) ? "Address not available" : f.address;
                subtitle.setText(dist + " • " + addr);

                StringBuilder sb = new StringBuilder();
                if (!TextUtils.isEmpty(f.phone)) sb.append("Phone: ").append(f.phone.trim());
                if (!TextUtils.isEmpty(f.website)) {
                    if (sb.length() > 0) sb.append("  •  ");
                    sb.append("Web: ").append(shorten(f.website.trim(), 34));
                }
                if (!TextUtils.isEmpty(f.email)) {
                    if (sb.length() > 0) sb.append("  •  ");
                    sb.append("Email: ").append(shorten(f.email.trim(), 28));
                }
                meta.setText(sb.length() == 0 ? "No contact listed in OpenStreetMap for this place." : sb.toString());

                setEnabledAlpha(bCall, !TextUtils.isEmpty(f.phone));
                setEnabledAlpha(bWeb, !TextUtils.isEmpty(f.website));
                setEnabledAlpha(bEmail, !TextUtils.isEmpty(f.email));

                itemView.setOnClickListener(v -> onItemClick.run(f));
                bMap.setOnClickListener(v -> onItemClick.run(f));
                bNav.setOnClickListener(v -> onNav.run(f));
                bCall.setOnClickListener(v -> onCall.run(f));
                bWeb.setOnClickListener(v -> onWeb.run(f));
                bEmail.setOnClickListener(v -> onEmail.run(f));
            }

            private void setEnabledAlpha(View v, boolean enabled) {
                v.setAlpha(enabled ? 1f : 0.35f);
                v.setEnabled(enabled);
            }

            private GradientDrawable makeDot(int color) {
                GradientDrawable d = new GradientDrawable();
                d.setColor(ColorUtils.setAlphaComponent(color, 220));
                d.setCornerRadius(dp(999));
                d.setStroke(dp(1), ColorUtils.setAlphaComponent(Color.WHITE, 20));
                return d;
            }

            private String shorten(String s, int max) {
                if (s == null) return "";
                s = s.trim();
                return s.length() > max ? s.substring(0, max) + "…" : s;
            }
        }
    }

    private interface FacilityAction { void run(Facility f); }

    interface NominatimApiService {
        @GET("search")
        Call<List<NominatimPlace>> search(
                @Query("q") String q,
                @Query("format") String format,
                @Query("limit") int limit
        );
    }

    static class NominatimPlace {
        public String lat;
        public String lon;
        public String display_name;
    }

    interface OverpassApiService {
        @FormUrlEncoded
        @POST("api/interpreter")
        Call<OverpassResponse> interpreter(@Field("data") String data);

        @GET
        Call<OverpassResponse> interpreterUrl(@Url String url);
    }

    static class OverpassResponse {
        public List<OverpassElement> elements;
    }

    static class OverpassElement {
        public String type;
        public long id;
        public Double lat;
        public Double lon;
        public Center center;
        public Map<String, String> tags;

        double getLat() {
            if (lat != null) return lat;
            if (center != null && center.lat != null) return center.lat;
            return 0d;
        }
        double getLon() {
            if (lon != null) return lon;
            if (center != null && center.lon != null) return center.lon;
            return 0d;
        }
    }

    static class Center {
        public Double lat;
        public Double lon;
    }


    private int dp(int v) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, v, getResources().getDisplayMetrics());
    }

    private float dpF(int v) {
        return TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, v, getResources().getDisplayMetrics());
    }
}
