package com.auction.server;

import com.auction.dao.DatabaseInitializer;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * =========================================================================
 * MÁY CHỦ ĐẤU GIÁ ĐA LUỒNG & OBSERVER SUBJECT: AuctionServer (Tuần 7 - Vị trí 2)
 * =========================================================================
 * - Mở cổng kết nối TCP (`ServerSocket`, mặc định cổng 8888) lắng nghe nhiều Client cùng lúc.
 * - Với mỗi Client kết nối tới, tạo một luồng {@link ClientHandler} riêng biệt trong ThreadPool
 *   để phục vụ độc lập, không làm nghẽn các Client khác.
 * - Đóng vai trò **Subject (Chủ thể)** trong mẫu thiết kế **Observer Pattern**:
 *   + Quản lý danh sách các luồng xuất của Client đăng ký theo dõi (`+ observers: Set<PrintWriter>`).
 *   + Cung cấp hàm `+ broadcast(msg: Message): void` để phát thông báo tức thời (realtime)
 *     tới tất cả các Client đang kết nối ngay khi có lượt đặt giá mới hoặc tin nhắn mới.
 */
public class AuctionServer {

    /** Cổng lắng nghe TCP mặc định của Server (khớp với cấu hình NetworkClient phía Client). */
    public static final int DEFAULT_PORT = 8888;

    /**
     * Danh sách các luồng xuất (PrintWriter) của các Client đang kết nối nhận thông báo realtime.
     * Sử dụng `ConcurrentHashMap.newKeySet()` để đảm bảo an toàn đa luồng (Thread-safe).
     * Khớp chính xác với thuộc tính `+ observers: Set<PrintWriter>` trong sơ đồ lớp UML.
     */
    public static final Set<PrintWriter> observers = ConcurrentHashMap.newKeySet();

    /**
     * Danh sách lưu lịch sử tin nhắn chat chung trên sàn đấu giá.
     * Khớp chính xác với thuộc tính `+ chatMessages: List<String>` trong sơ đồ lớp UML.
     */
    public static final List<String> chatMessages = Collections.synchronizedList(new ArrayList<>());

    private final int port;
    private volatile boolean running;
    private ServerSocket serverSocket;
    private final ExecutorService clientThreadPool = Executors.newCachedThreadPool();

    public AuctionServer() {
        this(DEFAULT_PORT);
    }

    public AuctionServer(int port) {
        this.port = port;
    }

    /**
     * Đăng ký thêm một Client vào danh sách nhận thông báo tức thời (Observer Attach).
     *
     * @param out Luồng ghi PrintWriter của Client
     */
    public static void addObserver(PrintWriter out) {
        if (out != null) {
            observers.add(out);
        }
    }

    /**
     * Gỡ một Client khỏi danh sách nhận thông báo khi Client ngắt kết nối (Observer Detach).
     *
     * @param out Luồng ghi PrintWriter của Client
     */
    public static void removeObserver(PrintWriter out) {
        if (out != null) {
            observers.remove(out);
        }
    }

    /**
     * Phát (Broadcast) một gói tin {@link Message} tức thời tới toàn bộ các Client trong `observers`.
     * Khớp chính xác với phương thức `+ broadcast(msg: Message): void` trong sơ đồ lớp UML.
     *
     * @param msg Gói tin cần phát tới các Client đang kết nối
     */
    public static void broadcast(Message msg) {
        if (msg == null) {
            return;
        }
        String json = GsonConfig.getGson().toJson(msg);
        for (PrintWriter writer : observers) {
            try {
                synchronized (writer) {
                    writer.println(json);
                    writer.flush();
                }
            } catch (Exception e) {
                observers.remove(writer);
            }
        }
    }

    /**
     * Khởi chạy Server: Tự động khởi tạo bảng CSDL và mở vòng lặp chờ kết nối Socket từ Client.
     */
    public void start() {
        running = true;
        try {
            // Tự động tạo các bảng CSDL nếu chưa tồn tại
            DatabaseInitializer.initialize();
        } catch (Exception e) {
            System.err.println("[AuctionServer] Cảnh báo khi khởi tạo CSDL: " + e.getMessage());
        }

        try (ServerSocket ss = new ServerSocket(port)) {
            this.serverSocket = ss;
            System.out.println("==============================================================");
            System.out.println("[AuctionServer] Đã khởi động thành công trên cổng TCP: " + port);
            System.out.println("[AuctionServer] Đang chờ các Client kết nối...");
            System.out.println("==============================================================");

            while (running && !serverSocket.isClosed()) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    System.out.println("[AuctionServer] Client mới kết nối từ: " + clientSocket.getRemoteSocketAddress());

                    // Tạo luồng ClientHandler riêng biệt để phục vụ Client vừa kết nối
                    ClientHandler handler = new ClientHandler(clientSocket);
                    clientThreadPool.execute(handler);
                } catch (IOException e) {
                    if (running) {
                        System.err.println("[AuctionServer] Lỗi khi nhận kết nối Client: " + e.getMessage());
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("[AuctionServer] Không thể mở cổng " + port + ": " + e.getMessage());
        } finally {
            stop();
        }
    }

    /**
     * Dừng Server an toàn và đóng toàn bộ các luồng kết nối.
     */
    public void stop() {
        running = false;
        observers.clear();
        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
            } catch (IOException ignored) {
            }
        }
        clientThreadPool.shutdownNow();
        System.out.println("[AuctionServer] Đã dừng máy chủ.");
    }

    /**
     * Điểm khởi chạy chính của ứng dụng Server (`mvn exec:java`).
     * Khớp chính xác với `+ main(args: String[]): void` trong sơ đồ lớp UML.
     *
     * @param args Tham số dòng lệnh (có thể truyền cổng tùy chọn ở args[0])
     */
    public static void main(String[] args) {
        int serverPort = DEFAULT_PORT;
        if (args != null && args.length > 0) {
            try {
                serverPort = Integer.parseInt(args[0]);
            } catch (NumberFormatException ignored) {
            }
        }
        AuctionServer server = new AuctionServer(serverPort);
        Runtime.getRuntime().addShutdownHook(new Thread(server::stop));
        server.start();
    }
}
