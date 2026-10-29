package com.auction.client.controllers;

import com.google.gson.Gson;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * Lớp cha trừu tượng (Abstract Base Class) cho toàn bộ các Controller phía Client.
 * Áp dụng Tính Kế Thừa (Inheritance) và Đóng Gói (Encapsulation) trong OOP để:
 * - Dùng chung đối tượng Gson cho toàn bộ các màn hình.
 * - Tái sử dụng logic chuyển màn hình (Scene Navigation).
 * - Quản lý thông báo lỗi, trạng thái thành công đồng bộ.
 * - Tự động xóa con trỏ nhấp nháy khi click chuột ra khoảng trống bên ngoài.
 */
public abstract class BaseController {

    // Đối tượng Gson dùng chung cho tất cả các Controller con
    protected Gson gson = new Gson();

    /**
     * Chuyển đổi màn hình (Scene) dùng chung cho toàn bộ ứng dụng JavaFX.
     *
     * @param event    sự kiện ActionEvent kích hoạt từ nút bấm
     * @param fxmlPath đường dẫn file FXML trong resources (ví dụ: /views/LoginView.fxml)
     * @param title    tiêu đề cửa sổ hiển thị
     */
    protected void switchToScene(ActionEvent event, String fxmlPath, String title) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxmlPath));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setTitle(title);
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            System.err.println("Lỗi khi chuyển đổi sang màn hình: " + fxmlPath);
            e.printStackTrace();
        }
    }

    /**
     * Xóa con trỏ nhấp nháy trong TextField khi người dùng click chuột ra khoảng trống bên ngoài.
     *
     * @param rootPane container gốc ngoài cùng (StackPane)
     * @param cardBox  khung chứa form ở giữa (VBox)
     */
    protected void setupFocusClear(Pane rootPane, Pane cardBox) {
        if (rootPane != null) {
            rootPane.setPickOnBounds(true);
            rootPane.setOnMouseClicked(event -> rootPane.requestFocus());
        }
        if (cardBox != null) {
            cardBox.setPickOnBounds(true);
            cardBox.setOnMouseClicked(event -> rootPane.requestFocus());
        }
    }

    /**
     * Hiển thị thông báo lỗi màu đỏ lên nhãn chỉ định.
     */
    protected void showError(Label lbl, String message) {
        if (lbl != null) {
            lbl.setTextFill(Color.RED);
            lbl.setText(message);
        }
    }

    /**
     * Hiển thị thông báo thành công màu xanh lá lên nhãn chỉ định.
     */
    protected void showSuccess(Label lbl, String message) {
        if (lbl != null) {
            lbl.setTextFill(Color.GREEN);
            lbl.setText(message);
        }
    }

    /**
     * Xóa sạch nội dung thông báo.
     */
    protected void clearMessage(Label lbl) {
        if (lbl != null) {
            lbl.setText("");
        }
    }
}
