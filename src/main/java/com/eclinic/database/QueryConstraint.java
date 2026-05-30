package com.eclinic.database;

import java.sql.Connection;
import java.sql.Statement;
import java.sql.ResultSet;

public class QueryConstraint {
    public static void main(String[] args) throws Exception {
        ConnectionManager.init(args[0], args[1], args[2]);
        Connection conn = ConnectionManager.getConnection();
        Statement stmt = conn.createStatement();

        ResultSet rs = stmt.executeQuery(
            "SELECT conname, pg_get_constraintdef(oid) FROM pg_constraint WHERE conname = 'users_role_check'"
        );
        while (rs.next()) {
            System.out.println(rs.getString(1) + ": " + rs.getString(2));
        }

        stmt.close();
        ConnectionManager.closeConnection(conn);
    }
}
