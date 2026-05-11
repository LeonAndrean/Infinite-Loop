package com.gereja.chatbot.controller;

import com.gereja.chatbot.database.DatabaseHelper;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Objects;

/**
 * AdminLoginController – mengelola halaman login panel admin.
 * Validasi username + password hash SHA-256 dari tabel admin_users.
 */
public class AdminLoginController {

    @FXML private TextField     txtUsername;
    @FXML private PasswordField txtPassword;
    @FXML private Label         lblError;
    @FXML private Button        btnLogin;

    @FXML
    public void initialize() {
        // Enter key pada password langsung trigger login
        txtPassword.setOnAction(e -> handleLogin());
    }

    @FXML
    private void handleLogin() {
        String username = txtUsername.getText().trim();
        String password = txtPassword.getText();

        if (username.isEmpty() || password.isEmpty()) {
            showError("Username dan password tidak boleh kosong.");
            return;
        }

        String adminNama = DatabaseHelper.validateAdmin(username, password);
        if (adminNama != null) {
            bukaAdminDashboard(adminNama, username);
        } else {
            showError("Username atau password salah. Silakan coba lagi.");
            txtPassword.clear();
        }
    }

    @FXML
    private void handleKembali() {
        try {
            Stage stage = (Stage) txtUsername.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/gereja/chatbot/fxml/ChurchChatbot.fxml"));
            Scene scene = new Scene(loader.load(), 1100, 720);
            scene.getStylesheets().add(
                    Objects.requireNonNull(
                            getClass().getResource("/com/gereja/chatbot/css/styles.css")
                    ).toExternalForm());
            stage.setScene(scene);
        } catch (IOException e) {
            showError("Gagal kembali ke chatbot: " + e.getMessage());
        }
    }

    private void bukaAdminDashboard(String namaAdmin, String username) {
        try {
            Stage stage = (Stage) txtUsername.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/gereja/chatbot/fxml/AdminDashboard.fxml"));
            Scene scene = new Scene(loader.load(), 1200, 780);
            scene.getStylesheets().add(
                    Objects.requireNonNull(
                            getClass().getResource("/com/gereja/chatbot/css/styles.css")
                    ).toExternalForm());

            // Kirim info admin ke dashboard controller
            AdminDashboardController ctrl = loader.getController();
            ctrl.setAdminInfo(namaAdmin, username);

            stage.setTitle("Faith Buddy – Panel Admin");
            stage.setScene(scene);
            stage.centerOnScreen();
        } catch (IOException e) {
            showError("Gagal membuka dashboard: " + e.getMessage());
        }
    }

    private void showError(String msg) {
        lblError.setText(msg);
        lblError.setVisible(true);
        lblError.setManaged(true);
    }
    @FXML
    private void handleForgotPassword() {
        try {
            Stage stage = (Stage) txtUsername.getScene().getWindow();

            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/gereja/chatbot/fxml/ForgotPassword.fxml"));

            Scene scene = new Scene(loader.load(), 900, 600);
            scene.getStylesheets().add(
                    Objects.requireNonNull(
                            getClass().getResource("/com/gereja/chatbot/css/styles.css")
                    ).toExternalForm());

            stage.setScene(scene);

        } catch (IOException e) {
            showError("Gagal membuka halaman reset password.");
        }
    }
}
