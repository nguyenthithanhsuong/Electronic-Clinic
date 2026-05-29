package com.eclinic.dao;

import com.eclinic.database.ConnectionManager;
import com.eclinic.models.MedicalRecord;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class MedicalRecordDAO {

    public long create(long appointmentId, String symptoms, String diagnosis, String recordType, String treatmentPlan) throws SQLException {
        String sql = "INSERT INTO medical_records (appointment_id, symptoms, diagnosis, record_type, treatment_plan) VALUES (?, ?, ?, ?, ?) RETURNING id";
        Connection conn = ConnectionManager.getConnection();
        try {
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setLong(1, appointmentId);
            stmt.setString(2, symptoms);
            stmt.setString(3, diagnosis);
            stmt.setString(4, recordType);
            stmt.setString(5, treatmentPlan);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getLong(1);
            }
            throw new SQLException("Failed to create medical record.");
        } finally {
            ConnectionManager.closeConnection(conn);
        }
    }

    public MedicalRecord findById(long id) throws SQLException {
        String sql = "SELECT id, appointment_id, symptoms, diagnosis, record_type, treatment_plan, created_at FROM medical_records WHERE id = ?";
        Connection conn = ConnectionManager.getConnection();
        try {
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setLong(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return new MedicalRecord(
                    rs.getLong("id"),
                    rs.getLong("appointment_id"),
                    rs.getString("symptoms"),
                    rs.getString("diagnosis"),
                    rs.getString("record_type"),
                    rs.getString("treatment_plan"),
                    rs.getString("created_at")
                );
            }
            return null;
        } finally {
            ConnectionManager.closeConnection(conn);
        }
    }

    public MedicalRecord findByAppointmentId(long appointmentId) throws SQLException {
        String sql = "SELECT id, appointment_id, symptoms, diagnosis, record_type, treatment_plan, created_at FROM medical_records WHERE appointment_id = ?";
        Connection conn = ConnectionManager.getConnection();
        try {
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setLong(1, appointmentId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return new MedicalRecord(
                    rs.getLong("id"),
                    rs.getLong("appointment_id"),
                    rs.getString("symptoms"),
                    rs.getString("diagnosis"),
                    rs.getString("record_type"),
                    rs.getString("treatment_plan"),
                    rs.getString("created_at")
                );
            }
            return null;
        } finally {
            ConnectionManager.closeConnection(conn);
        }
    }

    public List findAll() throws SQLException {
        String sql = "SELECT id, appointment_id, symptoms, diagnosis, record_type, treatment_plan, created_at FROM medical_records ORDER BY created_at DESC";
        Connection conn = ConnectionManager.getConnection();
        List records = new ArrayList();
        try {
            PreparedStatement stmt = conn.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                records.add(new MedicalRecord(
                    rs.getLong("id"),
                    rs.getLong("appointment_id"),
                    rs.getString("symptoms"),
                    rs.getString("diagnosis"),
                    rs.getString("record_type"),
                    rs.getString("treatment_plan"),
                    rs.getString("created_at")
                ));
            }
            return records;
        } finally {
            ConnectionManager.closeConnection(conn);
        }
    }

    public boolean update(long id, String diagnosis, String treatmentPlan) throws SQLException {
        String sql = "UPDATE medical_records SET diagnosis = ?, treatment_plan = ? WHERE id = ?";
        Connection conn = ConnectionManager.getConnection();
        try {
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, diagnosis);
            stmt.setString(2, treatmentPlan);
            stmt.setLong(3, id);
            int rows = stmt.executeUpdate();
            return rows > 0;
        } finally {
            ConnectionManager.closeConnection(conn);
        }
    }

    public boolean delete(long id) throws SQLException {
        String sql = "DELETE FROM medical_records WHERE id = ?";
        Connection conn = ConnectionManager.getConnection();
        try {
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setLong(1, id);
            int rows = stmt.executeUpdate();
            return rows > 0;
        } finally {
            ConnectionManager.closeConnection(conn);
        }
    }
}
