package com.auction.exception;

/**
 * Lớp AuctionException là lớp ngoại lệ gốc cho toàn bộ hệ thống đấu giá.
 *
 * <p>Tất cả các ngoại lệ liên quan đến nghiệp vụ đấu giá như đóng phiên đấu giá,
 * số dư không đủ, giá đặt không hợp lệ,... đều kế thừa từ lớp này.</p>
 *
 * <p>Lớp kế thừa từ {@link RuntimeException} nên là ngoại lệ không bắt buộc xử lý (unchecked exception).</p>
 */
public class AuctionException extends RuntimeException {

    /**
     * Khởi tạo ngoại lệ với thông báo lỗi.
     *
     * @param message nội dung thông báo lỗi
     */
    public AuctionException(String message) {
        super(message);
    }

    /**
     * Khởi tạo ngoại lệ với thông báo lỗi và nguyên nhân gây lỗi.
     *
     * @param message nội dung thông báo lỗi
     * @param cause ngoại lệ gốc gây ra lỗi
     */
    public AuctionException(String message, Throwable cause) {
        super(message, cause);
    }
}
