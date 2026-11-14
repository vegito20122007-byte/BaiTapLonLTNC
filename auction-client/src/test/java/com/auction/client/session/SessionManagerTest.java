package com.auction.client.session;

import com.auction.model.Admin;
import com.auction.model.Bidder;
import com.auction.model.Seller;
import com.auction.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Kiểm thử đơn vị toàn diện cho SessionManager (Singleton, State transitions, Concurrency).
 */
@DisplayName("Kiểm thử SessionManager (Toàn diện & Đa luồng)")
public class SessionManagerTest {

    private SessionManager sessionManager;

    @BeforeEach
    void setUp() {
        sessionManager = SessionManager.getInstance();
        sessionManager.clear();
    }

    @Test
    @DisplayName("SessionManager phải đảm bảo Singleton Pattern duy nhất trong toàn hệ thống")
    void testSingletonInstance() {
        SessionManager anotherInstance = SessionManager.getInstance();
        assertSame(sessionManager, anotherInstance, "SessionManager phải trả về cùng một instance (Singleton)");
    }

    @Test
    @DisplayName("Trạng thái ban đầu: Chưa đăng nhập, currentUser là null")
    void testInitialStateNotLoggedIn() {
        assertNull(sessionManager.getCurrentUser(), "Ban đầu chưa đăng nhập thì currentUser phải là null");
        assertFalse(sessionManager.isLoggedIn(), "Ban đầu isLoggedIn phải là false");
    }

    @Test
    @DisplayName("Lưu và truy xuất thông tin người dùng đăng nhập thành công")
    void testSetAndGetCurrentUser() {
        User mockUser = new Bidder(1, "taylermode", "password123");
        sessionManager.setCurrentUser(mockUser);

        assertTrue(sessionManager.isLoggedIn(), "Sau khi set user thì isLoggedIn phải là true");
        assertNotNull(sessionManager.getCurrentUser(), "currentUser không được null");
        assertEquals("taylermode", sessionManager.getCurrentUser().getUsername());
    }

    @Test
    @DisplayName("Xóa phiên đăng nhập (Logout): currentUser trở về null và isLoggedIn là false")
    void testClearSession() {
        User mockUser = new Bidder(2, "seller01", "password123");
        sessionManager.setCurrentUser(mockUser);
        assertTrue(sessionManager.isLoggedIn());

        sessionManager.clear();

        assertNull(sessionManager.getCurrentUser(), "Sau khi clear thì currentUser phải là null");
        assertFalse(sessionManager.isLoggedIn(), "Sau khi clear thì isLoggedIn phải là false");
    }

    @Test
    @DisplayName("Biên: Đăng nhập đè (ghi đè user khác mà không cần gọi clear trước)")
    void testOverwriteCurrentUser() {
        User user1 = new Bidder(1, "bidder_01", "pass1");
        sessionManager.setCurrentUser(user1);
        assertEquals("bidder_01", sessionManager.getCurrentUser().getUsername());

        // Ghi đè bằng tài khoản Quản trị viên
        User user2 = new Admin(99, "admin_boss", "rootpass");
        sessionManager.setCurrentUser(user2);

        assertTrue(sessionManager.isLoggedIn());
        assertEquals("admin_boss", sessionManager.getCurrentUser().getUsername());
        assertInstanceOf(Admin.class, sessionManager.getCurrentUser(), "User hiện tại phải là thể hiện của Admin");
    }

    @Test
    @DisplayName("Biên: Gọi setCurrentUser(null) phải đưa isLoggedIn về false")
    void testSetCurrentUserNull() {
        User user = new Seller(5, "seller_hiep", "pass");
        sessionManager.setCurrentUser(user);
        assertTrue(sessionManager.isLoggedIn());

        sessionManager.setCurrentUser(null);
        assertNull(sessionManager.getCurrentUser());
        assertFalse(sessionManager.isLoggedIn(), "isLoggedIn phải trả về false khi currentUser bị gán null");
    }

    @Test
    @DisplayName("Đa luồng (Concurrency): An toàn khi nhiều luồng cùng đọc và ghi Session đồng thời")
    void testConcurrentAccess() throws InterruptedException {
        int threadCount = 10;
        int operationsPerThread = 100;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        AtomicInteger errorCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    startLatch.await(); // Đợi tất cả luồng sẵn sàng xuất phát cùng lúc
                    for (int j = 0; j < operationsPerThread; j++) {
                        if (j % 3 == 0) {
                            sessionManager.setCurrentUser(new Bidder(threadId, "user_" + threadId, "pass"));
                        } else if (j % 3 == 1) {
                            User u = sessionManager.getCurrentUser();
                            boolean loggedIn = sessionManager.isLoggedIn();
                            if (loggedIn && u == null) {
                                errorCount.incrementAndGet(); // Bất biến trạng thái bị vi phạm
                            }
                        } else {
                            sessionManager.clear();
                        }
                    }
                } catch (Exception e) {
                    errorCount.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown(); // Phát lệnh chạy đồng thời
        boolean completed = doneLatch.await(5, TimeUnit.SECONDS);
        executor.shutdown();

        assertTrue(completed, "Toàn bộ các luồng phải hoàn thành trong thời gian cho phép");
        assertEquals(0, errorCount.get(), "Không được xảy ra xung đột trạng thái giữa isLoggedIn và getCurrentUser");
    }
}
