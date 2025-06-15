package com.cypher.zealth;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private DashboardAdapter adapter;
    private RecyclerView recycler;
    private GridLayoutManager glm;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        );

        setContentView(R.layout.activity_main);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        recycler = findViewById(R.id.recyclerDashboard);

        final int spanCount = (getResources().getConfiguration().smallestScreenWidthDp >= 600) ? 3 : 2;

        glm = new GridLayoutManager(this, spanCount);
        recycler.setLayoutManager(glm);

        recycler.setHasFixedSize(true);
        recycler.setItemViewCacheSize(12);


        recycler.setItemAnimator(null);

        adapter = new DashboardAdapter(this, buildItems());

        glm.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                return adapter.getSpanSize(position, spanCount);
            }
        });

    
        recycler.addItemDecoration(new TileSpacingDecoration(12));

        recycler.setAdapter(adapter);

        SearchView searchView = findViewById(R.id.searchView);
        if (searchView != null) {
            searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String query) {
                    adapter.filter(query);
                   
                    searchView.clearFocus();
                    return true;
                }

                @Override
                public boolean onQueryTextChange(String newText) {
                    adapter.filter(newText);
                    return true;
                }
            });
        }
    }

    @Override
    protected void onDestroy() {
   
        if (adapter != null) adapter.shutdown();
        super.onDestroy();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_about) {
            startActivity(new Intent(MainActivity.this, AboutActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private List<DashboardItem> buildItems() {
        List<DashboardItem> items = new ArrayList<>();

        items.add(DashboardItem.header("Wellness Tools"));
        items.add(DashboardItem.tile(
                "Health Places Map",
                "Find nearby clinics and services",
                "tile_heatmap",
                DashboardItem.TileStyle.WIDE,
                MapActivity.class
        ));
        items.add(DashboardItem.tile(
                "AI Wellness Guide",
                "General information and self-care tips",
                "tile_ai_doctor",
                DashboardItem.TileStyle.TALL,
                AiDoctorActivity.class
        ));
        items.add(DashboardItem.tile(
                "BMI Estimate",
                "Calculate BMI from your inputs",
                "tile_bmi",
                DashboardItem.TileStyle.SQUARE,
                BmiCalculatorActivity.class
        ));
        items.add(DashboardItem.tile(
                "Water Log",
                "Track daily hydration",
                "tile_water",
                DashboardItem.TileStyle.SQUARE,
                WaterIntakeActivity.class
        ));

        // MENTAL WELLBEING 
        items.add(DashboardItem.header("Mind & Wellbeing"));
        items.add(DashboardItem.tile(
                "Mood Journal",
                "Reflection and wellbeing notes",
                "tile_mental",
                DashboardItem.TileStyle.WIDE,
                MentalHealthActivity.class
        ));

        // WOMEN’S HEALTH (avoid “fertility tracking” claim; keep neutral cycle logging)
     //   items.add(DashboardItem.header("Cycle & Reproductive Health"));
  //      items.add(DashboardItem.tile(
    //            "Cycle Log",
     //           "Track cycle dates and reminders",
     //           "tile_womens",
    //            DashboardItem.TileStyle.SQUARE,
    //            WomensHealthActivity.class
     //   ));
     //   items.add(DashboardItem.tile(
     //           "Pregnancy Timeline",
    //            "Week-by-week reference content",
    //            "tile_pregnancy",
   //             DashboardItem.TileStyle.TALL,
   //             PregnancyTrackerActivity.class
   //     ));

        // LIFESTYLE (avoid cessation/treatment framing; keep as habit tracking)
        items.add(DashboardItem.header("Lifestyle"));
        items.add(DashboardItem.tile(
                "Weight Goals",
                "Set goals and track progress",
                "tile_weight",
                DashboardItem.TileStyle.WIDE,
                WeightGoalsActivity.class
        ));
        items.add(DashboardItem.tile(
                "Smoke-Free Tracker",
                "Track habits and milestones",
                "tile_smoking",
                DashboardItem.TileStyle.SQUARE,
                SmokingCessationActivity.class
        ));
        items.add(DashboardItem.tile(
                "Grocery List",
                "Build a food shopping list",
                "tile_groceries",
                DashboardItem.TileStyle.SQUARE,
                HealthyGroceriesActivity.class
        ));

        return items;
    }

}
