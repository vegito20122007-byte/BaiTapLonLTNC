package com.auction.dao;

import com.auction.model.User;
import com.auction.model.UserFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * =========================================================================
 * LỚP THỰC THI DAO QUẢN LÝ NGƯỜI DÙNG: UserDAO (Tuần 7 - Vị trí 2)
 * =========================================================================
 * - Thực thi giao diện {@link IUserDAO} theo đúng thiết kế Sơ đồ lớp UML.
 * - Chịu trách nhiệm thực hiện các thao tác CRUD (Thêm, Tìm kiếm, Cập nhật, Xóa)
 *   trực tiếp với bảng `users` trong Cơ sở dữ liệu MySQL (XAMPP) / SQLite.
 * - Kết hợp với {@link UserFactory} (Factory Method Pattern) trong hàm {@link #map(ResultSet)}
 *   để tự động khởi tạo đúng đối tượng con: Bidder, Seller hoặc Admin dựa vào cột `role`.
 */
public class UserDAO implements IUserDAO {

    /**
     * Đối tượng kết nối CSDL dùng cho các truy vấn của DAO này.
     * Đặt tên biến là `con` khớp 100% với thuộc tính `- con: Connection` trên sơ đồ UML.
     */
    private Connection con;

    /**
     * Constructor mặc định: Tự động lấy kết nối của luồng hiện tại từ {@link DBConnection}.
     */
    public UserDAO() {
        this.con = DBConnection.getConnection();
    }

    /**
     * Constructor nhận đối tượng Connection truyền vào từ bên ngoài.
     * Khớp chính xác với `+ UserDAO(con: Connection)` trong sơ đồ lớp UML.
     *
     * @param con Kết nối CSDL đang mở
     */
    public UserDAO(Connection con) {
        this.con = con;
    }

    /**
     * Hàm nội bộ kiểm tra và đảm bảo kết nối CSDL luôn ở trạng thái mở trước khi chạy câu lệnh SQL.
     * Nếu kết nối cũ bị đóng hoặc null, tự động lấy lại kết nối mới từ {@link DBConnection}.
     *
     * @return Đối tượng {@link Connection} hợp lệ
     * @throws SQLException nếu không thể kết nối tới CSDL
     */
    private Connection getActiveConnection() throws SQLException {
        if (this.con == null || this.con.isClosed()) {
            this.con = DBConnection.getConnection();
        }
        if (this.con == null) {
            throw new SQLException("Không thể kết nối tới Cơ sở dữ liệu.");
        }
        return this.con;
    }

    /**
     * Lưu mới một người dùng (Bidder, Seller hoặc Admin) vào bảng `users`.
     * Sau khi INSERT thành công, tự động lấy khóa chính (AUTO_INCREMENT ID) gán ngược lại vào đối tượng `user`.
     *
     * @param user Đối tượng người dùng cần thêm mới
     */
    @Override
    public void save(User user) {
        if (user == null) {
            throw new IllegalArgumentException("Đối tượng User không được null.");
        }
        String sql = "INSERT INTO users (username, password, role, balance) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = getActiveConnection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, user.getUsername());
            ps.setString(2, user.getPassword());
            ps.setString(3, user.getRole() != null ? user.getRole().toUpperCase() : "BIDDER");
            ps.setDouble(4, user.getBalance());
            ps.executeUpdate();

            // Lấy ID tự tăng vừa được MySQL sinh ra và gán lại cho đối tượng User
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    user.setId(keys.getInt(1));
                }
            }
        } catch (SQLException e) {
            System.err.println("[UserDAO] Lỗi khi lưu User: " + e.getMessage());
            throw new RuntimeException("Không thể lưu người dùng: " + e.getMessage(), e);
        }
    }

    /**
     * Tìm kiếm người dùng trong bảng `users` theo khóa chính `id`.
     *
     * @param id Mã định danh người dùng
     * @return Đối tượng User (Bidder/Seller/Admin) nếu tìm thấy, hoặc null nếu không tồn tại
     */
    @Override
    public User findById(Integer id) {
        if (id == null) {
            return null;
        }
        String sql = "SELECT id, username, password, role, balance FROM users WHERE id = ?";
        try (PreparedStatement ps = getActiveConnection().prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return map(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("[UserDAO] Lỗi khi tìm User theo ID (" + id + "): " + e.getMessage());
        }
        return null;
    }

    /**
     * Tìm kiếm người dùng theo tên tài khoản `username` (phục vụ Đăng nhập và kiểm tra trùng tên khi Đăng ký).
     *
     * @param username Tên đăng nhập cần tra cứu
     * @return Đối tượng User tương ứng, hoặc null nếu chưa tồn tại
     */
    @Override
    public User findByUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            return null;
        }
        String sql = "SELECT id, username, password, role, balance FROM users WHERE username = ?";
        try (PreparedStatement ps = getActiveConnection().prepareStatement(sql)) {
            ps.setString(1, username.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return map(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("[UserDAO] Lỗi khi tìm User theo username (" + username + "): " + e.getMessage());
        }
        return null;
    }

    /**
     * Lấy danh sách toàn bộ người dùng trong hệ thống (phục vụ màn hình Quản trị viên Admin).
     *
     * @return Danh sách các đối tượng User
     */
    @Override
    public List<User> findAll() {
        List<User> users = new ArrayList<>();
        String sql = "SELECT id, username, password, role, balance FROM users ORDER BY id ASC";
        try (PreparedStatement ps = getActiveConnection().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                users.add(map(rs));
            }
        } catch (SQLException e) {
            System.err.println("[UserDAO] Lỗi khi lấy danh sách User: " + e.getMessage());
        }
        return users;
    }

    /**
     * Cập nhật thông tin tài khoản (tên đăng nhập, mật khẩu băm, vai trò, số dư ví) theo ID.
     *
     * @param user Đối tượng User chứa dữ liệu mới cần cập nhật
     */
    @Override
    public void update(User user) {
        if (user == null || user.getId() == null) {
            throw new IllegalArgumentException("User hoặc ID không hợp lệ khi cập nhật.");
        }
        String sql = "UPDATE users SET username = ?, password = ?, role = ?, balance = ? WHERE id = ?";
        try (PreparedStatement ps = getActiveConnection().prepareStatement(sql)) {
            ps.setString(1, user.getUsername());
            ps.setString(2, user.getPassword());
            ps.setString(3, user.getRole() != null ? user.getRole().toUpperCase() : "BIDDER");
            ps.setDouble(4, user.getBalance());
            ps.setInt(5, user.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[UserDAO] Lỗi khi cập nhật User ID=" + user.getId() + ": " + e.getMessage());
            throw new RuntimeException("Không thể cập nhật người dùng: " + e.getMessage(), e);
        }
    }

    /**
     * Xóa một người dùng khỏi bảng `users` theo ID.
     *
     * @param id ID của người dùng cần xóa
     */
    @Override
    public void delete(Integer id) {
        if (id == null) {
            return;
        }
        String sql = "DELETE FROM users WHERE id = ?";
        try (PreparedStatement ps = getActiveConnection().prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[UserDAO] Lỗi khi xóa User ID=" + id + ": " + e.getMessage());
            throw new RuntimeException("Không thể xóa người dùng: " + e.getMessage(), e);
        }
    }

    /**
     * Chuyển đổi (ánh xạ) một dòng dữ liệu từ {@link ResultSet} của JDBC thành đối tượng {@link User}.
     * Khớp chính xác với phương thức `- map(rs: ResultSet): User` trong sơ đồ lớp UML.
     *
     * Lưu ý kỹ thuật: Do constructor của User tự động băm mật khẩu truyền vào, nên sau khi gọi
     * {@link UserFactory#createUser}, ta gán lại `user.setPassword(hashedPassword)` để giữ nguyên
     * chuỗi SHA-256 gốc trong CSDL (tránh bị băm 2 lần).
     *
     * @param rs Con trỏ ResultSet đang đứng tại dòng dữ liệu hợp lệ
     * @return Thể hiện cụ thể của User (Bidder, Seller hoặc Admin)
     * @throws SQLException nếu xảy ra lỗi đọc cột dữ liệu
     */
    private User map(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        String username = rs.getString("username");
        String hashedPassword = rs.getString("password");
        String role = rs.getString("role");
        double balance = rs.getDouble("balance");

        User user = UserFactory.createUser(id, username, hashedPassword, role);
        user.setPassword(hashedPassword);
        user.setBalance(balance);
        return user;
    }
}
