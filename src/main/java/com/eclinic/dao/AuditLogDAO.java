package com.eclinic.dao;

import com.eclinic.database.ConnectionManager;
import com.eclinic.models.AuditLog;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class AuditLogDAO {

    public void log(String action, String actor, String target) throws SQLException {
        String sql = "INSERT INTO audit_logs (action, actor, target) VALUES (?, ?, ?)";
        Connection conn = ConnectionManager.getConnection();
        try {
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, action);
            stmt.setString(2, actor);
            stmt.setString(3, target);
            stmt.executeUpdate();
        } finally {
            ConnectionManager.closeConnection(conn);
        }
    }

    public List findRecent(int limit) throws SQLException {
        String sql = "SELECT id, action, actor, target, created_at FROM audit_logs ORDER BY created_at DESC LIMIT ?";
        Connection conn = ConnectionManager.getConnection();
        List logs = new ArrayList();
        try {
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, limit);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                logs.add(new AuditLog(
                    rs.getLong("id"),
                    rs.getString("action"),
                    rs.getString("actor"),
                    rs.getString("target"),
                    rs.getString("created_at")
                ));
            }
            return logs;
        } finally {
            ConnectionManager.closeConnection(conn);
        }
    }
}
