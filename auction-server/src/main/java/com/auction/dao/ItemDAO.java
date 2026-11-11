package com.auction.dao;

import com.auction.model.Art;
import com.auction.model.Electronics;
import com.auction.model.Item;
import com.auction.model.ItemFactory;
import com.auction.model.Seller;
import com.auction.model.User;
import com.auction.model.Vehicle;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * =========================================================================
 * LỚP THỰC THI DAO QUẢN LÝ SẢN PHẨM ĐẤU GIÁ: ItemDAO (Tuần 7 - Vị trí 2)
 * =========================================================================
 * - Thực thi giao diện {@link IItemDAO} theo đúng thiết kế Sơ đồ lớp UML.
 * - Quản lý lưu trữ kế thừa đơn bảng (Single Table Inheritance) trên bảng `items` cho cả 3 dòng sản phẩm:
 *   + {@link Electronics} (có thêm cột `brand`, `warranty_months`)
 *   + {@link Art} (có thêm cột `artist`, `year_created`)
 *   + {@link Vehicle} (có thêm cột `manufacturer`, `odometer`)
 * - Sử dụng {@link ItemFactory} trong hàm {@link #map(ResultSet)} để tái tạo đúng lớp con đa hình khi đọc từ DB.
 */
public class ItemDAO implements IItemDAO {

    /** Kết nối CSDL theo thiết kế UML (- con: Connection). */
    private Connection con;

    /** Tham chiếu tới DAO người dùng để truy vấn chủ sở hữu sản phẩm (- userDAO: IUserDAO). */
    private IUserDAO userDAO;

    /**
     * Constructor mặc định: Sử dụng kết nối hiện tại từ {@link DBConnection}.
     */
    public ItemDAO() {
        this(DBConnection.getConnection());
    }

    /**
     * Constructor nhận đối tượng Connection truyền vào (khớp `+ ItemDAO(con: Connection)` trong UML).
     *
     * @param con Kết nối CSDL
     */
    public ItemDAO(Connection con) {
        this.con = con;
        this.userDAO = new UserDAO(con);
    }

    /**
     * Constructor nhận cả Connection và IUserDAO tùy chỉnh.
     *
     * @param con     Kết nối CSDL
     * @param userDAO Đối tượng DAO truy xuất người dùng
     */
    public ItemDAO(Connection con, IUserDAO userDAO) {
        this.con = con;
        this.userDAO = userDAO != null ? userDAO : new UserDAO(con);
    }

    /**
     * Đảm bảo luôn lấy được kết nối CSDL đang mở.
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
     * Lưu mới một sản phẩm (Art, Electronics hoặc Vehicle) vào bảng `items`.
     * Tự động gán ID sinh ra từ CSDL ngược lại vào đối tượng `item`.
     *
     * @param item Sản phẩm cần lưu mới
     */
    @Override
    public void save(Item item) {
        if (item == null) {
            throw new IllegalArgumentException("Đối tượng Item không được null.");
        }
        String sql = """
                INSERT INTO items (
                    name, owner_id, description, category, image_data,
                    brand, warranty_months, artist, year_created, manufacturer, odometer
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement ps = getActiveConnection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bindItemParameters(ps, item);
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    item.setId(keys.getInt(1));
                }
            }
        } catch (SQLException e) {
            System.err.println("[ItemDAO] Lỗi khi lưu Item: " + e.getMessage());
            throw new RuntimeException("Không thể lưu sản phẩm: " + e.getMessage(), e);
        }
    }

    /**
     * Tìm kiếm sản phẩm đấu giá theo ID.
     *
     * @param id Mã sản phẩm cần tìm
     * @return Đối tượng Item (Art/Electronics/Vehicle) hoặc null nếu không tìm thấy
     */
    @Override
    public Item findById(Integer id) {
        if (id == null) {
            return null;
        }
        String sql = "SELECT * FROM items WHERE id = ?";
        try (PreparedStatement ps = getActiveConnection().prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return map(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("[ItemDAO] Lỗi khi tìm Item theo ID (" + id + "): " + e.getMessage());
        }
        return null;
    }

    /**
     * Lấy danh sách toàn bộ sản phẩm trong bảng `items`.
     *
     * @return Danh sách các đối tượng Item
     */
    @Override
    public List<Item> findAll() {
        List<Item> items = new ArrayList<>();
        String sql = "SELECT * FROM items ORDER BY id ASC";
        try (PreparedStatement ps = getActiveConnection().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                items.add(map(rs));
            }
        } catch (SQLException e) {
            System.err.println("[ItemDAO] Lỗi khi lấy danh sách Item: " + e.getMessage());
        }
        return items;
    }

    /**
     * Cập nhật thông tin của sản phẩm hiện có trong bảng `items`.
     *
     * @param item Đối tượng sản phẩm chứa thông tin mới
     */
    @Override
    public void update(Item item) {
        if (item == null || item.getId() == null) {
            throw new IllegalArgumentException("Item hoặc ID không hợp lệ khi cập nhật.");
        }
        String sql = """
                UPDATE items SET
                    name = ?, owner_id = ?, description = ?, category = ?, image_data = ?,
                    brand = ?, warranty_months = ?, artist = ?, year_created = ?, manufacturer = ?, odometer = ?
                WHERE id = ?
                """;
        try (PreparedStatement ps = getActiveConnection().prepareStatement(sql)) {
            bindItemParameters(ps, item);
            ps.setInt(12, item.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[ItemDAO] Lỗi khi cập nhật Item ID=" + item.getId() + ": " + e.getMessage());
            throw new RuntimeException("Không thể cập nhật sản phẩm: " + e.getMessage(), e);
        }
    }

    /**
     * Xóa sản phẩm khỏi bảng `items` theo ID.
     *
     * @param id Mã định danh của sản phẩm cần xóa
     */
    @Override
    public void delete(Integer id) {
        if (id == null) {
            return;
        }
        String sql = "DELETE FROM items WHERE id = ?";
        try (PreparedStatement ps = getActiveConnection().prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[ItemDAO] Lỗi khi xóa Item ID=" + id + ": " + e.getMessage());
            throw new RuntimeException("Không thể xóa sản phẩm: " + e.getMessage(), e);
        }
    }

    /**
     * Hàm phụ trợ gán 11 tham số của đối tượng {@link Item} (bao gồm kiểm tra kiểu đa hình `instanceof`)
     * vào câu lệnh {@link PreparedStatement} dùng chung cho cả INSERT và UPDATE.
     */
    private void bindItemParameters(PreparedStatement ps, Item item) throws SQLException {
        ps.setString(1, item.getName());
        int ownerId = (item.getOwner() != null && item.getOwner().getId() != null)
                ? item.getOwner().getId() : 1;
        ps.setInt(2, ownerId);
        ps.setString(3, item.getDescription());
        ps.setString(4, item.getCategory() != null ? item.getCategory().toUpperCase() : "ELECTRONICS");
        ps.setString(5, item.getImageData());

        // Đặt mặc định NULL cho toàn bộ các cột riêng của từng phân loại sản phẩm
        ps.setNull(6, Types.VARCHAR);
        ps.setNull(7, Types.INTEGER);
        ps.setNull(8, Types.VARCHAR);
        ps.setNull(9, Types.INTEGER);
        ps.setNull(10, Types.VARCHAR);
        ps.setNull(11, Types.DOUBLE);

        // Kiểm tra kiểu thực tế của đối tượng (Đa hình) để gán đúng cột đặc trưng
        if (item instanceof Electronics electronics) {
            ps.setString(6, electronics.getBrand());
            ps.setInt(7, electronics.getWarrantyMonths());
        } else if (item instanceof Art art) {
            ps.setString(8, art.getArtist());
            ps.setInt(9, art.getYearCreated());
        } else if (item instanceof Vehicle vehicle) {
            ps.setString(10, vehicle.getManufacturer());
            ps.setDouble(11, vehicle.getOdometer());
        }
    }

    /**
     * Ánh xạ một dòng dữ liệu {@link ResultSet} thành đối tượng con của {@link Item}
     * thông qua {@link ItemFactory#createItemWithAttributes}.
     * Khớp chính xác với `- map(rs: ResultSet): Item` trong sơ đồ lớp UML.
     *
     * @param rs ResultSet đang trỏ tới bản ghi hợp lệ
     * @return Đối tượng Art, Electronics hoặc Vehicle hoàn chỉnh
     * @throws SQLException nếu lỗi đọc cột dữ liệu
     */
    private Item map(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        String name = rs.getString("name");
        int ownerId = rs.getInt("owner_id");
        String description = rs.getString("description");
        String category = rs.getString("category");
        String imageData = rs.getString("image_data");

        // Truy vấn thông tin người bán sở hữu sản phẩm thông qua userDAO
        Seller owner = null;
        User foundUser = userDAO.findById(ownerId);
        if (foundUser instanceof Seller seller) {
            owner = seller;
        } else if (foundUser != null) {
            owner = new Seller(foundUser.getId(), foundUser.getUsername(), foundUser.getPassword());
            owner.setPassword(foundUser.getPassword());
            owner.setBalance(foundUser.getBalance());
        }

        // Đóng gói các thuộc tính riêng vào Map để truyền cho ItemFactory khởi tạo đúng lớp con
        Map<String, Object> extraAttributes = new HashMap<>();
        extraAttributes.put("brand", rs.getString("brand"));
        extraAttributes.put("warrantyMonths", rs.getInt("warranty_months"));
        extraAttributes.put("artist", rs.getString("artist"));
        extraAttributes.put("yearCreated", rs.getInt("year_created"));
        extraAttributes.put("manufacturer", rs.getString("manufacturer"));
        extraAttributes.put("odometer", rs.getDouble("odometer"));

        Item item = ItemFactory.createItemWithAttributes(category, id, name, description, owner, extraAttributes);
        item.setImageData(imageData);
        return item;
    }
}
