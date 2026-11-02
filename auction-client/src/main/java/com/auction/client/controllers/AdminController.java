package com.auction.client.controllers;

import com.auction.client.session.SessionManager;
import com.auction.model.*;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Controller điều khiển giao diện Quản trị viên (AdminView.fxml).
 * Tuân thủ mô hình MVC và các thuộc tính quy định trong sơ đồ lớp UML:
 * - userTableView: TableView<User>
 * - auctionTableView: TableView<Auction>
 * - listUsers: ObservableList<User>
 * - listAuctions: ObservableList<Auction>
 */
public class AdminController extends BaseController {

    // ==========================================
    // 1. ÁNH XẠ CÁC CONTROL TỪ FXML (fx:id)
    // ==========================================
    @FXML
    private BorderPane rootPane;

    @FXML
    private Label lblAdminName;

    @FXML
    private Label lblMessage;

    @FXML
    private Label lblStats;

    // Các thành phần quản lý User
    @FXML
    private TableView<User> userTableView;

    @FXML
    private TableColumn<User, Integer> colUserId;

    @FXML
    private TableColumn<User, String> colUsername;

    @FXML
    private TableColumn<User, String> colRole;

    @FXML
    private TableColumn<User, Double> colBalance;

    @FXML
    private ComboBox<String> cbFilterRole;

    @FXML
    private TextField txtSearchUser;

    // Các thành phần quản lý Phiên đấu giá
    @FXML
    private TableView<Auction> auctionTableView;

    @FXML
    private TableColumn<Auction, Integer> colAuctionId;

    @FXML
    private TableColumn<Auction, String> colAuctionName;

    @FXML
    private TableColumn<Auction, String> colAuctionCategory;

    @FXML
    private TableColumn<Auction, String> colAuctionSeller;

    @FXML
    private TableColumn<Auction, Double> colAuctionStartingPrice;

    @FXML
    private TableColumn<Auction, Double> colAuctionCurrentPrice;

    @FXML
    private TableColumn<Auction, String> colAuctionStatus;

    @FXML
    private TableColumn<Auction, String> colAuctionEndTime;

    @FXML
    private ComboBox<String> cbFilterStatus;

    // ==========================================
    // 2. THUỘC TÍNH THEO SƠ ĐỒ UML
    // ==========================================
    private ObservableList<User> listUsers = FXCollections.observableArrayList();
    private ObservableList<Auction> listAuctions = FXCollections.observableArrayList();

    private FilteredList<User> filteredUsers;
    private FilteredList<Auction> filteredAuctions;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    /**
     * Vòng đời khởi tạo màn hình Quản trị viên.
     */
    @FXML
    public void initialize() {
        // Thiết lập Focus clear
        setupFocusClear(rootPane, null);
        clearMessage(lblMessage);

        // Hiển thị tên Admin đăng nhập từ SessionManager
        User currentUser = SessionManager.getInstance().getCurrentUser();
        if (currentUser != null) {
            lblAdminName.setText(currentUser.getUsername());
        }

        // Cấu hình bảng User
        colUserId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colUsername.setCellValueFactory(new PropertyValueFactory<>("username"));
        colRole.setCellValueFactory(new PropertyValueFactory<>("role"));
        colBalance.setCellValueFactory(new PropertyValueFactory<>("balance"));

        // Cấu hình bảng Auction
        colAuctionId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colAuctionName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colAuctionCategory.setCellValueFactory(cellData -> {
            Item item = cellData.getValue().getItem();
            return new SimpleStringProperty(item != null ? item.getCategory() : "N/A");
        });
        colAuctionSeller.setCellValueFactory(cellData -> {
            User seller = cellData.getValue().getSeller();
            return new SimpleStringProperty(seller != null ? seller.getUsername() : "N/A");
        });
        colAuctionStartingPrice.setCellValueFactory(new PropertyValueFactory<>("startingPrice"));
        colAuctionCurrentPrice.setCellValueFactory(new PropertyValueFactory<>("maxBid"));
        colAuctionStatus.setCellValueFactory(cellData -> {
            Status s = cellData.getValue().getStatus();
            return new SimpleStringProperty(s != null ? s.name() : "OPEN");
        });
        colAuctionEndTime.setCellValueFactory(cellData -> {
            LocalDateTime end = cellData.getValue().getEndTime();
            return new SimpleStringProperty(end != null ? end.format(DATE_FORMATTER) : "N/A");
        });

        // Cấu hình bộ lọc Role cho User
        cbFilterRole.setItems(FXCollections.observableArrayList("TẤT CẢ", "BIDDER", "SELLER", "ADMIN"));
        cbFilterRole.getSelectionModel().selectFirst();
        cbFilterRole.setOnAction(e -> applyUserFilter());
        txtSearchUser.textProperty().addListener((obs, oldVal, newVal) -> applyUserFilter());

        // Cấu hình bộ lọc Status cho Auction
        cbFilterStatus.setItems(FXCollections.observableArrayList("TẤT CẢ", "OPEN", "RUNNING", "FINISHED", "CANCELED"));
        cbFilterStatus.getSelectionModel().selectFirst();
        cbFilterStatus.setOnAction(e -> applyAuctionFilter());

        // Thiết lập FilteredList
        filteredUsers = new FilteredList<>(listUsers, p -> true);
        userTableView.setItems(filteredUsers);

        filteredAuctions = new FilteredList<>(listAuctions, p -> true);
        auctionTableView.setItems(filteredAuctions);

        // Nạp dữ liệu mẫu để kiểm thử
        loadData();
    }

