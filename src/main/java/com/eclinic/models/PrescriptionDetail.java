package com.eclinic.models;

public class PrescriptionDetail {
    private long id;
    private long prescriptionId;
    private long medicineId;
    private int quantity;
    private String dosage;

    public PrescriptionDetail(long id, long prescriptionId, long medicineId, int quantity, String dosage) {
        this.id = id;
        this.prescriptionId = prescriptionId;
        this.medicineId = medicineId;
        this.quantity = quantity;
        this.dosage = dosage;
    }

    public PrescriptionDetail() {}

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getPrescriptionId() { return prescriptionId; }
    public void setPrescriptionId(long prescriptionId) { this.prescriptionId = prescriptionId; }

    public long getMedicineId() { return medicineId; }
    public void setMedicineId(long medicineId) { this.medicineId = medicineId; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public String getDosage() { return dosage; }
    public void setDosage(String dosage) { this.dosage = dosage; }

    public String toString() {
        return "PrescriptionDetail{" +
                "id=" + id +
                ", prescriptionId=" + prescriptionId +
                ", medicineId=" + medicineId +
                ", quantity=" + quantity +
                ", dosage='" + dosage + '\'' +
                '}';
    }
}
