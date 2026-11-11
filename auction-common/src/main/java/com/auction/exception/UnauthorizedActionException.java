package com.auction.exception;

/**
 * Ngoại lệ xảy ra khi người dùng cố gắng thực hiện một thao tác hoặc hành động
 * mà tài khoản không có quyền hạn thực hiện (ví dụ: truy cập trái phép,
 * không đúng vai trò phân quyền BIDDER/SELLER/ADMIN).
 */
public class UnauthorizedActionException extends AuctionException {

    /**
     * Khởi tạo ngoại lệ với thông báo lỗi.
     *
     * @param message nội dung thông báo lỗi
     */
    public UnauthorizedActionException(String message) {
        super(message);
    }

    /**
     * Khởi tạo ngoại lệ với thông báo lỗi và nguyên nhân gây lỗi.
     *
     * @param message nội dung thông báo lỗi
     * @param cause ngoại lệ gốc gây ra lỗi
     */
    public UnauthorizedActionException(String message, Throwable cause) {
        super(message, cause);
    }
}
