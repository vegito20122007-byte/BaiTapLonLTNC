package com.auction.exception;

/**
 * Ngoại lệ xảy ra trong quá trình xác thực tài khoản và phân quyền người dùng
 * (ví dụ: sai mật khẩu, tài khoản không tồn tại, tên đăng nhập đã được sử dụng,...).
 */
public class AuthenticationException extends AuctionException {

    /**
     * Khởi tạo ngoại lệ với thông báo lỗi.
     *
     * @param message nội dung thông báo lỗi
     */
    public AuthenticationException(String message) {
        super(message);
    }

    /**
     * Khởi tạo ngoại lệ với thông báo lỗi và nguyên nhân gây lỗi.
     *
     * @param message nội dung thông báo lỗi
     * @param cause ngoại lệ gốc gây ra lỗi
     */
    public AuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }
}
