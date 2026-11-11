package com.auction.client.controllers;

import com.auction.client.util.GsonConfig;
import com.google.gson.Gson;
import javafx.event.Event;
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
    protected Gson gson = GsonConfig.getGson();

    /**
     * Chuyển đổi màn hình hiển thị trong ứng dụng (nhận Event đa năng).
     */
    protected void switchToScene(Event event, String fxmlPath, String title) {
        switchSceneWithController(event, fxmlPath, title);
    }

    /**
     * Chuyển đổi màn hình và trả về Controller của màn hình đích để truyền dữ liệu.
     */
    protected <T> T switchSceneWithController(Event event, String fxmlPath, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setTitle(title);
            stage.setScene(new Scene(root));
            stage.show();
            return loader.getController();
        } catch (IOException e) {
            System.err.println(">> Lỗi khi tải màn hình: " + fxmlPath);
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Tự động unfocus khi click ra khoảng trống ngoài form.
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

    protected void showError(Label lbl, String message) {
        if (lbl != null) {
            lbl.setTextFill(Color.RED);
            lbl.setText(message);
        }
    }

    protected void showSuccess(Label lbl, String message) {
        if (lbl != null) {
            lbl.setTextFill(Color.GREEN);
            lbl.setText(message);
        }
    }

    protected void clearMessage(Label lbl) {
        if (lbl != null) {
            lbl.setText("");
        }
    }
}
