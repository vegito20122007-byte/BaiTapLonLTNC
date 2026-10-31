package com.auction.model;

/**
 * Lớp trừu tượng định nghĩa sản phẩm đưa lên sàn đấu giá.
 * Thể hiện tính Đa hình (Polymorphism) qua phương thức trừu tượng getDetails().
 *
 * Các lớp kế thừa Item: Electronics, Art, Vehicle.
 */

public abstract class Item extends Entity{
    private static final long serialVersionUID = 1L;
    protected String name;
    protected String description;
    protected String category;
    protected String imageData;
    protected Seller owner;

    public Item(Integer id) {
        super(id);
    }

    public Item(Integer id, String name, String description, String category, Seller owner) {
        super(id);
        this.name = name;
        this.description = description;
        this.category = category;
        this.owner = owner;
    }

    /**
     * Kiểm tra xem một người dùng có phải là chủ sở hữu sản phẩm hay không.
     *
     * @param user Người dùng cần kiểm tra
     * @return true nếu chính là chủ sở hữu, ngược lại false
     */

    public boolean isOwner(User user) {
        return (owner != null) && (user != null) && (owner.getId() != null) && (owner.getId().equals(user.getId()));
    }

    /**
     * Phương thức trừu tượng trả về thông tin đặc trưng của từng loại sản phẩm (Đa hình).
     *
     * @return Chuỗi mô tả thuộc tính chuyên biệt
     */
    public abstract String getDetails();

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getImageData() { return imageData; }
    public void setImageData(String imageData) { this.imageData = imageData; }

    public Seller getOwner() { return owner; }
    public void setOwner(Seller owner) { this.owner = owner; }

}
