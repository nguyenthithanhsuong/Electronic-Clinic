package com.eclinic.models;

import java.math.BigDecimal;

public class Prescription {
    private long id;
    private long medicalRecordId;
    private String notes;
    private BigDecimal totalPrice;
    private String createdAt;

    public Prescription(long id, long medicalRecordId, String notes, BigDecimal totalPrice, String createdAt) {
        this.id = id;
        this.medicalRecordId = medicalRecordId;
        this.notes = notes;
        this.totalPrice = totalPrice;
        this.createdAt = createdAt;
    }

    public Prescription() {}

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getMedicalRecordId() { return medicalRecordId; }
    public void setMedicalRecordId(long medicalRecordId) { this.medicalRecordId = medicalRecordId; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public BigDecimal getTotalPrice() { return totalPrice; }
    public void setTotalPrice(BigDecimal totalPrice) { this.totalPrice = totalPrice; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String toString() {
        return "Prescription{" +
                "id=" + id +
                ", medicalRecordId=" + medicalRecordId +
                ", totalPrice=" + totalPrice +
                ", createdAt='" + createdAt + '\'' +
                '}';
    }
}
