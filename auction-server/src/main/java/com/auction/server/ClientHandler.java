package com.auction.server;

import com.auction.service.AuctionService;
import com.auction.exception.AuctionException;
import com.auction.exception.AuctionNotFoundException;
import com.auction.exception.AuthenticationException;
import com.auction.exception.DataValidationException;
import com.auction.dao.AuctionDAO;
import com.auction.dao.BidTransactionDAO;
import com.auction.dao.DBConnection;
import com.auction.dao.IAuctionDAO;
import com.auction.dao.IBidTransactionDAO;
import com.auction.dao.IItemDAO;
import com.auction.dao.IUserDAO;
import com.auction.dao.ItemDAO;
import com.auction.dao.UserDAO;
import com.auction.model.Auction;
import com.auction.model.BidTransaction;
import com.auction.model.Status;
import com.auction.model.User;
import com.auction.model.UserFactory;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * =========================================================================
 * LUỒNG PHỤC VỤ TỪNG CLIENT TRÊN SERVER: ClientHandler (Tuần 7 - Vị trí 2)
 * =========================================================================
 * - Thực thi giao diện {@link Runnable} để chạy trên luồng riêng biệt cho mỗi Client kết nối.
 * - Tuân thủ 100% các thuộc tính và phương thức trong hộp lớp `ClientHandler` trên sơ đồ UML:
 *   + `- socket: Socket` (Kết nối TCP với Client)
 *   + `- in: BufferedReader` (Luồng đọc chuỗi JSON gửi lên từ Client)
 *   + `- out: PrintWriter` (Luồng ghi chuỗi JSON phản hồi về cho Client)
 *   + `- gson: Gson` (Bộ chuyển đổi đối tượng Message <-> JSON)
 *   + `+ run(): void` (Vòng lặp lắng nghe yêu cầu từ Client)
 *   + `- processRequest(request: Message): Message` (Điều phối xử lý nghiệp vụ theo mã `action`)
 *   + `- hashSHA256(base: String): String` (Băm mật khẩu chuẩn SHA-256)
 */
public class ClientHandler implements Runnable {

    /** Kết nối Socket của Client (- socket: Socket). */
    private Socket socket;

    /** Luồng đọc dữ liệu từ Client (- in: BufferedReader). */
    private BufferedReader in;

    /** Luồng ghi dữ liệu về Client (- out: PrintWriter). */
    private PrintWriter out;

    /** Đối tượng chuyển đổi JSON (- gson: Gson). */
    private Gson gson = GsonConfig.getGson();

