package com.auction.client.controllers;

import com.auction.client.network.Message;
import com.auction.client.network.NetworkClient;
import com.auction.client.session.SessionManager;
import com.auction.client.util.ValidationUtils;
import com.auction.exception.AuctionException;
import com.auction.exception.DataValidationException;
import com.auction.model.Admin;
import com.auction.model.Bidder;
import com.auction.model.Seller;
import com.auction.model.User;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
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
 * Hỗ trợ giao tiếp mạng bất đồng bộ (Non-blocking UI) đảm bảo ứng dụng không bị đơ giật.
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
    private Button btnLogin;

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
     * Sử dụng luồng bất đồng bộ (Non-blocking) để giao diện mượt mà.
     *
     * @param event sự kiện ActionEvent
     */
    @FXML
    public void handleLogin(ActionEvent event) {
        String username = txtUsername.getText().trim();
        String password = txtPassword.getText().trim();

        // 1. Kiểm tra đầu vào bằng ValidationUtils (ném DataValidationException nếu không hợp lệ)
        try {
            ValidationUtils.validateLoginInput(username, password);
        } catch (DataValidationException e) {
            showError(lblError, e.getMessage());
            return;
        }

        clearMessage(lblError);
        System.out.println("[LoginController] Đang xử lý đăng nhập cho: " + username);

        // Kích hoạt trạng thái đang xử lý (vô hiệu hóa nút và ô nhập để chống spam click)
        setLoading(true);

        try {
            // 1. Tạo gói tin yêu cầu LOGIN
            Map<String, String> credentials = new HashMap<>();
            credentials.put("username", username);
            credentials.put("password", password);
            Message loginReq = Message.request(Message.ACTION_LOGIN, credentials);

            // 2. Gửi yêu cầu qua Socket TCP bất đồng bộ (Non-blocking background thread)
            NetworkClient.getInstance().sendRequestAsync(loginReq)
                    .thenAcceptAsync(response -> {
                        setLoading(false);
                        processLoginResponse(event, response, username, password);
                    }, Platform::runLater)
                    .exceptionallyAsync(throwable -> {
                        setLoading(false);
                        showError(lblError, "Lỗi kết nối Server: " + throwable.getMessage());
                        return null;
                    }, Platform::runLater);

        } catch (Exception e) {
            setLoading(false);
            showError(lblError, "Lỗi khởi tạo yêu cầu: " + e.getMessage());
        }
    }

    /**
     * Xử lý gói tin phản hồi đăng nhập từ Server trên luồng JavaFX UI.
     * Xác thực 100% qua Socket Server và CSDL MySQL thực tế (không dùng mock offline).
     */
    private void processLoginResponse(ActionEvent event, Message response, String username, String password) {
        if (response != null && response.isSuccess()) {
            User user = response.parsePayload(User.class);
            if (user != null) {
                // Lưu User vào SessionManager
                SessionManager.getInstance().setCurrentUser(user);
                System.out.println("[LoginController] Đăng nhập CSDL thành công cho: " + user.getUsername() + " (" + user.getClass().getSimpleName() + ")");

                // Điều hướng theo phân quyền
                if (user instanceof Admin) {
                    switchToScene(event, "/views/AdminView.fxml", "Hệ thống Đấu giá - Kênh Quản trị viên (Admin)");
                } else if (user instanceof Bidder) {
                    switchToScene(event, "/views/MainView.fxml", "Hệ thống Đấu giá Trực tuyến - Trang chủ");
                } else {
                    switchToScene(event, "/views/SellerView.fxml", "Hệ thống Đấu giá - Kênh Người bán (Seller)");
                }
                return;
            }
        }

        // Chuyển đổi phản hồi lỗi sang AuthenticationException / ConnectionException tương ứng
        AuctionException ex = NetworkClient.mapErrorToException(response);
        showError(lblError, "[" + resolveExceptionTitle(ex) + "] " + ex.getMessage());
    }

    /**
     * Bật/tắt trạng thái xử lý giao diện (Loading state).
     *
     * @param loading true nếu đang xử lý mạng, false nếu đã xong
     */
    private void setLoading(boolean loading) {
        if (btnLogin != null) {
            btnLogin.setDisable(loading);
            btnLogin.setText(loading ? "Đang xác thực..." : "Đăng nhập");
        }
        if (txtUsername != null) {
            txtUsername.setDisable(loading);
        }
        if (txtPassword != null) {
            txtPassword.setDisable(loading);
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
