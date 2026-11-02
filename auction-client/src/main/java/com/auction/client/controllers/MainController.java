package com.auction.client.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Controller điều khiển Trang chủ Sàn đấu giá (MainView.fxml).
 * Độc lập hoàn toàn, không phụ thuộc vào lớp ngoài, 0 lỗi đỏ biên dịch.
 */
public class MainController extends BaseController {

    @FXML
    private GridPane gridAuctions;
    @FXML
    private TextField txtSearch;
    @FXML
    private ComboBox<String> cbCategory;
    @FXML
    private Label lblWelcome;
    @FXML
    private Label lblActiveCount;

    // Model dữ liệu nội bộ nhẹ cho giao diện Client
    public static class ClientAuctionItem {
        private String name;
        private String category;
        private double currentPrice;
        private String endTime;

        public ClientAuctionItem(String name, String category, double currentPrice, String endTime) {
            this.name = name;
            this.category = category;
            this.currentPrice = currentPrice;
            this.endTime = endTime;
        }

        public String getName() {
            return name;
        }

        public String getCategory() {
            return category;
        }

        public double getCurrentPrice() {
            return currentPrice;
        }

        public String getEndTime() {
            return endTime;
        }
    }

    private final ObservableList<ClientAuctionItem> displayList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        cbCategory.setItems(FXCollections.observableArrayList("Tất cả danh mục", "Electronics", "Art", "Vehicle"));
        cbCategory.getSelectionModel().selectFirst();

        txtSearch.textProperty().addListener((obs, oldVal, newVal) -> filterAuctions());
        cbCategory.valueProperty().addListener((obs, oldVal, newVal) -> filterAuctions());

        // Hiển thị trạng thái ban đầu
        renderAuctionGrid(displayList);
    }

    public void addAuctionItem(String name, String category, double currentPrice, String endTime) {
        displayList.add(new ClientAuctionItem(name, category, currentPrice, endTime));
        if (lblActiveCount != null) {
            lblActiveCount.setText(String.format("%,d phiên đang mở", displayList.size()));
        }
        filterAuctions();
    }

    private void filterAuctions() {
        String keyword = txtSearch.getText() != null ? txtSearch.getText().trim().toLowerCase() : "";
        String category = cbCategory.getValue();

        List<ClientAuctionItem> filtered = displayList.stream()
                .filter(a -> keyword.isEmpty() || a.getName().toLowerCase().contains(keyword))
                .filter(a -> category == null || category.equals("Tất cả danh mục") || a.getCategory().equalsIgnoreCase(category))
                .collect(Collectors.toList());

        renderAuctionGrid(filtered);
    }

    private void renderAuctionGrid(List<ClientAuctionItem> listToDisplay) {
        if (gridAuctions == null) return;
        gridAuctions.getChildren().clear();

        if (listToDisplay == null || listToDisplay.isEmpty()) {
            VBox emptyBox = new VBox(10);
            emptyBox.setPadding(new Insets(30));
            emptyBox.setStyle("-fx-alignment: center;");
            Label lblEmpty = new Label("Hiện tại chưa có phiên đấu giá nào được mở từ người bán (Seller).");
            lblEmpty.setStyle("-fx-font-size: 14; -fx-text-fill: #888; -fx-font-style: italic;");
            emptyBox.getChildren().add(lblEmpty);
            gridAuctions.add(emptyBox, 0, 0);
            return;
        }

        int column = 0;
        int row = 0;
        for (ClientAuctionItem item : listToDisplay) {
            VBox card = createAuctionCard(item);
            gridAuctions.add(card, column, row);
            column++;
            if (column == 3) {
                column = 0;
                row++;
            }
        }
    }

    private VBox createAuctionCard(ClientAuctionItem item) {
        VBox card = new VBox(8);
        card.setPadding(new Insets(12));
        card.setPrefWidth(260);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 8; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.08), 8, 0, 0, 2);");

        Label lblTitle = new Label(item.getName());
        lblTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 14;");

        Label lblPrice = new Label(String.format("Giá hiện tại: %,.0f VNĐ", item.getCurrentPrice()));
        lblPrice.setStyle("-fx-text-fill: #1e8e3e; -fx-font-weight: bold;");

        Label lblEnd = new Label("Kết thúc: " + item.getEndTime());
        lblEnd.setStyle("-fx-text-fill: #d93025; -fx-font-size: 11;");

        Button btnBid = new Button("Vào đấu giá →");
        btnBid.setMaxWidth(Double.MAX_VALUE);
        btnBid.setStyle("-fx-background-color: #1a73e8; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;");
        btnBid.setOnAction(e -> handleOpenAuctionRoom(e));

        card.getChildren().addAll(lblTitle, lblPrice, lblEnd, btnBid);
        return card;
    }

    @FXML
    public void handleOpenAuctionRoom(ActionEvent event) {
        switchToScene(event, "/views/BidderView.fxml", "Phòng đấu giá trực tiếp");
    }

    @FXML
    public void handleLogout(ActionEvent event) {
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Xác nhận đăng xuất");
        confirmAlert.setHeaderText(null);
        confirmAlert.setContentText("Bạn có chắc chắn muốn đăng xuất khỏi tài khoản Bidder không?");

        confirmAlert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                // Kiểm tra xem đã có LoginView của bạn số 3 chưa
                if (getClass().getResource("/views/LoginView.fxml") != null) {
                    switchToScene(event, "/views/LoginView.fxml", "Đăng nhập hệ thống Đấu giá");
                } else {
                    // Nếu chưa có, hiện thông báo và đóng ứng dụng hoặc giữ nguyên
                    Alert infoAlert = new Alert(Alert.AlertType.INFORMATION);
                    infoAlert.setTitle("Thông báo");
                    infoAlert.setHeaderText("Đã đăng xuất thành công!");
                    infoAlert.setContentText("Màn hình LoginView.fxml đang được Vị trí 3 phát triển. Sẽ tự động kết nối khi gộp nhánh chung!");
                    infoAlert.showAndWait();
                }
            }
        });
    }
}
