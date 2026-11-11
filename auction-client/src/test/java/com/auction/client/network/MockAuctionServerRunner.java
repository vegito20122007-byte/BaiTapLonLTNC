package com.auction.client.network;

import com.auction.client.util.GsonConfig;
import com.auction.model.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Máy chủ giả lập độc lập phục vụ RIÊNG CHO CÁC BÀI TEST JUNIT (Test Harness/Double).
 * Đặt trong src/test/java để đảm bảo khi đóng gói JAR ứng dụng thật hoàn toàn sạch bóng code Mock.
 */
public class MockAuctionServerRunner {

    public static final int DEFAULT_PORT = 8888;
    public static final String DEFAULT_PASSWORD = "12345678910";

    private final int port;
    private ServerSocket serverSocket;
    private ExecutorService threadPool;
    private volatile boolean running = false;

    // Kho dữ liệu lưu trữ trong RAM (In-Memory Data Store)
    private final Map<String, User> userStore = new ConcurrentHashMap<>();
    private final List<Auction> auctionStore = new CopyOnWriteArrayList<>();
    private final Set<PrintWriter> observers = ConcurrentHashMap.newKeySet();
    private final AtomicInteger userIdGenerator = new AtomicInteger(10);
    private final AtomicInteger auctionIdGenerator = new AtomicInteger(100);

