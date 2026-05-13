package com.gereja.chatbot.controller;

import com.gereja.chatbot.database.DatabaseHelper;

import javafx.animation.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.util.Duration;

import java.sql.*;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.BiConsumer;

/**
 * CalendarViewController
 * ─────────────────────────────────────────────────────────────────────────────
 * Membangun tampilan kalender bulanan berbasis JavaFX murni (tanpa FXML).
 * Kalender ini ditampilkan sebagai overlay di dalam StackPane area chat.
 *
 * Warna pastel konsisten dengan palet aplikasi:
 *   - Hijau tua   : #1A3A2A  → header kalender
 *   - Hijau pastel: #C8EDD8  → sel hari biasa
 *   - Emas pastel : #FDF3D0  → sel dengan kegiatan
 *   - Emas        : #D4A843  → border & aksen
 *   - Krem        : #FAF6F1  → background kartu
 */
public class CalenderViewController {

    // ── Palet warna (konsisten & pastel) ─────────────────────
    private static final String CLR_HEADER_BG     = "#1A3A2A";
    private static final String CLR_HEADER_TEXT   = "#F5F0EB";
    private static final String CLR_GOLD           = "#D4A843";
    private static final String CLR_CARD_BG        = "#FAF6F1";
    private static final String CLR_DAY_NORMAL     = "#E8F4EC";   // pastel hijau
    private static final String CLR_DAY_HOVER      = "#C8E8D4";   // hijau lebih dalam
    private static final String CLR_DAY_HAS_EVENT  = "#FDF3D0";   // pastel emas
    private static final String CLR_DAY_EVENT_HOVER= "#F5E0A0";
    private static final String CLR_DAY_TODAY      = "#B8E0C8";   // hijau medium
    private static final String CLR_DAY_TODAY_BORDER= "#2D7A4F";
    private static final String CLR_DAY_SELECTED   = "#D4A843";   // emas solid
    private static final String CLR_DAY_SELECTED_TXT= "#122A1E";
    private static final String CLR_WEEKDAY_LABEL  = "#5A7A6A";
    private static final String CLR_NAV_BTN        = "#2D5A3D";
    private static final String CLR_CLOSE_BTN      = "#C0392B";
    private static final String CLR_BADGE_BG       = "#D4A843";
    private static final String CLR_BADGE_TEXT      = "#122A1E";
    private static final String CLR_BORDER         = "#C8DDD0";
    private static final String CLR_TEXT_DARK      = "#1A3A2A";
    private static final String CLR_TEXT_MUTED     = "#7A9A8A";

    // ── State ────────────────────────────────────────────────
    private YearMonth   currentMonth;
    private StackPane   overlay;       // container overlay
    private GridPane    calendarGrid;
    private Label       lblMonthYear;
    private VBox        rootCard;

    /** Callback: (tanggal, daftarKegiatan) → dipanggil saat user klik tanggal */
    private final BiConsumer<LocalDate, List<Map<String,String>>> onDateSelected;

    // ── DateTimeFormatter ─────────────────────────────────────
    private static final DateTimeFormatter FMT_MONTH =
            DateTimeFormatter.ofPattern("MMMM yyyy", new Locale("id", "ID"));
    private static final DateTimeFormatter FMT_DATE_DB =
            DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter FMT_DISPLAY =
            DateTimeFormatter.ofPattern("EEEE, dd MMMM yyyy", new Locale("id", "ID"));

    // ─────────────────────────────────────────────────────────
    public CalenderViewController(BiConsumer<LocalDate, List<Map<String,String>>> onDateSelected) {
        this.onDateSelected = onDateSelected;
        this.currentMonth   = YearMonth.now();
    }

    // ══════════════════════════════════════════════════════════
    //  BUILD OVERLAY
    // ══════════════════════════════════════════════════════════

