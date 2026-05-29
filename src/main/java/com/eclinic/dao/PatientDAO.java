package com.eclinic.dao;

import com.eclinic.database.ConnectionManager;
import com.eclinic.models.Patient;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class PatientDAO {

    public long create(Long userId, String fullName, String dob, String gender, String phone, String address, String insuranceCode) throws SQLException {
        String sql = "INSERT INTO patients (user_id, full_name, dob, gender, phone, address, insurance_code) VALUES (?, ?, ?, ?, ?, ?, ?) RETURNING id";
        Connection conn = ConnectionManager.getConnection();
        try {
            PreparedStatement stmt = conn.prepareStatement(sql);
            if (userId != null) {
                stmt.setLong(1, userId);
            } else {
                stmt.setNull(1, java.sql.Types.BIGINT);
            }
            stmt.setString(2, fullName);
            stmt.setDate(3, Date.valueOf(dob));
            stmt.setString(4, gender);
            stmt.setString(5, phone);
            stmt.setString(6, address);
            stmt.setString(7, insuranceCode);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getLong(1);
            }
            throw new SQLException("Failed to create patient.");
        } finally {
            ConnectionManager.closeConnection(conn);
        }
    }

    public Patient findById(long id) throws SQLException {
        String sql = "SELECT p.id, p.user_id, u.username, p.full_name, p.dob, p.gender, p.phone, p.address, p.insurance_code, p.created_at FROM patients p LEFT JOIN users u ON p.user_id = u.id WHERE p.id = ?";
        Connection conn = ConnectionManager.getConnection();
        try {
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setLong(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                Long userId = rs.getLong("user_id");
                if (rs.wasNull()) {
                    userId = null;
                }
                return new Patient(
                    rs.getLong("id"),
                    userId,
                    rs.getString("username"),
                    rs.getString("full_name"),
                    rs.getString("dob"),
                    rs.getString("gender"),
                    rs.getString("phone"),
                    null,  // email is stored in users table, not patients table
                    null,  // password is stored in users table, not patients table
                    rs.getString("address"),
                    rs.getString("insurance_code"),
                    rs.getString("created_at")
                );
            }
            return null;
        } finally {
            ConnectionManager.closeConnection(conn);
        }
    }

    public List findAll() throws SQLException {
        String sql = "SELECT p.id, p.user_id, u.username, p.full_name, p.dob, p.gender, p.phone, p.address, p.insurance_code, p.created_at FROM patients p LEFT JOIN users u ON p.user_id = u.id";
        Connection conn = ConnectionManager.getConnection();
        List patients = new ArrayList();
        try {
            PreparedStatement stmt = conn.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Long userId = rs.getLong("user_id");
                if (rs.wasNull()) {
                    userId = null;
                }
                patients.add(new Patient(
                    rs.getLong("id"),
                    userId,
                    rs.getString("username"),
                    rs.getString("full_name"),
                    rs.getString("dob"),
                    rs.getString("gender"),
                    rs.getString("phone"),
                    null,  // email is stored in users table, not patients table
                    null,  // password is stored in users table, not patients table
                    rs.getString("address"),
                    rs.getString("insurance_code"),
                    rs.getString("created_at")
                ));
            }
            return patients;
        } finally {
            ConnectionManager.closeConnection(conn);
        }
    }

    public boolean update(long id, String fullName, String phone, String address) throws SQLException {
        String sql = "UPDATE patients SET full_name = ?, phone = ?, address = ? WHERE id = ?";
        Connection conn = ConnectionManager.getConnection();
        try {
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, fullName);
            stmt.setString(2, phone);
            stmt.setString(3, address);
            stmt.setLong(4, id);
            int rows = stmt.executeUpdate();
            return rows > 0;
        } finally {
            ConnectionManager.closeConnection(conn);
        }
    }

    public boolean updateEmail(long id, String email) throws SQLException {
        String sql = "UPDATE patients SET email = ? WHERE id = ?";
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

    public Long getUserIdForPatient(long patientId) throws SQLException {
        String sql = "SELECT user_id FROM patients WHERE id = ?";
        Connection conn = ConnectionManager.getConnection();
        try {
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setLong(1, patientId);
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
        String selectSql = "SELECT user_id FROM patients WHERE id = ?";
        String deletePatientSql = "DELETE FROM patients WHERE id = ?";
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

            PreparedStatement delPatient = conn.prepareStatement(deletePatientSql);
            delPatient.setLong(1, id);
            int patientRows = delPatient.executeUpdate();

            if (patientRows <= 0) {
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
