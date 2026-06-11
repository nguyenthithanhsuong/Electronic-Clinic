package com.eclinic.database;

import java.sql.Connection;
import java.sql.Statement;
import java.sql.SQLException;
import java.util.Map;
import java.util.HashMap;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

public class SeedData {
    public static void main(String[] args) throws Exception {
        Map<String, String> dotenv = loadDotEnv();

        String dbUrl = System.getenv("SUPABASE_DB_URL");
        if (dbUrl == null) dbUrl = dotenv.getOrDefault("SUPABASE_DB_URL", "");
        
        String dbUser = System.getenv("SUPABASE_DB_USER");
        if (dbUser == null) dbUser = dotenv.getOrDefault("SUPABASE_DB_USER", "");
        
        String dbPassword = System.getenv("SUPABASE_DB_PASSWORD");
        if (dbPassword == null) dbPassword = dotenv.getOrDefault("SUPABASE_DB_PASSWORD", "");

        if (dbUrl.isEmpty() || dbUser.isEmpty() || dbPassword.isEmpty()) {
            System.err.println("Missing database configuration in .env");
            System.exit(1);
        }

        ConnectionManager.init(dbUrl, dbUser, dbPassword);
        Connection conn = ConnectionManager.getConnection();
        Statement stmt = conn.createStatement();

        System.out.println("Seeding test data...");

        try {
            // Insert test users
            String sql = "INSERT INTO users (username, password_hash, role, status) VALUES " +
                "('admin', 'admin123', 'ADMIN', 'ACTIVE'), " +
                "('receptionist', 'rec123', 'RECEPTIONIST', 'ACTIVE'), " +
                "('doctor', 'doc123', 'DOCTOR', 'ACTIVE'), " +
                "('patient', 'pat123', 'PATIENT', 'ACTIVE') " +
                "ON CONFLICT (username) DO NOTHING";
            stmt.execute(sql);
            System.out.println("  ✓ Inserted test users");
        } catch (SQLException e) {
            System.out.println("  ✗ Error inserting users: " + e.getMessage());
        }

        try {
            // Insert doctor profile
            String sql = "INSERT INTO doctors (user_id, full_name, specialty, phone) " +
                "SELECT id, 'BS Nguyễn Thị Hoa', 'Khám tổng quát', '0912345678' " +
                "FROM users WHERE username = 'doctor' " +
                "ON CONFLICT (user_id) DO NOTHING";
            stmt.execute(sql);
            System.out.println("  ✓ Inserted doctor profile");
        } catch (SQLException e) {
            System.out.println("  ✗ Error inserting doctor: " + e.getMessage());
        }

        try {
            // Verify
            var rs = stmt.executeQuery("SELECT COUNT(*) as count FROM users");
            if (rs.next()) {
                System.out.println("  ✓ Total users in database: " + rs.getInt("count"));
            }
        } catch (Exception e) {
            System.out.println("  ✗ Error verifying: " + e.getMessage());
        }

        stmt.close();
        ConnectionManager.closeConnection(conn);
        System.out.println("Seeding complete.");
    }

    private static Map<String, String> loadDotEnv() {
        Map<String, String> env = new HashMap<String, String>();
        File dotenvFile = new File(".env");
        if (dotenvFile.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(dotenvFile))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("#") || line.trim().equals("")) continue;
                    int eq = line.indexOf('=');
                    if (eq > 0) {
                        String key = line.substring(0, eq).trim();
                        String value = line.substring(eq + 1).trim();
                        env.put(key, value);
                    }
                }
            } catch (Exception e) {
                System.err.println("Error reading .env: " + e.getMessage());
            }
        }
        return env;
    }
}
