package com.gereja.chatbot.controller;

import com.gereja.chatbot.database.DatabaseHelper;
import com.gereja.chatbot.model.Notification;
import com.gereja.chatbot.service.NotificationService;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * JadwalController - tampilan jadwal, daftar layanan, dan riwayat.
 * Tampilan disederhanakan, riwayat chat diambil dari database.
 */
public class JadwalController implements Initializable {

    @FXML private Button btnClose;
    @FXML private Button tabJadwal;
    @FXML private Button tabDaftar;
    @FXML private Button tabRiwayat;
    @FXML private ScrollPane contentScrollPane;

    private final NotificationService notifService = new NotificationService();

    private static final String TAB_AKTIF =
            "-fx-background-color: transparent; -fx-text-fill: #1A3A2A; -fx-font-size: 12px; " +
                    "-fx-font-weight: bold; -fx-padding: 13 20; " +
                    "-fx-border-color: transparent transparent #1A3A2A transparent; " +
                    "-fx-border-width: 0 0 2 0; -fx-cursor: hand;";
    private static final String TAB_NONAKTIF =
            "-fx-background-color: transparent; -fx-text-fill: #7A8A7E; -fx-font-size: 12px; " +
                    "-fx-padding: 13 20; -fx-border-width: 0; -fx-cursor: hand;";

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if (btnClose  != null) btnClose.setOnAction(e ->
                ((Stage) btnClose.getScene().getWindow()).close());

        if (tabJadwal  != null) tabJadwal.setOnAction(e  -> tampilTabJadwal());
        if (tabDaftar  != null) tabDaftar.setOnAction(e  -> tampilTabDaftar());
        if (tabRiwayat != null) tabRiwayat.setOnAction(e -> tampilTabRiwayat());

