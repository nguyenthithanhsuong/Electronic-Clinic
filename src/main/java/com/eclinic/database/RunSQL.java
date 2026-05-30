package com.eclinic.database;

import java.sql.Connection;
import java.sql.Statement;

public class RunSQL {
    public static void main(String[] args) throws Exception {
        ConnectionManager.init(args[0], args[1], args[2]);
        Connection conn = ConnectionManager.getConnection();
        Statement stmt = conn.createStatement();
        for (int i = 3; i < args.length; i++) {
            System.out.println("Executing: " + args[i]);
            stmt.execute(args[i]);
            System.out.println("  OK");
        }
        stmt.close();
        ConnectionManager.closeConnection(conn);
    }
}
