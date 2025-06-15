package com.cypher.zealth;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class OpenAIClient {
    private static final String TAG = "OpenAIClient";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    // Classic Chat Completions endpoint:
    private static final String ENDPOINT = "https://api.openai.com/v1/chat/completions";

    private final OkHttpClient http = new OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build();

    public String chat(String userText) throws IOException {
        String apiKey = OpenAIConfig.API_KEY; 

        if (apiKey == null || apiKey.trim().isEmpty()) {
            return "Missing OpenAI API key (OpenAIConfig.API_KEY).";
        }

        try {
            JSONObject body = new JSONObject();
            body.put("model", OpenAIConfig.MODEL); 

            JSONArray messages = new JSONArray();
            messages.put(new JSONObject()
                    .put("role", "system")
                    .put("content", "You are a cautious symptom guidance assistant. Give safe, short advice and red flags."));
            messages.put(new JSONObject()
                    .put("role", "user")
                    .put("content", userText));

            body.put("messages", messages);
            body.put("max_tokens", 180);
            body.put("temperature", 0.4);

            Request req = new Request.Builder()
                    .url(ENDPOINT)
                    .addHeader("Authorization", "Bearer " + apiKey.trim())
                    .addHeader("Content-Type", "application/json")
                    .post(RequestBody.create(body.toString(), JSON))
                    .build();

            try (Response resp = http.newCall(req).execute()) {
                String raw = resp.body() != null ? resp.body().string() : "";

                if (!resp.isSuccessful()) {
                    Log.e(TAG, "HTTP " + resp.code() + " raw=" + raw);
                    return "AI error: HTTP " + resp.code() + "\n" + raw;
                }

                JSONObject json = new JSONObject(raw);
                return json.getJSONArray("choices")
                        .getJSONObject(0)
                        .getJSONObject("message")
                        .getString("content")
                        .trim();
            }

        } catch (Exception e) {
            Log.e(TAG, "JSON/build error", e);
            return "AI error: " + e.getMessage();
        }
    }
}
