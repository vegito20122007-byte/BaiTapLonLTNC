package com.auction.model;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Cấu hình chế độ đấu giá tự động (Auto-Bidding) cho một người dùng.
 * Phục vụ hàng đợi ưu tiên PriorityQueue (Tính năng nâng cao +0.5đ).
 */
public class AutoBidConfig implements Serializable, Comparable<AutoBidConfig> {
    private static final long serialVersionUID = 1L;

    /** ID của phiên đấu giá mà bot đang tham gia. */
    private int auctionId;

    /** ID người dùng đăng ký auto-bid. */
    private int userId;

    /** Đối tượng người dùng đặt auto-bid. */
    private User user;

    /** Giá tối đa sẵn sàng trả (maxBidLimit). */
    private double maxBidLimit;

    /** Bước giá tăng thêm mỗi khi có đối thủ vượt mặt (increment). */
    private double increment;

    /** Thời điểm đăng ký bot (phục vụ ưu tiên FIFO khi bằng mức giá). */
    private LocalDateTime registeredTime;

    /** Constructor rỗng phục vụ JSON Deserialization (Gson/Network). */
    public AutoBidConfig() {
        this.registeredTime = LocalDateTime.now();
    }

    public AutoBidConfig(int auctionId, int userId, double maxBidLimit, double increment) {
        this.auctionId = auctionId;
        this.userId = userId;
        this.maxBidLimit = maxBidLimit;
        this.increment = increment;
        this.registeredTime = LocalDateTime.now();
    }

    public AutoBidConfig(int auctionId, int userId, User user, double maxBidLimit, double increment) {
        this.auctionId = auctionId;
        this.userId = userId;
        this.user = user;
        this.maxBidLimit = maxBidLimit;
        this.increment = increment;
        this.registeredTime = LocalDateTime.now();
    }

    /**
     * Logic so sánh phục vụ PriorityQueue trên AuctionService (Server):
     * 1. Ưu tiên bot có giá trần (maxBidLimit) cao hơn trước.
     * 2. Nếu mức giá trần bằng nhau: Ưu tiên bot đăng ký trước (FIFO theo registeredTime).
     */
    @Override
    public int compareTo(AutoBidConfig other) {
        if (other == null) return 1;

        // So sánh theo maxBidLimit giảm dần (người trả cao hơn đứng trước)
        int priceCompare = Double.compare(other.maxBidLimit, this.maxBidLimit);
        if (priceCompare != 0) {
            return priceCompare;
        }

        // Nếu maxBidLimit bằng nhau, ưu tiên ai đăng ký sớm hơn (tăng dần)
        if (this.registeredTime != null && other.registeredTime != null) {
            return this.registeredTime.compareTo(other.registeredTime);
        }

        return 0;
    }

    // --- GETTERS & SETTERS ---
    public int getAuctionId() { return auctionId; }
    public void setAuctionId(int auctionId) { this.auctionId = auctionId; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public double getMaxBidLimit() { return maxBidLimit; }
    public void setMaxBidLimit(double maxBidLimit) { this.maxBidLimit = maxBidLimit; }

    public double getIncrement() { return increment; }
    public void setIncrement(double increment) { this.increment = increment; }

    public LocalDateTime getRegisteredTime() { return registeredTime; }
    public void setRegisteredTime(LocalDateTime registeredTime) { this.registeredTime = registeredTime; }
}