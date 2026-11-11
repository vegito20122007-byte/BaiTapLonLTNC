package com.auction.exception;

/**
 * Ngoại lệ xảy ra khi người dùng cố gắng thao tác (đặt giá, cập nhật)
 * trên một phiên đấu giá đã kết thúc, bị hủy hoặc chưa mở nhận giá.
 */
public class AuctionClosedException extends AuctionException {

    /**
     * Khởi tạo ngoại lệ với thông báo lỗi.
     *
     * @param message nội dung thông báo lỗi
     */
    public AuctionClosedException(String message) {
        super(message);
    }

    /**
     * Khởi tạo ngoại lệ với thông báo lỗi và nguyên nhân gây lỗi.
     *
     * @param message nội dung thông báo lỗi
     * @param cause ngoại lệ gốc gây ra lỗi
     */
    public AuctionClosedException(String message, Throwable cause) {
        super(message, cause);
    }
}