    /**
     * Membangun node overlay kalender lengkap.
     * Overlay ini dimasukkan ke StackPane di atas ScrollPane chat.
     */
    public StackPane buildOverlay() {
        // ── Backdrop semi-transparan ──────────────────────────
        overlay = new StackPane();
        overlay.setStyle("-fx-background-color: rgba(10, 25, 15, 0.45);");
        overlay.setAlignment(Pos.CENTER);

        // ── Kartu kalender utama ──────────────────────────────
        rootCard = new VBox(0);
        rootCard.setMaxWidth(680);
        rootCard.setMaxHeight(520);
        rootCard.setStyle(
                "-fx-background-color: " + CLR_CARD_BG + ";" +
                        "-fx-background-radius: 18;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.35), 24, 0, 0, 6);"
        );

        rootCard.getChildren().addAll(
                buildHeader(),
                buildWeekdayRow(),
                buildGridWrapper(),
                buildLegend()
        );

        // ── Klik backdrop tutup kalender ──────────────────────
        overlay.setOnMouseClicked(e -> {
            if (e.getTarget() == overlay) hideCalendar();
        });

        overlay.getChildren().add(rootCard);

        // Animasi masuk
        animateCardIn(rootCard);

        return overlay;
    }

    // ══════════════════════════════════════════════════════════
    //  HEADER (bulan + navigasi + tombol tutup)
    // ══════════════════════════════════════════════════════════

    private VBox buildHeader() {
        VBox header = new VBox(0);
        header.setStyle(
                "-fx-background-color: " + CLR_HEADER_BG + ";" +
                        "-fx-background-radius: 18 18 0 0;" +
                        "-fx-padding: 16 20 14 20;"
        );

        // Baris atas: judul + tombol tutup
        HBox topRow = new HBox();
        topRow.setAlignment(Pos.CENTER_LEFT);

        Label lblTitle = new Label("📅  Kalender Kegiatan Gereja");
        lblTitle.setStyle(
                "-fx-text-fill: " + CLR_GOLD + ";" +
                        "-fx-font-size: 14px;" +
                        "-fx-font-weight: bold;"
        );
        HBox.setHgrow(lblTitle, Priority.ALWAYS);

        Button btnClose = new Button("✕");
        btnClose.setStyle(
                "-fx-background-color: rgba(192,57,43,0.85);" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 12px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-background-radius: 50%;" +
                        "-fx-min-width: 26px;" +
                        "-fx-min-height: 26px;" +
                        "-fx-max-width: 26px;" +
                        "-fx-max-height: 26px;" +
                        "-fx-cursor: hand;" +
                        "-fx-padding: 0;"
        );
        btnClose.setOnAction(e -> hideCalendar());
        btnClose.setOnMouseEntered(e -> btnClose.setStyle(
                "-fx-background-color: #C0392B;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 12px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-background-radius: 50%;" +
                        "-fx-min-width: 26px; -fx-min-height: 26px;" +
                        "-fx-max-width: 26px; -fx-max-height: 26px;" +
                        "-fx-cursor: hand; -fx-padding: 0;" +
                        "-fx-scale-x: 1.1; -fx-scale-y: 1.1;"
        ));
        btnClose.setOnMouseExited(e -> btnClose.setStyle(
                "-fx-background-color: rgba(192,57,43,0.85);" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 12px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-background-radius: 50%;" +
                        "-fx-min-width: 26px; -fx-min-height: 26px;" +
                        "-fx-max-width: 26px; -fx-max-height: 26px;" +
                        "-fx-cursor: hand; -fx-padding: 0;"
        ));

        topRow.getChildren().addAll(lblTitle, btnClose);

        // Baris bawah: navigasi bulan
        HBox navRow = new HBox(12);
        navRow.setAlignment(Pos.CENTER);
        navRow.setPadding(new Insets(10, 0, 0, 0));

        Button btnPrev = buildNavButton("‹");
        btnPrev.setOnAction(e -> {
            currentMonth = currentMonth.minusMonths(1);
            rebuildGrid();
        });

        lblMonthYear = new Label(currentMonth.format(FMT_MONTH).toUpperCase());
        lblMonthYear.setStyle(
                "-fx-text-fill: " + CLR_HEADER_TEXT + ";" +
                        "-fx-font-size: 15px;" +
                        "-fx-font-weight: bold;"
        );
        lblMonthYear.setMinWidth(200);
        lblMonthYear.setAlignment(Pos.CENTER);

        Button btnNext = buildNavButton("›");
        btnNext.setOnAction(e -> {
            currentMonth = currentMonth.plusMonths(1);
            rebuildGrid();
        });

        // Tombol "Hari Ini"
        Button btnToday = new Button("Hari Ini");
        btnToday.setStyle(
                "-fx-background-color: " + CLR_GOLD + ";" +
                        "-fx-text-fill: #122A1E;" +
                        "-fx-font-size: 10px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-background-radius: 10;" +
                        "-fx-padding: 4 10;" +
                        "-fx-cursor: hand;"
        );
        btnToday.setOnAction(e -> {
            currentMonth = YearMonth.now();
            rebuildGrid();
        });

        navRow.getChildren().addAll(btnPrev, lblMonthYear, btnNext, btnToday);

        header.getChildren().addAll(topRow, navRow);
        return header;
    }

