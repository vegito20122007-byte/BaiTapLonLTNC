package com.auction.client.controllers;

import com.auction.client.session.SessionManager;
import com.auction.model.Admin;
import com.auction.model.BidTransaction;
import com.auction.model.Bidder;
import com.auction.model.Seller;
import com.auction.model.User;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Controller điều khiển giao diện Xem lịch sử giao dịch đấu giá (HistoryView.fxml).
 * Kế thừa BaseController, quản lý danh sách ObservableList<BidTransaction> theo đúng thiết kế UML.
 */
public class HistoryController extends BaseController {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    // ==========================================
    // 1. ÁNH XẠ CÁC CONTROL TỪ FXML (fx:id)
    // ==========================================
    @FXML
    private BorderPane rootPane;

    @FXML
    private Label lblUsername;

    @FXML
    private TextField txtSearch;

    @FXML
    private Label lblTotalCount;

    @FXML
    private Label lblTotalAmount;

    @FXML
    private Label lblMessage;

    @FXML
    private Label lblDetail;

    @FXML
    private TableView<BidTransaction> tableHistory;

    @FXML
    private TableColumn<BidTransaction, Integer> colId;

    @FXML
    private TableColumn<BidTransaction, Integer> colAuctionId;

    @FXML
    private TableColumn<BidTransaction, String> colBidder;

    @FXML
    private TableColumn<BidTransaction, String> colAmount;

    @FXML
    private TableColumn<BidTransaction, String> colTime;

    // ==========================================
    // 2. DỮ LIỆU BẢNG (Thuộc tính trong UML)
    // ==========================================
    private ObservableList<BidTransaction> listHistory = FXCollections.observableArrayList();
    private ObservableList<BidTransaction> allHistoryData = FXCollections.observableArrayList();

    /**
     * Vòng đời khởi tạo giao diện.
     */
    @FXML
    public void initialize() {
        initUserInfo();
        setupTableColumns();
        setupTableSelection();
        setupFocusClear(rootPane, null);
        loadData();
    }

    /**
     * Khởi tạo thông tin người dùng từ SessionManager.
     */
    private void initUserInfo() {
        User currentUser = SessionManager.getInstance().getCurrentUser();
        if (currentUser != null) {
            lblUsername.setText(currentUser.getUsername() + " (" + currentUser.getClass().getSimpleName() + ")");
        } else {
            lblUsername.setText("Khách vãng lai");
        }
    }

    /**
     * Thiết lập các cột TableColumn hiển thị thông tin BidTransaction.
     */
    private void setupTableColumns() {
        colId.setCellValueFactory(cell -> 
                new SimpleIntegerProperty(cell.getValue().getId() != null ? cell.getValue().getId() : 0).asObject());

        colAuctionId.setCellValueFactory(cell -> 
                new SimpleIntegerProperty(cell.getValue().getAuctionId() != null ? cell.getValue().getAuctionId() : 0).asObject());

        colBidder.setCellValueFactory(cell -> {
            User bidder = cell.getValue().getBidder();
            return new SimpleStringProperty(bidder != null ? bidder.getUsername() : "Chưa rõ");
        });

        colAmount.setCellValueFactory(cell -> 
                new SimpleStringProperty(String.format("%,.0f VNĐ", cell.getValue().getAmount())));

        colTime.setCellValueFactory(cell -> {
            LocalDateTime time = cell.getValue().getTime();
            return new SimpleStringProperty(time != null ? time.format(DATE_TIME_FORMATTER) : "N/A");
        });

        tableHistory.setItems(listHistory);
    }

