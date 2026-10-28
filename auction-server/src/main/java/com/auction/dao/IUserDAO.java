package com.auction.dao;

import com.auction.model.User;
import java.util.List;

/**
 * Interface định nghĩa các thao tác truy xuất dữ liệu (DAO) cho đối tượng {@link User}.
 * Bao gồm các phương thức tìm kiếm, thêm mới, cập nhật và xóa người dùng (Bidder, Seller, Admin).
 */
public interface IUserDAO {

    /**
     * Tìm kiếm người dùng theo ID.
     *
     * @param id ID định danh của người dùng
     * @return Đối tượng User tìm thấy, hoặc null nếu không tồn tại
     */
    User findById(Integer id);

    /**
     * Tìm kiếm người dùng theo tên đăng nhập (phục vụ đăng nhập và kiểm tra trùng tài khoản).
     *
     * @param username Tên đăng nhập cần tìm
     * @return Đối tượng User tương ứng, hoặc null nếu chưa tồn tại
     */
    User findByUsername(String username);

    /**
     * Lấy danh sách toàn bộ người dùng trong hệ thống.
     *
     * @return Danh sách các User
     */
    List<User> findAll();

    /**
     * Lưu mới một người dùng vào Cơ sở dữ liệu.
     *
     * @param user Đối tượng người dùng cần thêm mới
     */
    void save(User user);

    /**
     * Cập nhật thông tin người dùng (mật khẩu, số dư ví, vai trò...) trong Cơ sở dữ liệu.
     *
     * @param user Đối tượng người dùng chứa thông tin mới
     */
    void update(User user);

    /**
     * Xóa người dùng khỏi Cơ sở dữ liệu theo ID.
     *
     * @param id ID của người dùng cần xóa
     */
    void delete(Integer id);
}
