package com.auction.dao;

import com.auction.model.BidTransaction;
import java.util.List;

/**
 * Interface định nghĩa các thao tác truy xuất dữ liệu (DAO) cho lịch sử đặt giá {@link BidTransaction}.
 * Phục vụ việc lưu vết từng lượt trả giá và truy vấn lịch sử đặt giá theo từng phiên.
 */
public interface IBidTransactionDAO {

    /**
     * Lưu một lượt đặt giá hợp lệ mới vào Cơ sở dữ liệu.
     *
     * @param transaction Giao dịch đặt giá cần lưu
     */
    void save(BidTransaction transaction);

    /**
     * Lấy danh sách toàn bộ các lượt đặt giá của một phiên đấu giá cụ thể.
     *
     * @param auctionId ID của phiên đấu giá cần tra cứu lịch sử
     * @return Danh sách các lượt đặt giá thuộc phiên đó
     */
    List<BidTransaction> findByAuctionId(Integer auctionId);

    /**
     * Lấy danh sách toàn bộ các lượt đặt giá trên toàn hệ thống.
     *
     * @return Danh sách tất cả các BidTransaction
     */
    List<BidTransaction> findAll();
}
