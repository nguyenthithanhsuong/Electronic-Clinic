package com.eclinic.models;

import java.math.BigDecimal;

public class Medicine {
    private long id;
    private String name;
    private String unit;
    private BigDecimal price;
    private int stockQuantity;
    private String expiryDate;

    public Medicine(long id, String name, String unit, BigDecimal price, int stockQuantity, String expiryDate) {
        this.id = id;
        this.name = name;
        this.unit = unit;
        this.price = price;
        this.stockQuantity = stockQuantity;
        this.expiryDate = expiryDate;
    }

    public Medicine() {}

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public int getStockQuantity() { return stockQuantity; }
    public void setStockQuantity(int stockQuantity) { this.stockQuantity = stockQuantity; }

    public String getExpiryDate() { return expiryDate; }
    public void setExpiryDate(String expiryDate) { this.expiryDate = expiryDate; }

    public String toString() {
        return "Medicine{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", unit='" + unit + '\'' +
                ", price=" + price +
                ", stockQuantity=" + stockQuantity +
                ", expiryDate='" + expiryDate + '\'' +
                '}';
    }
}
