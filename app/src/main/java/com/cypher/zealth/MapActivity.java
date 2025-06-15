
package com.cypher.zealth;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.BlurMaskFilter;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.core.app.ActivityCompat;

import com.cypher.zealth.DiseaseCatalog.DiseaseOption;
import com.cypher.zealth.DiseaseCatalog.SourceType;
import com.cypher.zealth.models.GhoModels;
import com.cypher.zealth.network.CovidApiService;
import com.cypher.zealth.network.GhoApiService;
import com.cypher.zealth.network.models.CovidCountry;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.slider.Slider;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.osmdroid.config.Configuration;
import org.osmdroid.events.MapAdapter;
import org.osmdroid.events.ZoomEvent;
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.BoundingBox;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.util.MapTileIndex;
import org.osmdroid.views.CustomZoomButtonsController;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Polygon;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MapActivity extends AppCompatActivity {

    private static final String TAG = "MapActivity";
    private static final int REQ_LOCATION = 1001;

    private static final int START_YEAR = 2000;
    private static final int END_YEAR = 2030;

    private static final long COVID_REFRESH_MS = 5 * 60 * 1000L; // 5 minutes
    private final Handler uiHandler = new Handler(Looper.getMainLooper());

    private static final long UI_DEBOUNCE_MS = 280L;
    private Runnable pendingMapUpdate;
    private Runnable pendingZoomRerender;
    private Runnable covidRefreshLoop;

    private MapView map;
    private Slider yearSlider;
    private TextView tvYearLabel, tvBadge, tvDataMeta;
    private ChipGroup chipGroup;
    private FloatingActionButton fabFacilities;
    private ImageButton btnZoomIn, btnZoomOut;

    private ImageButton btnBack;

    private View infoCardRoot;
    private TextView tvInfoTitle, tvInfoSubtitle, tvInfoBody, tvInfoBadge;

    private int CURRENT_YEAR;
    private int selectedYear;
    private DiseaseOption selectedDisease;

    private final List<Polygon> activeHeatmapLayers = new ArrayList<>();
    private final Map<Polygon, RiskInfo> riskInfoByPolygon = new HashMap<>();

    private boolean didCenterOnUser = false;
    private MyLocationNewOverlay locationOverlay;

    private GhoApiService ghoApi;
    private CovidApiService covidApi;
    private final Map<String, CacheEntry> cache = new HashMap<>();

    private static final class CacheEntry {
        final List<GhoModels.GhoValue> values;
        final long fetchedAt;
        final int effectiveYear;
        CacheEntry(List<GhoModels.GhoValue> values, long fetchedAt, int effectiveYear) {
            this.values = values;
            this.fetchedAt = fetchedAt;
            this.effectiveYear = effectiveYear;
        }
    }

    private static class RiskInfo {
        String disease;
        int yearShown;
        int effectiveYear;
        String iso3;
        String label;
        double value;
        boolean isPrediction;
        String source;
        Long updatedEpoch;
        String notes;
    }

    private static final class RenderDatum {
        String iso3;
        GeoPoint center;
        double value;

        String label; 
        int color;

        RiskInfo info;
        Polygon polygon;
    }

    private static final class RenderModel {
        String datasetKey;
        int effectiveYear;
        boolean isPrediction;
        String sourceLabel;
        Long updatedEpoch;

        double p50, p85, p97;

        final Map<String, RenderDatum> byIso = new HashMap<>();
    }

    private RenderModel currentModel = null;
    private final Map<String, Polygon> polygonByIso = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Configuration.getInstance().setUserAgentValue(getPackageName());
        setContentView(R.layout.activity_map);

        CURRENT_YEAR = Calendar.getInstance().get(Calendar.YEAR);
        selectedYear = Math.min(CURRENT_YEAR, END_YEAR);

        CountryMapper.init(this);

        initViews();
        setupMap();
        initNetworking();
        initTimeline();
        initDiseaseChips();
        enableUserLocation();
        handleIncomingFocusIntent();

        updateMapForSelection(true);
    }

    @Override
    protected void onResume() {
        super.onResume();
        map.onResume();
        startCovidRefreshLoopIfNeeded();
    }

    @Override
    protected void onPause() {
        super.onPause();
        map.onPause();
        stopCovidRefreshLoop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        uiHandler.removeCallbacksAndMessages(null);
    }


    private void initViews() {
        map = findViewById(R.id.map);
        yearSlider = findViewById(R.id.yearSlider);
        tvYearLabel = findViewById(R.id.tvYearLabel);
        tvBadge = findViewById(R.id.tvPredictionBadge);
        tvDataMeta = findViewById(R.id.tvDataMeta);

        chipGroup = findViewById(R.id.diseaseChipGroup);
        fabFacilities = findViewById(R.id.fabFacilities);
        btnZoomIn = findViewById(R.id.btnZoomIn);
        btnZoomOut = findViewById(R.id.btnZoomOut);



        btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> onBackPressed());

        infoCardRoot = findViewById(R.id.riskInfoCard);
        tvInfoTitle = findViewById(R.id.tvInfoTitle);
        tvInfoSubtitle = findViewById(R.id.tvInfoSubtitle);
        tvInfoBody = findViewById(R.id.tvInfoBody);
        tvInfoBadge = findViewById(R.id.tvInfoBadge);

        hideRiskInfoCard(false);

        View infoScrim = findViewById(R.id.riskInfoScrim);
        infoScrim.setOnClickListener(v -> hideRiskInfoCard(true));

        View btnClose = findViewById(R.id.btnInfoClose);
        btnClose.setOnClickListener(v -> hideRiskInfoCard(true));

        fabFacilities.setOnClickListener(v -> {
            GeoPoint c = (GeoPoint) map.getMapCenter();
            Intent i = new Intent(MapActivity.this, HealthFacilitiesActivity.class);
            i.putExtra("center_lat", c.getLatitude());
            i.putExtra("center_lon", c.getLongitude());
            i.putExtra("radius_m", 15000);
            startActivity(i);
        });

        btnZoomIn.setOnClickListener(v -> map.getController().zoomIn());
        btnZoomOut.setOnClickListener(v -> map.getController().zoomOut());


        SearchView searchView = findViewById(R.id.searchView);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override public boolean onQueryTextSubmit(String query) {
                GeoPoint target = CountryMapper.findCoordsByName(query);
                if (target != null) {
                    map.getController().animateTo(target, 6.0, 1200L);
                    searchView.clearFocus();
                } else {
                    Toast.makeText(MapActivity.this, "Region not found", Toast.LENGTH_SHORT).show();
                }
                return true;
            }
            @Override public boolean onQueryTextChange(String newText) { return false; }
        });
    }

    private void initTimeline() {
        yearSlider.setValueFrom(START_YEAR);
        yearSlider.setValueTo(END_YEAR);
        yearSlider.setStepSize(1f);
        yearSlider.setValue(selectedYear);

        updateYearUI();

        yearSlider.addOnChangeListener((slider, value, fromUser) -> {
            selectedYear = (int) value;
            updateYearUI();

            if (!fromUser) return;

            if (pendingMapUpdate != null) uiHandler.removeCallbacks(pendingMapUpdate);
            pendingMapUpdate = () -> updateMapForSelection(false);
            uiHandler.postDelayed(pendingMapUpdate, UI_DEBOUNCE_MS);
        });
    }

    private void updateYearUI() {
        tvYearLabel.setText("Year: " + selectedYear);
        if (selectedYear > CURRENT_YEAR) applyBadge("PREDICTION", "#FF5252");
        else applyBadge("LOADING…", "#607D8B");
        tvDataMeta.setText("");
    }

    private void initDiseaseChips() {
        List<DiseaseOption> diseases = DiseaseCatalog.defaultList();
        selectedDisease = diseases.get(0);

        chipGroup.removeAllViews();
        chipGroup.setSingleSelection(true);

        for (DiseaseOption d : diseases) {
            Chip chip = new Chip(this);
            chip.setText(d.displayName);
            chip.setCheckable(true);
            chip.setClickable(true);

            chip.setChipBackgroundColorResource(R.color.chip_bg_selector);
            chip.setTextColor(getColorStateList(R.color.chip_text_selector));
            chip.setChipStrokeWidth(1f);
            chip.setChipStrokeColor(getColorStateList(R.color.chip_stroke_selector));

            chip.setOnClickListener(v -> {
                selectedDisease = d;
                hideRiskInfoCard(true);
                updateMapForSelection(true);
            });

            chipGroup.addView(chip);
        }

        ((Chip) chipGroup.getChildAt(0)).setChecked(true);
    }


    private void setupMap() {
        try { map.setTileSource(createDarkTileSource()); }
        catch (Exception e) { map.setTileSource(TileSourceFactory.MAPNIK); }

        map.setMultiTouchControls(true);
        map.setBuiltInZoomControls(false);
        map.getZoomController().setVisibility(CustomZoomButtonsController.Visibility.NEVER);

        map.setMinZoomLevel(3.0);
        map.setMaxZoomLevel(19.0);
        map.setHorizontalMapRepetitionEnabled(false);
        map.setVerticalMapRepetitionEnabled(false);

        
        map.setLayerType(View.LAYER_TYPE_SOFTWARE, null);

        BoundingBox worldBounds = new BoundingBox(85.0, 180.0, -85.0, -180.0);
        map.setScrollableAreaLimitDouble(worldBounds);
        addBoundsBorder(worldBounds);

        map.getController().setZoom(4.3);
        map.getController().setCenter(new GeoPoint(9.08, 8.67));

        map.setOnTouchListener((v, event) -> {
            if (infoCardRoot != null && infoCardRoot.getVisibility() == View.VISIBLE) hideRiskInfoCard(true);
            return false;
        });

        map.addMapListener(new MapAdapter() {
            @Override public boolean onZoom(ZoomEvent event) {
                if (pendingZoomRerender != null) uiHandler.removeCallbacks(pendingZoomRerender);
                pendingZoomRerender = () -> updatePolygonsForCurrentZoom();
                uiHandler.postDelayed(pendingZoomRerender, UI_DEBOUNCE_MS);
                return super.onZoom(event);
            }
        });
    }

    private OnlineTileSourceBase createDarkTileSource() {
        final String[] CARTO_BASES = new String[]{
                "https://a.basemaps.cartocdn.com/dark_all/",
                "https://b.basemaps.cartocdn.com/dark_all/",
                "https://c.basemaps.cartocdn.com/dark_all/",
                "https://d.basemaps.cartocdn.com/dark_all/"
        };

        return new OnlineTileSourceBase("CartoDark", 1, 19, 256, ".png", CARTO_BASES) {
            @Override public String getTileURLString(long pMapTileIndex) {
                int z = MapTileIndex.getZoom(pMapTileIndex);
                int x = MapTileIndex.getX(pMapTileIndex);
                int y = MapTileIndex.getY(pMapTileIndex);
                String base = CARTO_BASES[Math.floorMod(x + y, CARTO_BASES.length)];
                return base + z + "/" + x + "/" + y + ".png";
            }
        };
    }

    private void addBoundsBorder(BoundingBox bounds) {
        Polygon border = new Polygon(map);
        List<GeoPoint> pts = new ArrayList<>();
        pts.add(new GeoPoint(bounds.getLatNorth(), bounds.getLonWest()));
        pts.add(new GeoPoint(bounds.getLatNorth(), bounds.getLonEast()));
        pts.add(new GeoPoint(bounds.getLatSouth(), bounds.getLonEast()));
        pts.add(new GeoPoint(bounds.getLatSouth(), bounds.getLonWest()));
        pts.add(new GeoPoint(bounds.getLatNorth(), bounds.getLonWest()));
        border.setPoints(pts);
        border.getFillPaint().setColor(Color.argb(0, 0, 0, 0));
        border.getOutlinePaint().setColor(Color.argb(90, 255, 255, 255));
        border.getOutlinePaint().setStrokeWidth(2.5f);
        map.getOverlays().add(border);
    }

    private void handleIncomingFocusIntent() {
        Intent i = getIntent();
        if (i == null) return;
        if (i.hasExtra("focus_lat") && i.hasExtra("focus_lon")) {
            double lat = i.getDoubleExtra("focus_lat", 0);
            double lon = i.getDoubleExtra("focus_lon", 0);
            double zoom = i.getDoubleExtra("focus_zoom", 14.5);
            map.getController().animateTo(new GeoPoint(lat, lon), zoom, 1200L);
        }
    }


    private void initNetworking() {
        OkHttpClient ok = new OkHttpClient.Builder().build();

        Retrofit who = new Retrofit.Builder()
                .baseUrl("https://ghoapi.azureedge.net/")
                .client(ok)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        ghoApi = who.create(GhoApiService.class);

        Retrofit covid = new Retrofit.Builder()
                .baseUrl("https://disease.sh/")
                .client(ok)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        covidApi = covid.create(CovidApiService.class);
    }

    private void updateMapForSelection(boolean userInitiated) {
        clearRiskOverlays();
        hideRiskInfoCard(true);

        if (selectedYear > CURRENT_YEAR) {
            stopCovidRefreshLoop();
            generatePredictionData(selectedYear);
            applyBadge("PREDICTION", "#FF5252");
            tvDataMeta.setText("Projected overlay (synthetic; not surveillance).");
            return;
        }

        if (selectedDisease.sourceType == SourceType.DISEASE_SH_COVID) {
            if (selectedYear != CURRENT_YEAR) {
                Toast.makeText(this, "COVID live map is available for the current year only.", Toast.LENGTH_SHORT).show();
            }
            fetchCovidLive();
            startCovidRefreshLoopIfNeeded();
            return;
        }

        stopCovidRefreshLoop();
        fetchWhoLatestAvailable(selectedDisease, selectedYear);
    }

    private void clearRiskOverlays() {
        for (Polygon p : activeHeatmapLayers) map.getOverlays().remove(p);
        activeHeatmapLayers.clear();
        riskInfoByPolygon.clear();
        currentModel = null;
        map.invalidate();
    }


    private void startCovidRefreshLoopIfNeeded() {
        if (selectedDisease == null) return;
        if (selectedDisease.sourceType != SourceType.DISEASE_SH_COVID) return;
        if (selectedYear > CURRENT_YEAR) return;

        stopCovidRefreshLoop();

        covidRefreshLoop = new Runnable() {
            @Override public void run() {
                fetchCovidLive();
                uiHandler.postDelayed(this, COVID_REFRESH_MS);
            }
        };
        uiHandler.postDelayed(covidRefreshLoop, COVID_REFRESH_MS);
    }

    private void stopCovidRefreshLoop() {
        if (covidRefreshLoop != null) uiHandler.removeCallbacks(covidRefreshLoop);
        covidRefreshLoop = null;
    }

    private void fetchCovidLive() {
        applyBadge("LIVE", "#4CAF50");

        covidApi.getCovidByCountry(true).enqueue(new Callback<List<CovidCountry>>() {
            @Override public void onResponse(Call<List<CovidCountry>> call, Response<List<CovidCountry>> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    Toast.makeText(MapActivity.this, "COVID API error.", Toast.LENGTH_SHORT).show();
                    return;
                }

                List<GhoModels.GhoValue> asValues = new ArrayList<>();
                Long newestUpdated = null;

                for (CovidCountry c : response.body()) {
                    if (c == null || c.countryInfo == null || c.countryInfo.iso3 == null) continue;
                    if (c.deaths == null) continue;

                    String iso3 = c.countryInfo.iso3.trim().toUpperCase(Locale.ROOT);
                    if (CountryMapper.getCoords(iso3) == null) continue;

                    GhoModels.GhoValue v = new GhoModels.GhoValue();
                    v.spatialLocationCode = iso3;
                    v.numericValue = c.deaths;
                    v.timeDim = CURRENT_YEAR;
                    asValues.add(v);

                    if (c.updated != null) {
                        newestUpdated = (newestUpdated == null) ? c.updated : Math.max(newestUpdated, c.updated);
                    }
                }

                String updatedStr = (newestUpdated == null)
                        ? "Updated: unknown"
                        : "Updated: " + DateFormat.getDateTimeInstance().format(new Date(newestUpdated));

                tvDataMeta.setText("COVID deaths (cumulative live snapshot). " + updatedStr);
                renderRiskOverlayQuantiles(asValues, false, CURRENT_YEAR, "disease.sh", newestUpdated);
            }

            @Override public void onFailure(Call<List<CovidCountry>> call, Throwable t) {
                Log.w(TAG, "COVID request failed", t);
                Toast.makeText(MapActivity.this, "COVID network failure.", Toast.LENGTH_SHORT).show();
            }
        });
    }


    private void fetchWhoLatestAvailable(DiseaseOption disease, int desiredYear) {
        if (disease == null || disease.ghoKeyword == null) {
            Toast.makeText(this, "Invalid WHO disease selection.", Toast.LENGTH_SHORT).show();
            return;
        }

        resolveIndicatorCode(disease.ghoKeyword, disease.requiredTokens, indicatorCode -> {
            if (indicatorCode == null) {
                Toast.makeText(MapActivity.this, "No WHO indicator found for " + disease.displayName, Toast.LENGTH_SHORT).show();
                generateMockData(desiredYear);
                return;
            }
            fetchWhoWithYearFallback(indicatorCode, disease.displayName, desiredYear);
        });
    }

    private interface StringCallback { void onResult(String value); }

    private void resolveIndicatorCode(String keyword, String[] requiredTokens, StringCallback cb) {
        String k = keyword.trim().toLowerCase(Locale.ROOT).replace("'", "''");
        String filter = "contains(tolower(IndicatorName),'" + k + "')";

        ghoApi.searchIndicators(filter, "IndicatorCode,IndicatorName,IndicatorShortName", 80)
                .enqueue(new Callback<GhoModels.GhoResponse>() {
                    @Override public void onResponse(Call<GhoModels.GhoResponse> call, Response<GhoModels.GhoResponse> response) {
                        if (!response.isSuccessful() || response.body() == null || response.body().value == null) {
                            cb.onResult(null); return;
                        }

                        String bestCode = null;
                        int bestScore = Integer.MIN_VALUE;

                        for (GhoModels.GhoValue v : response.body().value) {
                            if (v == null || v.indicatorCode == null) continue;

                            String name = (v.indicatorName != null) ? v.indicatorName : "";
                            String shortName = (v.indicatorShortName != null) ? v.indicatorShortName : "";
                            String hay = (name + " " + shortName).toLowerCase(Locale.ROOT);

                            int score = 0;
                            if (hay.contains("death")) score += 30;
                            if (hay.contains("mortality")) score += 20;
                            if (hay.contains("number")) score += 8;
                            if (hay.contains("rate")) score -= 2;

                            if (requiredTokens != null) {
                                for (String t : requiredTokens) {
                                    if (t == null) continue;
                                    String tt = t.toLowerCase(Locale.ROOT);
                                    if (tt.length() < 3) continue;
                                    if (hay.contains(tt)) score += 10;
                                }
                            }

                            if (score > bestScore) { bestScore = score; bestCode = v.indicatorCode; }
                        }

                        cb.onResult(bestCode);
                    }

                    @Override public void onFailure(Call<GhoModels.GhoResponse> call, Throwable t) {
                        Log.w(TAG, "Indicator lookup failed", t);
                        cb.onResult(null);
                    }
                });
    }

    private void fetchWhoWithYearFallback(String indicatorCode, String diseaseName, int desiredYear) {
        tryWhoYear(indicatorCode, diseaseName, desiredYear, desiredYear, 12);
    }

    private void tryWhoYear(String code, String diseaseName, int desiredYear, int attemptYear, int remainingBack) {
        String cacheKey = "WHO|" + code + "|" + attemptYear;
        CacheEntry entry = cache.get(cacheKey);

        if (entry != null) {
            applyWhoMeta(diseaseName, desiredYear, entry.effectiveYear);
            renderRiskOverlayQuantiles(entry.values, false, entry.effectiveYear, "WHO GHO", null);
            return;
        }

        String filter = "TimeDim eq " + attemptYear;

        ghoApi.getIndicatorData(code, filter).enqueue(new Callback<GhoModels.GhoResponse>() {
            @Override public void onResponse(Call<GhoModels.GhoResponse> call, Response<GhoModels.GhoResponse> response) {
                if (response.isSuccessful()
                        && response.body() != null
                        && response.body().value != null
                        && !response.body().value.isEmpty()) {

                    cache.put(cacheKey, new CacheEntry(response.body().value, System.currentTimeMillis(), attemptYear));
                    applyWhoMeta(diseaseName, desiredYear, attemptYear);
                    renderRiskOverlayQuantiles(response.body().value, false, attemptYear, "WHO GHO", null);
                    return;
                }

                if (remainingBack > 0 && attemptYear > START_YEAR) {
                    tryWhoYear(code, diseaseName, desiredYear, attemptYear - 1, remainingBack - 1);
                } else {
                    Toast.makeText(MapActivity.this, "No WHO data available near " + desiredYear, Toast.LENGTH_SHORT).show();
                    generateMockData(desiredYear);
                }
            }

            @Override public void onFailure(Call<GhoModels.GhoResponse> call, Throwable t) {
                Log.w(TAG, "WHO request failed", t);
                Toast.makeText(MapActivity.this, "WHO network failure.", Toast.LENGTH_SHORT).show();
                generateMockData(desiredYear);
            }
        });
    }

    private void applyWhoMeta(String diseaseName, int desiredYear, int effectiveYear) {
        if (desiredYear == CURRENT_YEAR) applyBadge("LATEST: " + effectiveYear, "#2196F3");
        else applyBadge("DATA: " + effectiveYear, "#607D8B");

        tvDataMeta.setText(diseaseName + " (mortality indicator). Source: WHO GHO. Year: " + effectiveYear);
    }

    private void generateMockData(int year) {
        List<GhoModels.GhoValue> mock = new ArrayList<>();
        Random rng = new Random((long) year * 31L + (selectedDisease != null ? selectedDisease.displayName.hashCode() : 7));

        for (String iso : CountryMapper.getKnownIsos()) {
            if (rng.nextFloat() > 0.6f) continue;
            GhoModels.GhoValue v = new GhoModels.GhoValue();
            v.spatialLocationCode = iso;
            v.numericValue = (50000 * rng.nextDouble());
            v.timeDim = year;
            mock.add(v);
        }

        applyBadge("SIMULATED", "#9C27B0");
        tvDataMeta.setText("Simulated overlay (no data for this selection).");
        renderRiskOverlayQuantiles(mock, false, year, "Simulated", null);
    }

    private void generatePredictionData(int year) {
        List<GhoModels.GhoValue> predicted = new ArrayList<>();
        Random rng = new Random((long) year * 17L + (selectedDisease != null ? selectedDisease.displayName.hashCode() : 11));

        for (String iso : CountryMapper.getKnownIsos()) {
            if (rng.nextFloat() > 0.8f) {
                GhoModels.GhoValue v = new GhoModels.GhoValue();
                v.spatialLocationCode = iso;
                v.numericValue = 120000.0 * rng.nextDouble();
                v.timeDim = year;
                predicted.add(v);
            }
        }

        renderRiskOverlayQuantiles(predicted, true, year, "Synthetic projection", null);
    }

    private void renderRiskOverlayQuantiles(
            List<GhoModels.GhoValue> data,
            boolean isPrediction,
            int effectiveYear,
            String sourceLabel,
            Long updatedEpoch
    ) {
        String diseaseKey = (selectedDisease != null) ? selectedDisease.displayName : "Unknown";
        String datasetKey = sourceLabel + "|" + diseaseKey + "|" + effectiveYear + "|" + (isPrediction ? "P" : "O");

        RenderModel model = buildRenderModel(datasetKey, data, isPrediction, effectiveYear, sourceLabel, updatedEpoch);
        if (model.byIso.isEmpty()) {
            Toast.makeText(this, "No data available for this selection.", Toast.LENGTH_SHORT).show();
            return;
        }

        applyRenderModel(model, true);
    }

    private RenderModel buildRenderModel(
            String datasetKey,
            List<GhoModels.GhoValue> data,
            boolean isPrediction,
            int effectiveYear,
            String sourceLabel,
            Long updatedEpoch
    ) {
        RenderModel model = new RenderModel();
        model.datasetKey = datasetKey;
        model.isPrediction = isPrediction;
        model.effectiveYear = effectiveYear;
        model.sourceLabel = sourceLabel;
        model.updatedEpoch = updatedEpoch;

        List<Double> values = new ArrayList<>();
        for (GhoModels.GhoValue v : data) {
            if (v == null || v.numericValue == null) continue;
            double x = v.numericValue;
            if (Double.isNaN(x) || x <= 0) continue;
            values.add(x);
        }
        if (values.isEmpty()) return model;

        Collections.sort(values);
        model.p50 = percentile(values, 0.50);
        model.p85 = percentile(values, 0.85);
        model.p97 = percentile(values, 0.97);

        for (GhoModels.GhoValue v : data) {
            if (v == null || v.numericValue == null) continue;

            String iso = (v.spatialLocationCode == null) ? null : v.spatialLocationCode.trim().toUpperCase(Locale.ROOT);
            if (iso == null || iso.isEmpty()) continue;

            GeoPoint center = CountryMapper.getCoords(iso);
            if (center == null) continue;

            double x = v.numericValue;
            if (Double.isNaN(x) || x <= 0) continue;

            RenderDatum d = new RenderDatum();
            d.iso3 = iso;
            d.center = center;
            d.value = x;

            if (x >= model.p97) { d.label = "Critical";  d.color = Color.parseColor("#E53935"); }
            else if (x >= model.p85) { d.label = "Moderate"; d.color = Color.parseColor("#FFA000"); }
            else if (x >= model.p50) { d.label = "Elevated"; d.color = Color.parseColor("#43A047"); }
            else { d.label = "Low"; d.color = Color.parseColor("#2E7D32"); }

            RiskInfo info = new RiskInfo();
            info.disease = (selectedDisease != null) ? selectedDisease.displayName : "Unknown";
            info.yearShown = selectedYear;
            info.effectiveYear = effectiveYear;
            info.iso3 = iso;
            info.label = d.label;
            info.value = x;
            info.isPrediction = isPrediction;
            info.source = sourceLabel;
            info.updatedEpoch = updatedEpoch;
            info.notes = buildQuickNotes(d.label, isPrediction);
            d.info = info;

            model.byIso.put(iso, d);
        }

        return model;
    }

    private void applyRenderModel(RenderModel model, boolean rebuildOverlays) {
        if (rebuildOverlays) {
            for (Polygon p : activeHeatmapLayers) map.getOverlays().remove(p);
            activeHeatmapLayers.clear();
            riskInfoByPolygon.clear();
        }

        double zoom = map.getZoomLevelDouble();
        float strokeW = strokeWidthForZoom(zoom);
        float glowR = glowRadiusForZoom(zoom);

        for (RenderDatum d : model.byIso.values()) {
            Polygon p = polygonByIso.get(d.iso3);
            if (p == null) {
                p = new Polygon(map);
                polygonByIso.put(d.iso3, p);
            } else {
                map.getOverlays().remove(p);
            }

            p.getFillPaint().setStyle(Paint.Style.FILL);
            p.getFillPaint().setColor(Color.argb(10, Color.red(d.color), Color.green(d.color), Color.blue(d.color)));

            Paint outline = p.getOutlinePaint();
            outline.setStyle(Paint.Style.STROKE);
            outline.setStrokeWidth(strokeW);
            outline.setColor(Color.argb(220, Color.red(d.color), Color.green(d.color), Color.blue(d.color)));
            outline.setAntiAlias(true);
            outline.setStrokeCap(Paint.Cap.ROUND);
            outline.setStrokeJoin(Paint.Join.ROUND);
            outline.setMaskFilter(new BlurMaskFilter(glowR, BlurMaskFilter.Blur.OUTER));

            double radiusMeters = computeRadiusMeters(d.value, model.p50, model.p85, model.p97, zoom);
            p.setPoints(Polygon.pointsAsCircle(d.center, radiusMeters));

            RiskInfo info = d.info;
            p.setOnClickListener((polygon, mapView, eventPos) -> {
                showRiskInfoCard(info);
                return true;
            });

            d.polygon = p;

            activeHeatmapLayers.add(p);
            riskInfoByPolygon.put(p, info);
            map.getOverlays().add(p);
        }

        currentModel = model;
        map.invalidate();
    }

    private void updatePolygonsForCurrentZoom() {
        if (currentModel == null || currentModel.byIso.isEmpty()) return;

        double zoom = map.getZoomLevelDouble();
        float strokeW = strokeWidthForZoom(zoom);
        float glowR = glowRadiusForZoom(zoom);

        for (RenderDatum d : currentModel.byIso.values()) {
            if (d == null || d.polygon == null || d.center == null) continue;

            double radiusMeters = computeRadiusMeters(d.value, currentModel.p50, currentModel.p85, currentModel.p97, zoom);
            d.polygon.setPoints(Polygon.pointsAsCircle(d.center, radiusMeters));

            Paint outline = d.polygon.getOutlinePaint();
            outline.setStrokeWidth(strokeW);
            outline.setMaskFilter(new BlurMaskFilter(glowR, BlurMaskFilter.Blur.OUTER));
        }

        map.invalidate();
    }

    private float strokeWidthForZoom(double zoom) {
        double z = Math.max(3.0, Math.min(18.0, zoom));
        return (float) (6.0 - ((z - 3.0) / 15.0) * 3.8); // ~6px -> ~2.2px
    }

    private float glowRadiusForZoom(double zoom) {
        double z = Math.max(3.0, Math.min(18.0, zoom));
        return (float) (14.0 - ((z - 3.0) / 15.0) * 8.0); // ~14px -> ~6px
    }

    private String buildQuickNotes(String label, boolean isPrediction) {
        String basis = isPrediction
                ? "This is a synthetic projection (not live surveillance)."
                : "This is a country-level indicator signal (not a diagnosis).";

        String action;
        switch (label) {
            case "Critical": action = "Prioritize prevention measures and review local health advisories."; break;
            case "Moderate": action = "Maintain precautions; monitor community updates."; break;
            case "Elevated": action = "Risk is above baseline; stay informed and use routine prevention steps."; break;
            default: action = "Risk appears lower relative to other regions for this selection."; break;
        }
        return basis + "\n\nQuick note: " + action;
    }

    private double percentile(List<Double> sorted, double p) {
        double idx = p * (sorted.size() - 1);
        int i = (int) Math.floor(idx);
        int j = Math.min(i + 1, sorted.size() - 1);
        double frac = idx - i;
        return sorted.get(i) * (1 - frac) + sorted.get(j) * frac;
    }

    private double computeRadiusMeters(double x, double p50, double p85, double p97, double zoom) {
        double base = 230000;

        double severity;
        if (x >= p97) severity = 1.00;
        else if (x >= p85) severity = 0.72;
        else if (x >= p50) severity = 0.52;
        else severity = 0.36;

        double z = Math.max(3.0, Math.min(18.0, zoom));
        double zoomFactor = 1.0 - ((z - 3.0) / 15.0) * 0.70; // 1.0 -> 0.30

        return base * severity * zoomFactor;
    }

    private void showRiskInfoCard(RiskInfo info) {
        if (info == null) return;

        String countryName = CountryMapper.getNameByIso(info.iso3);
        if (countryName == null) countryName = info.iso3;

        tvInfoTitle.setText(countryName);

        String subtitle = info.disease + " • shown: " + info.yearShown;
        if (!info.isPrediction && info.effectiveYear != info.yearShown) subtitle += " • data: " + info.effectiveYear;
        tvInfoSubtitle.setText(subtitle);

        tvInfoBadge.setText(info.label.toUpperCase(Locale.ROOT));
        if ("Critical".equals(info.label)) tvInfoBadge.setBackgroundColor(Color.parseColor("#CCB71C1C"));
        else if ("Moderate".equals(info.label)) tvInfoBadge.setBackgroundColor(Color.parseColor("#CCB26A00"));
        else if ("Elevated".equals(info.label)) tvInfoBadge.setBackgroundColor(Color.parseColor("#CC2E7D32"));
        else tvInfoBadge.setBackgroundColor(Color.parseColor("#CC1B5E20"));

        String valueLine = String.format(Locale.ROOT, "Value: %.2f", info.value);
        String statusLine = info.isPrediction ? "Status: Synthetic projection" : "Status: Observed indicator";
        String sourceLine = "Source: " + info.source;

        String updatedLine = "";
        if (info.updatedEpoch != null) {
            updatedLine = "\nUpdated: " + DateFormat.getDateTimeInstance().format(new Date(info.updatedEpoch));
        }

        tvInfoBody.setText(valueLine + "\n" + statusLine + "\n" + sourceLine + updatedLine + "\n\n" + info.notes);

        View scrim = findViewById(R.id.riskInfoScrim);
        scrim.setVisibility(View.VISIBLE);
        infoCardRoot.setVisibility(View.VISIBLE);

        infoCardRoot.setAlpha(0f);
        infoCardRoot.setTranslationY(40f);
        infoCardRoot.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(220)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .start();
    }

    private void hideRiskInfoCard(boolean animate) {
        View scrim = findViewById(R.id.riskInfoScrim);
        if (!animate) {
            scrim.setVisibility(View.GONE);
            infoCardRoot.setVisibility(View.GONE);
            return;
        }
        if (infoCardRoot.getVisibility() != View.VISIBLE) {
            scrim.setVisibility(View.GONE);
            return;
        }
        infoCardRoot.animate()
                .alpha(0f)
                .translationY(40f)
                .setDuration(180)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .withEndAction(() -> {
                    infoCardRoot.setVisibility(View.GONE);
                    infoCardRoot.setAlpha(1f);
                    infoCardRoot.setTranslationY(0f);
                    scrim.setVisibility(View.GONE);
                })
                .start();
    }

    private void applyBadge(String text, String hexColor) {
        tvBadge.setVisibility(View.VISIBLE);
        tvBadge.setText(text);
        tvBadge.setBackgroundColor(Color.parseColor(hexColor));
    }

 
    private void enableUserLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {

            locationOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(this), map);
            locationOverlay.enableMyLocation();
            map.getOverlays().add(locationOverlay);

            locationOverlay.runOnFirstFix(() -> runOnUiThread(() -> {
                if (!didCenterOnUser && locationOverlay.getMyLocation() != null) {
                    didCenterOnUser = true;
                    map.getController().animateTo(locationOverlay.getMyLocation(), 11.5, 1200L);
                }
            }));

            return;
        }

        ActivityCompat.requestPermissions(
                this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                REQ_LOCATION
        );
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_LOCATION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) enableUserLocation();
            else Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show();
        }
    }

    static class CountryMapper {
        private static final String ASSET_FILE = "countries.json";

        private static final Map<String, GeoPoint> isoToPoint = new HashMap<>();
        private static final Map<String, GeoPoint> nameToPoint = new HashMap<>();
        private static final Map<String, String> isoToName = new HashMap<>();
        private static boolean isInitialized = false;

        static class Country {
            String name;
            String code;
            double lat;
            double lon;
        }

        static synchronized void init(Context c) {
            if (isInitialized) return;

            try {
                String json = readAssetAsString(c, ASSET_FILE);
                Type listType = new TypeToken<List<Country>>() {}.getType();
                List<Country> countries = new Gson().fromJson(json, listType);

                if (countries != null) {
                    for (Country country : countries) {
                        if (country == null || country.code == null) continue;

                        GeoPoint gp = new GeoPoint(country.lat, country.lon);
                        String iso = country.code.trim().toUpperCase(Locale.ROOT);

                        isoToPoint.put(iso, gp);
                        if (country.name != null) {
                            isoToName.put(iso, country.name);
                            nameToPoint.put(normalize(country.name), gp);
                        }
                    }
                }

                isInitialized = true;
                Log.i(TAG, "CountryMapper loaded " + isoToPoint.size() + " countries.");

            } catch (Exception e) {
                Log.e(TAG, "Failed to load countries.json; using minimal fallback", e);

                isoToPoint.put("NGA", new GeoPoint(9.08, 8.67));
                isoToPoint.put("IND", new GeoPoint(20.59, 78.96));
                isoToPoint.put("BRA", new GeoPoint(-14.2350, -51.9253));
                isoToPoint.put("USA", new GeoPoint(37.0902, -95.7129));

                isoToName.put("NGA", "Nigeria");
                isoToName.put("IND", "India");
                isoToName.put("BRA", "Brazil");
                isoToName.put("USA", "United States");

                nameToPoint.put(normalize("Nigeria"), isoToPoint.get("NGA"));
                nameToPoint.put(normalize("India"), isoToPoint.get("IND"));
                nameToPoint.put(normalize("Brazil"), isoToPoint.get("BRA"));
                nameToPoint.put(normalize("United States"), isoToPoint.get("USA"));

                isInitialized = true;
            }
        }

        static GeoPoint getCoords(String iso) {
            if (iso == null) return null;
            return isoToPoint.get(iso.trim().toUpperCase(Locale.ROOT));
        }

        static String getNameByIso(String iso) {
            if (iso == null) return null;
            return isoToName.get(iso.trim().toUpperCase(Locale.ROOT));
        }

        static GeoPoint findCoordsByName(String input) {
            if (input == null) return null;
            String trimmed = input.trim();
            if (trimmed.isEmpty()) return null;

            if (trimmed.length() == 3) {
                GeoPoint iso = getCoords(trimmed);
                if (iso != null) return iso;
            }

            GeoPoint byName = nameToPoint.get(normalize(trimmed));
            if (byName != null) return byName;

            String norm = normalize(trimmed);
            for (Map.Entry<String, GeoPoint> e : nameToPoint.entrySet()) {
                if (e.getKey().contains(norm)) return e.getValue();
            }
            return null;
        }

        static List<String> getKnownIsos() {
            return new ArrayList<>(isoToPoint.keySet());
        }

        private static String normalize(String s) {
            return s.toLowerCase(Locale.ROOT)
                    .replace("&", "and")
                    .replaceAll("[^a-z0-9 ]", " ")
                    .replaceAll("\\s+", " ")
                    .trim();
        }

        private static String readAssetAsString(Context c, String assetFile) throws Exception {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(c.getAssets().open(assetFile)))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) sb.append(line);
                return sb.toString();
            }
        }
    }
}
