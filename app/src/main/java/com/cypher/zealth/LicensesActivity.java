package com.cypher.zealth;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

public class LicensesActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_licenses);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        TextView tv = findViewById(R.id.tvLicenses);
        tv.setText(readRawText(R.raw.oss_licenses));
    }

    private String readRawText(int resId) {
        StringBuilder sb = new StringBuilder();
        try (InputStream is = getResources().openRawResource(resId);
             BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line).append('\n');
            }
        } catch (Exception e) {
            sb.append("Licenses could not be loaded.");
        }
        return sb.toString();
    }
}
