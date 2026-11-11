package com.auction.client.controllers;

import com.auction.client.network.Message;
import com.auction.client.network.NetworkClient;
import com.auction.client.session.SessionManager;
import com.auction.client.util.ImageUtils;
import com.auction.model.Auction;
import com.auction.model.Item;
import com.auction.model.User;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.util.Duration;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Controller điều khiển Phòng đấu giá trực tiếp (BidderView.fxml).
 * Tính năng hoàn chỉnh:
 * 1. Tự động thêm dấu phẩy (,) sau mỗi 3 chữ số khi gõ tiền (ví dụ: 9,000,000).
 * 2. Hiển thị rõ ràng Bước giá tối thiểu do Seller quy định.
 * 3. Chặn troll giá (bắt buộc đặt >= giá hiện tại + bước giá).
 * 4. Đồng hồ đếm ngược động theo giờ kết thúc của Seller + Anti-sniping (+60s).
 * 5. Vẽ biểu đồ biến động giá LineChart thời gian thực.
 * 6. Tích hợp NetworkClient & Observer Pattern đón nhận ACTION_BID_UPDATE từ Server.
 */
public class BidderController extends BaseController {

    @FXML private ImageView imgProduct;
    @FXML private Label lblItemName;
    @FXML private Label lblCategory;
    @FXML private Label lblDescription;
    @FXML private Label lblStartingPrice;
    @FXML private Label lblMinIncrement;
    @FXML private Label lblHeaderIncrement;
    @FXML private Label lblMinNextBidHint;
    @FXML private Label lblAuctionStatus;

    @FXML private Label lblCountdown;
    @FXML private Label lblCurrentPrice;
    @FXML private Label lblWinner;

    @FXML private TextField txtBidAmount;
    @FXML private Button btnPlaceBid;
    @FXML private Button btnQuick1;
    @FXML private Button btnQuick2;
    @FXML private Button btnQuick3;

    @FXML private CheckBox chkAutoBid;
    @FXML private TextField txtMaxBidLimit;
    @FXML private TextField txtIncrement;

    @FXML private LineChart<String, Number> bidChart;

    // --- BIẾN QUẢN LÝ ---
    private Auction currentAuction;
    private XYChart.Series<String, Number> priceSeries;
    private double currentPrice = 45000000.0;
    private double minIncrement = 100000.0; // Bước giá tối thiểu mặc định (100,000 VNĐ)
    private long remainingSeconds = 0;
    private Timeline countdownTimeline;
    private LocalDateTime endTimeOfSeller;

    // Listener realtime nhận broadcast từ Server
    private Consumer<Message> realtimeListener;

    // Định dạng dấu phẩy phân cách hàng nghìn (ví dụ: 9,000,000)
    private final DecimalFormat currencyFormatter = new DecimalFormat("#,###", new DecimalFormatSymbols(Locale.US));
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");
    private boolean isFormattingText = false;

    @FXML
    public void initialize() {
        // 1. Khởi tạo biểu đồ đường LineChart
        priceSeries = new XYChart.Series<>();
        priceSeries.setName("Diễn biến giá");
        if (bidChart != null) {
            bidChart.getData().clear();
            bidChart.getData().add(priceSeries);
        }

        // 2. Kích hoạt tính năng TỰ ĐỘNG THÊM DẤU PHẨY (9,000,000) khi gõ tiền
        setupCurrencyFormatting(txtBidAmount);
        setupCurrencyFormatting(txtMaxBidLimit);
        setupCurrencyFormatting(txtIncrement);

        // 3. Giả định phiên kết thúc sau 10 phút tính từ lúc mở phòng (nếu chưa truyền Auction cụ thể)
        LocalDateTime defaultEndTime = LocalDateTime.now().plusMinutes(10);
        setupCountdownFromSeller(defaultEndTime);

        // 4. Khởi tạo giá ban đầu và hiển thị
        updatePriceDisplay(currentPrice, "Chưa có ai đặt");
        updateIncrementUI();

        priceSeries.getData().add(new XYChart.Data<>(LocalTime.now().format(timeFormatter), currentPrice));

        // 5. Đăng ký Observer nhận gói tin realtime từ Server
        setupRealtimeObserver();
    }

