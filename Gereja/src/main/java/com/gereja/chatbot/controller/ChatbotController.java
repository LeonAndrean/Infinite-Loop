package com.gereja.chatbot.controller;

import com.gereja.chatbot.model.ChatMessage;
import com.gereja.chatbot.model.Notification;
import com.gereja.chatbot.service.ChatbotService;
import com.gereja.chatbot.service.NotificationService;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * ╔══════════════════════════════════════════════════════════════╗
 * ║  ChatbotController                                           ║
 * ║  Controller utama untuk ChurchChatbot.fxml                   ║
 * ║  Menangani: chat, navigasi sidebar, notifikasi, quick-reply  ║
 * ╚══════════════════════════════════════════════════════════════╝
 */
public class ChatbotController implements Initializable {

    // ── FXML Injections: Sidebar ──────────────────────────────

    @FXML private HBox menuBeranda;
    @FXML private HBox menuPembaptisan;
    @FXML private HBox menuPernikahan;
    @FXML private HBox menuSidi;
    @FXML private HBox menuKonseling;
    @FXML private HBox menuJadwal;
    @FXML private HBox menuNotifikasi;
    @FXML private HBox menuRiwayat;

    // ── FXML Injections: Chat Area ────────────────────────────

    @FXML private ScrollPane  chatScrollPane;
    @FXML private VBox        chatContainer;
    @FXML private TextArea    messageInput;
    @FXML private TextField   searchField;

    // ── FXML Injections: Welcome Quick-Reply Buttons ──────────

    @FXML private Button btnPembaptisan;
    @FXML private Button btnPernikahan;
    @FXML private Button btnSidi;
    @FXML private Button btnKonseling;
    @FXML private Button btnJadwal;

    // ── Services & Factories ──────────────────────────────────

    private final ChatbotService      chatbotService      = new ChatbotService();
    private final NotificationService notificationService = new NotificationService();
    private       ChatBubbleFactory   bubbleFactory;

    // ── State ─────────────────────────────────────────────────

    private HBox   activeMenuItem;
    private Node   typingNode;

    // ── Style constants ───────────────────────────────────────

    private static final String MENU_ACTIVE =
        "-fx-background-color: #D4A843; -fx-background-radius: 10; -fx-padding: 11 16; -fx-cursor: hand;";
    private static final String MENU_INACTIVE =
        "-fx-background-color: transparent; -fx-background-radius: 10; -fx-padding: 11 16; -fx-cursor: hand;";
    // Label style inside active item (dark text on gold)
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
        setupSearchField();
        setupScrollBehavior();

        // Mark Beranda as active at start
        setActiveMenu(menuBeranda);

