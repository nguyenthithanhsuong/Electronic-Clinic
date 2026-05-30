package com.eclinic.database;

import java.sql.Connection;
import java.sql.Statement;

public class Migration {
    public static void main(String[] args) throws Exception {
        String dbUrl = args[0];
        String dbUser = args[1];
        String dbPassword = args[2];

        ConnectionManager.init(dbUrl, dbUser, dbPassword);
        Connection conn = ConnectionManager.getConnection();
        Statement stmt = conn.createStatement();

        System.out.println("Running migrations...");

        try {
            stmt.execute("ALTER TABLE prescriptions ADD COLUMN payment_status VARCHAR(10) DEFAULT 'UNPAID'");
            System.out.println("  + Added payment_status to prescriptions");
        } catch (Exception e) {
            System.out.println("  ~ payment_status already exists or error: " + e.getMessage());
        }

        try {
            stmt.execute("ALTER TABLE prescriptions ADD COLUMN paid_at TIMESTAMP");
            System.out.println("  + Added paid_at to prescriptions");
        } catch (Exception e) {
            System.out.println("  ~ paid_at already exists or error: " + e.getMessage());
        }

        try {
            stmt.execute("CREATE TABLE IF NOT EXISTS audit_logs (" +
                "id BIGSERIAL PRIMARY KEY, " +
                "action VARCHAR(50) NOT NULL, " +
                "actor VARCHAR(100) NOT NULL, " +
                "target VARCHAR(200), " +
                "created_at TIMESTAMP DEFAULT NOW()" +
                ")");
            System.out.println("  + Created audit_logs table");
        } catch (Exception e) {
            System.out.println("  ~ audit_logs error: " + e.getMessage());
        }

        stmt.close();
        ConnectionManager.closeConnection(conn);
        System.out.println("Migrations complete.");
    }
}
