package com.auction.dao;

import com.auction.model.Auction;
import com.auction.model.Item;
import com.auction.model.Status;
import com.auction.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * =========================================================================
 * LỚP THỰC THI DAO QUẢN LÝ PHIÊN ĐẤU GIÁ: AuctionDAO (Tuần 7 - Vị trí 2)
 * =========================================================================
 * - Thực thi giao diện {@link IAuctionDAO} theo đúng thiết kế Sơ đồ lớp UML.
 * - Quản lý việc thêm mới, truy vấn, cập nhật trạng thái/giá cao nhất/người thắng
 *   và xóa phiên đấu giá trên bảng `auctions`.
 * - Phối hợp cùng {@link IUserDAO} và {@link IItemDAO} để tái tạo đầy đủ đối tượng
 *   {@link Auction} (bao gồm cả đối tượng `Item`, `Seller`, `currentWinner`) trong hàm {@link #map(ResultSet)}.
 */
public class AuctionDAO implements IAuctionDAO {

    /** Kết nối CSDL theo thiết kế UML (- con: Connection). */
    private Connection con;

    /** DAO truy xuất thông tin người bán và người dẫn đầu phiên (- userDAO: IUserDAO). */
    private IUserDAO userDAO;

    /** DAO truy xuất thông tin sản phẩm được đem đấu giá (- itemDAO: IItemDAO). */
    private IItemDAO itemDAO;

    /**
     * Constructor mặc định: Sử dụng kết nối của luồng hiện tại từ {@link DBConnection}.
     */
    public AuctionDAO() {
        this(DBConnection.getConnection());
    }

    /**
     * Constructor nhận đối tượng Connection truyền vào.
     *
     * @param con Kết nối CSDL
     */
    public AuctionDAO(Connection con) {
        this.con = con;
        this.userDAO = new UserDAO(con);
        this.itemDAO = new ItemDAO(con, this.userDAO);
    }

    /**
     * Constructor đầy đủ nhận Connection, IUserDAO và IItemDAO.
     */
    public AuctionDAO(Connection con, IUserDAO userDAO, IItemDAO itemDAO) {
        this.con = con;
        this.userDAO = userDAO != null ? userDAO : new UserDAO(con);
        this.itemDAO = itemDAO != null ? itemDAO : new ItemDAO(con, this.userDAO);
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
     * Lưu mới một phiên đấu giá vào bảng `auctions`.
     * Nếu sản phẩm đính kèm (`auction.getItem()`) chưa được lưu vào bảng `items`,
     * phương thức sẽ tự động gọi `itemDAO.save()` trước để lấy khóa ngoại `item_id`.
     *
     * @param auction Phiên đấu giá cần thêm mới
     */
    @Override
    public void save(Auction auction) {
        if (auction == null) {
            throw new IllegalArgumentException("Đối tượng Auction không được null.");
        }
        if (auction.getItem() != null && auction.getItem().getId() == null) {
            itemDAO.save(auction.getItem());
        }

        String sql = """
                INSERT INTO auctions (
                    name, starting_price, item_id, seller_id, max_bid,
                    current_winner_id, status, start_time, end_time, reserved_price
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement ps = getActiveConnection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bindAuctionParameters(ps, auction);
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    auction.setId(keys.getInt(1));
                }
            }
        } catch (SQLException e) {
            System.err.println("[AuctionDAO] Lỗi khi lưu Auction: " + e.getMessage());
            throw new RuntimeException("Không thể lưu phiên đấu giá: " + e.getMessage(), e);
        }
    }

    /**
     * Tìm kiếm phiên đấu giá theo ID.
     *
     * @param id Mã phiên đấu giá
     * @return Đối tượng Auction hoặc null nếu không tồn tại
     */
    @Override
    public Auction findById(Integer id) {
        if (id == null) {
            return null;
        }
        String sql = "SELECT * FROM auctions WHERE id = ?";
        try (PreparedStatement ps = getActiveConnection().prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return map(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("[AuctionDAO] Lỗi khi tìm Auction theo ID (" + id + "): " + e.getMessage());
        }
        return null;
    }

    /**
     * Lấy danh sách toàn bộ các phiên đấu giá trong bảng `auctions`.
     *
     * @return Danh sách các đối tượng Auction
     */
    @Override
    public List<Auction> findAll() {
        List<Auction> auctions = new ArrayList<>();
        String sql = "SELECT * FROM auctions ORDER BY id ASC";
        try (PreparedStatement ps = getActiveConnection().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                auctions.add(map(rs));
            }
        } catch (SQLException e) {
            System.err.println("[AuctionDAO] Lỗi khi lấy danh sách Auction: " + e.getMessage());
        }
        return auctions;
    }

    /**
     * Cập nhật thông tin phiên đấu giá (mức giá cao nhất `max_bid`, người dẫn đầu `current_winner_id`,
     * trạng thái `status`, thời gian kết thúc `end_time` khi gia hạn Anti-sniping...).
     *
     * @param auction Đối tượng Auction chứa thông tin mới
     */
    @Override
    public void update(Auction auction) {
        if (auction == null || auction.getId() == null) {
            throw new IllegalArgumentException("Auction hoặc ID không hợp lệ khi cập nhật.");
        }
        if (auction.getItem() != null && auction.getItem().getId() != null) {
            itemDAO.update(auction.getItem());
        }

        String sql = """
                UPDATE auctions SET
                    name = ?, starting_price = ?, item_id = ?, seller_id = ?, max_bid = ?,
                    current_winner_id = ?, status = ?, start_time = ?, end_time = ?, reserved_price = ?
                WHERE id = ?
                """;
        try (PreparedStatement ps = getActiveConnection().prepareStatement(sql)) {
            bindAuctionParameters(ps, auction);
            ps.setInt(11, auction.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[AuctionDAO] Lỗi khi cập nhật Auction ID=" + auction.getId() + ": " + e.getMessage());
            throw new RuntimeException("Không thể cập nhật phiên đấu giá: " + e.getMessage(), e);
        }
    }

    /**
     * Xóa phiên đấu giá khỏi bảng `auctions` theo ID.
     *
     * @param id Mã phiên đấu giá cần xóa
     */
    @Override
    public void delete(Integer id) {
        if (id == null) {
            return;
        }
        String sql = "DELETE FROM auctions WHERE id = ?";
        try (PreparedStatement ps = getActiveConnection().prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[AuctionDAO] Lỗi khi xóa Auction ID=" + id + ": " + e.getMessage());
            throw new RuntimeException("Không thể xóa phiên đấu giá: " + e.getMessage(), e);
        }
    }

    /**
     * Hàm phụ trợ gán 10 tham số của {@link Auction} vào {@link PreparedStatement}.
     */
    private void bindAuctionParameters(PreparedStatement ps, Auction auction) throws SQLException {
        ps.setString(1, auction.getName());
        ps.setDouble(2, auction.getStartingPrice());

        int itemId = (auction.getItem() != null && auction.getItem().getId() != null)
                ? auction.getItem().getId() : 1;
        ps.setInt(3, itemId);

        int sellerId = (auction.getSeller() != null && auction.getSeller().getId() != null)
                ? auction.getSeller().getId() : 1;
        ps.setInt(4, sellerId);

        ps.setDouble(5, auction.getMaxBid());

        // Cột current_winner_id có thể NULL khi phiên mới mở chưa có người đặt giá
        if (auction.getCurrentWinner() != null && auction.getCurrentWinner().getId() != null) {
            ps.setInt(6, auction.getCurrentWinner().getId());
        } else {
            ps.setNull(6, Types.INTEGER);
        }

        Status status = auction.getStatus() != null ? auction.getStatus() : Status.OPEN;
        ps.setString(7, status.name());

        LocalDateTime start = auction.getStartTime() != null ? auction.getStartTime() : LocalDateTime.now();
        LocalDateTime end = auction.getEndTime() != null ? auction.getEndTime() : start.plusMinutes(30);
        ps.setTimestamp(8, Timestamp.valueOf(start));
        ps.setTimestamp(9, Timestamp.valueOf(end));

        ps.setDouble(10, auction.getReservePrice());
    }

    /**
     * Ánh xạ một dòng dữ liệu {@link ResultSet} từ bảng `auctions` thành đối tượng {@link Auction}.
     * Khớp chính xác với `- map(rs: ResultSet): Auction` trong sơ đồ lớp UML.
     *
     * @param rs ResultSet đang trỏ tới bản ghi hợp lệ
     * @return Đối tượng Auction đầy đủ thông tin sản phẩm, người bán và người dẫn đầu
     * @throws SQLException nếu lỗi đọc cột dữ liệu
     */
    private Auction map(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        String name = rs.getString("name");
        double startingPrice = rs.getDouble("starting_price");
        int itemId = rs.getInt("item_id");
        int sellerId = rs.getInt("seller_id");
        double maxBid = rs.getDouble("max_bid");
        int winnerId = rs.getInt("current_winner_id");
        boolean hasWinner = !rs.wasNull();
        String statusStr = rs.getString("status");
        Timestamp startTs = rs.getTimestamp("start_time");
        Timestamp endTs = rs.getTimestamp("end_time");
        double reservedPrice = rs.getDouble("reserved_price");

        Item item = itemDAO.findById(itemId);
        User seller = userDAO.findById(sellerId);
        User winner = hasWinner ? userDAO.findById(winnerId) : null;

        LocalDateTime startTime = startTs != null ? startTs.toLocalDateTime() : LocalDateTime.now();
        LocalDateTime endTime = endTs != null ? endTs.toLocalDateTime() : startTime.plusMinutes(30);

        Auction auction = new Auction(id, name, item, seller, startingPrice, reservedPrice, startTime, endTime);
        auction.setMaxBid(maxBid);
        auction.setCurrentWinner(winner);

        if (statusStr != null) {
            try {
                auction.setStatus(Status.valueOf(statusStr.trim().toUpperCase()));
            } catch (IllegalArgumentException ignored) {
                auction.setStatus(Status.OPEN);
            }
        }
        return auction;
    }
}
