package com.auction.client;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class ClientApp extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/MainView.fxml"));
        Parent root = loader.load();

        primaryStage.setTitle("Hệ thống Đấu giá Trực tuyến - Trang chủ");
        primaryStage.setScene(new Scene(root, 1050, 720));
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}