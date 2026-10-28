package com.auction.dao;

import com.auction.model.Auction;
import java.util.List;

/**
 * Interface định nghĩa các thao tác truy xuất dữ liệu (DAO) cho phiên đấu giá {@link Auction}.
 * Quản lý việc lưu trữ thông tin phiên đấu giá, mức giá hiện tại và trạng thái vòng đời phiên.
 */
public interface IAuctionDAO {

    /**
     * Tìm kiếm phiên đấu giá theo ID.
     *
     * @param id ID định danh của phiên đấu giá
     * @return Đối tượng Auction tương ứng, hoặc null nếu không tìm thấy
     */
    Auction findById(Integer id);

    /**
     * Lấy danh sách toàn bộ các phiên đấu giá trong hệ thống.
     *
     * @return Danh sách các phiên đấu giá
     */
    List<Auction> findAll();

    /**
     * Lưu mới một phiên đấu giá vào Cơ sở dữ liệu.
     *
     * @param auction Phiên đấu giá cần thêm mới
     */
    void save(Auction auction);

    /**
     * Cập nhật thông tin phiên đấu giá (giá cao nhất, người thắng, thời gian, trạng thái...).
     *
     * @param auction Phiên đấu giá chứa thông tin cập nhật
     */
    void update(Auction auction);

    /**
     * Xóa phiên đấu giá khỏi Cơ sở dữ liệu theo ID.
     *
     * @param id ID của phiên đấu giá cần xóa
     */
    void delete(Integer id);
}
