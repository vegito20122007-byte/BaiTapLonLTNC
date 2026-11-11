package com.auction.client.network;

import com.auction.model.Admin;
import com.auction.model.Auction;
import com.auction.model.Bidder;
import com.auction.model.Seller;
import com.auction.model.User;
import com.google.gson.reflect.TypeToken;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.ServerSocket;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Kiểm thử tích hợp End-to-End với MockAuctionServerRunner")
class MockAuctionServerRunnerTest {

    private static MockAuctionServerRunner server;
    private static int testPort;

    @BeforeAll
    static void startServer() throws IOException {
        // Tìm 1 cổng ngẫu nhiên có sẵn
        try (ServerSocket tempSocket = new ServerSocket(0)) {
            testPort = tempSocket.getLocalPort();
        }

        server = new MockAuctionServerRunner(testPort);
        server.start();

        // Cấu hình NetworkClient trỏ vào cổng của Server vừa mở
        NetworkClient.getInstance().configure("localhost", testPort);
        NetworkClient.getInstance().setTimeoutMs(3000);
    }

    @AfterAll
    static void stopServer() {
        if (server != null) {
            server.stop();
        }
        NetworkClient.getInstance().disconnect();
        // Trả lại cấu hình mặc định
        NetworkClient.getInstance().configure("localhost", 8888);
    }

    @Test
    @DisplayName("Đăng nhập Admin thành công qua Socket thật và giải mã đúng đối tượng Admin")
    void testLoginAdminSuccess() {
        Map<String, String> creds = new HashMap<>();
        creds.put("username", "admin");
        creds.put("password", "12345678910");

        Message response = NetworkClient.getInstance().sendRequest(Message.request(Message.ACTION_LOGIN, creds));
        assertNotNull(response);
        assertTrue(response.isSuccess(), "Phải phản hồi SUCCESS");

        User user = response.parsePayload(User.class);
        assertNotNull(user);
        assertInstanceOf(Admin.class, user);
        assertEquals("admin", user.getUsername());
        assertEquals("ADMIN", user.getRole());
    }

    @Test
    @DisplayName("Đăng nhập Seller thành công qua Socket thật và giải mã đúng đối tượng Seller")
    void testLoginSellerSuccess() {
        Map<String, String> creds = new HashMap<>();
        creds.put("username", "seller");
        creds.put("password", "12345678910");

        Message response = NetworkClient.getInstance().sendRequest(Message.request(Message.ACTION_LOGIN, creds));
        assertNotNull(response);
        assertTrue(response.isSuccess());

        User user = response.parsePayload(User.class);
        assertNotNull(user);
        assertInstanceOf(Seller.class, user);
        assertEquals("seller", user.getUsername());
        assertEquals(50_000_000.0, user.getBalance());
    }

    @Test
    @DisplayName("Đăng nhập Bidder thành công qua Socket thật và giải mã đúng đối tượng Bidder")
    void testLoginBidderSuccess() {
        Map<String, String> creds = new HashMap<>();
        creds.put("username", "bidder");
        creds.put("password", "12345678910");

        Message response = NetworkClient.getInstance().sendRequest(Message.request(Message.ACTION_LOGIN, creds));
        assertNotNull(response);
        assertTrue(response.isSuccess());

        User user = response.parsePayload(User.class);
        assertNotNull(user);
        assertInstanceOf(Bidder.class, user);
        assertEquals("bidder", user.getUsername());
        assertEquals(100_000_000.0, user.getBalance());
    }

    @Test
    @DisplayName("Nhập sai mật khẩu: Server phải trả về ERROR và không cho đăng nhập")
    void testLoginWrongPasswordFails() {
        Map<String, String> creds = new HashMap<>();
        creds.put("username", "admin");
        creds.put("password", "mat_khau_sai_123");

        Message response = NetworkClient.getInstance().sendRequest(Message.request(Message.ACTION_LOGIN, creds));
        assertNotNull(response);
        assertFalse(response.isSuccess());
        assertEquals(Message.STATUS_ERROR, response.getStatus());
        assertTrue(response.getErrorMessage().contains("Mật khẩu không chính xác"));
    }

