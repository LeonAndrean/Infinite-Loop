package com.gereja.chatbot.controller;

import com.gereja.chatbot.model.ChatMessage;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;

import java.util.function.Consumer;

/**
 * ╔══════════════════════════════════════════════════════════╗
 * ║  ChatBubbleFactory                                       ║
 * ║  Membuat elemen UI untuk setiap jenis pesan chat:        ║
 * ║  • Bubble pesan pengguna                                 ║
 * ║  • Bubble teks bot                                       ║
 * ║  • Bubble langkah (step) bot                             ║
 * ║  • Indikator mengetik (typing indicator)                 ║
 * ╚══════════════════════════════════════════════════════════╝
 */
public class ChatBubbleFactory {

    // ── Style Constants ───────────────────────────────────────

    private static final String BOT_BG =
            "-fx-background-color: #FFFFFF; " +
                    "-fx-background-radius: 4 18 18 18; " +
                    "-fx-padding: 14 18; " +
                    "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.07), 8, 0, 2, 2);";

    private static final String USER_BG =
            "-fx-background-color: #1A3A2A; " +
                    "-fx-background-radius: 18 4 18 18; " +
                    "-fx-padding: 13 18; " +
                    "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.12), 8, 0, 2, 2);";

    private static final String CHIP_STYLE =
            "-fx-background-color: #FFFFFF; " +
                    "-fx-text-fill: #1A3A2A; " +
                    "-fx-font-size: 11px; " +
                    "-fx-background-radius: 16; " +
                    "-fx-border-color: #C8DDD0; " +
                    "-fx-border-radius: 16; " +
                    "-fx-border-width: 1.5; " +
                    "-fx-padding: 6 14; " +
                    "-fx-cursor: hand;";

    // ── State ─────────────────────────────────────────────────

    private final Consumer<String> onChipClick;

    // ── Constructor ───────────────────────────────────────────

    public ChatBubbleFactory(Consumer<String> onChipClick) {
        this.onChipClick = onChipClick;
    }

    // ══════════════════════════════════════════════════════════
    //  PUBLIC BUILDERS
    // ══════════════════════════════════════════════════════════

    /** Membuat bubble untuk pesan dari pengguna (kanan). */
    public Node buildUserBubble(ChatMessage msg) {
        HBox row = new HBox(12);
        row.setAlignment(Pos.TOP_RIGHT);

        VBox textBox = new VBox(6);
        textBox.setMaxWidth(420);

        // Timestamp
        HBox timeRow = new HBox();
        timeRow.setAlignment(Pos.CENTER_RIGHT);
        Label time = label("Anda  •  " + msg.getTimestamp(),
                "-fx-font-size: 10px; -fx-text-fill: #9A8A7A;");
        timeRow.getChildren().add(time);

        // Bubble teks
        VBox bubble = new VBox();
        bubble.setStyle(USER_BG);
        Label text = label(msg.getContent(),
                "-fx-font-size: 13px; -fx-text-fill: #F5F0EB; -fx-wrap-text: true;");
        text.setWrapText(true);
        bubble.getChildren().add(text);

        textBox.getChildren().addAll(timeRow, bubble);

        StackPane avatar = buildAvatar("U", "#D4A843", "#1A3A2A");
        row.getChildren().addAll(textBox, avatar);
        return row;
    }

    /** Membuat bubble teks biasa dari bot (kiri). */
    public Node buildBotTextBubble(ChatMessage msg) {
        HBox row = new HBox(12);
        row.setAlignment(Pos.TOP_LEFT);

        StackPane avatar = buildBotAvatar();

        VBox col = new VBox(6);
        col.setMaxWidth(520);

        Label time = label("Faith Buddy  •  " + msg.getTimestamp(),
                "-fx-font-size: 10px; -fx-text-fill: #9A8A7A;");

        VBox bubble = new VBox();
        bubble.setStyle(BOT_BG);
        Label text = label(msg.getContent(),
                "-fx-font-size: 13px; -fx-text-fill: #3D5045; -fx-wrap-text: true;");
        text.setWrapText(true);
        text.setMaxWidth(480);
        bubble.getChildren().add(text);

        col.getChildren().addAll(time, bubble);

        // Quick-reply chips (opsional)
        if (msg.getQuickReplies() != null && msg.getQuickReplies().length > 0) {
            col.getChildren().add(buildChips(msg.getQuickReplies()));
        }

        row.getChildren().addAll(avatar, col);
        return row;
    }

