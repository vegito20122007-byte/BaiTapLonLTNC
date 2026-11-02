package com.auction.model;

import java.util.Map;

/**
 * Factory chịu trách nhiệm khởi tạo các đối tượng sản phẩm (Item) cụ thể (Factory Pattern).
 * Giúp trừu tượng hóa quá trình khởi tạo, giảm phụ thuộc (loose coupling) giữa
 * tầng giao diện (Controller), tầng dữ liệu (DAO) với các lớp cụ thể (Art, Electronics, Vehicle).
 */
public class ItemFactory {

    /**
     * Phương thức khởi tạo nhanh khớp chính xác:
     * + createItem(category: String, id: Integer): Item
     *
     * Gọi trực tiếp constructor 1 tham số của từng lớp con.
     *
     * @param category Loại sản phẩm ("ART", "ELECTRONICS", "VEHICLE")
     * @param id Mã định danh sản phẩm
     * @return Thể hiện cụ thể tương ứng của Item
     * @throws IllegalArgumentException nếu category rỗng hoặc không hợp lệ
     */
    public static Item createItem(String category, Integer id) {
        if (category == null || category.trim().isEmpty()) {
            throw new IllegalArgumentException("Loại sản phẩm không được để trống");
        }

        switch (category.trim().toUpperCase()) {
            case "ART":
                return new Art(id);

            case "ELECTRONICS":
                return new Electronics(id);

            case "VEHICLE":
                return new Vehicle(id);

            default:
                throw new IllegalArgumentException("Loại sản phẩm không được hỗ trợ: " + category);
        }
    }

    /**
     * Phương thức tạo nhanh khi có các thông tin cơ bản chung của sản phẩm.
     *
     * @param category Danh mục sản phẩm
     * @param id Mã sản phẩm
     * @param name Tên sản phẩm
     * @param description Mô tả
     * @param owner Người bán sở hữu
     * @return Đối tượng Item đã được set các thông tin cơ bản
     */
    public static Item createItem(String category, Integer id, String name, String description, Seller owner) {
        Item item = createItem(category, id);
        item.setName(name);
        item.setDescription(description);
        item.setOwner(owner);
        return item;
    }

    // CÁC PHƯƠNG THỨC TIỆN ÍCH TẠO ĐỐI TƯỢNG ĐẦY ĐỦ THUỘC TÍNH (FULL CONSTRUCTOR)
    // Dùng khi đọc dữ liệu từ DAO / CSDL hoặc khi người dùng nộp form tạo sản phẩm

    /**
     * Khởi tạo tác phẩm Nghệ thuật (Art) đầy đủ thuộc tính.
     */
    public static Art createArt(Integer id, String name, String description, Seller owner,
                                String artist, int yearCreated) {
        return new Art(id, name, description, owner, artist, yearCreated);
    }

    /**
     * Khởi tạo Thiết bị điện tử (Electronics) đầy đủ thuộc tính.
     */
    public static Electronics createElectronics(Integer id, String name, String description,
                                                Seller owner, String brand, int warrantyMonths) {
        return new Electronics(id, name, description, owner, brand, warrantyMonths);
    }

    /**
     * Khởi tạo Phương tiện giao thông (Vehicle) đầy đủ thuộc tính.
     */
    public static Vehicle createVehicle(Integer id, String name, String description,
                                        Seller owner, String manufacturer, double odometer) {
        return new Vehicle(id, name, description, owner, manufacturer, odometer);
    }

    /**
     * Phương thức linh hoạt tạo Item đầy đủ thông tin thông qua Map thuộc tính mở rộng.
     * Cực kỳ hữu ích cho tầng DAO khi đọc các trường động từ CSDL.
     */
    public static Item createItemWithAttributes(String category, Integer id, String name,
                                                String description, Seller owner,
                                                Map<String, Object> extraAttributes) {
        if (category == null) {
            throw new IllegalArgumentException("Danh mục không được để trống");
        }

        switch (category.trim().toUpperCase()) {
            case "ART": {
                String artist = (extraAttributes != null && extraAttributes.get("artist") != null)
                        ? extraAttributes.get("artist").toString() : "Unknown";
                int year = (extraAttributes != null && extraAttributes.get("yearCreated") instanceof Number)
                        ? ((Number) extraAttributes.get("yearCreated")).intValue() : 0;
                return createArt(id, name, description, owner, artist, year);
            }

            case "ELECTRONICS": {
                String brand = (extraAttributes != null && extraAttributes.get("brand") != null)
                        ? extraAttributes.get("brand").toString() : "Unknown";
                int warranty = (extraAttributes != null && extraAttributes.get("warrantyMonths") instanceof Number)
                        ? ((Number) extraAttributes.get("warrantyMonths")).intValue() : 0;
                return createElectronics(id, name, description, owner, brand, warranty);
            }

            case "VEHICLE": {
                String manufacturer = (extraAttributes != null && extraAttributes.get("manufacturer") != null)
                        ? extraAttributes.get("manufacturer").toString() : "Unknown";
                double odo = (extraAttributes != null && extraAttributes.get("odometer") instanceof Number)
                        ? ((Number) extraAttributes.get("odometer")).doubleValue() : 0.0;
                return createVehicle(id, name, description, owner, manufacturer, odo);
            }

            default:
                throw new IllegalArgumentException("Danh mục không hợp lệ: " + category);
        }
    }
}