package com.cypher.zealth;

public class ChatMessage {
    public final boolean isUser;
    public final String text;

    private ChatMessage(boolean isUser, String text) {
        this.isUser = isUser;
        this.text = text;
    }

    public static ChatMessage user(String text) {
        return new ChatMessage(true, text);
    }

    public static ChatMessage bot(String text) {
        return new ChatMessage(false, text);
    }
}