    private Button buildNavButton(String text) {
        Button btn = new Button(text);
        String styleNormal =
                "-fx-background-color: " + CLR_NAV_BTN + ";" +
                        "-fx-text-fill: " + CLR_GOLD + ";" +
                        "-fx-font-size: 18px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-background-radius: 50%;" +
                        "-fx-min-width: 32px; -fx-min-height: 32px;" +
                        "-fx-max-width: 32px; -fx-max-height: 32px;" +
                        "-fx-cursor: hand; -fx-padding: 0;";
        String styleHover =
                "-fx-background-color: " + CLR_GOLD + ";" +
                        "-fx-text-fill: #122A1E;" +
                        "-fx-font-size: 18px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-background-radius: 50%;" +
                        "-fx-min-width: 32px; -fx-min-height: 32px;" +
                        "-fx-max-width: 32px; -fx-max-height: 32px;" +
                        "-fx-cursor: hand; -fx-padding: 0;";
        btn.setStyle(styleNormal);
        btn.setOnMouseEntered(e -> btn.setStyle(styleHover));
        btn.setOnMouseExited(e -> btn.setStyle(styleNormal));
        return btn;
    }

    // ══════════════════════════════════════════════════════════
    //  BARIS NAMA HARI (Sen–Min)
    // ══════════════════════════════════════════════════════════

    private HBox buildWeekdayRow() {
        HBox row = new HBox(0);
        row.setStyle(
                "-fx-background-color: #E8F4EC;" +
                        "-fx-padding: 8 20 8 20;" +
                        "-fx-border-color: " + CLR_BORDER + ";" +
                        "-fx-border-width: 0 0 1 0;"
        );
        String[] days = {"Sen", "Sel", "Rab", "Kam", "Jum", "Sab", "Min"};
        for (String d : days) {
            Label lbl = new Label(d);
            lbl.setStyle(
                    "-fx-text-fill: " + CLR_WEEKDAY_LABEL + ";" +
                            "-fx-font-size: 11px;" +
                            "-fx-font-weight: bold;"
            );
            lbl.setAlignment(Pos.CENTER);
            lbl.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(lbl, Priority.ALWAYS);
            // Minggu merah
            if ("Min".equals(d)) lbl.setStyle(
                    "-fx-text-fill: #C05050;" +
                            "-fx-font-size: 11px;" +
                            "-fx-font-weight: bold;"
            );
            row.getChildren().add(lbl);
        }
        return row;
    }

    // ══════════════════════════════════════════════════════════
    //  GRID KALENDER
    // ══════════════════════════════════════════════════════════

    private VBox buildGridWrapper() {
        VBox wrapper = new VBox();
        wrapper.setStyle("-fx-padding: 10 14 6 14;");
        VBox.setVgrow(wrapper, Priority.ALWAYS);

        calendarGrid = new GridPane();
        calendarGrid.setHgap(6);
        calendarGrid.setVgap(6);

        // 7 kolom sama lebar
        for (int i = 0; i < 7; i++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setHgrow(Priority.ALWAYS);
            cc.setPercentWidth(100.0 / 7);
            calendarGrid.getColumnConstraints().add(cc);
        }

        populateGrid();

        wrapper.getChildren().add(calendarGrid);
        return wrapper;
    }

