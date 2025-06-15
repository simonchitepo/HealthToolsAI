package com.cypher.zealth;

import android.app.Activity;
import android.os.Build;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;

import eightbitlab.com.blurview.BlurView;
import eightbitlab.com.blurview.RenderScriptBlur;

import java.lang.reflect.Method;

public final class BlurUtil {

    private BlurUtil() {}

    public static void setupBlur(@NonNull Activity activity,
                                 @NonNull BlurView blurView,
                                 @NonNull View rootContent,
                                 float radius) {

        try {
            ViewGroup decorView = (ViewGroup) activity.getWindow().getDecorView();
            ViewGroup rootView = decorView.findViewById(android.R.id.content);

            Object rsBlur = new RenderScriptBlur(activity);

            Object chain = tryInvokeSetupWith(blurView, rootView, rsBlur);

            if (chain == null) chain = blurView;

            tryInvoke(chain, "setBlurAlgorithm",
                    new Class[]{Class.forName("eightbitlab.com.blurview.BlurAlgorithm")},
                    new Object[]{rsBlur});

            float finalRadius = radius;
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
                finalRadius = Math.min(radius, 12f);
            }

            tryInvoke(chain, "setBlurRadius",
                    new Class[]{float.class},
                    new Object[]{finalRadius});

            tryInvoke(chain, "setHasFixedTransformationMatrix",
                    new Class[]{boolean.class},
                    new Object[]{true});

        } catch (Throwable ignored) {

        }
    }

    private static Object tryInvokeSetupWith(BlurView blurView, ViewGroup rootView, Object rsBlur) {
        try {
            Class<?> blurAlgClass = Class.forName("eightbitlab.com.blurview.BlurAlgorithm");
            Method m = blurView.getClass().getMethod("setupWith", ViewGroup.class, blurAlgClass);
            return m.invoke(blurView, rootView, rsBlur);
        } catch (Throwable ignored) {}
        try {
            Method m = blurView.getClass().getMethod("setupWith", ViewGroup.class);
            return m.invoke(blurView, rootView);
        } catch (Throwable ignored) {}

        return null;
    }

    private static void tryInvoke(Object target, String methodName, Class<?>[] paramTypes, Object[] args) {
        try {
            Method m = target.getClass().getMethod(methodName, paramTypes);
            m.invoke(target, args);
        } catch (Throwable ignored) {}
    }
}
