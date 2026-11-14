package com.auction.client.controllers;

import com.auction.client.util.GsonConfig;
import com.auction.exception.AuctionClosedException;
import com.auction.exception.AuctionException;
import com.auction.exception.AuctionNotFoundException;
import com.auction.exception.AuthenticationException;
import com.auction.exception.ConnectionException;
import com.auction.exception.DataValidationException;
import com.auction.exception.InsufficientBalanceException;
import com.auction.exception.InvalidBidException;
import com.auction.exception.UnauthorizedActionException;
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

    /**
     * Trả về tiêu đề thông báo thân thiện tương ứng với từng loại {@link AuctionException}.
     */
    protected String resolveExceptionTitle(AuctionException ex) {
        if (ex instanceof ConnectionException) {
            return "Lỗi kết nối máy chủ";
        }
        if (ex instanceof AuthenticationException) {
            return "Lỗi xác thực tài khoản";
        }
        if (ex instanceof UnauthorizedActionException) {
            return "Không có quyền thao tác";
        }
        if (ex instanceof AuctionClosedException) {
            return "Phiên đấu giá đã đóng";
        }
        if (ex instanceof AuctionNotFoundException) {
            return "Không tìm thấy phiên đấu giá";
        }
        if (ex instanceof InvalidBidException) {
            return "Giá đặt không hợp lệ";
        }
        if (ex instanceof InsufficientBalanceException) {
            return "Số dư không đủ";
        }
        if (ex instanceof DataValidationException) {
            return "Dữ liệu không hợp lệ";
        }
        return "Lỗi hệ thống đấu giá";
    }
}
