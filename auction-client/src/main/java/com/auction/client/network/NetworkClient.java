package com.auction.client.network;

import com.auction.client.util.GsonConfig;
import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * Lớp điều khiển kết nối mạng Socket TCP giữa Client và AuctionServer.
 * Áp dụng thiết kế Singleton Pattern (duy nhất 1 kết nối Socket trong toàn ứng dụng).
 */
public class NetworkClient {

    private static NetworkClient instance;

    // Cấu hình địa chỉ Server (mặc định localhost:8888)
    private String host = "localhost";
    private int port = 8888;
    private int timeoutMs = 5000;

    // Các tài nguyên Socket I/O
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private final Gson gson = GsonConfig.getGson();

    /**
     * Hàm dựng private đảm bảo tính chất Singleton.
     */
    private NetworkClient() {
    }

    /**
     * Lấy thể hiện duy nhất của NetworkClient.
     *
     * @return thể hiện NetworkClient
     */
    public static synchronized NetworkClient getInstance() {
        if (instance == null) {
            instance = new NetworkClient();
        }
        return instance;
    }

    /**
     * Cấu hình lại địa chỉ Server (dùng khi kiểm thử hoặc đổi IP/Port).
     *
     * @param host IP hoặc tên miền Server
     * @param port cổng kết nối TCP
     */
    public synchronized void configure(String host, int port) {
        if (!this.host.equals(host) || this.port != port) {
            disconnect();
            this.host = host;
            this.port = port;
        }
    }

    /**
     * Thiết lập thời gian chờ tối đa khi nhận dữ liệu từ Socket (Timeout).
     *
     * @param timeoutMs thời gian tính bằng mili-giây
     */
    public synchronized void setTimeoutMs(int timeoutMs) {
        this.timeoutMs = timeoutMs;
        if (socket != null && !socket.isClosed()) {
            try {
                socket.setSoTimeout(timeoutMs);
            } catch (IOException ignored) {
            }
        }
    }

    /**
     * Mở kết nối Socket tới Server nếu chưa kết nối.
     *
     * @throws IOException khi không thể kết nối tới Server
     */
    public synchronized void connect() throws IOException {
        if (isConnected()) {
            return;
        }

        socket = new Socket();
        socket.connect(new InetSocketAddress(host, port), timeoutMs);
        socket.setSoTimeout(timeoutMs);

        in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);

        System.out.println("[NetworkClient] Đã kết nối thành công tới Server: " + host + ":" + port);
    }

    /**
     * Kiểm tra trạng thái kết nối hiện thời của Socket.
     *
     * @return true nếu socket đang mở và kết nối
     */
    public synchronized boolean isConnected() {
        return socket != null && !socket.isClosed() && socket.isConnected();
    }

    /**
     * Đóng an toàn kết nối Socket và các luồng đọc/ghi.
     */
    public synchronized void disconnect() {
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
        } catch (IOException e) {
            System.err.println("[NetworkClient] Lỗi khi đóng socket: " + e.getMessage());
        } finally {
            socket = null;
            in = null;
            out = null;
            System.out.println("[NetworkClient] Đã ngắt kết nối Socket.");
        }
    }

    /**
     * Gửi yêu cầu (Message) sang Server và đợi nhận gói tin phản hồi (Message) tương ứng.
     * Luồng thực thi an toàn (Thread-safe) nhờ cơ chế synchronized.
     *
     * @param request gói tin Message gửi đi
     * @return gói tin Message phản hồi từ Server, hoặc Message thông báo lỗi nếu có sự cố mạng
     */
    public synchronized Message sendRequest(Message request) {
        if (request == null) {
            return createErrorResponse("UNKNOWN", "Request không được để null!");
        }

        try {
            // 1. Tự động kết nối lại nếu chưa kết nối
            if (!isConnected()) {
                connect();
            }

            // 2. Chuyển Message sang chuỗi JSON và gửi qua Socket
            String jsonRequest = gson.toJson(request);
            out.println(jsonRequest);
            System.out.println("[NetworkClient] Gửi: " + jsonRequest);

            // 3. Đợi đọc phản hồi 1 dòng JSON từ Server
            String jsonResponse = in.readLine();
            if (jsonResponse == null) {
                disconnect();
                return createErrorResponse(request.getAction(), "Máy chủ đã đóng kết nối đột ngột.");
            }

            System.out.println("[NetworkClient] Nhận: " + jsonResponse);

            // 4. Giải mã chuỗi JSON thành đối tượng Message
            return gson.fromJson(jsonResponse, Message.class);

        } catch (IOException e) {
            System.err.println("[NetworkClient] Lỗi truyền thông Socket: " + e.getMessage());
            disconnect();
            return createErrorResponse(request.getAction(), "Lỗi mạng hoặc không thể kết nối tới Server (" + host + ":" + port + "): " + e.getMessage());
        } catch (Exception e) {
            System.err.println("[NetworkClient] Lỗi xử lý dữ liệu: " + e.getMessage());
            return createErrorResponse(request.getAction(), "Lỗi định dạng dữ liệu phản hồi: " + e.getMessage());
        }
    }

    /**
     * Tạo đối tượng Message báo lỗi chuẩn khi có sự cố mạng hoặc mất kết nối.
     *
     * @param action mã hành động tương ứng
     * @param error nội dung thông báo lỗi
     * @return đối tượng Message trạng thái ERROR
     */
    public Message createErrorResponse(String action, String error) {
        return Message.error(action, error);
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public Gson getGson() {
        return gson;
    }
}
