package com.auction.exception;

/**
 * Ngoại lệ xảy ra khi có sự cố kết nối mạng Socket hoặc mất kết nối với Server.
 */
public class ConnectionException extends AuctionException {

    /**
     * Khởi tạo ngoại lệ với thông báo lỗi.
     *
     * @param message nội dung thông báo lỗi
     */
    public ConnectionException(String message) {
        super(message);
    }

    /**
     * Khởi tạo ngoại lệ với thông báo lỗi và nguyên nhân gây lỗi.
     *
     * @param message nội dung thông báo lỗi
     * @param cause ngoại lệ gốc gây ra lỗi
     */
    public ConnectionException(String message, Throwable cause) {
        super(message, cause);
    }
}
