package com.auction.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Quản lý phiên đấu giá trực tuyến.
 * Kế thừa Entity, chứa thông tin sản phẩm, người bán, mức giá cao nhất,
 * danh sách lịch sử đặt giá và các cấu hình Auto-Bid.
 */
public class Auction extends Entity {
    private static final long serialVersionUID = 1L;

    /** Tên hoặc tiêu đề của phiên đấu giá. */
    private String name;

    /** Giá khởi điểm của sản phẩm. */
    private double startingPrice;

    /** Sản phẩm được đưa ra đấu giá (Art, Electronics hoặc Vehicle). */
    private Item item;

    /** Người bán sở hữu phiên đấu giá này. */
    private User seller;

    /** Mức giá cao nhất hiện tại đã được trả (maxBid). */
    private double maxBid;

    /** Người đang trả giá cao nhất hiện thời. */
    private User currentWinner;

    /** Trạng thái hiện tại của phiên (OPEN, RUNNING, FINISHED, PAID, CANCELED). */
    private Status status;

    /** Thời gian bắt đầu mở nhận giá. */
    private LocalDateTime startTime;

    /** Thời gian kết thúc phiên đấu giá. */
    private LocalDateTime endTime;

    /** Giá sàn tối thiểu người bán kỳ vọng (nếu không đạt sẽ không bán). */
    private double reservePrice;

    /** Lịch sử các lượt đặt giá hợp lệ. */
    private List<BidTransaction> bids = new ArrayList<>();

    /** Danh sách các cấu hình Auto-Bid đã đăng ký trong phiên. */
    private List<AutoBidConfig> autoBidList = new ArrayList<>();

    public Auction() {
        super();
        this.status = Status.OPEN;
    }

    public Auction(Integer id, String name, Item item, User seller, double startingPrice,
                   double reservePrice, LocalDateTime startTime, LocalDateTime endTime) {
        super(id);
        this.name = name;
        this.item = item;
        this.seller = seller;
        this.startingPrice = startingPrice;
        this.reservePrice = reservePrice;
        this.maxBid = startingPrice;
        this.status = Status.OPEN;
        this.startTime = startTime;
        this.endTime = endTime;
        this.bids = new ArrayList<>();
        this.autoBidList = new ArrayList<>();
    }

    /**
     * Thực hiện đặt giá cho phiên đấu giá.
     * Kiểm tra tính hợp lệ của người đặt và giá đấu theo quy tắc nghiệp vụ.
     *
     * @param bidder Người đặt giá
     * @param amount Số tiền muốn đặt
     * @throws IllegalArgumentException nếu giá không hợp lệ hoặc người bán tự bid
     * @throws IllegalStateException nếu phiên chưa mở hoặc đã kết thúc
     */
    public synchronized void placeBid(User bidder, double amount) {
        updateStatus();

        if (status != Status.RUNNING) {
            throw new IllegalStateException("Phiên đấu giá hiện không mở để nhận giá (Trạng thái: " + status + ")");
        }

        if (bidder == null || bidder.getId() == null) {
            throw new IllegalArgumentException("Thông tin người đặt giá không hợp lệ");
        }

        // Quy tắc: Người bán không được tự đặt giá sản phẩm của chính mình
        if (seller != null && seller.getId().equals(bidder.getId())) {
            throw new IllegalArgumentException("Người bán không thể tự đặt giá cho sản phẩm của mình");
        }

        // Quy tắc: Nếu chưa có ai bid thì giá phải >= giá khởi điểm; nếu đã có bid thì phải > giá cao nhất hiện tại
        double currentPrice = getCurrentPrice();
        if (bids.isEmpty()) {
            if (amount < startingPrice) {
                throw new IllegalArgumentException(String.format("Giá đặt (%,.0f) phải lớn hơn hoặc bằng giá khởi điểm (%,.0f)", amount, startingPrice));
            }
        } else {
            if (amount <= currentPrice) {
                throw new IllegalArgumentException(String.format("Giá đặt (%,.0f) phải cao hơn mức giá hiện tại (%,.0f)", amount, currentPrice));
            }
        }

        // Cập nhật người dẫn đầu và mức giá cao nhất
        this.maxBid = amount;
        this.currentWinner = bidder;

        // Lưu vết lịch sử giao dịch đặt giá
        BidTransaction transaction = new BidTransaction(
                null, this.id, amount, bidder, LocalDateTime.now()
        );
        this.bids.add(transaction);
    }

