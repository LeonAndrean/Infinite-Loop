package com.gereja.chatbot.controller;

import com.gereja.chatbot.database.DatabaseHelper;
import com.gereja.chatbot.model.ChatMessage;
import com.gereja.chatbot.model.Notification;
import com.gereja.chatbot.service.ChatbotService;
import com.gereja.chatbot.service.NotificationService;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.ResourceBundle;

/**
 * ChatbotController – Controller utama Faith Buddy
 * Menampilkan ayat harian, quick reply chips, dan menangani semua chat.
 */



public class ChatbotController implements Initializable {

    // ── FXML ─────────────────────────────────────────────────
    @FXML private HBox      menuBeranda;
    @FXML private ScrollPane chatScrollPane;
    @FXML private VBox       chatContainer;
    @FXML private TextArea   messageInput;
    @FXML private VBox       sidebar;

    // Ayat Harian
    @FXML private VBox  ayatHarianBox;
    @FXML private Label lblAyatReferensi;
    @FXML private Label lblAyatIsi;
    @FXML private Label lblAyatKeterangan;

    // ── Services ─────────────────────────────────────────────
    private final ChatbotService      chatbotService      = new ChatbotService();
    private final NotificationService notificationService = new NotificationService();
    private       ChatBubbleFactory   bubbleFactory;

    // ── State ─────────────────────────────────────────────────
    private HBox activeMenuItem;
    private Node typingNode;

    private static final String MENU_ACTIVE =
            "-fx-background-color: #D4A843; -fx-background-radius: 10; -fx-padding: 11 16; -fx-cursor: hand;";
    private static final String MENU_INACTIVE =
            "-fx-background-color: transparent; -fx-background-radius: 10; -fx-padding: 11 16; -fx-cursor: hand;";
    private static final String MENU_LABEL_ACTIVE   =
            "-fx-text-fill: #122A1E; -fx-font-size: 13px; -fx-font-weight: bold;";
    private static final String MENU_LABEL_INACTIVE =
            "-fx-text-fill: #C8DDD0; -fx-font-size: 13px;";


    // ══════════════════════════════════════════════════════════
    //  INITIALIZE
    // ══════════════════════════════════════════════════════════

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        bubbleFactory = new ChatBubbleFactory(this::handleChipClick);

        setupMenuNavigation();
        setupInputHandling();
        setupScrollBehavior();

        // Load ayat harian dari DB
        loadAyatHarian();

        // Tampilkan pesan welcome
        Platform.runLater(() -> {
            ChatMessage welcome = chatbotService.getWelcomeMessage();
            appendBotMessage(welcome);
        });

