package com.auction.dao;

import com.auction.model.BidTransaction;
import com.auction.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * =========================================================================
 * LỚP THỰC THI DAO QUẢN LÝ LỊCH SỬ ĐẶT GIÁ: BidTransactionDAO (Tuần 7 - Vị trí 2)
 * =========================================================================
 * - Thực thi giao diện {@link IBidTransactionDAO} theo đúng thiết kế Sơ đồ lớp UML.
 * - Lưu vết mọi lượt trả giá hợp lệ vào bảng `bid_transactions` và hỗ trợ truy vấn
 *   lịch sử đặt giá theo từng phiên (`findByAuctionId`) hoặc toàn hệ thống (`findAll`).
 */
public class BidTransactionDAO implements IBidTransactionDAO {

    /** Kết nối CSDL theo thiết kế UML (- con: Connection). */
    private Connection con;

    /** DAO truy xuất thông tin người đặt giá (- userDAO: IUserDAO). */
    private IUserDAO userDAO;

    /** DAO truy xuất thông tin phiên đấu giá (- auctionDAO: IAuctionDAO). */
    private IAuctionDAO auctionDAO;

    /**
     * Constructor mặc định: Sử dụng kết nối hiện tại từ {@link DBConnection}.
     */
    public BidTransactionDAO() {
        this(DBConnection.getConnection());
    }

    /**
     * Constructor nhận đối tượng Connection truyền vào (khớp `+ BidTransactionDAO(con: Connection)` trong UML).
     *
     * @param con Kết nối CSDL
     */
    public BidTransactionDAO(Connection con) {
        this.con = con;
        this.userDAO = new UserDAO(con);
        this.auctionDAO = new AuctionDAO(con, this.userDAO, new ItemDAO(con, this.userDAO));
    }

    /**
     * Constructor nhận đầy đủ Connection, IUserDAO và IAuctionDAO.
     */
    public BidTransactionDAO(Connection con, IUserDAO userDAO, IAuctionDAO auctionDAO) {
        this.con = con;
        this.userDAO = userDAO != null ? userDAO : new UserDAO(con);
        this.auctionDAO = auctionDAO != null ? auctionDAO : new AuctionDAO(con);
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
     * Lưu một lượt đặt giá hợp lệ mới vào bảng `bid_transactions`.
     * Tự động gán ID vừa sinh trong DB ngược lại vào đối tượng `bid`.
     *
     * @param bid Giao dịch đặt giá cần lưu
     */
    @Override
    public void save(BidTransaction bid) {
        if (bid == null) {
            throw new IllegalArgumentException("Đối tượng BidTransaction không được null.");
        }
        String sql = "INSERT INTO bid_transactions (auction_id, bidder_id, amount, time) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = getActiveConnection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            int auctionId = bid.getAuctionId() != null ? bid.getAuctionId() : 1;
            int bidderId = (bid.getBidder() != null && bid.getBidder().getId() != null)
                    ? bid.getBidder().getId() : 1;
            LocalDateTime bidTime = bid.getTime() != null ? bid.getTime() : LocalDateTime.now();

            ps.setInt(1, auctionId);
            ps.setInt(2, bidderId);
            ps.setDouble(3, bid.getAmount());
            ps.setTimestamp(4, Timestamp.valueOf(bidTime));
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    bid.setId(keys.getInt(1));
                }
            }
        } catch (SQLException e) {
            System.err.println("[BidTransactionDAO] Lỗi khi lưu BidTransaction: " + e.getMessage());
            throw new RuntimeException("Không thể lưu lịch sử đặt giá: " + e.getMessage(), e);
        }
    }

    /**
     * Lấy danh sách toàn bộ các lượt đặt giá của một phiên đấu giá cụ thể theo thứ tự thời gian tăng dần
     * (phục vụ kiểm tra bước giá hợp lệ và vẽ biểu đồ biến động giá LineChart).
     *
     * @param auctionId Mã định danh của phiên đấu giá
     * @return Danh sách các lượt đặt giá thuộc phiên đó
     */
    @Override
    public List<BidTransaction> findByAuctionId(Integer auctionId) {
        List<BidTransaction> list = new ArrayList<>();
        if (auctionId == null) {
            return list;
        }
        String sql = "SELECT id, auction_id, bidder_id, amount, time FROM bid_transactions WHERE auction_id = ? ORDER BY time ASC, id ASC";
        try (PreparedStatement ps = getActiveConnection().prepareStatement(sql)) {
            ps.setInt(1, auctionId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(map(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[BidTransactionDAO] Lỗi khi tìm BidTransaction theo auctionId (" + auctionId + "): " + e.getMessage());
        }
        return list;
    }

    /**
     * Lấy danh sách toàn bộ lịch sử đặt giá trên toàn hệ thống (phục vụ màn hình HistoryView).
     *
     * @return Danh sách tất cả các BidTransaction
     */
    @Override
    public List<BidTransaction> findAll() {
        List<BidTransaction> list = new ArrayList<>();
        String sql = "SELECT id, auction_id, bidder_id, amount, time FROM bid_transactions ORDER BY time DESC, id DESC";
        try (PreparedStatement ps = getActiveConnection().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(map(rs));
            }
        } catch (SQLException e) {
            System.err.println("[BidTransactionDAO] Lỗi khi lấy toàn bộ lịch sử đặt giá: " + e.getMessage());
        }
        return list;
    }

    /**
     * Ánh xạ một dòng dữ liệu {@link ResultSet} từ bảng `bid_transactions` thành đối tượng {@link BidTransaction}.
     * Khớp chính xác với `- map(rs: ResultSet): BidTransaction` trong sơ đồ lớp UML.
     *
     * @param rs ResultSet đang trỏ tới bản ghi hợp lệ
     * @return Đối tượng BidTransaction hoàn chỉnh
     * @throws SQLException nếu lỗi đọc cột dữ liệu
     */
    private BidTransaction map(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        int auctionId = rs.getInt("auction_id");
        int bidderId = rs.getInt("bidder_id");
        double amount = rs.getDouble("amount");
        Timestamp ts = rs.getTimestamp("time");

        User bidder = userDAO.findById(bidderId);
        LocalDateTime time = ts != null ? ts.toLocalDateTime() : LocalDateTime.now();

        return new BidTransaction(id, auctionId, amount, bidder, time);
    }
}
