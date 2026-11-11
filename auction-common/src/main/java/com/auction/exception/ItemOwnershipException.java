package com.auction.exception;

/**
 * Ngoại lệ xảy ra khi có vi phạm liên quan đến quyền sở hữu sản phẩm/vật phẩm đấu giá
 * (ví dụ: người dùng cố gắng chỉnh sửa, xóa hoặc đưa vào phiên đấu giá một sản phẩm
 * không thuộc quyền sở hữu của mình).
 */
public class ItemOwnershipException extends AuctionException {

    /**
     * Khởi tạo ngoại lệ với thông báo lỗi.
     *
     * @param message nội dung thông báo lỗi
     */
    public ItemOwnershipException(String message) {
        super(message);
    }

    /**
     * Khởi tạo ngoại lệ với thông báo lỗi và nguyên nhân gây lỗi.
     *
     * @param message nội dung thông báo lỗi
     * @param cause ngoại lệ gốc gây ra lỗi
     */
    public ItemOwnershipException(String message, Throwable cause) {
        super(message, cause);
    }
}