    @Test
    @DisplayName("Tài khoản không tồn tại: Server phải trả về ERROR chi tiết")
    void testLoginUnknownUserFails() {
        Map<String, String> creds = new HashMap<>();
        creds.put("username", "unknown_user_999");
        creds.put("password", "12345678910");

        Message response = NetworkClient.getInstance().sendRequest(Message.request(Message.ACTION_LOGIN, creds));
        assertNotNull(response);
        assertFalse(response.isSuccess());
        assertEquals(Message.STATUS_ERROR, response.getStatus());
        assertTrue(response.getErrorMessage().contains("không tồn tại"));
    }

    @Test
    @DisplayName("Đăng ký tài khoản mới, sau đó đăng nhập bằng tài khoản vừa tạo thành công 100%")
    void testRegisterAndLoginFlow() {
        String newUsername = "test_e2e_user";
        Map<String, String> regData = new HashMap<>();
        regData.put("username", newUsername);
        regData.put("password", "12345678910");
        regData.put("role", "BIDDER");

        // 1. Gửi request REGISTER
        Message regResponse = NetworkClient.getInstance().sendRequest(Message.request(Message.ACTION_REGISTER, regData));
        assertNotNull(regResponse);
        assertTrue(regResponse.isSuccess());

        // 2. Thử đăng ký lại chính username đó -> phải bị báo lỗi trùng lặp
        Message duplicateResponse = NetworkClient.getInstance().sendRequest(Message.request(Message.ACTION_REGISTER, regData));
        assertNotNull(duplicateResponse);
        assertFalse(duplicateResponse.isSuccess());
        assertTrue(duplicateResponse.getErrorMessage().contains("đã được sử dụng"));

        // 3. Đăng nhập bằng tài khoản mới vừa tạo -> phải thành công
        Map<String, String> loginCreds = new HashMap<>();
        loginCreds.put("username", newUsername);
        loginCreds.put("password", "12345678910");

        Message loginResponse = NetworkClient.getInstance().sendRequest(Message.request(Message.ACTION_LOGIN, loginCreds));
        assertNotNull(loginResponse);
        assertTrue(loginResponse.isSuccess());

        User loggedUser = loginResponse.parsePayload(User.class);
        assertNotNull(loggedUser);
        assertEquals(newUsername, loggedUser.getUsername());
        assertEquals(10_000_000.0, loggedUser.getBalance());
    }

    @Test
    @DisplayName("Lấy danh sách phiên đấu giá (GET_AUCTIONS) và parse qua TypeToken<List<Auction>>")
    void testGetAuctionsList() {
        Message response = NetworkClient.getInstance().sendRequest(Message.request(Message.ACTION_GET_AUCTIONS, null));
        assertNotNull(response);
        assertTrue(response.isSuccess());

        Type listType = new TypeToken<List<Auction>>() {}.getType();
        List<Auction> auctions = response.parsePayload(listType);

        assertNotNull(auctions);
        assertFalse(auctions.isEmpty());
        assertTrue(auctions.size() >= 3, "Phải có ít nhất 3 phiên đấu giá mẫu");
        assertNotNull(auctions.get(0).getName());
        assertNotNull(auctions.get(0).getItem());
    }

    @Test
    @DisplayName("Tạo phiên đấu giá mới (CREATE_AUCTION) và xác minh Server cấp ID")
    void testCreateAuctionSuccess() {
        Seller seller = new Seller(2, "seller", "12345678910");
        com.auction.model.Item item = com.auction.model.ItemFactory.createItem(
                "ART", 99, "Tranh Thủy Mặc Hoa Sen", "Tranh sơn dầu hoa sen", seller);
        Auction newAuction = new Auction(null, "Đấu giá Hoa Sen", item, seller, 3_000_000.0, 4_000_000.0,
                java.time.LocalDateTime.now(), java.time.LocalDateTime.now().plusDays(2));

        Message response = NetworkClient.getInstance().sendRequest(Message.request(Message.ACTION_CREATE_AUCTION, newAuction));
        assertNotNull(response);
        assertTrue(response.isSuccess());

        Auction created = response.parsePayload(Auction.class);
        assertNotNull(created);
        assertNotNull(created.getId());
        assertEquals("Đấu giá Hoa Sen", created.getName());
    }

