package com.cypher.zealth;

import android.content.Context;
import android.text.TextUtils;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class EmergencyResources {

    public static class EmergencyOption {
        public final String title;
        public final String subtitle;
        public final String phone; 
        public final String url;   

        public EmergencyOption(String title, String subtitle, String phone, String url) {
            this.title = title;
            this.subtitle = subtitle;
            this.phone = phone;
            this.url = url;
        }
    }

    private final Map<String, List<EmergencyOption>> map = new HashMap<>();

    public void loadFromAssets(Context ctx, String assetName) {
        try (InputStream is = ctx.getAssets().open(assetName)) {
            String blob = new String(readAllBytes(is), "UTF-8");
            parseBlock(blob);
        } catch (Exception ignored) { }
    }

    public void loadFromAssetsPdf(Context ctx, String assetPdfName) {
        try (InputStream is = ctx.getAssets().open(assetPdfName)) {

            String blob = new String(readAllBytes(is), "ISO-8859-1");
            parseBlock(blob);
        } catch (Exception ignored) { }
    }

    private void parseBlock(String blob) {
        String block = extractBlock(blob, "ZEMERGENCY_BEGIN", "ZEMERGENCY_END");
        if (TextUtils.isEmpty(block)) return;

        for (String raw : block.split("\n")) {
            String line = raw.trim();
            if (line.isEmpty()) continue;

            String[] p = line.split("\\|", -1);
            if (p.length < 3) continue;

            String iso = safeUpper(p[0]);
            String title = p[1].trim();
            String sub = p[2].trim();
            String phone = p.length >= 4 ? p[3].trim() : "";
            String url = p.length >= 5 ? p[4].trim() : "";

            if (TextUtils.isEmpty(iso) || TextUtils.isEmpty(title)) continue;

            List<EmergencyOption> list = map.get(iso);
            if (list == null) list = new ArrayList<>();
            list.add(new EmergencyOption(title, sub, phone, url));
            map.put(iso, list);
        }
    }

    public List<EmergencyOption> getForCountry(String iso) {
        if (TextUtils.isEmpty(iso)) return getFallbackFor("DEFAULT");
        iso = iso.toUpperCase(Locale.US);

        List<EmergencyOption> direct = map.get(iso);
        if (direct != null && !direct.isEmpty()) return direct;

        List<EmergencyOption> def = map.get("DEFAULT");
        if (def != null && !def.isEmpty()) return def;

        return new ArrayList<>();
    }

    public List<EmergencyOption> getFallbackFor(String iso) {
        iso = safeUpper(iso);
        List<EmergencyOption> list = new ArrayList<>();

        if ("US".equals(iso) || "CA".equals(iso)) {
            list.add(new EmergencyOption(
                    "988 Suicide & Crisis Lifeline",
                    "24/7 crisis support (call or text 988)",
                    "988",
                    ""
            ));
        }
        if ("GB".equals(iso) || "UK".equals(iso)) {
            list.add(new EmergencyOption(
                    "Samaritans",
                    "24/7 listening line",
                    "116123",
                    ""
            ));
        }

        list.add(new EmergencyOption(
                "FindAHelpline",
                "Global directory for local crisis resources",
                "",
                "https://findahelpline.com/"
        ));

        return list;
    }

    public static String emergencyNumberFor(String iso) {
        iso = safeUpper(iso);
        if ("US".equals(iso) || "CA".equals(iso)) return "911";
        if ("GB".equals(iso) || "UK".equals(iso)) return "999";
        if ("AU".equals(iso)) return "000";
        return "112";
    }

    private static String extractBlock(String blob, String begin, String end) {
        int a = blob.indexOf(begin);
        if (a < 0) return "";
        int b = blob.indexOf(end, a + begin.length());
        if (b < 0) return "";
        return blob.substring(a + begin.length(), b).trim();
    }

    private static String safeUpper(String s) {
        if (s == null) return "";
        String t = s.trim();
        if (t.isEmpty()) return "";
        return t.toUpperCase(Locale.US);
    }

    private static byte[] readAllBytes(InputStream is) throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = is.read(buf)) > 0) bos.write(buf, 0, n);
        return bos.toByteArray();
    }
}
