package com.auction.client.controllers;

import com.auction.client.network.Message;
import com.auction.client.network.NetworkClient;
import com.auction.client.session.SessionManager;
import com.auction.client.util.ImageUtils;
import com.auction.client.util.ValidationUtils;
import com.auction.model.*;
import com.google.gson.reflect.TypeToken;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * Controller điều khiển giao diện Người bán (SellerView.fxml).
 * Tuân thủ mô hình MVC và các thuộc tính, phương thức quy định trong sơ đồ lớp UML:
 * - auctionDataList: ObservableList<Auction>
 * - encodedImage: String
 * - handleAddAuction(event: ActionEvent)
 * - handleUpdateAuction(event: ActionEvent)
 * - handleDeleteAuction(event: ActionEvent)
 * - handleSelectImage()
 * - loadData()
 *
 * Tích hợp NetworkClient giao tiếp với AuctionServer và Observer realtime.
 */
public class SellerController extends BaseController {

    // ==========================================
    // 1. ÁNH XẠ CÁC CONTROL TỪ FXML (fx:id)
    // ==========================================
    @FXML
    private BorderPane rootPane;

    @FXML
    private VBox formPane;

    @FXML
    private Label lblSellerName;

    @FXML
    private Label lblBalance;

    @FXML
    private TableView<Auction> auctionTableView;

    @FXML
    private TableColumn<Auction, Integer> colId;

    @FXML
    private TableColumn<Auction, String> colName;

    @FXML
    private TableColumn<Auction, String> colCategory;

    @FXML
    private TableColumn<Auction, Double> colStartingPrice;

    @FXML
    private TableColumn<Auction, Double> colMaxBid;

    @FXML
    private TableColumn<Auction, String> colStatus;

    @FXML
    private TableColumn<Auction, String> colEndTime;

    @FXML
    private TextField txtName;

    @FXML
    private ComboBox<String> cbCategory;

    @FXML
    private TextField txtDescription;

    @FXML
    private TextField txtStartingPrice;

    @FXML
    private TextField txtReservePrice;

    @FXML
    private TextField txtDurationMinutes;

    @FXML
    private ImageView imgPreview;

    @FXML
    private Label lblMessage;

    // Danh sách quan sát chứa dữ liệu các phiên đấu giá
    private final ObservableList<Auction> auctionDataList = FXCollections.observableArrayList();

    // Chuỗi mã hóa ảnh Base64
    private String encodedImage;

    // Bộ sinh ID tạm thời phục vụ kiểm thử dữ liệu cục bộ khi Offline
    private static final AtomicInteger AUTO_ID_GEN = new AtomicInteger(100);

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // Listener realtime nhận broadcast từ Server
    private Consumer<Message> realtimeListener;

