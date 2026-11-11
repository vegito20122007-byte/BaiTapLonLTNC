package com.auction.exception;

/**
 * Ngoại lệ xảy ra khi dữ liệu đầu vào không hợp lệ hoặc để trống các trường bắt buộc.
 */
public class DataValidationException extends AuctionException {

    /**
     * Khởi tạo ngoại lệ với thông báo lỗi.
     *
     * @param message nội dung thông báo lỗi
     */
    public DataValidationException(String message) {
        super(message);
    }

    /**
     * Khởi tạo ngoại lệ với thông báo lỗi và nguyên nhân gây lỗi.
     *
     * @param message nội dung thông báo lỗi
     * @param cause ngoại lệ gốc gây ra lỗi
     */
    public DataValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
