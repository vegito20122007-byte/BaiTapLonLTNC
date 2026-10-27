package com.auction.model;

/**
 * Sản phẩm thuộc nhóm Thiết bị điện tử.
 */

public class Electronics extends Item{
    private static final long serialVersionUID = 1L;
    private String brand;
    private int warrantyMonths;

    public Electronics(Integer id) {
        super();
    }
    public Electronics(Integer id, String name, String description, Seller owner, String brand, int warrantyMonths) {
        super(id, name, description, "ELECTRONICS", owner);
        this.brand = brand;
        this.warrantyMonths = warrantyMonths;
    }

    @Override
    public String getDetails() {
        return String.format("Thương hiệu: %s | Bảo hành: %d tháng", brand, warrantyMonths);
    }

    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }

    public int getWarrantyMonths() { return warrantyMonths; }
    public void setWarrantyMonths(int warrantyMonths) { this.warrantyMonths = warrantyMonths; }
}
