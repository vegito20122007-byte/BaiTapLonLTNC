package com.auction.client.network;

import com.auction.client.util.GsonConfig;
import com.auction.exception.AuctionClosedException;
import com.auction.exception.AuctionNotFoundException;
import com.auction.exception.AuthenticationException;
import com.auction.exception.ConnectionException;
import com.auction.exception.DataValidationException;
import com.auction.exception.InvalidBidException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Kiểm thử NetworkClient với Mock Socket Server (Toàn diện & Ngoại lệ)")
class MockSocketServerTest {

    private ServerSocket mockServerSocket;
    private int mockPort;
    private ExecutorService serverExecutor;
    private volatile boolean running = true;
    private final List<Socket> acceptedSockets = new CopyOnWriteArrayList<>();

    @BeforeEach
    void setUp() throws IOException {
        resetNetworkClientHostIfNull();
        // Mở ServerSocket trên cổng động có sẵn
        mockServerSocket = new ServerSocket(0);
        mockPort = mockServerSocket.getLocalPort();
        running = true;

        serverExecutor = Executors.newCachedThreadPool();
        serverExecutor.submit(() -> {
            try {
                while (running && !mockServerSocket.isClosed()) {
                    Socket clientSocket = mockServerSocket.accept();
                    acceptedSockets.add(clientSocket);
                    serverExecutor.submit(() -> handleClient(clientSocket));
                }
            } catch (IOException ignored) {
            }
        });

        // Cấu hình NetworkClient trỏ tới cổng của Mock Server
        NetworkClient.getInstance().configure("localhost", mockPort);
        NetworkClient.getInstance().setTimeoutMs(3000);
    }

    private void resetNetworkClientHostIfNull() {
        try {
            Field hostField = NetworkClient.class.getDeclaredField("host");
            hostField.setAccessible(true);
            if (hostField.get(NetworkClient.getInstance()) == null) {
                hostField.set(NetworkClient.getInstance(), "localhost");
            }
        } catch (Exception ignored) {
        }
    }

    private void crashCurrentMockServer() throws IOException {
        running = false;
        if (mockServerSocket != null && !mockServerSocket.isClosed()) {
            mockServerSocket.close();
        }
        for (Socket s : acceptedSockets) {
            try {
                if (!s.isClosed()) {
                    s.close();
                }
            } catch (IOException ignored) {
            }
        }
        acceptedSockets.clear();
        if (serverExecutor != null) {
            serverExecutor.shutdownNow();
        }
    }

