package com.gereja.chatbot.controller;

import javafx.fxml.FXML;
import javafx.scene.layout.VBox;

public class SettingsController {

    @FXML
    private VBox umumPane;

    @FXML
    private VBox notifPane;

    @FXML
    private VBox chatPane;

    @FXML
    private VBox akunPane;

    @FXML
    private VBox tentangPane;

    // ================= UMUM =================

    @FXML
    public void showUmum() {

        hideAll();

        umumPane.setVisible(true);
        umumPane.setManaged(true);
    }

    // ================= NOTIF =================

    @FXML
    public void showNotif() {

        hideAll();

        notifPane.setVisible(true);
        notifPane.setManaged(true);
    }

    // ================= CHAT =================

    @FXML
    public void showChat() {

        hideAll();

        chatPane.setVisible(true);
        chatPane.setManaged(true);
    }

    // ================= AKUN =================

    @FXML
    public void showAkun() {

        hideAll();

        akunPane.setVisible(true);
        akunPane.setManaged(true);
    }

    // ================= TENTANG =================

    @FXML
    public void showTentang() {

        hideAll();

        tentangPane.setVisible(true);
        tentangPane.setManaged(true);
    }

    // ================= HIDE ALL =================

    private void hideAll() {

        umumPane.setVisible(false);
        umumPane.setManaged(false);

        notifPane.setVisible(false);
        notifPane.setManaged(false);

        chatPane.setVisible(false);
        chatPane.setManaged(false);

        akunPane.setVisible(false);
        akunPane.setManaged(false);

        tentangPane.setVisible(false);
        tentangPane.setManaged(false);
    }

    // ================= BUTTON ACTION =================

    @FXML
    public void handleSave(javafx.event.ActionEvent event) {
        System.out.println("Pengaturan disimpan!");
        handleBack(event);
    }

    @FXML
    public void handleBack(javafx.event.ActionEvent event) {
        try {
            javafx.stage.Stage stage = (javafx.stage.Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                    getClass().getResource("/com/gereja/chatbot/fxml/ChurchChatbot.fxml")
            );
            javafx.scene.Scene scene = new javafx.scene.Scene(loader.load());
            stage.setScene(scene);
            stage.show();
        } catch (Exception e) {
            System.err.println("[SettingsController] Gagal kembali ke Chatbot: " + e.getMessage());
            e.printStackTrace();
            // Fallback to landing page if chatbot load fails
            try {
                javafx.stage.Stage stage = (javafx.stage.Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
                javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                        getClass().getResource("/com/gereja/chatbot/fxml/LandingPage.fxml")
                );
                javafx.scene.Scene scene = new javafx.scene.Scene(loader.load(), 500, 620);
                scene.getStylesheets().add(java.util.Objects.requireNonNull(
                        getClass().getResource("/com/gereja/chatbot/css/styles.css")).toExternalForm());
                stage.setScene(scene);
                stage.show();
            } catch (Exception ex) {
                System.err.println("[SettingsController] Fallback FATAL: " + ex.getMessage());
                ex.printStackTrace();
            }
        }
    }
}