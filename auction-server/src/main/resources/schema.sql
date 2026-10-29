-- =========================================================================
-- DATABASE SCHEMA: HỆ THỐNG ĐẤU GIÁ TRỰC TUYẾN (ONLINE AUCTION SYSTEM)
-- Thiết kế chuẩn theo sơ đồ lớp UML của nhóm
-- =========================================================================

CREATE DATABASE IF NOT EXISTS auction_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE auction_db;

-- 1. Bảng Người dùng (User -> Bidder, Seller, Admin)
CREATE TABLE IF NOT EXISTS users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(64) NOT NULL COMMENT 'Mật khẩu băm SHA-256',
    role VARCHAR(20) NOT NULL COMMENT 'BIDDER, SELLER, ADMIN',
    balance DOUBLE NOT NULL DEFAULT 0.0
);

-- 2. Bảng Sản phẩm đấu giá (Item -> Electronics, Art, Vehicle)
CREATE TABLE IF NOT EXISTS items (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    owner_id INT NOT NULL,
    description TEXT,
    category VARCHAR(30) NOT NULL COMMENT 'ELECTRONICS, ART, VEHICLE',
    image_data LONGTEXT,
    -- Thuộc tính riêng của Electronics
    brand VARCHAR(100),
    warranty_months INT,
    -- Thuộc tính riêng của Art
    artist VARCHAR(100),
    year_created INT,
    -- Thuộc tính riêng của Vehicle
    manufacturer VARCHAR(100),
    odometer DOUBLE,
    CONSTRAINT fk_items_owner FOREIGN KEY (owner_id) REFERENCES users(id) ON DELETE CASCADE
);

-- 3. Bảng Phiên đấu giá (Auction - khớp các trường trong UML)
CREATE TABLE IF NOT EXISTS auctions (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    starting_price DOUBLE NOT NULL,
    item_id INT NOT NULL,
    seller_id INT NOT NULL,
    max_bid DOUBLE NOT NULL DEFAULT 0.0,
    current_winner_id INT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN' COMMENT 'OPEN, RUNNING, FINISHED, PAID, CANCELED',
    start_time DATETIME NOT NULL,
    end_time DATETIME NOT NULL,
    reserved_price DOUBLE NOT NULL DEFAULT 0.0,
    CONSTRAINT fk_auctions_item FOREIGN KEY (item_id) REFERENCES items(id) ON DELETE CASCADE,
    CONSTRAINT fk_auctions_seller FOREIGN KEY (seller_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_auctions_winner FOREIGN KEY (current_winner_id) REFERENCES users(id) ON DELETE SET NULL
);

-- 4. Bảng Lịch sử giao dịch đặt giá (BidTransaction)
CREATE TABLE IF NOT EXISTS bid_transactions (
    id INT AUTO_INCREMENT PRIMARY KEY,
    auction_id INT NOT NULL,
    bidder_id INT NOT NULL,
    amount DOUBLE NOT NULL,
    time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_bids_auction FOREIGN KEY (auction_id) REFERENCES auctions(id) ON DELETE CASCADE,
    CONSTRAINT fk_bids_bidder FOREIGN KEY (bidder_id) REFERENCES users(id) ON DELETE CASCADE
);

-- 5. Bảng Cấu hình đấu giá tự động (AutoBidConfig)
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
);
