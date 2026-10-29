package com.auction.dao;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Lớp khởi tạo cấu trúc Cơ sở dữ liệu (Database Schema / Tables) theo đúng sơ đồ UML.
 */
public class DatabaseInitializer {

    /** Bảng lưu trữ thông tin người dùng (User -> Bidder, Seller, Admin). */
    public static final String CREATE_USERS_TABLE = """
            CREATE TABLE IF NOT EXISTS users (
                id INT AUTO_INCREMENT PRIMARY KEY,
                username VARCHAR(50) NOT NULL UNIQUE,
                password VARCHAR(64) NOT NULL,
                role VARCHAR(20) NOT NULL,
                balance DOUBLE NOT NULL DEFAULT 0.0
            )
            """;

    /** Bảng lưu trữ sản phẩm đấu giá (Item -> Electronics, Art, Vehicle). */
    public static final String CREATE_ITEMS_TABLE = """
            CREATE TABLE IF NOT EXISTS items (
                id INT AUTO_INCREMENT PRIMARY KEY,
                name VARCHAR(255) NOT NULL,
                owner_id INT NOT NULL,
                description TEXT,
                category VARCHAR(30) NOT NULL,
                image_data LONGTEXT,
                brand VARCHAR(100),
                warranty_months INT,
                artist VARCHAR(100),
                year_created INT,
                manufacturer VARCHAR(100),
                odometer DOUBLE,
                CONSTRAINT fk_items_owner FOREIGN KEY (owner_id) REFERENCES users(id) ON DELETE CASCADE
            )
            """;

    /** Bảng lưu trữ phiên đấu giá (Auction) khớp với các thuộc tính trong UML. */
    public static final String CREATE_AUCTIONS_TABLE = """
            CREATE TABLE IF NOT EXISTS auctions (
                id INT AUTO_INCREMENT PRIMARY KEY,
                name VARCHAR(255) NOT NULL,
                starting_price DOUBLE NOT NULL,
                item_id INT NOT NULL,
                seller_id INT NOT NULL,
                max_bid DOUBLE NOT NULL DEFAULT 0.0,
                current_winner_id INT NULL,
                status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
                start_time DATETIME NOT NULL,
                end_time DATETIME NOT NULL,
                reserved_price DOUBLE NOT NULL DEFAULT 0.0,
                CONSTRAINT fk_auctions_item FOREIGN KEY (item_id) REFERENCES items(id) ON DELETE CASCADE,
                CONSTRAINT fk_auctions_seller FOREIGN KEY (seller_id) REFERENCES users(id) ON DELETE CASCADE,
                CONSTRAINT fk_auctions_winner FOREIGN KEY (current_winner_id) REFERENCES users(id) ON DELETE SET NULL
            )
            """;

    /** Bảng lưu vết lịch sử giao dịch đặt giá (BidTransaction). */
    public static final String CREATE_BID_TRANSACTIONS_TABLE = """
            CREATE TABLE IF NOT EXISTS bid_transactions (
                id INT AUTO_INCREMENT PRIMARY KEY,
                auction_id INT NOT NULL,
                bidder_id INT NOT NULL,
                amount DOUBLE NOT NULL,
                time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                CONSTRAINT fk_bids_auction FOREIGN KEY (auction_id) REFERENCES auctions(id) ON DELETE CASCADE,
                CONSTRAINT fk_bids_bidder FOREIGN KEY (bidder_id) REFERENCES users(id) ON DELETE CASCADE
            )
            """;

    /** Bảng lưu cấu hình đấu giá tự động (AutoBidConfig). */
    public static final String CREATE_AUTO_BID_CONFIGS_TABLE = """
            CREATE TABLE IF NOT EXISTS auto_bid_configs (
                id INT AUTO_INCREMENT PRIMARY KEY,
                auction_id INT NOT NULL,
                user_id INT NOT NULL,
                max_bid_limit DOUBLE NOT NULL,
                increment DOUBLE NOT NULL,
                registered_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                CONSTRAINT uk_auction_user UNIQUE (auction_id, user_id),
                CONSTRAINT fk_autobid_auction FOREIGN KEY (auction_id) REFERENCES auctions(id) ON DELETE CASCADE,
                CONSTRAINT fk_autobid_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
            )
            """;

    /**
     * Thực thi tạo toàn bộ các bảng trong Cơ sở dữ liệu thông qua {@link DBConnection}.
     */
    public static void initialize() {
        Connection conn = DBConnection.getConnection();
        if (conn == null) {
            System.err.println("[DatabaseInitializer] Không thể khởi tạo bảng do chưa có kết nối CSDL.");
            return;
        }
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(CREATE_USERS_TABLE);
            stmt.execute(CREATE_ITEMS_TABLE);
            stmt.execute(CREATE_AUCTIONS_TABLE);
            stmt.execute(CREATE_BID_TRANSACTIONS_TABLE);
            stmt.execute(CREATE_AUTO_BID_CONFIGS_TABLE);
            System.out.println("[DatabaseInitializer] Đã khởi tạo đầy đủ các bảng CSDL theo chuẩn UML thành công.");
        } catch (SQLException e) {
            System.err.println("[DatabaseInitializer] Lỗi khi tạo bảng CSDL: " + e.getMessage());
        } finally {
            DBConnection.closeConnection();
        }
    }

    public static void main(String[] args) {
        initialize();
    }
}