        chatContainer.heightProperty().addListener((obs, oldH, newH) -> scrollToBottom());
    }


    // ══════════════════════════════════════════════════════════
    //  AYAT HARIAN
    // ══════════════════════════════════════════════════════════

    private void loadAyatHarian() {
        try {
            String[] ayat = DatabaseHelper.getAyatHarian();
            if (ayat != null && ayat.length >= 2) {
                lblAyatReferensi.setText(ayat[0]);
                lblAyatIsi.setText("\"" + ayat[1] + "\"");
                if (ayat.length >= 3 && ayat[2] != null && !ayat[2].isBlank()) {
                    lblAyatKeterangan.setText("— " + ayat[2]);
                }
                // Animasi fade in
                ayatHarianBox.setOpacity(0);
                FadeTransition ft = new FadeTransition(Duration.millis(800), ayatHarianBox);
                ft.setFromValue(0); ft.setToValue(1); ft.play();
            }
        } catch (Exception e) {
            System.out.println("[Controller] loadAyatHarian error: " + e.getMessage());
            ayatHarianBox.setManaged(false);
            ayatHarianBox.setVisible(false);
        }
    }


    // ══════════════════════════════════════════════════════════
    //  SETUP
    // ══════════════════════════════════════════════════════════

    private void setupMenuNavigation() {
        menuBeranda.setOnMouseClicked(e -> handleMenuBeranda());
    }

    private void setupInputHandling() {
        messageInput.setOnKeyPressed(event -> {
            switch (event.getCode()) {
                case ENTER -> {
                    if (event.isShiftDown()) {
                        // allow newline
                    } else {
                        event.consume();
                        sendMessage();
                    }
                }
                default -> {}
            }
        });
    }

    private void setupScrollBehavior() {
        chatScrollPane.setFitToWidth(true);
        chatScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        chatScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
    }

    // ══════════════════════════════════════════════════════════
    //  MENU HANDLERS
    // ══════════════════════════════════════════════════════════

    @FXML
    public void handleToggleSidebar() {
        if (sidebar != null) { // Tambahkan pengecekan null agar lebih aman
            if (sidebar.isVisible()) {
                sidebar.setVisible(false);
                sidebar.setManaged(false);
            } else {
                sidebar.setVisible(true);
                sidebar.setManaged(true);
            }
        }
    }

    @FXML private void handleMenuBeranda() {
        setActiveMenu(menuBeranda);
        chatbotService.resetState();
        appendBotMessage(chatbotService.getWelcomeMessage());
    }

    @FXML public void handleMenuPembaptisan() {
        addUserMessage("💧 Informasi Pembaptisan");
        showTypingThenRespond(List.of(chatbotService.getInfoBaptis()));
    }

    @FXML public void handleMenuPernikahan() {
        addUserMessage("💍 Informasi Pernikahan Gerejawi");
        showTypingThenRespond(List.of(chatbotService.getInfoPernikahan()));
    }

    @FXML public void handleMenuSidi() {
        addUserMessage("📖 Informasi SIDI / Peneguhan");
        showTypingThenRespond(List.of(chatbotService.getInfoSidi()));
    }

    @FXML public void handleMenuKonseling() {
        addUserMessage("🤝 Informasi Konseling Pastoral");
        showTypingThenRespond(List.of(chatbotService.getInfoKonseling()));
    }

    @FXML public void handleMenuJadwal() {
        addUserMessage("📅 Jadwal & Pendaftaran");
        showTypingThenRespond(List.of(chatbotService.getInfoJadwal()));
    }

    @FXML public void handleMenuKontak() {
        addUserMessage("📞 Kontak & Lokasi Gereja");
        showTypingThenRespond(chatbotService.processInput("kontak"));
    }

    @FXML private void handleMenuNotifikasi() {
        long unread = notificationService.getUnreadCount();
        notificationService.markAllRead();
        ChatMessage info = ChatMessage.botMessage(
                "🔔 Notifikasi Anda (" + unread + " belum dibaca):\n\n" +
                        buildNotificationSummary()
        );
        appendBotMessage(info);
    }

    @FXML private void handleMenuRiwayat() {
        var riwayat = DatabaseHelper.getRiwayatChat(10);
        StringBuilder sb = new StringBuilder("📜 Riwayat 10 percakapan terakhir:\n\n");
        if (riwayat.isEmpty()) {
            sb.append("Belum ada riwayat percakapan.");
        } else {
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("HH:mm");
            for (int i = riwayat.size() - 1; i >= 0; i--) {
                String[] row = riwayat.get(i);
                String icon = "USER".equals(row[0]) ? "👤" : "🤖";
                sb.append(icon).append(" ").append(row[0])
                        .append(" [").append(row[2]).append("]\n")
                        .append("   ").append(row[1]).append("\n\n");
            }
        }
        appendBotMessage(ChatMessage.botMessage(sb.toString()));
    }

    @FXML public void handleQuickBantuan() {
        addUserMessage("❓ Bantuan");
        showTypingThenRespond(chatbotService.processInput("halo"));
    }

    // ══════════════════════════════════════════════════════════
    //  SEND MESSAGE
    // ══════════════════════════════════════════════════════════

    @FXML public void sendMessage() {
        String text = messageInput.getText().trim();
        if (text.isEmpty()) return;
        messageInput.clear();

        addUserMessage(text);
        List<ChatMessage> responses = chatbotService.processInput(text);
        showTypingThenRespond(responses);
    }

    // ══════════════════════════════════════════════════════════
    //  CHIP CLICK
    // ══════════════════════════════════════════════════════════

    private void handleChipClick(String chipText) {
        addUserMessage(chipText);
        List<ChatMessage> responses = chatbotService.processInput(chipText);
        showTypingThenRespond(responses);
    }

    // ══════════════════════════════════════════════════════════
    //  CHAT UI
    // ══════════════════════════════════════════════════════════

    private void addUserMessage(String text) {
        ChatMessage msg = ChatMessage.userMessage(text);
        Node bubble = bubbleFactory.buildUserBubble(msg);
        animateIn(bubble);
        chatContainer.getChildren().add(bubble);
        scrollToBottom();
    }

    private void appendBotMessage(ChatMessage msg) {
        Node bubble = buildBotNode(msg);
        animateIn(bubble);
        chatContainer.getChildren().add(bubble);
        scrollToBottom();
    }

    private void showTypingThenRespond(List<ChatMessage> responses) {
        if (responses == null || responses.isEmpty()) return;

        typingNode = bubbleFactory.buildTypingIndicator();
        animateIn(typingNode);
        chatContainer.getChildren().add(typingNode);
        scrollToBottom();

        int totalLength = responses.stream().mapToInt(m -> m.getContent().length()).sum();
        long delayMs = Math.min(600 + totalLength * 4L, 2200);

        PauseTransition delay = new PauseTransition(Duration.millis(delayMs));
        delay.setOnFinished(e -> Platform.runLater(() -> {
            chatContainer.getChildren().remove(typingNode);
            typingNode = null;
            for (ChatMessage msg : responses) appendBotMessage(msg);
        }));
        delay.play();
    }

    private Node buildBotNode(ChatMessage msg) {
        return switch (msg.getType()) {
            case STEP_INFO -> bubbleFactory.buildBotStepBubble(msg);
            default        -> bubbleFactory.buildBotTextBubble(msg);
        };
    }

    // ══════════════════════════════════════════════════════════
    //  NOTIFIKASI
    // ══════════════════════════════════════════════════════════

    private String buildNotificationSummary() {
        StringBuilder sb = new StringBuilder();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("EEEE, dd MMM", new Locale("id", "ID"));
        for (Notification n : notificationService.getNotifications()) {
            if (n.getCategory() == Notification.Category.JADWAL) {
                sb.append(n.getEmoji()).append(" ").append(n.getTitle()).append("\n")
                        .append("   📍 ").append(n.getLocation()).append("\n")
                        .append("   📅 ").append(n.getDate().format(fmt))
                        .append(" (").append(n.daysUntil()).append(" hari lagi)\n\n");
            } else if (n.getCategory() == Notification.Category.DEADLINE) {
                sb.append("⏰ DEADLINE: ").append(n.getTitle()).append("\n")
                        .append("   Sisa: ").append(n.daysUntil()).append(" hari\n\n");
            }
        }
        return sb.toString().trim();
    }

    // ══════════════════════════════════════════════════════════
    //  ACTIVE MENU
    // ══════════════════════════════════════════════════════════

    private void setActiveMenu(HBox newActive) {
        if (activeMenuItem != null && activeMenuItem != newActive) {
            activeMenuItem.setStyle(MENU_INACTIVE);
            setMenuLabelStyle(activeMenuItem, MENU_LABEL_INACTIVE);
        }
        newActive.setStyle(MENU_ACTIVE);
        setMenuLabelStyle(newActive, MENU_LABEL_ACTIVE);
        activeMenuItem = newActive;
    }

    private void setMenuLabelStyle(HBox menu, String style) {
        menu.getChildren().stream()
                .filter(n -> n instanceof Label)
                .map(n -> (Label) n)
                .filter(l -> !l.getText().isEmpty() && l.getText().length() > 1)
                .forEach(l -> l.setStyle(style));
    }

    // ══════════════════════════════════════════════════════════
    //  ANIMASI
    // ══════════════════════════════════════════════════════════

    private void animateIn(Node node) {
        node.setOpacity(0);
        node.setTranslateY(10);
        Timeline tl = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(node.opacityProperty(), 0),
                        new KeyValue(node.translateYProperty(), 10)),
                new KeyFrame(Duration.millis(280),
                        new KeyValue(node.opacityProperty(), 1, Interpolator.EASE_OUT),
                        new KeyValue(node.translateYProperty(), 0, Interpolator.EASE_OUT))
        );
        tl.play();
    }

    // ══════════════════════════════════════════════════════════
    //  SCROLL
    // ══════════════════════════════════════════════════════════

    private void scrollToBottom() {
        Platform.runLater(() -> {
            chatScrollPane.layout();
            chatScrollPane.setVvalue(1.0);
        });
    }

    // ══════════════════════════════════════════════════════════
    //  ADMIN LOGIN
    // ══════════════════════════════════════════════════════════

    @FXML public void handleAdminLogin() {
        try {
            Stage stage = (Stage) chatContainer.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/gereja/chatbot/fxml/AdminLogin.fxml"));
            Scene scene = new Scene(loader.load(), 900, 600);
            scene.getStylesheets().add(Objects.requireNonNull(
                    getClass().getResource("/com/gereja/chatbot/css/styles.css")).toExternalForm());
            stage.setTitle("Faith Buddy – Login Admin");
            stage.setScene(scene);
            stage.centerOnScreen();
        } catch (IOException e) {
            System.out.println("[ChatbotController] handleAdminLogin error: " + e.getMessage());
        }
    }

        @FXML
        private void handleMenuAboutUs() {
            addUserMessage("ℹ️ Informasi Tentang Gereja");
            // Contoh: Panggil service untuk deskripsi gereja
            appendBotMessage(ChatMessage.botMessage("Faith Buddy adalah asisten digital Gereja untuk membantu jemaat mendapatkan informasi layanan jemaat secara cepat."));
        }

        @FXML
        private void handleExit() {
            Platform.exit(); // Menutup aplikasi secara rapi
            System.exit(0);
        }

}
