package com.gereja.chatbot.app;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.IOException;
import java.util.Objects;

/**
 * ╔══════════════════════════════════════════════════════════╗
 * ║        GerejaCare – Church Information Chatbot           ║
 * ║        Main Application Entry Point                      ║
 * ╚══════════════════════════════════════════════════════════╝
 */
public class MainApp extends Application {

    @Override
    public void start(Stage primaryStage) throws IOException {
        FXMLLoader loader = new FXMLLoader(
            getClass().getResource("/com/gereja/chatbot/fxml/ChurchChatbot.fxml")
        );

        Scene scene = new Scene(loader.load(), 1100, 720);

        // Load CSS
        scene.getStylesheets().add(
            Objects.requireNonNull(
                getClass().getResource("/com/gereja/chatbot/css/styles.css")
            ).toExternalForm()
        );

        primaryStage.setTitle("GerejaCare – Layanan Informasi Jemaat");
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(900);
        primaryStage.setMinHeight(600);
        primaryStage.show();

        // Center on screen
        primaryStage.centerOnScreen();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
