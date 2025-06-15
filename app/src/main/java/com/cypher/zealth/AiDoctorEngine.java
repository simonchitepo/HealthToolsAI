package com.cypher.zealth;

import java.util.List;
import java.util.Locale;

public class AiDoctorEngine {

    public static String respond(String userInput, List<ChatMessage> conversation) {
        String raw = userInput == null ? "" : userInput.trim();
        String t = normalize(raw);

        if (containsAny(t, "suicidal", "suicide", "kill myself", "end my life", "self harm", "self-harm")) {
            return "I’m really sorry you’re feeling this way. I can’t help with self-harm.\n\n" +
                    "If you might act on these thoughts or feel unsafe, call your local emergency number now.\n" +
                    "If you can, reach out to someone you trust and stay with them.\n\n" +
                    "If you tell me your country, I can suggest crisis resources.";
        }

        String redFlag = checkRedFlags(t);
        if (redFlag != null) return redFlag;

        if (looksLikeTooLittleInfo(t)) {
            return "Tell me the main symptom and how long it’s been going on.";
        }

        return "Got it. What’s the main symptom, how long has it been happening, and is it mild/moderate/severe?";
    }

    public static String buildPromptForNative(String userInput, List<ChatMessage> conversation) {
        StringBuilder sb = new StringBuilder(2048);

        sb.append("<|system|>\n")
                .append("You are a cautious symptom-triage assistant. ")
                .append("If emergency red flags exist, advise emergency care. ")
                .append("Otherwise ask minimal follow-up questions, give 2–4 likely causes, ")
                .append("simple self-care, and when to seek in-person care. ")
                .append("Keep responses concise.\n")
                .append("</s>\n");

        if (conversation != null && !conversation.isEmpty()) {
            int start = Math.max(conversation.size() - 8, 0);
            for (int i = start; i < conversation.size(); i++) {
                ChatMessage m = conversation.get(i);
                if (m == null || m.text == null) continue;

                if (m.isUser) {
                    sb.append("<|user|>\n").append(m.text).append("\n</s>\n");
                } else {
                    if ("…".equals(m.text)) continue;
                    sb.append("<|assistant|>\n").append(m.text).append("\n</s>\n");
                }
            }
        }

        sb.append("<|user|>\n")
                .append(userInput == null ? "" : userInput)
                .append("\n</s>\n")
                .append("<|assistant|>\n");

        return sb.toString();
    }

    private static String checkRedFlags(String t) {
        if (containsAny(t,
                "crushing chest pain", "blue lips", "turning blue",
                "severe shortness of breath", "cannot breathe", "cant breathe",
                "unconscious", "unresponsive", "seizure",
                "face droop", "slurred speech", "one sided weakness", "one-sided weakness",
                "worst headache", "vomiting blood", "coughing blood",
                "throat closing", "swollen tongue", "anaphylaxis"
        )) {
            return "This could be an emergency. If you have severe symptoms right now, call your local emergency number immediately.";
        }
        return null;
    }

    private static String normalize(String s) {
        return s == null ? "" : s.trim().toLowerCase(Locale.US);
    }

    private static boolean looksLikeTooLittleInfo(String t) {
        if (t == null) return true;
        String trimmed = t.trim();
        if (trimmed.isEmpty()) return true;
        String[] parts = trimmed.split("\\s+");
        return parts.length <= 2 && trimmed.length() <= 14;
    }

    private static boolean containsAny(String t, String... needles) {
        if (t == null) return false;
        for (String n : needles) if (t.contains(n)) return true;
        return false;
    }
}
