package com.auction.client.util;

import java.util.regex.Pattern;

/**
 * Lớp tiện ích kiểm tra tính hợp lệ dữ liệu đầu vào (Input Validation) cho toàn bộ Client.
 * Đảm bảo các quy tắc nghiệp vụ:
 * - Định dạng tên đăng nhập (độ dài >= 3, không chứa khoảng trắng hay ký tự đặc biệt).
 * - Mật khẩu tối thiểu 6 ký tự.
 * - Vai trò người dùng bắt buộc chọn.
 * - Giá bảo lưu (Reserve Price) phải >= Giá khởi điểm (Starting Price).
 * - Thời lượng phiên đấu giá từ 5 phút đến tối đa 30 ngày (43.200 phút).
 */
public final class ValidationUtils {

    public static final int MIN_USERNAME_LENGTH = 3;
    public static final int MIN_PASSWORD_LENGTH = 6;
    public static final int MIN_DURATION_MINUTES = 5;
    public static final int MAX_DURATION_MINUTES = 43200; // 30 ngày = 30 * 24 * 60

    // Chỉ cho phép chữ cái (a-z, A-Z), chữ số (0-9) và dấu gạch dưới (_), không có khoảng trắng
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_]+$");

    private ValidationUtils() {
        // Chặn khởi tạo đối tượng tiện ích (Utility class)
    }

    /**
     * Kiểm tra tính hợp lệ của tên đăng nhập (Username).
     *
     * @param username tên đăng nhập cần kiểm tra
     * @return true nếu username có độ dài >= 3 và chỉ chứa [a-zA-Z0-9_]
     */
    public static boolean isValidUsername(String username) {
        if (username == null) {
            return false;
        }
        String trimmed = username.trim();
        return trimmed.length() >= MIN_USERNAME_LENGTH && USERNAME_PATTERN.matcher(trimmed).matches();
    }

    /**
     * Kiểm tra tính hợp lệ của mật khẩu.
     *
     * @param password mật khẩu cần kiểm tra
     * @return true nếu mật khẩu không rỗng và có ít nhất 6 ký tự
     */
    public static boolean isValidPassword(String password) {
        if (password == null) {
            return false;
        }
        return password.trim().length() >= MIN_PASSWORD_LENGTH;
    }

    /**
     * Kiểm tra vai trò người dùng đã được chủ động chọn hay chưa.
     *
     * @param role chuỗi vai trò từ ComboBox
     * @return true nếu role không rỗng và chứa BIDDER hoặc SELLER
     */
    public static boolean isValidRole(String role) {
        if (role == null || role.trim().isEmpty()) {
            return false;
        }
        String upper = role.toUpperCase();
        return upper.contains("BIDDER") || upper.contains("SELLER");
    }

    /**
     * Kiểm tra giá bảo lưu so với giá khởi điểm.
     * Giá bảo lưu (Reserve Price) là giá tối thiểu người bán đồng ý bán, do đó phải >= Giá khởi điểm.
     *
     * @param startPrice   giá khởi điểm (> 0)
     * @param reservePrice giá bảo lưu
     * @return true nếu startPrice > 0 và reservePrice >= startPrice
     */
    public static boolean isValidReservePrice(double startPrice, double reservePrice) {
        return startPrice > 0 && reservePrice >= startPrice;
    }

    /**
     * Kiểm tra thời lượng phiên đấu giá hợp lệ (từ 5 phút đến 30 ngày = 43.200 phút).
     *
     * @param durationMinutes thời lượng tính theo phút
     * @return true nếu trong khoảng [5, 43200]
     */
    public static boolean isValidAuctionDuration(int durationMinutes) {
        return durationMinutes >= MIN_DURATION_MINUTES && durationMinutes <= MAX_DURATION_MINUTES;
    }
}
