package com.auction.model;

public enum Status {
    /** Phiên vừa tạo, chưa tới thời gian bắt đầu. */
    OPEN,

    /** Phiên đang diễn ra, cho phép người dùng đặt giá. */
    RUNNING,

    /** Phiên đã hết thời gian, đóng nhận giá và xác định người thắng. */
    FINISHED,

    /** Người thắng cuộc đã thanh toán thành công cho người bán. */
    PAID,

    /** Phiên bị hủy bỏ do vi phạm hoặc người bán rút lại. */
    CANCELED
}
