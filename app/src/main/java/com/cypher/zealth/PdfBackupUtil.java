package com.cypher.zealth;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.text.TextUtils;
import android.util.Base64;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class PdfBackupUtil {

    private static final String BEGIN = "ZBACKUP_BEGIN";
    private static final String END = "ZBACKUP_END";

    private PdfBackupUtil() {}

    public static void exportToPdf(List<MentalEntry> data, OutputStream os) throws Exception {
        if (data == null) data = new ArrayList<>();

        android.graphics.pdf.PdfDocument doc = new android.graphics.pdf.PdfDocument();
        final int pageW = 595;  
        final int pageH = 842;

        Paint title = new Paint(Paint.ANTI_ALIAS_FLAG);
        title.setColor(Color.BLACK);
        title.setTextSize(18f);
        title.setFakeBoldText(true);

        Paint body = new Paint(Paint.ANTI_ALIAS_FLAG);
        body.setColor(Color.BLACK);
        body.setTextSize(10f);

        Paint mono = new Paint(Paint.ANTI_ALIAS_FLAG);
        mono.setColor(Color.DKGRAY);
        mono.setTextSize(7.5f);
        mono.setTypeface(android.graphics.Typeface.MONOSPACE);

        StringBuilder payload = new StringBuilder();
        payload.append(BEGIN).append("\n");
        for (MentalEntry e : data) {
            payload.append(encodeEntryLine(e)).append("\n");
        }
        payload.append(END).append("\n");

        int pageNum = 1;
        android.graphics.pdf.PdfDocument.Page page = doc.startPage(
                new android.graphics.pdf.PdfDocument.PageInfo.Builder(pageW, pageH, pageNum).create()
        );
        Canvas c = page.getCanvas();

        float x = 36;
        float y = 48;

        c.drawText("Zealth Mental Health Backup", x, y, title);
        y += 18;
        c.drawText("This PDF contains your exported entries and a Zealth import block.", x, y, body);
        y += 14;
        c.drawText("Do not edit the backup block if you want to re-import.", x, y, body);
        y += 18;

        String[] lines = payload.toString().split("\n");
        float lineH = 9.5f;
        float maxY = pageH - 36;

        for (String line : lines) {
            if (y + lineH > maxY) {
                doc.finishPage(page);
                pageNum++;
                page = doc.startPage(new android.graphics.pdf.PdfDocument.PageInfo.Builder(pageW, pageH, pageNum).create());
                c = page.getCanvas();
                y = 36;
            }
            c.drawText(line, x, y, mono);
            y += lineH;
        }

        doc.finishPage(page);
        doc.writeTo(os);
        doc.close();
    }

    public static List<MentalEntry> importFromPdf(InputStream is) throws Exception {
        byte[] bytes = readAllBytes(is);

        String blob = new String(bytes, "ISO-8859-1");

        int a = blob.indexOf(BEGIN);
        if (a < 0) throw new IllegalArgumentException("Missing backup block");
        int b = blob.indexOf(END, a + BEGIN.length());
        if (b < 0) throw new IllegalArgumentException("Missing backup block end");

        String block = blob.substring(a + BEGIN.length(), b).trim();
        List<MentalEntry> out = new ArrayList<>();
        if (block.isEmpty()) return out;

        for (String raw : block.split("\n")) {
            String line = raw.trim();
            if (line.isEmpty()) continue;
            MentalEntry e = decodeEntryLine(line);
            if (e != null) out.add(e);
        }

        Collections.sort(out, (x, y) -> Long.compare(y.timestampMs, x.timestampMs));
        return out;
    }


    private static String encodeEntryLine(MentalEntry e) {
        long id = e.id;
        long ts = e.timestampMs;
        String type = e.type == null ? "MOOD" : e.type.name();
        int mood = e.mood;

        String note = e.note == null ? "" : e.note;
        String journal = e.journalText == null ? "" : e.journalText;

        String tagsJoin = "";
        if (e.tags != null && !e.tags.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < e.tags.size(); i++) {
                if (i > 0) sb.append('\u001F'); 
                sb.append(e.tags.get(i));
            }
            tagsJoin = sb.toString();
        }

        return id + "|" + ts + "|" + type + "|" + mood + "|"
                + b64(note) + "|" + b64(tagsJoin) + "|" + b64(journal);
    }

    private static MentalEntry decodeEntryLine(String line) {
        try {
            String[] p = line.split("\\|", -1);
            if (p.length < 7) return null;

            long id = Long.parseLong(p[0]);
            long ts = Long.parseLong(p[1]);
            MentalEntry.Type type = MentalEntry.Type.valueOf(p[2]);
            int mood = Integer.parseInt(p[3]);

            String note = ub64(p[4]);
            String tagsJoin = ub64(p[5]);
            String journal = ub64(p[6]);

            MentalEntry e = new MentalEntry();
            e.id = id;
            e.timestampMs = ts;
            e.type = type;
            e.mood = mood;
            e.note = note;

            e.tags = new ArrayList<>();
            if (!TextUtils.isEmpty(tagsJoin)) {
                String[] parts = tagsJoin.split(String.valueOf('\u001F'), -1);
                for (String t : parts) {
                    if (!TextUtils.isEmpty(t)) e.tags.add(t);
                }
            }

            e.journalText = journal;
            return e;
        } catch (Exception ignored) {
            return null;
        }
    }

    private static String b64(String s) {
        if (s == null) s = "";
        return Base64.encodeToString(s.getBytes(StandardCharsets.UTF_8), Base64.NO_WRAP);
    }

    private static String ub64(String b64) {
        if (b64 == null) return "";
        try {
            byte[] bytes = Base64.decode(b64, Base64.NO_WRAP);
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return "";
        }
    }

    private static byte[] readAllBytes(InputStream is) throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = is.read(buf)) > 0) bos.write(buf, 0, n);
        return bos.toByteArray();
    }
}