    public void broadcast(Message msg) {
        if (msg == null) return;
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

    public MockAuctionServerRunner() {
        this(DEFAULT_PORT);
    }

    public MockAuctionServerRunner(int port) {
        this.port = port;
        initDefaultData();
    }

    /**
     * Khởi tạo dữ liệu người dùng và sản phẩm đấu giá mẫu ban đầu.
     */
    private void initDefaultData() {
        userStore.clear();
        auctionStore.clear();

        // 1. Tạo 3 tài khoản mẫu chuẩn
        Admin admin = new Admin(1, "admin", DEFAULT_PASSWORD);
        admin.setBalance(0.0);
        userStore.put("admin", admin);

        Seller seller = new Seller(2, "seller", DEFAULT_PASSWORD);
        seller.setBalance(50_000_000.0);
        userStore.put("seller", seller);

        Bidder bidder = new Bidder(3, "bidder", DEFAULT_PASSWORD);
        bidder.setBalance(100_000_000.0);
        userStore.put("bidder", bidder);

        // 2. Tạo một số phiên đấu giá mẫu
        Item item1 = ItemFactory.createItem("ART", 1, "Tranh Sơn Dầu Phố Cổ Hà Nội", "Tranh sơn dầu vẽ phố cổ thập niên 90", seller);
        Auction auction1 = new Auction(1, "Đấu giá Tranh Phố Cổ", item1, seller, 5_000_000.0, 7_000_000.0,
                LocalDateTime.now(), LocalDateTime.now().plusDays(3));

        Item item2 = ItemFactory.createItem("VEHICLE", 2, "Xe Máy Honda Super Cub 50cc", "Xe nguyên bản sưu tầm màu xanh ngọc", seller);
        Auction auction2 = new Auction(2, "Đấu giá Honda Cub Cổ", item2, seller, 15_000_000.0, 20_000_000.0,
                LocalDateTime.now(), LocalDateTime.now().plusDays(5));

        Item item3 = ItemFactory.createItem("ELECTRONICS", 3, "Máy ảnh cơ Leica M3 Vintage", "Máy ảnh phim cổ điển năm 1954 còn hoạt động tốt", seller);
        Auction auction3 = new Auction(3, "Đấu giá Leica M3 Cổ", item3, seller, 25_000_000.0, 30_000_000.0,
                LocalDateTime.now(), LocalDateTime.now().plusDays(7));

        auctionStore.add(auction1);
        auctionStore.add(auction2);
        auctionStore.add(auction3);
    }

    /**
     * Khởi động máy chủ Socket và lắng nghe kết nối từ Client.
     */
    public synchronized void start() throws IOException {
        if (running) {
            return;
        }

        serverSocket = new ServerSocket(port);
        threadPool = Executors.newCachedThreadPool();
        running = true;

        threadPool.submit(() -> {
            while (running && !serverSocket.isClosed()) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    threadPool.submit(() -> handleClient(clientSocket));
                } catch (IOException e) {
                    if (!running) break;
                }
            }
        });
    }

    /**
     * Xử lý trao đổi gói tin JSON với một Client cụ thể.
     */
    private void handleClient(Socket socket) {
        try (
                BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                PrintWriter writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true)
        ) {
            observers.add(writer);
            String jsonLine;
            while (running && (jsonLine = reader.readLine()) != null) {
                if (jsonLine.trim().isEmpty()) continue;

                Message request = GsonConfig.getGson().fromJson(jsonLine, Message.class);
                Message response = processRequest(request);

                String jsonResponse = GsonConfig.getGson().toJson(response);
                synchronized (writer) {
                    writer.println(jsonResponse);
                    writer.flush();
                }
            }
        } catch (IOException e) {
            // Client disconnects
        } finally {
            try {
                if (!socket.isClosed()) socket.close();
            } catch (IOException ignored) {
            }
        }
    }

    /**
     * Xử lý nghiệp vụ theo từng loại hành động (Action).
     */
    @SuppressWarnings("unchecked")
    private Message processRequest(Message request) {
        if (request == null || request.getAction() == null) {
            return Message.error("UNKNOWN", "Gói tin yêu cầu không hợp lệ!");
        }

        String action = request.getAction().toUpperCase();
        try {
            switch (action) {
                case Message.ACTION_LOGIN:
                    return handleLogin(request);

                case Message.ACTION_REGISTER:
                    return handleRegister(request);

                case Message.ACTION_GET_AUCTIONS:
                    return handleGetAuctions();

                case Message.ACTION_CREATE_AUCTION:
                    return handleCreateAuction(request);

                case Message.ACTION_PLACE_BID:
                    return handlePlaceBid(request);

                case Message.ACTION_LOGOUT:
                    return Message.success(Message.ACTION_LOGOUT, "Đăng xuất thành công");

                case "PING":
                    return Message.success("PONG", "OK");

                default:
                    return Message.error(action, "Hành động chưa được hỗ trợ: " + action);
            }
        } catch (Exception e) {
            return Message.error(action, "Lỗi máy chủ nội bộ: " + e.getMessage());
        }
    }

    /**
     * Xử lý Đăng nhập (LOGIN).
     */
    @SuppressWarnings("unchecked")
    private Message handleLogin(Message request) {
        Map<String, String> credentials = request.parsePayload(Map.class);
        if (credentials == null || !credentials.containsKey("username") || !credentials.containsKey("password")) {
            return Message.error(Message.ACTION_LOGIN, "Dữ liệu đăng nhập thiếu username hoặc password!");
        }

        String username = credentials.get("username").trim().toLowerCase();
        String password = credentials.get("password").trim();

        User user = userStore.get(username);
        if (user == null) {
            return Message.error(Message.ACTION_LOGIN, "Tài khoản '" + username + "' không tồn tại trên hệ thống!");
        }

        if (!user.authenticatePassword(password)) {
            return Message.error(Message.ACTION_LOGIN, "Mật khẩu không chính xác cho tài khoản '" + username + "'!");
        }

        return Message.success(Message.ACTION_LOGIN, user);
    }

    /**
     * Xử lý Đăng ký (REGISTER).
     */
    @SuppressWarnings("unchecked")
    private Message handleRegister(Message request) {
        Map<String, String> regData = request.parsePayload(Map.class);
        if (regData == null || !regData.containsKey("username") || !regData.containsKey("password") || !regData.containsKey("role")) {
            return Message.error(Message.ACTION_REGISTER, "Dữ liệu đăng ký không đầy đủ!");
        }

        String username = regData.get("username").trim();
        String password = regData.get("password").trim();
        String role = regData.get("role").trim().toUpperCase();

        String key = username.toLowerCase();
        if (userStore.containsKey(key)) {
            return Message.error(Message.ACTION_REGISTER, "Tên tài khoản '" + username + "' đã được sử dụng!");
        }

        int newId = userIdGenerator.incrementAndGet();
        User newUser = UserFactory.createUser(newId, username, password, role);
        if (newUser instanceof Bidder) {
            newUser.setBalance(10_000_000.0);
        }
        userStore.put(key, newUser);

        return Message.success(Message.ACTION_REGISTER, newUser);
    }

    /**
     * Xử lý Lấy danh sách phiên đấu giá (GET_AUCTIONS).
     */
    private Message handleGetAuctions() {
        return Message.success(Message.ACTION_GET_AUCTIONS, new ArrayList<>(auctionStore));
    }

    /**
     * Xử lý Tạo phiên đấu giá mới (CREATE_AUCTION).
     */
    private Message handleCreateAuction(Message request) {
        Auction newAuction = request.parsePayload(Auction.class);
        if (newAuction == null) {
            return Message.error(Message.ACTION_CREATE_AUCTION, "Dữ liệu phiên đấu giá rỗng!");
        }

        newAuction.setId(auctionIdGenerator.incrementAndGet());
        auctionStore.add(newAuction);
        broadcast(Message.success(Message.ACTION_AUCTION_UPDATE, newAuction));
        return Message.success(Message.ACTION_CREATE_AUCTION, newAuction);
    }

    /**
     * Xử lý Đặt giá (PLACE_BID).
     */
    @SuppressWarnings("unchecked")
    private Message handlePlaceBid(Message request) {
        Map<String, Object> bidData = request.parsePayload(Map.class);
        if (bidData == null || !bidData.containsKey("auctionId") || (!bidData.containsKey("bidAmount") && !bidData.containsKey("amount"))) {
            return Message.error(Message.ACTION_PLACE_BID, "Thông tin đặt giá không hợp lệ!");
        }

        int auctionId = ((Number) bidData.get("auctionId")).intValue();
        Object amtObj = bidData.containsKey("amount") ? bidData.get("amount") : bidData.get("bidAmount");
        double bidAmount = ((Number) amtObj).doubleValue();
        int bidderId = bidData.containsKey("bidderId") ? ((Number) bidData.get("bidderId")).intValue() : -1;

        for (Auction a : auctionStore) {
            if (a.getId() == auctionId) {
                if (bidAmount <= a.getMaxBid()) {
                    return Message.error(Message.ACTION_PLACE_BID, "Giá đặt phải lớn hơn giá cao nhất hiện tại (" + a.getMaxBid() + " VNĐ)!");
                }
                a.setMaxBid(bidAmount);

                if (bidderId > 0) {
                    for (User u : userStore.values()) {
                        if (u.getId() != null && u.getId().equals(bidderId)) {
                            a.setCurrentWinner(u);
                            break;
                        }
                    }
                }

                LocalDateTime now = LocalDateTime.now();
                if (a.getEndTime() != null && a.getEndTime().isAfter(now)) {
                    long remainingSeconds = java.time.Duration.between(now, a.getEndTime()).getSeconds();
                    if (remainingSeconds <= 30) {
                        a.setEndTime(a.getEndTime().plusSeconds(60));
                    }
                }

                broadcast(Message.success(Message.ACTION_BID_UPDATE, a));
                return Message.success(Message.ACTION_PLACE_BID, a);
            }
        }

        return Message.error(Message.ACTION_PLACE_BID, "Không tìm thấy phiên đấu giá #" + auctionId);
    }

    /**
     * Dừng máy chủ an toàn.
     */
    public synchronized void stop() {
        running = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException ignored) {
        }

        if (threadPool != null) {
            threadPool.shutdownNow();
        }
    }

    public boolean isRunning() {
        return running;
    }

    public int getPort() {
        return port;
    }
}