    /**
     * Đăng ký nhận thông báo phát thanh (Broadcast) từ Server qua Observer Pattern.
     */
    private void setupRealtimeObserver() {
        realtimeListener = msg -> {
            if (msg == null || msg.getAction() == null) {
                return;
            }

            if (Message.ACTION_BID_UPDATE.equalsIgnoreCase(msg.getAction())) {
                try {
                    Auction updated = msg.parsePayload(Auction.class);
                    if (updated != null && currentAuction != null && updated.getId() != null
                            && updated.getId().equals(currentAuction.getId())) {
                        Platform.runLater(() -> {
                            this.currentAuction = updated;
                            double newPrice = updated.getMaxBid();
                            String winner = (updated.getCurrentWinner() != null)
                                    ? updated.getCurrentWinner().getUsername() : "Người dùng khác";
                            executeBidSuccess(newPrice, winner + " (Vừa đặt)");

                            // Nếu Server đã gia hạn Anti-sniping
                            if (updated.getEndTime() != null) {
                                setupCountdownFromSeller(updated.getEndTime());
                            }
                        });
                    }
                } catch (Exception e) {
                    System.err.println("[BidderController] Lỗi xử lý ACTION_BID_UPDATE: " + e.getMessage());
                }
            }
        };
        NetworkClient.getInstance().addListener(realtimeListener);
    }

    /**
     * Nạp dữ liệu một đối tượng Auction cụ thể từ MainController hoặc Server.
     */
    public void setAuction(Auction auction) {
        if (auction == null) return;
        this.currentAuction = auction;

        String name = auction.getName();
        double startPrice = auction.getStartingPrice();
        double currPrice = auction.getMaxBid() > 0 ? auction.getMaxBid() : startPrice;
        LocalDateTime end = auction.getEndTime();

        if (auction.getItem() != null) {
            Item item = auction.getItem();
            if (item.getDescription() != null && lblDescription != null) {
                lblDescription.setText(item.getDescription());
            }
            if (item.getCategory() != null && lblCategory != null) {
                lblCategory.setText("Danh mục: " + item.getCategory());
            }
            if (item.getImageData() != null && !item.getImageData().isBlank() && imgProduct != null) {
                try {
                    imgProduct.setImage(ImageUtils.base64ToImage(item.getImageData()));
                } catch (Exception ignored) {
                }
            }
        }

        String winner = auction.getCurrentWinner() != null ? auction.getCurrentWinner().getUsername() : "Chưa có ai đặt";
        setAuctionInfo(name, startPrice, currPrice, this.minIncrement, end);
        if (lblWinner != null) {
            lblWinner.setText(winner);
        }
    }

    /**
     * Tự động thêm dấu phẩy sau mỗi 3 số khi người dùng gõ (ví dụ: 9,000,000).
     */
    private void setupCurrencyFormatting(TextField textField) {
        if (textField == null) return;

        textField.textProperty().addListener((obs, oldValue, newValue) -> {
            if (isFormattingText || newValue == null) return;

            int caretPos = textField.getCaretPosition();
            int digitsBeforeCaret = 0;
            for (int i = 0; i < Math.min(caretPos, newValue.length()); i++) {
                if (Character.isDigit(newValue.charAt(i))) {
                    digitsBeforeCaret++;
                }
            }

            String digits = newValue.replaceAll("[^0-9]", "");
            if (digits.isEmpty()) {
                isFormattingText = true;
                textField.setText("");
                isFormattingText = false;
                return;
            }

            try {
                long val = Long.parseLong(digits);
                String formatted = currencyFormatter.format(val);

                if (!newValue.equals(formatted)) {
                    isFormattingText = true;
                    textField.setText(formatted);

                    int newCaret = 0;
                    int countedDigits = 0;
                    while (newCaret < formatted.length() && countedDigits < digitsBeforeCaret) {
                        if (Character.isDigit(formatted.charAt(newCaret))) {
                            countedDigits++;
                        }
                        newCaret++;
                    }

                    textField.positionCaret(newCaret);
                    isFormattingText = false;
                }
            } catch (NumberFormatException ignored) {
            }
        });
    }