    /**
     * Vòng đời khởi tạo giao diện.
     */
    @FXML
    public void initialize() {
        // Cấu hình ComboBox danh mục
        cbCategory.setItems(FXCollections.observableArrayList("ART", "ELECTRONICS", "VEHICLE"));
        cbCategory.getSelectionModel().selectFirst();

        // Xóa focus khi click chuột ra vùng nền ngoài
        setupFocusClear(rootPane, formPane);
        clearMessage(lblMessage);

        // Hiển thị thông tin người dùng đang đăng nhập từ SessionManager
        User currentUser = SessionManager.getInstance().getCurrentUser();
        if (currentUser != null) {
            lblSellerName.setText(currentUser.getUsername());
            lblBalance.setText(String.format("%,.0f VNĐ", currentUser.getBalance()));
        }

        // Cấu hình các cột trong TableView
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colCategory.setCellValueFactory(cellData -> {
            Item item = cellData.getValue().getItem();
            return new SimpleStringProperty(item != null ? item.getCategory() : "N/A");
        });
        colStartingPrice.setCellValueFactory(new PropertyValueFactory<>("startingPrice"));
        colMaxBid.setCellValueFactory(new PropertyValueFactory<>("maxBid"));
        colStatus.setCellValueFactory(cellData -> {
            Status s = cellData.getValue().getStatus();
            return new SimpleStringProperty(s != null ? s.name() : "OPEN");
        });
        colEndTime.setCellValueFactory(cellData -> {
            LocalDateTime end = cellData.getValue().getEndTime();
            return new SimpleStringProperty(end != null ? end.format(DATE_FORMATTER) : "N/A");
        });

        // Lắng nghe sự kiện click chọn một dòng trên bảng để đưa dữ liệu vào form
        auctionTableView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, selected) -> {
            if (selected != null) {
                populateFormWithAuction(selected);
            }
        });

        auctionTableView.setItems(auctionDataList);

        // Đăng ký nhận thông báo realtime (Observer Pattern)
        setupRealtimeObserver();

        // Tải dữ liệu ban đầu
        loadData();
    }

    /**
     * Đăng ký nhận cập nhật Realtime từ Server (Observer Pattern).
     */
    private void setupRealtimeObserver() {
        realtimeListener = msg -> {
            if (msg == null || msg.getAction() == null) {
                return;
            }
            String action = msg.getAction().toUpperCase();
            if (Message.ACTION_BID_UPDATE.equals(action)) {
                Auction updated = msg.parsePayload(Auction.class);
                if (updated != null && updated.getId() != null) {
                    Platform.runLater(() -> {
                        for (int i = 0; i < auctionDataList.size(); i++) {
                            if (updated.getId().equals(auctionDataList.get(i).getId())) {
                                auctionDataList.set(i, updated);
                                auctionTableView.refresh();
                                showSuccess(lblMessage, "🔔 Phiên #" + updated.getId() + " vừa có lượt đặt giá mới: " + String.format("%,.0f VNĐ", updated.getMaxBid()));
                                break;
                            }
                        }
                    });
                }
            } else if (Message.ACTION_AUCTION_UPDATE.equals(action) || Message.ACTION_DELETE_AUCTION.equals(action)) {
                Platform.runLater(this::loadData);
            }
        };
        NetworkClient.getInstance().addListener(realtimeListener);
    }

    /**
     * Tải danh sách phiên đấu giá của Người bán từ Server (theo sơ đồ UML).
     * Đọc trực tiếp từ CSDL MySQL qua Socket (không dùng mock offline).
     */
    public void loadData() {
        NetworkClient.getInstance().sendRequestAsync(Message.request(Message.ACTION_GET_AUCTIONS, null))
                .thenAccept(response -> {
                    Platform.runLater(() -> {
                        if (response != null && response.isSuccess()) {
                            Type listType = new TypeToken<List<Auction>>() {}.getType();
                            List<Auction> serverAuctions = response.parsePayload(listType);
                            if (serverAuctions != null) {
                                User currentUser = SessionManager.getInstance().getCurrentUser();
                                Integer sellerId = (currentUser != null) ? currentUser.getId() : null;

                                auctionDataList.clear();
                                for (Auction a : serverAuctions) {
                                    // Lọc đúng phiên của Seller này (nếu đã có ID người bán), hoặc hiển thị tất cả
                                    if (sellerId == null || a.getSeller() == null || sellerId.equals(a.getSeller().getId())) {
                                        auctionDataList.add(a);
                                    }
                                }
                                auctionTableView.refresh();
                                showSuccess(lblMessage, "Đã đồng bộ " + auctionDataList.size() + " phiên từ CSDL Server!");
                                return;
                            }
                        }
                        showError(lblMessage, "Không thể tải danh sách: Máy chủ chưa có dữ liệu hoặc mất kết nối.");
                    });
                })
                .exceptionally(ex -> {
                    Platform.runLater(() -> showError(lblMessage, "Lỗi kết nối Server: " + ex.getMessage()));
                    return null;
                });
    }

    /**
     * Chọn ảnh sản phẩm từ máy tính và chuyển sang Base64 (theo sơ đồ UML).
     */
    @FXML
    public void handleSelectImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Chọn ảnh sản phẩm đấu giá");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Ảnh hợp lệ (*.png, *.jpg, *.jpeg)", "*.png", "*.jpg", "*.jpeg")
        );

        Stage stage = (Stage) rootPane.getScene().getWindow();
        File selectedFile = fileChooser.showOpenDialog(stage);

        if (selectedFile != null) {
            try {
                // Mã hóa ảnh sang Base64 qua ImageUtils
                this.encodedImage = ImageUtils.fileToBase64(selectedFile);
                imgPreview.setImage(ImageUtils.base64ToImage(this.encodedImage));
                showSuccess(lblMessage, "Đã tải ảnh: " + selectedFile.getName());
            } catch (Exception e) {
                showError(lblMessage, "Lỗi đọc file ảnh: " + e.getMessage());
            }
        }
    }

    // ==========================================
    // 2. XỬ LÝ SỰ KIỆN NÚT BẤM (onAction)
    // ==========================================

    /**
     * Thêm một phiên đấu giá mới gửi lên Server qua ACTION_CREATE_AUCTION (theo sơ đồ UML).
     *
     * @param event sự kiện ActionEvent
     */
    @FXML
    public void handleAddAuction(ActionEvent event) {
        String name = txtName.getText().trim();
        String category = cbCategory.getValue();
        String description = txtDescription.getText().trim();
        String startPriceStr = txtStartingPrice.getText().trim();
        String reservePriceStr = txtReservePrice.getText().trim();
        String durationStr = txtDurationMinutes.getText().trim();

        // 1. Validation kiểm tra rỗng các trường bắt buộc
        if (name.isEmpty() || startPriceStr.isEmpty() || durationStr.isEmpty()) {
            showError(lblMessage, "Vui lòng nhập tên, giá khởi điểm và thời lượng phiên!");
            return;
        }

        try {
            double startPrice = Double.parseDouble(startPriceStr);
            if (startPrice <= 0) {
                showError(lblMessage, "Giá khởi điểm phải lớn hơn 0!");
                return;
            }

            // 2. Kiểm tra giá bảo lưu: nếu để trống thì mặc định bằng giá khởi điểm; nếu có nhập thì phải >= giá khởi điểm
            double reservePrice = reservePriceStr.isEmpty() ? startPrice : Double.parseDouble(reservePriceStr);
            if (!ValidationUtils.isValidReservePrice(startPrice, reservePrice)) {
                showError(lblMessage, "Giá bảo lưu không được nhỏ hơn giá khởi điểm (" + String.format("%,.0f VNĐ", startPrice) + ")!");
                return;
            }

            // 3. Kiểm tra giới hạn thời lượng phiên: tối thiểu 5 phút, tối đa 30 ngày (43.200 phút)
            int durationMinutes = Integer.parseInt(durationStr);
            if (!ValidationUtils.isValidAuctionDuration(durationMinutes)) {
                showError(lblMessage, "Thời lượng phiên đấu giá phải từ 5 phút đến tối đa 30 ngày (43.200 phút)!");
                return;
            }

            // 4. Lấy thông tin Seller từ Session
            User currentUser = SessionManager.getInstance().getCurrentUser();
            Seller seller = (currentUser instanceof Seller) ? (Seller) currentUser : null;
            if (seller == null) {
                showError(lblMessage, "Vui lòng đăng nhập với tài khoản Người bán (SELLER) để thực hiện thao tác này!");
                return;
            }

            // 5. Dùng ItemFactory để khởi tạo sản phẩm đúng tính chất OOP (Ảnh là Optional)
            int tempId = AUTO_ID_GEN.incrementAndGet();
            Item item = ItemFactory.createItem(category, tempId, name, description, seller);
            if (this.encodedImage != null) {
                item.setImageData(this.encodedImage);
            }

            // 6. Tạo đối tượng Auction mới
            LocalDateTime startTime = LocalDateTime.now();
            LocalDateTime endTime = startTime.plusMinutes(durationMinutes);
            Auction newAuction = new Auction(tempId, name, item, seller, startPrice, reservePrice, startTime, endTime);

            // 7. Gửi lên AuctionServer qua TCP Socket
            NetworkClient.getInstance().sendRequestAsync(Message.request(Message.ACTION_CREATE_AUCTION, newAuction))
                    .thenAccept(response -> {
                        Platform.runLater(() -> {
                            if (response != null && response.isSuccess()) {
                                Auction created = response.parsePayload(Auction.class);
                                Auction toAdd = created != null ? created : newAuction;
                                auctionDataList.add(toAdd);
                                auctionTableView.refresh();
                                showSuccess(lblMessage, "Tạo phiên đấu giá thành công trên Server: #" + toAdd.getId());
                                handleClearForm(event);
                            } else {
                                String err = (response != null && response.getErrorMessage() != null)
                                        ? response.getErrorMessage()
                                        : "Không thể kết nối tới máy chủ (localhost:8888).";
                                showError(lblMessage, "Lỗi tạo phiên: " + err);
                            }
                        });
                    })
                    .exceptionally(ex -> {
                        Platform.runLater(() -> showError(lblMessage, "Lỗi kết nối: " + ex.getMessage()));
                        return null;
                    });

        } catch (NumberFormatException e) {
            showError(lblMessage, "Giá tiền và thời lượng phải là số hợp lệ!");
        } catch (Exception e) {
            showError(lblMessage, "Lỗi khi tạo phiên: " + e.getMessage());
        }
    }

    /**
     * Cập nhật thông tin phiên đấu giá đang chọn lên Server (theo sơ đồ UML).
     *
     * @param event sự kiện ActionEvent
     */
    @FXML
    public void handleUpdateAuction(ActionEvent event) {
        Auction selected = auctionTableView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError(lblMessage, "Vui lòng chọn một phiên trên bảng để cập nhật!");
            return;
        }

        String name = txtName.getText().trim();
        String startPriceStr = txtStartingPrice.getText().trim();

        if (name.isEmpty() || startPriceStr.isEmpty()) {
            showError(lblMessage, "Tên và giá khởi điểm không được để trống!");
            return;
        }

        try {
            double startPrice = Double.parseDouble(startPriceStr);
            if (startPrice <= 0) {
                showError(lblMessage, "Giá khởi điểm phải lớn hơn 0!");
                return;
            }

            selected.setName(name);
            selected.setStartingPrice(startPrice);

            if (selected.getItem() != null) {
                selected.getItem().setName(name);
                selected.getItem().setDescription(txtDescription.getText().trim());
                if (this.encodedImage != null) {
                    selected.getItem().setImageData(this.encodedImage);
                }
            }

            NetworkClient.getInstance().sendRequestAsync(Message.request(Message.ACTION_UPDATE_AUCTION, selected))
                    .thenAccept(response -> {
                        Platform.runLater(() -> {
                            auctionTableView.refresh();
                            showSuccess(lblMessage, "Cập nhật thành công phiên #" + selected.getId());
                        });
                    });

        } catch (NumberFormatException e) {
            showError(lblMessage, "Giá khởi điểm phải là số hợp lệ!");
        }
    }

    /**
     * Xóa một phiên đấu giá khỏi danh sách và Server (theo sơ đồ UML).
     *
     * @param event sự kiện ActionEvent
     */
    @FXML
    public void handleDeleteAuction(ActionEvent event) {
        Auction selected = auctionTableView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError(lblMessage, "Vui lòng chọn một phiên trên bảng để xóa!");
            return;
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("auctionId", selected.getId());
        payload.put("id", selected.getId());

        NetworkClient.getInstance().sendRequestAsync(Message.request(Message.ACTION_DELETE_AUCTION, payload))
                .thenAccept(response -> {
                    Platform.runLater(() -> {
                        auctionDataList.remove(selected);
                        auctionTableView.refresh();
                        handleClearForm(event);
                        showSuccess(lblMessage, "Đã xóa phiên đấu giá #" + selected.getId());
                    });
                });
    }

    /**
     * Làm mới form nhập liệu.
     */
    @FXML
    public void handleClearForm(ActionEvent event) {
        txtName.clear();
        txtDescription.clear();
        txtStartingPrice.clear();
        txtReservePrice.clear();
        txtDurationMinutes.clear();
        imgPreview.setImage(null);
        this.encodedImage = null;
        auctionTableView.getSelectionModel().clearSelection();
        clearMessage(lblMessage);
    }

    /**
     * Mở màn hình Lịch sử đấu giá & giao dịch.
     */
    @FXML
    public void handleOpenHistory(ActionEvent event) {
        cleanupListener();
        switchToScene(event, "/views/HistoryView.fxml", "Hệ thống Đấu giá - Lịch sử giao dịch & đấu giá");
    }

    /**
     * Đăng xuất và quay về màn hình Login.
     */
    @FXML
    public void handleLogout(ActionEvent event) {
        cleanupListener();
        SessionManager.getInstance().clear();
        switchToScene(event, "/views/LoginView.fxml", "Hệ thống Đấu giá Trực tuyến - Đăng nhập");
    }

    private void cleanupListener() {
        if (realtimeListener != null) {
            NetworkClient.getInstance().removeListener(realtimeListener);
            realtimeListener = null;
        }
    }

    /**
     * Điền thông tin phiên đấu giá được chọn lên form bên phải.
     */
    private void populateFormWithAuction(Auction auction) {
        if (auction == null) return;
        txtName.setText(auction.getName() != null ? auction.getName() : "");
        txtStartingPrice.setText(String.valueOf(auction.getStartingPrice()));
        txtReservePrice.setText(String.valueOf(auction.getReservePrice()));

        if (auction.getItem() != null) {
            txtDescription.setText(auction.getItem().getDescription() != null ? auction.getItem().getDescription() : "");
            if (auction.getItem().getCategory() != null) {
                cbCategory.setValue(auction.getItem().getCategory().toUpperCase());
            }

            // Hiển thị ảnh nếu có
            String imgData = auction.getItem().getImageData();
            if (imgData != null && !imgData.isEmpty()) {
                this.encodedImage = imgData;
                imgPreview.setImage(ImageUtils.base64ToImage(imgData));
            } else {
                imgPreview.setImage(null);
                this.encodedImage = null;
            }
        }
    }

    public ObservableList<Auction> getAuctionDataList() {
        return auctionDataList;
    }
}