    /** Membuat bubble step-info dari bot (kiri), menampilkan langkah-langkah bernomor. */
    public Node buildBotStepBubble(ChatMessage msg) {
        HBox row = new HBox(12);
        row.setAlignment(Pos.TOP_LEFT);

        StackPane avatar = buildBotAvatar();

        VBox col = new VBox(8);
        col.setMaxWidth(540);

        Label time = label("Faith Buddy  •  " + msg.getTimestamp(),
                "-fx-font-size: 10px; -fx-text-fill: #9A8A7A;");

        // Card utama
        VBox card = new VBox();
        card.setStyle(BOT_BG);
        card.setSpacing(0);

        Label title = label(msg.getContent(),
                "-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #1A3A2A;");
        VBox.setMargin(title, new Insets(0, 0, 12, 0));
        card.getChildren().add(title);

        // Baris langkah-langkah
        if (msg.getSteps() != null) {
            for (int i = 0; i < msg.getSteps().length; i++) {
                ChatMessage.StepInfo step = msg.getSteps()[i];
                card.getChildren().add(buildStepRow(step));

                // Konektor antar langkah (kecuali langkah terakhir)
                if (i < msg.getSteps().length - 1) {
                    card.getChildren().add(buildConnector());
                }
            }
        }

        col.getChildren().addAll(time, card);

        // Quick-reply chips (opsional)
        if (msg.getQuickReplies() != null && msg.getQuickReplies().length > 0) {
            col.getChildren().add(buildChips(msg.getQuickReplies()));
        }

        row.getChildren().addAll(avatar, col);
        return row;
    }

    /** Membuat animasi indikator mengetik (tiga titik). */
    public Node buildTypingIndicator() {
        HBox row = new HBox(12);
        row.setAlignment(Pos.TOP_LEFT);

        StackPane avatar = buildBotAvatar();

        HBox dots = new HBox(6);
        dots.setAlignment(Pos.CENTER_LEFT);
        dots.setStyle(
                "-fx-background-color: #FFFFFF; " +
                        "-fx-background-radius: 4 18 18 18; " +
                        "-fx-padding: 14 20; " +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.07), 8, 0, 2, 2);"
        );
        for (int i = 0; i < 3; i++) {
            Circle dot = new Circle(4);
            dot.setStyle("-fx-fill: #A0B0A8;");
            dots.getChildren().add(dot);
        }

        row.getChildren().addAll(avatar, dots);
        return row;
    }

    // ══════════════════════════════════════════════════════════
    //  PRIVATE HELPERS
    // ══════════════════════════════════════════════════════════

    /** Membuat satu baris langkah dengan nomor lingkaran, judul, dan detail. */
    private HBox buildStepRow(ChatMessage.StepInfo step) {
        HBox row = new HBox(12);
        row.setAlignment(Pos.TOP_LEFT);

        // Lingkaran nomor
        StackPane numCircle = new StackPane();
        numCircle.setMinSize(28, 28);

        Circle circle = new Circle(14);
        circle.setStyle("-fx-fill: " + step.getColor() + ";");

        String numTextStyle = "#D4A843".equals(step.getColor())
                ? "-fx-text-fill: #1A3A2A; -fx-font-size: 11px; -fx-font-weight: bold;"
                : "-fx-text-fill: white; -fx-font-size: 11px; -fx-font-weight: bold;";
        Label num = label(String.valueOf(step.getNumber()), numTextStyle);

        numCircle.getChildren().addAll(circle, num);

        // Teks judul dan detail
        VBox info = new VBox(3);
        Label stepTitle  = label(step.getTitle(),
                "-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #1A3A2A;");
        Label stepDetail = label(step.getDetail(),
                "-fx-font-size: 11px; -fx-text-fill: #5A6A5E; -fx-wrap-text: true;");
        stepDetail.setWrapText(true);
        stepDetail.setMaxWidth(380);
        info.getChildren().addAll(stepTitle, stepDetail);

        row.getChildren().addAll(numCircle, info);
        return row;
    }

    /** Membuat garis konektor vertikal antar langkah. */
    private HBox buildConnector() {
        HBox line = new HBox();
        line.setPadding(new Insets(0, 0, 0, 13));

        Rectangle rect = new Rectangle(2, 12);
        rect.setStyle("-fx-fill: #C8DDD0;");
        line.getChildren().add(rect);

        return line;
    }

    /** Membuat baris chip (quick-reply button). */
    private FlowPane buildChips(String[] texts) {
        FlowPane pane = new FlowPane();
        pane.setHgap(8);
        pane.setVgap(8);
        pane.setPadding(new Insets(4, 0, 0, 0));

        for (String text : texts) {
            Button chip = new Button(text);
            chip.setStyle(CHIP_STYLE);
            chip.setOnAction(e -> onChipClick.accept(text));
            pane.getChildren().add(chip);
        }

        return pane;
    }

    /** Avatar khusus untuk bot (ikon salib ✝). */
    private StackPane buildBotAvatar() {
        return buildAvatar("✝", "#1A3A2A", "#D4A843");
    }

    /** Membuat avatar bulat dengan teks/ikon di tengah. */
    private StackPane buildAvatar(String text, String bgColor, String textColor) {
        StackPane pane = new StackPane();
        pane.setMinSize(40, 40);
        pane.setMaxSize(40, 40);

        Circle bg = new Circle(20);
        bg.setStyle("-fx-fill: " + bgColor + ";");

        Label lbl = label(text,
                "-fx-text-fill: " + textColor + "; -fx-font-size: 14px; -fx-font-weight: bold;");

        pane.getChildren().addAll(bg, lbl);
        return pane;
    }

    /** Shortcut untuk membuat Label dengan style tertentu. */
    private Label label(String text, String style) {
        Label l = new Label(text);
        l.setStyle(style);
        return l;
    }
}