    private void populateGrid() {
        calendarGrid.getChildren().clear();

        LocalDate today     = LocalDate.now();
        LocalDate firstDay  = currentMonth.atDay(1);
        // Hari mulai (Senin=0 … Minggu=6)
        int startCol = firstDay.getDayOfWeek().getValue() - 1;
        int daysInMonth = currentMonth.lengthOfMonth();

        // Ambil semua jadwal di bulan ini
        Map<String, List<Map<String,String>>> jadwalPerTanggal = getJadwalBulan(currentMonth);

        int col = startCol;
        int row = 0;

        for (int day = 1; day <= daysInMonth; day++) {
            LocalDate date = currentMonth.atDay(day);
            String dateKey = date.format(FMT_DATE_DB);
            List<Map<String,String>> events = jadwalPerTanggal.getOrDefault(dateKey, List.of());

            VBox cell = buildDayCell(date, today, events);

            calendarGrid.add(cell, col, row);

            col++;
            if (col == 7) { col = 0; row++; }
        }

        // Update label bulan
        if (lblMonthYear != null)
            lblMonthYear.setText(currentMonth.format(FMT_MONTH).toUpperCase());
    }

    /** Rebuild grid saat navigasi bulan */
    private void rebuildGrid() {
        populateGrid();
        // Animasi ringan
        FadeTransition ft = new FadeTransition(Duration.millis(180), calendarGrid);
        ft.setFromValue(0.5);
        ft.setToValue(1.0);
        ft.play();
    }

    // ══════════════════════════════════════════════════════════
    //  SEL HARI
    // ══════════════════════════════════════════════════════════

    private VBox buildDayCell(LocalDate date, LocalDate today, List<Map<String,String>> events) {
        VBox cell = new VBox(2);
        cell.setAlignment(Pos.TOP_CENTER);
        cell.setPadding(new Insets(6, 4, 6, 4));
        cell.setMinHeight(60);

        boolean isToday    = date.equals(today);
        boolean hasEvent   = !events.isEmpty();
        boolean isSunday   = date.getDayOfWeek().getValue() == 7;

        // ── Warna sel ─────────────────────────────────────────
        String bgNormal, bgHover;
        String borderNormal = CLR_BORDER;

        if (isToday) {
            bgNormal    = CLR_DAY_TODAY;
            bgHover     = CLR_DAY_HOVER;
            borderNormal= CLR_DAY_TODAY_BORDER;
        } else if (hasEvent) {
            bgNormal    = CLR_DAY_HAS_EVENT;
            bgHover     = CLR_DAY_EVENT_HOVER;
        } else {
            bgNormal    = CLR_DAY_NORMAL;
            bgHover     = CLR_DAY_HOVER;
        }

        String styleNormal = buildCellStyle(bgNormal, borderNormal, false);
        String styleHover  = buildCellStyle(bgHover, CLR_NAV_BTN, true);
        String styleToday  = isToday ? styleNormal : null;

        cell.setStyle(styleNormal);
        cell.setOnMouseEntered(e -> cell.setStyle(styleHover));
        cell.setOnMouseExited(e  -> cell.setStyle(isToday ? styleNormal : styleNormal));

        // ── Nomor tanggal ─────────────────────────────────────
        Label lblDay = new Label(String.valueOf(date.getDayOfMonth()));
        lblDay.setStyle(
                "-fx-font-size: 13px;" +
                        "-fx-font-weight: " + (isToday ? "bold" : "normal") + ";" +
                        "-fx-text-fill: " + (isSunday ? "#C05050" : (isToday ? CLR_TEXT_DARK : CLR_TEXT_DARK)) + ";"
        );

        cell.getChildren().add(lblDay);

        // ── Badge jumlah kegiatan ──────────────────────────────
        if (hasEvent) {
            int count = events.size();
            HBox badgeRow = new HBox(3);
            badgeRow.setAlignment(Pos.CENTER);

            if (count <= 2) {
                // Tampilkan nama singkat
                for (Map<String,String> ev : events) {
                    String nama = ev.getOrDefault("nama_kegiatan", "Kegiatan");
                    if (nama.length() > 9) nama = nama.substring(0, 8) + "…";
                    Label chip = new Label(nama);
                    chip.setStyle(
                            "-fx-background-color: " + CLR_BADGE_BG + ";" +
                                    "-fx-text-fill: " + CLR_BADGE_TEXT + ";" +
                                    "-fx-font-size: 7.5px;" +
                                    "-fx-font-weight: bold;" +
                                    "-fx-background-radius: 4;" +
                                    "-fx-padding: 1 4;"
                    );
                    badgeRow.getChildren().add(chip);
                }
            } else {
                // Hanya badge angka
                Label badge = new Label("+" + count + " acara");
                badge.setStyle(
                        "-fx-background-color: " + CLR_BADGE_BG + ";" +
                                "-fx-text-fill: " + CLR_BADGE_TEXT + ";" +
                                "-fx-font-size: 8px;" +
                                "-fx-font-weight: bold;" +
                                "-fx-background-radius: 6;" +
                                "-fx-padding: 2 6;"
                );
                badgeRow.getChildren().add(badge);
            }
            cell.getChildren().add(badgeRow);
        }

        // ── Klik tanggal → kirim ke chat ──────────────────────
        cell.setOnMouseClicked(e -> {
            // Animasi press
            ScaleTransition st = new ScaleTransition(Duration.millis(100), cell);
            st.setFromX(1.0); st.setToX(0.93);
            st.setFromY(1.0); st.setToY(0.93);
            st.setAutoReverse(true);
            st.setCycleCount(2);
            st.play();

            st.setOnFinished(ev -> {
                hideCalendar();
                onDateSelected.accept(date, events);
            });
        });

        return cell;
    }

