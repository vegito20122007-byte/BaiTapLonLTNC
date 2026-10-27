package com.auction.model;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

/**
 * Lớp trừu tượng đại diện cho người dùng chung trong hệ thống.
 * Cung cấp:
 * - Thông tin tài khoản, mật khẩu băm SHA-256, số dư ví.
 * - Danh sách sản phẩm sở hữu.
 *
 * Các lớp kế thừa User: Bidder, Seller, Admin.
 */

public abstract class User extends Entity {
    protected String username;
    protected String password;
    protected String role;
    protected double balance;
    protected List<Item> items = new ArrayList<>();

    public User(){
        super();
    }
    public User(Integer id, String username, String plainPassword, String role) {
        this.id = id;
        this.username = username;
        this.password = hashPassword(plainPassword);
        this.role = role;
        this.balance = 0.0;
    }

    /**
     * Băm mật khẩu theo chuẩn SHA-256 nhằm bảo mật thông tin.
     *
     * @param password Mật khẩu gốc cần băm
     * @return Chuỗi hex mã hóa SHA-256
     */

    public static String hashPassword(String password) {
        if (password == null) return null;
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(password.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Không tìm thấy thuật toán SHA-256");
        }
    }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public double getBalance() { return balance; }
    public void setBalance(double balance) { this.balance = balance; }

    public List<Item> getItems() { return items; }
    public void setItems(List<Item> items) { this.items = items; }
}



