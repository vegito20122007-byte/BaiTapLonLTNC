package com.auction.client.session;

import com.auction.model.Bidder;
import com.auction.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Kiểm thử đơn vị (Unit Test) cho SessionManager với model User thật từ auction-common.
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
        User mockUser = new Bidder(1, "taylermode", "password123");
        sessionManager.setCurrentUser(mockUser);

        assertTrue(sessionManager.isLoggedIn(), "Sau khi set user thì isLoggedIn phải là true");
        assertNotNull(sessionManager.getCurrentUser(), "currentUser không được null");
        assertEquals("taylermode", sessionManager.getCurrentUser().getUsername());
    }

    @Test
    void testClearSession() {
        User mockUser = new Bidder(2, "seller01", "password123");
        sessionManager.setCurrentUser(mockUser);
        assertTrue(sessionManager.isLoggedIn());

        sessionManager.clear();

        assertNull(sessionManager.getCurrentUser(), "Sau khi clear thì currentUser phải là null");
        assertFalse(sessionManager.isLoggedIn(), "Sau khi clear thì isLoggedIn phải là false");
    }
}
