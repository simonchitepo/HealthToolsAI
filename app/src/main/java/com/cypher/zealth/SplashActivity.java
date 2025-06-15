package com.cypher.zealth;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.view.animation.DecelerateInterpolator;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.progressindicator.LinearProgressIndicator;

public class SplashActivity extends AppCompatActivity {

    private LinearProgressIndicator progressSplash;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        progressSplash = findViewById(R.id.progressSplash);

        startLoadingAnimation();
    }

  
    private void startLoadingAnimation() {

      
        progressSplash.setIndeterminate(false);
        progressSplash.setProgressCompat(0, false);

      
        ObjectAnimator animator =
                ObjectAnimator.ofInt(progressSplash, "progress", 0, 100);

        animator.setInterpolator(new DecelerateInterpolator());
        animator.start();

        animator.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                launchMain();
            }
        });
    }

    private void launchMain() {
        startActivity(new Intent(SplashActivity.this, MainActivity.class));
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
       
        if (progressSplash != null) {
            progressSplash.clearAnimation();
        }
    }
}
