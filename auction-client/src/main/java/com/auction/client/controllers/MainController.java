package com.auction.client.controllers;

import com.auction.client.network.Message;
import com.auction.client.network.NetworkClient;
import com.auction.client.session.SessionManager;
import com.auction.client.util.ImageUtils;
import com.auction.model.Auction;
import com.auction.model.ItemFactory;
import com.auction.model.Seller;
import com.auction.model.User;
import com.google.gson.reflect.TypeToken;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Controller điều khiển Trang chủ Sàn đấu giá (MainView.fxml).
 * Tích hợp NetworkClient giao tiếp với AuctionServer:
 * - Tự động tải danh sách phiên đấu giá thực từ CSDL/Server qua ACTION_GET_AUCTIONS.
 * - Lắng nghe thông báo Realtime (Observer Pattern) khi có phiên mới hoặc thay đổi giá.
 * - Cho phép chọn phiên bất kỳ để chuyển thẳng vào phòng đấu giá BidderView tương ứng.
 */
public class MainController extends BaseController {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

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

    // Listener realtime nhận broadcast từ Server
    private Consumer<Message> realtimeListener;

    /**
     * DTO hiển thị thẻ đấu giá trên giao diện Grid.
     */
    public static class ClientAuctionItem {
        private final String name;
        private final String category;
        private final double currentPrice;
        private final String endTime;
        private final Auction auction;

        public ClientAuctionItem(String name, String category, double currentPrice, String endTime) {
            this(name, category, currentPrice, endTime, null);
        }

        public ClientAuctionItem(String name, String category, double currentPrice, String endTime, Auction auction) {
            this.name = name;
            this.category = category;
            this.currentPrice = currentPrice;
            this.endTime = endTime;
            this.auction = auction;
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

        public Auction getAuction() {
            return auction;
        }
    }

    private final ObservableList<ClientAuctionItem> displayList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        cbCategory.setItems(FXCollections.observableArrayList("Tất cả danh mục", "Electronics", "Art", "Vehicle"));
        cbCategory.getSelectionModel().selectFirst();

        txtSearch.textProperty().addListener((obs, oldVal, newVal) -> filterAuctions());
        cbCategory.valueProperty().addListener((obs, oldVal, newVal) -> filterAuctions());

        // Hiển thị thông tin người dùng từ Session
        User currentUser = SessionManager.getInstance().getCurrentUser();
        if (currentUser != null && lblWelcome != null) {
            lblWelcome.setText("Xin chào, " + currentUser.getUsername() + " (" + currentUser.getRole() + ")!");
        }

        // Đăng ký nhận thông báo realtime (Observer Pattern)
        setupRealtimeObserver();

        // Tải danh sách phiên đấu giá từ Server
        loadAuctionsFromServer();
    }

    /**
     * Đăng ký nhận thông báo phát thanh (Broadcast) từ Server khi có cập nhật.
     */
    private void setupRealtimeObserver() {
        realtimeListener = msg -> {
            if (msg == null || msg.getAction() == null) {
                return;
            }
            String action = msg.getAction().toUpperCase();
            if (Message.ACTION_AUCTION_UPDATE.equals(action)
                    || Message.ACTION_BID_UPDATE.equals(action)
                    || Message.ACTION_DELETE_AUCTION.equals(action)
                    || Message.ACTION_CREATE_AUCTION.equals(action)) {
                Platform.runLater(this::loadAuctionsFromServer);
            }
        };
        NetworkClient.getInstance().addListener(realtimeListener);
    }