    /**
     * Cập nhật trạng thái phiên dựa theo thời gian thực:
     * - Trước startTime: OPEN
     * - Từ startTime đến endTime: RUNNING
     * - Sau endTime: FINISHED (kích hoạt finalProcess)
     */
    public void updateStatus() {
        LocalDateTime now = LocalDateTime.now();

        if (status == Status.PAID || status == Status.CANCELED) {
            return;
        }

        if (status == Status.OPEN && startTime != null && !now.isBefore(startTime) && (endTime == null || now.isBefore(endTime))) {
            this.status = Status.RUNNING;
        } else if ((status == Status.OPEN || status == Status.RUNNING) && endTime != null && now.isAfter(endTime)) {
            finalProcess();
        }
    }

    /**
     * Xử lý kết thúc phiên đấu giá:
     * Chốt trạng thái FINISHED và kiểm tra điều kiện giá sàn (reservePrice).
     */
    public void finalProcess() {
        this.status = Status.FINISHED;

        // Nếu có giá sàn và mức giá chốt chưa đạt giá sàn -> Hủy phiên
        if (reservePrice > 0 && maxBid < reservePrice) {
            this.currentWinner = null;
            this.status = Status.CANCELED;
        }
    }

    /**
     * Đăng ký cấu hình đấu giá tự động (Auto-Bid) vào phiên.
     */
    public void registerAutoBid(int userId, double maxBidLimit, double increment) {
        // Nếu user này đã đăng ký trước đó, cập nhật cấu hình mới
        for (AutoBidConfig config : autoBidList) {
            if (config.getUserId() == userId) {
                config.setMaxBidLimit(maxBidLimit);
                config.setIncrement(increment);
                return;
            }
        }
        // Chưa có thì tạo mới thêm vào danh sách
        autoBidList.add(new AutoBidConfig(this.id != null ? this.id : 0, userId, maxBidLimit, increment));
    }

    /**
     * Lấy giá hiện tại của phiên đấu giá.
     */
    public double getCurrentPrice() {
        return (bids == null || bids.isEmpty()) ? startingPrice : maxBid;
    }

    /**
     * Lấy người thắng cuộc hiện tại.
     */
    public User getWinner() {
        return currentWinner;
    }

    // --- GETTERS & SETTERS ---
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public double getStartingPrice() { return startingPrice; }
    public void setStartingPrice(double startingPrice) { this.startingPrice = startingPrice; }

    public Item getItem() { return item; }
    public void setItem(Item item) { this.item = item; }

    public User getSeller() { return seller; }
    public void setSeller(User seller) { this.seller = seller; }

    public double getMaxBid() { return maxBid; }
    public void setMaxBid(double maxBid) { this.maxBid = maxBid; }

    public User getCurrentWinner() { return currentWinner; }
    public void setCurrentWinner(User currentWinner) { this.currentWinner = currentWinner; }

    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }

    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }

    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }

    public double getReservePrice() { return reservePrice; }
    public void setReservePrice(double reservePrice) { this.reservePrice = reservePrice; }

    public List<BidTransaction> getBids() { return bids; }
    public void setBids(List<BidTransaction> bids) { this.bids = bids; }

    public List<AutoBidConfig> getAutoBidList() { return autoBidList; }
    public void setAutoBidList(List<AutoBidConfig> autoBidList) { this.autoBidList = autoBidList; }
}