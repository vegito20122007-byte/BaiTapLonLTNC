package com.auction.model;

/**
 * Đại diện cho Quản trị viên (Admin).
 * Có toàn quyền quản lý tài khoản người dùng và giám sát các phiên đấu giá.
 */

public class Admin extends User{
    private static final long serialVersionUID = 1L;

    public Admin(Integer id) {
        super(id);
    }
    public Admin(Integer id, String username, String password) {
        super(id, username, password, "ADMIN");
    }
}
