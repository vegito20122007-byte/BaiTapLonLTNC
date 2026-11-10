package com.auction.client.controllers;

import com.auction.client.network.Message;
import com.auction.client.network.NetworkClient;
import com.auction.client.session.SessionManager;
import com.auction.client.util.ValidationUtils;
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

import java.util.HashMap;
import java.util.Map;

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

        // 1. Kiểm tra validation không để trống
        if (username.isEmpty() || password.isEmpty()) {
            showError(lblError, "Vui lòng nhập đầy đủ tên đăng nhập và mật khẩu!");
            return;
        }

        // 2. Kiểm tra độ dài tối thiểu của username (ít nhất 3 ký tự)
        if (username.length() < ValidationUtils.MIN_USERNAME_LENGTH) {
            showError(lblError, "Tên đăng nhập phải chứa ít nhất " + ValidationUtils.MIN_USERNAME_LENGTH + " ký tự!");
            return;
        }

        // 3. Kiểm tra ký tự không hợp lệ (không chứa khoảng trắng hoặc ký tự đặc biệt)
        if (!ValidationUtils.isValidUsername(username)) {
            showError(lblError, "Tên đăng nhập không được chứa khoảng trắng hoặc ký tự đặc biệt!");
            return;
        }

        clearMessage(lblError);
        System.out.println("[LoginController] Đang xử lý đăng nhập cho: " + username);

        try {
            // 1. Tạo gói tin yêu cầu LOGIN
            Map<String, String> credentials = new HashMap<>();
            credentials.put("username", username);
            credentials.put("password", password);
            Message loginReq = Message.request(Message.ACTION_LOGIN, credentials);

            // 2. Gửi yêu cầu qua Socket TCP
            Message response = NetworkClient.getInstance().sendRequest(loginReq);

            User user = null;
            if (response != null && response.isSuccess()) {
                // Server thật phản hồi thành công kèm thông tin User
                user = response.parsePayload(User.class);
            }

            // 3. Nếu chưa có server thật đang chạy (hoặc môi trường test), fallback linh hoạt để thử nghiệm UI
            if (user == null) {
                System.out.println("[LoginController] Server chưa phản hồi đối tượng User hợp lệ. " +
                        "Tự động kích hoạt phiên đăng nhập tương ứng để phục vụ kiểm thử giao diện.");

                if (username.toLowerCase().contains("admin")) {
                    user = new Admin(1, username, password);
                } else if (username.toLowerCase().contains("bidder")) {
                    user = new Bidder(1, username, password);
                } else {
                    user = new Seller(1, username, password);
                }
            }

            // 4. Lưu User vào SessionManager
            SessionManager.getInstance().setCurrentUser(user);
            System.out.println("[LoginController] Đăng nhập thành công với vai trò: " + user.getClass().getSimpleName());

            // 5. Điều hướng theo phân quyền
            if (user instanceof Admin) {
                switchToScene(event, "/views/AdminView.fxml", "Hệ thống Đấu giá - Kênh Quản trị viên (Admin)");
            } else if (username.toLowerCase().contains("history")) {
                switchToScene(event, "/views/HistoryView.fxml", "Hệ thống Đấu giá - Lịch sử giao dịch & đấu giá");
            } else if (user instanceof Bidder) {
                switchToScene(event, "/views/MainView.fxml", "Hệ thống Đấu giá Trực tuyến - Trang chủ");
            } else {
                switchToScene(event, "/views/SellerView.fxml", "Hệ thống Đấu giá - Kênh Người bán (Seller)");
            }

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
         switchToScene(event, "/views/MainView.fxml", "Hệ thống Đấu giá Trực tuyến - Trang chủ");
    }
}
