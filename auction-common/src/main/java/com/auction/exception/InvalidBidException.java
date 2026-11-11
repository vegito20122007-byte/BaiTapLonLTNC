package com.auction.exception;

/**
 * Ngoại lệ xảy ra khi lượt đặt giá (bid) không hợp lệ theo quy tắc nghiệp vụ
 * (ví dụ: đặt giá thấp hơn giá hiện tại, thấp hơn giá khởi điểm, người bán tự đặt giá,...).
 */
public class InvalidBidException extends AuctionException {

    /**
     * Khởi tạo ngoại lệ với thông báo lỗi.
     *
     * @param message nội dung thông báo lỗi
     */
    public InvalidBidException(String message) {
        super(message);
    }

    /**
     * Khởi tạo ngoại lệ với thông báo lỗi và nguyên nhân gây lỗi.
     *
     * @param message nội dung thông báo lỗi
     * @param cause ngoại lệ gốc gây ra lỗi
     */
    public InvalidBidException(String message, Throwable cause) {
        super(message, cause);
    }
}
