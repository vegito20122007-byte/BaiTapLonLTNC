package com.auction.client.controllers;

import com.auction.client.network.Message;
import com.auction.client.network.NetworkClient;
import com.auction.client.util.ValidationUtils;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
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
 * Hỗ trợ giao tiếp mạng bất đồng bộ (Non-blocking UI) đảm bảo giao diện phản hồi mượt mà.
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
    private Button btnRegister;

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
        // Mặc định không chọn trước vai trò, bắt buộc người dùng chủ động chọn
        cbRole.setValue(null);

        clearMessage(lblMessage);
        setupFocusClear(rootPane, cardBox);
    }

    // ==========================================
    // 2. XỬ LÝ SỰ KIỆN NÚT BẤM (onAction)
    // ==========================================

    /**
     * Xử lý đăng ký tài khoản mới khi nhấn nút "Đăng ký tài khoản".
     * Sử dụng luồng bất đồng bộ (Non-blocking) để giao diện không bị giật lag.
     *
     * @param event sự kiện ActionEvent
     */
    @FXML
    public void handleRegister(ActionEvent event) {
        String username = txtUsername.getText().trim();
        String password = txtPassword.getText().trim();
        String confirmPassword = txtConfirmPassword.getText().trim();
        String selectedRole = cbRole.getValue();

        // 1. Kiểm tra rỗng các trường bắt buộc
        if (username.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            showError(lblMessage, "Vui lòng nhập đầy đủ tất cả các trường thông tin!");
            return;
        }

        // 2. Kiểm tra định dạng username (độ dài >= 3, không chứa khoảng trắng hay ký tự đặc biệt)
        if (username.length() < ValidationUtils.MIN_USERNAME_LENGTH) {
            showError(lblMessage, "Tên đăng nhập phải chứa ít nhất " + ValidationUtils.MIN_USERNAME_LENGTH + " ký tự!");
            return;
        }

        if (!ValidationUtils.isValidUsername(username)) {
            showError(lblMessage, "Tên đăng nhập không được chứa khoảng trắng hoặc ký tự đặc biệt!");
            return;
        }

        // 3. Bắt buộc người dùng chủ động chọn vai trò
        if (!ValidationUtils.isValidRole(selectedRole)) {
            showError(lblMessage, "Vui lòng chủ động chọn vai trò tài khoản (Bidder hoặc Seller)!");
            return;
        }

        // 4. Kiểm tra độ dài mật khẩu (ít nhất 6 ký tự)
        if (password.length() < ValidationUtils.MIN_PASSWORD_LENGTH) {
            showError(lblMessage, "Mật khẩu phải chứa ít nhất " + ValidationUtils.MIN_PASSWORD_LENGTH + " ký tự!");
            return;
        }

        // 5. Kiểm tra mật khẩu xác nhận
        if (!password.equals(confirmPassword)) {
            showError(lblMessage, "Mật khẩu xác nhận không khớp!");
            return;
        }

        // 6. Lấy mã vai trò chuẩn (BIDDER hoặc SELLER)
        String roleCode = selectedRole.contains("SELLER") ? "SELLER" : "BIDDER";

        clearMessage(lblMessage);
        System.out.println("[RegisterController] Tiến hành đăng ký tài khoản: " + username + " (" + roleCode + ")");

        // Kích hoạt trạng thái đang tải (vô hiệu hóa các nút và ô nhập để chống spam click)
        setLoading(true);

        try {
            // 1. Đóng gói dữ liệu đăng ký thành Message
            Map<String, String> regData = new HashMap<>();
            regData.put("username", username);
            regData.put("password", password);
            regData.put("role", roleCode);
            Message regReq = Message.request(Message.ACTION_REGISTER, regData);

            // 2. Gửi yêu cầu qua Socket TCP bất đồng bộ (Non-blocking background thread)
            NetworkClient.getInstance().sendRequestAsync(regReq)
                    .thenAcceptAsync(response -> {
                        setLoading(false);
                        processRegisterResponse(event, response, username, password, roleCode);
                    }, Platform::runLater)
                    .exceptionallyAsync(throwable -> {
                        setLoading(false);
                        showError(lblMessage, "Lỗi kết nối khi đăng ký: " + throwable.getMessage());
                        return null;
                    }, Platform::runLater);

        } catch (Exception e) {
            setLoading(false);
            showError(lblMessage, "Lỗi khởi tạo yêu cầu: " + e.getMessage());
        }
    }

    /**
     * Xử lý gói tin phản hồi đăng ký từ Server trên luồng JavaFX UI.
     * Lưu trữ trực tiếp vào CSDL MySQL (không dùng mock offline).
     */
    private void processRegisterResponse(ActionEvent event, Message response, String username, String password, String roleCode) {
        if (response != null && response.isSuccess()) {
            showSuccess(lblMessage, "Đăng ký tài khoản thành công vào CSDL! Đang chuyển về Đăng nhập...");
            switchToLogin(event);
        } else {
            String errMsg = (response != null && response.getErrorMessage() != null)
                    ? response.getErrorMessage()
                    : "Không thể kết nối tới máy chủ (localhost:8888). Vui lòng thử lại!";
            showError(lblMessage, errMsg);
        }
    }

    /**
     * Bật/tắt trạng thái xử lý giao diện (Loading state).
     *
     * @param loading true nếu đang xử lý mạng, false nếu đã xong
     */
    private void setLoading(boolean loading) {
        if (btnRegister != null) {
            btnRegister.setDisable(loading);
            btnRegister.setText(loading ? "Đang xử lý..." : "Đăng ký tài khoản");
        }
        if (txtUsername != null) txtUsername.setDisable(loading);
        if (txtPassword != null) txtPassword.setDisable(loading);
        if (txtConfirmPassword != null) txtConfirmPassword.setDisable(loading);
        if (cbRole != null) cbRole.setDisable(loading);
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