        tampilTabJadwal(); // default
    }

    // ── Tab handler ───────────────────────────────────────────

    private void tampilTabJadwal() {
        setTabAktif(tabJadwal);
        if (contentScrollPane != null)
            contentScrollPane.setContent(buatKontenJadwal());
    }

    private void tampilTabDaftar() {
        setTabAktif(tabDaftar);
        if (contentScrollPane != null)
            contentScrollPane.setContent(buatKontenDaftar());
    }

    private void tampilTabRiwayat() {
        setTabAktif(tabRiwayat);
        if (contentScrollPane != null)
            contentScrollPane.setContent(buatKontenRiwayat());
    }

    private void setTabAktif(Button aktif) {
        for (Button tab : new Button[]{tabJadwal, tabDaftar, tabRiwayat}) {
            if (tab != null) tab.setStyle(TAB_NONAKTIF);
        }
        if (aktif != null) aktif.setStyle(TAB_AKTIF);
    }

    // ── Konten Tab Jadwal ────────────────────────────────────

    private VBox buatKontenJadwal() {
        VBox root = new VBox(10);
        root.setPadding(new Insets(16, 20, 16, 20));

        root.getChildren().add(label("Jadwal Mendatang",
                "-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #1A3A2A;"));

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("EEEE, dd MMM yyyy", new Locale("id","ID"));

        for (Notification n : notifService.getNotifications()) {
            if (n.getCategory() == Notification.Category.JADWAL) {
                root.getChildren().add(buatKartuJadwal(n, fmt));
            }
        }

        root.getChildren().add(label("BATAS PENDAFTARAN",
                "-fx-text-fill: #9A8A7A; -fx-font-size: 10px; -fx-font-weight: bold; -fx-padding: 8 0 4 0;"));

        for (Notification n : notifService.getNotifications()) {
            if (n.getCategory() == Notification.Category.DEADLINE) {
                root.getChildren().add(buatKartuDeadline(n));
            }
        }

        return root;
    }

    private HBox buatKartuJadwal(Notification n, DateTimeFormatter fmt) {
        HBox kartu = new HBox(0);
        kartu.setStyle("-fx-background-color: white; -fx-background-radius: 10; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.06), 6, 0, 0, 2);");

        // Blok tanggal
        VBox tgl = new VBox();
        tgl.setAlignment(Pos.CENTER);
        tgl.setMinWidth(64);
        tgl.setStyle("-fx-background-color: " + n.getAccentColor() +
                "; -fx-background-radius: 10 0 0 10; -fx-padding: 12 8;");

        boolean gelap  = "#D4A843".equals(n.getAccentColor());
        String warnaTgl  = gelap ? "#1A3A2A" : "white";
        String warnaBulan= gelap ? "#5A4010" : "#CCDDCC";

        tgl.getChildren().addAll(
                label(String.valueOf(n.getDate().getDayOfMonth()),
                        "-fx-text-fill: "+warnaTgl+"; -fx-font-size: 22px; -fx-font-weight: bold;"),
                label(n.getDate().format(DateTimeFormatter.ofPattern("MMM")).toUpperCase(),
                        "-fx-text-fill: "+warnaTgl+"; -fx-font-size: 10px; -fx-font-weight: bold;"),
                label(n.getDate().format(DateTimeFormatter.ofPattern("EEE", new Locale("id","ID"))).toUpperCase(),
                        "-fx-text-fill: "+warnaBulan+"; -fx-font-size: 9px;")
        );

        // Info
        VBox info = new VBox(4);
        HBox.setHgrow(info, Priority.ALWAYS);
        info.setPadding(new Insets(12, 14, 12, 14));

        long hari = n.daysUntil();
        String badgeTeks  = hari <= 3 ? "Segera" : hari <= 7 ? "Minggu ini" : "Terdaftar";
        String badgeBg    = hari <= 3 ? "#FFF0F0" : "#EEF5EE";
        String badgeWarna = hari <= 3 ? "#C03030" : "#2D6A3D";

        HBox judul = new HBox(8);
        judul.setAlignment(Pos.CENTER_LEFT);
        Label jdl = label(n.getTitle(),
                "-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #1A3A2A;");
        HBox.setHgrow(jdl, Priority.ALWAYS);
        Label badge = label(badgeTeks,
                "-fx-background-color: "+badgeBg+"; -fx-text-fill: "+badgeWarna+
                        "; -fx-font-size: 9px; -fx-font-weight: bold; -fx-background-radius: 6; -fx-padding: 2 7;");
        judul.getChildren().addAll(jdl, badge);

        info.getChildren().addAll(judul,
                label("🕐 " + n.getSubtitle() + "  📍 " + n.getLocation(),
                        "-fx-font-size: 10px; -fx-text-fill: #6A7A6E;"),
                label(hari + " hari lagi",
                        "-fx-font-size: 10px; -fx-text-fill: #8A9A8E;")
        );

        // Tombol detail
        VBox tombolBox = new VBox();
        tombolBox.setAlignment(Pos.CENTER);
        tombolBox.setPadding(new Insets(0, 12, 0, 12));
        Button btn = new Button("Detail");
        btn.setStyle("-fx-background-color: #1A3A2A; -fx-text-fill: #D4A843; " +
                "-fx-font-size: 10px; -fx-background-radius: 8; -fx-padding: 6 12; -fx-cursor: hand;");
        btn.setOnAction(e -> tampilDetail(n));
        tombolBox.getChildren().add(btn);

        kartu.getChildren().addAll(tgl, info, tombolBox);
        return kartu;
    }

    private VBox buatKartuDeadline(Notification n) {
        VBox kartu = new VBox(6);
        kartu.setStyle("-fx-background-color: #FFF8EC; -fx-background-radius: 8; " +
                "-fx-border-color: #FFE5A0; -fx-border-width: 1; -fx-border-radius: 8; -fx-padding: 10 14;");
        VBox.setMargin(kartu, new Insets(0, 0, 6, 0));

        boolean urgent = n.daysUntil() <= 7;
        String warna   = urgent ? "#C03030" : "#9A7010";

        HBox baris = new HBox(8);
        baris.setAlignment(Pos.CENTER_LEFT);
        VBox teks = new VBox(2);
        teks.getChildren().addAll(
                label(n.getTitle(), "-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: "+warna+";"),
                label("Tutup: "+n.getDate(), "-fx-font-size: 10px; -fx-text-fill: "+(urgent?"#A04040":"#7A6010")+";")
        );
        baris.getChildren().addAll(label(n.getEmoji(), "-fx-font-size: 13px;"), teks);

        ProgressBar bar = new ProgressBar(n.deadlineProgress(30));
        bar.setMaxWidth(Double.MAX_VALUE);
        bar.setStyle("-fx-accent: "+(urgent?"#E05252":"#D4A843")+"; -fx-pref-height: 5;");

        kartu.getChildren().addAll(baris, bar,
                label(n.daysUntil()+" hari lagi", "-fx-font-size: 9px; -fx-text-fill: "+warna+";"));
        return kartu;
    }

    // ── Konten Tab Daftar ─────────────────────────────────────

    private VBox buatKontenDaftar() {
        VBox root = new VBox(12);
        root.setPadding(new Insets(16, 20, 16, 20));
        root.getChildren().add(label("Daftar Layanan Gerejawi",
                "-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #1A3A2A;"));

        String[][] layanan = {
                {"💧","Pembaptisan",   "Minggu pertama bulan depan","#1A3A2A","white"},
                {"💍","Pernikahan",    "Konsultasi: Senin & Rabu",  "#D4A843","#1A3A2A"},
                {"📖","SIDI",          "Kelas: setiap Minggu",      "#2D5A3D","white"},
                {"🤝","Konseling",     "Selasa & Kamis / Rabu & Jumat","#6A4A8A","white"},
                {"📋","Kelas Pra-Nikah","Setiap Sabtu",             "#4A7A5A","white"},
                {"⛪","Ibadah Khusus", "Lihat Jadwal Ibadah",       "#1A3A2A","white"}
        };

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        for (int i = 0; i < layanan.length; i++) {
            String[] s = layanan[i];
            VBox kartu = buatKartuLayanan(s[0], s[1], s[2], s[3], s[4]);
            GridPane.setColumnIndex(kartu, i % 3);
            GridPane.setRowIndex(kartu, i / 3);
            GridPane.setHgrow(kartu, Priority.ALWAYS);
            grid.getChildren().add(kartu);
        }
        root.getChildren().add(grid);
        return root;
    }

    private VBox buatKartuLayanan(String emoji, String nama, String jadwal,
                                  String warnaBg, String warnaText) {
        VBox kartu = new VBox(8);
        kartu.setAlignment(Pos.CENTER);
        kartu.setStyle("-fx-background-color: white; -fx-background-radius: 10; " +
                "-fx-padding: 16; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.06), 6, 0, 0, 2); " +
                "-fx-cursor: hand;");

        Button btn = new Button("Daftar");
        btn.setStyle("-fx-background-color: "+warnaBg+"; -fx-text-fill: "+warnaText+"; " +
                "-fx-font-size: 10px; -fx-background-radius: 8; -fx-padding: 6 14; -fx-cursor: hand;");
        btn.setOnAction(e -> tampilFormDaftar(nama));

        kartu.getChildren().addAll(
                label(emoji, "-fx-font-size: 26px;"),
                label(nama, "-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #1A3A2A;"),
                label(jadwal, "-fx-font-size: 10px; -fx-text-fill: #7A8A7E;"),
                btn
        );
        return kartu;
    }

    // ── Konten Tab Riwayat (dari database) ───────────────────

    private VBox buatKontenRiwayat() {
        VBox root = new VBox(10);
        root.setPadding(new Insets(16, 20, 16, 20));

        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        Label judul = label("Riwayat Percakapan",
                "-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #1A3A2A;");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Button btnHapus = new Button("Hapus Semua");
        btnHapus.setStyle("-fx-background-color: #E05252; -fx-text-fill: white; " +
                "-fx-font-size: 10px; -fx-background-radius: 6; -fx-padding: 5 10;");
        btnHapus.setOnAction(e -> {
            DatabaseHelper.hapusRiwayat();
            root.getChildren().clear();
            root.getChildren().add(header);
            root.getChildren().add(label("Riwayat sudah dihapus.",
                    "-fx-font-size: 12px; -fx-text-fill: #7A8A7E;"));
        });
        header.getChildren().addAll(judul, spacer, btnHapus);
        root.getChildren().add(header);

        List<String[]> riwayat = DatabaseHelper.getRiwayatChat(50);

        if (riwayat.isEmpty()) {
            root.getChildren().add(label("Belum ada riwayat percakapan.",
                    "-fx-font-size: 12px; -fx-text-fill: #7A8A7E;"));
        } else {
            // Tampilkan dari yang terlama (balik urutan)
            for (int i = riwayat.size() - 1; i >= 0; i--) {
                String[] r = riwayat.get(i);
                root.getChildren().add(buatBubbleRiwayat(r[0], r[1], r[2]));
            }
        }

        return root;
    }

    private HBox buatBubbleRiwayat(String pengirim, String pesan, String waktu) {
        boolean isUser = "USER".equals(pengirim);
        HBox baris = new HBox();
        baris.setAlignment(isUser ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);

        VBox bubble = new VBox(3);
        bubble.setMaxWidth(300);
        bubble.setStyle(
                (isUser
                        ? "-fx-background-color: #1A3A2A; "
                        : "-fx-background-color: white; ") +
                        "-fx-background-radius: 10; -fx-padding: 8 12;");

        Label pesanLabel = new Label(pesan);
        pesanLabel.setWrapText(true);
        pesanLabel.setStyle(
                (isUser ? "-fx-text-fill: #F0EDE8; " : "-fx-text-fill: #2A2A2A; ") +
                        "-fx-font-size: 11px;");

        Label waktuLabel = label(waktu,
                "-fx-font-size: 9px; -fx-text-fill: "+(isUser?"#8AB09A":"#9A9A9A")+";");

        bubble.getChildren().addAll(pesanLabel, waktuLabel);
        baris.getChildren().add(bubble);
        return baris;
    }

    // ── Dialog detail & form ─────────────────────────────────

    private void tampilDetail(Notification n) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle("Detail Jadwal");
        a.setHeaderText(n.getEmoji() + "  " + n.getTitle());
        a.setContentText(
                "Tanggal : " + n.getDate() + "\n" +
                        "Waktu   : " + n.getSubtitle() + "\n" +
                        "Lokasi  : " + n.getLocation() + "\n\n" +
                        "Info lebih lanjut hubungi sekretariat:\n" +
                        DatabaseHelper.getInfo("telepon"));
        a.showAndWait();
    }

    private void tampilFormDaftar(String layanan) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle("Pendaftaran " + layanan);
        a.setHeaderText("Cara Mendaftar – " + layanan);
        a.setContentText(
                "1. Datang ke sekretariat\n" +
                        "   Senin-Jumat 08.00-16.00 WIB\n\n" +
                        "2. Hubungi via WhatsApp\n" +
                        "   " + DatabaseHelper.getInfo("whatsapp") + "\n\n" +
                        "3. Email ke\n" +
                        "   " + DatabaseHelper.getInfo("email") + "\n\n" +
                        "Siapkan dokumen persyaratan sebelum mendaftar.");
        a.showAndWait();
    }

    // ── Utility ───────────────────────────────────────────────

    private Label label(String teks, String style) {
        Label l = new Label(teks);
        l.setStyle(style);
        l.setWrapText(true);
        return l;
    }
}
