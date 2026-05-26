package com.gereja.chatbot.app;

import com.gereja.chatbot.database.DatabaseHelper;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Objects;

import static com.gereja.chatbot.database.DatabaseHelper.insertKataKunci;

/**
 * MainApp – Entry point aplikasi Faith Buddy.
 *
 * PERBAIKAN:
 *  - Layar pertama yang tampil adalah LandingPage (bukan langsung ChurchChatbot).
 *  - Error handling di start() agar aplikasi tidak diam tanpa pesan jika ada error.
 */
public class MainApp extends Application {

    @Override
    public void start(Stage stage) {
        try {
            // Inisialisasi database (buat tabel + isi data awal jika kosong)
            DatabaseHelper.getConnection();
        } catch (Exception e) {
            System.err.println("[MainApp] Peringatan: inisialisasi DB bermasalah – " + e.getMessage());
            // Lanjut saja; DatabaseHelper sudah handle fallback internal
        }

        try {
            // ── Tampilkan Landing Page terlebih dahulu ──────────
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/gereja/chatbot/fxml/LandingPage.fxml"));

            Scene scene = new Scene(loader.load(), 500, 620);

            // Tambahkan stylesheet jika tersedia
            try {
                String css = Objects.requireNonNull(
                        getClass().getResource("/com/gereja/chatbot/css/styles.css")
                ).toExternalForm();
                scene.getStylesheets().add(css);
            } catch (Exception cssErr) {
                System.err.println("[MainApp] CSS tidak ditemukan, lanjut tanpa stylesheet.");
            }

            stage.setTitle("Faith Buddy – Layanan Informasi Jemaat");
            stage.setScene(scene);
            stage.setMinWidth(900);
            stage.setMinHeight(600);
            stage.centerOnScreen();
            stage.show();

        } catch (IOException e) {
            System.err.println("[MainApp] Gagal memuat LandingPage.fxml: " + e.getMessage());
            e.printStackTrace();
            // Jika landing page gagal, coba langsung chatbot sebagai fallback
            try {
                FXMLLoader fallback = new FXMLLoader(
                        getClass().getResource("/com/gereja/chatbot/fxml/ChurchChatbot.fxml"));
                Scene scene = new Scene(fallback.load(), 1100, 720);
                stage.setTitle("Faith Buddy – Layanan Informasi Jemaat");
                stage.setScene(scene);
                stage.setMinWidth(900);
                stage.setMinHeight(600);
                stage.centerOnScreen();
                stage.show();
            } catch (IOException fallbackErr) {
                System.err.println("[MainApp] FATAL: Gagal memuat ChurchChatbot.fxml: " + fallbackErr.getMessage());
                fallbackErr.printStackTrace();
            }
        } catch (Exception e) {
            System.err.println("[MainApp] Error tidak terduga saat start: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }

}

