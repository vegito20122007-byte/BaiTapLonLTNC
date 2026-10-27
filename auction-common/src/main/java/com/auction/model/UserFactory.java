package com.auction.model;

/**
 * Factory tạo đối tượng User tương ứng theo vai trò (Factory Method Pattern).
 */
public class UserFactory {

    /**
     * Khởi tạo đối tượng User cụ thể dựa vào role truyền vào.
     *
     * @param role Vai trò người dùng (BIDDER, SELLER, ADMIN)
     * @param id ID người dùng
     * @param username Tên tài khoản
     * @param password Mật khẩu
     * @return Thể hiện cụ thể của User (Bidder, Seller hoặc Admin)
     */
    public static User createUser(Integer id, String username, String password, String role) {
        if (role == null) {
            throw new IllegalArgumentException("Vai trò không được để trống");
        }
        switch (role.trim().toUpperCase()) {
            case "BIDDER":
                return new Bidder(id, username, password);
            case "SELLER":
                return new Seller(id, username, password);
            case "ADMIN":
                return new Admin(id, username, password);
            default:
                throw new IllegalArgumentException("Vai trò người dùng không hợp lệ: " + role);
        }
    }
}