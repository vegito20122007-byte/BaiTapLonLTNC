package com.auction.client.util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Kiểm thử đơn vị (Unit Test) cho ImageUtils bao gồm trường hợp chuẩn, biên và ngoại lệ.
 */
@DisplayName("Kiểm thử ImageUtils (Toàn diện & Ngoại lệ)")
public class ImageUtilsTest {

    private Path tempFile;

    @BeforeEach
    void setUp() throws IOException {
        tempFile = Files.createTempFile("test_auction_img", ".png");
        byte[] sampleData = "SAMPLE_PNG_IMAGE_BINARY_DATA_123456789".getBytes(StandardCharsets.UTF_8);
        Files.write(tempFile, sampleData);
    }

    @AfterEach
    void tearDown() throws IOException {
        if (tempFile != null && Files.exists(tempFile)) {
            Files.deleteIfExists(tempFile);
        }

        // Dọn dẹp thư mục storage sinh ra trong quá trình test nếu có
        Path storageDir = Path.of("storage");
        if (Files.exists(storageDir)) {
            Files.walk(storageDir)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        }
    }

    @Test
    @DisplayName("Chuyển đổi File sang Base64 và giải mã ngược lại khớp 100%")
    void testFileToBase64AndBack() throws IOException {
        File file = tempFile.toFile();
        String base64 = ImageUtils.fileToBase64(file);

        assertNotNull(base64, "Chuỗi Base64 không được null");
        assertFalse(base64.isEmpty(), "Chuỗi Base64 không được rỗng");

        // Giải mã ngược lại
        byte[] originalBytes = Files.readAllBytes(tempFile);
        byte[] decodedBytes = ImageUtils.base64ToBytes(base64);

        assertArrayEquals(originalBytes, decodedBytes, "Dữ liệu sau khi giải mã phải trùng khớp 100% với dữ liệu gốc");
    }

    @Test
    @DisplayName("Bảo toàn mảng byte qua chu trình bytes -> Base64 -> bytes")
    void testBytesToBase64RoundTrip() {
        byte[] raw = new byte[]{10, 20, 30, 40, 50, -60, -70, 80};
        String base64 = ImageUtils.bytesToBase64(raw);
        byte[] decoded = ImageUtils.base64ToBytes(base64);

        assertArrayEquals(raw, decoded, "Quá trình encode và decode Base64 phải bảo toàn dữ liệu nhị phân");
    }

    @Test
    @DisplayName("Xử lý an toàn khi truyền tham số null hoặc mảng rỗng")
    void testNullAndEmptyHandling() {
        assertEquals("", ImageUtils.bytesToBase64(null));
        assertEquals("", ImageUtils.bytesToBase64(new byte[0]));
        assertArrayEquals(new byte[0], ImageUtils.base64ToBytes(null));
        assertArrayEquals(new byte[0], ImageUtils.base64ToBytes(""));
        assertArrayEquals(new byte[0], ImageUtils.base64ToBytes("   "));

        assertThrows(IllegalArgumentException.class, () -> ImageUtils.fileToBase64(null));
        assertThrows(IllegalArgumentException.class, () -> ImageUtils.fileToBase64(new File("non_existent_file_xyz.png")));
    }

    @Test
    @DisplayName("Lưu ảnh Base64 ra đĩa và nạp lại thành công")
    void testSaveAndLoadImage() throws IOException {
        String originalBase64 = ImageUtils.fileToBase64(tempFile.toFile());

        // Lưu ảnh
        String savedPath = ImageUtils.saveImageToServer(originalBase64, "Vintage Car");
        assertNotNull(savedPath, "Đường dẫn file sau khi lưu không được null");
        assertTrue(Files.exists(Path.of(savedPath)), "File ảnh sau khi lưu phải tồn tại trên đĩa");

        // Đọc lại từ đường dẫn
        String loadedBase64 = ImageUtils.loadImageFromServer(savedPath);
        assertEquals(originalBase64, loadedBase64, "Chuỗi Base64 nạp lại phải trùng với chuỗi ban đầu");
    }

    @Test
    @DisplayName("Biên & Ngoại lệ: Ném IllegalArgumentException khi Base64 chứa ký tự không hợp lệ")
    void testCorruptedBase64ThrowsException() {
        String malformedBase64 = "!!@@##$$%%^^&&**(()";
        assertThrows(IllegalArgumentException.class, () -> ImageUtils.base64ToBytes(malformedBase64));
    }

    @Test
    @DisplayName("Biên: saveImageToServer trả về null khi Base64 là null hoặc rỗng")
    void testSaveImageNullAndEmptyPayload() throws IOException {
        assertNull(ImageUtils.saveImageToServer(null, "TestItem"));
        assertNull(ImageUtils.saveImageToServer("", "TestItem"));
        assertNull(ImageUtils.saveImageToServer("   ", "TestItem"));
    }

    @Test
    @DisplayName("Biên: saveImageToServer tự sinh tên mặc định khi itemName là null")
    void testSaveImageNullItemName() throws IOException {
        String originalBase64 = ImageUtils.fileToBase64(tempFile.toFile());
        String savedPath = ImageUtils.saveImageToServer(originalBase64, null);

        assertNotNull(savedPath);
        assertTrue(savedPath.contains("item_"), "Tên file phải bắt đầu bằng tiền tố mặc định item_");
        assertTrue(Files.exists(Path.of(savedPath)));
    }

    @Test
    @DisplayName("Biên: Chuẩn hóa ký tự đặc biệt và dấu tiếng Việt trong tên sản phẩm khi lưu file")
    void testSaveImageSpecialCharactersInItemName() throws IOException {
        String originalBase64 = ImageUtils.fileToBase64(tempFile.toFile());
        String specialName = "Đồng Hồ Cổ: Rolex 1954 / Thụy Sĩ * & ? # @";
        String savedPath = ImageUtils.saveImageToServer(originalBase64, specialName);

        assertNotNull(savedPath);
        assertTrue(Files.exists(Path.of(savedPath)), "File phải được tạo an toàn trên hệ thống tệp Windows");
    }

    @Test
    @DisplayName("Biên: loadImageFromServer trả về null khi path là null hoặc rỗng")
    void testLoadImageNullOrEmptyPath() throws IOException {
        assertNull(ImageUtils.loadImageFromServer(null));
        assertNull(ImageUtils.loadImageFromServer(""));
        assertNull(ImageUtils.loadImageFromServer("   "));
    }

    @Test
    @DisplayName("Ngoại lệ: loadImageFromServer ném IllegalArgumentException khi file không tồn tại")
    void testLoadImageNonExistentFileThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> ImageUtils.loadImageFromServer("path/to/never_exists_image.png"));
    }

    @Test
    @DisplayName("Biên: saveImageToServer gọi liên tiếp trong cùng mili-giây không được trùng tên hay ghi đè")
    void testImageUtilsSameMillisecondCollision() throws IOException {
        java.util.Set<String> uniquePaths = new java.util.HashSet<>();
        int attempts = 20;
        for (int i = 0; i < attempts; i++) {
            String b64 = ImageUtils.bytesToBase64(("IMAGE_DATA_" + i).getBytes(StandardCharsets.UTF_8));
            String path = ImageUtils.saveImageToServer(b64, "ItemA");
            uniquePaths.add(path);
        }

        assertEquals(attempts, uniquePaths.size(),
                "Lưu " + attempts + " ảnh liên tiếp phải tạo đủ " + attempts + " file riêng biệt, không được ghi đè nhau!");
    }
}
