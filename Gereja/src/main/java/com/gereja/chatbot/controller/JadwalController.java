package com.gereja.chatbot.controller;

import com.gereja.chatbot.model.Notification;
import com.gereja.chatbot.service.NotificationService;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;

import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * Controller untuk JadwalDialog.fxml
 * Menangani tampilan & interaksi jadwal mendatang,
 * daftar layanan, dan riwayat.
 */
public class JadwalController implements Initializable {

    @FXML private Button btnClose;
    @FXML private Button tabJadwal;
    @FXML private Button tabDaftar;
    @FXML private Button tabRiwayat;
    @FXML private ScrollPane contentScrollPane;

    private final NotificationService notificationService = new NotificationService();

    private static final String TAB_ACTIVE =
        "-fx-background-color: transparent; -fx-text-fill: #1A3A2A; -fx-font-size: 12px; "
        + "-fx-font-weight: bold; -fx-padding: 13 20; "
        + "-fx-border-color: transparent transparent #1A3A2A transparent; "
        + "-fx-border-width: 0 0 3 0; -fx-cursor: hand;";
    private static final String TAB_INACTIVE =
        "-fx-background-color: transparent; -fx-text-fill: #7A8A7E; -fx-font-size: 12px; "
        + "-fx-padding: 13 20; -fx-border-width: 0; -fx-cursor: hand;";

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Close button
        if (btnClose != null) {
            btnClose.setOnAction(e -> {
                Stage stage = (Stage) btnClose.getScene().getWindow();
                stage.close();
            });
        }

        // Tab switching
        if (tabJadwal  != null) tabJadwal.setOnAction(e  -> showTabJadwal());
        if (tabDaftar  != null) tabDaftar.setOnAction(e  -> showTabDaftar());
        if (tabRiwayat != null) tabRiwayat.setOnAction(e -> showTabRiwayat());

