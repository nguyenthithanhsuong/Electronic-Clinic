package com.eclinic.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class ConnectionManager {
    private static String dbUrl;
    private static String dbUser;
    private static String dbPassword;

    public static void init(String url, String user, String password) {
        dbUrl = url;
        dbUser = user;
        dbPassword = password;
    }

    public static Connection getConnection() throws SQLException {
        if (dbUrl == null || dbUser == null || dbPassword == null) {
            throw new SQLException("Database connection not initialized. Call ConnectionManager.init() first.");
        }
        return DriverManager.getConnection(dbUrl, dbUser, dbPassword);
    }

    public static void closeConnection(Connection conn) {
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException ignored) {
            }
        }
    }
}
