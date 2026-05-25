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
import java.util.Map;
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
    @FXML private StackPane  chatOverlayPane;   // ← StackPane pembungkus chat + kalender overlay

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

    // Style sidebar item hover
    private static final String SIDEBAR_ITEM_HOVER =
            "-fx-background-color: #2D5A3D; -fx-background-radius: 10; -fx-padding: 11 16; -fx-cursor: hand;";
    private static final String SIDEBAR_ITEM_NORMAL =
            "-fx-background-color: transparent; -fx-background-radius: 10; -fx-padding: 11 16; -fx-cursor: hand;";
    private static final String SIDEBAR_SUBITEM_HOVER =
            "-fx-background-color: #2D5A3D; -fx-background-radius: 8; -fx-padding: 8 12; -fx-cursor: hand;";
    private static final String SIDEBAR_SUBITEM_NORMAL =
            "-fx-background-color: transparent; -fx-background-radius: 8; -fx-padding: 8 12; -fx-cursor: hand;";
    private static final String SIDEBAR_EXIT_HOVER =
            "-fx-background-color: #3A1A1A; -fx-background-radius: 10; -fx-padding: 11 16; -fx-cursor: hand;";


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
    //  SIDEBAR TOGGLE & HOVER HANDLERS
    // ══════════════════════════════════════════════════════════

    @FXML
    public void handleToggleSidebar() {
        if (sidebar != null) {
            boolean nowVisible = !sidebar.isVisible();
            if (nowVisible) {
                sidebar.setVisible(true);
                sidebar.setManaged(true);
                // Animasi slide in
                sidebar.setTranslateX(-220);
                Timeline tl = new Timeline(
                        new KeyFrame(Duration.ZERO,
                                new KeyValue(sidebar.translateXProperty(), -220)),
                        new KeyFrame(Duration.millis(200),
                                new KeyValue(sidebar.translateXProperty(), 0, Interpolator.EASE_OUT))
                );
                tl.play();
            } else {
                // Animasi slide out
                Timeline tl = new Timeline(
                        new KeyFrame(Duration.ZERO,
                                new KeyValue(sidebar.translateXProperty(), 0)),
                        new KeyFrame(Duration.millis(180),
                                new KeyValue(sidebar.translateXProperty(), -220, Interpolator.EASE_IN))
                );
                tl.setOnFinished(e -> {
                    sidebar.setVisible(false);
                    sidebar.setManaged(false);
                    sidebar.setTranslateX(0);
                });
                tl.play();
            }
        }
    }

    /** Hover effect untuk item sidebar biasa */
    @FXML public void onSidebarItemHover(javafx.scene.input.MouseEvent e) {
        if (e.getSource() instanceof HBox hbox) hbox.setStyle(SIDEBAR_ITEM_HOVER);
    }

    /** Hover exit untuk item sidebar biasa & keluar */
    @FXML public void onSidebarItemExit(javafx.scene.input.MouseEvent e) {
        if (e.getSource() instanceof HBox hbox) hbox.setStyle(SIDEBAR_ITEM_NORMAL);
    }

    /** Hover effect untuk sub-item di dalam TitledPane Setting */
    @FXML public void onSidebarSubItemHover(javafx.scene.input.MouseEvent e) {
        if (e.getSource() instanceof HBox hbox) hbox.setStyle(SIDEBAR_SUBITEM_HOVER);
    }

    /** Hover exit untuk sub-item */
    @FXML public void onSidebarSubItemExit(javafx.scene.input.MouseEvent e) {
        if (e.getSource() instanceof HBox hbox) hbox.setStyle(SIDEBAR_SUBITEM_NORMAL);
    }

    /** Hover khusus tombol Keluar (warna merah gelap) */
    @FXML public void onSidebarExitHover(javafx.scene.input.MouseEvent e) {
        if (e.getSource() instanceof HBox hbox) hbox.setStyle(SIDEBAR_EXIT_HOVER);
    }

    // ══════════════════════════════════════════════════════════
    //  MENU HANDLERS
    // ══════════════════════════════════════════════════════════

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

    // ── Sidebar menu handlers ─────────────────────────────────

    // ── Kalender ──────────────────────────────────────────────

    /**
     * Menampilkan overlay kalender interaktif di atas area chat.
     * Saat user klik tanggal → kalender tutup → pesan kegiatan muncul di chat.
     */
    @FXML private void handleMenuKalender() {
        // Tutup sidebar terlebih dahulu agar kalender terlihat jelas
        if (sidebar != null && sidebar.isVisible()) {
            sidebar.setVisible(false);
            sidebar.setManaged(false);
            sidebar.setTranslateX(0);
        }

        if (chatOverlayPane == null) {
            // Fallback jika fx:id belum terpasang di FXML lama
            addUserMessage("📅 Kalender Kegiatan Gereja");
            showTypingThenRespond(chatbotService.processInput("jadwal kegiatan"));
            return;
        }

        // Jika kalender sudah terbuka, tutup dulu
        chatOverlayPane.getChildren().removeIf(n -> "calenderOverlay".equals(n.getId()));

        CalenderViewController calVC = new CalenderViewController(
                (date, events) -> onCalenderDateSelected(date, events)
        );

        javafx.scene.layout.StackPane overlayNode = calVC.buildOverlay();
        overlayNode.setId("calenderOverlay");
        chatOverlayPane.getChildren().add(overlayNode);
    }

    /**
     * Callback dari CalenderViewController saat user memilih tanggal.
     * Menampilkan pesan kegiatan di chat.
     */
    private void onCalenderDateSelected(LocalDate date, List<Map<String,String>> events) {
        // Pesan user: "📅 [tanggal yang dipilih]"
        java.time.format.DateTimeFormatter fmtUser =
                java.time.format.DateTimeFormatter.ofPattern("dd MMMM yyyy", new java.util.Locale("id","ID"));
        addUserMessage("📅 " + date.format(fmtUser));

        // Pesan bot: detail kegiatan
        String botText = CalenderViewController.formatEventMessage(date, events);
        List<ChatMessage> resp = List.of(ChatMessage.botMessage(botText));
        showTypingThenRespond(resp);
    }

    @FXML private void handleMenuAboutUs(javafx.scene.input.MouseEvent event) {
        if (event != null) {
            try {
                FXMLLoader loader = new FXMLLoader(
                        getClass().getResource("/com/gereja/chatbot/fxml/TentangKami.fxml")
                );

                Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
                Scene scene = new Scene(loader.load());

                stage.setScene(scene);
                stage.show();
                return;

            } catch (Exception e) {
                System.err.println("[ChatbotController] Gagal membuka Tentang Kami: " + e.getMessage());
                e.printStackTrace();
            }
        }

        addUserMessage("ℹ️ Informasi Tentang Gereja");
        appendBotMessage(ChatMessage.botMessage(
                "🏛️ Tentang Faith Buddy & GKJ Ngupasan\n\n" +
                        "Faith Buddy adalah asisten digital Gereja Kristen Jawa (GKJ) Ngupasan Yogyakarta " +
                        "yang hadir untuk membantu jemaat mendapatkan informasi layanan gereja secara cepat dan mudah.\n\n" +
                        "📍 Alamat: Jl. Ngupasan No. 1, Gondomanan, Yogyakarta\n" +
                        "📞 Telepon: (0274) 512-345\n" +
                        "🌐 Website: www.gkjngupasan.org\n" +
                        "📱 Instagram: @gkjngupasan\n\n" +
                        "Gereja kami berdiri sejak 1857 dan berkomitmen melayani jemaat dengan kasih Kristus."
        ));
    }

    @FXML
    public void handleOpenSettings(javafx.scene.input.MouseEvent event) {
        try {
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/gereja/chatbot/fxml/Settings.fxml")
            );
            Scene scene = new Scene(loader.load());
            stage.setScene(scene);
            stage.show();
        } catch (Exception e) {
            System.err.println("[ChatbotController] Gagal membuka Pengaturan: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    public void handleBackToLanding(javafx.scene.input.MouseEvent event) {
        try {
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/gereja/chatbot/fxml/LandingPage.fxml")
            );
            Scene scene = new Scene(loader.load(), 500, 620);
            scene.getStylesheets().add(Objects.requireNonNull(
                    getClass().getResource("/com/gereja/chatbot/css/styles.css")).toExternalForm());
            stage.setScene(scene);
            stage.show();
        } catch (Exception e) {
            System.err.println("[ChatbotController] Gagal kembali ke Landing Page: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    public void handleAdminLoginFromSidebar(javafx.scene.input.MouseEvent event) {
        try {
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/gereja/chatbot/fxml/AdminLogin.fxml")
            );
            Scene scene = new Scene(loader.load(), 900, 600);
            scene.getStylesheets().add(Objects.requireNonNull(
                    getClass().getResource("/com/gereja/chatbot/css/styles.css")).toExternalForm());
            stage.setScene(scene);
            stage.show();
        } catch (Exception e) {
            System.err.println("[ChatbotController] Gagal membuka Admin Login: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ── Setting sub-menu handlers ─────────────────────────────

    @FXML private void handleSettingBahasa() {
        showTypingThenRespond(chatbotService.processInput("bantuan"));
    }

    // ── Admin dari sidebar ────────────────────────────────────

    @FXML public void handleAdminLoginFromSidebar() {
        handleAdminLogin();
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

        // ── FALLBACK: Chatbot selalu membalas ──────────────────
        // Jika service mengembalikan list kosong atau null,
        // tampilkan pesan fallback agar user tidak jadi last chat.
        if (responses == null || responses.isEmpty()) {
            responses = buildFallbackResponse(text);
        }

        showTypingThenRespond(responses);
    }

    /**
     * Membangun respons fallback ketika tidak ada jawaban terdeteksi.
     * Menyimpan pertanyaan ke DB untuk ditinjau admin, lalu balaskan pesan informatif.
     */
    private List<ChatMessage> buildFallbackResponse(String originalText) {
        // Simpan pertanyaan tak terjawab ke database untuk ditinjau admin
        DatabaseHelper.simpanPertanyaanTakTerjawab(originalText);

        String fallback =
                "🙏 Maaf, saya belum menemukan jawaban yang tepat untuk pertanyaan Anda.\n\n" +
                        "Pertanyaan Anda telah dicatat dan akan kami tinjau untuk pembaruan ke depannya.\n\n" +
                        "Untuk bantuan langsung, Anda dapat:\n" +
                        "• 📞 Hubungi sekretariat: (0274) 512-345\n" +
                        "• 💬 WhatsApp: +62 811-2800-345\n" +
                        "• 📧 Email: sekretariat@gkjngupasan.org\n\n" ;
//                        "Atau coba tanyakan salah satu topik berikut:\n" +
//                        "• Syarat baptis / sidi / pernikahan\n" +
//                        "• Jadwal ibadah\n" +
//                        "• Kontak gereja";

        return List.of(ChatMessage.botMessage(fallback));
    }

    // ══════════════════════════════════════════════════════════
    //  CHIP CLICK
    // ══════════════════════════════════════════════════════════

    private void handleChipClick(String chipText) {
        addUserMessage(chipText);
        List<ChatMessage> responses = chatbotService.processInput(chipText);
        if (responses == null || responses.isEmpty()) {
            responses = buildFallbackResponse(chipText);
        }
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
        return bubbleFactory.buildBotTextBubble(msg);
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
    private void handleExit() {
        Platform.exit();
        System.exit(0);
    }

}