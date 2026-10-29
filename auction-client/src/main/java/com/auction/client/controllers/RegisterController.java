package com.auction.client.controllers;

import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * Controller điều khiển giao diện Đăng ký (RegisterView.fxml).
 * Kế thừa BaseController để tái sử dụng toàn bộ hàm chuyển Scene, xử lý lỗi và quản lý Focus.
 */
public class RegisterController extends BaseController {

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
    private PasswordField txtConfirmPassword;

    @FXML
    private ComboBox<String> cbRole;

    @FXML
    private Label lblMessage;

    /**
     * Vòng đời khởi tạo giao diện.
     */
    @FXML
    public void initialize() {
        // Nạp danh sách vai trò cho phép người dùng đăng ký
        cbRole.setItems(FXCollections.observableArrayList(
                "Người đấu giá (BIDDER)",
                "Người bán hàng (SELLER)"
        ));
        cbRole.getSelectionModel().selectFirst();

        clearMessage(lblMessage);
        setupFocusClear(rootPane, cardBox);
    }

    // ==========================================
    // 2. XỬ LÝ SỰ KIỆN NÚT BẤM (onAction)
    // ==========================================

    /**
     * Xử lý đăng ký tài khoản mới khi nhấn nút "Đăng ký tài khoản".
     *
     * @param event sự kiện ActionEvent
     */
    @FXML
    public void handleRegister(ActionEvent event) {
        String username = txtUsername.getText().trim();
        String password = txtPassword.getText().trim();
        String confirmPassword = txtConfirmPassword.getText().trim();
        String selectedRole = cbRole.getValue();

        // 1. Kiểm tra rỗng
        if (username.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            showError(lblMessage, "Vui lòng nhập đầy đủ tất cả các trường thông tin!");
            return;
        }

        // 2. Kiểm tra độ dài mật khẩu
        if (password.length() < 6) {
            showError(lblMessage, "Mật khẩu phải chứa ít nhất 6 ký tự!");
            return;
        }

        // 3. Kiểm tra mật khẩu xác nhận
        if (!password.equals(confirmPassword)) {
            showError(lblMessage, "Mật khẩu xác nhận không khớp!");
            return;
        }

        // 4. Lấy mã vai trò chuẩn (BIDDER hoặc SELLER)
        String roleCode = selectedRole != null && selectedRole.contains("SELLER") ? "SELLER" : "BIDDER";

        System.out.println("[RegisterController] Tiến hành đăng ký tài khoản: " + username + " (" + roleCode + ")");

        try {
            // =========================================================================
            // TODO: GỬI REQUEST REGISTER SANG SERVER QUA NETWORKCLIENT
            // =========================================================================

            // GIẢ LẬP: Đăng ký thành công -> hiển thị thông báo và chuyển về Đăng nhập
            showSuccess(lblMessage, "Đăng ký tài khoản thành công! Đang chuyển về Đăng nhập...");
            switchToLogin(event);

        } catch (Exception e) {
            showError(lblMessage, "Lỗi kết nối khi đăng ký: " + e.getMessage());
        }
    }

    /**
     * Bắt sự kiện chuyển về màn hình Đăng nhập khi bấm "Đăng nhập ngay".
     *
     * @param event sự kiện ActionEvent
     */
    @FXML
    public void switchToLogin(ActionEvent event) {
        System.out.println("[RegisterController] Chuyển về màn hình LoginView.fxml");
        switchToScene(event, "/views/LoginView.fxml", "Hệ thống Đấu giá Trực tuyến - Đăng nhập");
    }
}
