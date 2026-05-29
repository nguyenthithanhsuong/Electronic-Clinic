package com.eclinic.dao;

import com.eclinic.database.ConnectionManager;
import com.eclinic.models.Medicine;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class MedicineDAO {

    public long create(String name, String unit, BigDecimal price, int stockQuantity, String expiryDate) throws SQLException {
        String sql = "INSERT INTO medicines (name, unit, price, stock_quantity, expiry_date) VALUES (?, ?, ?, ?, ?) RETURNING id";
        Connection conn = ConnectionManager.getConnection();
        try {
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, name);
            stmt.setString(2, unit);
            stmt.setBigDecimal(3, price);
            stmt.setInt(4, stockQuantity);
            stmt.setTimestamp(5, Timestamp.valueOf(expiryDate + " 00:00:00"));
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getLong(1);
            }
            throw new SQLException("Failed to create medicine.");
        } finally {
            ConnectionManager.closeConnection(conn);
        }
    }

    public Medicine findById(long id) throws SQLException {
        String sql = "SELECT id, name, unit, price, stock_quantity, expiry_date FROM medicines WHERE id = ?";
        Connection conn = ConnectionManager.getConnection();
        try {
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setLong(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return new Medicine(
                    rs.getLong("id"),
                    rs.getString("name"),
                    rs.getString("unit"),
                    rs.getBigDecimal("price"),
                    rs.getInt("stock_quantity"),
                    rs.getString("expiry_date")
                );
            }
            return null;
        } finally {
            ConnectionManager.closeConnection(conn);
        }
    }

    public List findAll() throws SQLException {
        String sql = "SELECT id, name, unit, price, stock_quantity, expiry_date FROM medicines";
        Connection conn = ConnectionManager.getConnection();
        List medicines = new ArrayList();
        try {
            PreparedStatement stmt = conn.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                medicines.add(new Medicine(
                    rs.getLong("id"),
                    rs.getString("name"),
                    rs.getString("unit"),
                    rs.getBigDecimal("price"),
                    rs.getInt("stock_quantity"),
                    rs.getString("expiry_date")
                ));
            }
            return medicines;
        } finally {
            ConnectionManager.closeConnection(conn);
        }
    }

    public boolean updateStock(long id, int newQuantity) throws SQLException {
        String sql = "UPDATE medicines SET stock_quantity = ? WHERE id = ?";
        Connection conn = ConnectionManager.getConnection();
        try {
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, newQuantity);
            stmt.setLong(2, id);
            int rows = stmt.executeUpdate();
            return rows > 0;
        } finally {
            ConnectionManager.closeConnection(conn);
        }
    }

    public boolean delete(long id) throws SQLException {
        String sql = "DELETE FROM medicines WHERE id = ?";
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
