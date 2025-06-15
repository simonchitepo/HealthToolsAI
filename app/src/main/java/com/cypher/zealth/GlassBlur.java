package com.cypher.zealth;

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewOutlineProvider;

import eightbitlab.com.blurview.BlurTarget;
import eightbitlab.com.blurview.BlurView;

public final class GlassBlur {

    private GlassBlur() {}

    public static Drawable getWindowBackground(Activity activity) {
        View decor = activity.getWindow().getDecorView();
        return decor.getBackground();
    }

    public static void setupGlass(BlurView blurView, BlurTarget target, Drawable windowBackground) {
        setupGlass(blurView, target, windowBackground, 18f);
    }

    public static void setupGlass(BlurView blurView, BlurTarget target, Drawable windowBackground, float radius) {
        if (blurView == null || target == null) return;

        blurView.setupWith(target)
                .setFrameClearDrawable(windowBackground)
                .setBlurRadius(radius);

        blurView.setOutlineProvider(ViewOutlineProvider.BACKGROUND);
        blurView.setClipToOutline(true);
    }
}
