module com.gereja.chatbot {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;

    opens com.gereja.chatbot.app        to javafx.graphics;
    opens com.gereja.chatbot.controller to javafx.fxml;
    opens com.gereja.chatbot.model      to javafx.base;
    opens com.gereja.chatbot.service    to javafx.base;

    exports com.gereja.chatbot.app;
    exports com.gereja.chatbot.controller;
    exports com.gereja.chatbot.model;
    exports com.gereja.chatbot.service;
}
