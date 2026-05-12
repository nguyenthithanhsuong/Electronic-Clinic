package com.eclinic.api;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpContext;
import com.eclinic.database.ConnectionManager;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.HashMap;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class RestServer {
    private static int PORT = 8080;
    private static HttpServer server;

    public static void main(String[] args) throws Exception {
        Map dotenv = loadDotEnv();

        String dbUrl = getConfig(dotenv, "SUPABASE_DB_URL", "supabase.db.url");
        String dbUser = getConfig(dotenv, "SUPABASE_DB_USER", "supabase.db.user");
        String dbPassword = getConfig(dotenv, "SUPABASE_DB_PASSWORD", "supabase.db.password");

        if (isBlank(dbUrl) || isBlank(dbUser) || isBlank(dbPassword)) {
            System.err.println("Missing database configuration");
            return;
        }

        System.out.println("Initializing database connection...");
        ConnectionManager.init(dbUrl, dbUser, dbPassword);
        System.out.println("Database connection initialized.");

        System.out.println("Starting REST API server on http://localhost:" + PORT);
        server = HttpServer.create(new InetSocketAddress("localhost", PORT), 0);

        // Dashboard
        server.createContext("/api/dashboard", new DashboardHandler());

        // Users
        server.createContext("/api/users", new UsersHandler());

        // Doctors
        server.createContext("/api/doctors", new DoctorsHandler());

        // Patients
        server.createContext("/api/patients", new PatientsHandler());
        server.createContext("/api/patients/medical-records", new MedicalRecordsHandler());

        // Appointments
        server.createContext("/api/appointments", new AppointmentsHandler());

        // Medicines
        server.createContext("/api/medicines", new MedicinesHandler());

        // Prescriptions
        server.createContext("/api/prescriptions", new PrescriptionHandler());
        server.createContext("/api/prescriptions/details", new PrescriptionDetailHandler());

        // Static files (HTML, CSS, JS)
        server.createContext("/", new StaticFileHandler());

        server.setExecutor(null);
        server.start();
        System.out.println("REST API server started. Open http://localhost:" + PORT + " in browser.");
    }

    private static Map loadDotEnv() {
        Map env = new HashMap();
        File dotenvFile = new File("Clinic/.env");
        if (!dotenvFile.exists()) {
            dotenvFile = new File(".env");
        }
        if (dotenvFile.exists()) {
            BufferedReader reader = null;
            try {
                reader = new BufferedReader(new FileReader(dotenvFile));
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("#") || line.trim().equals("")) continue;
                    int eq = line.indexOf('=');
                    if (eq > 0) {
                        String key = line.substring(0, eq).trim();
                        String value = line.substring(eq + 1).trim();
                        if (value.startsWith("\"") && value.endsWith("\"")) {
                            value = value.substring(1, value.length() - 1);
                        }
                        env.put(key, value);
                    }
                }
            } catch (IOException ignored) {
            } finally {
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (IOException ignored) {
                    }
                }
            }
        }
        return env;
    }

    private static String getConfig(Map dotenv, String envKey, String systemPropertyKey) {
        String val = System.getProperty(systemPropertyKey);
        if (!isBlank(val)) return val;
        val = System.getenv(envKey);
        if (!isBlank(val)) return val;
        return (String) dotenv.get(envKey);
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().equals("");
    }
}
