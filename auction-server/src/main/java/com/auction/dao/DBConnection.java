package com.auction.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Quản lý kết nối Cơ sở dữ liệu theo đúng thiết kế UML và Singleton Pattern.
 * Sử dụng ThreadLocal<Connection> để đảm bảo an toàn đa luồng (Multi-threading) cho Server.
 */
public class DBConnection {

    /** Đường dẫn kết nối JDBC tới MySQL (XAMPP) hoặc SQLite. */
    private static final String URL = System.getProperty("db.url",
            System.getenv().getOrDefault("DB_URL",
                    "jdbc:mysql://localhost:3306/auction_db?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=UTF-8"));

    private static final String USER = System.getProperty("db.user",
            System.getenv().getOrDefault("DB_USER", "root"));

    private static final String PASSWORD = System.getProperty("db.password",
            System.getenv().getOrDefault("DB_PASSWORD", ""));

    /** Lưu trữ kết nối riêng biệt cho từng luồng thực thi theo đúng thiết kế UML. */
    private static final ThreadLocal<Connection> connectionHolder = new ThreadLocal<>();

    /** Thể hiện duy nhất của DBConnection (Singleton Pattern). */
    private static volatile DBConnection instance;

    static {
        try {
            if (URL.startsWith("jdbc:sqlite:")) {
                Class.forName("org.sqlite.JDBC");
            } else {
                Class.forName("com.mysql.cj.jdbc.Driver");
            }
        } catch (ClassNotFoundException e) {
            System.err.println("[DBConnection] Không tìm thấy JDBC Driver: " + e.getMessage());
        }
    }

    private DBConnection() {
    }

    /**
     * Lấy thể hiện duy nhất của DBConnection (Singleton).
     *
     * @return Thể hiện DBConnection
     */
    public static DBConnection getInstance() {
        if (instance == null) {
            synchronized (DBConnection.class) {
                if (instance == null) {
                    instance = new DBConnection();
                }
            }
        }
        return instance;
    }

    /**
     * Lấy đối tượng Connection cho luồng hiện tại (khớp với + getConnection(): Connection trong UML).
     *
     * @return Đối tượng {@link Connection}
     */
    public static Connection getConnection() {
        try {
            Connection conn = connectionHolder.get();
            if (conn == null || conn.isClosed()) {
                if (URL.startsWith("jdbc:sqlite:")) {
                    conn = DriverManager.getConnection(URL);
                } else {
                    conn = DriverManager.getConnection(URL, USER, PASSWORD);
                }
                connectionHolder.set(conn);
            }
            return conn;
        } catch (SQLException e) {
            System.err.println("[DBConnection] Lỗi kết nối CSDL: " + e.getMessage());
            return null;
        }
    }

    /**
     * Đóng kết nối CSDL của luồng hiện tại và giải phóng khỏi ThreadLocal
     * (khớp với + closeConnection(): void trong UML).
     */
    public static void closeConnection() {
        Connection conn = connectionHolder.get();
        if (conn != null) {
            try {
                if (!conn.isClosed()) {
                    conn.close();
                }
            } catch (SQLException e) {
                System.err.println("[DBConnection] Lỗi khi đóng kết nối CSDL: " + e.getMessage());
            } finally {
                connectionHolder.remove();
            }
        }
    }
}