    @Test
    @DisplayName("Đặt giá thành công (PLACE_BID) cập nhật giá cao nhất và Anti-sniping")
    void testPlaceBidSuccess() {
        Map<String, Object> bidPayload = new HashMap<>();
        bidPayload.put("auctionId", 1);
        bidPayload.put("bidderId", 3);
        bidPayload.put("amount", 8_000_000.0);
        bidPayload.put("bidAmount", 8_000_000.0);

        Message response = NetworkClient.getInstance().sendRequest(Message.request(Message.ACTION_PLACE_BID, bidPayload));
        assertNotNull(response);
        assertTrue(response.isSuccess());

        Auction updated = response.parsePayload(Auction.class);
        assertNotNull(updated);
        assertEquals(8_000_000.0, updated.getMaxBid());
    }

    @Test
    @DisplayName("Hợp đồng JSON (PLACE_BID): Hỗ trợ tương thích cả khi chỉ gửi 'amount' hoặc chỉ gửi 'bidAmount'")
    void testPlaceBidJsonContractCompatibility() {
        // 1. Chỉ gửi trường chuẩn 'amount'
        Map<String, Object> payloadAmountOnly = new HashMap<>();
        payloadAmountOnly.put("auctionId", 3);
        payloadAmountOnly.put("bidderId", 3);
        payloadAmountOnly.put("amount", 26_000_000.0);

        Message resp1 = NetworkClient.getInstance().sendRequest(Message.request(Message.ACTION_PLACE_BID, payloadAmountOnly));
        assertNotNull(resp1);
        assertTrue(resp1.isSuccess(), "Phải chấp nhận trường chuẩn 'amount'");
        assertEquals(26_000_000.0, resp1.parsePayload(Auction.class).getMaxBid());

        // 2. Chỉ gửi trường alias 'bidAmount'
        Map<String, Object> payloadBidAmountOnly = new HashMap<>();
        payloadBidAmountOnly.put("auctionId", 3);
        payloadBidAmountOnly.put("bidderId", 3);
        payloadBidAmountOnly.put("bidAmount", 27_000_000.0);

        Message resp2 = NetworkClient.getInstance().sendRequest(Message.request(Message.ACTION_PLACE_BID, payloadBidAmountOnly));
        assertNotNull(resp2);
        assertTrue(resp2.isSuccess(), "Phải chấp nhận alias tương thích ngược 'bidAmount'");
        assertEquals(27_000_000.0, resp2.parsePayload(Auction.class).getMaxBid());
    }

    @Test
    @DisplayName("Đặt giá thấp hơn mức cao nhất hiện tại bị Server từ chối")
    void testPlaceBidLowerFails() {
        Map<String, Object> bidPayload = new HashMap<>();
        bidPayload.put("auctionId", 1);
        bidPayload.put("bidderId", 3);
        bidPayload.put("amount", 1_000_000.0);
        bidPayload.put("bidAmount", 1_000_000.0);

        Message response = NetworkClient.getInstance().sendRequest(Message.request(Message.ACTION_PLACE_BID, bidPayload));
        assertNotNull(response);
        assertFalse(response.isSuccess());
        assertTrue(response.getErrorMessage().contains("lớn hơn giá cao nhất"));
    }

    @Test
    @DisplayName("Observer Pattern: Nhận thông báo phát thanh Realtime khi có lượt đặt giá mới (BID_UPDATE)")
    void testRealtimeObserverBidUpdate() throws Exception {
        java.util.concurrent.CompletableFuture<Message> observerFuture = new java.util.concurrent.CompletableFuture<>();
        java.util.function.Consumer<Message> testListener = msg -> {
            if (msg != null && Message.ACTION_BID_UPDATE.equalsIgnoreCase(msg.getAction())) {
                observerFuture.complete(msg);
            }
        };

        NetworkClient.getInstance().addListener(testListener);

        try {
            Map<String, Object> bidPayload = new HashMap<>();
            bidPayload.put("auctionId", 2);
            bidPayload.put("bidderId", 3);
            bidPayload.put("amount", 22_000_000.0);
            bidPayload.put("bidAmount", 22_000_000.0);

            Message bidResponse = NetworkClient.getInstance().sendRequest(Message.request(Message.ACTION_PLACE_BID, bidPayload));
            assertTrue(bidResponse.isSuccess());

            Message broadcastMsg = observerFuture.get(3, java.util.concurrent.TimeUnit.SECONDS);
            assertNotNull(broadcastMsg);
            assertEquals(Message.ACTION_BID_UPDATE, broadcastMsg.getAction());
        } finally {
            NetworkClient.getInstance().removeListener(testListener);
        }
    }
}
