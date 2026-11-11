package com.auction.client.network;

import com.auction.client.util.GsonConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Kiểm thử NetworkClient với Mock Socket Server (Toàn diện & Ngoại lệ)")
class MockSocketServerTest {

    private ServerSocket mockServerSocket;
    private int mockPort;
    private ExecutorService serverExecutor;
    private volatile boolean running = true;

    @BeforeEach
    void setUp() throws IOException {
        // Mở ServerSocket trên cổng động có sẵn
        mockServerSocket = new ServerSocket(0);
        mockPort = mockServerSocket.getLocalPort();
        running = true;

        serverExecutor = Executors.newSingleThreadExecutor();
        serverExecutor.submit(() -> {
            try {
                while (running && !mockServerSocket.isClosed()) {
                    Socket clientSocket = mockServerSocket.accept();
                    handleClient(clientSocket);
                }
            } catch (IOException ignored) {
            }
        });

        // Cấu hình NetworkClient trỏ tới cổng của Mock Server
        NetworkClient.getInstance().configure("localhost", mockPort);
        NetworkClient.getInstance().setTimeoutMs(3000);
    }

    private void handleClient(Socket socket) {
        try (
                BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                PrintWriter writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true)
        ) {
            String line;
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
        running = false;
        NetworkClient.getInstance().disconnect();

        try {
            if (mockServerSocket != null && !mockServerSocket.isClosed()) {
                mockServerSocket.close();
            }
        } catch (IOException ignored) {
        }

        if (serverExecutor != null) {
            serverExecutor.shutdownNow();
            try {
                serverExecutor.awaitTermination(1, TimeUnit.SECONDS);
            } catch (InterruptedException ignored) {
            }
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
}
