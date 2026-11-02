package com.auction.client;

/**
 * Lớp Launcher chứa hàm main chính thức để khởi chạy ứng dụng.
 *
 * Mẹo quan trọng trong JavaFX:
 * Nếu chạy trực tiếp từ một lớp kế thừa Application (ClientApp), Java có thể báo lỗi
 * thiếu JavaFX runtime nếu không cấu hình module/VM options.
 * Dùng một lớp Launcher trung gian như thế này sẽ giúp bạn bấm Run trực tiếp trong IDE
 * (VS Code, IntelliJ, Eclipse) mà KHÔNG cần cấu hình bất kỳ VM options phức tạp nào!
 */
public class Launcher {
    public static void main(String[] args) {
        ClientApp.main(args);
    }
}