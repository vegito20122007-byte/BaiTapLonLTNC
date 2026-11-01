package com.auction.client.util;

import javafx.scene.image.Image;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Base64;

/**
 * Lớp tiện ích xử lý ảnh (ImageUtils) theo đúng sơ đồ UML.
 * Chức năng chính:
 * 1. Chuyển đổi qua lại giữa File ảnh <-> Mảng byte <-> Chuỗi Base64.
 * 2. Hỗ trợ hiển thị ảnh lên giao diện JavaFX (ImageView) từ chuỗi Base64.
 * 3. Hỗ trợ lưu trữ và nạp ảnh từ đường dẫn tệp.
 */
public class ImageUtils {

    // Thư mục mặc định để lưu trữ ảnh (nếu chạy lưu local)
    private static final String DEFAULT_IMAGE_DIR = "storage/images";

    // Private constructor ngăn việc khởi tạo đối tượng vì đây là lớp tiện ích tĩnh (Utility Class)
    private ImageUtils() {
    }

    /**
     * Chuyển đổi một file ảnh từ máy tính thành chuỗi mã hóa Base64.
     * Sử dụng khi Người bán (Seller) bấm chọn ảnh sản phẩm từ FileChooser.
     *
     * @param file file ảnh được chọn
     * @return chuỗi Base64 biểu diễn dữ liệu nhị phân của ảnh
     * @throws IOException nếu có lỗi đọc file
     */
    public static String fileToBase64(File file) throws IOException {
        if (file == null || !file.exists() || !file.isFile()) {
            throw new IllegalArgumentException("File ảnh không hợp lệ hoặc không tồn tại!");
        }
        byte[] fileContent = Files.readAllBytes(file.toPath());
        return Base64.getEncoder().encodeToString(fileContent);
    }

    /**
     * Giải mã chuỗi Base64 thành mảng byte gốc.
     *
     * @param base64 chuỗi Base64
     * @return mảng byte dữ liệu ảnh
     */
    public static byte[] base64ToBytes(String base64) {
        if (base64 == null || base64.trim().isEmpty()) {
            return new byte[0];
        }
        return Base64.getDecoder().decode(base64.trim());
    }

    /**
     * Chuyển mảng byte dữ liệu nhị phân thành chuỗi Base64.
     *
     * @param bytes mảng byte
     * @return chuỗi Base64
     */
    public static String bytesToBase64(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return "";
        }
        return Base64.getEncoder().encodeToString(bytes);
    }

    /**
     * Chuyển đổi chuỗi Base64 trực tiếp thành đối tượng JavaFX Image để hiển thị lên ImageView.
     *
     * @param base64 chuỗi Base64 của ảnh
     * @return đối tượng Image của JavaFX (null nếu chuỗi rỗng hoặc không hợp lệ)
     */
    public static Image base64ToImage(String base64) {
        if (base64 == null || base64.trim().isEmpty()) {
            return null;
        }
        try {
            byte[] bytes = base64ToBytes(base64);
            return new Image(new ByteArrayInputStream(bytes));
        } catch (Exception e) {
            System.err.println("[ImageUtils] Lỗi khi tạo JavaFX Image từ Base64: " + e.getMessage());
            return null;
        }
    }

    /**
     * Lưu ảnh từ chuỗi Base64 vào thư mục lưu trữ (mô phỏng theo phương thức trong UML).
     *
     * @param base64   chuỗi Base64 của ảnh
     * @param itemName tên sản phẩm dùng để đặt tên file
     * @return đường dẫn tuyệt đối hoặc tương đối của file đã lưu
     * @throws IOException nếu có lỗi ghi file
     */
    public static String saveImageToServer(String base64, String itemName) throws IOException {
        if (base64 == null || base64.trim().isEmpty()) {
            return null;
        }

        // Tạo thư mục nếu chưa tồn tại
        Path dirPath = Paths.get(DEFAULT_IMAGE_DIR);
        if (!Files.exists(dirPath)) {
            Files.createDirectories(dirPath);
        }

        // Tạo tên file an toàn dựa trên tên sản phẩm và dấu thời gian
        String safeName = (itemName != null ? itemName.replaceAll("[^a-zA-Z0-9_-]", "_") : "item");
        String fileName = safeName + "_" + System.currentTimeMillis() + ".png";
        Path filePath = dirPath.resolve(fileName);

        // Ghi dữ liệu byte vào file
        byte[] imageBytes = base64ToBytes(base64);
        Files.write(filePath, imageBytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

        return filePath.toString();
    }

    /**
     * Đọc file ảnh từ đường dẫn trên đĩa và chuyển thành chuỗi Base64 (theo phương thức trong UML).
     *
     * @param path đường dẫn tới file ảnh
     * @return chuỗi Base64 của ảnh
     * @throws IOException nếu có lỗi đọc file
     */
    public static String loadImageFromServer(String path) throws IOException {
        if (path == null || path.trim().isEmpty()) {
            return null;
        }
        File file = new File(path);
        return fileToBase64(file);
    }
}
