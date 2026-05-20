package com.gereja.chatbot.controller;

import com.gereja.chatbot.database.DatabaseHelper;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.io.IOException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * AdminDashboardController – Panel Admin Faith Buddy
 * Tab: Info Gereja | Pelayanan | Kata Kunci Q&A | Ayat Harian | Kelola Admin
 */
public class AdminDashboardController {

    // ─── Top Bar ───
    @FXML private Label lblAdminName;

    // ─── Nav Buttons ───
    @FXML private Button btnNavInfo, btnNavPelayanan, btnNavKataKunci, btnNavAyat, btnNavKalender, btnNavAdmin;

    // ─── Stats ───
    @FXML private Label lblTotalChat, lblTotalKataKunci, lblTotalAyat;

    // ─── Tab Panels ───
    @FXML private VBox tabInfo, tabPelayanan, tabKataKunci, tabAyat, tabKalender, tabAdmin;

    // ─── Info Gereja ───
    @FXML private TextField txtInfoKunci, txtInfoNilai;
    @FXML private TableView<Map<String, String>> tblInfo;
    @FXML private TableColumn<Map<String,String>, String> colInfoId, colInfoKunci, colInfoNilai;
    @FXML private TableColumn<Map<String,String>, Void> colInfoAksi;
    private Integer editingInfoId = null;

    // ─── Pelayanan ───
    @FXML private ComboBox<String> cmbKategori, cmbTipePelayanan, cmbFilterKategori;
    @FXML private TextField txtPelNamaItem, txtPelBiaya;
    @FXML private TextArea txtPelKeterangan;
    @FXML private TableView<Map<String, String>> tblPelayanan;
    @FXML private TableColumn<Map<String,String>,String> colPelId, colPelKategori, colPelTipe, colPelNama, colPelBiaya;
    @FXML private TableColumn<Map<String,String>,Void> colPelAksi;
    private Integer editingPelId = null;

    // ─── Kata Kunci Q&A ───
    @FXML private TextField txtKK, txtCariKK;
    @FXML private TextArea txtKKPertanyaan, txtKKJawaban;
    @FXML private ComboBox<String> cmbKKKategori, cmbFilterKK;
    @FXML private TableView<Map<String, String>> tblKataKunci;
    @FXML private TableColumn<Map<String,String>,String> colKKId, colKKKunci, colKKKategori, colKKPertanyaan, colKKHit;
    @FXML private TableColumn<Map<String,String>,Void> colKKAksi;
    private Integer editingKKId = null;

    // ─── Ayat Harian ───
    @FXML private TextField txtAyatReferensi, txtAyatKeterangan, txtAyatTanggal;
    @FXML private TextArea txtAyatIsi;
    @FXML private CheckBox chkAyatAktif;
    @FXML private Label lblAyatFormMsg, lblPreviewRef, lblPreviewAyat;
    @FXML private TableView<Map<String, String>> tblAyat;
    @FXML private TableColumn<Map<String,String>,String> colAyatId, colAyatTanggal, colAyatReferensi, colAyatIsi, colAyatKet, colAyatAktif;
    @FXML private TableColumn<Map<String,String>,Void> colAyatAksi;
    private Integer editingAyatId = null;

    // ─── Kalender Kegiatan ───
    @FXML private GridPane calendarGrid;
    @FXML private VBox calendarEventList;
    @FXML private Label lblCalendarMonth, lblCalendarSelectedDate, lblCalendarSummary;
    private YearMonth currentCalendarMonth = YearMonth.now();
    private static final DateTimeFormatter FMT_DATE_DB = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter FMT_MONTH_ID =
            DateTimeFormatter.ofPattern("MMMM yyyy", new java.util.Locale("id", "ID"));
    private static final DateTimeFormatter FMT_DATE_ID =
            DateTimeFormatter.ofPattern("EEEE, dd MMMM yyyy", new java.util.Locale("id", "ID"));

    // ─── Kelola Admin ───
    @FXML private TextField txtAdminUser, txtAdminNama;
    @FXML private PasswordField txtAdminPw, txtAdminPwKonfirm;
    @FXML private Label lblAdminFormMsg;
    @FXML private TableView<Map<String, String>> tblAdmin;
    @FXML private TableColumn<Map<String,String>,String> colAdminId, colAdminUser, colAdminNama;
    @FXML private TableColumn<Map<String,String>,Void> colAdminAksi;
    private Integer editingAdminId = null;

    private String currentUsername;

