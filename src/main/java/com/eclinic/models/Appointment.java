package com.eclinic.models;

public class Appointment {
    private long id;
    private long doctorId;
    private long patientId;
    private String appointmentStartDate;
    private String appointmentEndDate;
    private String reason;
    private String status;
    private String createdAt;

    public Appointment(long id, long doctorId, long patientId, String appointmentStartDate, String appointmentEndDate, String reason, String status, String createdAt) {
        this.id = id;
        this.doctorId = doctorId;
        this.patientId = patientId;
        this.appointmentStartDate = appointmentStartDate;
        this.appointmentEndDate = appointmentEndDate;
        this.reason = reason;
        this.status = status;
        this.createdAt = createdAt;
    }

    public Appointment() {}

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getDoctorId() { return doctorId; }
    public void setDoctorId(long doctorId) { this.doctorId = doctorId; }

    public long getPatientId() { return patientId; }
    public void setPatientId(long patientId) { this.patientId = patientId; }

    public String getAppointmentStartDate() { return appointmentStartDate; }
    public void setAppointmentStartDate(String appointmentStartDate) { this.appointmentStartDate = appointmentStartDate; }

    public String getAppointmentEndDate() { return appointmentEndDate; }
    public void setAppointmentEndDate(String appointmentEndDate) { this.appointmentEndDate = appointmentEndDate; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String toString() {
        return "Appointment{" +
                "id=" + id +
                ", doctorId=" + doctorId +
                ", patientId=" + patientId +
                ", appointmentStartDate='" + appointmentStartDate + '\'' +
                ", status='" + status + '\'' +
                '}';
    }
}
