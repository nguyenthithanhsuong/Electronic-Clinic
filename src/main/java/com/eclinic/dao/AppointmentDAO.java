package com.eclinic.dao;

import com.eclinic.database.ConnectionManager;
import com.eclinic.models.Appointment;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class AppointmentDAO {

    public long create(long doctorId, long patientId, String startDate, String endDate, String reason, String status) throws SQLException {
        String sql = "INSERT INTO appointments (doctor_id, patient_id, appointment_start_date, appointment_end_date, reason, status) VALUES (?, ?, ?, ?, ?, ?) RETURNING id";
        Connection conn = ConnectionManager.getConnection();
        try {
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setLong(1, doctorId);
            stmt.setLong(2, patientId);
            stmt.setTimestamp(3, Timestamp.valueOf(startDate));
            stmt.setTimestamp(4, Timestamp.valueOf(endDate));
            stmt.setString(5, reason);
            stmt.setString(6, status);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getLong(1);
            }
            throw new SQLException("Failed to create appointment.");
        } finally {
            ConnectionManager.closeConnection(conn);
        }
    }

    public Appointment findById(long id) throws SQLException {
        String sql = "SELECT id, doctor_id, patient_id, appointment_start_date, appointment_end_date, reason, status, created_at FROM appointments WHERE id = ?";
        Connection conn = ConnectionManager.getConnection();
        try {
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setLong(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return new Appointment(
                    rs.getLong("id"),
                    rs.getLong("doctor_id"),
                    rs.getLong("patient_id"),
                    rs.getString("appointment_start_date"),
                    rs.getString("appointment_end_date"),
                    rs.getString("reason"),
                    rs.getString("status"),
                    rs.getString("created_at")
                );
            }
            return null;
        } finally {
            ConnectionManager.closeConnection(conn);
        }
    }

    public List findByPatientId(long patientId) throws SQLException {
        String sql = "SELECT id, doctor_id, patient_id, appointment_start_date, appointment_end_date, reason, status, created_at FROM appointments WHERE patient_id = ? ORDER BY appointment_start_date DESC";
        Connection conn = ConnectionManager.getConnection();
        List appointments = new ArrayList();
        try {
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setLong(1, patientId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                appointments.add(new Appointment(
                    rs.getLong("id"),
                    rs.getLong("doctor_id"),
                    rs.getLong("patient_id"),
                    rs.getString("appointment_start_date"),
                    rs.getString("appointment_end_date"),
                    rs.getString("reason"),
                    rs.getString("status"),
                    rs.getString("created_at")
                ));
            }
            return appointments;
        } finally {
            ConnectionManager.closeConnection(conn);
        }
    }

    public List findAll() throws SQLException {
        String sql = "SELECT id, doctor_id, patient_id, appointment_start_date, appointment_end_date, reason, status, created_at FROM appointments ORDER BY appointment_start_date DESC";
        Connection conn = ConnectionManager.getConnection();
        List appointments = new ArrayList();
        try {
            PreparedStatement stmt = conn.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                appointments.add(new Appointment(
                    rs.getLong("id"),
                    rs.getLong("doctor_id"),
                    rs.getLong("patient_id"),
                    rs.getString("appointment_start_date"),
                    rs.getString("appointment_end_date"),
                    rs.getString("reason"),
                    rs.getString("status"),
                    rs.getString("created_at")
                ));
            }
            return appointments;
        } finally {
            ConnectionManager.closeConnection(conn);
        }
    }

    public boolean updateStatus(long id, String newStatus) throws SQLException {
        String sql = "UPDATE appointments SET status = ? WHERE id = ?";
        Connection conn = ConnectionManager.getConnection();
        try {
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, newStatus);
            stmt.setLong(2, id);
            int rows = stmt.executeUpdate();
            return rows > 0;
        } finally {
            ConnectionManager.closeConnection(conn);
        }
    }

    public boolean delete(long id) throws SQLException {
        String sql = "DELETE FROM appointments WHERE id = ?";
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
