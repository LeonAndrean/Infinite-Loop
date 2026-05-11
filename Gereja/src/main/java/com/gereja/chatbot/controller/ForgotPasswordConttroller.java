package com.gereja.chatbot.controller;

import com.gereja.chatbot.database.DatabaseHelper;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Objects;

public class ForgotPasswordConttroller {

    @FXML private TextField txtUsername;
    @FXML private PasswordField txtNewPassword;
    @FXML private PasswordField txtConfirmPassword;
    @FXML private Label lblInfo;


    @FXML
    private void handleReset() {
        String username = txtUsername.getText().trim();
        String newPass = txtNewPassword.getText();
        String confirm = txtConfirmPassword.getText();

        if (username.isEmpty() || newPass.isEmpty() || confirm.isEmpty()) {
            showInfo("Semua field wajib diisi!");
            return;
        }

        if (!newPass.equals(confirm)) {
            showInfo("Password tidak sama!");
            return;
        }

        boolean success = DatabaseHelper.resetPassword(username, newPass);

        if (success) {
            showInfo("Password berhasil direset! Silakan login.");
        } else {
            showInfo("Username tidak ditemukan!");
        }
    }

    @FXML
    private void handleBack() {
        try {
            Stage stage = (Stage) txtUsername.getScene().getWindow();

            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/gereja/chatbot/fxml/AdminLogin.fxml"));

            Scene scene = new Scene(loader.load(), 900, 600);
            scene.getStylesheets().add(
                    Objects.requireNonNull(
                            getClass().getResource("/com/gereja/chatbot/css/styles.css")
                    ).toExternalForm());

            stage.setScene(scene);

        } catch (IOException e) {
            showInfo("Gagal kembali ke login");
        }
    }

    private void showInfo(String msg) {
        lblInfo.setText(msg);
    }
}