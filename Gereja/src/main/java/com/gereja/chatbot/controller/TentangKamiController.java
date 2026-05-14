package com.gereja.chatbot.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;

import java.io.InputStream;

public class TentangKamiController {

    @FXML
    private ImageView logoImage;

    @FXML
    public void initialize() {
        try (InputStream logoStream = getClass().getResourceAsStream("/com/gereja/chatbot/logo/logo.png")) {
            if (logoStream == null) {
                return;
            }

            logoImage.setImage(new Image(logoStream));
        } catch (Exception e) {
            System.out.println("Logo gagal dimuat");
            e.printStackTrace();
        }
    }

    @FXML
    private void handleBack(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(
                    getClass().getResource("/com/gereja/chatbot/fxml/LandingPage.fxml")
            );

            Stage stage = (Stage) logoImage.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
