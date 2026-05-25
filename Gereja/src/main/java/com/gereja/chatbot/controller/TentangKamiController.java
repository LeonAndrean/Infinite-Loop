package com.gereja.chatbot.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Objects;

/**
 * TentangKamiController – Mengelola interaksi halaman Tentang Kami (About Us).
 */
public class TentangKamiController {

    @FXML
    public void handleBack(ActionEvent event) {
        try {
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/gereja/chatbot/fxml/ChurchChatbot.fxml")
            );

            Scene scene = new Scene(loader.load());
            stage.setScene(scene);
            stage.show();

        } catch (IOException e) {
            System.err.println("[TentangKamiController] Gagal kembali ke Chatbot: " + e.getMessage());
            e.printStackTrace();

            // Fallback ke Landing Page jika terjadi kegagalan
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
            } catch (Exception ex) {
                System.err.println("[TentangKamiController] Fallback FATAL: " + ex.getMessage());
                ex.printStackTrace();
            }
        }
    }
}
