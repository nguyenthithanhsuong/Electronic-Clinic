package com.eclinic.dao;

import com.eclinic.database.ConnectionManager;
import com.eclinic.models.Prescription;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class PrescriptionDAO {

    public long create(long medicalRecordId, String notes, BigDecimal totalPrice) throws SQLException {
        String sql = "INSERT INTO prescriptions (medical_record_id, notes, total_price) VALUES (?, ?, ?) RETURNING id";
        Connection conn = ConnectionManager.getConnection();
        try {
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setLong(1, medicalRecordId);
            stmt.setString(2, notes);
            stmt.setBigDecimal(3, totalPrice);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getLong(1);
            }
            throw new SQLException("Failed to create prescription.");
        } finally {
            ConnectionManager.closeConnection(conn);
        }
    }

    public Prescription findById(long id) throws SQLException {
        String sql = "SELECT id, medical_record_id, notes, total_price, created_at FROM prescriptions WHERE id = ?";
        Connection conn = ConnectionManager.getConnection();
        try {
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setLong(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return new Prescription(
                    rs.getLong("id"),
                    rs.getLong("medical_record_id"),
                    rs.getString("notes"),
                    rs.getBigDecimal("total_price"),
                    rs.getString("created_at")
                );
            }
            return null;
        } finally {
            ConnectionManager.closeConnection(conn);
        }
    }

    public Prescription findByMedicalRecordId(long medicalRecordId) throws SQLException {
        String sql = "SELECT id, medical_record_id, notes, total_price, created_at FROM prescriptions WHERE medical_record_id = ?";
        Connection conn = ConnectionManager.getConnection();
        try {
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setLong(1, medicalRecordId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return new Prescription(
                    rs.getLong("id"),
                    rs.getLong("medical_record_id"),
                    rs.getString("notes"),
                    rs.getBigDecimal("total_price"),
                    rs.getString("created_at")
                );
            }
            return null;
        } finally {
            ConnectionManager.closeConnection(conn);
        }
    }

    public List findAll() throws SQLException {
        String sql = "SELECT id, medical_record_id, notes, total_price, created_at FROM prescriptions";
        Connection conn = ConnectionManager.getConnection();
        List prescriptions = new ArrayList();
        try {
            PreparedStatement stmt = conn.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                prescriptions.add(new Prescription(
                    rs.getLong("id"),
                    rs.getLong("medical_record_id"),
                    rs.getString("notes"),
                    rs.getBigDecimal("total_price"),
                    rs.getString("created_at")
                ));
            }
            return prescriptions;
        } finally {
            ConnectionManager.closeConnection(conn);
        }
    }

    public boolean update(long id, String notes, BigDecimal totalPrice) throws SQLException {
        String sql = "UPDATE prescriptions SET notes = ?, total_price = ? WHERE id = ?";
        Connection conn = ConnectionManager.getConnection();
        try {
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, notes);
            stmt.setBigDecimal(2, totalPrice);
            stmt.setLong(3, id);
            int rows = stmt.executeUpdate();
            return rows > 0;
        } finally {
            ConnectionManager.closeConnection(conn);
        }
    }

    public boolean delete(long id) throws SQLException {
        String sql = "DELETE FROM prescriptions WHERE id = ?";
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
