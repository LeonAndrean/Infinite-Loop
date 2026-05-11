package com.gereja.chatbot.controller;

import com.gereja.chatbot.database.DatabaseHelper;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
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
    @FXML private Button btnNavInfo, btnNavPelayanan, btnNavKataKunci, btnNavAyat, btnNavAdmin;

    // ─── Stats ───
    @FXML private Label lblTotalChat, lblTotalKataKunci, lblTotalAyat;

    // ─── Tab Panels ───
    @FXML private VBox tabInfo, tabPelayanan, tabKataKunci, tabAyat, tabAdmin;

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
    @FXML public void showTabAdmin()      { switchTab(tabAdmin,      btnNavAdmin); }

    private void switchTab(VBox active, Button activeBtn) {
        for (VBox v : List.of(tabInfo, tabPelayanan, tabKataKunci, tabAyat, tabAdmin)) {
            v.setVisible(false); v.setManaged(false);
        }
        active.setVisible(true); active.setManaged(true);

        String activeStyle = "-fx-background-color: #D4A843; -fx-text-fill: #122A1E; " +
                "-fx-background-radius: 8; -fx-padding: 10 14; -fx-alignment: CENTER_LEFT; " +
                "-fx-cursor: hand; -fx-font-size: 13px; -fx-font-weight: bold;";
        String inactiveStyle = "-fx-background-color: transparent; -fx-text-fill: #C8DDD0; " +
                "-fx-background-radius: 8; -fx-padding: 10 14; -fx-alignment: CENTER_LEFT; " +
                "-fx-cursor: hand; -fx-font-size: 13px;";
        for (Button b : List.of(btnNavInfo, btnNavPelayanan, btnNavKataKunci, btnNavAyat, btnNavAdmin))
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
