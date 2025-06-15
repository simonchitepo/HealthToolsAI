package com.cypher.zealth;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;

public class AboutActivity extends AppCompatActivity {

    private static final String APP_VERSION = "1.0";
    private static final String PRIVACY_URL = "https://pxrishuh.github.io/PRIVACY.md/";
    private static final String SUPPORT_EMAIL = "support@zealth.app";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        TextView tvVersion = findViewById(R.id.tvVersion);
        tvVersion.setText("Version " + APP_VERSION);

        MaterialButton btnPrivacy = findViewById(R.id.btnPrivacy);
        MaterialButton btnLicenses = findViewById(R.id.btnLicenses);
        MaterialButton btnContact = findViewById(R.id.btnContact);

        btnPrivacy.setOnClickListener(v -> openUrl(PRIVACY_URL));

        btnLicenses.setOnClickListener(v ->
                startActivity(new Intent(AboutActivity.this, LicensesActivity.class))
        );

        btnContact.setOnClickListener(v ->
                composeEmail(SUPPORT_EMAIL, " Health Tools AI: Doctor and Map Support")
        );
    }

    private void openUrl(String url) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
        }
    }

    private void composeEmail(String to, String subject) {
        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setData(Uri.parse("mailto:" + Uri.encode(to)));
        intent.putExtra(Intent.EXTRA_SUBJECT, subject);

        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(Intent.createChooser(intent, "Contact Support"));
        }
    }
}