    private String buildCellStyle(String bg, String border, boolean hover) {
        return
                "-fx-background-color: " + bg + ";" +
                        "-fx-background-radius: 10;" +
                        "-fx-border-color: " + border + ";" +
                        "-fx-border-radius: 10;" +
                        "-fx-border-width: 1;" +
                        "-fx-cursor: hand;" +
                        (hover ? "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.12), 6, 0, 0, 2);" : "");
    }

    // ══════════════════════════════════════════════════════════
    //  LEGENDA
    // ══════════════════════════════════════════════════════════

    private HBox buildLegend() {
        HBox legend = new HBox(16);
        legend.setAlignment(Pos.CENTER);
        legend.setStyle(
                "-fx-padding: 8 20 14 20;" +
                        "-fx-border-color: " + CLR_BORDER + ";" +
                        "-fx-border-width: 1 0 0 0;"
        );

        legend.getChildren().addAll(
                legendItem(CLR_DAY_TODAY,     "Hari ini"),
                legendItem(CLR_DAY_HAS_EVENT, "Ada kegiatan"),
                legendItem(CLR_DAY_NORMAL,    "Tidak ada kegiatan")
        );
        return legend;
    }

    private HBox legendItem(String color, String label) {
        HBox item = new HBox(6);
        item.setAlignment(Pos.CENTER_LEFT);

        Label dot = new Label("  ");
        dot.setStyle(
                "-fx-background-color: " + color + ";" +
                        "-fx-background-radius: 4;" +
                        "-fx-min-width: 14px; -fx-min-height: 14px;" +
                        "-fx-border-color: " + CLR_BORDER + "; -fx-border-radius: 4; -fx-border-width: 1;"
        );

        Label txt = new Label(label);
        txt.setStyle("-fx-text-fill: " + CLR_TEXT_MUTED + "; -fx-font-size: 10px;");

        item.getChildren().addAll(dot, txt);
        return item;
    }

    // ══════════════════════════════════════════════════════════
    //  SHOW / HIDE
    // ══════════════════════════════════════════════════════════

    public void hideCalendar() {
        ScaleTransition st = new ScaleTransition(Duration.millis(160), rootCard);
        st.setFromX(1.0); st.setToX(0.92);
        st.setFromY(1.0); st.setToY(0.92);

        FadeTransition ft = new FadeTransition(Duration.millis(160), overlay);
        ft.setFromValue(1.0); ft.setToValue(0.0);

        ParallelTransition pt = new ParallelTransition(st, ft);
        pt.setOnFinished(e -> {
            if (overlay.getParent() instanceof StackPane sp) {
                sp.getChildren().remove(overlay);
            }
        });
        pt.play();
    }

