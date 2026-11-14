package com.auction.model;

import java.time.LocalDateTime;

/**
 * Lưu vết một lượt đặt giá hợp lệ trong phiên đấu giá.
 */
public class BidTransaction extends Entity {
    private static final long serialVersionUID = 1L;

    /** ID của phiên đấu giá tương ứng. */
    private Integer auctionId;

    /** Mức giá đặt. */
    private double amount;

    /** Người thực hiện đặt giá. */
    private User bidder;

    /** Thời điểm diễn ra giao dịch. */
    private LocalDateTime time;

    public BidTransaction() {
        super();
    }
    public BidTransaction(Integer id, Integer auctionId, double amount, User bidder, LocalDateTime time) {
        super(id);
        this.auctionId = auctionId;
        this.amount = amount;
        this.bidder = bidder;
        this.time = time;
    }

    public Integer getAuctionId() { return auctionId; }
    public double getAmount() { return amount; }
    public User getBidder() { return bidder; }
    public LocalDateTime getTime() { return time; }
}