package com.auction.client.util;

import com.auction.exception.DataValidationException;
import com.auction.exception.InvalidBidException;

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
        return Double.isFinite(startPrice) && Double.isFinite(reservePrice)
                && startPrice > 0 && reservePrice >= startPrice;
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

    /**
     * Kiểm tra dữ liệu đăng nhập hợp lệ, ném {@link DataValidationException} nếu sai quy tắc.
     */
    public static void validateLoginInput(String username, String password) {
        String u = username != null ? username.trim() : "";
        String p = password != null ? password.trim() : "";
        if (u.isEmpty() || p.isEmpty()) {
            throw new DataValidationException("Vui lòng nhập đầy đủ tên đăng nhập và mật khẩu!");
        }
        validateUsernameFormat(u);
    }

    /**
     * Kiểm tra dữ liệu đăng ký tài khoản hợp lệ, ném {@link DataValidationException} nếu sai quy tắc.
     */
    public static void validateRegistrationInput(String username, String password, String confirmPassword, String role) {
        String u = username != null ? username.trim() : "";
        String p = password != null ? password.trim() : "";
        String cp = confirmPassword != null ? confirmPassword.trim() : "";
        if (u.isEmpty() || p.isEmpty() || cp.isEmpty()) {
            throw new DataValidationException("Vui lòng nhập đầy đủ tất cả các trường thông tin!");
        }
        validateUsernameFormat(u);
        if (!isValidRole(role)) {
            throw new DataValidationException("Vui lòng chủ động chọn vai trò tài khoản (Bidder hoặc Seller)!");
        }
        if (!isValidPassword(p)) {
            throw new DataValidationException("Mật khẩu phải chứa ít nhất " + MIN_PASSWORD_LENGTH + " ký tự!");
        }
        if (!p.equals(cp)) {
            throw new DataValidationException("Mật khẩu xác nhận không khớp!");
        }
    }

    /**
     * Kiểm tra dữ liệu tạo phiên đấu giá hợp lệ, ném {@link DataValidationException} nếu sai quy tắc.
     */
    public static void validateAuctionCreation(String name, double startPrice, double reservePrice, int durationMinutes) {
        if (name == null || name.trim().isEmpty()) {
            throw new DataValidationException("Tên phiên đấu giá không được để trống!");
        }
        if (!Double.isFinite(startPrice) || startPrice <= 0) {
            throw new DataValidationException("Giá khởi điểm phải là số hữu hạn lớn hơn 0!");
        }
        if (!isValidReservePrice(startPrice, reservePrice)) {
            throw new DataValidationException("Giá bảo lưu không được nhỏ hơn giá khởi điểm ("
                    + String.format("%,.0f VNĐ", startPrice) + ")!");
        }
        if (!isValidAuctionDuration(durationMinutes)) {
            throw new DataValidationException("Thời lượng phiên đấu giá phải từ "
                    + MIN_DURATION_MINUTES + " phút đến tối đa 30 ngày (" + MAX_DURATION_MINUTES + " phút)!");
        }
    }

    /**
     * Kiểm tra giá đặt hợp lệ theo bước giá tối thiểu, ném {@link InvalidBidException} nếu vi phạm.
     */
    public static void validateBidAmount(double bidValue, double currentPrice, double minIncrement) {
        if (!Double.isFinite(bidValue) || bidValue <= 0) {
            throw new InvalidBidException("Số tiền đặt giá phải là số hữu hạn lớn hơn 0!");
        }
        if (!Double.isFinite(currentPrice) || currentPrice < 0
                || !Double.isFinite(minIncrement) || minIncrement <= 0) {
            throw new InvalidBidException("Giá hiện tại và bước giá tối thiểu phải là số hữu hạn hợp lệ!");
        }
        double minValidBid = currentPrice + minIncrement;
        if (bidValue < minValidBid) {
            throw new InvalidBidException(String.format(
                    "Mức giá bạn đặt (%,.0f VNĐ) chưa đạt bước giá tối thiểu! Mức giá hợp lệ phải từ %,.0f VNĐ trở lên.",
                    bidValue, minValidBid));
        }
    }

    /**
     * Tính toán bước giá tối thiểu động theo mô hình bậc thang khoảng giá (Tiered Bid Increment).
     * Áp dụng theo quy chuẩn thực tế của các sàn đấu giá chuyên nghiệp (eBay, Sotheby's):
     * - Dưới 1 triệu VNĐ: bước giá 20,000 VNĐ
     * - Từ 1 triệu đến < 10 triệu VNĐ: bước giá 100,000 VNĐ
     * - Từ 10 triệu đến < 50 triệu VNĐ: bước giá 500,000 VNĐ
     * - Từ 50 triệu đến < 200 triệu VNĐ: bước giá 1,000,000 VNĐ
     * - Từ 200 triệu VNĐ trở lên: bước giá 5,000,000 VNĐ
     *
     * @param currentPrice mức giá hiện tại của sản phẩm
     * @return bước giá tối thiểu tương ứng
     */
    public static double calculateMinIncrement(double currentPrice) {
        if (!Double.isFinite(currentPrice) || currentPrice <= 0) {
            return 20_000.0;
        }
        if (currentPrice < 1_000_000.0) {
            return 20_000.0;
        } else if (currentPrice < 10_000_000.0) {
            return 100_000.0;
        } else if (currentPrice < 50_000_000.0) {
            return 500_000.0;
        } else if (currentPrice < 200_000_000.0) {
            return 1_000_000.0;
        } else {
            return 5_000_000.0;
        }
    }

    private static void validateUsernameFormat(String trimmedUsername) {
        if (trimmedUsername.length() < MIN_USERNAME_LENGTH) {
            throw new DataValidationException("Tên đăng nhập phải chứa ít nhất " + MIN_USERNAME_LENGTH + " ký tự!");
        }
        if (!isValidUsername(trimmedUsername)) {
            throw new DataValidationException("Tên đăng nhập không được chứa khoảng trắng hoặc ký tự đặc biệt!");
        }
    }
}
