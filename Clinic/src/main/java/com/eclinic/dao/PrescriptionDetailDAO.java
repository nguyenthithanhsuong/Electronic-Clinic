package com.eclinic.dao;

import com.eclinic.database.ConnectionManager;
import com.eclinic.models.PrescriptionDetail;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class PrescriptionDetailDAO {

    public long create(long prescriptionId, long medicineId, int quantity, String dosage) throws SQLException {
        String sql = "INSERT INTO prescription_details (prescription_id, medicine_id, quantity, dosage) VALUES (?, ?, ?, ?) RETURNING id";
        Connection conn = ConnectionManager.getConnection();
        try {
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setLong(1, prescriptionId);
            stmt.setLong(2, medicineId);
            stmt.setInt(3, quantity);
            stmt.setString(4, dosage);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getLong(1);
            }
            throw new SQLException("Failed to create prescription detail.");
        } finally {
            ConnectionManager.closeConnection(conn);
        }
    }

    public PrescriptionDetail findById(long id) throws SQLException {
        String sql = "SELECT id, prescription_id, medicine_id, quantity, dosage FROM prescription_details WHERE id = ?";
        Connection conn = ConnectionManager.getConnection();
        try {
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setLong(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return new PrescriptionDetail(
                    rs.getLong("id"),
                    rs.getLong("prescription_id"),
                    rs.getLong("medicine_id"),
                    rs.getInt("quantity"),
                    rs.getString("dosage")
                );
            }
            return null;
        } finally {
            ConnectionManager.closeConnection(conn);
        }
    }

    public List findByPrescriptionId(long prescriptionId) throws SQLException {
        String sql = "SELECT id, prescription_id, medicine_id, quantity, dosage FROM prescription_details WHERE prescription_id = ?";
        Connection conn = ConnectionManager.getConnection();
        List details = new ArrayList();
        try {
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setLong(1, prescriptionId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                details.add(new PrescriptionDetail(
                    rs.getLong("id"),
                    rs.getLong("prescription_id"),
                    rs.getLong("medicine_id"),
                    rs.getInt("quantity"),
                    rs.getString("dosage")
                ));
            }
            return details;
        } finally {
            ConnectionManager.closeConnection(conn);
        }
    }

    public List findAll() throws SQLException {
        String sql = "SELECT id, prescription_id, medicine_id, quantity, dosage FROM prescription_details";
        Connection conn = ConnectionManager.getConnection();
        List details = new ArrayList();
        try {
            PreparedStatement stmt = conn.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                details.add(new PrescriptionDetail(
                    rs.getLong("id"),
                    rs.getLong("prescription_id"),
                    rs.getLong("medicine_id"),
                    rs.getInt("quantity"),
                    rs.getString("dosage")
                ));
            }
            return details;
        } finally {
            ConnectionManager.closeConnection(conn);
        }
    }

    public boolean update(long id, int quantity, String dosage) throws SQLException {
        String sql = "UPDATE prescription_details SET quantity = ?, dosage = ? WHERE id = ?";
        Connection conn = ConnectionManager.getConnection();
        try {
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, quantity);
            stmt.setString(2, dosage);
            stmt.setLong(3, id);
            int rows = stmt.executeUpdate();
            return rows > 0;
        } finally {
            ConnectionManager.closeConnection(conn);
        }
    }

    public boolean delete(long id) throws SQLException {
        String sql = "DELETE FROM prescription_details WHERE id = ?";
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
