package com.auction.client;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.net.URL;

/**
 * Lớp khởi chạy ứng dụng JavaFX chính.
 */
public class ClientApp extends Application {

    @Override
    public void start(Stage primaryStage) {
        try {
            // Nạp file giao diện FXML từ thư mục resources/views/
            URL fxmlLocation = getClass().getResource("/views/LoginView.fxml");
            if (fxmlLocation == null) {
                System.err.println("LỖI: Không tìm thấy file /views/LoginView.fxml trong resources!");
                return;
            }

            Parent root = FXMLLoader.load(fxmlLocation);

            // Thiết lập cửa sổ hiển thị (Stage)
            primaryStage.setTitle("Hệ thống Đấu giá Trực tuyến - Đăng nhập");
            primaryStage.setScene(new Scene(root, 650, 550));

            // Đặt kích thước tối thiểu để không bị vỡ giao diện khi người dùng co nhỏ cửa sổ
            primaryStage.setMinWidth(480);
            primaryStage.setMinHeight(520);

            // Hiển thị cửa sổ
            primaryStage.show();

        } catch (Exception e) {
            System.err.println("Lỗi khi khởi động giao diện JavaFX:");
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