    /**
     * Hàm nhận dữ liệu trực tiếp từ Seller/Server.
     */
    public void setAuctionInfo(String itemName, double startPrice, double currentPrice, double minIncrement, LocalDateTime endTime) {
        if (lblItemName != null && itemName != null) lblItemName.setText(itemName);
        if (lblStartingPrice != null) lblStartingPrice.setText(currencyFormatter.format(startPrice) + " VNĐ");
        this.currentPrice = currentPrice;
        this.minIncrement = minIncrement > 0 ? minIncrement : 100000.0;

        updatePriceDisplay(this.currentPrice, "Chưa có ai đặt");
        if (endTime != null) {
            setupCountdownFromSeller(endTime);
        }
        updateIncrementUI();

        if (priceSeries != null) {
            priceSeries.getData().clear();
            priceSeries.getData().add(new XYChart.Data<>(LocalTime.now().format(timeFormatter), currentPrice));
        }
    }

    public void setMinIncrement(double increment) {
        if (increment > 0) {
            this.minIncrement = increment;
            updateIncrementUI();
        }
    }

    private void updateIncrementUI() {
        String incStr = currencyFormatter.format(minIncrement) + " VNĐ";
        double minValidBid = currentPrice + minIncrement;
        String minBidStr = currencyFormatter.format(minValidBid) + " VNĐ";

        if (lblMinIncrement != null) lblMinIncrement.setText(incStr);
        if (lblHeaderIncrement != null) lblHeaderIncrement.setText("+" + incStr);
        if (lblMinNextBidHint != null) lblMinNextBidHint.setText("➤ Giá hợp lệ tối thiểu tiếp theo: " + minBidStr);
        if (txtBidAmount != null) txtBidAmount.setPromptText("Tối thiểu: " + currencyFormatter.format(minValidBid) + "...");

        if (btnQuick1 != null) btnQuick1.setText("+" + currencyFormatter.format(minIncrement) + "đ");
        if (btnQuick2 != null) btnQuick2.setText("+" + currencyFormatter.format(minIncrement * 2) + "đ");
        if (btnQuick3 != null) btnQuick3.setText("+" + currencyFormatter.format(minIncrement * 5) + "đ");
    }

