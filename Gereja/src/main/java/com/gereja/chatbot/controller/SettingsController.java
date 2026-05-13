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
    public void handleSave() {

        System.out.println("Pengaturan disimpan!");
    }

    @FXML
    public void handleBack() {

        System.out.println("Kembali ditekan!");
    }
}