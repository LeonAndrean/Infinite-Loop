package com.gereja.chatbot.app;

import com.gereja.chatbot.database.DatabaseHelper;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Objects;


public class MainApp extends Application {

    @Override
    public void start(Stage stage) throws IOException {
        // Inisialisasi database (buat tabel + isi data awal kalo kosong)
        DatabaseHelper.getConnection();

        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/com/gereja/chatbot/fxml/ChurchChatbot.fxml"));

        Scene scene = new Scene(loader.load(), 1100, 720);
        scene.getStylesheets().add(
                Objects.requireNonNull(
                        getClass().getResource("/com/gereja/chatbot/css/styles.css")
                ).toExternalForm()
        );

        stage.setTitle("Faith Buddy – Layanan Informasi Jemaat");
        stage.setScene(scene);
        stage.setMinWidth(900);
        stage.setMinHeight(600);
        stage.centerOnScreen();
        stage.show();
    }


    public static void main(String[] args) {
        launch(args);
    }
}