    public void setupCountdownFromSeller(LocalDateTime sellerEndTime) {
        this.endTimeOfSeller = sellerEndTime;
        LocalDateTime now = LocalDateTime.now();
        if (sellerEndTime.isAfter(now)) {
            this.remainingSeconds = java.time.Duration.between(now, sellerEndTime).getSeconds();
        } else {
            this.remainingSeconds = 0;
        }

        if (countdownTimeline != null) {
            countdownTimeline.stop();
        }

        countdownTimeline = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
            if (remainingSeconds > 0) {
                remainingSeconds--;
                long hours = remainingSeconds / 3600;
                long minutes = (remainingSeconds % 3600) / 60;
                long seconds = remainingSeconds % 60;

                if (lblCountdown != null) {
                    if (hours > 0) {
                        lblCountdown.setText(String.format("%02d:%02d:%02d", hours, minutes, seconds));
                    } else {
                        lblCountdown.setText(String.format("00:%02d:%02d", minutes, seconds));
                    }
                }
            } else {
                countdownTimeline.stop();
                if (lblCountdown != null) lblCountdown.setText("00:00:00");
                if (lblAuctionStatus != null) {
                    lblAuctionStatus.setText("ĐÃ KẾT THÚC");
                    lblAuctionStatus.setStyle("-fx-background-color: #d93025; -fx-padding: 5 12; -fx-background-radius: 12;");
                }
                if (btnPlaceBid != null) btnPlaceBid.setDisable(true);
            }
        }));
        countdownTimeline.setCycleCount(Timeline.INDEFINITE);
        countdownTimeline.play();
    }

    public void extendEndTime(int additionalSeconds) {
        this.remainingSeconds += additionalSeconds;
        System.out.println(">> [ANTI-SNIPING] Gia hạn thêm " + additionalSeconds + " giây!");
    }

    // --- CÁC HÀM ĐẶT GIÁ ---

    @FXML
    public void handlePlaceBid(ActionEvent event) {
        String rawInput = txtBidAmount.getText().replaceAll("[^0-9]", "");
        if (rawInput.isEmpty()) {
            showAlert("Lỗi", "Vui lòng nhập mức giá bạn muốn đặt!");
            return;
        }

        try {
            double bidValue = Double.parseDouble(rawInput);
            double minValidBid = currentPrice + minIncrement;

            // KIỂM TRA BƯỚC GIÁ TỐI THIỂU (CHỐNG TROLL GIÁ BATMAN)
            if (bidValue < minValidBid) {
                showAlert("Giá không hợp lệ", String.format(
                        "Mức giá bạn đặt (%s VNĐ) chưa đạt bước giá tối thiểu!\n\n" +
                                "• Giá cao nhất hiện tại: %s VNĐ\n" +
                                "• Bước giá tối thiểu (Seller): %s VNĐ\n" +
                                "➤ Mức giá hợp lệ bạn phải đặt từ: %s VNĐ trở lên!",
                        currencyFormatter.format(bidValue),
                        currencyFormatter.format(currentPrice),
                        currencyFormatter.format(minIncrement),
                        currencyFormatter.format(minValidBid)
                ));
                return;
            }

            if (btnPlaceBid != null) {
                btnPlaceBid.setDisable(true);
            }

            User currentUser = SessionManager.getInstance().getCurrentUser();
            int bidderId = (currentUser != null && currentUser.getId() != null) ? currentUser.getId() : 3;
            int auctionId = (currentAuction != null && currentAuction.getId() != null) ? currentAuction.getId() : 1;

            Map<String, Object> payload = new HashMap<>();
            payload.put("auctionId", auctionId);
            payload.put("bidderId", bidderId);
            payload.put("amount", bidValue);
            payload.put("bidAmount", bidValue);

            Message request = Message.request(Message.ACTION_PLACE_BID, payload);

            NetworkClient.getInstance().sendRequestAsync(request)
                    .thenAccept(response -> {
                        Platform.runLater(() -> {
                            if (btnPlaceBid != null) {
                                btnPlaceBid.setDisable(false);
                            }

                            if (response != null && response.isSuccess()) {
                                Auction updated = response.parsePayload(Auction.class);
                                if (updated != null) {
                                    this.currentAuction = updated;
                                    if (updated.getEndTime() != null) {
                                        setupCountdownFromSeller(updated.getEndTime());
                                    }
                                }
                                String username = currentUser != null ? currentUser.getUsername() : "Bạn";
                                executeBidSuccess(bidValue, username + " (Đang dẫn đầu)");
                                txtBidAmount.clear();

                                // Kích hoạt Anti-sniping cảnh báo
                                if (remainingSeconds > 0 && remainingSeconds <= 30) {
                                    extendEndTime(60);
                                    showAlert("Anti-sniping", "Có lượt đặt giá ở phút chót! Phiên đấu giá được tự động gia hạn thêm 60 giây!");
                                } else {
                                    showAlert("Thành công", "Bạn đã đặt giá thành công: " + currencyFormatter.format(bidValue) + " VNĐ!");
                                }
                            } else {
                                String errMsg = (response != null && response.getErrorMessage() != null)
                                        ? response.getErrorMessage()
                                        : "Không thể kết nối tới máy chủ (localhost:8888).";
                                showAlert("Đặt giá thất bại", errMsg);
                            }
                        });
                    })
                    .exceptionally(ex -> {
                        Platform.runLater(() -> {
                            if (btnPlaceBid != null) {
                                btnPlaceBid.setDisable(false);
                            }
                            showAlert("Lỗi", "Lỗi gửi yêu cầu đặt giá: " + ex.getMessage());
                        });
                        return null;
                    });

        } catch (NumberFormatException e) {
            showAlert("Lỗi", "Số tiền nhập vào không hợp lệ!");
        }
    }

    @FXML
    public void handleQuickBid1(ActionEvent event) {
        placeQuickBid(minIncrement);
    }

    @FXML
    public void handleQuickBid2(ActionEvent event) {
        placeQuickBid(minIncrement * 2);
    }

    @FXML
    public void handleQuickBid3(ActionEvent event) {
        placeQuickBid(minIncrement * 5);
    }

    @FXML public void handleQuickBid100k(ActionEvent event) { handleQuickBid1(event); }
    @FXML public void handleQuickBid500k(ActionEvent event) { handleQuickBid2(event); }
    @FXML public void handleQuickBid1M(ActionEvent event) { handleQuickBid3(event); }

    private void placeQuickBid(double amountToAdd) {
        double newPrice = currentPrice + amountToAdd;
        txtBidAmount.setText(currencyFormatter.format((long) newPrice));
        handlePlaceBid(null);
    }

    private void executeBidSuccess(double newPrice, String winnerName) {
        this.currentPrice = newPrice;
        updatePriceDisplay(newPrice, winnerName);
        updateIncrementUI();

        String nowTime = LocalTime.now().format(timeFormatter);
        if (priceSeries != null) {
            priceSeries.getData().add(new XYChart.Data<>(nowTime, newPrice));
        }
    }

    private void updatePriceDisplay(double price, String winner) {
        if (lblCurrentPrice != null) lblCurrentPrice.setText(currencyFormatter.format(price) + " VNĐ");
        if (lblWinner != null) lblWinner.setText(winner);
    }

    @FXML
    public void activateAutoBid(ActionEvent event) {
        String maxRaw = txtMaxBidLimit.getText().replaceAll("[^0-9]", "");
        String incRaw = txtIncrement.getText().replaceAll("[^0-9]", "");
        if (maxRaw.isEmpty() || incRaw.isEmpty()) {
            showAlert("Thông báo", "Vui lòng nhập đầy đủ Giá trần tối đa và Bước giá!");
            return;
        }
        chkAutoBid.setSelected(true);
        showAlert("Thành công", "Đã kích hoạt chế độ Đấu giá tự động (Auto-Bid) thành công!");
    }

    @FXML
    public void handleCancelAutoBid(ActionEvent event) {
        chkAutoBid.setSelected(false);
        txtMaxBidLimit.clear();
        txtIncrement.clear();
        showAlert("Thông báo", "Đã hủy chế độ Đấu giá tự động.");
    }

    @FXML
    public void handleBackToMain(ActionEvent event) {
        cleanupListener();
        if (countdownTimeline != null) {
            countdownTimeline.stop();
        }
        switchToScene(event, "/views/MainView.fxml", "Hệ thống Đấu giá Trực tuyến - Trang chủ");
    }

    private void cleanupListener() {
        if (realtimeListener != null) {
            NetworkClient.getInstance().removeListener(realtimeListener);
            realtimeListener = null;
        }
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    public Auction getCurrentAuction() {
        return currentAuction;
    }

    public double getCurrentPrice() {
        return currentPrice;
    }
}