    private void handleClient(Socket socket) {
        try (
                BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                PrintWriter writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true)
        ) {
            String line;
            int mixedCount = 0;
            while ((line = reader.readLine()) != null) {
                Message req = GsonConfig.getGson().fromJson(line, Message.class);

                // Giả lập Server ngắt kết nối đột ngột
                if ("DISCONNECT_IMMEDIATELY".equals(req.getAction())) {
                    socket.close();
                    return;
                }

                // Giả lập Server bị treo xử lý
                if ("DELAY_ACTION".equals(req.getAction())) {
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException ignored) {
                    }
                }

                // Giả lập Server im lặng hoàn toàn không trả lời
                if ("STALL_FOREVER".equals(req.getAction())) {
                    try {
                        Thread.sleep(2500);
                    } catch (InterruptedException ignored) {
                    }
                    continue;
                }

                // Giả lập Shared Action cho kiểm thử timeout độc lập giữa các luồng
                if ("SHARED_ACTION".equals(req.getAction())) {
                    if ("drop-req-1".equals(req.getPayload())) {
                        continue;
                    }
                    try {
                        Thread.sleep(220);
                    } catch (InterruptedException ignored) {
                    }
                    writer.println(GsonConfig.getGson().toJson(Message.success("SHARED_ACTION", "reply-2")));
                    continue;
                }

                // Giả lập Server đẩy broadcast DELETE_AUCTION trước khi trả GET_AUCTIONS
                if (Message.ACTION_GET_AUCTIONS.equals(req.getAction())) {
                    writer.println(GsonConfig.getGson().toJson(Message.success(Message.ACTION_DELETE_AUCTION, 99)));
                    try {
                        Thread.sleep(40);
                    } catch (InterruptedException ignored) {
                    }
                    writer.println(GsonConfig.getGson().toJson(Message.success(Message.ACTION_GET_AUCTIONS, "[]")));
                    continue;
                }

                // Giả lập Server gửi JSON hỏng + dòng trắng xen trước phản hồi hợp lệ
                if ("CORRUPTED_BEFORE_PING".equals(req.getAction())) {
                    writer.println("{corrupted_json_without_closing");
                    writer.println("   ");
                    writer.println(GsonConfig.getGson().toJson(Message.success("CORRUPTED_BEFORE_PING", "alive")));
                    continue;
                }

                // Giả lập Server gửi BID_UPDATE trước khi phản hồi PING
                if ("BID_BEFORE_PING".equals(req.getAction())) {
                    writer.println(GsonConfig.getGson().toJson(Message.success(Message.ACTION_BID_UPDATE, "update-1")));
                    writer.println(GsonConfig.getGson().toJson(Message.success("BID_BEFORE_PING", "ok")));
                    continue;
                }

                // Giả lập nhiều action song song xen kẽ BID_UPDATE
                if (req.getAction() != null && req.getAction().startsWith("MIXED_ACTION_")) {
                    mixedCount++;
                    if (mixedCount % 2 == 0) {
                        writer.println(GsonConfig.getGson().toJson(Message.success(Message.ACTION_BID_UPDATE, "live-" + mixedCount)));
                    }
                    writer.println(GsonConfig.getGson().toJson(Message.success(req.getAction(), req.getPayload())));
                    continue;
                }

                Message resp;
                if (Message.ACTION_LOGIN.equals(req.getAction())) {
                    resp = Message.success(Message.ACTION_LOGIN, "{\"id\":10,\"username\":\"test_user\",\"role\":\"SELLER\"}");
                } else if ("UNICODE_TEST".equals(req.getAction())) {
                    resp = Message.success("UNICODE_TEST", req.getPayload());
                } else if ("PING".equals(req.getAction())) {
                    resp = Message.success("PONG", "ok");
                } else if ("FAIL_ACTION".equals(req.getAction())) {
                    resp = Message.error("FAIL_ACTION", "Tài khoản bị khóa hoặc không hợp lệ!");
                } else {
                    resp = Message.error(req.getAction(), "Hành động chưa được hỗ trợ");
                }

                writer.println(GsonConfig.getGson().toJson(resp));
            }
        } catch (IOException ignored) {
        } finally {
            try {
                socket.close();
            } catch (IOException ignored) {
            }
        }
    }

    @AfterEach
    void tearDown() {
        resetNetworkClientHostIfNull();
        NetworkClient.getInstance().disconnect();

        try {
            crashCurrentMockServer();
            if (serverExecutor != null) {
                serverExecutor.awaitTermination(1, TimeUnit.SECONDS);
            }
        } catch (IOException | InterruptedException ignored) {
        }

        // Khôi phục lại cấu hình mặc định (port 8888)
        NetworkClient.getInstance().configure("localhost", 8888);
        NetworkClient.getInstance().setTimeoutMs(5000);
    }

    @Test
    @DisplayName("NetworkClient đảm bảo chuẩn thiết kế Singleton Pattern")
    void testSingletonInstance() {
        NetworkClient client1 = NetworkClient.getInstance();
        NetworkClient client2 = NetworkClient.getInstance();

        assertNotNull(client1);
        assertSame(client1, client2, "Phải luôn trả về cùng 1 thể hiện NetworkClient");
    }

    @Test
    @DisplayName("Gửi Message LOGIN thành công và nhận phản hồi SUCCESS")
    void testSendRequestSuccess() {
        NetworkClient client = NetworkClient.getInstance();
        Message loginRequest = Message.request(Message.ACTION_LOGIN, "{\"username\":\"admin\",\"password\":\"123\"}");
        Message response = client.sendRequest(loginRequest);

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals(Message.ACTION_LOGIN, response.getAction());
        assertNotNull(response.getPayload());
        assertTrue(response.getPayload().contains("test_user"));
    }

    @Test
    @DisplayName("Nhận phản hồi lỗi ERROR từ máy chủ khi nghiệp vụ thất bại")
    void testSendRequestErrorResponse() {
        NetworkClient client = NetworkClient.getInstance();

        Message request = Message.request("FAIL_ACTION", "dummy");
        Message response = client.sendRequest(request);

        assertNotNull(response);
        assertFalse(response.isSuccess());
        assertEquals("ERROR", response.getStatus());
        assertEquals("Tài khoản bị khóa hoặc không hợp lệ!", response.getErrorMessage());
    }

    @Test
    @DisplayName("Không bị sập ứng dụng (Crash) khi gửi tới cổng Server không khả dụng")
    void testUnreachableServerGracefulError() {
        // Cấu hình trỏ tới 1 cổng ngẫu nhiên chưa ai mở
        NetworkClient client = NetworkClient.getInstance();
        client.configure("localhost", 59123);
        client.setTimeoutMs(1000);

        Message request = Message.request(Message.ACTION_LOGIN, "test");
        Message response = client.sendRequest(request);

        assertNotNull(response, "Phải luôn trả về Message lỗi thay vì ném ngoại lệ làm chết app");
        assertFalse(response.isSuccess());
        assertEquals("ERROR", response.getStatus());
        assertNotNull(response.getErrorMessage());
        assertTrue(response.getErrorMessage().contains("Lỗi"));
    }

    @Test
    @DisplayName("Xử lý an toàn khi truyền Message null vào sendRequest")
    void testNullRequestHandling() {
        NetworkClient client = NetworkClient.getInstance();
        Message response = client.sendRequest(null);

        assertNotNull(response);
        assertEquals("ERROR", response.getStatus());
        assertEquals("Request không được để null!", response.getErrorMessage());
    }

    @Test
    @DisplayName("Ngoại lệ: Máy chủ đột ngột đóng kết nối (Broken pipe / EOF)")
    void testSuddenServerDisconnect() {
        NetworkClient client = NetworkClient.getInstance();

        Message request = Message.request("DISCONNECT_IMMEDIATELY", "test_payload");
        Message response = client.sendRequest(request);

        assertNotNull(response);
        assertFalse(response.isSuccess());
        assertEquals("ERROR", response.getStatus());
        assertTrue(response.getErrorMessage().contains("đóng kết nối"),
                "Thông báo lỗi phải thể hiện rõ việc máy chủ đóng kết nối đột ngột");
        assertFalse(client.isConnected(), "Client phải tự động dọn dẹp và đóng socket");
    }

    @Test
    @DisplayName("Biên: Đảm bảo toàn vẹn mã hóa UTF-8 với tiếng Việt có dấu và ký tự đặc biệt")
    void testUnicodeAndSpecialCharacters() {
        NetworkClient client = NetworkClient.getInstance();

        String vietnamesePayload = "Phiên đấu giá Đồ Cổ: Bình gốm thời Lý - Trần (100.000.000 VNĐ) 💎🏆";
        Message request = Message.request("UNICODE_TEST", vietnamesePayload);
        Message response = client.sendRequest(request);

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals(vietnamesePayload, response.getPayload(), "Tiếng Việt có dấu và emoji phải bảo toàn 100%");
    }

    @Test
    @DisplayName("Gửi nhiều yêu cầu tuần tự (Sequential requests) trên cùng 1 kết nối Socket")
    void testSequentialMultipleRequests() {
        NetworkClient client = NetworkClient.getInstance();

        // Lần 1: PING
        Message pingResp = client.sendRequest(Message.request("PING", null));
        assertNotNull(pingResp);
        assertTrue(pingResp.isSuccess());
        assertEquals("PONG", pingResp.getAction());

        // Lần 2: LOGIN
        Message loginResp = client.sendRequest(Message.request(Message.ACTION_LOGIN, "user"));
        assertNotNull(loginResp);
        assertTrue(loginResp.isSuccess());
        assertEquals(Message.ACTION_LOGIN, loginResp.getAction());

        // Lần 3: PING lần nữa
        Message pingResp2 = client.sendRequest(Message.request("PING", null));
        assertNotNull(pingResp2);
        assertTrue(pingResp2.isSuccess());
    }

    @Test
    @DisplayName("Ngoại lệ: Xử lý Socket Timeout khi Server bị đơ không phản hồi trong thời gian chờ")
    void testServerTimeout() {
        NetworkClient client = NetworkClient.getInstance();
        client.setTimeoutMs(400); // Đặt timeout rất ngắn (400ms), trong khi mock server delay 2000ms

        Message request = Message.request("DELAY_ACTION", "dummy");
        Message response = client.sendRequest(request);

        assertNotNull(response);
        assertFalse(response.isSuccess());
        assertEquals("ERROR", response.getStatus());
        assertTrue(response.getErrorMessage().contains("Lỗi mạng") || response.getErrorMessage().contains("timed out"),
                "Phải phát hiện lỗi Timeout và không treo luồng gọi");
    }

    @Test
    @DisplayName("Gửi yêu cầu bất đồng bộ (sendRequestAsync) trả về CompletableFuture thành công")
    void testSendRequestAsync() throws Exception {
        NetworkClient client = NetworkClient.getInstance();
        Message request = Message.request("PING", null);

        CompletableFuture<Message> future = client.sendRequestAsync(request);
        assertNotNull(future);

        Message response = future.get(3, TimeUnit.SECONDS);
        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("PONG", response.getAction());
    }

    @Test
    @DisplayName("connectOrThrow và sendRequestOrThrow ném ConnectionException khi mất kết nối Server")
    void testConnectionExceptionOnUnreachableServer() {
        NetworkClient client = NetworkClient.getInstance();
        client.configure("localhost", 59124);
        client.setTimeoutMs(500);

        assertThrows(ConnectionException.class, client::connectOrThrow);
        assertThrows(ConnectionException.class,
                () -> client.sendRequestOrThrow(Message.request(Message.ACTION_LOGIN, "dummy")));
    }

    @Test
    @DisplayName("sendRequestOrThrow và mapErrorToException phân loại đúng Custom Exceptions của hệ thống")
    void testSendRequestOrThrowAndExceptionMapping() {
        NetworkClient client = NetworkClient.getInstance();

        assertThrows(DataValidationException.class, () -> client.sendRequestOrThrow(null));

        assertInstanceOf(AuthenticationException.class,
                NetworkClient.mapErrorToException(Message.error(Message.ACTION_LOGIN, "Mật khẩu không chính xác.")));
        assertInstanceOf(AuctionClosedException.class,
                NetworkClient.mapErrorToException(Message.error(Message.ACTION_PLACE_BID, "Phiên đấu giá hiện không mở để nhận giá (FINISHED)")));
        assertInstanceOf(AuctionNotFoundException.class,
                NetworkClient.mapErrorToException(Message.error(Message.ACTION_PLACE_BID, "Không tìm thấy phiên đấu giá #99")));
        assertInstanceOf(InvalidBidException.class,
                NetworkClient.mapErrorToException(Message.error(Message.ACTION_PLACE_BID, "Giá đặt phải cao hơn mức giá hiện tại")));
        assertInstanceOf(ConnectionException.class,
                NetworkClient.mapErrorToException(Message.error(Message.ACTION_GET_AUCTIONS, "Lỗi mạng hoặc không thể kết nối tới Server")));
    }

    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    @DisplayName("sendRequestAsync phải tự timeout nếu Server nhận kết nối rồi im lặng")
    void testSendRequestAsyncMustTimeoutWhenServerStalls() throws Exception {
        NetworkClient client = NetworkClient.getInstance();
        client.setTimeoutMs(250);

        CompletableFuture<Message> future = client.sendRequestAsync(Message.request("STALL_FOREVER", "ping"));
        Message res = future.get(700, TimeUnit.MILLISECONDS);
        assertNotNull(res);
        assertFalse(res.isSuccess());
        assertTrue(res.getErrorMessage().contains("timed out") || res.getErrorMessage().contains("Lỗi mạng"));
    }

    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    @DisplayName("Broadcast DELETE_AUCTION xen ngang không được cướp nhầm Future của GET_AUCTIONS")
    void testDeleteAuctionBroadcastMustNotHijackPendingGetAuctions() {
        Message response = NetworkClient.getInstance().sendRequest(Message.request(Message.ACTION_GET_AUCTIONS, null));
        assertNotNull(response);
        assertEquals(Message.ACTION_GET_AUCTIONS, response.getAction());
        assertTrue(response.isSuccess());
    }

    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    @DisplayName("Một luồng timeout cùng action không được xóa mất PendingRequest của luồng khác đang chạy hợp lệ")
    void testTimeoutOfOneCallerMustNotDropConcurrentSameActionWaiter() throws Exception {
        NetworkClient client = NetworkClient.getInstance();
        client.setTimeoutMs(150);

        CompletableFuture<Message> caller1 = CompletableFuture.supplyAsync(
                () -> client.sendRequest(Message.request("SHARED_ACTION", "drop-req-1"))
        );
        Thread.sleep(30);
        client.setTimeoutMs(1500);
        CompletableFuture<Message> caller2Future = client.sendRequestAsync(Message.request("SHARED_ACTION", "req-2"));

        Message res1 = caller1.get(1, TimeUnit.SECONDS);
        assertFalse(res1.isSuccess());

        Message res2 = caller2Future.get(1500, TimeUnit.MILLISECONDS);
        assertTrue(res2.isSuccess());
        assertEquals("reply-2", res2.getPayload());
    }

    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    @DisplayName("Cấu hình configure(null, port) không được làm sập NullPointerException ở lần configure sau")
    void testConfigureNullHostShouldNotCrashSubsequentConfigure() {
        NetworkClient client = NetworkClient.getInstance();
        assertDoesNotThrow(() -> client.configure(null, 8888));
        assertDoesNotThrow(() -> client.configure("localhost", mockPort));
    }

    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    @DisplayName("connectOrThrow với port âm phải ném ConnectionException (hoặc DataValidationException)")
    void testConnectOrThrowInvalidPortShouldWrapInAuctionException() {
        NetworkClient client = NetworkClient.getInstance();
        client.configure("localhost", -1);
        assertThrows(ConnectionException.class, client::connectOrThrow);
    }

    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    @DisplayName("Server gửi dòng JSON hỏng xen giữa gói tin hợp lệ không được làm chết ReaderLoop")
    void testMalformedJsonFromServerDoesNotKillReaderLoop() {
        Message res = NetworkClient.getInstance().sendRequest(Message.request("CORRUPTED_BEFORE_PING", null));
        assertTrue(res.isSuccess());
        assertEquals("alive", res.getPayload());
    }

    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    @DisplayName("Một Observer Listener ném Exception không được làm mất sự kiện của các Listener khác")
    void testObserverListenerExceptionIsolation() {
        AtomicBoolean secondListenerCalled = new AtomicBoolean(false);
        Consumer<Message> faultyListener = msg -> {
            throw new RuntimeException("Simulated UI listener crash");
        };
        Consumer<Message> healthyListener = msg -> {
            if (Message.ACTION_BID_UPDATE.equals(msg.getAction())) {
                secondListenerCalled.set(true);
            }
        };

        NetworkClient client = NetworkClient.getInstance();
        client.addListener(faultyListener);
        client.addListener(healthyListener);
        try {
            Message res = client.sendRequest(Message.request("BID_BEFORE_PING", null));
            assertTrue(res.isSuccess());
            assertTrue(secondListenerCalled.get());
        } finally {
            client.removeListener(faultyListener);
            client.removeListener(healthyListener);
        }
    }

    @Test
    @Timeout(value = 8, unit = TimeUnit.SECONDS)
    @DisplayName("Khả năng tự kết nối lại (Reconnect) sau khi Server sập rồi khởi động lại")
    void testServerRestartReconnectRecovery() throws Exception {
        int fixedPort = this.mockPort;
        NetworkClient client = NetworkClient.getInstance();
        client.setTimeoutMs(800);

        Message r1 = client.sendRequest(Message.request("PING", null));
        assertTrue(r1.isSuccess());

        crashCurrentMockServer();
        Thread.sleep(100);

        Message r2 = client.sendRequest(Message.request("PING", null));
        assertFalse(r2.isSuccess());

        mockServerSocket = new ServerSocket(fixedPort);
        running = true;
        serverExecutor = Executors.newSingleThreadExecutor();
        serverExecutor.submit(() -> {
            try {
                Socket s = mockServerSocket.accept();
                acceptedSockets.add(s);
                handleClient(s);
            } catch (IOException ignored) {
            }
        });

        Message r3 = client.sendRequest(Message.request("PING", null));
        assertTrue(r3.isSuccess());
        assertEquals("ok", r3.getPayload());
    }

    @Test
    @Timeout(value = 8, unit = TimeUnit.SECONDS)
    @DisplayName("Bắn đồng thời 8 luồng sendRequestAsync khác action xen kẽ Broadcast BID_UPDATE")
    void testConcurrentMixedAsyncRequestsWithInterleavedBroadcasts() throws Exception {
        int threads = 8;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch startGate = new CountDownLatch(1);
        List<Future<Message>> futures = new ArrayList<>();

        for (int i = 0; i < threads; i++) {
            final String act = "MIXED_ACTION_" + i;
            final String data = "payload_" + i;
            futures.add(pool.submit(() -> {
                startGate.await();
                return NetworkClient.getInstance().sendRequestAsync(Message.request(act, data)).get(3, TimeUnit.SECONDS);
            }));
        }

        startGate.countDown();
        for (int i = 0; i < threads; i++) {
            Message m = futures.get(i).get(3, TimeUnit.SECONDS);
            assertTrue(m.isSuccess());
            assertEquals("MIXED_ACTION_" + i, m.getAction());
            assertEquals("payload_" + i, m.getPayload());
        }
        pool.shutdownNow();
    }

    @Test
    @DisplayName("mapErrorToException phân loại chính xác 'Phiên đấu giá đã đóng' và lỗi validation ở Register")
    void testMapErrorToExceptionMisclassification() {
        assertInstanceOf(AuctionClosedException.class,
                NetworkClient.mapErrorToException(Message.error(Message.ACTION_PLACE_BID, "Phiên đấu giá đã đóng!")));
        assertInstanceOf(DataValidationException.class,
                NetworkClient.mapErrorToException(Message.error(Message.ACTION_REGISTER, "Tên đăng nhập không hợp lệ!")));
    }
}