        // Auto-scroll to bottom whenever chat content changes
        chatContainer.heightProperty().addListener((obs, oldH, newH) -> scrollToBottom());
    }

    // ══════════════════════════════════════════════════════════
    //  SETUP METHODS
    // ══════════════════════════════════════════════════════════

    private void setupMenuNavigation() {
        menuBeranda.setOnMouseClicked(e    -> handleMenuBeranda());
        menuPembaptisan.setOnMouseClicked(e -> handleMenuPembaptisan());
        menuPernikahan.setOnMouseClicked(e  -> handleMenuPernikahan());
        menuSidi.setOnMouseClicked(e       -> handleMenuSidi());
        menuKonseling.setOnMouseClicked(e  -> handleMenuKonseling());
        menuJadwal.setOnMouseClicked(e     -> handleMenuJadwal());
        menuNotifikasi.setOnMouseClicked(e -> handleMenuNotifikasi());
        menuRiwayat.setOnMouseClicked(e    -> handleMenuRiwayat());

        // Welcome screen quick-reply buttons
        if (btnPembaptisan != null) btnPembaptisan.setOnAction(e -> triggerBotInfo("pembaptisan"));
        if (btnPernikahan  != null) btnPernikahan.setOnAction(e  -> triggerBotInfo("pernikahan"));
        if (btnSidi        != null) btnSidi.setOnAction(e        -> triggerBotInfo("sidi"));
        if (btnKonseling   != null) btnKonseling.setOnAction(e   -> triggerBotInfo("konseling"));
        if (btnJadwal      != null) btnJadwal.setOnAction(e      -> triggerBotInfo("jadwal"));
    }

    private void setupInputHandling() {
        // Enter to send, Shift+Enter for newline
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
                default -> { /* nothing */ }
            }
        });
    }

    private void setupSearchField() {
        searchField.textProperty().addListener((obs, oldText, newText) -> {
            if (newText != null && !newText.isBlank() && newText.length() > 2) {
                handleSearch(newText);
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
    private void handleMenuBeranda() {
        setActiveMenu(menuBeranda);
        chatbotService.resetState();
        appendBotMessage(chatbotService.getWelcomeMessage());
    }

    @FXML
    private void handleMenuPembaptisan() {
        setActiveMenu(menuPembaptisan);
        addUserMessage("💧 Informasi Pembaptisan");
        showTypingThenRespond(List.of(chatbotService.getInfoPembaptisan()));
    }

    @FXML
    private void handleMenuPernikahan() {
        setActiveMenu(menuPernikahan);
        addUserMessage("💍 Informasi Pernikahan Gerejawi");
        showTypingThenRespond(List.of(chatbotService.getInfoPernikahan()));
    }

    @FXML
    private void handleMenuSidi() {
        setActiveMenu(menuSidi);
        addUserMessage("📖 Informasi SIDI / Peneguhan");
        showTypingThenRespond(List.of(chatbotService.getInfoSidi()));
    }

    @FXML
    private void handleMenuKonseling() {
        setActiveMenu(menuKonseling);
        addUserMessage("🤝 Informasi Konseling Pendeta");
        showTypingThenRespond(List.of(chatbotService.getInfoKonseling()));
    }

    @FXML
    private void handleMenuJadwal() {
        setActiveMenu(menuJadwal);
        addUserMessage("📅 Jadwal & Pendaftaran");
        showTypingThenRespond(List.of(chatbotService.getInfoJadwal()));
    }

    @FXML
    private void handleMenuNotifikasi() {
        setActiveMenu(menuNotifikasi);
        // Show notification summary in chat
        long unread = notificationService.getUnreadCount();
        notificationService.markAllRead();
        ChatMessage info = ChatMessage.botMessage(
            "🔔 Notifikasi Anda (" + unread + " belum dibaca):\n\n" +
            buildNotificationSummary()
        );
        appendBotMessage(info);
    }

    @FXML
    private void handleMenuRiwayat() {
        setActiveMenu(menuRiwayat);
        ChatMessage info = ChatMessage.botMessage(
            "🕐 Riwayat Konsultasi Anda:\n\n" +
            "Sesi terakhir tersimpan di sini. " +
            "Fitur riwayat lengkap akan segera tersedia.\n\n" +
            "Untuk saat ini Anda dapat scroll ke atas untuk melihat " +
            "percakapan dalam sesi ini."
        );
        appendBotMessage(info);
    }

    // ══════════════════════════════════════════════════════════
    //  SEND MESSAGE (triggered by button or Enter key)
    // ══════════════════════════════════════════════════════════

    @FXML
    public void sendMessage() {
        String text = messageInput.getText().trim();
        if (text.isBlank()) return;

        messageInput.clear();
        messageInput.setPrefRowCount(1);

        addUserMessage(text);

        // Process & respond
        List<ChatMessage> responses = chatbotService.processInput(text);
        showTypingThenRespond(responses);
    }

    // ══════════════════════════════════════════════════════════
    //  QUICK ACTION BUTTONS (bottom bar)
    // ══════════════════════════════════════════════════════════

    @FXML
    public void handleQuickPersyaratan() {
        addUserMessage("📋 Persyaratan Layanan");
        showTypingThenRespond(chatbotService.processInput("syarat persyaratan dokumen"));
    }

    @FXML
    public void handleQuickJadwal() {
        addUserMessage("📅 Lihat Jadwal");
        showTypingThenRespond(chatbotService.processInput("jadwal"));
    }

    @FXML
    public void handleQuickKontak() {
        addUserMessage("📞 Informasi Kontak");
        showTypingThenRespond(chatbotService.processInput("kontak"));
    }

    @FXML
    public void handleQuickBantuan() {
        addUserMessage("❓ Bantuan");
        showTypingThenRespond(chatbotService.processInput("halo"));
    }

    // ══════════════════════════════════════════════════════════
    //  CHIP CLICK (quick reply chips)
    // ══════════════════════════════════════════════════════════

    private void handleChipClick(String chipText) {
        addUserMessage(chipText);
        List<ChatMessage> responses = chatbotService.processInput(chipText);
        showTypingThenRespond(responses);
    }

    // ══════════════════════════════════════════════════════════
    //  INTERNAL TRIGGER (from sidebar/welcome buttons)
    // ══════════════════════════════════════════════════════════

    private void triggerBotInfo(String topic) {
        List<ChatMessage> responses = chatbotService.processInput(topic);
        showTypingThenRespond(responses);
    }

    // ══════════════════════════════════════════════════════════
    //  SEARCH
    // ══════════════════════════════════════════════════════════

    private void handleSearch(String query) {
        // Tunda pencarian 500ms untuk debounce
        PauseTransition pause = new PauseTransition(Duration.millis(500));
        pause.setOnFinished(e -> {
            List<ChatMessage> responses = chatbotService.processInput(query);
            showTypingThenRespond(responses);
        });
        pause.play();
    }

    // ══════════════════════════════════════════════════════════
    //  CHAT UI BUILDING BLOCKS
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

    /** Tampilkan typing indicator, lalu setelah delay tampilkan respons. */
    private void showTypingThenRespond(List<ChatMessage> responses) {
        if (responses == null || responses.isEmpty()) return;

        typingNode = bubbleFactory.buildTypingIndicator();
        animateIn(typingNode);
        chatContainer.getChildren().add(typingNode);
        scrollToBottom();

        // Hitung delay berdasarkan panjang konten (simulasi "mengetik")
        int totalLength = responses.stream()
            .mapToInt(m -> m.getContent().length())
            .sum();
        long delayMs = Math.min(600 + totalLength * 5L, 2500);

        PauseTransition delay = new PauseTransition(Duration.millis(delayMs));
        delay.setOnFinished(e -> Platform.runLater(() -> {
            // Hapus typing indicator
            chatContainer.getChildren().remove(typingNode);
            typingNode = null;

            // Tambahkan semua respons
            for (ChatMessage msg : responses) {
                appendBotMessage(msg);
            }
        }));
        delay.play();
    }

    private Node buildBotNode(ChatMessage msg) {
        return switch (msg.getType()) {
            case STEP_INFO  -> bubbleFactory.buildBotStepBubble(msg);
            default         -> bubbleFactory.buildBotTextBubble(msg);
        };
    }

    // ══════════════════════════════════════════════════════════
    //  NOTIFICATION SUMMARY
    // ══════════════════════════════════════════════════════════

    private String buildNotificationSummary() {
        StringBuilder sb = new StringBuilder();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("EEEE, dd MMM", new Locale("id", "ID"));

        for (Notification n : notificationService.getNotifications()) {
            if (n.getCategory() == Notification.Category.JADWAL) {
                sb.append(n.getEmoji()).append(" ")
                  .append(n.getTitle()).append("\n")
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
    //  ACTIVE MENU MANAGEMENT
    // ══════════════════════════════════════════════════════════

    private void setActiveMenu(HBox newActive) {
        // Deactivate old
        if (activeMenuItem != null && activeMenuItem != newActive) {
            activeMenuItem.setStyle(MENU_INACTIVE);
            // Update label color in previous menu
            setMenuLabelStyle(activeMenuItem, MENU_LABEL_INACTIVE);
        }

        // Activate new
        newActive.setStyle(MENU_ACTIVE);
        setMenuLabelStyle(newActive, MENU_LABEL_ACTIVE);
        activeMenuItem = newActive;
    }

    /** Update label text color inside an HBox menu item. */
    private void setMenuLabelStyle(HBox menu, String style) {
        menu.getChildren().stream()
            .filter(n -> n instanceof Label)
            .map(n -> (Label) n)
            .filter(l -> !l.getText().isEmpty() && l.getText().length() > 1)
            .forEach(l -> l.setStyle(style));
    }

    // ══════════════════════════════════════════════════════════
    //  ANIMATIONS
    // ══════════════════════════════════════════════════════════

    private void animateIn(Node node) {
        node.setOpacity(0);
        node.setTranslateY(10);
        Timeline tl = new Timeline(
            new KeyFrame(Duration.ZERO,
                new KeyValue(node.opacityProperty(), 0),
                new KeyValue(node.translateYProperty(), 10)
            ),
            new KeyFrame(Duration.millis(280),
                new KeyValue(node.opacityProperty(), 1, Interpolator.EASE_OUT),
                new KeyValue(node.translateYProperty(), 0, Interpolator.EASE_OUT)
            )
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
}