    /**
     * Tải danh sách phiên đấu giá bất đồng bộ từ Server qua ACTION_GET_AUCTIONS.
     * Toàn bộ dữ liệu hiển thị đến từ CSDL MySQL thực tế (không dùng mock offline).
     */
    public void loadAuctionsFromServer() {
        if (lblActiveCount != null) {
            lblActiveCount.setText("Đang tải dữ liệu từ CSDL...");
        }

        NetworkClient.getInstance().sendRequestAsync(Message.request(Message.ACTION_GET_AUCTIONS, null))
                .thenAccept(response -> {
                    Platform.runLater(() -> {
                        if (response != null && response.isSuccess()) {
                            Type listType = new TypeToken<List<Auction>>() {}.getType();
                            List<Auction> serverAuctions = response.parsePayload(listType);
                            if (serverAuctions != null) {
                                updateDisplayListFromAuctions(serverAuctions);
                                return;
                            }
                        }
                        displayList.clear();
                        filterAuctions();
                        if (lblActiveCount != null) {
                            lblActiveCount.setText("Chưa có phiên đấu giá hoặc mất kết nối Server.");
                        }
                    });
                })
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        displayList.clear();
                        filterAuctions();
                        if (lblActiveCount != null) {
                            lblActiveCount.setText("Lỗi kết nối máy chủ: " + ex.getMessage());
                        }
                    });
                    return null;
                });
    }

    private void updateDisplayListFromAuctions(List<Auction> serverAuctions) {
        displayList.clear();
        for (Auction a : serverAuctions) {
            String category = (a.getItem() != null && a.getItem().getCategory() != null)
                    ? a.getItem().getCategory() : "Khác";
            double price = a.getMaxBid() > 0 ? a.getMaxBid() : a.getStartingPrice();
            String end = a.getEndTime() != null ? a.getEndTime().format(DATE_FORMATTER) : "N/A";

            displayList.add(new ClientAuctionItem(a.getName(), category, price, end, a));
        }

        if (lblActiveCount != null) {
            lblActiveCount.setText(String.format("%,d phiên đang mở (CSDL thực)", displayList.size()));
        }
        filterAuctions();
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

        // Ảnh thumbnail sản phẩm nếu có
        if (item.getAuction() != null && item.getAuction().getItem() != null) {
            String imgData = item.getAuction().getItem().getImageData();
            if (imgData != null && !imgData.isBlank()) {
                try {
                    ImageView thumb = new ImageView(ImageUtils.base64ToImage(imgData));
                    thumb.setFitWidth(236);
                    thumb.setFitHeight(130);
                    thumb.setPreserveRatio(true);
                    thumb.setStyle("-fx-background-radius: 6;");
                    card.getChildren().add(thumb);
                } catch (Exception ignored) {
                }
            }
        }

        Label lblTitle = new Label(item.getName());
        lblTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 14;");

        Label lblCategoryTag = new Label("Danh mục: " + item.getCategory());
        lblCategoryTag.setStyle("-fx-text-fill: #1a73e8; -fx-font-size: 11; -fx-background-color: #e8f0fe; -fx-padding: 2 6; -fx-background-radius: 4;");

        Label lblPrice = new Label(String.format("Giá hiện tại: %,.0f VNĐ", item.getCurrentPrice()));
        lblPrice.setStyle("-fx-text-fill: #1e8e3e; -fx-font-weight: bold;");

        Label lblEnd = new Label("Kết thúc: " + item.getEndTime());
        lblEnd.setStyle("-fx-text-fill: #d93025; -fx-font-size: 11;");

        Button btnBid = new Button("Vào đấu giá →");
        btnBid.setMaxWidth(Double.MAX_VALUE);
        btnBid.setStyle("-fx-background-color: #1a73e8; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;");

        // Chuyển sang BidderView và nạp thông tin phiên được click
        btnBid.setOnAction(e -> {
            cleanupListener();
            BidderController controller = switchSceneWithController(e, "/views/BidderView.fxml", "Phòng đấu giá trực tiếp - " + item.getName());
            if (controller != null && item.getAuction() != null) {
                controller.setAuction(item.getAuction());
            }
        });

        card.getChildren().addAll(lblTitle, lblCategoryTag, lblPrice, lblEnd, btnBid);
        return card;
    }

    @FXML
    public void handleOpenAuctionRoom(ActionEvent event) {
        cleanupListener();
        BidderController controller = switchSceneWithController(event, "/views/BidderView.fxml", "Phòng đấu giá trực tiếp");
        if (controller != null && !displayList.isEmpty()) {
            Auction target = displayList.get(0).getAuction();
            if (target != null) {
                controller.setAuction(target);
            }
        }
    }

    @FXML
    public void handleLogout(ActionEvent event) {
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Xác nhận đăng xuất");
        confirmAlert.setHeaderText(null);
        confirmAlert.setContentText("Bạn có chắc chắn muốn đăng xuất khỏi tài khoản không?");

        confirmAlert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                cleanupListener();
                SessionManager.getInstance().clear();
                switchToScene(event, "/views/LoginView.fxml", "Hệ thống Đấu giá Trực tuyến - Đăng nhập");
            }
        });
    }

    private void cleanupListener() {
        if (realtimeListener != null) {
            NetworkClient.getInstance().removeListener(realtimeListener);
            realtimeListener = null;
        }
    }

    public ObservableList<ClientAuctionItem> getDisplayList() {
        return displayList;
    }
}
