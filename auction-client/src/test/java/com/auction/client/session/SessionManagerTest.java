package com.auction.client.session;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Kiểm thử đơn vị (Unit Test) cho SessionManager.
 */
public class SessionManagerTest {

    private SessionManager sessionManager;

    @BeforeEach
    void setUp() {
        sessionManager = SessionManager.getInstance();
        sessionManager.clear();
    }

    @Test
    void testSingletonInstance() {
        SessionManager anotherInstance = SessionManager.getInstance();
        assertSame(sessionManager, anotherInstance, "SessionManager phải trả về cùng một instance (Singleton)");
    }

    @Test
    void testInitialStateNotLoggedIn() {
        assertNull(sessionManager.getCurrentUser(), "Ban đầu chưa đăng nhập thì currentUser phải là null");
        assertFalse(sessionManager.isLoggedIn(), "Ban đầu isLoggedIn phải là false");
    }

    @Test
    void testSetAndGetCurrentUser() {
        String mockUser = "test_user_payload";
        sessionManager.setCurrentUser(mockUser);

        assertTrue(sessionManager.isLoggedIn(), "Sau khi set user thì isLoggedIn phải là true");
        assertEquals(mockUser, sessionManager.getCurrentUser(), "getCurrentUser phải trả về đúng đối tượng đã lưu");
    }

    @Test
    void testClearSession() {
        sessionManager.setCurrentUser("sample_user");
        assertTrue(sessionManager.isLoggedIn());

        sessionManager.clear();

        assertNull(sessionManager.getCurrentUser(), "Sau khi clear thì currentUser phải là null");
        assertFalse(sessionManager.isLoggedIn(), "Sau khi clear thì isLoggedIn phải là false");
    }
}
