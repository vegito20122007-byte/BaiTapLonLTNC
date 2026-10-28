package com.auction.model;

/**
 * Sản phẩm thuộc nhóm Phương tiện giao thông (Ô tô, Xe máy).
 */

public class Vehicle extends Item {
    private static final long serialVersionUID = 1L;
    private String manufacturer;
    private double odometer;

    public Vehicle(Integer id) {
        super(id);
        setCategory("VEHICLE");
    }
    public Vehicle(Integer id, String name, String description, Seller owner, String manufacturer, double odometer) {
        super(id, name, description, "VEHICLE", owner);
        this.manufacturer = manufacturer;
        this.odometer = odometer;
    }

    @Override
    public String getDetails() {
        return String.format("Hãng sản xuất: %s | Số km đã chạy: %,.0f km", manufacturer, odometer);
    }

    public String getManufacturer() { return manufacturer; }
    public void setManufacturer(String manufacturer) { this.manufacturer = manufacturer; }

    public double getOdometer() { return odometer; }
    public void setOdometer(double odometer) { this.odometer = odometer; }
}
