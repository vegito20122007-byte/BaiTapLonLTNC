package com.auction.client.network;

import com.auction.client.util.GsonConfig;
import com.auction.exception.AuctionClosedException;
import com.auction.exception.AuctionException;
import com.auction.exception.AuctionNotFoundException;
import com.auction.exception.AuthenticationException;
import com.auction.exception.ConnectionException;
import com.auction.exception.DataValidationException;
import com.auction.exception.InsufficientBalanceException;
import com.auction.exception.InvalidBidException;
import com.auction.exception.UnauthorizedActionException;
import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

/**
 * Lớp điều khiển kết nối mạng Socket TCP giữa Client và AuctionServer.
 * Áp dụng thiết kế Singleton Pattern (duy nhất 1 kết nối Socket trong toàn ứng dụng)
 * kết hợp mẫu Observer Pattern:
 * - Duy trì một luồng ngầm liên tục đọc dữ liệu đẩy về từ máy chủ (Server Push/Broadcast).
 * - Hỗ trợ gửi/nhận yêu cầu tuần tự và bất đồng bộ (Async Non-blocking UI) không gây treo JavaFX.
 * - Cho phép các Controller đăng ký Listener để tự động cập nhật dữ liệu Realtime
 *   (giá mới, lượt đặt giá mới, phiên đấu giá mới...).
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
    private final Object outLock = new Object();
    private final Gson gson = GsonConfig.getGson();

    private volatile boolean running = false;
    private Thread readerThread;

    // Danh sách lưu các yêu cầu đang đợi phản hồi từ Server
    private static class PendingRequest {
        final String action;
        final CompletableFuture<Message> future;

        PendingRequest(String action, CompletableFuture<Message> future) {
            this.action = action;
            this.future = future;
        }
    }

    private final List<PendingRequest> pendingRequests = new CopyOnWriteArrayList<>();

    // Danh sách đăng ký Observer lắng nghe sự kiện Realtime từ Server
    private final List<Consumer<Message>> listeners = new CopyOnWriteArrayList<>();

    // Bộ lập lịch timeout riêng cho từng yêu cầu bất đồng bộ (Daemon thread)
    private final ScheduledExecutorService timeoutScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "NetworkClient-TimeoutScheduler");
        t.setDaemon(true);
        return t;
    });

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
     * Đăng ký một Listener nhận các thông điệp realtime (Observer Pattern).
     *
     * @param listener hàm callback xử lý Message
     */
    public void addListener(Consumer<Message> listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    /**
     * Hủy đăng ký Listener khi Controller đóng hoặc chuyển màn hình.
     *
     * @param listener hàm callback đã đăng ký
     */
    public void removeListener(Consumer<Message> listener) {
        if (listener != null) {
            listeners.remove(listener);
        }
    }

    /**
     * Cấu hình lại địa chỉ Server (dùng khi kiểm thử hoặc đổi IP/Port).
     *
     * @param host IP hoặc tên miền Server
     * @param port cổng kết nối TCP
     */
    public synchronized void configure(String host, int port) {
        if (host == null || host.isBlank()) {
            return;
        }
        if (!Objects.equals(this.host, host) || this.port != port) {
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
    }

    /**
     * Mở kết nối Socket tới Server nếu chưa kết nối và khởi động luồng đọc nền.
     *
     * @throws IOException khi không thể kết nối tới Server
     */
    public synchronized void connect() throws IOException {
        if (isConnected()) {
            return;
        }
        if (host == null || host.isBlank()) {
            throw new IOException("Địa chỉ máy chủ (host) không hợp lệ.");
        }
        if (port < 1 || port > 65535) {
            throw new IOException("Cổng kết nối không hợp lệ: " + port);
        }

        socket = new Socket();
        socket.connect(new InetSocketAddress(host, port), timeoutMs);

        in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
        running = true;

        readerThread = new Thread(this::runReaderLoop, "NetworkClient-ReaderLoop");
        readerThread.setDaemon(true);
        readerThread.start();

        System.out.println("[NetworkClient] Đã kết nối thành công tới Server: " + host + ":" + port);
    }

    /**
     * Mở kết nối tới Server và ném {@link ConnectionException} nếu không thể kết nối.
     *
     * @throws ConnectionException khi mất kết nối hoặc Server không khả dụng
     */
    public synchronized void connectOrThrow() {
        try {
            connect();
        } catch (IOException | IllegalArgumentException e) {
            throw new ConnectionException(
                    "Không thể kết nối tới máy chủ (" + host + ":" + port + "): " + e.getMessage(), e);
        }
    }

    /**
     * Kiểm tra trạng thái kết nối hiện thời của Socket.
     *
     * @return true nếu socket đang mở và kết nối
     */
    public synchronized boolean isConnected() {
        return socket != null && !socket.isClosed() && socket.isConnected() && running;
    }

    /**
     * Đóng an toàn kết nối Socket và các luồng đọc/ghi.
     */
    public synchronized void disconnect() {
        running = false;
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

            // Hoàn tất các yêu cầu còn chờ phản hồi bằng thông báo ngắt kết nối
            for (PendingRequest pending : pendingRequests) {
                pending.future.complete(createErrorResponse(pending.action, "Máy chủ đã đóng kết nối đột ngột."));
            }
            pendingRequests.clear();

            System.out.println("[NetworkClient] Đã ngắt kết nối Socket.");
        }
    }

    private boolean isBroadcastAction(String action) {
        if (action == null) return false;
        String a = action.trim().toUpperCase();
        return Message.ACTION_BID_UPDATE.equals(a)
                || Message.ACTION_AUCTION_UPDATE.equals(a)
                || Message.ACTION_DELETE_AUCTION.equals(a)
                || Message.ACTION_CHAT.equals(a);
    }

    /**
     * Luồng đọc nền liên tục (Background Reader Loop) đón nhận dữ liệu từ Server:
     * - Khớp các gói tin phản hồi với yêu cầu đang đợi.
     * - Phát (Broadcast/Dispatch) các gói tin realtime tới toàn bộ Observer đã đăng ký.
     */
    private void runReaderLoop() {
        try {
            String line;
            while (running && in != null && (line = in.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }

                System.out.println("[NetworkClient] Nhận: " + line);
                Message msg;
                try {
                    msg = gson.fromJson(line, Message.class);
                } catch (Exception e) {
                    System.err.println("[NetworkClient] Lỗi giải mã gói tin JSON: " + e.getMessage());
                    continue;
                }

                if (msg == null) {
                    continue;
                }

                // 1. Tìm yêu cầu đang đợi khớp chính xác mã action
                PendingRequest matched = null;
                for (PendingRequest p : pendingRequests) {
                    if (p.action != null && p.action.equalsIgnoreCase(msg.getAction())) {
                        matched = p;
                        break;
                    }
                }

                // 2. Nếu không khớp trực tiếp và không phải gói tin broadcast, khớp theo hàng đợi FIFO (ví dụ: PING/PONG hoặc Error)
                if (matched != null) {
                    pendingRequests.remove(matched);
                    matched.future.complete(msg);
                } else if (!isBroadcastAction(msg.getAction()) && !pendingRequests.isEmpty()) {
                    PendingRequest head = pendingRequests.remove(0);
                    head.future.complete(msg);
                }

                // 3. Luôn phát thông điệp tới các Observer đang theo dõi (Ví dụ: BID_UPDATE, AUCTION_UPDATE...)
                notifyListeners(msg);
            }
        } catch (IOException e) {
            if (running) {
                System.err.println("[NetworkClient] Lỗi đọc luồng Socket: " + e.getMessage());
            }
        } finally {
            if (running) {
                disconnect();
            }
        }
    }

    /**
     * Phát thông điệp nhận được từ Server tới toàn bộ Listener đã đăng ký.
     */
    private void notifyListeners(Message msg) {
        for (Consumer<Message> listener : listeners) {
            try {
                listener.accept(msg);
            } catch (Exception e) {
                System.err.println("[NetworkClient] Lỗi tại Listener Observer: " + e.getMessage());
            }
        }
    }

    /**
     * Gửi yêu cầu bất đồng bộ (Asynchronous) không gây khóa luồng giao diện (Non-blocking UI).
     *
     * @param request gói tin Message gửi đi
     * @return CompletableFuture chứa kết quả Message phản hồi
     */
    public CompletableFuture<Message> sendRequestAsync(Message request) {
        return sendRequestInternal(request).future;
    }

    private PendingRequest sendRequestInternal(Message request) {
        if (request == null) {
            return new PendingRequest("UNKNOWN",
                    CompletableFuture.completedFuture(createErrorResponse("UNKNOWN", "Request không được để null!")));
        }

        CompletableFuture<Message> future = new CompletableFuture<>();
        PendingRequest pending = new PendingRequest(request.getAction(), future);
        ScheduledFuture<?> timeoutTask = null;

        try {
            if (!isConnected()) {
                connect();
            }

            long effectiveTimeout = Math.max(1L, this.timeoutMs);
            pendingRequests.add(pending);

            timeoutTask = timeoutScheduler.schedule(() -> {
                if (!future.isDone()) {
                    pendingRequests.remove(pending);
                    future.complete(createErrorResponse(request.getAction(),
                            "Lỗi mạng hoặc không thể kết nối tới Server (" + host + ":" + port + "): Read timed out"));
                }
            }, effectiveTimeout, TimeUnit.MILLISECONDS);
            final ScheduledFuture<?> scheduledRef = timeoutTask;
            future.whenComplete((res, ex) -> scheduledRef.cancel(false));

            String jsonRequest = gson.toJson(request);
            synchronized (outLock) {
                if (out == null) {
                    throw new IOException("Luồng xuất PrintWriter bị null");
                }
                out.println(jsonRequest);
                out.flush();
            }
            System.out.println("[NetworkClient] Gửi: " + jsonRequest);

        } catch (IOException e) {
            if (timeoutTask != null) {
                timeoutTask.cancel(false);
            }
            pendingRequests.remove(pending);
            System.err.println("[NetworkClient] Lỗi truyền thông Socket: " + e.getMessage());
            disconnect();
            return new PendingRequest(request.getAction(), CompletableFuture.completedFuture(
                    createErrorResponse(request.getAction(), "Lỗi mạng hoặc không thể kết nối tới Server (" + host + ":" + port + "): " + e.getMessage())
            ));
        } catch (Exception e) {
            if (timeoutTask != null) {
                timeoutTask.cancel(false);
            }
            pendingRequests.remove(pending);
            System.err.println("[NetworkClient] Lỗi xử lý yêu cầu: " + e.getMessage());
            return new PendingRequest(request.getAction(), CompletableFuture.completedFuture(
                    createErrorResponse(request.getAction(), "Lỗi xử lý yêu cầu: " + e.getMessage())
            ));
        }

        return pending;
    }

    /**
     * Gửi yêu cầu (Message) sang Server và đợi nhận gói tin phản hồi (Message) tương ứng.
     *
     * @param request gói tin Message gửi đi
     * @return gói tin Message phản hồi từ Server, hoặc Message thông báo lỗi nếu có sự cố mạng
     */
    public Message sendRequest(Message request) {
        if (request == null) {
            return createErrorResponse("UNKNOWN", "Request không được để null!");
        }

        PendingRequest pending = sendRequestInternal(request);
        try {
            return pending.future.get(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            System.err.println("[NetworkClient] Lỗi truyền thông Socket: Read timed out");
            pendingRequests.remove(pending);
            return createErrorResponse(request.getAction(), "Lỗi mạng hoặc không thể kết nối tới Server (" + host + ":" + port + "): Read timed out");
        } catch (ExecutionException e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            return createErrorResponse(request.getAction(), "Lỗi thực thi: " + cause.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            pendingRequests.remove(pending);
            return createErrorResponse(request.getAction(), "Yêu cầu bị gián đoạn: " + e.getMessage());
        }
    }

    /**
     * Gửi yêu cầu tới Server và ném ngoại lệ {@link AuctionException} tương ứng nếu thất bại.
     * Hữu ích cho các luồng cần xử lý lỗi theo kiểu phân cấp Custom Exception.
     *
     * @param request gói tin Message gửi đi
     * @return gói tin phản hồi thành công (status == SUCCESS)
     * @throws AuctionException nếu lỗi validate, mất kết nối hoặc Server trả về lỗi nghiệp vụ
     */
    public Message sendRequestOrThrow(Message request) {
        if (request == null) {
            throw new DataValidationException("Request không được để null!");
        }
        Message response = sendRequest(request);
        if (response == null) {
            throw new ConnectionException("Không nhận được phản hồi từ máy chủ (" + host + ":" + port + ").");
        }
        if (!response.isSuccess()) {
            throw mapErrorToException(response);
        }
        return response;
    }

    /**
     * Ánh xạ một gói tin lỗi {@link Message} nhận từ Server hoặc tầng mạng
     * thành lớp ngoại lệ nghiệp vụ {@link AuctionException} tương ứng.
     *
     * @param response gói tin phản hồi trạng thái ERROR
     * @return đối tượng AuctionException phù hợp
     */
    public static AuctionException mapErrorToException(Message response) {
        if (response == null) {
            return new ConnectionException("Không thể kết nối tới máy chủ (localhost:8888).");
        }
        String err = response.getErrorMessage() != null ? response.getErrorMessage() : "Lỗi không xác định từ máy chủ.";
        String lowerErr = err.toLowerCase();
        String action = response.getAction() != null ? response.getAction().trim().toUpperCase() : "";

        if (lowerErr.contains("lỗi mạng") || lowerErr.contains("không thể kết nối")
                || lowerErr.contains("đóng kết nối") || lowerErr.contains("timed out")
                || lowerErr.contains("gián đoạn")) {
            return new ConnectionException(err);
        }
        if (Message.ACTION_LOGIN.equals(action) || Message.ACTION_REGISTER.equals(action)) {
            if (lowerErr.contains("để trống") || lowerErr.contains("nhập đầy đủ")
                    || lowerErr.contains("không hợp lệ") || lowerErr.contains("không được trống")) {
                return new DataValidationException(err);
            }
            return new AuthenticationException(err);
        }
        if (lowerErr.contains("không tìm thấy phiên")) {
            return new AuctionNotFoundException(err);
        }
        if (lowerErr.contains("không mở để nhận giá") || lowerErr.contains("đã kết thúc")
                || lowerErr.contains("đã đóng") || lowerErr.contains("chưa mở")
                || lowerErr.contains("closed") || lowerErr.contains("finished") || lowerErr.contains("canceled")) {
            return new AuctionClosedException(err);
        }
        if (lowerErr.contains("số dư")) {
            return new InsufficientBalanceException(err);
        }
        if (lowerErr.contains("không có quyền") || lowerErr.contains("vai trò")) {
            return new UnauthorizedActionException(err);
        }
        if (Message.ACTION_PLACE_BID.equals(action)) {
            if (lowerErr.contains("không hợp lệ") && lowerErr.contains("auctionid")) {
                return new DataValidationException(err);
            }
            return new InvalidBidException(err);
        }
        if (lowerErr.contains("không hợp lệ") || lowerErr.contains("không được trống")) {
            return new DataValidationException(err);
        }
        return new AuctionException(err);
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
