package com.auction.client.util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Kiểm thử đơn vị (Unit Test) cho ImageUtils.
 */
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
    void testBytesToBase64RoundTrip() {
        byte[] raw = new byte[]{10, 20, 30, 40, 50, -60, -70, 80};
        String base64 = ImageUtils.bytesToBase64(raw);
        byte[] decoded = ImageUtils.base64ToBytes(base64);

        assertArrayEquals(raw, decoded, "Quá trình encode và decode Base64 phải bảo toàn dữ liệu nhị phân");
    }

    @Test
    void testNullAndEmptyHandling() {
        assertEquals("", ImageUtils.bytesToBase64(null));
        assertEquals("", ImageUtils.bytesToBase64(new byte[0]));
        assertArrayEquals(new byte[0], ImageUtils.base64ToBytes(null));
        assertArrayEquals(new byte[0], ImageUtils.base64ToBytes(""));

        assertThrows(IllegalArgumentException.class, () -> {
            ImageUtils.fileToBase64(null);
        });

        assertThrows(IllegalArgumentException.class, () -> {
            ImageUtils.fileToBase64(new File("non_existent_file_xyz.png"));
        });
    }

    @Test
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
}
