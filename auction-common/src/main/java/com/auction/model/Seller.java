package com.auction.model;

/**
 * Đại diện cho Người bán (Seller).
 * Có quyền tạo sản phẩm đấu giá, quản lý sản phẩm và xác nhận thanh toán.
 */

public class Seller extends User{
    private static final long serialVersionUID = 1L;

    public Seller(Integer id) {
        super(id);
    }
    public Seller(Integer id, String username, String password) {
        super(id, username, password, "SELLER");
    }

}