    /** Tầng nghiệp vụ xử lý logic đấu giá và khóa đa luồng (- auctionService: AuctionService). */
    private final AuctionService auctionService = AuctionService.getInstance();

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    /**
     * Vòng lặp chính của luồng phục vụ Client (khớp `+ run(): void` trong sơ đồ UML):
     * 1. Khởi tạo luồng đọc (`in`) và ghi (`out`) UTF-8.
     * 2. Đăng ký `out` vào danh sách `AuctionServer.observers` để nhận thông báo realtime.
     * 3. Liên tục đọc từng gói tin JSON, gọi `processRequest` xử lý và trả kết quả về Client.
     * 4. Khi Client ngắt kết nối: gỡ khỏi `observers` và đóng kết nối CSDL của luồng hiện tại.
     */
    @Override
    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);

            // Đăng ký Client này vào danh sách Observer của AuctionServer
            AuctionServer.addObserver(out);

            String line;
            while ((line = in.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }
                Message response;
                try {
                    Message request = gson.fromJson(line, Message.class);
                    response = processRequest(request);
                } catch (Exception e) {
                    response = Message.error("INVALID_MESSAGE", "Gói tin JSON không hợp lệ: " + e.getMessage());
                }

                if (response != null) {
                    synchronized (out) {
                        out.println(gson.toJson(response));
                        out.flush();
                    }
                }
            }
        } catch (IOException e) {
            System.out.println("[ClientHandler] Client ngắt kết nối: " + e.getMessage());
        } finally {
            AuctionServer.removeObserver(out);
            DBConnection.closeConnection();
            closeResources();
        }
    }

    /**
     * Phân tích mã hành động (`action`) trong gói tin {@link Message} của Client,
     * gọi tầng DAO xử lý và trả về gói tin phản hồi tương ứng.
     * Khớp chính xác với `- processRequest(request: Message): Message` trong sơ đồ UML.
     *
     * @param request Gói tin yêu cầu từ Client
     * @return Gói tin phản hồi trả về cho Client
     */
    private Message processRequest(Message request) {
        if (request == null || request.getAction() == null || request.getAction().isBlank()) {
            return Message.error("UNKNOWN", "Yêu cầu không hợp lệ hoặc thiếu mã action.");
        }

        String action = request.getAction().trim().toUpperCase();
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

                case Message.ACTION_UPDATE_AUCTION:
                    return handleUpdateAuction(request);

                case Message.ACTION_DELETE_AUCTION:
                    return handleDeleteAuction(request);

                case Message.ACTION_PLACE_BID:
                    return handlePlaceBid(request);

                case Message.ACTION_GET_HISTORY:
                    return handleGetHistory(request);

                case Message.ACTION_CHAT:
                    return handleChat(request);

                case Message.ACTION_SUBSCRIBE:
                    AuctionServer.addObserver(out);
                    return Message.success(Message.ACTION_SUBSCRIBE, "Đã đăng ký nhận thông báo realtime.");

                case Message.ACTION_LOGOUT:
                    return Message.success(Message.ACTION_LOGOUT, "Đăng xuất thành công.");

                default:
                    throw new DataValidationException("Hành động không được hỗ trợ: " + action);
            }
        } catch (AuctionException e) {
            return Message.error(action, e.getMessage());
        } catch (Exception e) {
            System.err.println("[ClientHandler] Lỗi khi xử lý action " + action + ": " + e.getMessage());
            return Message.error(action, e.getMessage());
        }
    }

    /**
     * Xử lý yêu cầu Đăng nhập (LOGIN): Kiểm tra tài khoản và đối chiếu mật khẩu băm SHA-256.
     */
    private Message handleLogin(Message request) {
        JsonObject payload = parseJsonObject(request.getPayload());
        String username = getStringField(payload, "username");
        String password = getStringField(payload, "password");

        if (username.isEmpty() || password.isEmpty()) {
            throw new DataValidationException("Vui lòng nhập đầy đủ tên đăng nhập và mật khẩu.");
        }

        IUserDAO userDAO = new UserDAO();
        User user = userDAO.findByUsername(username);
        if (user == null) {
            throw new AuthenticationException("Tài khoản không tồn tại: " + username);
        }

        String hashedInput = hashSHA256(password);
        if (!hashedInput.equalsIgnoreCase(user.getPassword()) && !password.equals(user.getPassword())) {
            throw new AuthenticationException("Mật khẩu không chính xác.");
        }

        return Message.success(Message.ACTION_LOGIN, user);
    }

    /**
     * Xử lý yêu cầu Đăng ký tài khoản mới (REGISTER): Băm mật khẩu SHA-256 và lưu xuống CSDL.
     */
    private Message handleRegister(Message request) {
        JsonObject payload = parseJsonObject(request.getPayload());
        String username = getStringField(payload, "username");
        String password = getStringField(payload, "password");
        String role = getStringField(payload, "role");
        if (role.isEmpty()) {
            role = "BIDDER";
        }

        if (username.isEmpty() || password.isEmpty()) {
            throw new DataValidationException("Tên đăng nhập và mật khẩu không được để trống.");
        }

        IUserDAO userDAO = new UserDAO();
        if (userDAO.findByUsername(username) != null) {
            throw new AuthenticationException("Tên đăng nhập đã tồn tại: " + username);
        }

        User newUser = UserFactory.createUser(null, username, password, role);
        newUser.setPassword(hashSHA256(password));
        userDAO.save(newUser);

        return Message.success(Message.ACTION_REGISTER, newUser);
    }

    /**
     * Lấy danh sách toàn bộ phiên đấu giá hiện có (GET_AUCTIONS).
     */
    private Message handleGetAuctions() {
        IAuctionDAO auctionDAO = new AuctionDAO();
        List<Auction> auctions = auctionDAO.findAll();
        return Message.success(Message.ACTION_GET_AUCTIONS, auctions);
    }

    /**
     * Tạo mới một phiên đấu giá (CREATE_AUCTION) và phát thông báo Observer cho toàn bộ Client.
     */
    private Message handleCreateAuction(Message request) {
        Auction auction = request.parsePayload(Auction.class);
        if (auction == null) {
            throw new DataValidationException("Dữ liệu phiên đấu giá không hợp lệ.");
        }

        IItemDAO itemDAO = new ItemDAO();
        IAuctionDAO auctionDAO = new AuctionDAO();

        if (auction.getItem() != null) {
            Integer itemId = auction.getItem().getId();
            if (itemId == null || itemId <= 0 || itemDAO.findById(itemId) == null) {
                itemDAO.save(auction.getItem());
            }
        }
        auctionDAO.save(auction);

        AuctionServer.broadcast(Message.success(Message.ACTION_AUCTION_UPDATE, auction));
        return Message.success(Message.ACTION_CREATE_AUCTION, auction);
    }

    /**
     * Cập nhật thông tin phiên đấu giá (UPDATE_AUCTION) và phát thông báo Observer.
     */
    private Message handleUpdateAuction(Message request) {
        Auction auction = request.parsePayload(Auction.class);
        if (auction == null || auction.getId() == null) {
            throw new DataValidationException("Phiên đấu giá cần cập nhật không hợp lệ.");
        }

        IAuctionDAO auctionDAO = new AuctionDAO();
        if (auctionDAO.findById(auction.getId()) == null) {
            throw new AuctionNotFoundException("Không tìm thấy phiên đấu giá #" + auction.getId());
        }
        auctionDAO.update(auction);

        AuctionServer.broadcast(Message.success(Message.ACTION_AUCTION_UPDATE, auction));
        return Message.success(Message.ACTION_UPDATE_AUCTION, auction);
    }

    /**
     * Xóa phiên đấu giá theo ID (DELETE_AUCTION) và phát thông báo Observer.
     */
    private Message handleDeleteAuction(Message request) {
        Integer auctionId = request.parsePayload(Integer.class);
        if (auctionId == null) {
            JsonObject obj = parseJsonObject(request.getPayload());
            if (obj.has("auctionId")) {
                auctionId = obj.get("auctionId").getAsInt();
            } else if (obj.has("id")) {
                auctionId = obj.get("id").getAsInt();
            }
        }
        if (auctionId == null) {
            throw new DataValidationException("Thiếu ID phiên đấu giá cần xóa.");
        }

        IAuctionDAO auctionDAO = new AuctionDAO();
        if (auctionDAO.findById(auctionId) == null) {
            throw new AuctionNotFoundException("Không tìm thấy phiên đấu giá #" + auctionId);
        }
        auctionDAO.delete(auctionId);

        AuctionServer.broadcast(Message.success(Message.ACTION_DELETE_AUCTION, auctionId));
        return Message.success(Message.ACTION_DELETE_AUCTION, auctionId);
    }

    /**
     * Xử lý yêu cầu đặt giá (PLACE_BID):
     * Bóc tách JSON request -> gọi AuctionService xử lý đa luồng -> Broadcast kết quả cho các Client.
     */
    private Message handlePlaceBid(Message request) {
        JsonObject payload = parseJsonObject(request.getPayload());
        int auctionId = payload.has("auctionId") ? payload.get("auctionId").getAsInt() : -1;
        int bidderId = payload.has("bidderId") ? payload.get("bidderId").getAsInt() : -1;

        // Hỗ trợ chuẩn hóa hợp đồng JSON: đọc trường amount chính thức, tự động fallback bidAmount
        double amount = 0.0;
        if (payload.has("amount") && !payload.get("amount").isJsonNull()) {
            amount = payload.get("amount").getAsDouble();
        } else if (payload.has("bidAmount") && !payload.get("bidAmount").isJsonNull()) {
            amount = payload.get("bidAmount").getAsDouble();
        }

        if (auctionId <= 0 || bidderId <= 0 || amount <= 0) {
            return Message.error(Message.ACTION_PLACE_BID, "Thông tin đặt giá (auctionId, bidderId, amount) không hợp lệ.");
        }

        try {
            // 1. Gọi tầng Service xử lý (đã có ReentrantLock chống Race Condition & Anti-sniping)
            Auction updatedAuction = auctionService.placeBid(auctionId, bidderId, amount);

            // 2. Phát thông báo realtime (Observer Pattern) cho mọi Client đang kết nối
            AuctionServer.broadcast(Message.success(Message.ACTION_BID_UPDATE, updatedAuction));

            // 3. Phản hồi thành công cho Client vừa đặt giá
            return Message.success(Message.ACTION_PLACE_BID, updatedAuction);
        } catch (AuctionException e) {
            // Bắt các ngoại lệ nghiệp vụ (InvalidBidException, AuctionClosedException, ...)
            return Message.error(Message.ACTION_PLACE_BID, e.getMessage());
        } catch (Exception e) {
            return Message.error(Message.ACTION_PLACE_BID, "Lỗi hệ thống: " + e.getMessage());
        }
    }

    /**
     * Lấy lịch sử giao dịch đặt giá (GET_HISTORY).
     */
    private Message handleGetHistory(Message request) {
        IBidTransactionDAO bidDAO = new BidTransactionDAO();
        String rawPayload = request.getPayload();
        if (rawPayload != null && !rawPayload.isBlank()) {
            try {
                Integer auctionId = request.parsePayload(Integer.class);
                if (auctionId != null && auctionId > 0) {
                    return Message.success(Message.ACTION_GET_HISTORY, bidDAO.findByAuctionId(auctionId));
                }
            } catch (Exception ignored) {
            }
        }
        return Message.success(Message.ACTION_GET_HISTORY, bidDAO.findAll());
    }

    /**
     * Xử lý tin nhắn chat chung (CHAT) và phát lại cho mọi Client qua Observer broadcast.
     */
    private Message handleChat(Message request) {
        String chatText = request.getPayload();
        if (chatText == null || chatText.isBlank()) {
            return Message.error(Message.ACTION_CHAT, "Nội dung tin nhắn trống.");
        }
        AuctionServer.chatMessages.add(chatText);
        Message chatBroadcast = Message.success(Message.ACTION_CHAT, chatText);
        AuctionServer.broadcast(chatBroadcast);
        return chatBroadcast;
    }

    /**
     * Băm chuỗi mật khẩu theo chuẩn thuật toán SHA-256.
     * Khớp chính xác với `- hashSHA256(base: String): String` trong sơ đồ lớp UML.
     *
     * @param base Chuỗi mật khẩu gốc
     * @return Chuỗi hex 64 ký tự đã băm SHA-256
     */
    private String hashSHA256(String base) {
        if (base == null) {
            return "";
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(base.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder(2 * hash.length);
            for (byte b : hash) {
                hexString.append(String.format("%02x", b));
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Không tìm thấy thuật toán SHA-256", e);
        }
    }

    private JsonObject parseJsonObject(String json) {
        if (json == null || json.isBlank()) {
            return new JsonObject();
        }
        try {
            return JsonParser.parseString(json).getAsJsonObject();
        } catch (Exception e) {
            return new JsonObject();
        }
    }

    private String getStringField(JsonObject obj, String key) {
        if (obj == null || !obj.has(key) || obj.get(key).isJsonNull()) {
            return "";
        }
        return obj.get(key).getAsString().trim();
    }

    private void closeResources() {
        try {
            if (out != null) {
                out.close();
            }
            if (in != null) {
                in.close();
            }
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException ignored) {
        }
    }
}
