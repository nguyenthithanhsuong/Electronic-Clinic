package com.eclinic.api;

import com.eclinic.database.ConnectionManager;
import com.eclinic.dao.*;
import com.eclinic.models.*;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SimpleRestServer {
    private static int PORT = 3001;

    public static void main(String[] args) throws Exception {
        Map<String, String> dotenv = loadDotEnv();

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
        ServerSocket serverSocket = new ServerSocket(PORT);
        System.out.println("REST API server started. Open http://localhost:" + PORT + " in browser.");

        while (true) {
            Socket clientSocket = serverSocket.accept();
            new Thread(new ClientHandler(clientSocket)).start();
        }
    }

    static class ClientHandler implements Runnable {
        private Socket socket;

        ClientHandler(Socket socket) {
            this.socket = socket;
        }

        public void run() {
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);

                String requestLine = reader.readLine();
                if (requestLine == null) return;

                String[] parts = requestLine.split(" ");
                String method = parts[0];
                String path = parts[1];

                Map headers = new HashMap();
                String line;
                int contentLength = 0;
                while ((line = reader.readLine()) != null && !line.equals("")) {
                    if (line.toLowerCase().startsWith("content-length:")) {
                        contentLength = Integer.parseInt(line.substring(15).trim());
                    }
                }

                String body = "";
                if (contentLength > 0) {
                    char[] buffer = new char[contentLength];
                    reader.read(buffer);
                    body = new String(buffer);
                }

                String response = handleRequest(method, path, body);
                writer.println("HTTP/1.1 200 OK");
                writer.println("Content-Type: application/json");
                writer.println("Access-Control-Allow-Origin: *");
                writer.println("Access-Control-Allow-Methods: GET, POST, PUT, DELETE, OPTIONS");
                writer.println("Access-Control-Allow-Headers: Content-Type");
                writer.println("Content-Length: " + response.getBytes().length);
                writer.println("");
                writer.println(response);
                writer.flush();

                socket.close();
            } catch (Exception e) {
                System.err.println("Error handling client: " + e.getMessage());
            }
        }
    }

    private static String handleRequest(String method, String path, String body) {
        try {
            if ("OPTIONS".equals(method)) {
                return "{}";
            }

            if (path.startsWith("/api/dashboard")) {
                return handleDashboard();
            } else if (path.startsWith("/api/doctors")) {
                return handleDoctors(method, path, body);
            } else if (path.startsWith("/api/patients")) {
                return handlePatients(method, path, body);
            } else if (path.startsWith("/api/appointments")) {
                return handleAppointments(method, path, body);
            } else if (path.startsWith("/api/medicines")) {
                return handleMedicines(method, path, body);
            } else {
                return serveStaticFile(path);
            }
        } catch (Exception e) {
            return "{\"error\": \"" + e.getMessage().replace("\"", "\\\"") + "\"}";
        }
    }

    private static String handleDashboard() throws Exception {
        UserDAO userDAO = new UserDAO();
        DoctorDAO doctorDAO = new DoctorDAO();
        List allUsers = userDAO.findAll();
        List allDoctors = doctorDAO.findAll();
        int totalPatients = 0;
        for (int i = 0; i < allUsers.size(); i++) {
            User u = (User) allUsers.get(i);
            if ("PATIENT".equals(u.getRole())) totalPatients++;
        }
        return "{\"totalUsers\": " + allUsers.size() + ", \"totalDoctors\": " + allDoctors.size() + ", \"totalPatients\": " + totalPatients + ", \"todayAppointments\": 0, \"revenue\": 0}";
    }

    private static String handleDoctors(String method, String path, String body) throws Exception {
        DoctorDAO dao = new DoctorDAO();
        if ("GET".equals(method)) {
            List allDoctors = dao.findAll();
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < allDoctors.size(); i++) {
                if (i > 0) sb.append(",");
                Doctor d = (Doctor) allDoctors.get(i);
                sb.append("{\"id\":").append(d.getId()).append(",\"fullName\":\"").append(d.getFullName())
                        .append("\",\"specialty\":\"").append(d.getSpecialty()).append("\"}");
            }
            sb.append("]");
            return sb.toString();
        } else if ("POST".equals(method)) {
            long userId = extractLong(body, "userId");
            String fullName = extractString(body, "fullName");
            String specialty = extractString(body, "specialty");
            String phone = extractString(body, "phone");
            String email = extractString(body, "email");
            String roomNumber = extractString(body, "roomNumber");
            long id = dao.create(userId, fullName, specialty, phone, email, roomNumber);
            return "{\"id\": " + id + ", \"status\": \"created\"}";
        }
        return "{}";
    }

    private static String handlePatients(String method, String path, String body) throws Exception {
        PatientDAO dao = new PatientDAO();
        if ("GET".equals(method)) {
            List allPatients = dao.findAll();
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < allPatients.size(); i++) {
                if (i > 0) sb.append(",");
                Patient p = (Patient) allPatients.get(i);
                sb.append("{\"id\":").append(p.getId()).append(",\"fullName\":\"").append(p.getFullName())
                        .append("\",\"phone\":\"").append(p.getPhone()).append("\"}");
            }
            sb.append("]");
            return sb.toString();
        } else if ("POST".equals(method)) {
            String fullName = extractString(body, "fullName");
            String dob = extractString(body, "dob");
            String gender = extractString(body, "gender");
            String phone = extractString(body, "phone");
            String address = extractString(body, "address");
            String insuranceCode = extractString(body, "insuranceCode");
            long id = dao.create(null, fullName, dob, gender, phone, address, insuranceCode);
            return "{\"id\": " + id + ", \"status\": \"created\"}";
        }
        return "{}";
    }

    private static String handleAppointments(String method, String path, String body) throws Exception {
        AppointmentDAO dao = new AppointmentDAO();
        if ("GET".equals(method)) {
            StringBuilder sb = new StringBuilder("[");
            sb.append("]");
            return sb.toString();
        } else if ("POST".equals(method)) {
            long doctorId = extractLong(body, "doctorId");
            long patientId = extractLong(body, "patientId");
            String startDate = extractString(body, "appointmentStartDate");
            String endDate = extractString(body, "appointmentEndDate");
            String reason = extractString(body, "reason");
            String status = extractString(body, "status");
            long id = dao.create(doctorId, patientId, startDate, endDate, reason, status);
            return "{\"id\": " + id + ", \"status\": \"created\"}";
        }
        return "{}";
    }

    private static String handleMedicines(String method, String path, String body) throws Exception {
        MedicineDAO dao = new MedicineDAO();
        if ("GET".equals(method)) {
            List allMeds = dao.findAll();
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < allMeds.size(); i++) {
                if (i > 0) sb.append(",");
                Medicine m = (Medicine) allMeds.get(i);
                sb.append("{\"id\":").append(m.getId()).append(",\"name\":\"").append(m.getName())
                        .append("\",\"stockQuantity\":").append(m.getStockQuantity()).append("}");
            }
            sb.append("]");
            return sb.toString();
        } else if ("POST".equals(method)) {
            String name = extractString(body, "name");
            String unit = extractString(body, "unit");
            int stock = Integer.parseInt(extractString(body, "stockQuantity"));
            long id = dao.create(name, unit, new java.math.BigDecimal("1.0"), stock, extractString(body, "expiryDate"));
            return "{\"id\": " + id + ", \"status\": \"created\"}";
        }
        return "{}";
    }

    private static String serveStaticFile(String path) throws Exception {
        if ("/".equals(path)) path = "/index.html";
        File file = new File("frontend-mock" + path);
        if (file.exists() && file.isFile()) {
            BufferedReader fr = new BufferedReader(new FileReader(file));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = fr.readLine()) != null) {
                sb.append(line).append("\n");
            }
            fr.close();
            return sb.toString();
        }
        return "<h1>File not found: " + path + "</h1>";
    }

    private static long extractLong(String json, String key) {
        String search = "\"" + key + "\":";
        int idx = json.indexOf(search);
        if (idx == -1) return 0;
        int end = json.indexOf(",", idx);
        if (end == -1) end = json.indexOf("}", idx);
        String num = json.substring(idx + search.length(), end).trim();
        try {
            return Long.parseLong(num);
        } catch (Exception e) {
            return 0;
        }
    }

    private static String extractString(String json, String key) {
        String search = "\"" + key + "\":\"";
        int idx = json.indexOf(search);
        if (idx == -1) return "";
        int start = idx + search.length();
        int end = json.indexOf("\"", start);
        if (end == -1) return "";
        return json.substring(start, end);
    }

    private static Map<String, String> loadDotEnv() {
        Map<String, String> env = new HashMap<String, String>();
        File dotenvFile = new File(".env");
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
                        if (value.startsWith("\"") && value.endsWith("\"")) value = value.substring(1, value.length() - 1);
                        env.put(key, value);
                    }
                }
            } catch (IOException ignored) {
            } finally {
                if (reader != null) try { reader.close(); } catch (IOException ignored) {}
            }
        }
        return env;
    }

    private static String getConfig(Map<String, String> dotenv, String envKey, String systemPropertyKey) {
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