    /**
     * Tải dữ liệu ban đầu cho cả 2 bảng (người dùng và phiên đấu giá).
     */
    public void loadData() {
        if (listUsers.isEmpty()) {
            listUsers.addAll(
                    UserFactory.createUser(1, "admin", "admin123", "ADMIN"),
                    UserFactory.createUser(2, "seller_quang", "pass123", "SELLER"),
                    UserFactory.createUser(3, "seller_hieu", "pass123", "SELLER"),
                    UserFactory.createUser(4, "bidder_nam", "pass123", "BIDDER"),
                    UserFactory.createUser(5, "bidder_hiep", "pass123", "BIDDER")
            );
        }

        if (listAuctions.isEmpty()) {
            Seller seller1 = (Seller) listUsers.get(1);
            Seller seller2 = (Seller) listUsers.get(2);

            Item item1 = ItemFactory.createItem("ART", 101, "Bức Họa Mùa Thu", "Tác phẩm nghệ thuật đương đại", seller1);
            Auction auction1 = new Auction(101, "Đấu giá Tranh Mùa Thu", item1, seller1, 8000000.0, 10000000.0,
                    LocalDateTime.now(), LocalDateTime.now().plusDays(3));

            Item item2 = ItemFactory.createItem("ELECTRONICS", 102, "MacBook Pro M3", "Máy mới 99% fullbox", seller2);
            Auction auction2 = new Auction(102, "Đấu giá MacBook Pro", item2, seller2, 35000000.0, 40000000.0,
                    LocalDateTime.now(), LocalDateTime.now().plusHours(12));

            Item item3 = ItemFactory.createItem("VEHICLE", 103, "Xe Cổ Vespa Sprint", "Vespa Ý 150cc", seller1);
            Auction auction3 = new Auction(103, "Đấu giá Vespa Cổ", item3, seller1, 45000000.0, 50000000.0,
                    LocalDateTime.now(), LocalDateTime.now().plusDays(7));

            listAuctions.addAll(auction1, auction2, auction3);
        }

        updateStats();
        showSuccess(lblMessage, "Đã tải dữ liệu quản trị thành công!");
    }

    /**
     * Xóa tài khoản người dùng được chọn.
     *
     * @param event sự kiện ActionEvent
     */
    @FXML
    public void handleDeleteUser(ActionEvent event) {
        User selected = userTableView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError(lblMessage, "Vui lòng chọn một người dùng trên bảng để xóa!");
            return;
        }

