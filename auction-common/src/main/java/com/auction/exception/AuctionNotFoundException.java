package com.auction.exception;

/**
 * Ngoại lệ xảy ra khi không tìm thấy phiên đấu giá tương ứng với ID yêu cầu.
 */
public class AuctionNotFoundException extends AuctionException {

    /**
     * Khởi tạo ngoại lệ với thông báo lỗi.
     *
     * @param message nội dung thông báo lỗi
     */
    public AuctionNotFoundException(String message) {
        super(message);
    }

    /**
     * Khởi tạo ngoại lệ với thông báo lỗi và nguyên nhân gây lỗi.
     *
     * @param message nội dung thông báo lỗi
     * @param cause ngoại lệ gốc gây ra lỗi
     */
    public AuctionNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
