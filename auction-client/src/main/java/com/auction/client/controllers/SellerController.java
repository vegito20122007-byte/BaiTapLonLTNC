package com.auction.client.controllers;

import com.auction.client.session.SessionManager;
import com.auction.client.util.ImageUtils;
import com.auction.model.*;
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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicInteger;

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

    // ==========================================
    // 2. THUỘC TÍNH THEO SƠ ĐỒ UML
    // ==========================================
    private ObservableList<Auction> auctionDataList = FXCollections.observableArrayList();
    private String encodedImage;
    private static final AtomicInteger AUTO_ID_GEN = new AtomicInteger(100);

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    /**
     * Khởi tạo bảng dữ liệu và các bộ chọn khi mở màn hình Seller.
     */
    @FXML
    public void initialize() {
        // Cấu hình các danh mục hàng hóa theo ItemFactory
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

        // Tải dữ liệu ban đầu
        loadData();
    }

    /**
     * Tải danh sách phiên đấu giá của Người bán (theo sơ đồ UML).
     */
    public void loadData() {
        if (auctionDataList.isEmpty()) {
            // Dữ liệu mẫu ban đầu để Seller kiểm tra giao diện trực quan
            User currentUser = SessionManager.getInstance().getCurrentUser();
            Seller seller = (currentUser instanceof Seller) ? (Seller) currentUser : new Seller(1, "seller_user", "pass");

            Item sampleItem1 = ItemFactory.createItem("ART", 1, "Tranh Sơn Dầu Phố Cổ", "Tranh sơn dầu vẽ Hà Nội thập niên 90", seller);
            Auction sampleAuction1 = new Auction(1, "Đấu giá Tranh Phố Cổ", sampleItem1, seller, 5000000.0, 7000000.0,
                    LocalDateTime.now(), LocalDateTime.now().plusDays(2));

            Item sampleItem2 = ItemFactory.createItem("VEHICLE", 2, "Xe Máy Honda Cub 50", "Xe nguyên bản màu xanh ngọc", seller);
            Auction sampleAuction2 = new Auction(2, "Đấu giá Honda Cub Cổ", sampleItem2, seller, 15000000.0, 18000000.0,
                    LocalDateTime.now(), LocalDateTime.now().plusDays(5));

            auctionDataList.addAll(sampleAuction1, sampleAuction2);
        }

        auctionTableView.setItems(auctionDataList);
        auctionTableView.refresh();
        showSuccess(lblMessage, "Đã cập nhật danh sách phiên đấu giá!");
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

                // Hiển thị xem trước ảnh ngay trên ImageView
                imgPreview.setImage(ImageUtils.base64ToImage(this.encodedImage));
                showSuccess(lblMessage, "Đã chọn ảnh: " + selectedFile.getName());
            } catch (Exception e) {
                showError(lblMessage, "Lỗi đọc file ảnh: " + e.getMessage());
            }
        }
    }

    /**
     * Thêm một phiên đấu giá mới (theo sơ đồ UML).
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

        // 1. Validation kiểm tra rỗng
        if (name.isEmpty() || startPriceStr.isEmpty() || durationStr.isEmpty()) {
            showError(lblMessage, "Vui lòng nhập tên, giá khởi điểm và thời lượng phiên!");
            return;
        }

        try {
            double startPrice = Double.parseDouble(startPriceStr);
            double reservePrice = reservePriceStr.isEmpty() ? startPrice : Double.parseDouble(reservePriceStr);
            int durationMinutes = Integer.parseInt(durationStr);

            if (startPrice <= 0 || durationMinutes <= 0) {
                showError(lblMessage, "Giá và thời lượng phải lớn hơn 0!");
                return;
            }

            // 2. Lấy thông tin Seller từ Session
            User currentUser = SessionManager.getInstance().getCurrentUser();
            Seller seller = (currentUser instanceof Seller) ? (Seller) currentUser : new Seller(1, "seller_user", "pass");

            // 3. Dùng ItemFactory để khởi tạo sản phẩm đúng tính chất OOP
            int newItemId = AUTO_ID_GEN.incrementAndGet();
            Item item = ItemFactory.createItem(category, newItemId, name, description, seller);
            if (this.encodedImage != null) {
                item.setImageData(this.encodedImage);
            }

            // 4. Tạo đối tượng Auction mới
            int newAuctionId = newItemId;
            LocalDateTime startTime = LocalDateTime.now();
            LocalDateTime endTime = startTime.plusMinutes(durationMinutes);

            Auction newAuction = new Auction(newAuctionId, name, item, seller, startPrice, reservePrice, startTime, endTime);

            // 5. Thêm vào danh sách quan sát
            auctionDataList.add(newAuction);
            auctionTableView.refresh();

            showSuccess(lblMessage, "Tạo phiên đấu giá thành công: #" + newAuctionId);
            handleClearForm(event);

        } catch (NumberFormatException e) {
            showError(lblMessage, "Giá tiền và thời lượng phải là số hợp lệ!");
        } catch (Exception e) {
            showError(lblMessage, "Lỗi khi tạo phiên: " + e.getMessage());
        }
    }

    /**
     * Cập nhật thông tin phiên đấu giá đang chọn (theo sơ đồ UML).
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
            selected.setName(name);
            selected.setStartingPrice(startPrice);

            if (selected.getItem() != null) {
                selected.getItem().setName(name);
                selected.getItem().setDescription(txtDescription.getText().trim());
                if (this.encodedImage != null) {
                    selected.getItem().setImageData(this.encodedImage);
                }
            }

            auctionTableView.refresh();
            showSuccess(lblMessage, "Cập nhật thành công phiên #" + selected.getId());
        } catch (NumberFormatException e) {
            showError(lblMessage, "Giá khởi điểm phải là số hợp lệ!");
        }
    }

    /**
     * Xóa một phiên đấu giá khỏi danh sách (theo sơ đồ UML).
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

        auctionDataList.remove(selected);
        auctionTableView.refresh();
        handleClearForm(event);
        showSuccess(lblMessage, "Đã xóa phiên đấu giá #" + selected.getId());
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
        switchToScene(event, "/views/HistoryView.fxml", "Hệ thống Đấu giá - Lịch sử giao dịch & đấu giá");
    }

    /**
     * Đăng xuất và quay về màn hình Login.
     */
    @FXML
    public void handleLogout(ActionEvent event) {
        SessionManager.getInstance().clear();
        switchToScene(event, "/views/LoginView.fxml", "Hệ thống Đấu giá Trực tuyến - Đăng nhập");
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
}
