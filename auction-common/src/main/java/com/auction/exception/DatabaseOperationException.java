package com.auction.exception;

/**
 * Ngoại lệ xảy ra khi có lỗi trong quá trình thực hiện các thao tác với cơ sở dữ liệu
 * (ví dụ: lỗi kết nối CSDL, truy vấn thất bại, lỗi ràng buộc khóa ngoại, lỗi giao dịch,...).
 */
public class DatabaseOperationException extends AuctionException {

    /**
     * Khởi tạo ngoại lệ với thông báo lỗi.
     *
     * @param message nội dung thông báo lỗi
     */
    public DatabaseOperationException(String message) {
        super(message);
    }

    /**
     * Khởi tạo ngoại lệ với nguyên nhân gây lỗi.
     *
     * @param cause ngoại lệ gốc gây ra lỗi
     */
    public DatabaseOperationException(Throwable cause) {
        super(cause != null ? cause.getMessage() : null, cause);
    }

    /**
     * Khởi tạo ngoại lệ với thông báo lỗi và nguyên nhân gây lỗi.
     *
     * @param message nội dung thông báo lỗi
     * @param cause ngoại lệ gốc gây ra lỗi
     */
    public DatabaseOperationException(String message, Throwable cause) {
        super(message, cause);
    }
}