        // Show default tab
        showTabJadwal();
    }

    // ══════════════════════════════════════════════════════════
    //  TAB HANDLERS
    // ══════════════════════════════════════════════════════════

    private void showTabJadwal() {
        setActiveTab(tabJadwal);
        if (contentScrollPane != null) {
            contentScrollPane.setContent(buildJadwalContent());
        }
    }

    private void showTabDaftar() {
        setActiveTab(tabDaftar);
        if (contentScrollPane != null) {
            contentScrollPane.setContent(buildDaftarContent());
        }
    }

    private void showTabRiwayat() {
        setActiveTab(tabRiwayat);
        if (contentScrollPane != null) {
            contentScrollPane.setContent(buildRiwayatContent());
        }
    }

    private void setActiveTab(Button active) {
        for (Button tab : new Button[]{tabJadwal, tabDaftar, tabRiwayat}) {
            if (tab != null) tab.setStyle(TAB_INACTIVE);
        }
        if (active != null) active.setStyle(TAB_ACTIVE);
    }

    // ══════════════════════════════════════════════════════════
    //  CONTENT BUILDERS
    // ══════════════════════════════════════════════════════════

    private VBox buildJadwalContent() {
        VBox root = new VBox(12);
        root.setPadding(new Insets(20, 24, 20, 24));

        // Month header
        HBox monthRow = new HBox(10);
        monthRow.setAlignment(Pos.CENTER_LEFT);
        Label monthLabel = styledLabel("Jadwal Mendatang",
            "-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #1A3A2A; -fx-font-family: 'Georgia';");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        monthRow.getChildren().addAll(monthLabel, spacer);
        root.getChildren().add(monthRow);

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("EEEE, dd MMM yyyy", new Locale("id", "ID"));

        // Build event cards from notification service
        for (Notification n : notificationService.getNotifications()) {
            if (n.getCategory() == Notification.Category.JADWAL) {
                root.getChildren().add(buildEventCard(n, fmt));
            }
        }

        // Section: Deadline
        root.getChildren().add(styledLabel("BATAS PENDAFTARAN",
            "-fx-text-fill: #9A8A7A; -fx-font-size: 10px; -fx-font-weight: bold; "
            + "-fx-font-family: 'Verdana'; -fx-padding: 8 0 4 0;"));

        for (Notification n : notificationService.getNotifications()) {
            if (n.getCategory() == Notification.Category.DEADLINE) {
                root.getChildren().add(buildDeadlineCard(n));
            }
        }

        return root;
    }

    private HBox buildEventCard(Notification n, DateTimeFormatter fmt) {
        HBox card = new HBox(0);
        card.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 14; "
                    + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.07), 8, 0, 0, 3); -fx-cursor: hand;");

        // Date block
        VBox dateBlock = new VBox();
        dateBlock.setAlignment(Pos.CENTER);
        dateBlock.setMinWidth(70);
        dateBlock.setStyle("-fx-background-color: " + n.getAccentColor()
                         + "; -fx-background-radius: 14 0 0 14; -fx-padding: 14 10;");

        boolean isDark = "#D4A843".equals(n.getAccentColor());
        String numColor  = isDark ? "#1A3A2A" : "white";
        String textColor = isDark ? "#5A4010" : "#CCDDCC";

        Label dayNum  = styledLabel(String.valueOf(n.getDate().getDayOfMonth()),
            "-fx-text-fill: " + numColor + "; -fx-font-size: 26px; -fx-font-weight: bold; -fx-font-family: 'Georgia';");
        Label month   = styledLabel(n.getDate().format(DateTimeFormatter.ofPattern("MMM")).toUpperCase(),
            "-fx-text-fill: " + numColor + "; -fx-font-size: 11px; -fx-font-weight: bold;");
        Label dayName = styledLabel(n.getDate().format(DateTimeFormatter.ofPattern("EEE", new Locale("id","ID"))).toUpperCase(),
            "-fx-text-fill: " + textColor + "; -fx-font-size: 10px;");

        dateBlock.getChildren().addAll(dayNum, month, dayName);

        // Info section
        VBox info = new VBox(6);
        HBox.setHgrow(info, Priority.ALWAYS);
        info.setPadding(new Insets(14, 18, 14, 18));

        HBox titleRow = new HBox(8);
        titleRow.setAlignment(Pos.CENTER_LEFT);
        Label title = styledLabel(n.getTitle(),
            "-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #1A3A2A;");
        HBox.setHgrow(title, Priority.ALWAYS);

        // Status badge
        long days = n.daysUntil();
        String badgeText  = days <= 3 ? "Segera" : days <= 7 ? "Minggu ini" : "Terdaftar";
        String badgeColor = days <= 3 ? "#FFF0F0" : "#EEF5EE";
        String badgeTxt   = days <= 3 ? "#C03030" : "#2D6A3D";
        Label badge = styledLabel(badgeText,
            "-fx-background-color: " + badgeColor + "; -fx-text-fill: " + badgeTxt
            + "; -fx-font-size: 10px; -fx-font-weight: bold; -fx-background-radius: 8; -fx-padding: 3 8;");
        titleRow.getChildren().addAll(title, badge);

        HBox meta = new HBox(14);
        meta.getChildren().addAll(
            styledLabel("🕐 " + n.getSubtitle(), "-fx-font-size: 11px; -fx-text-fill: #6A7A6E;"),
            styledLabel("📍 " + n.getLocation(), "-fx-font-size: 11px; -fx-text-fill: #6A7A6E;")
        );

        Label daysLabel = styledLabel(days + " hari lagi",
            "-fx-font-size: 11px; -fx-text-fill: #8A9A8E;");

        info.getChildren().addAll(titleRow, meta, daysLabel);

        // Action button
        VBox actionBox = new VBox();
        actionBox.setAlignment(Pos.CENTER);
        actionBox.setPadding(new Insets(14, 16, 14, 16));
        Button btn = new Button("Detail");
        btn.setStyle("-fx-background-color: #1A3A2A; -fx-text-fill: #D4A843; "
                   + "-fx-font-size: 11px; -fx-background-radius: 10; "
                   + "-fx-padding: 7 14; -fx-cursor: hand; -fx-font-weight: bold;");
        btn.setOnAction(e -> showEventDetail(n));
        actionBox.getChildren().add(btn);

        card.getChildren().addAll(dateBlock, info, actionBox);
        return card;
    }

    private VBox buildDeadlineCard(Notification n) {
        VBox card = new VBox(8);
        card.setStyle("-fx-background-color: #FFF8EC; -fx-background-radius: 10; "
                    + "-fx-border-color: #FFE5A0; -fx-border-width: 1; -fx-border-radius: 10; -fx-padding: 12 16;");
        card.setPadding(new Insets(12, 16, 12, 16));
        VBox.setMargin(card, new Insets(0, 0, 8, 0));

        HBox titleRow = new HBox(8);
        titleRow.setAlignment(Pos.CENTER_LEFT);
        Label icon  = styledLabel(n.getEmoji(), "-fx-font-size: 14px;");
        VBox textCol = new VBox(2);
        boolean urgent = n.daysUntil() <= 7;
        String titleColor = urgent ? "#C03030" : "#9A7010";
        Label title   = styledLabel(n.getTitle(),
            "-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: " + titleColor + ";");
        Label subDate = styledLabel("Tutup: " + n.getDate(),
            "-fx-font-size: 10px; -fx-text-fill: " + (urgent ? "#A04040" : "#7A6010") + ";");
        textCol.getChildren().addAll(title, subDate);
        titleRow.getChildren().addAll(icon, textCol);

        ProgressBar bar = new ProgressBar(n.deadlineProgress(30));
        bar.setMaxWidth(Double.MAX_VALUE);
        bar.setStyle("-fx-accent: " + (urgent ? "#E05252" : "#D4A843") + "; "
                   + "-fx-pref-height: 6; -fx-background-radius: 4;");

        Label days = styledLabel(n.daysUntil() + " hari lagi",
            "-fx-font-size: 10px; -fx-text-fill: " + titleColor + ";");

        card.getChildren().addAll(titleRow, bar, days);
        return card;
    }

    private VBox buildDaftarContent() {
        VBox root = new VBox(16);
        root.setPadding(new Insets(20, 24, 20, 24));

        root.getChildren().add(styledLabel("Daftar Layanan Gerejawi",
            "-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #1A3A2A; -fx-font-family: 'Georgia';"));

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);

        String[][] services = {
            {"💧", "Pembaptisan",   "Maret 2024",  "#1A3A2A", "white"},
            {"💍", "Pernikahan",    "April 2024",  "#D4A843",  "#1A3A2A"},
            {"📖", "SIDI",          "April 2024",  "#2D5A3D",  "white"},
            {"🤝", "Konseling",     "Tersedia",    "#6A4A8A",  "white"},
            {"📋", "Kelas Pra-Nikah","Setiap Sabtu","#4A7A5A",  "white"},
            {"🏛",  "Ibadah Khusus", "Lihat Jadwal","#1A3A2A",  "white"}
        };

        for (int i = 0; i < services.length; i++) {
            String[] s = services[i];
            VBox card = buildServiceCard(s[0], s[1], s[2], s[3], s[4]);
            GridPane.setColumnIndex(card, i % 3);
            GridPane.setRowIndex(card, i / 3);
            GridPane.setHgrow(card, Priority.ALWAYS);
            grid.getChildren().add(card);
        }

        root.getChildren().add(grid);
        return root;
    }

    private VBox buildServiceCard(String emoji, String name, String date,
                                   String bgColor, String textColor) {
        VBox card = new VBox(10);
        card.setAlignment(Pos.CENTER);
        card.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 14; "
                    + "-fx-padding: 18; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.07), 8, 0, 0, 3); "
                    + "-fx-cursor: hand;");

        Label emojiLabel = styledLabel(emoji, "-fx-font-size: 28px;");
        Label nameLabel  = styledLabel(name,  "-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #1A3A2A;");
        Label dateLabel  = styledLabel("Jadwal: " + date, "-fx-font-size: 10px; -fx-text-fill: #7A8A7E;");

        Button btn = new Button("Daftar →");
        btn.setStyle("-fx-background-color: " + bgColor + "; -fx-text-fill: " + textColor + "; "
                   + "-fx-font-size: 11px; -fx-background-radius: 10; "
                   + "-fx-padding: 7 16; -fx-cursor: hand; -fx-font-weight: bold;");
        btn.setOnAction(e -> showDaftarForm(name));

        card.getChildren().addAll(emojiLabel, nameLabel, dateLabel, btn);
        return card;
    }

    private VBox buildRiwayatContent() {
        VBox root = new VBox(16);
        root.setPadding(new Insets(20, 24, 20, 24));
        root.setAlignment(Pos.CENTER);

        Label icon = styledLabel("🕐", "-fx-font-size: 48px;");
        Label title = styledLabel("Riwayat Konsultasi",
            "-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #1A3A2A;");
        Label desc = styledLabel(
            "Riwayat percakapan dan konsultasi Anda dengan GerejaCare\nakan ditampilkan di sini.",
            "-fx-font-size: 12px; -fx-text-fill: #7A8A7E; -fx-text-alignment: center;");
        desc.setWrapText(true);

        root.getChildren().addAll(icon, title, desc);
        return root;
    }

    // ══════════════════════════════════════════════════════════
    //  DIALOGS / DETAIL
    // ══════════════════════════════════════════════════════════

    private void showEventDetail(Notification n) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Detail Jadwal");
        alert.setHeaderText(n.getEmoji() + "  " + n.getTitle());
        alert.setContentText(
            "📅 Tanggal : " + n.getDate() + "\n" +
            "🕐 Waktu   : " + n.getSubtitle() + "\n" +
            "📍 Lokasi  : " + n.getLocation() + "\n\n" +
            "Untuk informasi lebih lanjut, hubungi sekretariat gereja\n" +
            "di nomor (021) 123-4567."
        );
        alert.showAndWait();
    }

    private void showDaftarForm(String layanan) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Pendaftaran " + layanan);
        alert.setHeaderText("Formulir Pendaftaran – " + layanan);
        alert.setContentText(
            "Untuk mendaftar " + layanan + ", silakan:\n\n" +
            "1. Hubungi sekretariat gereja\n" +
            "   Telepon: (021) 123-4567\n\n" +
            "2. Datang langsung ke sekretariat\n" +
            "   Senin–Jumat: 08.00–16.00 WIB\n\n" +
            "3. Siapkan dokumen persyaratan yang diperlukan\n\n" +
            "Kami akan menghubungi Anda untuk konfirmasi jadwal."
        );
        alert.showAndWait();
    }

    // ── Utility ───────────────────────────────────────────────

    private Label styledLabel(String text, String style) {
        Label l = new Label(text);
        l.setStyle(style);
        return l;
    }
}
