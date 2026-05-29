package com.eclinic.models;

public class MedicalRecord {
    private long id;
    private long appointmentId;
    private String symptoms;
    private String diagnosis;
    private String recordType;
    private String treatmentPlan;
    private String createdAt;

    public MedicalRecord(long id, long appointmentId, String symptoms, String diagnosis, String recordType, String treatmentPlan, String createdAt) {
        this.id = id;
        this.appointmentId = appointmentId;
        this.symptoms = symptoms;
        this.diagnosis = diagnosis;
        this.recordType = recordType;
        this.treatmentPlan = treatmentPlan;
        this.createdAt = createdAt;
    }

    public MedicalRecord() {}

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getAppointmentId() { return appointmentId; }
    public void setAppointmentId(long appointmentId) { this.appointmentId = appointmentId; }

    public String getSymptoms() { return symptoms; }
    public void setSymptoms(String symptoms) { this.symptoms = symptoms; }

    public String getDiagnosis() { return diagnosis; }
    public void setDiagnosis(String diagnosis) { this.diagnosis = diagnosis; }

    public String getRecordType() { return recordType; }
    public void setRecordType(String recordType) { this.recordType = recordType; }

    public String getTreatmentPlan() { return treatmentPlan; }
    public void setTreatmentPlan(String treatmentPlan) { this.treatmentPlan = treatmentPlan; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String toString() {
        return "MedicalRecord{" +
                "id=" + id +
                ", appointmentId=" + appointmentId +
                ", recordType='" + recordType + '\'' +
                ", diagnosis='" + diagnosis + '\'' +
                '}';
    }
}