        if ("ADMIN".equalsIgnoreCase(selected.getRole())) {
            showError(lblMessage, "Không thể xóa tài khoản Quản trị viên (ADMIN)!");
            return;
        }

        listUsers.remove(selected);
        updateStats();
        showSuccess(lblMessage, "Đã xóa người dùng: " + selected.getUsername());
    }

    /**
     * Hủy phiên đấu giá có dấu hiệu vi phạm (chuyển trạng thái sang CANCELED).
     *
     * @param event sự kiện ActionEvent
     */
    @FXML
    public void handleCancelAuction(ActionEvent event) {
        Auction selected = auctionTableView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError(lblMessage, "Vui lòng chọn một phiên trên bảng để hủy!");
            return;
        }

        if (selected.getStatus() == Status.CANCELED) {
            showError(lblMessage, "Phiên này đã bị hủy từ trước!");
            return;
        }

        selected.setStatus(Status.CANCELED);
        auctionTableView.refresh();
        updateStats();
        showSuccess(lblMessage, "Đã hủy phiên đấu giá vi phạm #" + selected.getId());
    }

    /**
     * Làm mới danh sách Người dùng.
     */
    @FXML
    public void handleRefreshUsers(ActionEvent event) {
        userTableView.refresh();
        updateStats();
        showSuccess(lblMessage, "Đã làm mới danh sách Người dùng!");
    }

    /**
     * Làm mới danh sách Phiên đấu giá.
     */
    @FXML
    public void handleRefreshAuctions(ActionEvent event) {
        auctionTableView.refresh();
        updateStats();
        showSuccess(lblMessage, "Đã làm mới danh sách Phiên đấu giá!");
    }

    /**
     * Mở màn hình Lịch sử đấu giá & giao dịch.
     */
    @FXML
    public void handleOpenHistory(ActionEvent event) {
        switchToScene(event, "/views/HistoryView.fxml", "Hệ thống Đấu giá - Lịch sử giao dịch & đấu giá");
    }

    /**
     * Đăng xuất và quay về màn hình Đăng nhập.
     *
     * @param event sự kiện ActionEvent
     */
    @FXML
    public void handleLogout(ActionEvent event) {
        SessionManager.getInstance().clear();
        switchToScene(event, "/views/LoginView.fxml", "Hệ thống Đấu giá Trực tuyến - Đăng nhập");
    }

    /**
     * Lọc danh sách Người dùng theo vai trò và ô tìm kiếm.
     */
    private void applyUserFilter() {
        String selectedRole = cbFilterRole.getValue();
        String searchText = txtSearchUser.getText() != null ? txtSearchUser.getText().trim().toLowerCase() : "";

        filteredUsers.setPredicate(user -> {
            boolean matchesRole = true;
            if (selectedRole != null && !"TẤT CẢ".equals(selectedRole)) {
                matchesRole = selectedRole.equalsIgnoreCase(user.getRole());
            }

            boolean matchesSearch = true;
            if (!searchText.isEmpty()) {
                matchesSearch = user.getUsername() != null && user.getUsername().toLowerCase().contains(searchText);
            }

            return matchesRole && matchesSearch;
        });

        updateStats();
    }

    /**
     * Lọc danh sách Phiên đấu giá theo trạng thái.
     */
    private void applyAuctionFilter() {
        String selectedStatus = cbFilterStatus.getValue();

        filteredAuctions.setPredicate(auction -> {
            if (selectedStatus == null || "TẤT CẢ".equals(selectedStatus)) {
                return true;
            }
            Status status = auction.getStatus();
            return status != null && selectedStatus.equalsIgnoreCase(status.name());
        });

        updateStats();
    }

    /**
     * Cập nhật số liệu thống kê ở thanh chân trang.
     */
    private void updateStats() {
        int userCount = filteredUsers != null ? filteredUsers.size() : listUsers.size();
        int auctionCount = filteredAuctions != null ? filteredAuctions.size() : listAuctions.size();
        lblStats.setText(String.format("Thống kê: %d Người dùng | %d Phiên đấu giá", userCount, auctionCount));
    }
}
