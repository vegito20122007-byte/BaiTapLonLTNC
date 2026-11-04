package com.auction.client.controllers;

import com.auction.client.network.Message;
import com.auction.client.network.NetworkClient;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.HashMap;
import java.util.Map;

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
            // 1. Đóng gói dữ liệu đăng ký thành Message
            Map<String, String> regData = new HashMap<>();
            regData.put("username", username);
            regData.put("password", password);
            regData.put("role", roleCode);
            Message regReq = Message.request(Message.ACTION_REGISTER, regData);

            // 2. Gửi yêu cầu qua Socket TCP
            Message response = NetworkClient.getInstance().sendRequest(regReq);

            if (response != null && response.isSuccess()) {
                showSuccess(lblMessage, "Đăng ký tài khoản thành công! Đang chuyển về Đăng nhập...");
                switchToLogin(event);
            } else if (response != null && response.getErrorMessage() != null && response.getErrorMessage().contains("Lỗi mạng")) {
                // Server chưa chạy: mô phỏng đăng ký thành công để kiểm thử giao diện
                System.out.println("[RegisterController] Server offline (" + response.getErrorMessage() + "), mô phỏng thành công cho kiểm thử UI.");
                showSuccess(lblMessage, "Đăng ký thành công (Chế độ kiểm thử)! Đang chuyển về Đăng nhập...");
                switchToLogin(event);
            } else {
                showError(lblMessage, response != null ? response.getErrorMessage() : "Đăng ký tài khoản thất bại!");
            }

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
