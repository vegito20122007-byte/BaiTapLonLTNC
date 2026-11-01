package com.auction.client.controllers;

import javafx.event.Event;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * Controller cơ sở chứa các tiện ích dùng chung cho toàn bộ giao diện JavaFX.
 */
public abstract class BaseController {

    /**
     * Chuyển đổi màn hình hiển thị trong ứng dụng.
     */
    protected void switchToScene(Event event, String fxmlPath, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setTitle(title);
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            System.err.println(">> Lỗi khi tải màn hình: " + fxmlPath);
            e.printStackTrace();
        }
    }
}