    private static final List<String> KATEGORI_LIST =
            List.of("BAPTIS", "PERNIKAHAN", "SIDI", "KONSELING", "UMUM");
    private static final List<String> TIPE_PELAYANAN =
            List.of("SYARAT_DOKUMEN", "BIAYA_LAYANAN");

    // ══════════════════════════════════════════════════════════
    //  INIT
    // ══════════════════════════════════════════════════════════

    @FXML public void initialize() {
        setupInfoTable();
        setupPelayananTable();
        setupKataKunciTable();
        setupAyatTable();
        setupKalender();
        setupAdminTable();
        muatSemuaData();
        showTabInfo();
    }

    public void setAdminInfo(String namaAdmin, String username) {
        this.currentUsername = username;
        lblAdminName.setText("Login sebagai: " + namaAdmin);
    }

    // ══════════════════════════════════════════════════════════
    //  NAVIGASI TAB
    // ══════════════════════════════════════════════════════════

    @FXML public void showTabInfo()       { switchTab(tabInfo,       btnNavInfo); }
    @FXML public void showTabPelayanan()  { switchTab(tabPelayanan,  btnNavPelayanan); }
    @FXML public void showTabKataKunci()  { switchTab(tabKataKunci,  btnNavKataKunci); }
    @FXML public void showTabAyat()       { switchTab(tabAyat,       btnNavAyat); loadPreviewAyat(); }
    @FXML public void showTabKalender()   { switchTab(tabKalender,   btnNavKalender); renderKalender(); }
    @FXML public void showTabAdmin()      { switchTab(tabAdmin,      btnNavAdmin); }

    private void switchTab(VBox active, Button activeBtn) {
        for (VBox v : List.of(tabInfo, tabPelayanan, tabKataKunci, tabAyat, tabKalender, tabAdmin)) {
            v.setVisible(false); v.setManaged(false);
        }
        active.setVisible(true); active.setManaged(true);

        String activeStyle = "-fx-background-color: #D4A843; -fx-text-fill: #122A1E; " +
                "-fx-background-radius: 8; -fx-padding: 10 14; -fx-alignment: CENTER_LEFT; " +
                "-fx-cursor: hand; -fx-font-size: 13px; -fx-font-weight: bold;";
        String inactiveStyle = "-fx-background-color: transparent; -fx-text-fill: #C8DDD0; " +
                "-fx-background-radius: 8; -fx-padding: 10 14; -fx-alignment: CENTER_LEFT; " +
                "-fx-cursor: hand; -fx-font-size: 13px;";
        for (Button b : List.of(btnNavInfo, btnNavPelayanan, btnNavKataKunci, btnNavAyat, btnNavKalender, btnNavAdmin))
            b.setStyle(inactiveStyle);
        activeBtn.setStyle(activeStyle);
    }

    // ══════════════════════════════════════════════════════════
    //  MUAT DATA & STATISTIK
    // ══════════════════════════════════════════════════════════

    private void muatSemuaData() {
        muatTblInfo();
        muatTblPelayanan();
        muatTblKataKunci();
        muatTblAyat();
        renderKalender();
        muatTblAdmin();
        muatStatistik();
    }

    private void muatStatistik() {
        lblTotalChat.setText(String.valueOf(DatabaseHelper.countRiwayatChat()));
        lblTotalKataKunci.setText(String.valueOf(DatabaseHelper.countKataKunci()));
        lblTotalAyat.setText(String.valueOf(DatabaseHelper.countAyatHarian()));
    }

    // ══════════════════════════════════════════════════════════
    //  INFO GEREJA
    // ══════════════════════════════════════════════════════════

