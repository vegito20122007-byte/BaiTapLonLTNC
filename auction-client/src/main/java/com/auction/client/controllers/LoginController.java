package com.auction.client.controllers;

import com.auction.client.session.SessionManager;
import com.auction.model.Admin;
import com.auction.model.Bidder;
import com.auction.model.Seller;
import com.auction.model.User;
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
            // Giả lập phiên đăng nhập theo phân quyền phục vụ kiểm thử luồng UI độc lập
            User user;
            if (username.toLowerCase().contains("admin")) {
                user = new Admin(1, username, password);
            } else if (username.toLowerCase().contains("bidder")) {
                user = new Bidder(1, username, password);
            } else {
                user = new Seller(1, username, password);
            }

            // Lưu User vào SessionManager
            SessionManager.getInstance().setCurrentUser(user);
            System.out.println("[LoginController] Đăng nhập thành công với vai trò: " + user.getClass().getSimpleName());

            // Điều hướng theo phân quyền
            if (user instanceof Admin) {
                switchToScene(event, "/views/AdminView.fxml", "Hệ thống Đấu giá - Kênh Quản trị viên (Admin)");
            } else if (username.toLowerCase().contains("history")) {
                switchToScene(event, "/views/HistoryView.fxml", "Hệ thống Đấu giá - Lịch sử giao dịch & đấu giá");
            } else {
                switchToScene(event, "/views/SellerView.fxml", "Hệ thống Đấu giá - Kênh Người bán (Seller)");
            }

        } catch (Exception e) {
            showError(lblError, "Lỗi điều hướng giao diện: " + e.getMessage());
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
