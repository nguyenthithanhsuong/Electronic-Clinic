package com.eclinic.dao;

import com.eclinic.database.ConnectionManager;
import com.eclinic.models.Receptionist;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ReceptionistDAO {

    public long create(long userId, String fullName, String department, String phone, String email) throws SQLException {
        String sql = "INSERT INTO receptionists (user_id, full_name, department, phone, email) VALUES (?, ?, ?, ?, ?) RETURNING id";
        Connection conn = ConnectionManager.getConnection();
        try {
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setLong(1, userId);
            stmt.setString(2, fullName);
            stmt.setString(3, department);
            stmt.setString(4, phone);
            stmt.setString(5, email);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getLong(1);
            }
            throw new SQLException("Failed to create receptionist.");
        } finally {
            ConnectionManager.closeConnection(conn);
        }
    }

    public Receptionist findById(long id) throws SQLException {
        String sql = "SELECT r.id, r.user_id, u.username, r.full_name, r.department, r.phone, r.email, r.created_at FROM receptionists r LEFT JOIN users u ON r.user_id = u.id WHERE r.id = ?";
        Connection conn = ConnectionManager.getConnection();
        try {
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setLong(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return new Receptionist(
                    rs.getLong("id"),
                    rs.getLong("user_id"),
                    rs.getString("username"),
                    rs.getString("full_name"),
                    rs.getString("department"),
                    rs.getString("phone"),
                    rs.getString("email"),
                    null,  // password is not stored in receptionists table
                    rs.getString("created_at")
                );
            }
            return null;
        } finally {
            ConnectionManager.closeConnection(conn);
        }
    }

    public List findAll() throws SQLException {
        String sql = "SELECT r.id, r.user_id, u.username, r.full_name, r.department, r.phone, r.email, r.created_at FROM receptionists r LEFT JOIN users u ON r.user_id = u.id";
        Connection conn = ConnectionManager.getConnection();
        List receptionists = new ArrayList();
        try {
            PreparedStatement stmt = conn.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                receptionists.add(new Receptionist(
                    rs.getLong("id"),
                    rs.getLong("user_id"),
                    rs.getString("username"),
                    rs.getString("full_name"),
                    rs.getString("department"),
                    rs.getString("phone"),
                    rs.getString("email"),
                    null,  // password is not stored in receptionists table
                    rs.getString("created_at")
                ));
            }
            return receptionists;
        } finally {
            ConnectionManager.closeConnection(conn);
        }
    }

    public boolean update(long id, String fullName, String department, String email, String phone) throws SQLException {
        String sql = "UPDATE receptionists SET full_name = ?, department = ?, email = ?, phone = ? WHERE id = ?";
        Connection conn = ConnectionManager.getConnection();
        try {
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, fullName);
            stmt.setString(2, department);
            stmt.setString(3, email);
            stmt.setString(4, phone);
            stmt.setLong(5, id);
            int rows = stmt.executeUpdate();
            return rows > 0;
        } finally {
            ConnectionManager.closeConnection(conn);
        }
    }

    public boolean updateEmail(long id, String email) throws SQLException {
        String sql = "UPDATE receptionists SET email = ? WHERE id = ?";
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

    public Long getUserIdForReceptionist(long receptionistId) throws SQLException {
        String sql = "SELECT user_id FROM receptionists WHERE id = ?";
        Connection conn = ConnectionManager.getConnection();
        try {
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setLong(1, receptionistId);
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
        String selectSql = "SELECT user_id FROM receptionists WHERE id = ?";
        String deleteReceptionistSql = "DELETE FROM receptionists WHERE id = ?";
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

            PreparedStatement delRecp = conn.prepareStatement(deleteReceptionistSql);
            delRecp.setLong(1, id);
            int recpRows = delRecp.executeUpdate();

            if (recpRows <= 0) {
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