    private void setupInfoTable() {
        colInfoId.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().get("id")));
        colInfoKunci.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().get("kunci")));
        colInfoNilai.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().get("nilai")));
        addAksiColumn(colInfoAksi, "INFO");
    }

    private void muatTblInfo() {
        tblInfo.setItems(FXCollections.observableArrayList(DatabaseHelper.getAllInfoGereja()));
    }

    @FXML private void simpanInfo() {
        String kunci = txtInfoKunci.getText().trim();
        String nilai = txtInfoNilai.getText().trim();
        if (kunci.isEmpty() || nilai.isEmpty()) {
            showAlert("Kunci dan nilai wajib diisi.", Alert.AlertType.WARNING); return;
        }
        if (editingInfoId != null) {
            DatabaseHelper.updateInfoGereja(editingInfoId, kunci, nilai);
            editingInfoId = null;
        } else {
            DatabaseHelper.insertInfoGereja(kunci, nilai);
        }
        resetFormInfo(); muatTblInfo();
    }

    @FXML private void resetFormInfo() {
        txtInfoKunci.clear(); txtInfoNilai.clear(); editingInfoId = null;
    }

    // ══════════════════════════════════════════════════════════
    //  PELAYANAN
    // ══════════════════════════════════════════════════════════

    private void setupPelayananTable() {
        colPelId.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().get("id")));
        colPelKategori.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().get("kategori")));
        colPelTipe.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().get("tipe")));
        colPelNama.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().get("nama")));
        colPelBiaya.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().get("biaya")));
        addAksiColumn(colPelAksi, "PELAYANAN");

        cmbKategori.setItems(FXCollections.observableArrayList(KATEGORI_LIST));
        cmbTipePelayanan.setItems(FXCollections.observableArrayList(TIPE_PELAYANAN));
        cmbFilterKategori.setItems(FXCollections.observableArrayList(
                List.of("SEMUA", "BAPTIS", "PERNIKAHAN", "SIDI", "KONSELING")));
        cmbFilterKategori.getSelectionModel().selectFirst();
    }

    private void muatTblPelayanan() {
        String filter = cmbFilterKategori.getValue();
        List<Map<String,String>> data = (filter == null || "SEMUA".equals(filter))
                ? DatabaseHelper.getAllPelayanan()
                : DatabaseHelper.getPelayananByKategori(filter);
        tblPelayanan.setItems(FXCollections.observableArrayList(data));
    }

    @FXML private void filterPelayanan() { muatTblPelayanan(); }

    @FXML private void simpanPelayanan() {
        String kat   = cmbKategori.getValue();
        String tipe  = cmbTipePelayanan.getValue();
        String nama  = txtPelNamaItem.getText().trim();
        String biaya = txtPelBiaya.getText().trim();
        String ket   = txtPelKeterangan.getText().trim();
        if (kat == null || tipe == null || nama.isEmpty()) {
            showAlert("Kategori, tipe, dan nama item wajib diisi.", Alert.AlertType.WARNING); return;
        }
        if (editingPelId != null) {
            DatabaseHelper.updatePelayanan(editingPelId, tipe, kat, nama, biaya, ket);
            editingPelId = null;
        } else {
            DatabaseHelper.insertPelayanan(tipe, kat, nama, biaya, ket);
        }
        resetFormPelayanan(); muatTblPelayanan();
    }

    @FXML private void resetFormPelayanan() {
        cmbKategori.getSelectionModel().clearSelection();
        cmbTipePelayanan.getSelectionModel().clearSelection();
        txtPelNamaItem.clear(); txtPelBiaya.clear(); txtPelKeterangan.clear();
        editingPelId = null;
    }

    // ══════════════════════════════════════════════════════════
    //  KATA KUNCI Q&A
    // ══════════════════════════════════════════════════════════

    private void setupKataKunciTable() {
        colKKId.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().get("id")));
        colKKKunci.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().get("kata_kunci")));
        colKKKategori.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().get("kategori")));
        colKKPertanyaan.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().get("pertanyaan_asli")));
        colKKHit.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().get("hit_count")));
        addAksiColumn(colKKAksi, "KATAKUNCI");

        cmbKKKategori.setItems(FXCollections.observableArrayList(KATEGORI_LIST));
        cmbFilterKK.setItems(FXCollections.observableArrayList(
                List.of("SEMUA", "BAPTIS", "PERNIKAHAN", "SIDI", "KONSELING", "UMUM")));
        cmbFilterKK.getSelectionModel().selectFirst();
        txtCariKK.textProperty().addListener((o, ov, nv) -> filterKataKunci());
    }

    private void muatTblKataKunci() {
        String filter = cmbFilterKK.getValue();
        String cari   = txtCariKK.getText().trim();
        List<Map<String,String>> data = DatabaseHelper.searchKataKunci(
                (filter == null || "SEMUA".equals(filter)) ? null : filter,
                cari.isEmpty() ? null : cari);
        tblKataKunci.setItems(FXCollections.observableArrayList(data));
        muatStatistik();
    }

    @FXML private void filterKataKunci() { muatTblKataKunci(); }

    @FXML private void simpanKataKunci() {
        String kk   = txtKK.getText().trim();
        String pert = txtKKPertanyaan.getText().trim();
        String jaw  = txtKKJawaban.getText().trim();
        String kat  = cmbKKKategori.getValue();
        if (kk.isEmpty() || jaw.isEmpty()) {
            showAlert("Kata kunci dan jawaban wajib diisi.", Alert.AlertType.WARNING); return;
        }
        if (editingKKId != null) {
            DatabaseHelper.updateKataKunci(editingKKId, kk, pert, jaw, kat != null ? kat : "UMUM");
            editingKKId = null;
        } else {
            DatabaseHelper.insertKataKunci(kk, pert, jaw, kat != null ? kat : "UMUM");
        }
        resetFormKataKunci(); muatTblKataKunci();
    }

    @FXML private void resetFormKataKunci() {
        txtKK.clear(); txtKKPertanyaan.clear(); txtKKJawaban.clear();
        cmbKKKategori.getSelectionModel().clearSelection();
        editingKKId = null;
    }

    @FXML private void scanRiwayatChat() {
        List<String[]> riwayat = DatabaseHelper.getRiwayatChat(200);
        int added = 0;
        for (int i = 0; i < riwayat.size() - 1; i++) {
            String[] pesan = riwayat.get(i);
            if (!"USER".equals(pesan[0])) continue;
            String pertanyaan = pesan[1];
            String jawaban = (i + 1 < riwayat.size() && "BOT".equals(riwayat.get(i + 1)[0]))
                    ? riwayat.get(i + 1)[1] : "";
            if (jawaban.isEmpty()) continue;
            String kataKunci = DatabaseHelper.ekstrakKataKunci(pertanyaan);
            if (kataKunci == null || kataKunci.isEmpty()) continue;
            if (!DatabaseHelper.isKataKunciExists(kataKunci)) {
                String kategori = DatabaseHelper.deteksiKategori(pertanyaan);
                DatabaseHelper.insertKataKunci(kataKunci, pertanyaan, jawaban, kategori);
                added++;
            }
        }
        muatTblKataKunci();
        showAlert(added > 0
                ? "✅ " + added + " kata kunci baru berhasil diekstrak dari riwayat chat!"
                : "ℹ️ Tidak ada kata kunci baru yang ditemukan.", Alert.AlertType.INFORMATION);
    }

    // ══════════════════════════════════════════════════════════
    //  AYAT HARIAN
    // ══════════════════════════════════════════════════════════

    private void setupAyatTable() {
        colAyatId.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().get("id")));
        colAyatTanggal.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().get("tanggal")));
        colAyatReferensi.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().get("referensi")));
        colAyatIsi.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().get("ayat")));
        colAyatKet.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().get("keterangan")));
        colAyatAktif.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().get("aktif")));
        addAksiColumn(colAyatAksi, "AYAT");
    }

    private void muatTblAyat() {
        tblAyat.setItems(FXCollections.observableArrayList(DatabaseHelper.getAllAyatHarian()));
        muatStatistik();
    }

    private void loadPreviewAyat() {
        try {
            String[] ayat = DatabaseHelper.getAyatHarian();
            if (ayat != null && ayat.length >= 2) {
                lblPreviewRef.setText(ayat[0]);
                lblPreviewAyat.setText("\"" + ayat[1] + "\"");
            }
        } catch (Exception e) {
            lblPreviewRef.setText("—");
            lblPreviewAyat.setText("—");
        }
    }

    @FXML private void simpanAyat() {
        String ref = txtAyatReferensi.getText().trim();
        String isi = txtAyatIsi.getText().trim();
        String ket = txtAyatKeterangan.getText().trim();
        String tgl = txtAyatTanggal.getText().trim();
        boolean aktif = chkAyatAktif.isSelected();

        if (ref.isEmpty() || isi.isEmpty()) {
            showAyatFormMsg("Referensi dan isi ayat wajib diisi."); return;
        }
        // Validasi format tanggal jika diisi
        if (!tgl.isEmpty() && !tgl.matches("\\d{4}-\\d{2}-\\d{2}")) {
            showAyatFormMsg("Format tanggal: YYYY-MM-DD (contoh: 2026-12-25)"); return;
        }

        if (editingAyatId != null) {
            DatabaseHelper.updateAyatHarian(editingAyatId, tgl.isEmpty() ? null : tgl, ref, isi, ket, aktif);
            editingAyatId = null;
        } else {
            DatabaseHelper.insertAyatHarian(tgl.isEmpty() ? null : tgl, ref, isi, ket);
        }
        resetFormAyat();
        muatTblAyat();
        loadPreviewAyat();
        showAlert("✅ Ayat harian berhasil disimpan!", Alert.AlertType.INFORMATION);
    }

    @FXML private void resetFormAyat() {
        txtAyatReferensi.clear(); txtAyatIsi.clear();
        txtAyatKeterangan.clear(); txtAyatTanggal.clear();
        chkAyatAktif.setSelected(true);
        editingAyatId = null;
        lblAyatFormMsg.setVisible(false); lblAyatFormMsg.setManaged(false);
    }

    private void showAyatFormMsg(String msg) {
        lblAyatFormMsg.setText(msg);
        lblAyatFormMsg.setVisible(true); lblAyatFormMsg.setManaged(true);
    }

    // ══════════════════════════════════════════════════════════
    //  KALENDER KEGIATAN
    // ══════════════════════════════════════════════════════════

    private void setupKalender() {
        calendarGrid.getColumnConstraints().clear();
        calendarGrid.getRowConstraints().clear();

        for (int i = 0; i < 7; i++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setHgrow(Priority.ALWAYS);
            cc.setPercentWidth(100.0 / 7);
            calendarGrid.getColumnConstraints().add(cc);
        }

        for (int i = 0; i < 6; i++) {
            RowConstraints rc = new RowConstraints();
            rc.setVgrow(Priority.ALWAYS);
            rc.setMinHeight(72);
            calendarGrid.getRowConstraints().add(rc);
        }
    }

    @FXML private void kalenderBulanSebelumnya() {
        currentCalendarMonth = currentCalendarMonth.minusMonths(1);
        renderKalender();
    }

    @FXML private void kalenderBulanBerikutnya() {
        currentCalendarMonth = currentCalendarMonth.plusMonths(1);
        renderKalender();
    }

    @FXML private void kalenderHariIni() {
        currentCalendarMonth = YearMonth.now();
        renderKalender();
        tampilkanDetailTanggal(LocalDate.now(), getEventsForDate(LocalDate.now()));
    }

    private void renderKalender() {
        if (calendarGrid == null) return;

        calendarGrid.getChildren().clear();
        lblCalendarMonth.setText(currentCalendarMonth.format(FMT_MONTH_ID).toUpperCase());

        LocalDate firstDay = currentCalendarMonth.atDay(1);
        LocalDate today = LocalDate.now();
        int startCol = firstDay.getDayOfWeek().getValue() - 1;
        int daysInMonth = currentCalendarMonth.lengthOfMonth();
        String startDate = currentCalendarMonth.atDay(1).format(FMT_DATE_DB);
        String endDate = currentCalendarMonth.atEndOfMonth().format(FMT_DATE_DB);
        Map<String, List<Map<String, String>>> eventsByDate =
                DatabaseHelper.getJadwalBulan(startDate, endDate);

        int col = startCol;
        int row = 0;
        for (int day = 1; day <= daysInMonth; day++) {
            LocalDate date = currentCalendarMonth.atDay(day);
            List<Map<String, String>> events =
                    eventsByDate.getOrDefault(date.format(FMT_DATE_DB), List.of());

            calendarGrid.add(buildCalendarDayCell(date, today, events), col, row);

            col++;
            if (col == 7) {
                col = 0;
                row++;
            }
        }

        tampilkanRingkasanBulan(eventsByDate);
    }

    private VBox buildCalendarDayCell(LocalDate date, LocalDate today, List<Map<String, String>> events) {
        VBox cell = new VBox(4);
        cell.setPadding(new Insets(8));
        cell.setAlignment(Pos.TOP_LEFT);
        cell.setMinHeight(72);
        cell.setMaxWidth(Double.MAX_VALUE);

        boolean isToday = date.equals(today);
        boolean hasEvent = !events.isEmpty();
        String bg = isToday ? "#B8E0C8" : hasEvent ? "#FDF3D0" : "#F7FBF8";
        String border = isToday ? "#2D7A4F" : hasEvent ? "#D4A843" : "#DCE8E0";
        cell.setStyle(calendarCellStyle(bg, border));
        cell.setOnMouseEntered(e -> cell.setStyle(calendarCellStyle(hasEvent ? "#F5E0A0" : "#E8F4EC", "#2D5A3D")));
        cell.setOnMouseExited(e -> cell.setStyle(calendarCellStyle(bg, border)));
        cell.setOnMouseClicked(e -> tampilkanDetailTanggal(date, events));

        HBox topRow = new HBox();
        topRow.setAlignment(Pos.CENTER_LEFT);
        Label lblDay = new Label(String.valueOf(date.getDayOfMonth()));
        lblDay.setStyle("-fx-text-fill: #1A3A2A; -fx-font-size: 13px; -fx-font-weight: "
                + (isToday ? "bold" : "normal") + ";");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        topRow.getChildren().addAll(lblDay, spacer);

        if (hasEvent) {
            Label badge = new Label(String.valueOf(events.size()));
            badge.setStyle("-fx-background-color: #D4A843; -fx-text-fill: #122A1E;"
                    + "-fx-font-size: 10px; -fx-font-weight: bold; -fx-background-radius: 10;"
                    + "-fx-min-width: 20; -fx-min-height: 20; -fx-alignment: CENTER;");
            topRow.getChildren().add(badge);
        }

        cell.getChildren().add(topRow);
        for (Map<String, String> ev : events.stream().limit(2).toList()) {
            String title = ev.getOrDefault("nama_kegiatan", "Kegiatan");
            Label eventLabel = new Label(title.length() > 18 ? title.substring(0, 17) + "..." : title);
            eventLabel.setMaxWidth(Double.MAX_VALUE);
            eventLabel.setStyle("-fx-background-color: rgba(212,168,67,0.22); -fx-text-fill: #1A3A2A;"
                    + "-fx-font-size: 9px; -fx-background-radius: 3; -fx-padding: 2 5;");
            cell.getChildren().add(eventLabel);
        }

        if (events.size() > 2) {
            Label more = new Label("+" + (events.size() - 2) + " kegiatan");
            more.setStyle("-fx-text-fill: #7A6A2A; -fx-font-size: 9px; -fx-font-weight: bold;");
            cell.getChildren().add(more);
        }

        return cell;
    }

    private String calendarCellStyle(String bg, String border) {
        return "-fx-background-color: " + bg + ";"
                + "-fx-background-radius: 4;"
                + "-fx-border-color: " + border + ";"
                + "-fx-border-radius: 4;"
                + "-fx-border-width: 1;"
                + "-fx-cursor: hand;";
    }

    private void tampilkanRingkasanBulan(Map<String, List<Map<String, String>>> eventsByDate) {
        int total = eventsByDate.values().stream().mapToInt(List::size).sum();
        lblCalendarSelectedDate.setText("Ringkasan "
                + currentCalendarMonth.format(FMT_MONTH_ID));
        lblCalendarSummary.setText(total == 0
                ? "Belum ada kegiatan pada bulan ini."
                : total + " kegiatan terjadwal pada " + eventsByDate.size() + " tanggal.");

        calendarEventList.getChildren().clear();
        List<Map<String, String>> allEvents = new ArrayList<>();
        eventsByDate.values().forEach(allEvents::addAll);
        if (allEvents.isEmpty()) {
            calendarEventList.getChildren().add(buildEmptyCalendarMessage("Tidak ada kegiatan bulan ini."));
            return;
        }
        allEvents.stream().limit(8).forEach(ev -> calendarEventList.getChildren().add(buildEventCard(ev)));
    }

    private void tampilkanDetailTanggal(LocalDate date, List<Map<String, String>> events) {
        lblCalendarSelectedDate.setText(date.format(FMT_DATE_ID));
        lblCalendarSummary.setText(events.isEmpty()
                ? "Tidak ada kegiatan gereja pada tanggal ini."
                : events.size() + " kegiatan terjadwal.");

        calendarEventList.getChildren().clear();
        if (events.isEmpty()) {
            calendarEventList.getChildren().add(buildEmptyCalendarMessage("Tanggal ini masih kosong."));
            return;
        }
        events.forEach(ev -> calendarEventList.getChildren().add(buildEventCard(ev)));
    }

    private List<Map<String, String>> getEventsForDate(LocalDate date) {
        return DatabaseHelper.getJadwalBulan(date.format(FMT_DATE_DB), date.format(FMT_DATE_DB))
                .getOrDefault(date.format(FMT_DATE_DB), List.of());
    }

    private Label buildEmptyCalendarMessage(String message) {
        Label label = new Label(message);
        label.setWrapText(true);
        label.setStyle("-fx-text-fill: #7A9A8A; -fx-font-size: 12px; -fx-padding: 8;");
        return label;
    }

    private VBox buildEventCard(Map<String, String> event) {
        VBox card = new VBox(4);
        card.setStyle("-fx-background-color: #F7FBF8; -fx-background-radius: 4;"
                + "-fx-border-color: #DCE8E0; -fx-border-radius: 4; -fx-padding: 10;");

        Label title = new Label(event.getOrDefault("nama_kegiatan", "Kegiatan"));
        title.setWrapText(true);
        title.setStyle("-fx-text-fill: #1A3A2A; -fx-font-size: 12px; -fx-font-weight: bold;");

        String time = event.getOrDefault("jam_mulai", "");
        String endTime = event.getOrDefault("jam_selesai", "");
        if (!time.isBlank() && !endTime.isBlank()) time += " - " + endTime;

        Label meta = new Label(String.join("  |  ",
                List.of(
                        event.getOrDefault("tanggal", "-"),
                        time.isBlank() ? "Jam belum diatur" : time,
                        event.getOrDefault("lokasi", "Lokasi belum diatur")
                )));
        meta.setWrapText(true);
        meta.setStyle("-fx-text-fill: #667; -fx-font-size: 10px;");

        Label category = new Label(event.getOrDefault("kategori", "UMUM"));
        category.setStyle("-fx-background-color: #D4A843; -fx-text-fill: #122A1E;"
                + "-fx-font-size: 9px; -fx-font-weight: bold; -fx-background-radius: 3; -fx-padding: 2 6;");

        card.getChildren().addAll(title, meta, category);
        return card;
    }

    // ══════════════════════════════════════════════════════════
    //  KELOLA ADMIN
    // ══════════════════════════════════════════════════════════

    private void setupAdminTable() {
        colAdminId.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().get("id")));
        colAdminUser.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().get("username")));
        colAdminNama.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().get("nama_lengkap")));
        addAksiColumn(colAdminAksi, "ADMIN");
    }

    private void muatTblAdmin() {
        tblAdmin.setItems(FXCollections.observableArrayList(DatabaseHelper.getAllAdmin()));
    }

    @FXML private void simpanAdmin() {
        String user = txtAdminUser.getText().trim();
        String pw   = txtAdminPw.getText();
        String pwK  = txtAdminPwKonfirm.getText();
        String nama = txtAdminNama.getText().trim();
        lblAdminFormMsg.setVisible(false); lblAdminFormMsg.setManaged(false);

        if (user.isEmpty() || nama.isEmpty()) {
            showFormMsg("Username dan nama lengkap wajib diisi."); return;
        }
        if (editingAdminId == null && pw.isEmpty()) {
            showFormMsg("Password wajib diisi untuk admin baru."); return;
        }
        if (!pw.isEmpty() && !pw.equals(pwK)) {
            showFormMsg("Password dan konfirmasi tidak cocok."); return;
        }
        if (editingAdminId != null) {
            DatabaseHelper.updateAdmin(editingAdminId, user, pw.isEmpty() ? null : pw, nama);
            editingAdminId = null;
        } else {
            if (DatabaseHelper.isUsernameExists(user)) {
                showFormMsg("Username sudah digunakan."); return;
            }
            DatabaseHelper.insertAdmin(user, pw, nama);
        }
        resetFormAdmin(); muatTblAdmin();
    }

    @FXML private void resetFormAdmin() {
        txtAdminUser.clear(); txtAdminPw.clear(); txtAdminPwKonfirm.clear(); txtAdminNama.clear();
        editingAdminId = null;
        lblAdminFormMsg.setVisible(false); lblAdminFormMsg.setManaged(false);
    }

    private void showFormMsg(String msg) {
        lblAdminFormMsg.setText(msg);
        lblAdminFormMsg.setVisible(true); lblAdminFormMsg.setManaged(true);
    }

    // ══════════════════════════════════════════════════════════
    //  LOGOUT
    // ══════════════════════════════════════════════════════════

    @FXML private void handleLogout() {
        try {
            Stage stage = (Stage) lblAdminName.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/gereja/chatbot/fxml/AdminLogin.fxml"));
            Scene scene = new Scene(loader.load(), 900, 600);
            scene.getStylesheets().add(Objects.requireNonNull(
                    getClass().getResource("/com/gereja/chatbot/css/styles.css")).toExternalForm());
            stage.setTitle("Faith Buddy – Login Admin");
            stage.setScene(scene); stage.centerOnScreen();
        } catch (IOException e) {
            showAlert("Gagal logout: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    // ══════════════════════════════════════════════════════════
    //  HELPER – Tombol Edit/Hapus di tabel
    // ══════════════════════════════════════════════════════════

    @SuppressWarnings("unchecked")
    private <T extends Map<String, String>> void addAksiColumn(TableColumn<T, Void> col, String tipe) {
        col.setCellFactory(c -> new TableCell<>() {
            private final Button btnEdit  = new Button("✏️");
            private final Button btnHapus = new Button("🗑️");
            private final HBox box = new HBox(6, btnEdit, btnHapus);
            {
                String s = "-fx-background-radius: 6; -fx-padding: 4 8; -fx-cursor: hand; -fx-font-size: 11px;";
                btnEdit.setStyle("-fx-background-color: #E8F5E0;" + s);
                btnHapus.setStyle("-fx-background-color: #FDE8E8;" + s);
                btnEdit.setOnAction(e -> handleEdit(tipe, (Map<String,String>) getTableView().getItems().get(getIndex())));
                btnHapus.setOnAction(e -> handleHapus(tipe, (Map<String,String>) getTableView().getItems().get(getIndex())));
            }
            @Override protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty); setGraphic(empty ? null : box);
            }
        });
    }

    private void handleEdit(String tipe, Map<String, String> row) {
        int id = Integer.parseInt(row.get("id"));
        switch (tipe) {
            case "INFO" -> {
                editingInfoId = id;
                txtInfoKunci.setText(row.get("kunci"));
                txtInfoNilai.setText(row.get("nilai"));
                showTabInfo();
            }
            case "PELAYANAN" -> {
                editingPelId = id;
                cmbKategori.setValue(row.get("kategori"));
                cmbTipePelayanan.setValue(row.get("tipe"));
                txtPelNamaItem.setText(row.get("nama"));
                txtPelBiaya.setText(row.getOrDefault("biaya", ""));
                txtPelKeterangan.setText(row.getOrDefault("keterangan", ""));
                showTabPelayanan();
            }
            case "KATAKUNCI" -> {
                editingKKId = id;
                txtKK.setText(row.get("kata_kunci"));
                txtKKPertanyaan.setText(row.getOrDefault("pertanyaan_asli", ""));
                txtKKJawaban.setText(row.getOrDefault("jawaban", ""));
                cmbKKKategori.setValue(row.getOrDefault("kategori", "UMUM"));
                showTabKataKunci();
            }
            case "AYAT" -> {
                editingAyatId = id;
                txtAyatReferensi.setText(row.get("referensi"));
                txtAyatIsi.setText(row.get("ayat"));
                txtAyatKeterangan.setText(row.getOrDefault("keterangan", ""));
                String tgl = row.getOrDefault("tanggal", "(rotasi)");
                txtAyatTanggal.setText("(rotasi)".equals(tgl) ? "" : tgl);
                chkAyatAktif.setSelected("Ya".equals(row.getOrDefault("aktif", "Ya")));
                showTabAyat();
            }
            case "ADMIN" -> {
                editingAdminId = id;
                txtAdminUser.setText(row.get("username"));
                txtAdminNama.setText(row.get("nama_lengkap"));
                txtAdminPw.clear(); txtAdminPwKonfirm.clear();
                showTabAdmin();
            }
        }
    }

    private void handleHapus(String tipe, Map<String, String> row) {
        String nama = switch (tipe) {
            case "INFO"      -> row.get("kunci");
            case "PELAYANAN" -> row.get("nama");
            case "KATAKUNCI" -> row.get("kata_kunci");
            case "AYAT"      -> row.get("referensi");
            case "ADMIN"     -> row.get("username");
            default          -> "item ini";
        };
        Alert konfirm = new Alert(Alert.AlertType.CONFIRMATION);
        konfirm.setTitle("Konfirmasi Hapus");
        konfirm.setHeaderText("Hapus \"" + nama + "\"?");
        konfirm.setContentText("Data yang dihapus tidak dapat dikembalikan.");
        konfirm.showAndWait().ifPresent(r -> {
            if (r == ButtonType.OK) {
                int id = Integer.parseInt(row.get("id"));
                switch (tipe) {
                    case "INFO"      -> DatabaseHelper.deleteInfoGereja(id);
                    case "PELAYANAN" -> DatabaseHelper.deletePelayanan(id, row.get("tipe"));
                    case "KATAKUNCI" -> DatabaseHelper.deleteKataKunci(id);
                    case "AYAT"      -> DatabaseHelper.deleteAyatHarian(id);
                    case "ADMIN"     -> {
                        if (row.get("username").equals(currentUsername)) {
                            showAlert("Tidak dapat menghapus akun yang sedang aktif.", Alert.AlertType.WARNING);
                            return;
                        }
                        DatabaseHelper.deleteAdmin(id);
                    }
                }
                muatSemuaData();
            }
        });
    }

    private void showAlert(String msg, Alert.AlertType type) {
        Alert alert = new Alert(type, msg, ButtonType.OK);
        alert.setHeaderText(null);
        alert.showAndWait();
    }
}
