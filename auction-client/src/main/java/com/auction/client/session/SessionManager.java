package com.auction.client.session;

/**
 * Quản lý phiên đăng nhập phía Client (Singleton Pattern).
 * Lưu giữ thông tin người dùng hiện tại đang đăng nhập.
 * User đang để tạm dạng Object. Sẽ thay đổi sau
 */
public class SessionManager {

    private static volatile SessionManager instance;
    private Object currentUser;

    // Private constructor ngăn việc khởi tạo trực tiếp từ bên ngoài (Singleton)
    private SessionManager() {
    }

    /**
     * Thread-safe Singleton với Double-Checked Locking
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
     * Lấy thông tin người dùng hiện tại,
     * @return đối tượng người dùng hiện tại (null nếu chưa đăng nhập)
     */
    public Object getCurrentUser() {
        return currentUser;
    }

    /**
     * Lưu thông tin người dùng sau khi đăng nhập thành công.
     *
     * @param currentUser đối tượng người dùng đăng nhập
     */
    public void setCurrentUser(Object currentUser) {
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
