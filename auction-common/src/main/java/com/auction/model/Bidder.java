package com.auction.model;

/**
 * Đại diện cho Người tham gia đấu giá (Bidder).
 * Có quyền đặt giá trực tiếp hoặc đăng ký cấu hình Auto-Bid.
 */

public class Bidder extends User{
    private static final long serialVersionUID = 1L;

    public Bidder() {
        super();
    }
    public Bidder(Integer id, String username, String password) {
        super(id, username, password, "BIDDER");
    }
}
