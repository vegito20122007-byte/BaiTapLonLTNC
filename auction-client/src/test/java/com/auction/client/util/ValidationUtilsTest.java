package com.auction.client.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Kiểm thử ValidationUtils - Quy tắc kiểm tra tính hợp lệ dữ liệu đầu vào")
class ValidationUtilsTest {

    // ==========================================
    // 1. KIỂM THỬ ĐỊNH DẠNG TÊN ĐĂNG NHẬP (USERNAME)
    // ==========================================

    @Test
    @DisplayName("Username null hoặc rỗng phải bị từ chối")
    void testUsernameNullOrEmpty() {
        assertFalse(ValidationUtils.isValidUsername(null));
        assertFalse(ValidationUtils.isValidUsername(""));
        assertFalse(ValidationUtils.isValidUsername("   "));
    }

    @Test
    @DisplayName("Username dưới 3 ký tự phải bị từ chối")
    void testUsernameTooShort() {
        assertFalse(ValidationUtils.isValidUsername("a"));
        assertFalse(ValidationUtils.isValidUsername("ab"));
        assertFalse(ValidationUtils.isValidUsername("a "));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "user name",     // Chứa khoảng trắng ở giữa
            "user@123",      // Chứa ký tự đặc biệt @
            "admin!",        // Chứa ký tự đặc biệt !
            "test#user",     // Chứa ký tự đặc biệt #
            "hiep.pham",     // Chứa dấu chấm
            "bidder-01"      // Chứa dấu gạch ngang
    })
    @DisplayName("Username chứa ký tự không hợp lệ hoặc khoảng trắng phải bị từ chối")
    void testUsernameWithInvalidCharacters(String invalidUsername) {
        assertFalse(ValidationUtils.isValidUsername(invalidUsername),
                "Username không hợp lệ: " + invalidUsername);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "abc",
            "admin",
            "user_01",
            "taylermode",
            "BIDDER_99",
            "seller123"
    })
    @DisplayName("Username hợp lệ (>= 3 ký tự, chỉ chứa chữ cái, số, gạch dưới) phải được chấp nhận")
    void testValidUsernames(String validUsername) {
        assertTrue(ValidationUtils.isValidUsername(validUsername),
                "Username hợp lệ nhưng bị từ chối: " + validUsername);
    }

    // ==========================================
    // 2. KIỂM THỬ CHỦ ĐỘNG CHỌN VAI TRÒ (ROLE)
    // ==========================================

    @Test
    @DisplayName("Vai trò null, rỗng hoặc chưa chọn phải bị từ chối")
    void testRoleUnselected() {
        assertFalse(ValidationUtils.isValidRole(null));
        assertFalse(ValidationUtils.isValidRole(""));
        assertFalse(ValidationUtils.isValidRole("   "));
        assertFalse(ValidationUtils.isValidRole("-- Chọn vai trò --"));
    }

    @Test
    @DisplayName("Vai trò hợp lệ (chứa BIDDER hoặc SELLER) phải được chấp nhận")
    void testValidRoles() {
        assertTrue(ValidationUtils.isValidRole("Người đấu giá (BIDDER)"));
        assertTrue(ValidationUtils.isValidRole("Người bán hàng (SELLER)"));
        assertTrue(ValidationUtils.isValidRole("BIDDER"));
        assertTrue(ValidationUtils.isValidRole("SELLER"));
    }

    // ==========================================
    // 3. KIỂM THỬ GIÁ BẢO LƯU (RESERVE PRICE)
    // ==========================================

    @Test
    @DisplayName("Giá bảo lưu nhỏ hơn giá khởi điểm phải bị từ chối")
    void testReservePriceLessThanStartPrice() {
        double startPrice = 1000000.0;
        double invalidReservePrice = 800000.0;
        assertFalse(ValidationUtils.isValidReservePrice(startPrice, invalidReservePrice));
    }

    @Test
    @DisplayName("Giá khởi điểm <= 0 phải bị từ chối")
    void testInvalidStartPrice() {
        assertFalse(ValidationUtils.isValidReservePrice(0, 1000000.0));
        assertFalse(ValidationUtils.isValidReservePrice(-50000.0, 100000.0));
    }

    @Test
    @DisplayName("Giá bảo lưu >= giá khởi điểm phải được chấp nhận")
    void testValidReservePrice() {
        // Bằng giá khởi điểm
        assertTrue(ValidationUtils.isValidReservePrice(5000000.0, 5000000.0));
        // Lớn hơn giá khởi điểm
        assertTrue(ValidationUtils.isValidReservePrice(5000000.0, 7000000.0));
    }

    // ==========================================
    // 4. KIỂM THỬ GIỚI HẠN THỜI LƯỢNG PHIÊN (DURATION)
    // ==========================================

    @Test
    @DisplayName("Thời lượng dưới 5 phút phải bị từ chối")
    void testDurationTooShort() {
        assertFalse(ValidationUtils.isValidAuctionDuration(0));
        assertFalse(ValidationUtils.isValidAuctionDuration(-10));
        assertFalse(ValidationUtils.isValidAuctionDuration(4));
    }

    @Test
    @DisplayName("Thời lượng vượt quá 30 ngày (43.200 phút) phải bị từ chối")
    void testDurationTooLong() {
        assertFalse(ValidationUtils.isValidAuctionDuration(43201));
        assertFalse(ValidationUtils.isValidAuctionDuration(100000));
    }

    @Test
    @DisplayName("Thời lượng trong khoảng [5, 43200] phút phải được chấp nhận")
    void testValidDuration() {
        assertTrue(ValidationUtils.isValidAuctionDuration(5), "Biên dưới: 5 phút");
        assertTrue(ValidationUtils.isValidAuctionDuration(60), "1 giờ");
        assertTrue(ValidationUtils.isValidAuctionDuration(1440), "1 ngày");
        assertTrue(ValidationUtils.isValidAuctionDuration(43200), "Biên trên: 30 ngày");
    }
}
