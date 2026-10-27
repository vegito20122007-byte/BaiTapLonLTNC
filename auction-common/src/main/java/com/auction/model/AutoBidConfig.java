package com.auction.model;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Cấu hình chế độ đấu giá tự động (Auto-Bidding) cho một người dùng.
 * Phục vụ hàng đợi ưu tiên PriorityQueue (Tính năng nâng cao +0.5đ).
 */
public class AutoBidConfig implements Serializable {
    private static final long serialVersionUID = 1L;

    /** ID người dùng đăng ký auto-bid. */
    private int userId;

    /** Đối tượng người dùng đặt auto-bid. */
    private User user;

    /** Giá tối đa sẵn sàng trả (maxBidLimit). */
    private double maxBidLimit;

    /** Bước giá tăng thêm mỗi khi có đối thủ vượt mặt (increment). */
    private double increment;

    /** Thời điểm đăng ký bot (ưu tiên FIFO nếu mức giá ngang nhau). */
    private LocalDateTime registeredTime;

    public AutoBidConfig(int userId, User user, double maxBidLimit, double increment) {
        this.userId = userId;
        this.user = user;
        this.maxBidLimit = maxBidLimit;
        this.increment = increment;
        this.registeredTime = LocalDateTime.now();
    }

    public int getUserId() { return userId; }
    public User getUser() { return user; }
    public double getMaxBidLimit() { return maxBidLimit; }
    public double getIncrement() { return increment; }
    public LocalDateTime getRegisteredTime() { return registeredTime; }

    public void setMaxBidLimit(double maxBidLimit) {this.maxBidLimit = maxBidLimit;}
    public void setIncrement(double increment) {
        this.increment = increment;
    }
    public void setUserId(int userId) {this.userId = userId;}
}