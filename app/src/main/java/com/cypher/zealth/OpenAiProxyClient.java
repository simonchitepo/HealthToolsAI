package com.cypher.zealth;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public final class OpenAiProxyClient {

    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private final OkHttpClient http;

    public OpenAiProxyClient() {
        http = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(20, TimeUnit.SECONDS)
                .build();
    }

    public String chat(String proxyUrl, String userText) throws IOException {
        
        String bodyJson = "{"
                + "\"text\":" + jsonString(userText)
                + "}";

        Request req = new Request.Builder()
                .url(proxyUrl)
                .post(RequestBody.create(bodyJson, JSON))
                .build();

        try (Response resp = http.newCall(req).execute()) {
            if (!resp.isSuccessful()) {
                String err = resp.body() != null ? resp.body().string() : "";
                throw new IOException("Proxy HTTP " + resp.code() + ": " + err);
            }
            String raw = resp.body() != null ? resp.body().string() : "";
          
            return extractReply(raw);
        }
    }

 
    private static String jsonString(String s) {
        if (s == null) return "null";
        return "\"" + s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r") + "\"";
    }


    private static String extractReply(String json) throws IOException {
        if (json == null) return "";
        int idx = json.indexOf("\"reply\"");
        if (idx < 0) return json; 
        int colon = json.indexOf(':', idx);
        if (colon < 0) throw new IOException("Bad JSON (no :)");

        int firstQuote = json.indexOf('"', colon + 1);
        if (firstQuote < 0) return "";
        int i = firstQuote + 1;

        StringBuilder out = new StringBuilder();
        boolean esc = false;
        for (; i < json.length(); i++) {
            char c = json.charAt(i);
            if (esc) {
                if (c == 'n') out.append('\n');
                else if (c == 'r') out.append('\r');
                else out.append(c);
                esc = false;
            } else {
                if (c == '\\') esc = true;
                else if (c == '"') break;
                else out.append(c);
            }
        }
        return out.toString();
    }
}
