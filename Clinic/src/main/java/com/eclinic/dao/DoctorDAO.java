package com.eclinic.dao;

import com.eclinic.database.ConnectionManager;
import com.eclinic.models.Doctor;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class DoctorDAO {

    public long create(long userId, String fullName, String specialty, String phone, String email, String roomNumber) throws SQLException {
        String sql = "INSERT INTO doctors (user_id, full_name, specialty, phone, email, room_number) VALUES (?, ?, ?, ?, ?, ?) RETURNING id";
        Connection conn = ConnectionManager.getConnection();
        try {
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setLong(1, userId);
            stmt.setString(2, fullName);
            stmt.setString(3, specialty);
            stmt.setString(4, phone);
            stmt.setString(5, email);
            stmt.setString(6, roomNumber);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getLong(1);
            }
            throw new SQLException("Failed to create doctor.");
        } finally {
            ConnectionManager.closeConnection(conn);
        }
    }

    public Doctor findById(long id) throws SQLException {
        String sql = "SELECT d.id, d.user_id, u.username, d.full_name, d.specialty, d.phone, d.email, d.room_number, d.created_at FROM doctors d LEFT JOIN users u ON d.user_id = u.id WHERE d.id = ?";
        Connection conn = ConnectionManager.getConnection();
        try {
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setLong(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return new Doctor(
                    rs.getLong("id"),
                    rs.getLong("user_id"),
                    rs.getString("username"),
                    rs.getString("full_name"),
                    rs.getString("specialty"),
                    rs.getString("phone"),
                    rs.getString("email"),
                    null,  // password is not stored in doctors table
                    rs.getString("room_number"),
                    rs.getString("created_at")
                );
            }
            return null;
        } finally {
            ConnectionManager.closeConnection(conn);
        }
    }

    public List findAll() throws SQLException {
        String sql = "SELECT d.id, d.user_id, u.username, d.full_name, d.specialty, d.phone, d.email, d.room_number, d.created_at FROM doctors d LEFT JOIN users u ON d.user_id = u.id";
        Connection conn = ConnectionManager.getConnection();
        List doctors = new ArrayList();
        try {
            PreparedStatement stmt = conn.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                doctors.add(new Doctor(
                    rs.getLong("id"),
                    rs.getLong("user_id"),
                    rs.getString("username"),
                    rs.getString("full_name"),
                    rs.getString("specialty"),
                    rs.getString("phone"),
                    rs.getString("email"),
                    null,  // password is not stored in doctors table
                    rs.getString("room_number"),
                    rs.getString("created_at")
                ));
            }
            return doctors;
        } finally {
            ConnectionManager.closeConnection(conn);
        }
    }

    public boolean update(long id, String fullName, String specialty, String email, String roomNumber) throws SQLException {
        String sql = "UPDATE doctors SET full_name = ?, specialty = ?, email = ?, room_number = ? WHERE id = ?";
        Connection conn = ConnectionManager.getConnection();
        try {
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, fullName);
            stmt.setString(2, specialty);
            stmt.setString(3, email);
            stmt.setString(4, roomNumber);
            stmt.setLong(5, id);
            int rows = stmt.executeUpdate();
            return rows > 0;
        } finally {
            ConnectionManager.closeConnection(conn);
        }
    }

    public boolean updateEmail(long id, String email) throws SQLException {
        String sql = "UPDATE doctors SET email = ? WHERE id = ?";
        Connection conn = ConnectionManager.getConnection();
        try {
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, email);
            stmt.setLong(2, id);
            int rows = stmt.executeUpdate();
            return rows > 0;
        } finally {
            ConnectionManager.closeConnection(conn);
        }
    }

    public Long getUserIdForDoctor(long doctorId) throws SQLException {
        String sql = "SELECT user_id FROM doctors WHERE id = ?";
        Connection conn = ConnectionManager.getConnection();
        try {
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setLong(1, doctorId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                long userId = rs.getLong("user_id");
                if (!rs.wasNull()) {
                    return userId;
                }
            }
            return null;
        } finally {
            ConnectionManager.closeConnection(conn);
        }
    }

    public boolean delete(long id) throws SQLException {
        String selectSql = "SELECT user_id FROM doctors WHERE id = ?";
        String deleteDoctorSql = "DELETE FROM doctors WHERE id = ?";
        String deleteUserSql = "DELETE FROM users WHERE id = ?";
        Connection conn = ConnectionManager.getConnection();
        try {
            conn.setAutoCommit(false);
            PreparedStatement selectStmt = conn.prepareStatement(selectSql);
            selectStmt.setLong(1, id);
            ResultSet rs = selectStmt.executeQuery();
            if (!rs.next()) {
                conn.rollback();
                return false;
            }
            long userId = rs.getLong("user_id");

            PreparedStatement delDoc = conn.prepareStatement(deleteDoctorSql);
            delDoc.setLong(1, id);
            int docRows = delDoc.executeUpdate();

            if (docRows <= 0) {
                conn.rollback();
                return false;
            }

            if (!rs.wasNull()) {
                PreparedStatement delUser = conn.prepareStatement(deleteUserSql);
                delUser.setLong(1, userId);
                int userRows = delUser.executeUpdate();
                if (userRows <= 0) {
                    conn.rollback();
                    return false;
                }
            }

            conn.commit();
            return true;
        } catch (SQLException ex) {
            try {
                conn.rollback();
            } catch (SQLException ignore) {
            }
            throw ex;
        } finally {
            try {
                conn.setAutoCommit(true);
            } catch (SQLException ignore) {
            }
            ConnectionManager.closeConnection(conn);
        }
    }
}
