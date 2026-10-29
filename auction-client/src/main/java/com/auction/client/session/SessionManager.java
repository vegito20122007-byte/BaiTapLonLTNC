package com.auction.client.session;

import com.auction.model.User;

/**
 * Quản lý phiên đăng nhập phía Client (Singleton Pattern) theo sơ đồ UML.
 * Lưu giữ thông tin đối tượng User hiện tại đang đăng nhập.
 */
public class SessionManager {

    private static volatile SessionManager instance;
    private User currentUser;

    // Private constructor ngăn việc khởi tạo trực tiếp từ bên ngoài (Singleton)
    private SessionManager() {
    }

    /**
     * Lấy thể hiện duy nhất của SessionManager (Thread-safe Singleton với Double-Checked Locking).
     *
     * @return thể hiện duy nhất của SessionManager
     */
    public static SessionManager getInstance() {
        if (instance == null) {
            synchronized (SessionManager.class) {
                if (instance == null) {
                    instance = new SessionManager();
                }
            }
        }
        return instance;
    }

    /**
     * Lấy thông tin người dùng hiện tại.
     *
     * @return đối tượng User hiện tại (null nếu chưa đăng nhập)
     */
    public User getCurrentUser() {
        return currentUser;
    }

    /**
     * Lưu thông tin người dùng sau khi đăng nhập thành công.
     *
     * @param currentUser đối tượng User đăng nhập
     */
    public void setCurrentUser(User currentUser) {
        this.currentUser = currentUser;
    }

    /**
     * Xóa sạch thông tin phiên đăng nhập (dùng khi Đăng xuất / Logout).
     */
    public void clear() {
        this.currentUser = null;
    }

    /**
     * Kiểm tra xem người dùng đã đăng nhập hay chưa.
     *
     * @return true nếu đã đăng nhập, ngược lại false
     */
    public boolean isLoggedIn() {
        return this.currentUser != null;
    }
}
