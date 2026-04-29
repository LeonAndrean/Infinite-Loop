package com.gereja.chatbot.model;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Model untuk satu pesan dalam percakapan chatbot.
 */
public class ChatMessage {

    public enum Sender { BOT, USER }
    public enum MessageType { TEXT, STEP_INFO, QUICK_REPLY, TYPING }

    private final Sender sender;
    private final String content;
    private final String timestamp;
    private final MessageType type;
    private String[] quickReplies;  // opsional
    private StepInfo[] steps;        // opsional untuk tipe STEP_INFO

    public ChatMessage(Sender sender, String content, MessageType type) {
        this.sender    = sender;
        this.content   = content;
        this.type      = type;
        this.timestamp = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
    }

    // ── Static factories ──────────────────────────────────────

    public static ChatMessage userMessage(String text) {
        return new ChatMessage(Sender.USER, text, MessageType.TEXT);
    }

    public static ChatMessage botMessage(String text) {
        return new ChatMessage(Sender.BOT, text, MessageType.TEXT);
    }

    public static ChatMessage botStepMessage(String title, StepInfo[] steps, String[] quickReplies) {
        ChatMessage msg = new ChatMessage(Sender.BOT, title, MessageType.STEP_INFO);
        msg.steps       = steps;
        msg.quickReplies = quickReplies;
        return msg;
    }

    // ── Getters ───────────────────────────────────────────────

    public Sender      getSender()      { return sender; }
    public String      getContent()     { return content; }
    public String      getTimestamp()   { return timestamp; }
    public MessageType getType()        { return type; }
    public String[]    getQuickReplies(){ return quickReplies; }
    public StepInfo[]  getSteps()       { return steps; }

    // ── Nested: StepInfo ─────────────────────────────────────

    public static class StepInfo {
        private final int    number;
        private final String title;
        private final String detail;
        private final String color;

        public StepInfo(int number, String title, String detail, String color) {
            this.number = number;
            this.title  = title;
            this.detail = detail;
            this.color  = color;
        }

        public int    getNumber() { return number; }
        public String getTitle()  { return title;  }
        public String getDetail() { return detail; }
        public String getColor()  { return color;  }
    }
}
