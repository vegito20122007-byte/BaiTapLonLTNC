package com.auction.client.controllers;

import com.auction.client.session.SessionManager;
import com.auction.model.Bidder;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * Controller điều khiển giao diện Đăng nhập (LoginView.fxml).
 * Kế thừa BaseController để tái sử dụng toàn bộ hàm chuyển Scene, xử lý lỗi và quản lý Focus.
 */
public class LoginController extends BaseController {

    // ==========================================
    // 1. ÁNH XẠ CÁC CONTROL TỪ FXML (fx:id)
    // ==========================================
    @FXML
    private StackPane rootPane;

    @FXML
    private VBox cardBox;

    @FXML
    private TextField txtUsername;

    @FXML
    private PasswordField txtPassword;

    @FXML
    private Label lblError;

    /**
     * Vòng đời khởi tạo giao diện.
     */
    @FXML
    public void initialize() {
        clearMessage(lblError);
        setupFocusClear(rootPane, cardBox);
    }

    // ==========================================
    // 2. XỬ LÝ SỰ KIỆN NÚT BẤM (onAction)
    // ==========================================

    /**
     * Xử lý đăng nhập khi người dùng nhấn nút "Đăng nhập" hoặc phím Enter.
     *
     * @param event sự kiện ActionEvent
     */
    @FXML
    public void handleLogin(ActionEvent event) {
        String username = txtUsername.getText().trim();
        String password = txtPassword.getText().trim();

        // 1. Kiểm tra validation
        if (username.isEmpty() || password.isEmpty()) {
            showError(lblError, "Vui lòng nhập đầy đủ tên đăng nhập và mật khẩu!");
            return;
        }

        clearMessage(lblError);
        System.out.println("[LoginController] Đang xử lý đăng nhập cho: " + username);

        try {
            // =========================================================================
            // TODO: GỬI REQUEST LOGIN QUA NETWORKCLIENT SANG SERVER
            // =========================================================================

            // GIẢ LẬP: Lưu người dùng vào SessionManager
            SessionManager.getInstance().setCurrentUser(new Bidder(1, username, password));
            System.out.println("[LoginController] Đăng nhập thành công! Đã lưu Session.");
            showSuccess(lblError, "Đăng nhập thành công!");

            // Sau này chuyển sang màn hình chính:
            // switchToMain(event);

        } catch (Exception e) {
            showError(lblError, "Lỗi kết nối Server: " + e.getMessage());
        }
    }

    /**
     * Chuyển sang màn hình Đăng ký tài khoản (RegisterView.fxml).
     *
     * @param event sự kiện ActionEvent
     */
    @FXML
    public void switchToSignUp(ActionEvent event) {
        System.out.println("[LoginController] Chuyển hướng sang màn hình RegisterView.fxml");
        switchToScene(event, "/views/RegisterView.fxml", "Hệ thống Đấu giá Trực tuyến - Đăng ký");
    }

    /**
     * Chuyển sang màn hình chính sau khi đăng nhập thành công.
     *
     * @param event sự kiện ActionEvent
     */
    public void switchToMain(ActionEvent event) {
        System.out.println("[LoginController] Chuyển hướng sang màn hình chính (MainView)");
        // switchToScene(event, "/views/MainView.fxml", "Hệ thống Đấu giá Trực tuyến - Trang chủ");
    }
}