    private void animateCardIn(VBox card) {
        card.setScaleX(0.88);
        card.setScaleY(0.88);
        card.setOpacity(0);
        overlay.setOpacity(0);

        ScaleTransition st = new ScaleTransition(Duration.millis(220), card);
        st.setFromX(0.88); st.setToX(1.0);
        st.setFromY(0.88); st.setToY(1.0);
        st.setInterpolator(Interpolator.EASE_OUT);

        // BUG FIX: card.setOpacity(0) di-set tapi tidak pernah di-animasikan.
        // FadeTransition untuk card harus ada agar kartu kalender terlihat.
        FadeTransition ftCard = new FadeTransition(Duration.millis(200), card);
        ftCard.setFromValue(0.0);
        ftCard.setToValue(1.0);

        FadeTransition ftOverlay = new FadeTransition(Duration.millis(200), overlay);
        ftOverlay.setFromValue(0.0);
        ftOverlay.setToValue(1.0);

        // Simpan referensi agar tidak di-GC sebelum selesai
        ParallelTransition pt = new ParallelTransition(st, ftCard, ftOverlay);
        pt.play();
    }

    // ══════════════════════════════════════════════════════════
    //  DATABASE – ambil jadwal satu bulan
    // ══════════════════════════════════════════════════════════

    /**
     * Mengembalikan Map<tanggal_string, List<event>> untuk satu bulan.
     * tanggal_string format: "yyyy-MM-dd"
     */
    public static Map<String, List<Map<String,String>>> getJadwalBulan(YearMonth ym) {
        Map<String, List<Map<String,String>>> result = new HashMap<>();

        String startDate = ym.atDay(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String endDate   = ym.atEndOfMonth().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        String sql = "SELECT nama_kegiatan, kategori, tanggal, jam_mulai, jam_selesai, lokasi " +
                "FROM jadwal WHERE tanggal >= ? AND tanggal <= ? ORDER BY tanggal, jam_mulai";

        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, startDate);
            ps.setString(2, endDate);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String tgl = rs.getString("tanggal");
                Map<String,String> ev = new LinkedHashMap<>();
                ev.put("nama_kegiatan", rs.getString("nama_kegiatan"));
                ev.put("kategori",      rs.getString("kategori"));
                ev.put("tanggal",       tgl);
                ev.put("jam_mulai",     rs.getString("jam_mulai")   != null ? rs.getString("jam_mulai")   : "");
                ev.put("jam_selesai",   rs.getString("jam_selesai") != null ? rs.getString("jam_selesai") : "");
                ev.put("lokasi",        rs.getString("lokasi")      != null ? rs.getString("lokasi")      : "");
                result.computeIfAbsent(tgl, k -> new ArrayList<>()).add(ev);
            }
        } catch (SQLException e) {
            System.out.println("[CalendarView] getJadwalBulan error: " + e.getMessage());
        }
        return result;
    }

    /**
     * Format pesan chat untuk tanggal yang dipilih user.
     */
    public static String formatEventMessage(LocalDate date, List<Map<String,String>> events) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("EEEE, dd MMMM yyyy", new Locale("id", "ID"));
        String tgl = date.format(fmt);

        if (events == null || events.isEmpty()) {
            return "📅 " + tgl + "\n\n" +
                    "Tidak ada kegiatan gereja yang terdaftar pada tanggal ini.\n\n" +
                    "Untuk informasi lebih lanjut, hubungi sekretariat gereja.";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("📅 Kegiatan pada ").append(tgl).append(":\n\n");

        int no = 1;
        for (Map<String,String> ev : events) {
            sb.append(no++).append(". ")
                    .append("📌 ").append(ev.getOrDefault("nama_kegiatan", "-")).append("\n");

            String jamMulai   = ev.getOrDefault("jam_mulai", "");
            String jamSelesai = ev.getOrDefault("jam_selesai", "");
            if (!jamMulai.isEmpty()) {
                sb.append("   🕐 ").append(jamMulai);
                if (!jamSelesai.isEmpty()) sb.append(" – ").append(jamSelesai);
                sb.append("\n");
            }

            String lokasi = ev.getOrDefault("lokasi", "");
            if (!lokasi.isEmpty()) sb.append("   📍 ").append(lokasi).append("\n");

            String kat = ev.getOrDefault("kategori", "");
            if (!kat.isEmpty()) sb.append("   🏷️ ").append(kat).append("\n");

            sb.append("\n");
        }

        sb.append("Untuk pendaftaran atau informasi lebih lanjut, hubungi sekretariat gereja.");
        return sb.toString();
    }
}