    /**
     * Bắt sự kiện chọn dòng trên bảng để hiển thị chi tiết dưới thanh trạng thái.
     */
    private void setupTableSelection() {
        tableHistory.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                String bidderName = newVal.getBidder() != null ? newVal.getBidder().getUsername() : "N/A";
                String timeStr = newVal.getTime() != null ? newVal.getTime().format(DATE_TIME_FORMATTER) : "N/A";
                lblDetail.setText(String.format("Giao dịch #%d | Phiên #%d | Người đặt: %s | Mức giá: %,.0f VNĐ | Lúc: %s",
                        newVal.getId(), newVal.getAuctionId(), bidderName, newVal.getAmount(), timeStr));
            } else {
                lblDetail.setText("Chọn một dòng trên bảng để xem chi tiết.");
            }
        });
    }

    // ==========================================
    // 3. TẢI DỮ LIỆU & LỌC TÌM KIẾM
    // ==========================================

    /**
     * Tải danh sách lịch sử đấu giá (loadData theo đúng UML).
     * Hiện đang nạp dữ liệu mẫu để kiểm thử UI; khi nối Server sẽ nhận danh sách qua NetworkClient.
     */
    @FXML
    public void loadData() {
        clearMessage(lblMessage);
        txtSearch.clear();

        allHistoryData.clear();
        allHistoryData.addAll(createMockTransactions());

        listHistory.clear();
        listHistory.addAll(allHistoryData);

        updateSummaryStatistics();
        showSuccess(lblMessage, "Đã làm mới dữ liệu lịch sử thành công (" + listHistory.size() + " giao dịch).");
    }

    /**
     * Xử lý tìm kiếm giao dịch theo từ khóa (Mã GD, Mã phiên, Tên người đặt).
     *
     * @param event sự kiện ActionEvent
     */
    @FXML
    public void handleSearch(ActionEvent event) {
        String keyword = txtSearch.getText().trim().toLowerCase();

        if (keyword.isEmpty()) {
            listHistory.clear();
            listHistory.addAll(allHistoryData);
            updateSummaryStatistics();
            clearMessage(lblMessage);
            return;
        }

        listHistory.clear();
        for (BidTransaction tx : allHistoryData) {
            boolean matchId = tx.getId() != null && String.valueOf(tx.getId()).contains(keyword);
            boolean matchAuction = tx.getAuctionId() != null && String.valueOf(tx.getAuctionId()).contains(keyword);
            boolean matchBidder = tx.getBidder() != null && tx.getBidder().getUsername().toLowerCase().contains(keyword);

            if (matchId || matchAuction || matchBidder) {
                listHistory.add(tx);
            }
        }

        updateSummaryStatistics();
        showSuccess(lblMessage, "Tìm thấy " + listHistory.size() + " giao dịch phù hợp.");
    }

    /**
     * Cập nhật số liệu thống kê tổng số giao dịch và tổng giá trị đặt.
     */
    private void updateSummaryStatistics() {
        lblTotalCount.setText("Tổng số giao dịch: " + listHistory.size());

        double totalAmount = 0.0;
        for (BidTransaction tx : listHistory) {
            totalAmount += tx.getAmount();
        }
        lblTotalAmount.setText(String.format("Tổng giá trị: %,.0f VNĐ", totalAmount));
    }

    /**
     * Tạo danh sách dữ liệu mẫu để kiểm tra giao diện độc lập.
     */
    private List<BidTransaction> createMockTransactions() {
        User bidder1 = new Bidder(101, "nguyen_van_a", "pass123");
        User bidder2 = new Bidder(102, "tran_thi_b", "pass123");
        User bidder3 = new Bidder(103, "le_van_c", "pass123");

        LocalDateTime now = LocalDateTime.now();

        return List.of(
                new BidTransaction(1, 10, 25500000.0, bidder1, now.minusHours(3).minusMinutes(12)),
                new BidTransaction(2, 10, 26000000.0, bidder2, now.minusHours(2).minusMinutes(45)),
                new BidTransaction(3, 10, 27500000.0, bidder3, now.minusHours(1).minusMinutes(10)),
                new BidTransaction(4, 11, 450000000.0, bidder2, now.minusDays(1).minusHours(2)),
                new BidTransaction(5, 11, 480000000.0, bidder1, now.minusHours(5)),
                new BidTransaction(6, 12, 12000000.0, bidder3, now.minusMinutes(35))
        );
    }

    // ==========================================
    // 4. ĐIỀU HƯỚNG MÀN HÌNH (backToMain)
    // ==========================================

    /**
     * Quay lại màn hình chính của vai trò hiện tại (backToMain theo đúng UML).
     *
     * @param event sự kiện ActionEvent
     */
    @FXML
    public void backToMain(ActionEvent event) {
        User currentUser = SessionManager.getInstance().getCurrentUser();

        if (currentUser instanceof Admin) {
            switchToScene(event, "/views/AdminView.fxml", "Hệ thống Đấu giá - Kênh Quản trị viên (Admin)");
        } else if (currentUser instanceof Seller) {
            switchToScene(event, "/views/SellerView.fxml", "Hệ thống Đấu giá - Kênh Người bán (Seller)");
        } else if (currentUser instanceof Bidder) {
            // Điều hướng chính xác cho Bidder về lại Trang chủ
            switchToScene(event, "/views/MainView.fxml", "Hệ thống Đấu giá Trực tuyến - Trang chủ");
        } else {
            // Dự phòng khi phiên rỗng hoặc chưa đăng nhập
            switchToScene(event, "/views/LoginView.fxml", "Hệ thống Đấu giá Trực tuyến - Đăng nhập");
        }
    }

    /**
     * Đăng xuất khỏi hệ thống.
     *
     * @param event sự kiện ActionEvent
     */
    @FXML
    public void handleLogout(ActionEvent event) {
        SessionManager.getInstance().clear();
        switchToScene(event, "/views/LoginView.fxml", "Hệ thống Đấu giá Trực tuyến - Đăng nhập");
    }

    public ObservableList<BidTransaction> getListHistory() {
        return listHistory;
    }
}
