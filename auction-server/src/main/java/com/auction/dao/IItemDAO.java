package com.auction.dao;

import com.auction.model.Item;
import java.util.List;

/**
 * Interface định nghĩa các thao tác truy xuất dữ liệu (DAO) cho sản phẩm đấu giá {@link Item}.
 * Hỗ trợ quản lý các phân loại sản phẩm kế thừa từ Item: Electronics, Art, Vehicle.
 */
public interface IItemDAO {

    /**
     * Tìm kiếm sản phẩm theo ID.
     *
     * @param id ID định danh của sản phẩm
     * @return Đối tượng Item (Electronics, Art hoặc Vehicle), hoặc null nếu không tồn tại
     */
    Item findById(Integer id);

    /**
     * Lấy danh sách toàn bộ sản phẩm trong hệ thống.
     *
     * @return Danh sách các Item
     */
    List<Item> findAll();

    /**
     * Lưu mới một sản phẩm vào Cơ sở dữ liệu.
     *
     * @param item Đối tượng sản phẩm cần thêm mới
     */
    void save(Item item);

    /**
     * Cập nhật thông tin sản phẩm hiện có trong Cơ sở dữ liệu.
     *
     * @param item Đối tượng sản phẩm chứa thông tin cập nhật
     */
    void update(Item item);

    /**
     * Xóa sản phẩm khỏi Cơ sở dữ liệu theo ID.
     *
     * @param id ID của sản phẩm cần xóa
     */
    void delete(Integer id);
}
