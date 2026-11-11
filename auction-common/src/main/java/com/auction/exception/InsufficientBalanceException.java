package com.auction.exception;

/**
 * Ngoại lệ xảy ra khi số dư tài khoản của người tham gia đấu giá không đủ để đặt giá.
 */
public class InsufficientBalanceException extends AuctionException {

    /**
     * Khởi tạo ngoại lệ với thông báo lỗi.
     *
     * @param message nội dung thông báo lỗi
     */
    public InsufficientBalanceException(String message) {
        super(message);
    }

    /**
     * Khởi tạo ngoại lệ với thông báo lỗi và nguyên nhân gây lỗi.
     *
     * @param message nội dung thông báo lỗi
     * @param cause ngoại lệ gốc gây ra lỗi
     */
    public InsufficientBalanceException(String message, Throwable cause) {
        super(message, cause);
    }
}
